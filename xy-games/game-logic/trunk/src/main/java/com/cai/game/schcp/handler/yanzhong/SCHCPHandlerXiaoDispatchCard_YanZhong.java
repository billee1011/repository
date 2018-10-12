package com.cai.game.schcp.handler.yanzhong;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DisplayCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcp.handler.SCHCPHandlerDispatchCard;

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
public class SCHCPHandlerXiaoDispatchCard_YanZhong extends SCHCPHandlerDispatchCard<SCHCPTable_YanZhong> {

	@Override
	public void exe(SCHCPTable_YanZhong table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

//		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 1) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			// 显示胡牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
						table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

			}
//			table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		
	
		table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
				table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			table._ting_card[_seat_index] = true;
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}
		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		 table._last_card = _send_card_data;
//		 if(table.DEBUG_CARDS_MODE==true)
//			 _send_card_data = 0x09;
		 table._send_card_data = _send_card_data;
		//用户是否可以提扫
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,true);
	
		ChiHuRight	chr = table.GRR._chi_hu_rights[_seat_index];
		
			
		if(table.estimate_player_hua(_seat_index, _send_card_data) != GameConstants.CP_WIK_NULL )
			return;
		int is_hu = 0;
		int action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],_seat_index,_seat_index,_send_card_data, chr, card_type,true,false);// 自摸
		if (action_hu != GameConstants.WIK_NULL) {
			
			table._playerStatus[_seat_index].add_action(GameConstants.CP_WIK_ZI_MO);
			table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);
			table._playerStatus[_seat_index].add_action(GameConstants.CP_WIK_NULL);
			table._playerStatus[_seat_index].add_pass(_send_card_data, _seat_index);
			is_hu = 1;
		
		}
		else {
			chr.set_empty();
		}	
	



		if (table.isTrutess(_seat_index)) {
			
			GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
					GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
			return;
		}
		if (curPlayerStatus.has_action() ) {// 有动作
			if(curPlayerStatus.has_action_by_code(GameConstants.CP_WIK_HUA) )
			{
				curPlayerStatus.delete_action(GameConstants.CP_WIK_NULL);
			}
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}

			table.operate_player_action(_seat_index, false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_status();
			return ;
			
		}
//		table.operate_player_get_card(_seat_index, 0,  null, GameConstants.INVALID_SEAT,true);
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		int temp_must_count = 0;
		if(table._playerStatus[_seat_index]._hu_card_count != 0&&is_hu==0){
			int dot = table._logic.get_dot(_send_card_data);
			int must_cards [] = new int[3];
			int must_count = table._logic.switch_to_value_to_card(dot, must_cards);
			for(int i = 0; i<must_count ; i++){
				if(table.GRR._cannot_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]!=0)
					continue;
				if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]!=0)
				{
					if(table.GRR._must_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])] == 0)
						table.GRR._must_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]++;
					temp_must_count ++;
				}
			}			
			if(temp_must_count >0)
			{
				table.operate_must_out_card(_seat_index, true);
				if(table.check_out_card(_seat_index)==false)
				{
					table.no_card_out_game_end(_seat_index, 0);
					return  ;
				}
//				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
//				table.operate_player_status();
				GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,true),
						800, TimeUnit.MILLISECONDS);
				_send_card_data = 0;
				return ;
			}
			else{
				if(table.check_out_card(_seat_index)==false)
				{
					table.no_card_out_game_end(_seat_index, 0);
					return  ;
				}
				table.no_card_out_game_end(_seat_index, _send_card_data);
				return ;
				
			}
			
		}
//		table.must_out_card();
//		int cards[] = new int[GameConstants.MAX_CP_COUNT];
//		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
//		
//		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
//		table.operate_player_status();
		GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,true),
				800, TimeUnit.MILLISECONDS);
		_send_card_data = 0;
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
	public boolean handler_operate_card(SCHCPTable_YanZhong table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
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
		if(operate_card != this._send_card_data)
		{
			table.log_player_error(seat_index,"DispatchCard 操作牌，与当前牌不一样 operate_card :"+operate_card+ "this._send_card_data= "+this._send_card_data);
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code =luoCode;
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
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			return true;
		}
	
		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
		//判断可不可以吃的上家用户
		int eat_type = GameConstants.CP_WIK_LEFT | GameConstants.CP_WIK_CENTER | GameConstants.CP_WIK_RIGHT;
		
		if(target_action == GameConstants.WIK_NULL)
		{
			// 显示出牌
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			//过张的牌都不可以吃
			//过张的牌都不可以吃
			table.cannot_chicard(_seat_index, _send_card_data);
			table.cannot_chicard(next_player, _send_card_data);
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if(table._playerStatus[i]._action[j] == GameConstants.WIK_PENG)
					{
						table.cannot_pengcard(i,_send_card_data);
					}
				}
			}
		}
		else if( (target_action & eat_type) != GameConstants.WIK_NULL)
		{
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if(table._playerStatus[i]._action[j] == GameConstants.WIK_PENG)
					{
						table.cannot_pengcard(i,_send_card_data);
					}
				}
			}
			if(_seat_index != target_player)
				table.cannot_chicard(_seat_index, _send_card_data);
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {	
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {

		case GameConstants.CP_WIK_NULL:
		{
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
		
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				
				// 显示胡牌
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.MAX_CP_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

				}
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {

				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
			
				int dispatch_time = 500;
				int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
				SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
						.get(1105);
				if (sysParamModel1105 != null && sysParamModel1105.getVal3() > 0 && sysParamModel1105.getVal3() < 10000) {
					dispatch_time = sysParamModel1105.getVal3();
				}

				table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
						table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				table.exe_add_discard(_seat_index, 1, new int[] { _send_card_data  }, true, 0);
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				
				table._current_player = next_player;
				_seat_index = next_player;
				table._last_player = next_player;
				//没有人要就加入到牌堆
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
			    table._last_card = _send_card_data;
			}
			return true;

		}
		case GameConstants.CP_WIK_LEFT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[1];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.CP_WIK_LEFT);
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 1)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			
			
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.CP_WIK_RIGHT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[1];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.CP_WIK_RIGHT);

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 1)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
		
		
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.CP_WIK_CENTER: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[1];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.CP_WIK_CENTER);
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 1)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
	 	
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		
		case GameConstants.CP_WIK_CHE: // 碰牌操作
		{
			// 删除扑克
			table.exe_gang(target_player, _seat_index, _send_card_data, GameConstants.CP_WIK_CHE,GameConstants.CP_TYPE_DISPATCH_CHE,true, true, false,1000);
//			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
//					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;
//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_score_schcp(target_player, _seat_index, operate_card, true);
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			
		
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
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}

}
