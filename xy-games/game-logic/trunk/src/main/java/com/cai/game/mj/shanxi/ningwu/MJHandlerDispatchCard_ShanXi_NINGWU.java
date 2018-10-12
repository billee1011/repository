package com.cai.game.mj.shanxi.ningwu;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_SXJY;
import com.cai.common.constant.game.mj.GameConstants_SXNW;
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

public class MJHandlerDispatchCard_ShanXi_NINGWU extends MJHandlerDispatchCard<MJTable_ShanXi_NINGWU> {
	boolean can_bao_ting = false;
	boolean ting_send_card = false;

	// 发牌
	@Override
	public void exe(MJTable_ShanXi_NINGWU table) {
		// 用户状态 清除
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();
		table._playerStatus[_seat_index].clear_cards_abandoned_feng_gang();

		if (table.has_rule(GameConstants_SXNW.GAME_RULE_SHANXI_LAO_PAI)) {
			if (table.GRR._left_card_count <= (table.all_player_gang_count % 2 + 12)
					&& !table.is_thirteen_and_only_one_gang) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				// 流局不换庄
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

				return;
			}
		} else {
			if (table.GRR._left_card_count <= 0) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				// 流局不换庄
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

				return;
			}
		}
		// 过完这个状态就要重置了，这个状态只用于
		table.is_thirteen_and_only_one_gang = false;

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x18;
		}
		table._provide_player = _seat_index;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int action = GameConstants.WIK_NULL;
		// 胡牌检测
		if (table._playerStatus[_seat_index].is_bao_ting()) {
			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, GameConstants_SXNW.HU_CARD_TYPE_ZI_MO,
					_seat_index, false);
		}
		if (GameConstants.WIK_NULL != action && table._playerStatus[_seat_index].is_bao_ting()) {
			if (table.has_rule(GameConstants_SXNW.GAME_RULE_SHANXI_NENG_HU_BI_HU)) {
				curPlayerStatus.add_action(GameConstants_SXJY.WIK_HIDE_ACTION);
			}
			// 如果发的这张牌能胡牌，当前玩家状态，添加相应的动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			chr.set_empty();
		}
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin

		if (table._playerStatus[_seat_index].is_bao_ting() == true) {
		} else {
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
							table._playerStatus[_seat_index]._hu_out_cards[ting_count],
							table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic
								.switch_to_card_data(i);

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
				can_bao_ting = true;
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
				// 判断能不能添加报听的操作
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);

			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		}

		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;// 如果有报听提示
		}
		// 客户端显示玩家抓牌
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		// 当前牌桌记录提供的牌
		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table.analyse_gang_exclude_magic_card(table.GRR._cards_index[_seat_index],
					_send_card_data, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
					m_gangCardResult, table._playerStatus[_seat_index].get_cards_abandoned_gang(),
					table._playerStatus[_seat_index]._cards_feng_gang_count, _seat_index);

			if (table._playerStatus[_seat_index].is_bao_ting()) {
				if (0 != cbActionMask) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
						int tmp_card_count = table.GRR._cards_index[_seat_index][tmp_card_index];
						int tmp_weave_count = table.GRR._weave_count[_seat_index];

						// 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
						// 发牌时，杠牌只要碰杠和暗杠这两种
						table.GRR._cards_index[_seat_index][tmp_card_index] = 0;

						if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[i]) {
							table.GRR._weave_items[_seat_index][tmp_weave_count].public_card = 0;
							table.GRR._weave_items[_seat_index][tmp_weave_count].center_card = m_gangCardResult.cbCardData[i];
							table.GRR._weave_items[_seat_index][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
							table.GRR._weave_items[_seat_index][tmp_weave_count].provide_player = _seat_index;
							++table.GRR._weave_count[_seat_index];
						}

						boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

						table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
						table.GRR._weave_count[_seat_index] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							curPlayerStatus.add_action(GameConstants.WIK_GANG);
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
									m_gangCardResult.isPublic[i]);
						}
					}
				}
			} else {
				if (0 != cbActionMask) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
								m_gangCardResult.isPublic[i]);
					}
				}
			}
		}

		// 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
		if (curPlayerStatus.has_action()) { // 有动作
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), table.delay,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

	}

	@Override
	public boolean handler_operate_card(MJTable_ShanXi_NINGWU table, int seat_index, int operate_code,
			int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
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

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			if (can_bao_ting) {
				table.operate_player_cards_flag(_seat_index, 0, null, 0, null);
			}
			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					if (m_gangCardResult.cbCardData[i] != _send_card_data) {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i],
								true, false);
					} else {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i],
								true, true);
					}
					return true;
				}
			}
			return true;
		}
		case GameConstants.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);
			// 如果胡牌的人不是庄家，
			if (table._cur_banker != seat_index) {
				table._cur_banker = (table._cur_banker + (table.getTablePlayerNumber() + 1))
						% table.getTablePlayerNumber(); // 若闲家胡牌则庄家的下家坐庄
			} else {
				table._cur_banker = seat_index;// 庄家胡牌则继续坐庄
			}

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true, false);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_BAO_TING: // 报听
		{
			operate_card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			// 效验参数
			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShanXi_NINGWU table, int seat_index) {
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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
						if (!table._playerStatus[seat_index].is_bao_ting()) {
							hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						}
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
			roomResponse.addOutCardTing(
					table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].is_bao_ting()) {
				table._playerStatus[i].set_card_status(GameConstants.CARD_STATUS_BAO_TING);
			}
		}

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

		// 癞子
		int real_card = _send_card_data;
		if (!table._playerStatus[seat_index].is_bao_ting()) {
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

}
