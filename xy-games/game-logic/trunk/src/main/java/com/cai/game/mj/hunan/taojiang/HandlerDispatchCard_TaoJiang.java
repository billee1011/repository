package com.cai.game.mj.hunan.taojiang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
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

public class HandlerDispatchCard_TaoJiang extends MJHandlerDispatchCard<Table_TaoJiang> {
	boolean ting_send_card = false;

	@Override
	public boolean handler_player_out_card(Table_TaoJiang table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			table.exe_out_card(_seat_index, card, GameConstants.HU_CARD_TYPE_GANG_KAI);
		} else {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_TaoJiang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		// 正常抓牌之后，都算过圈了
		table.score_when_abandoned_jie_pao[_seat_index] = 0;

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.can_qiang_gang[_seat_index] = true;

		// 剩余8张时，荒庄结束
		if (table.GRR._left_card_count <= Constants_TaoJiang.CARDS_LEFT_DRAW_FENCE) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			// 最后摸牌的是下一局的庄家
			table._cur_banker = table._last_dispatch_player;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.operate_player_cards(i, 0, null, 0, null);

				int hand_cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);

				for (int j = 0; j < hand_card_count; j++) {
					if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
					} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
					}
				}

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, hand_cards, GameConstants.INVALID_SEAT);
			}

			// 流局
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW), table.DELAY_GAME_FINISH,
					TimeUnit.MILLISECONDS);

			return;
		}

		PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
		currentPlayerStatus.reset();

		if (_type != GameConstants.WIK_BAO_TING) {
			++table._send_card_count;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			--table.GRR._left_card_count;

			if (table.distance_to_ding_wang_card > 0) {
				table.distance_to_ding_wang_card = table.GRR._left_card_count - (table.tou_zi_dian_shu[0] + table.tou_zi_dian_shu[1]) * 2;
			}

			table.mo_pai_count[_seat_index]++;

			if (table.DEBUG_CARDS_MODE) {
				_send_card_data = 0x24;
			}

			table._send_card_data = _send_card_data;
		}

		table._current_player = _seat_index;
		table._provide_player = _seat_index;

		table.seat_index_when_win = _seat_index;

		table._last_dispatch_player = _seat_index;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		// 自摸时的胡牌检测
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], table._send_card_data, chr, Constants_TaoJiang.HU_CARD_TYPE_ZI_MO, _seat_index);

		if (GameConstants.WIK_NULL != action) {
			currentPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			currentPlayerStatus.add_zi_mo(table._send_card_data, _seat_index);
		} else {
			chr.set_empty();
		}

		// 加到手牌
		if (_type != GameConstants.WIK_BAO_TING) {
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(table._send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI;

		for (int i = 0; i < card_type_count; i++) {
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
						if (cards[i] == table.joker_card_1 || cards[i] == table.joker_card_2) {
							cards[i] = cards[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
							break;
						} else if (cards[i] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
							cards[i] = cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
							break;
						} else {
							cards[i] = cards[i] + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
							break;
						}
					}
				}
				if (cards[i] == table.joker_card_1 || cards[i] == table.joker_card_2 && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (cards[i] == table.ding_wang_card && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING
						&& table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 处理王牌
		if (_type != GameConstants.WIK_BAO_TING || ting_count > 0) {
			int real_card = table._send_card_data;
			if (ting_send_card) {
				if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
				} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
				} else {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
				}
			} else if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}

			// 客户端显示玩家抓牌
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		table._provide_card = table._send_card_data;

		// 湖南桃江麻将，只要抓牌之后，并且杠之后，不影响听牌就可以了。和接杠还是有区别的
		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true);

			if (0 != cbActionMask) {
				boolean flag = false;
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

					boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index);

					// 还原手牌数据和落地牌数据
					table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
					table.GRR._weave_count[_seat_index] = tmp_weave_count;

					// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
					if (is_ting_state_after_gang) {
						currentPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						flag = true;
					}
				}
				if (flag) { // 如果能杠，当前用户状态加上杠牌动作
					currentPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}
		}

		if (table.is_match() || table.isClubMatch() || table.isCoinRoom()) {
			// 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
			if (currentPlayerStatus.has_action()) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		} else {
			if (table.is_bao_ting[_seat_index] || (table.istrustee[_seat_index] && table.is_gang_tuo_guan[_seat_index])) {
				if (currentPlayerStatus.has_zi_mo() || currentPlayerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
					table.operate_player_action(_seat_index, false);
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				} else {
					table.operate_player_action(_seat_index, true);
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), table.DELAY_AUTO_OPERATE,
							TimeUnit.MILLISECONDS);
				}
			} else if (table.istrustee[_seat_index]) {
				if (currentPlayerStatus.has_zi_mo()) {
					table.exe_jian_pao_hu(_seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
				} else {
					table.operate_player_action(_seat_index, true);
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), table.DELAY_AUTO_OPERATE,
							TimeUnit.MILLISECONDS);
				}
			} else {
				// 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
				if (currentPlayerStatus.has_action()) {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index, false);
				} else {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_TaoJiang table, int seat_index, int operate_code, int operate_card) {
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
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			playerStatus.clean_action();
			playerStatus.clean_status();

			if (table.istrustee[_seat_index] && !table.is_match() && !table.isClubMatch() && !table.isCoinRoom()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), table.DELAY_AUTO_OPERATE,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(seat_index, seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true; // 即使有多个杠，杠玩第一张牌之后就要返回，然后再次进入发牌处理器
				}
			}
		}
			break;
		case GameConstants.WIK_ZI_MO: {
			table.card_type_when_win = Constants_TaoJiang.HU_CARD_TYPE_ZI_MO;

			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			table._cur_banker = seat_index;

			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._player_result.zi_mo_count[seat_index]++;

			table.set_niao_card(seat_index);

			table.process_chi_hu_player_operate(seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			if (table.get_da_hu_count(table.GRR._chi_hu_rights[seat_index]) > 0) {
				table._player_result.da_hu_zi_mo[seat_index]++;
			} else {
				table._player_result.xiao_hu_zi_mo[seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), table.DELAY_GAME_FINISH,
					TimeUnit.MILLISECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TaoJiang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 离地王牌还有多少张
		if (table.distance_to_ding_wang_card > 0) {
			roomResponse.setOperateLen(table.distance_to_ding_wang_card);
		}

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
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
				if (table.GRR._discard_cards[i][j] == table.joker_card_1 || table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
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

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, table._send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
							hand_cards[j] = hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
							break;
						} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
							hand_cards[j] = hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI
									+ GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
							break;
						} else {
							hand_cards[j] = hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
							break;
						}
					}
				}
				if (hand_cards[j] == table.joker_card_1
						|| hand_cards[j] == table.joker_card_2 && hand_cards[j] < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == table.ding_wang_card && hand_cards[j] < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING
						&& table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
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

			int tmp_card = table._playerStatus[seat_index]._hu_out_card_ting[i];
			if (tmp_card == table.joker_card_1 || tmp_card == table.joker_card_2) {
				roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
			} else if (tmp_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
			} else {
				roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = table._send_card_data;
		if (ting_send_card) {
			if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
			} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
			} else {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
			}
		} else if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// TODO 显示听牌数据
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);

		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
