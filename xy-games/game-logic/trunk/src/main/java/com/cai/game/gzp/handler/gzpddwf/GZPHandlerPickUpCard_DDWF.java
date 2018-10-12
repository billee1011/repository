package com.cai.game.gzp.handler.gzpddwf;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.gzp.GZPTable;
import com.cai.game.gzp.handler.GZPHandlerDispatchCard;
import com.cai.game.gzp.handler.GZPHandlerPickUpOperate;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class GZPHandlerPickUpCard_DDWF extends GZPHandlerPickUpOperate {

	@Override
	public void exe(GZPTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			//table._playerStatus[i].clean_status();
		}

		table._playerStatus[_pick_up_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_pick_up_index];
		curPlayerStatus.reset();

		if (table.DEBUG_CARDS_MODE) {
//			 table.GRR._left_card_count=6;
		}

	

		table._current_player = _pick_up_index;// 轮到操作的人是自己


		table._provide_player = _pick_up_index;
		table._pick_up_card = _pick_up_data;
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_pick_up_index];
		chr.set_empty();


		// 加到手牌
		table.GRR._cards_index[_pick_up_index][table._logic.switch_to_card_index(_pick_up_data)]++;
//		table.GRR._pick_up_card_index[_pick_up_index][table._logic.switch_to_card_index(_pick_up_data)]++;
		table._pick_up_index[_pick_up_index][table._logic.switch_to_card_index(_pick_up_data)]++;
		//效果
		table.operate_effect_action(_pick_up_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
				
		// 发送数据
		// 只有自己才有数值
//		table.operate_player_get_card(_pick_up_index, 1, new int[] { _pick_up_data }, GameConstants.INVALID_SEAT);
		//刷新手牌包括组合
							
		int cur_logic_index =  table._logic.switch_to_card_logic_index(_pick_up_data);
		

		
		
			//删掉出来的那张牌
			//table.operate_out_card(this._provider, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
		table.operate_remove_discard(this._out_card_player, table.GRR._discard_count[_out_card_player],_pick_up_index);
		
		
		table.operate_pick_up_single_card(_pick_up_index,1,new int[]{_pick_up_data});
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_pick_up_index], cards);
		table.operate_player_cards(_pick_up_index, hand_card_count, cards, table.GRR._weave_count[_pick_up_index], table.GRR._weave_items[_pick_up_index]);
		table.cannot_pickup_card_ddwf(_pick_up_index, 1, _pick_up_data, true);
		// 设置变量
		_pick_up_data = 0; 
	
		if (curPlayerStatus.has_action()) {// 有动作
			if (table.isTrutess(_pick_up_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _pick_up_index, _pick_up_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.change_player_status(_pick_up_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_pick_up_index, false);
		} else {
			if (table.isTrutess(_pick_up_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(),_pick_up_index, _pick_up_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			// 不能换章,自动出牌
			if (table._playerStatus[_pick_up_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _pick_up_index, _pick_up_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.change_player_status(_pick_up_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(GZPTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}

		if (seat_index != _pick_up_index) {
			table.log_error("DispatchCard 不是当前玩家操作");
			return false;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
	
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		//playerStatus.clean_status();
		table.change_player_status(seat_index,GameConstants.INVALID_VALUE);
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			//add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			// 用户状态
			table._playerStatus[_pick_up_index].clean_action();
			//table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_pick_up_index, GameConstants.INVALID_VALUE);
			if (table._playerStatus[_pick_up_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _pick_up_data, _pick_up_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_pick_up_index, GameConstants.Player_Status_OUT_CARD);
				//table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			//table._playerStatus[i].clean_status();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (operate_code) {

		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_pick_up_index].set_valid(true);

			table.GRR._chi_hu_card[_pick_up_index][0] = operate_card;

			table._banker_select = _pick_up_index;
			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
				if (table.GRR._banker_player == _pick_up_index) {
					table._banker_select = _pick_up_index;
				} else {
					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
							% table.getTablePlayerNumber();
				}
			}
			table._shang_zhuang_player = _pick_up_index;
			table.process_chi_hu_player_operate(_pick_up_index, operate_card, true);
			table.process_chi_hu_player_score_gzp_ddwf(_pick_up_index, _pick_up_index, operate_card, true);

		
			table.countChiHuTimes(_pick_up_index, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[_pick_up_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[_pick_up_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _pick_up_index, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(GZPTable table,int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		boolean is_out = false;
		if((card&0x100)>>8 == 1)
		{
			card&=0xFF;
			is_out = true;
		}
		if(is_out == true && table._pick_up_index[_pick_up_index][table._logic.switch_to_card_index(card)] == 0)
		{
			table.log_error("出捡牌,牌型出错");
			return false;
		}
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _pick_up_index) {
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_info("状态不对不能出牌");
			return false;
		}
		if(is_out == false &&table.GRR._cannot_out_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_info("当前牌不能出");
			return false;
		}
		if(table._cannot_pickup_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_info("当前捡的牌不能出");
			return false;
		}
//		if (card == MJGameConstants.ZZ_MAGIC_CARD && table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
//			table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
//			table.log_error("癞子牌不能出癞子");
//			return false;
//		}

		// 删除扑克
		int flower_index = table._logic.switch_to_card_flower_index(card);
		int common_index =  table._logic.switch_to_card_common_index(card);
		int card_index = table._logic.switch_to_card_index(card);
		if(flower_index != -1)
		{
			if(table._pick_up_index[_pick_up_index][flower_index]>0)
			{
				table._pick_up_index[_pick_up_index][flower_index]--;
				table.operate_pick_up_card(_pick_up_index);
			}
			else if(table._pick_up_index[_pick_up_index][card_index]>0)
			{
				table._pick_up_index[_pick_up_index][card_index]--;
				table.operate_pick_up_card(_pick_up_index);	
			}
		}
		else if(common_index != -1)
		{
			if(table._pick_up_index[_pick_up_index][card_index]>0)
			{
				table._pick_up_index[_pick_up_index][card_index]--;
				table.operate_pick_up_card(_pick_up_index);
			}
			else if(table._pick_up_index[_pick_up_index][card_index]>0)
			{
				table._pick_up_index[_pick_up_index][card_index]--;
				table.operate_pick_up_card(_pick_up_index);	
			}
		}
		else {
			if(table._pick_up_index[_pick_up_index][card_index]>0)
			{
				table._pick_up_index[_pick_up_index][card_index]--;
				table.operate_pick_up_card(_pick_up_index);	
			}
		}
			
		
			
		
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_pick_up_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		//出牌
		table.exe_out_card(_pick_up_index,card,GameConstants.WIK_NULL);

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(GZPTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.be_in_room_trustee(seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
	
	
	@Override
	public boolean handler_be_set_trustee(GZPTable table, int seat_index){
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
		if(curPlayerStatus.has_action()){
			table.operate_player_action(seat_index, true);
			if(curPlayerStatus.has_zi_mo()){
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_ZI_MO, _pick_up_data);
			}else{
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, 0);
			}
		}else if(curPlayerStatus.get_status() == GameConstants.Player_Status_OUT_CARD){	
			int out_card = GameConstants.INVALID_VALUE;			
			int send_index = table._logic.switch_to_card_index(_pick_up_data);
			if(send_index != GameConstants.MAX_FLS_INDEX_DP && table.GRR._cards_index[seat_index][send_index] > 0){//手牌有
				out_card = _pick_up_data;
			}else{
				for (int i = 0; i < GameConstants.MAX_FLS_INDEX_DP; i++) {
					if(table.GRR._cards_index[seat_index][i] > 0){//托管 随意出一张牌
						out_card = table._logic.switch_to_card_data(i);
					}
				}			
			}
			if(out_card != GameConstants.INVALID_VALUE){
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, out_card),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);		
			}
		}
		return false;
	}

}
