package com.cai.game.mj.hunan.zhuzhouwang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ZhuZhouWang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.common.domain.WeaveItem;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_ZhuZhouWang extends MJHandlerDispatchCard<Table_ZhuZhouWang> {
	boolean ting_send_card = false;

	@Override
	public void exe(Table_ZhuZhouWang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		// 过手碰清理
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();
		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].chi_peng_round_valid(); // 可以碰了

		// 荒庄
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._cur_banker = table._cur_banker;// 继续做庄
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			return;
		}

		// 状态清理
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		// 海底
		if (table.GRR._left_card_count == 1) {
			table.exe_hai_di(_seat_index, _type);
			return;
		}

		table._current_player = _seat_index;

		// 刷新手牌
		int tmpcards[] = new int[GameConstants.MAX_COUNT];
		int[] tmp_card_index=table.GRR._cards_index[_seat_index].clone();
		int tmp_hand_card_count=GameConstants.MAX_COUNT;

		if (_type == GameConstants.WIK_BAO_TING && table._logic.is_valid_card(table._send_card_data)) {
			if (tmp_card_index[table._logic.switch_to_card_index(table._send_card_data)] > 0) {
				tmp_card_index[table._logic.switch_to_card_index(table._send_card_data)]--;
			}	
		}
		tmp_hand_card_count = table._logic.switch_to_cards_data(tmp_card_index, tmpcards);

		for (int j = 0; j < tmp_hand_card_count; j++) {
			if (tmpcards[j] == table.joker_card_1 || tmpcards[j] == table.joker_card_2) {
				tmpcards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (tmpcards[j] == table.ding_wang_card) {
				tmpcards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		if (_type != GameConstants.WIK_BAO_TING) {
			table._send_card_count++;
			// 摸牌
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

			--table.GRR._left_card_count;

			table._provide_player = _seat_index;

			if (table.DEBUG_CARDS_MODE) {
				_send_card_data = 0x15;
			}
			table._send_card_data = _send_card_data;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants_ZhuZhouWang.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_KAI) {
			card_type = GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_KAI;
		}

		if (_type != GameConstants.WIK_BAO_TING) {
			// 判断胡牌
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], table._send_card_data,
					chr, card_type, _seat_index);
			if (action != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(table._send_card_data, _seat_index);
			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
			}

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		}

		table._provide_card = table._send_card_data;

		if (table.GRR._left_card_count >= 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index],_send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult);

			if (cbActionMask != GameConstants.WIK_NULL) {
				if (table.is_bao_ting[_seat_index]) {
					GangCardResult tmp_gangCardResult = new GangCardResult();
					tmp_gangCardResult.cbCardCount = 0;

					boolean flag = filtrate_ting_data(table, _seat_index, m_gangCardResult, tmp_gangCardResult);
					if (flag) {
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
						for (int i = 0; i < tmp_gangCardResult.cbCardCount; i++) {
							curPlayerStatus.add_gang(tmp_gangCardResult.cbCardData[i], _seat_index,
									tmp_gangCardResult.isPublic[i]);
						}
					}
				} else {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
								m_gangCardResult.isPublic[i]);
					}
				}
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(table._send_card_data);
		// 打出摸的牌是否可以听牌
		ting_send_card = false;
		int card_type_count = GameConstants.MAX_ZI;
		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];
			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;
				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
				// 打出该牌可以听牌
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
			table.GRR._cards_index[_seat_index][send_card_index]--;
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (table.get_real_card(cards[i]) == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}

				if (table.get_real_card(cards[i]) == table.joker_card_1
						|| table.get_real_card(cards[i]) == table.joker_card_2) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (table.get_real_card(cards[i]) == table.ding_wang_card) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}

			}
			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		} else {
			table.operate_player_cards(_seat_index, tmp_hand_card_count, tmpcards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = table._send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		if (table._send_card_data == table.joker_card_1 || table._send_card_data == table.joker_card_2) {
			table.operate_player_get_card(_seat_index, 1,
					new int[] { real_card + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA }, GameConstants.INVALID_SEAT);
		} else if (table._send_card_data == table.ding_wang_card) {
			table.operate_player_get_card(_seat_index, 1,
					new int[] { real_card + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI },
					GameConstants.INVALID_SEAT);
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			if (table.is_bao_ting[_seat_index]) {
				table.operate_player_action(_seat_index, true);
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), 300,
						TimeUnit.MILLISECONDS);
			}
		}

		return;
	}

	@Override
	public boolean handler_operate_card(Table_ZhuZhouWang table, int seat_index, int operate_code, int operate_card) {
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

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			if (table.is_bao_ting[_seat_index]) {
				table.operate_player_action(_seat_index, true);
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), 300,
						TimeUnit.MILLISECONDS);
			}

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG:
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
							false);
					return true;
				}
			}
		case GameConstants.WIK_ZI_MO:

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			if (!table.GRR._chi_hu_rights[seat_index].opr_and(GameConstants_ZhuZhouWang.CHR_GANG_KAI).is_empty()) {
				table.hu_dec_type[seat_index] = 4;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					table.hu_dec_type[i] = 5;
				}
			} else {
				table.hu_dec_type[seat_index] = 1;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					table.hu_dec_type[i] = 5;
				}
			}
			// table.set_niao_card(seat_index, GameConstants.INVALID_VALUE, true); // 抓鸟
			table.set_niao_card(_seat_index, GameConstants.INVALID_VALUE, true);

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);

			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			table._cur_banker = _seat_index;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;

		}
		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_ZhuZhouWang table, int seat_index, int card) {
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

		if (_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_KAI) {
			table.exe_out_card(_seat_index, card, GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_SHANG_PAO);
		} else {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(Table_ZhuZhouWang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 骰子
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		roomResponse.setIsGoldRoom(table.is_sys());

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
				if (table.GRR._discard_cards[i][j] == table.joker_card_1
						|| table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
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

			//
			tableResponse.addWinnerOrder(0);

			// 牌

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}

		}

		// 数据
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
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}

				if (table.get_real_card(hand_cards[j]) == table.joker_card_1
						|| table.get_real_card(hand_cards[j]) == table.joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (table.get_real_card(hand_cards[j]) == table.ding_wang_card) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}

			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == table.ding_wang_card) {
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
			roomResponse.addOutCardTing(
					table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = table._send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		if (table._send_card_data == table.joker_card_1 || table._send_card_data == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (table._send_card_data == table.ding_wang_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 听牌显示
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

	public boolean filtrate_ting_data(Table_ZhuZhouWang table, int seat_index, GangCardResult srcGangCardResult,
			GangCardResult dstGangCardResult) {
		int index = 0;
		for (int i = 0; i < srcGangCardResult.cbCardCount; i++) {
			if (srcGangCardResult.isPublic[i] == 0) {
				int[] tmp_cards_index = table.GRR._cards_index[_seat_index].clone();
				tmp_cards_index[table._logic.switch_to_card_index(srcGangCardResult.cbCardData[i])] = 0;
				WeaveItem tmp_weave_item[] = table.GRR._weave_items[_seat_index].clone();
				int tmp_weave_count = table.GRR._weave_count[_seat_index];

				int[] tmp_hu_cards = new int[GameConstants.MAX_INDEX];
				int tmp_ting_count = table.get_ting_card(tmp_hu_cards, tmp_cards_index, tmp_weave_item, tmp_weave_count,
						_seat_index);

				if (table.check_ting_equals(tmp_ting_count, tmp_hu_cards, table._bao_ting_count_qishou[_seat_index],
						table._bao_ting_card_qishou[_seat_index])) {
					index = dstGangCardResult.cbCardCount++;
					dstGangCardResult.cbCardData[index] = table._logic.switch_to_card_data(i);
					dstGangCardResult.isPublic[index] = 0;// 安刚
					dstGangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
				}
			}
		}
		if (index > 0)
			return true;
		return false;
	}
}
