package com.cai.game.mj.sichuan.luzhougui;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerDispatchCard_LuZhouGui extends MJHandlerDispatchCard<AbstractSiChuanMjTable> {
	public HandlerDispatchCard_LuZhouGui() {
		m_gangCardResult = new GangCardResult(GameConstants.MAX_COUNT);
	}

	boolean ting_send_card = false;

	@Override
	public boolean handler_player_out_card(AbstractSiChuanMjTable table, int seat_index, int card) {
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

		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_GANG);
		} else {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(AbstractSiChuanMjTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.score_when_abandoned_win[_seat_index] = 0;

		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		if (table.GRR._left_card_count == 0) {
			// 处理杠分
			table.process_gang_score();

			// 查大叫
			table.cha_da_jiao();

			table.process_show_hand_card();

			if (table.next_banker_player != -1)
				table._cur_banker = table.next_banker_player;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x27;
		}

		table._send_card_data = _send_card_data;

		table.mo_pai_count[_seat_index]++;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = Constants_SiChuan.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_KAI;
		} else {
			// 非开杠之后的抓牌，重置牌桌上的杠上杠状态
			table.gang_shang_gang = false;
		}

		table.analyse_state = table.FROM_NORMAL;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			chr.set_empty();
		}

		int temp_cards[] = new int[GameConstants.MAX_COUNT];
		@SuppressWarnings("unused")
		int temp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], temp_cards,
				table.ding_que_pai_se[_seat_index]);

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			if (i >= GameConstants.MAX_ZI && i != table.magicCardIndex)
				continue;

			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, ting_count);

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
			int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
					table.ding_que_pai_se[_seat_index]);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					int real_card = table.get_real_card(cards[i]);
					if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] = real_card + GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);

			if (table.mo_pai_count[_seat_index] == 1 && table.GRR._weave_count[_seat_index] == 0 && _seat_index != table.GRR._banker_player) {
				curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				curPlayerStatus.add_normal_wik(0, 0, 0);
			}
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (real_card == table.magicCard) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.operate_player_get_card_gang(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
			table.gang_dispatch_count++;
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		table._provide_card = _send_card_data;

		if (table.is_bao_ting[_seat_index]) {
			if (table.GRR._left_card_count > 0) {
				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_card_all_luzhougui(table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
						table.passed_gang_cards[_seat_index], table.passed_gang_count[_seat_index]);

				if (cbActionMask != GameConstants.WIK_NULL) {
					boolean flag = false;
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						if (table.is_bao_ting[_seat_index]) {
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

							boolean is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
									table.GRR._weave_count[_seat_index], _seat_index);

							// 还原手牌数据和落地牌数据
							table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
							table.GRR._weave_count[_seat_index] = tmp_weave_count;

							// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
							if (is_ting_state) {
								curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
								flag = true;
							}
						} else {
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
							flag = true;
						}
					}
					if (flag) { // 如果能杠，当前用户状态加上杠牌动作
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
					}
				}
			}
		} else {
			if (table.GRR._left_card_count > 0) {
				int[] colorFlag = new int[4];
				int colorCount = 0;
				for (int j = 0; j < table.GRR._weave_count[_seat_index]; j++) {
					int wCard = table.GRR._weave_items[_seat_index][j].center_card;
					int wColor = table._logic.get_card_color(wCard);
					if (colorFlag[wColor] == 0) {
						colorFlag[wColor] = 1;
						colorCount++;
					}
				}

				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_with_suo_pai_luzhougui(table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
						table.passed_gang_cards[_seat_index], table.passed_gang_count[_seat_index], true, table.display_ruan_peng[_seat_index]);

				boolean flag = false;
				if (cbActionMask != GameConstants.WIK_NULL) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						if (table._logic.is_magic_card(m_gangCardResult.cbCardData[i])
								|| (table._logic.get_card_color(m_gangCardResult.cbCardData[i]) + 1 != table.ding_que_pai_se[_seat_index])) {
							int tmpColor = table._logic.get_card_color(m_gangCardResult.cbCardData[i]);

							if (colorCount != 2 || colorFlag[tmpColor] != 0) {
								curPlayerStatus.add_normal_gang_wik(m_gangCardResult.cbCardData[i], m_gangCardResult.detailActionType[i], _seat_index,
										m_gangCardResult.isPublic[i]);
								flag = true;
							}
						}
					}
				}
				if (flag) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}
		}

		// 重置胡牌分析的入口点
		table.analyse_state = table.FROM_NORMAL;

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.is_bao_ting[_seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), 1000, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, UniversalConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}
	}

	@Override
	public boolean handler_operate_card(AbstractSiChuanMjTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (operate_code == GameConstants.WIK_SUO_PENG_1 || operate_code == GameConstants.WIK_SUO_PENG_2) {
			if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(GameConstants.WIK_PENG) == false) {
				table.log_error("没有这个操作");
				return true;
			}
		} else if (operate_code == GameConstants.WIK_SUO_GANG_1 || operate_code == GameConstants.WIK_SUO_GANG_2
				|| operate_code == GameConstants.WIK_SUO_GANG_3) {
			if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(GameConstants.WIK_GANG) == false) {
				table.log_error("没有这个操作");
				return true;
			}
		} else {
			if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
				table.log_error("没有这个操作");
				return true;
			}
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

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table.is_bao_ting[_seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), 1000, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(seat_index, UniversalConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		if (table._playerStatus[seat_index].has_zi_mo() && operate_code != GameConstants.WIK_ZI_MO) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			table.GRR._chi_hu_rights[seat_index].set_empty();

			// 记录过胡的时候，牌型的番数，变大了，本圈才能接炮
			table.score_when_abandoned_win[seat_index] = table.score_when_win[seat_index];
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG:
		case GameConstants.WIK_SUO_GANG_1:
		case GameConstants.WIK_SUO_GANG_2:
		case GameConstants.WIK_SUO_GANG_3: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i] || operate_card == m_gangCardResult.realOperateCard[i]) {
					if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG
							|| _type == GameConstants.GANG_TYPE_JIE_GANG) {
						// 杠之后抓了一张牌，继续杠，牌桌上就是杠上杠状态
						table.gang_shang_gang = true;
					}
					table.exe_gang(_seat_index, _seat_index, m_gangCardResult.cbCardData[i], operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
		case GameConstants.WIK_ZI_MO: {
			table.table_hu_cards[table.table_hu_card_count++] = operate_card;

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			table.whoProvided[_seat_index] = _seat_index;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table.operate_player_cards_flag(_seat_index, 0, null, 0, null);

			table._player_result.zi_mo_count[_seat_index]++;

			table.had_hu_pai[_seat_index] = true;
			table.left_player_count--;
			table.win_order[_seat_index] = table.getTablePlayerNumber() - table.left_player_count;
			table.win_type[_seat_index] = table.ZI_MO_HU;

			if (table.left_player_count == table.getTablePlayerNumber() - 1) {
				table.next_banker_player = _seat_index;
			}

			table.operate_player_info();

			if (table.left_player_count == 1) {
				table._cur_banker = table.next_banker_player;

				// 处理杠分
				table.process_gang_score();

				table.process_show_hand_card();

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.had_hu_pai[i] == false && table._playerStatus[i]._hu_card_count > 0) {
						table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
					}
				}

				table.exe_dispatch_card(table.get_next_seat(_seat_index), GameConstants.WIK_NULL, 0);
			}

			return true;
		}
		case GameConstants.WIK_BAO_TING: {
			operate_card = table.get_real_card(operate_card);

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(AbstractSiChuanMjTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		MahjongUtils.showTouZiSiChuan(table, roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		MahjongUtils.dealCommonDataReconnect(table, roomResponse, tableResponse);

		MahjongUtils.dealAllPlayerCardsLaiZi(table, tableResponse);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		if (seat_index == _seat_index) {
			table.remove_card_by_data(hand_cards, _send_card_data);
		}

		int out_ting_count = (seat_index == _seat_index) ? table._playerStatus[seat_index]._hu_out_card_count : 0;
		roomResponse.setOutCardCount(out_ting_count);

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

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			for (int j = 0; j < ting_card_cout; j++) {
				roomResponse.addDouliuzi(table.ting_pai_fan_shu[seat_index][i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (real_card == table.magicCard) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.operate_player_get_card_gang(_seat_index, 1, new int[] { real_card }, seat_index);
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		}

		if (seat_index != _seat_index)
			MahjongUtils.showTingPai(table, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 处理断线重连时，胡牌人的胡牌显示
		table.process_duan_xian_chong_lian(seat_index);

		return true;
	}
}
