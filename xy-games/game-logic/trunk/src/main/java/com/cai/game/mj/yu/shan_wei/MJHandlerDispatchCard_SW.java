package com.cai.game.mj.yu.shan_wei;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.constant.game.GameConstants_SQ;
import com.cai.common.constant.game.GameConstants_SW;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_SW extends MJHandlerDispatchCard<Table_SW> {

	private boolean ting_send_card;

	private boolean flag;

	// 发牌
	@Override
	public void exe(Table_SW table) {
		// 用户状态 清除
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		// 荒庄结束
		if (table.GRR._left_card_count == table.getCsDingNiaoNum()) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._cur_banker = (table.GRR._banker_player + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			table._lian_zhuang_player = table._cur_banker;

			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		if (table.DEBUG_CARDS_MODE || table.BACK_DEBUG_CARDS_MODE) {
			_send_card_data = 0x22;
		}

		table._provide_player = _seat_index;

		// 是否进行了亮牌补张
		// flag = false;
		// if (_type == GameConstants_SQ.DISPATCHCARD_TYPE_BU_HUA) {
		// int sendType = 3;
		// // 显示补花的牌
		// if (table.has_rule(GameConstants_SQ.GAME_RULE_SQ_LIANG_SI_DA_YI)) {
		// if (table.GRR.change_liang_bu_pai(card, _send_card_data,
		// _seat_index)) {
		// flag = true;
		// }
		// }
		// table.operate_show_card_other(_seat_index, sendType);
		// }
		//
		// // 补牌
		// if (table._logic.is_hua_card(_send_card_data)) {
		//
		// table.GRR.addHuaCard(_seat_index, _send_card_data);
		// // 发送数据
		// // 只有自己才有数值
		// int real_card = _send_card_data;
		// if (flag) {
		// real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
		// }
		//
		// table.operate_player_get_card(_seat_index, 1, new int[] { real_card
		// }, GameConstants.INVALID_SEAT);
		//
		// // 效果
		// table.operate_effect_action(_seat_index,
		// GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] {
		// GameConstants.WIK_BU_HUA }, 1,
		// GameConstants.INVALID_SEAT);
		//
		// table.GRR._chi_hu_rights[_seat_index].hua_count++;// 花牌加一
		//
		// // 补花数量
		// table.bu_hua_count++;
		//
		// // 发牌
		// table.exe_dispatch_card(_seat_index,
		// GameConstants_SQ.DISPATCHCARD_TYPE_BU_HUA, _send_card_data, 1000);
		// return;
		//
		// } else if
		// (table._logic.is_hua_card_array(table.GRR._cards_index[_seat_index]))
		// {
		// // 加到手牌
		// table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		//
		// // 获取花牌
		// int index =
		// table._logic.rt_Hua_Index(table.GRR._cards_index[_seat_index]);
		// if (index == GameConstants.INVALID_SEAT) {
		// table.log_error("商丘麻将补牌出错");
		// return;
		// }
		// int card = table._logic.switch_to_card_data(index);
		// // 删除花牌
		// table.GRR.addHuaCard(_seat_index, card);
		// table.GRR._cards_index[_seat_index][index]--;
		//
		// // 刷新手牌
		// int cards[] = new int[GameConstants.MAX_COUNT];
		// // 刷新自己手牌
		// int hand_card_count =
		// table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],
		// cards);
		// table.operate_player_cards(_seat_index, hand_card_count, cards, 0,
		// null, _send_card_data);
		//
		// // 效果
		// table.operate_effect_action(_seat_index,
		// GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] {
		// GameConstants.WIK_BU_HUA }, 1,
		// GameConstants.INVALID_SEAT);
		//
		// table.GRR._chi_hu_rights[_seat_index].hua_count++;// 花牌加一
		//
		// // 补花数量
		// table.bu_hua_count++;
		//
		// // 发牌
		// table.exe_dispatch_card(_seat_index,
		// GameConstants_SQ.DISPATCHCARD_TYPE_BU_HUA, card, 0);
		// return;
		// }

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants_SW.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG) {
			card_type = GameConstants_SW.HU_CARD_TYPE_GANG_KAI_HUA;
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants_HZLZG.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants_HZLZG.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			if (table._logic.is_magic_index(i))
				continue;

			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

					ting_count++;

					if (send_card_index == i) {
						ting_send_card = true;
					}
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants_HZLZG.INVALID_SEAT);

		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.GRR._cards_abandoned_gang[_seat_index]);

			if (cbActionMask != GameConstants_HZLZG.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants_HZLZG.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return;
	}

	@Override
	public boolean handler_operate_card(Table_SW table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants_HZLZG.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants_HZLZG.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			return true;
		}
		
		

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					if (m_gangCardResult.cbCardData[i] != _send_card_data) {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					} else {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, true);
					}
					return true;
				}
			}

		}
		case GameConstants.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table._cur_banker = seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;

			table.set_niao_card(_seat_index, 0, true, 0);

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_SW table, int seat_index) {
		// 重连初始化花牌亮牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.operate_show_card_other(i, GameConstants_SQ.SEND_OTHER_TYPE3);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		// if (_type != Constants_HuangZhou.LIANG_LAI_ZI) {
		// int real_card = _send_card_data;
		// if (table._logic.is_magic_card(_send_card_data)) {
		// real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		// } else if (ting_send_card) {
		// real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		// }
		//
		// table.operate_player_get_card(_seat_index, 1, new int[] { real_card
		// }, seat_index);
		// }
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		int show_send_card = _send_card_data;
		if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
