/**
 * 
 */
package com.cai.game.schcp.handler.yanzhong;

import java.util.Arrays;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DisplayCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcp.handler.SCHCPHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class SCHCPHandlerDispatchAddCard_YanZhong extends SCHCPHandlerDispatchCard<SCHCPTable_YanZhong> {

	@Override
	public void exe(SCHCPTable_YanZhong table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
//		if (table.GRR._left_card_count == 1) {
//			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
//			}
//			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//					% table.getTablePlayerNumber();
//			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
//			// 流局
//			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
//
//			return;
//		}
//		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;
		table._ti_mul_long[_seat_index] --;
		
		table.operate_player_get_card(_seat_index, 1,  new int[]{_send_card_data}, GameConstants.INVALID_SEAT,true);
		table.operate_effect_action(-1, GameConstants.EFFECT_ACTION_CP, 1, new long[] { GameConstants.CP_WAIT_TOU }, 1,
				GameConstants.INVALID_SEAT);
//		if(table.estimate_player_hua(_seat_index, _send_card_data)!=GameConstants.CP_WIK_NULL){
//
//			table.exe_gang(_seat_index, _seat_index, _send_card_data, GameConstants.CP_WIK_HUA,GameConstants.CP_TYPE_HUA,true, true, false,1000);
//			return;
//		}
		if(table._logic.check_nei_hua(table.GRR._cards_index[_seat_index], _send_card_data) == 0)
			table.estimate_player_zhao(_seat_index, _send_card_data);
		if(_seat_index == table._cur_banker&&table._ti_mul_long[_seat_index] == 0)
    	{
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			chr.set_empty();
    		int action = GameConstants.WIK_NULL;
    	
			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],_seat_index,_seat_index, _send_card_data, chr, card_type,true,false);// 自摸
		
    		if (action != GameConstants.WIK_NULL) {
    			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chr.set_empty();
					chr.opr_or(GameConstants.CHR_TIAN_HU);
				}
    			// 添加动作
    			table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
    			table._playerStatus[_seat_index].add_zi_mo(table._send_card_data, _seat_index);
    			GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,table._send_card_data),
    					600, TimeUnit.MILLISECONDS);
    			//发 操作
    			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
    			table.operate_player_action(_seat_index,false);
    			return ;

    		}
    		else{
    			chr.set_empty();	
    		}	
    	}
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		if (curPlayerStatus.has_action() ) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.isTrutess(_seat_index)) {
				
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			if(table.is_zhao_card(_seat_index, _send_card_data)==true)
				table._zhao_card[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
			// 刷新自己手牌
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
			table.must_out_card();
			int s_cards[] = new int[3];
			int count = table._logic.switch_to_value_to_card(7,s_cards);
			for(int i = 0; i<count ;i++)
			{
				if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
				{
					table.cannot_outcard(_seat_index, s_cards[i],1, true);
				}
			}
			table.exe_chuli_first_card(_seat_index,GameConstants.WIK_NULL,1000);
			
		}
    
		return;
	}
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(SCHCPTable_YanZhong table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(seat_index,card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != table._cur_banker) {
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[table._cur_banker].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI状态不对不能出牌");
			return false;
		}
		if (table.GRR._cards_index[table._cur_banker][table._logic.switch_to_card_index(card)] >= 3) {
			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			// 显示出牌
			table.operate_out_card(table._cur_banker, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[table._cur_banker], cards);
			table.operate_player_cards(table._cur_banker, hand_card_count, cards, table.GRR._weave_count[table._cur_banker], table.GRR._weave_items[table._cur_banker]);

			
			return true;
		}
		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[table._cur_banker], card) == false) {
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI出牌删除出错");
			return false;
		}

		// 出牌
		table.exe_out_card(table._cur_banker, card, GameConstants.WIK_NULL);

		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	public boolean handler_operate_card(SCHCPTable_YanZhong table,int seat_index, int operate_code, int operate_card,int luoCode){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		if(operate_code != GameConstants.CP_WIK_ZI_MO){
			if(playerStatus.has_zi_mo() == true)
				table.cannot_hu_card(seat_index,operate_card);
		}
	
		//if (seat_index != _seat_index) {
		//	table.log_error("DispatchCard 不是当前玩家操作");
		//	return false;
		//}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}



		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			return true;
		}
	
		// 执行动作
		switch (target_action) {
		case GameConstants.CP_WIK_NULL:
		{
			// 用户状态
			if(table._playerStatus[_seat_index].has_cp_zhao())
			{
				if(table.is_zhao_card(_seat_index, _send_card_data)==true)
					table._zhao_card[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
	
				// 刷新自己手牌
//				int cards[] = new int[GameConstants.MAX_CP_COUNT];
//				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
//				table.must_out_card();
//				int s_cards[] = new int[3];
//				int count = table._logic.switch_to_value_to_card(7,s_cards);
//				for(int i = 0; i<count ;i++)
//				{
//					if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
//					{
//						table.cannot_outcard(_seat_index, s_cards[i],1, true);
//					}
//				}
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
			}
		
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//要出牌，但是没有牌出设置成相公  下家用户发牌
				GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,false),
						800, TimeUnit.MILLISECONDS);
//				table.exe_chuli_first_card(_seat_index,GameConstants.WIK_NULL,1000);
			}
			return true;

		}
		case GameConstants.CP_WIK_ZHAO:
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
				}
			table.operate_player_get_card(_seat_index, 1,  new int[]{_send_card_data}, GameConstants.INVALID_SEAT,false);
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
			table._zhao_guo_card[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

			// 刷新自己手牌
//			int cards[] = new int[GameConstants.MAX_CP_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
//			table.operate_player_get_card(_seat_index, 0,  null, GameConstants.INVALID_SEAT,false);
//			table.must_out_card();
//			int chi_peng_cards[] = new int[3];
//			int chi_peng_count = table._logic.switch_to_value_to_card(table._logic.get_dot(_send_card_data), chi_peng_cards);
//			for(int i = 0; i < chi_peng_count; i++){
//				table._cannot_chi_index[_seat_index][table._logic.switch_to_card_index(chi_peng_cards[i])] = 0;
//				table._cannot_peng_index[_seat_index][table._logic.switch_to_card_index(chi_peng_cards[i])] = 0;
//			}
//			int s_cards[] = new int[3];
//			int count = table._logic.switch_to_value_to_card(7,s_cards);
//			for(int i = 0; i<count ;i++)
//			{
//				if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
//				{
//					table.cannot_outcard(_seat_index, s_cards[i],1, true);
//				}
//			}
//
//			table.exe_chuli_first_card(_seat_index,GameConstants.WIK_NULL,1000);
			GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,false),
					800, TimeUnit.MILLISECONDS);
			_send_card_data = 0;
			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
				}
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;


//			table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_schcp(target_player, _seat_index, operate_card, true);
		
			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(SCHCPTable_YanZhong table, int seat_index) {
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
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

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
			for (int j = 0; j < GameConstants.MAX_CP_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.CP_WIK_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.CP_WIK_KAN)
							&& table._is_display == false) {
						weaveItem_item.setCenterCard(0);
						weaveItem_item.setHuXi(0);
					} else {
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._hu_xi[i]);

			// 牌

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_CP_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		boolean flag = false;
		if (_send_card_data != 0)
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		// 如果断线重连的人是自己
		// if(seat_index == _seat_index){
		// if(!((seat_index == table._current_player) && (_send_card_data ==
		// 0)))
		// table._logic.remove_card_by_data(hand_cards, _send_card_data);
		// }

		for(int i = 0; i < table.getTablePlayerNumber();i++)
		{
			int zhao_count = 0;
			for(int j = 0; j <GameConstants.MAX_CP_INDEX;j++ ){
				if(table.GRR._cards_index[i][j] != 0)
					zhao_count += table._zhao_card[i][j];
			}
			tableResponse.addCardsDataNiao(zhao_count);
		}
		int cards_index[] = new int[GameConstants.MAX_CP_INDEX];
		Arrays.fill(cards_index, 0);
		for(int i = 0; i<GameConstants.MAX_CP_INDEX;i++)
		{
			cards_index[i] = table._zhao_card[seat_index][i];
		}
		// 数据
		tableResponse.setSendCardData(0);
		for (int i = 0; i < hand_card_count; i++) {
			if(cards_index[table._logic.switch_to_card_index(hand_cards[i])]>0)
			{
				cards_index[table._logic.switch_to_card_index(hand_cards[i])]--;
				hand_cards[i]|=0x100;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 摸牌
		boolean is_zhao = false;
		for(int i = 0; i< table.getTablePlayerNumber();i++)
		{
			if(table._playerStatus[i].has_cp_zhao())
			{
				is_zhao = true;
				break;
			}
		}
		if((_send_card_data != 0)&&(is_zhao==true)){
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, true);

		}
		else if ((_send_card_data != 0) && (flag == false))
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);
		table.operate_cannot_card(seat_index,false);
		table.operate_must_out_card(seat_index, false);
		table.istrustee[seat_index] = false;
	
		return true;

	}
}
