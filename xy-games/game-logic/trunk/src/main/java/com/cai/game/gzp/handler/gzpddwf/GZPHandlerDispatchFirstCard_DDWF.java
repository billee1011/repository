package com.cai.game.gzp.handler.gzpddwf;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.gzp.GZPTable;
import com.cai.game.gzp.handler.GZPHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class GZPHandlerDispatchFirstCard_DDWF extends GZPHandlerDispatchCard {

	@Override
	public void exe(GZPTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			//table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

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

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();


		// 从牌堆拿出一张牌
		if(table._guo_qi_guan[_seat_index] == false){
			if(table._pu_count == 2)
			{
				table._send_card_count++;
				table._pu_card[0] = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				--table.GRR._left_card_count;
				table._send_card_count++;
				int cur_logic_index = table._logic.switch_to_card_common_index(table._pu_card[0]);
				if(cur_logic_index == -1)
					cur_logic_index = table._logic.switch_to_card_index(table._pu_card[0]);
				if(table._sheng_guan_index[_seat_index][cur_logic_index] == 1)
				{
					table.cannot_outcard(_seat_index, -1, table._pu_card[0], true);
				}
				
			}
		
				table._send_card_count++;
				_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				--table.GRR._left_card_count;
				table._pu_card[1] = _send_card_data;
			
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				int cur_logic_index = table._logic.switch_to_card_common_index(_send_card_data);
				if(cur_logic_index == -1)
					cur_logic_index = table._logic.switch_to_card_index(_send_card_data);
				if(table._sheng_guan_index[_seat_index][cur_logic_index] == 1)
				{
					table.cannot_outcard(_seat_index, -1, _send_card_data, true);
				}
			// 加到手牌
			

			// 发送数据
			// 只有自己才有数值
			// 发送数据
			// 只有自己才有数值
			if(table._pu_count == 2)
				table.operate_player_get_card(_seat_index, table._pu_count, table._pu_card, GameConstants.INVALID_SEAT,false,1);
			
			else if(table.GRR._left_card_count > 3)
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false,1);
			else
			    table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,true,0);

		}
		

//		if (table.DEBUG_CARDS_MODE) {
//			_send_card_data = 0x11;
//		}
		int hu_pai = 0;
		table._provide_player = _seat_index;
		if(table._first_card == 0){
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();

			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],table._sheng_guan_index[_seat_index],table._pick_up_index[_seat_index], _send_card_data, chr, card_type,_seat_index,_seat_index);// 自摸

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
			if (action != GameConstants.WIK_NULL) {
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
				hu_pai = 1;
				if(table.getRuleValue(GameConstants.GAME_RULE_TC_ZI_MO_HU)==1){
					GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_send_card_data),
							600, TimeUnit.MILLISECONDS);
					return ;
				}
			} else {
				chr.set_empty();
			}
			table._first_card ++ ;
			if(hu_pai == 0){
				
				table._pu_count = 1;
				_send_card_data = 0;
				table._send_card_data = 0;
				Arrays.fill(table._pu_card, 0);
				_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();
			}
		}
		//判断是否观生
		while (table._guan_sheng_count<table.getTablePlayerNumber()&&hu_pai == 0){
			while(table._guan_sheng_count<table.getTablePlayerNumber()&&table._guo_qi_guan[_seat_index] == true){
				table._guan_sheng_count++;
				table._pu_count = 1;
				_send_card_data = 0;
				table._send_card_data = 0;
				_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();
			}
			if(table._guan_sheng_count == table.getTablePlayerNumber())
			{
				break;
			}
			curPlayerStatus = table._playerStatus[_seat_index];
			table.estimate_player_sheng_guan(_seat_index,0);
			if(curPlayerStatus.has_action_by_bh_code(GameConstants.GZP_WIK_GUAN))
				table.estimate_player_hua(_seat_index, 0,false);
			// 发牌处理,判断发给的这个人有没有胡牌或杠牌
			// 胡牌判断
			if(_seat_index == table.GRR._banker_player&&_send_card_data != 0)
			{
				ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
				chr.set_empty();

				int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
				int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index],table._sheng_guan_index[_seat_index],table._pick_up_index[_seat_index], _send_card_data, chr, card_type,_seat_index,_seat_index);// 自摸

				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				if (action != GameConstants.WIK_NULL) {
					// 添加动作
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
					if(table.getRuleValue(GameConstants.GAME_RULE_TC_ZI_MO_HU)==1){
						GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_send_card_data),
								600, TimeUnit.MILLISECONDS);
						return ;
					}
					break;

				} else {
					chr.set_empty();
				}
			}
	
			
			if(curPlayerStatus.has_action()){
				break;
			}
			else {
				table._pu_count = 1;
				_send_card_data = 0;
				table._send_card_data = 0;
				Arrays.fill(table._pu_card, 0);
				table._guan_sheng_count ++;
				_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();
			}
		
			

		};
		//判断是否观生
		while (table._guan_sheng_count==table.getTablePlayerNumber()&&table._hua_count<table.getTablePlayerNumber()){
			
			
			while(table._hua_count<table.getTablePlayerNumber()&&table._guo_qi_guan[_seat_index] == true){
				table._hua_count++;
				table._pu_count = 1;
				_send_card_data = 0;
				table._send_card_data = 0;
				_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();
			}
			if(table._hua_count == table.getTablePlayerNumber())
			{
				break;
			}
			curPlayerStatus = table._playerStatus[_seat_index];
			table.estimate_player_hua(_seat_index, 0,false);

			// 发牌处理,判断发给的这个人有没有胡牌或杠牌
			// 胡牌判断
			if(_seat_index == table.GRR._banker_player&&_send_card_data != 0)
			{
				ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
				chr.set_empty();

				int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
				int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index],table._sheng_guan_index[_seat_index],table._pick_up_index[_seat_index], _send_card_data, chr, card_type,_seat_index,_seat_index);// 自摸

				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				if (action != GameConstants.WIK_NULL) {
					// 添加动作
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
					if(table.getRuleValue(GameConstants.GAME_RULE_TC_ZI_MO_HU)==1){
						GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_send_card_data),
								600, TimeUnit.MILLISECONDS);
						return ;
					}
					break;

				} else {
					chr.set_empty();
				}
			}
			if(curPlayerStatus.has_action()){
				break;
			}
			else {
				table._pu_count = 1;
				_send_card_data = 0;
				table._send_card_data = 0;
				Arrays.fill(table._pu_card, 0);
				table._hua_count ++;
				_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();
			}
		
			

		};
		
		
		


		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		if (curPlayerStatus.has_action()) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table._current_player = _seat_index;
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table._current_player = table.GRR._banker_player;
				_seat_index = table.GRR._banker_player;
				table.change_player_status(table.GRR._banker_player, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_action(table.GRR._banker_player, false);
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

		if (seat_index != _seat_index) {
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
		table.change_player_status(seat_index,GameConstants.INVALID_VALUE);
		// 放弃操作
		if(playerStatus.has_zi_mo()){
			table._guo_hu[seat_index][0] = 1;
		}

		// 用户状态
//		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			table._playerStatus[i].clean_action();
//			//table._playerStatus[i].clean_status();
//			table.change_player_status(i, GameConstants.INVALID_VALUE);
//			table.operate_player_action(i, true);
//		}
		// 执行动作
		table._pu_count = 1;
		switch (operate_code) {
		case GameConstants.WIK_NULL:
		{
			_send_card_data = 0;
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				//table._playerStatus[i].clean_status();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}
			//add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
//			if(table._guan_sheng_count < table.getTablePlayerNumber())
//			{
//				table._guan_sheng_count++;
//			}
//			else if(table._hua_count < table.getTablePlayerNumber())
//			{
//				table._hua_count++;
//			}
//		
//			_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();	
			table._guo_qi_guan[_seat_index] = true;
			table.exe_dispatch_first_card(_seat_index, GameConstants.WIK_NULL, 450);
//			while (table._guan_sheng_count<table.getTablePlayerNumber()){
//				table.estimate_player_sheng_guan(_seat_index,_send_card_data);
//				table.estimate_player_hua(_seat_index, _send_card_data,false);
//				if(_send_card_data != 0)
//				{
//					int cur_logic_index =  table._logic.switch_to_card_logic_index(_send_card_data);
//					if(table._sheng_guan_index[_seat_index][cur_logic_index] == 1)
//					{
//						table.cannot_outcard(_seat_index, -1, _send_card_data, true);
//					}
//				}
//				
//				if(table._playerStatus[_seat_index].has_action()==false)
//				{
//					table._guan_sheng_count++;
//					_seat_index  = (_seat_index + 1)%table.getTablePlayerNumber();
//				}
//				else{
//					
//					table._current_player = _seat_index ; 
//					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
//					table.operate_player_action(_seat_index, false);
//					table.operate_player_status();
//					return true;
//				}
//
//			};
//			
//			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
//				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), table.GRR._banker_player, _send_card_data),
//						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
//			} else {
//
//				table._current_player = table.GRR._banker_player;
//				_seat_index = table._current_player; 
//				table.change_player_status(table.GRR._banker_player, GameConstants.Player_Status_OUT_CARD);
//		
//				table.operate_player_status();
//			}

			return true;
		}
		case GameConstants.GZP_WIK_GUAN: // 观生操作
		{
			boolean flag = table._playerStatus[_seat_index].is_get_weave_card(GameConstants.GZP_WIK_GUAN,operate_card);
			if(flag == false)
			{
				table.log_error(" 找不到可以操作的牌 _seat_index = "+_seat_index);
				return true;
			}
			table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, GameConstants.GZP_TYPE_GUAN, true,
					false);
			return true;
		}
		case GameConstants.GZP_WIK_HUA:  // 滑操作
		{
			boolean flag = table._playerStatus[_seat_index].is_get_weave_card(GameConstants.GZP_WIK_HUA,operate_card);
			if(flag == false)
			{
				table.log_error(" 找不到可以操作的牌 _seat_index = "+_seat_index);
				return true;
			}
			table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, GameConstants.GZP_TYPE_HUA_MO, true,
					false);
			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

 			table._banker_select = _seat_index;
			table._shang_zhuang_player = _seat_index;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_gzp_ddwf(_seat_index, _seat_index, operate_card, true);

			table.countChiHuTimes(_seat_index, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[_seat_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[_seat_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
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
		if(is_out == true && table._pick_up_index[seat_index][table._logic.switch_to_card_index(card)] == 0)
		{
			table.log_error("出捡牌,牌型出错");
			return false;
		}
		
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_info("状态不对不能出牌");
			return false;
		}
		if(is_out == false && table.GRR._cannot_out_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_info("当前牌不能出");
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
			if(table._pick_up_index[_seat_index][flower_index]>0)
			{
				table._pick_up_index[_seat_index][flower_index]--;
				table.operate_pick_up_card(_seat_index);
			}
			else if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);	
			}
		}
		else if(common_index != -1)
		{
			if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);
			}
			else if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);	
			}
		}
		else {
			if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);	
			}
		}
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		//出牌
		table.exe_out_card(_seat_index,card,GameConstants.WIK_NULL);

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(GZPTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GZP_MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				for(int k = 0; k < table.GRR._weave_items[i][j].weave_card.length;k++)
				{
					weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
				}
				if(seat_index!=i) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			
			if(i == _seat_index&&_send_card_data !=0){
				if(table._pu_count == 2)
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-2);
				else
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			}else{
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
		
		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		//如果断线重连的人是自己
		if(_send_card_data != 0){
			if(seat_index == _seat_index){
				if(table._pu_count == 2)
				{
					table._logic.remove_card_by_data(hand_cards, table._pu_card[0]);
					table._logic.remove_card_by_data(hand_cards, table._pu_card[1]);
				}
				else
					table._logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}
		
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		
		//摸牌
		// 发送数据
		// 只有自己才有数值
		if(_send_card_data != 0){
			if(table.GRR._left_card_count > 3)
				if(table._pu_count == 2)
					table.operate_player_get_card(_seat_index, table._pu_count, table._pu_card, GameConstants.INVALID_SEAT,false,1);
				else
					table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data},seat_index,false,1);
			else
				table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data},seat_index,true,0);
		}
		

		
		if(table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone()==false)){
			table.operate_player_action(seat_index, false,false);
		}
		table.operate_cannot_card(seat_index);
		table.operate_pick_up_card(seat_index);
		table.be_in_room_trustee(seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.operate_player_connect_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
	
		return true;
	}
	
	
	@Override
	public boolean handler_be_set_trustee(GZPTable table, int seat_index){
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
		if(curPlayerStatus.has_action()){
			table.operate_player_action(seat_index, true);
			if(curPlayerStatus.has_zi_mo()){
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
			}else{
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, 0);
			}
		}else if(curPlayerStatus.get_status() == GameConstants.Player_Status_OUT_CARD){	
			int out_card = GameConstants.INVALID_VALUE;			
			int send_index = table._logic.switch_to_card_index(_send_card_data);
			if(send_index != GameConstants.MAX_FLS_INDEX_DP && table.GRR._cards_index[seat_index][send_index] > 0){//手牌有
				out_card = _send_card_data;
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
