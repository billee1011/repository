package com.cai.game.schcpdz.handler.cqydr;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.schcpdz.handler.SCHCPDZHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class SCHCPHandlerGang_YDR extends SCHCPDZHandlerGang<SCHCPDZTable> {

	@Override
	public void exe(SCHCPDZTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._guo_hu_pai_count[_seat_index] = 0;

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		if (_depatch == false)
			table.operate_out_card(this._provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		else
			table.operate_player_get_card(this._provide_player, 0, null, GameConstants.INVALID_SEAT, false);

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);

		this.exe_gang(table);

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
	public boolean handler_operate_card(SCHCPDZTable table, int seat_index, int operate_code, int operate_card,
			int luoCode) {
		// 抢杠胡

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "GXZPHandlerGang_YX出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "GXZPHandlerGang_YX出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "GXZPHandlerGang_YX出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "GXZPHandlerGang_YX出牌操作,操作牌对象出错");
			return false;
		}
		if (playerStatus.has_zi_mo() == true)
			table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
		if (playerStatus.has_chi_hu() == true)
			table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
	
		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		// if (operate_code == GameConstants.WIK_NULL) {
		// table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
		// table._playerStatus[seat_index].chi_hu_round_invalid();//
		// 这一轮就不能吃胡了没过牌之前都不能胡
		// } else if (operate_code == GameConstants.WIK_CHI_HU) {
		// table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
		// table.process_chi_hu_player_operate_hh(seat_index, new int[] {
		// _center_card }, 1, false);// 效果
		// } else {
		// table.log_player_error(seat_index, "HHHandlerGang_YX出牌操作,没有动作");
		// return false;
		// }

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

		if (table._playerStatus[target_player].is_respone() == false) {
			return true;
		}


		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL: {

			if ((table._is_xiang_gong[_seat_index] == false)
					&& (table._long_count[_seat_index] == 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)) {
				// 要出牌，但是没有牌出设置成相公 下家用户发牌
				int pai_count = 0;
				for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
					if (table.GRR._cards_index[_seat_index][i] < 3)
						pai_count += table.GRR._cards_index[_seat_index][i];
				}
				if (pai_count == 0) {
					table._is_xiang_gong[_seat_index] = true;
					table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
					int next_player = table.get_cur_index(_seat_index);
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);

				} else { // 胡牌了不执行
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
					table.log_player_error(_seat_index, "扫和提龙出牌状态");
				}

			} else {
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				int next_player = table.get_cur_index(_seat_index);
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1500);

			}

			return true;

		}
		case GameConstants.WIK_CHI_HU: // 胡
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[target_player][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player, operate_card, true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score_chq_ydr(target_player, this._provide_player, operate_card, false);

			table.countChiHuTimes(target_player, false);
			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
			// if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG))
			// {// 轮装
			// if (table.GRR._banker_player == target_player) {
			// table._banker_select = target_player;
			// } else {
			// table._banker_select = (table.GRR._banker_player +
			// table.getTablePlayerNumber() + 1)
			// % table.getTablePlayerNumber();
			// }
			// }
			if (_depatch == true)
				table.operate_player_get_card(this._provide_player, 1, new int[] { _center_card },
						GameConstants.INVALID_SEAT, false);

			table._shang_zhuang_player = _seat_index;
			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player, operate_card, true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_chq_ydr(target_player, _seat_index, operate_card, true);

			table.countChiHuTimes(_seat_index, true);
			table._cur_banker = target_player;
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

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	protected boolean exe_gang(SCHCPDZTable table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		
		for(int i = 0; i< table.GRR._weave_count[_seat_index];i++){
			if(table.GRR._weave_items[_seat_index][i].weave_kind==GameConstants.DZ_WIK_SHA 
					&& _center_card  == table.GRR._weave_items[_seat_index][i].center_card){
				cbWeaveIndex = i;
				break;
			}
		}
	
		
		table._long_count[_seat_index]++;
		table._ti_mul_long[_seat_index]++;
		Arrays.fill(table._is_sha_index, -1);
		if(_type != GameConstants.DZ_WIK_TU_HUO)
			table._is_sha_index[_seat_index] = table._logic.switch_to_card_index(_center_card);
		if(GameConstants.CHR_DZ_OUT_CARD == _type)
		{
			table.operate_out_card(_provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}
		if (GameConstants.CHR_DZ_ADD_CARD == _type || GameConstants.CHR_DZ_DISPATCH_CARD == _type ) {
			table.operate_player_get_card(_provide_player, 0, null, GameConstants.INVALID_SEAT, false);
		}
		if(_action == GameConstants.DZ_WIK_TU_HUO){
			table._tu_huo_index[_seat_index][table._logic.switch_to_card_index(_center_card)]++;
		}
		else{
			if(cbWeaveIndex == -1)
			{
				cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_count[_seat_index]++;
			}
			table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		
			table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_analyse_tuo_shu(_action,
					_center_card);
			// 设置用户
			table._current_player = _seat_index;
			table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
			table._table_cards[table._logic.switch_to_card_index(_center_card)] += table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_center_card)];
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_center_card)] = 0;
		}

		for(int i = 0; i < table.getTablePlayerNumber();i++)
		{
			if(table._is_bao_zi[i] == true)
				continue;
			if(table._is_system_bao_zi[i] ==true)
				continue;
			if(table._is_yang[i] == true)
				continue;
			if(i == _seat_index)
				continue;
			if(table.is_bao_zi(i)){
				table._is_system_bao_zi[i] = true;
				table.operate_player_xiang_gong_flag(i, true);
				table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.DZ_BAO_ZI }, 1,
						GameConstants.INVALID_SEAT);
			}
		}
		if(table.is_game_end()==false)
			return true;
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		table._playerStatus[_seat_index]._hu_card_count = 0;
		Arrays.fill(table._playerStatus[_seat_index]._hu_cards, 0);

		int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
		int dispatch_time = 3000;
		SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
		if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
			dispatch_time = sysParamModel1105.getVal2();
		}
		table.exe_Dispatch_tou_card_data(_seat_index, GameConstants.WIK_NULL, dispatch_time);
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(SCHCPDZTable table, int seat_index) {
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
		table.istrustee[seat_index] = false;
		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_CP_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (seat_index != i) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

		}
		roomResponse.addDouliuzi(table.caculate_mao_shu(seat_index)); //卯数
		roomResponse.addEspecialShowCards(table.caculate_tuo_shu(seat_index));//砣数
		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;// 将癞子转下
			}
		}

		for (int i = 0; i < GameConstants.DZ_MAX_CP_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		if (table.is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
			table.operate_cannot_card(seat_index, false);
			if (table._xian_ming_zhao[seat_index] == true)
				table.operate_must_out_card(seat_index, false);
		}

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		for(int i = 0; i< table.getTablePlayerNumber();i++){
			table.operate_player_xiang_gong_flag(i, true);
		}
		table.operate_cannot_card(seat_index,false);
		table.operate_must_out_card(seat_index, false);
		table.set_qi_player();
		return true;
	}
}
