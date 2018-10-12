package com.cai.game.mj.sichuan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerDispatchCard_SiChuan extends MJHandlerDispatchCard<AbstractSiChuanMjTable> {
	public HandlerDispatchCard_SiChuan() {
		m_gangCardResult = new GangCardResult(GameConstants.MAX_COUNT);
	}

	boolean ting_send_card = false;
	int tiao_count = 0;
	int[] tiao_cards_data = new int[GameConstants.MAX_COUNT];

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

			if (table.has_win()) {
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			} else {
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			}

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
			_send_card_data = 0x17;
		}

		table._send_card_data = _send_card_data;

		table.mo_pai_count[_seat_index]++;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int card_type = Constants_SiChuan.HU_CARD_TYPE_ZI_MO;
		if (table.is_mj_type(GameConstants.GAME_TYPE_QIONG_LAI)) {
			if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
				card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_KAI;
			} else {
				// 非开杠之后的抓牌，重置牌桌上的杠上杠状态
				table.gang_shang_gang = false;
			}
		} else {
			if (_type == GameConstants.GANG_TYPE_JIE_GANG) {
				card_type = Constants_SiChuan.HU_CARD_TYPE_DIAN_GANG_GANG_KAI;
			} else if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
				card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_KAI;
			} else {
				// 非开杠之后的抓牌，重置牌桌上的杠上杠状态
				table.gang_shang_gang = false;
			}
		}

		table.analyse_state = table.FROM_NORMAL;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			if (card_type == Constants_SiChuan.HU_CARD_TYPE_DIAN_GANG_GANG_KAI && table.has_rule(Constants_SiChuan.GAME_RULE_DGH_DIAN_PAO)) {
				curPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
				curPlayerStatus.add_chi_hu(_send_card_data, _seat_index);
			} else {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			}
		} else {
			chr.set_empty();
		}

		int temp_cards[] = new int[GameConstants.MAX_COUNT];
		int temp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], temp_cards,
				table.ding_que_pai_se[_seat_index]);

		int must_out_card_count = 0;
		int tmp_pai_se = table._logic.get_card_color(_send_card_data);
		if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
			for (int i = 0; i < temp_hand_card_count; i++) {
				if (table._logic.is_magic_card(table.get_real_card(temp_cards[i])))
					continue;

				int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
				if ((pai_se + 1) == table.ding_que_pai_se[_seat_index]) {
					must_out_card_count++;
				}
			}

			if ((tmp_pai_se + 1) == table.ding_que_pai_se[_seat_index] && !table._logic.is_magic_card(_send_card_data)) {
				must_out_card_count++;
			}

			if (must_out_card_count > 0) {
				for (int i = 0; i < temp_hand_card_count; i++) {
					if (table._logic.is_magic_card(table.get_real_card(temp_cards[i]))) {
						temp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						continue;
					}

					int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
					if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
						temp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					}
				}

				table.operate_player_cards(_seat_index, temp_hand_card_count, temp_cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
			}
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI;
		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
			card_type_count = GameConstants.MAX_ZI_FENG;
		}

		for (int i = 0; i < card_type_count; i++) {
			if (table._logic.is_magic_index(i))
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

		if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
		}

		if (ting_count > 0) {
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
					table.ding_que_pai_se[_seat_index]);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
				if (must_out_card_count > 0) {
					for (int j = 0; j < hand_card_count; j++) {
						if (table._logic.is_magic_card(table.get_real_card(cards[j]))) {
							cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
							continue;
						}

						int pai_se = table._logic.get_card_color(table.get_real_card(cards[j]));
						if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
							cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						}
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = _send_card_data;
		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) && !table.hasRuleDingQue) {
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			} else if (table._logic.is_magic_card(real_card)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		} else {
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			} else if ((tmp_pai_se + 1) != table.ding_que_pai_se[_seat_index] && must_out_card_count > 0) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
				if (table._logic.is_magic_card(table.get_real_card(real_card))) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			} else if (table._logic.is_magic_card(real_card)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.operate_player_get_card_gang(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
			table.gang_dispatch_count++;
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		table._provide_card = _send_card_data;

		if (table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			if (table.GRR._left_card_count > 0) {
				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_with_suo_pai(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.passed_gang_cards[_seat_index],
						table.passed_gang_count[_seat_index], table.hasRuleRuanGang);

				boolean flag = false;
				if (cbActionMask != GameConstants.WIK_NULL) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						if (table._logic.is_magic_card(m_gangCardResult.cbCardData[i])
								|| (table._logic.get_card_color(m_gangCardResult.cbCardData[i]) + 1 != table.ding_que_pai_se[_seat_index])) {
							curPlayerStatus.add_normal_gang_wik(m_gangCardResult.cbCardData[i], m_gangCardResult.detailActionType[i], _seat_index,
									m_gangCardResult.isPublic[i]);
							flag = true;
						}
					}
				}
				if (flag) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}
		} else {
			if (table.GRR._left_card_count > 0) {
				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_card_all_xzdd(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.passed_gang_cards[_seat_index],
						table.passed_gang_count[_seat_index]);

				boolean flag = false;
				if (cbActionMask != GameConstants.WIK_NULL) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						if (table._logic.get_card_color(m_gangCardResult.cbCardData[i]) + 1 != table.ding_que_pai_se[_seat_index]) {
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
							flag = true;
						}
					}
				}
				if (flag) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}
		}

		if (table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			// 乐山麻将，分析挑
			tiao_count = table.analyse_tiao_pai(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], tiao_cards_data);
			if (tiao_count > 0)
				curPlayerStatus.add_action(GameConstants.WIK_TIAO);
			for (int i = 0; i < tiao_count; i++) {
				curPlayerStatus.add_normal_wik(tiao_cards_data[i], GameConstants.WIK_TIAO, _seat_index);
			}
		}

		// 重置胡牌分析的入口点
		table.analyse_state = table.FROM_NORMAL;

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
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

			table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			if (table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI))
				return true;

			if (table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
				int temp_cards[] = new int[GameConstants.MAX_COUNT];
				int temp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], temp_cards,
						table.ding_que_pai_se[_seat_index]);

				table.remove_card_by_data(temp_cards, _send_card_data);

				int must_out_card_count = 0;
				if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
					for (int i = 0; i < temp_hand_card_count - 1; i++) {
						if (table._logic.is_magic_card(table.get_real_card(temp_cards[i])))
							continue;

						int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
						if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
							must_out_card_count++;
						}
					}

					int tmp_pai_se = table._logic.get_card_color(_send_card_data);
					if ((tmp_pai_se + 1) == table.ding_que_pai_se[seat_index] && !table._logic.is_magic_card(_send_card_data)) {
						must_out_card_count++;
					}

					if (must_out_card_count > 0) {
						for (int i = 0; i < temp_hand_card_count - 1; i++) {
							if (table._logic.is_magic_card(table.get_real_card(temp_cards[i]))) {
								temp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
								continue;
							}

							int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
							if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
								temp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
							}
						}

						table.operate_player_cards(_seat_index, temp_hand_card_count - 1, temp_cards, table.GRR._weave_count[_seat_index],
								table.GRR._weave_items[_seat_index]);

						int temp_card = _send_card_data;
						int pai_se = table._logic.get_card_color(temp_card);
						if ((pai_se + 1) != table.ding_que_pai_se[_seat_index] && !table._logic.is_magic_card(temp_card)) {
							temp_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						} else if (table._logic.is_magic_card(temp_card)) {
							temp_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							temp_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						}
						table.operate_player_get_card(_seat_index, 1, new int[] { temp_card }, GameConstants.INVALID_SEAT);
					}
				}
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
		case GameConstants.WIK_TIAO: {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1,
					GameConstants.INVALID_SEAT);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			_type = GameConstants.WIK_NULL;

			int index = table._logic.switch_to_card_index(operate_card);
			table.GRR._cards_index[_seat_index][index]--;

			int wcount = table.GRR._weave_count[_seat_index];
			for (int i = 0; i < wcount; i++) {
				int kind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int card = table.GRR._weave_items[_seat_index][i].center_card;
				if (card == operate_card) {
					if (kind == GameConstants.WIK_SUO_GANG_1) {
						table.GRR._weave_items[_seat_index][i].weave_kind = GameConstants.WIK_GANG;

						// 挑完之后 对杠牌数据的流水 进行更新
						for (int[] sd : table.scoreDetails) {
							if (sd[5] == card) {
								if (sd[0] == ScoreRowType.AN_GANG_RUAN_GANG.getType())
									sd[0] = ScoreRowType.AN_GANG_WU_JI.getType();
								else if (sd[0] == ScoreRowType.MING_GANG_RUAN_GANG.getType())
									sd[0] = ScoreRowType.MING_GANG_WU_JI.getType();
								else if (sd[0] == ScoreRowType.BA_GANG_RUAN_GANG.getType())
									sd[0] = ScoreRowType.BA_GANG_WU_JI.getType();

								if (table.hasRuleWuJiGangDouble) {
									sd[1] *= 2;
									sd[2] *= 2;
									sd[3] *= 2;
									sd[4] *= 2;
								}
							}
						}
					}
					if (kind == GameConstants.WIK_SUO_GANG_2) {
						table.GRR._weave_items[_seat_index][i].weave_kind = GameConstants.WIK_SUO_GANG_1;
					}
					if (kind == GameConstants.WIK_SUO_GANG_3) {
						table.GRR._weave_items[_seat_index][i].weave_kind = GameConstants.WIK_SUO_GANG_2;
					}
				}
			}

			int temp_cards[] = new int[GameConstants.MAX_COUNT];
			int temp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], temp_cards,
					table.ding_que_pai_se[_seat_index]);
			table.operate_player_cards(_seat_index, temp_hand_card_count, temp_cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);

			_send_card_data = table.magicCard;
			table._send_card_data = _send_card_data;

			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			int card_type = Constants_SiChuan.HU_CARD_TYPE_ZI_MO;

			table.analyse_state = table.FROM_NORMAL;
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			curPlayerStatus.reset();

			if (action != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			} else {
				chr.set_empty();
			}

			int must_out_card_count = 0;
			int tmp_pai_se = table._logic.get_card_color(_send_card_data);
			if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
				for (int i = 0; i < temp_hand_card_count; i++) {
					if (table._logic.is_magic_card(table.get_real_card(temp_cards[i])))
						continue;

					int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
					if ((pai_se + 1) == table.ding_que_pai_se[_seat_index]) {
						must_out_card_count++;
					}
				}

				if ((tmp_pai_se + 1) == table.ding_que_pai_se[_seat_index] && !table._logic.is_magic_card(_send_card_data)) {
					must_out_card_count++;
				}

				if (must_out_card_count > 0) {
					for (int i = 0; i < temp_hand_card_count; i++) {
						if (table._logic.is_magic_card(table.get_real_card(temp_cards[i])))
							continue;

						int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
						if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
							temp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						}
					}

					table.operate_player_cards(_seat_index, temp_hand_card_count, temp_cards, table.GRR._weave_count[_seat_index],
							table.GRR._weave_items[_seat_index]);
				}
			}

			table.GRR._cards_index[_seat_index][table.magicCardIndex]++;

			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI;
			if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
				card_type_count = GameConstants.MAX_ZI_FENG;
			}

			for (int i = 0; i < card_type_count; i++) {
				if (table._logic.is_magic_index(i))
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

			if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
				table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
			}

			if (ting_count > 0) {
				table.GRR._cards_index[_seat_index][send_card_index]--;

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
						table.ding_que_pai_se[_seat_index]);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
					if (must_out_card_count > 0) {
						for (int j = 0; j < hand_card_count; j++) {
							if (table._logic.is_magic_card(table.get_real_card(cards[j]))) {
								cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
								continue;
							}

							int pai_se = table._logic.get_card_color(table.get_real_card(cards[j]));
							if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
								cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
							}
						}
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End

			int real_card = _send_card_data;
			if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) && !table.hasRuleDingQue) {
				if (ting_send_card) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
				}
			} else {
				if (ting_send_card) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
				} else if ((tmp_pai_se + 1) != table.ding_que_pai_se[_seat_index] && must_out_card_count > 0) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					if (table._logic.is_magic_card(table.get_real_card(real_card))) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				} else if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

			table._provide_card = _send_card_data;

			if (table.GRR._left_card_count > 0) {
				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_with_suo_pai(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.passed_gang_cards[_seat_index],
						table.passed_gang_count[_seat_index], table.hasRuleRuanGang);

				boolean flag = false;
				if (cbActionMask != GameConstants.WIK_NULL) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						if (table._logic.is_magic_card(m_gangCardResult.cbCardData[i])
								|| (table._logic.get_card_color(m_gangCardResult.cbCardData[i]) + 1 != table.ding_que_pai_se[_seat_index])) {
							curPlayerStatus.add_normal_gang_wik(m_gangCardResult.cbCardData[i], m_gangCardResult.detailActionType[i], _seat_index,
									m_gangCardResult.isPublic[i]);
							flag = true;
						}
					}
				}
				if (flag) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}

			// 乐山麻将，分析挑
			tiao_count = table.analyse_tiao_pai(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], tiao_cards_data);
			if (tiao_count > 0)
				curPlayerStatus.add_action(GameConstants.WIK_TIAO);
			for (int i = 0; i < tiao_count; i++) {
				curPlayerStatus.add_normal_wik(tiao_cards_data[i], GameConstants.WIK_TIAO, _seat_index);
			}

			if (curPlayerStatus.has_action()) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.table_hu_cards[table.table_hu_card_count++] = operate_card;

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			int p_index = -1;
			for (int w = table.GRR._weave_count[seat_index] - 1; w >= 0; w--) {
				WeaveItem wi = table.GRR._weave_items[seat_index][w];
				if (wi.weave_kind == GameConstants.WIK_GANG || wi.weave_kind == GameConstants.WIK_SUO_GANG_1
						|| wi.weave_kind == GameConstants.WIK_SUO_GANG_2 || wi.weave_kind == GameConstants.WIK_SUO_GANG_3) {
					p_index = wi.provide_player;
					break;
				}
			}
			table.whoProvided[_seat_index] = p_index;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
				table.operate_player_cards_flag(_seat_index, 0, null, 0, null);
			}

			table._player_result.jie_pao_count[_seat_index]++;
			table._player_result.dian_pao_count[p_index]++;

			table.had_hu_pai[_seat_index] = true;
			table.left_player_count--;
			table.win_order[_seat_index] = table.getTablePlayerNumber() - table.left_player_count;
			table.win_type[_seat_index] = table.JIE_PAO_HU;

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
					if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
						if (table.had_hu_pai[i] == false && table._playerStatus[i]._hu_card_count > 0) {
							table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
						}
					}
				}

				table.exe_dispatch_card(table.get_next_seat(_seat_index), GameConstants.WIK_NULL, 0);
			}

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.table_hu_cards[table.table_hu_card_count++] = operate_card;

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			table.whoProvided[_seat_index] = _seat_index;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
				table.operate_player_cards_flag(_seat_index, 0, null, 0, null);
			}

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
					if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
						if (table.had_hu_pai[i] == false && table._playerStatus[i]._hu_card_count > 0) {
							table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
						}
					}
				}

				table.exe_dispatch_card(table.get_next_seat(_seat_index), GameConstants.WIK_NULL, 0);
			}

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

		MahjongUtils.dealAllPlayerCardsNoSpecial(table, tableResponse);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		if (seat_index == _seat_index) {
			table.remove_card_by_data(hand_cards, _send_card_data);
		}

		int out_ting_count = (seat_index == _seat_index) ? table._playerStatus[seat_index]._hu_out_card_count : 0;
		roomResponse.setOutCardCount(out_ting_count);

		if ((out_ting_count > 0) && (seat_index == _seat_index)
				&& (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) || table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS))) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		int must_out_card_count = 0;
		if ((table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) && table.hasRuleDingQue)
				|| table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			for (int i = 0; i < hand_card_count - 1; i++) {
				if (table._logic.is_magic_card(table.get_real_card(hand_cards[i])))
					continue;

				int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
				if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
					must_out_card_count++;
				}
			}

			if (seat_index == _seat_index) {
				int tmp_pai_se = table._logic.get_card_color(_send_card_data);
				if ((tmp_pai_se + 1) == table.ding_que_pai_se[seat_index] && !table._logic.is_magic_card(_send_card_data)) {
					must_out_card_count++;
				}
			}

			if (must_out_card_count > 0) {
				for (int i = 0; i < hand_card_count - 1; i++) {
					if (table._logic.is_magic_card(table.get_real_card(hand_cards[i]))) {
						hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						continue;
					}

					int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
					if ((pai_se + 1) != table.ding_que_pai_se[seat_index]) {
						hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					}
				}
			}
		} else if (table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
			if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
				for (int i = 0; i < hand_card_count - 1; i++) {
					if (table._logic.is_magic_card(table.get_real_card(hand_cards[i])))
						continue;

					int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
					if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
						must_out_card_count++;
					}
				}

				if (seat_index == _seat_index) {
					int tmp_pai_se = table._logic.get_card_color(_send_card_data);
					if ((tmp_pai_se + 1) == table.ding_que_pai_se[seat_index] && !table._logic.is_magic_card(_send_card_data)) {
						must_out_card_count++;
					}
				}

				if (must_out_card_count > 0) {
					for (int i = 0; i < hand_card_count - 1; i++) {
						if (table._logic.is_magic_card(table.get_real_card(hand_cards[i]))) {
							hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
							continue;
						}

						int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
						if ((pai_se + 1) != table.ding_que_pai_se[seat_index]) {
							hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						}
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

		if (out_ting_count > 0 && table.getRuleValue(Constants_SiChuan.GAME_RULE_TING_PAI_TS) == 1) {
			table.operate_player_cards_with_ting(seat_index, hand_card_count - 1, hand_cards, 0, null);
		} else if (table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OUT_CARD && must_out_card_count > 0) {
			table.operate_player_cards(seat_index, hand_card_count - 1, hand_cards, table.GRR._weave_count[seat_index],
					table.GRR._weave_items[seat_index]);
		}

		int real_card = _send_card_data;
		int tmp_pai_se = table._logic.get_card_color(_send_card_data);
		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) && !table.hasRuleDingQue) {
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			} else if (table._logic.is_magic_card(real_card)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		} else {
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			} else if ((tmp_pai_se + 1) != table.ding_que_pai_se[_seat_index] && must_out_card_count > 0) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
				if (table._logic.is_magic_card(table.get_real_card(real_card))) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			} else if (table._logic.is_magic_card(real_card)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
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
