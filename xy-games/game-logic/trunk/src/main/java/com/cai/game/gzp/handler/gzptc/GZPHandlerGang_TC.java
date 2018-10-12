package com.cai.game.gzp.handler.gzptc;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.gzp.GZPTable;
import com.cai.game.gzp.handler.GZPHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class GZPHandlerGang_TC extends GZPHandlerGang {

	@Override
	public void exe(GZPTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			//table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);

		
		this.exe_gang(table);
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
		// 抢杠胡

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "FLSHandlerGang_LX出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "FLSHandlerGang_LX出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "FLSHandlerGang_LX出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "FLSHandlerGang_LX出牌操作,操作牌对象出错");
			return false;
		}
		Arrays.fill(table._guo_hu[seat_index], 0);
		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			
			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
		} else {
			table.log_player_error(seat_index, "FLSHandlerGang_LX出牌操作,没有动作");
			return false;
		}

		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		// 执行判断
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
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// int cbTargetActionRank =
				// table._logic.get_action_rank(target_action) +
				// target_p;//table._playerStatus[target_player].get_perform()

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
			return true;

		// 选择了不胡
		if (target_action == GameConstants.WIK_NULL) {
			//add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
			// 发牌给杠的玩家
			this.exe_gang(table);
			return true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player) {
				table.GRR._chi_hu_rights[i].set_valid(true);
			} else {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
		}
		table.process_chi_hu_player_operate(target_player, target_action, false);

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			//table._playerStatus[i].clean_status();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {// (i ==
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			table._banker_select = target_player;
			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
				if(table.GRR._banker_player==target_player) {
					table._banker_select = target_player;
				}else {
					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
							% table.getTablePlayerNumber();
				}
			}
			table._shang_zhuang_player = target_player;
			
			
			table.GRR._chi_hu_card[target_player][0] = _center_card;

			table.process_chi_hu_player_score_gzp(target_player, _seat_index, _center_card, false);


			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay+=table.GRR._chi_hu_rights[target_player].type_count-1;
			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

		} else {
			// 选择了不胡
			// 发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	protected boolean exe_gang(GZPTable table) {
		Arrays.fill(table._temp_guan[_seat_index], 0);
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		int cbRemoveCard[] = new int[5];
		int card_count = 5;
		int[] copy=	Arrays.copyOf(cbRemoveCard, card_count);
		 
		if (GameConstants.GZP_TYPE_HUA_MO == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			card_count = 5;
			table._logic.get_remove_cards(table.GRR._cards_index[_seat_index], table._pick_up_index[_seat_index], cbRemoveCard, card_count, _center_card);
			int common_index = table._logic.switch_to_card_common_index(_center_card);
			if(common_index == -1)
				common_index = table._logic.switch_to_card_index(_center_card);
			if(table._sheng_guan_index[_seat_index][common_index]>0)
				table._sheng_guan_index[_seat_index][common_index]--;
			else 
				table._pu_count = 2;
			boolean is_error =table._logic.remove_cards_by_index(table.GRR._cards_index[_seat_index], cbRemoveCard, card_count);
			if (!is_error) {
				table.log_player_error(_seat_index, "滑牌出错GZP_TYPE_HUA_MO "+_center_card);
				return false;
			}
			

		} else if (GameConstants.GZP_TYPE_ZHAO == _type) {
			// 别人打的牌

			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			card_count = 3;
			table._logic.get_remove_cards(table.GRR._cards_index[_seat_index], table._pick_up_index[_seat_index], cbRemoveCard, card_count, _center_card);
			boolean is_error =table._logic.remove_cards_by_index(table.GRR._cards_index[_seat_index], cbRemoveCard, card_count);
			if (!is_error) {
				table.log_player_error(_seat_index, "招牌出错");
				return false;
			}
			
			int flower_index = table._logic.switch_to_card_flower_index(_center_card);
			int common_index = table._logic.switch_to_card_common_index(_center_card);
			table._temp_guan[_seat_index][table._logic.switch_to_card_index(_center_card)]++;

			if(flower_index != -1)
				table._temp_guan[_seat_index][flower_index] ++;
		    if(common_index != -1)
		    	table._temp_guan[_seat_index][common_index] ++;	
			cbRemoveCard[3] = _center_card;
			copy[card_count]=_center_card;

		} else if (GameConstants.GZP_TYPE_HUA_OUT == _type) {
			// 别人打的牌

			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player],_seat_index);
			int common_index = table._logic.switch_to_card_common_index(_center_card);
			if(common_index == -1)
				common_index = table._logic.switch_to_card_index(_center_card);
			if(table._sheng_guan_index[_seat_index][common_index]>0)
				table._sheng_guan_index[_seat_index][common_index]--;
			else 
				table._pu_count = 2;
			card_count = 4;
			table._logic.get_remove_cards(table.GRR._cards_index[_seat_index], table._pick_up_index[_seat_index], cbRemoveCard, card_count, _center_card);
			boolean is_error =table._logic.remove_cards_by_index(table.GRR._cards_index[_seat_index], cbRemoveCard, card_count);
			if (!is_error) {
				table.log_player_error(_seat_index, "滑出出错 GZP_TYPE_HUA_OUT"+_center_card);
				return false;
			}
			cbRemoveCard[4] = _center_card;
			copy[card_count]=_center_card;
		} else if(GameConstants.GZP_TYPE_ZHAO_HUA == _type){
			cbWeaveIndex = table.GRR._weave_count[_seat_index]-1;
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "滑牌出错");
				return false;
			}
			card_count = 1;
			table._logic.get_remove_cards(table.GRR._cards_index[_seat_index], table._pick_up_index[_seat_index], cbRemoveCard, card_count, _center_card);
			boolean is_error =table._logic.remove_cards_by_index(table.GRR._cards_index[_seat_index], cbRemoveCard, card_count);
			if (!is_error) {
				table.log_player_error(_seat_index, "滑出出错 GZP_TYPE_ZHAO_HUA"+_center_card);
				return false;
			}
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[4] = _center_card;
            table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi =  table._logic.get_weave_items_gzp(table.GRR._weave_items[_seat_index],table.GRR._weave_count[_seat_index]);
			
			// 设置用户
			table._current_player = _seat_index;
			// 删除手上的牌
			table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
			// 刷新手牌包括组合
			int cards[] = new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);	
		}
		if(GameConstants.GZP_TYPE_GUAN != _type&&GameConstants.GZP_TYPE_ZHAO_HUA != _type  )
		{
			table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card = copy;
			for(int i = 0; i<card_count; i++)
			{
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[i] = cbRemoveCard[i];
			}
			table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi =  table._logic.get_weave_items_gzp(table.GRR._weave_items[_seat_index],table.GRR._weave_count[_seat_index]);
			
			// 设置用户
			table._current_player = _seat_index;
	
			// 删除手上的牌
			table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
			// 刷新手牌包括组合
			int cards[] = new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
		}
		else if(GameConstants.GZP_TYPE_GUAN == _type)
		{
			table._sheng_guan_index[_seat_index][table._logic.switch_to_card_index(_center_card)]++;
			int common_index = table._logic.switch_to_card_common_index(_center_card);
			if(common_index == -1)
				common_index = table._logic.switch_to_card_index(_center_card);
			int flower_index = table._logic.switch_to_card_flower_index(_center_card);
			int card_index = table.GRR._cards_index[_seat_index][common_index];
			if(flower_index != -1)
				card_index += table.GRR._cards_index[_seat_index][flower_index];
			if(card_index==4)
				table.cannot_outcard(_seat_index, 1, _center_card, true);
			
			
			// 刷新手牌包括组合
			int cards[] = new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
		}
		if(table._guan_sheng_count < table.getTablePlayerNumber())
		{
			table.exe_dispatch_first_card(_seat_index, GameConstants.WIK_NULL, 450);
		}
		else if(table._hua_count < table.getTablePlayerNumber()){
			table.exe_dispatch_first_card(_seat_index, GameConstants.WIK_NULL, 450);
		}
		else
		{
			table.exe_dispatch_card(_seat_index, GameConstants.WIK_NULL, 450);
		}
		


		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(GZPTable table,int seat_index) {
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
		table.istrustee[seat_index]=false;
		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
					//癞子
					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_HUN);
				}else{
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GZP_MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(seat_index!=i) {
					if(( table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_ZHAO) &&table.GRR._weave_items[i][j].public_card==0) {
						weaveItem_item.setCenterCard(0);
					}else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				for(int k = 0; k < table.GRR._weave_items[i][j].weave_card.length;k++)
				{
					weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			
		}

		
		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,seat_index);
		
		
		if(table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone()==false){
			table.operate_player_action(seat_index, false,false);
		}
		table.be_in_room_trustee(seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		table.operate_cannot_card(seat_index);
		table.operate_pick_up_card(seat_index);
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.operate_player_connect_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
	
		return true;
	}
}
