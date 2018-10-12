package com.cai.game.mj.yu.gy;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_GY;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.yu.mygy.GameConstants_MYGY;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_GY extends MJHandlerDispatchCard<Table_GY> {
	boolean ting_send_card = false;
	protected int _seat_index;
	protected int _send_card_data;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_GY() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_GY table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_HZLZG.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();

		if (table.GRR._left_card_count == 0) {
			table.huan_zhuan();
			// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			// table.GRR._chi_hu_card[i][0] = GameConstants_HZLZG.INVALID_VALUE;
			// }
			//
			// int jiao_pai_count = 0;
			// for (int player = 0; player < table.getTablePlayerNumber();
			// player++)
			// if (table._playerStatus[player]._hu_card_count > 0)
			// jiao_pai_count++;
			//
			// if (jiao_pai_count == 0) {
			// table._cur_banker = table._cur_banker;
			// } else if (jiao_pai_count == 1) {
			// int index = -1;
			// for (int player = 0; player < table.getTablePlayerNumber();
			// player++)
			// if (table._playerStatus[player]._hu_card_count > 0) {
			// index = player;
			// break;
			// }
			// table._cur_banker = index;
			// } else if (jiao_pai_count == 2) {
			// int next_banker = (table._cur_banker + 1 +
			// table.getTablePlayerNumber()) % table.getTablePlayerNumber();
			// table._cur_banker = next_banker;
			// } else if (jiao_pai_count == 3) {
			// int index = -1;
			// for (int player = 0; player < table.getTablePlayerNumber();
			// player++)
			// if (table._playerStatus[player]._hu_card_count == 0) {
			// index = player;
			// break;
			// }
			// table._cur_banker = index;
			// } else {
			// table._cur_banker = table._cur_banker;
			// }
			//
			// if (table._cur_banker == table.old_banker) {
			// table.continue_banker_count++;
			// } else {
			// table.continue_banker_count = 1;
			// }
			// table.exe_select_magic();

			table.continue_banker_count++;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);
			table.process_chi_hu_player_operate(_seat_index, GameConstants.WIK_NULL, false);
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

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu && (!table.has_rule(GameConstants_GY.GAME_RULE_ER_FANG_PAI)
				&& (table._game_type_index == GameConstants.GAME_TYPE_GY_EDG || table._game_type_index == GameConstants.GAME_TYPE_GT_SDG))) {

			for (int player = 0; player < table.getTablePlayerNumber(); player++) {
				table._playerStatus[player].add_action(GameConstants_GY.WIK_DING_WANG);
				table._playerStatus[player].add_action(GameConstants_GY.WIK_DING_TONG);
				table._playerStatus[player].add_action(GameConstants_GY.WIK_DING_TIAO);
				table._player_result.ziba[player] = 1;
				table._player_result.dingQueInfo.needDingQueVaild(player);
				table.change_player_status(player, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(player, false);
			}
			table.operate_player_data();
			// table.operate_player_get_card(_seat_index, 1, new int[] {
			// _send_card_data }, GameConstants_HZLZG.INVALID_SEAT);
			return;
		}
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;

		int card_type = GameConstants_GY.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG) {
			card_type = GameConstants_GY.HU_CARD_TYPE_GANG_KAI_HUA;
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
		if (table.player_duan[_seat_index] != -1) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table.GRR._cards_index[_seat_index][i] > 0
						&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[_seat_index]) {
					action = GameConstants_HZLZG.WIK_NULL;
					break;
				}
			}
		}
		if (action != GameConstants_HZLZG.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants_HZLZG.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		boolean hand_have_duan_card = false;
		if (table.player_duan[_seat_index] != -1) {
			if (table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)) {
				hand_have_duan_card = true;
			} else {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++)
					if (table.GRR._cards_index[_seat_index][i] > 0
							&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[_seat_index]) {
						hand_have_duan_card = true;
						break;
					}
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;
		if (!hand_have_duan_card && !table._playerStatus[_seat_index].is_bao_ting()) {
			for (int i = 0; i < card_type_count; i++) {
				if (table._logic.is_magic_index(i))
					continue;

				count = table.GRR._cards_index[_seat_index][i];

				if (count > 0) {
					table.GRR._cards_index[_seat_index][i]--;

					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
							table._playerStatus[_seat_index]._hu_out_cards[ting_count],
							table._playerStatus[_seat_index]._hu_out_cards_fan[ting_count], table.GRR._cards_index[_seat_index],
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
				int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards, _seat_index);

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

				// 摸第一张牌才能报听
				if (table.player_mo_first[_seat_index]) {
					// 添加动作
					curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				}
			}
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		if (hand_have_duan_card) {
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--; // 刷新下手牌，摸的牌先刷出去

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table.GRR._cards_index[_seat_index][i] == 0)
					continue;

				for (int j = 0; j < table.GRR._cards_index[_seat_index][i]; j++) {
					if (table._logic.get_card_color(table._logic.switch_to_card_data(i)) != table.player_duan[_seat_index]) {
						cards[hand_card_count++] = table._logic.switch_to_card_data(i) + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					}
				}
			}
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table.GRR._cards_index[_seat_index][i] == 0)
					continue;
				for (int j = 0; j < table.GRR._cards_index[_seat_index][i]; j++) {
					if (table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[_seat_index]) {
						cards[hand_card_count++] = table._logic.switch_to_card_data(i);
					}
				}
			}
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

			if (table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)) {
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants_HZLZG.INVALID_SEAT);
			} else {
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG },
						GameConstants_HZLZG.INVALID_SEAT);
			}

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants_HZLZG.INVALID_SEAT);
		}

		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table._playerStatus[_seat_index].get_cards_abandoned_gang(),
					_seat_index, _send_card_data);

			if (cbActionMask != GameConstants_GY.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants_GY.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu && table.check_ying_bao()) {
			table.exe_ying_bao(_type, m_gangCardResult);
			return;
		}
		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.player_mo_first[_seat_index] = false;
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	@Override
	public boolean handler_player_out_card(Table_GY table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[seat_index], card) == false) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[seat_index], cards, seat_index);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);

			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu) {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants_GY.OutCard_Type_Di_Hu);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, _type);
		}

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_GY table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants_HZLZG.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		// if (seat_index != _seat_index) {
		// table.log_error("不是当前玩家操作");
		// return false;
		// }

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants_HZLZG.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.player_mo_first[_seat_index] = false;
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (GameConstants_GY.GANG_TYPE_ADD_GANG == m_gangCardResult.type[i]) {
					table._playerStatus[_seat_index].add_cards_abandoned_gang(table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]));
				}
			}

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

		if (operate_code == GameConstants.WIK_ZI_MO) {
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}
		}
		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			table.player_mo_first[_seat_index] = false;
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

			if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu) {
				table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_GY.CHR_TIAN_HU);
			}
			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;

			table.exe_select_magic();
			table.process_ji_fen();
			table.process_reponsibility_ji_fen();

			// 将胡的牌加入鸡牌中
			if (table.is_ji_card(operate_card))
				table.out_ji_pai[_seat_index][table.out_ji_pai_count[_seat_index]++] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_BAO_TING: // 报听
		{
			if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu) {
				table.player_ying_bao[_seat_index] = true;
			} else {
				table.player_ruan_bao[_seat_index] = true;
			}

			operate_card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

			if (table._out_card_count == 0) {// 起手报听
				ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
				chr.qi_shou_bao_ting = GameConstants.CHR_HUNAN_QISHOU_BAO_TING;
			}

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
			table.exe_out_card_bao_ting(_seat_index, operate_card,
					_type == GameConstants.DispatchCard_Type_Tian_Hu ? GameConstants.OutCard_Type_Di_Hu : GameConstants.WIK_NULL);
			return true;
		}
		case GameConstants_GY.WIK_DING_WANG:
		case GameConstants_GY.WIK_DING_TONG:
		case GameConstants_GY.WIK_DING_TIAO: {

			// 定缺信息
			if (operate_code == GameConstants_GY.WIK_DING_WANG) {
				table.player_duan[seat_index] = table._logic.get_card_color(0x00);
				table.zi_da[seat_index] = 2;
			}
			if (operate_code == GameConstants_GY.WIK_DING_TIAO) {
				table.player_duan[seat_index] = table._logic.get_card_color(0x10);
				table.zi_da[seat_index] = 3;
			}
			if (operate_code == GameConstants_GY.WIK_DING_TONG) {
				table.player_duan[seat_index] = table._logic.get_card_color(0x20);
				table.zi_da[seat_index] = 4;
			}

			// 加入定缺信息
			table._player_result.dingQueInfo.needDingQueInVaild(seat_index);
			table._player_result.dingQueInfo.addQueColors(seat_index, table.player_duan[seat_index]);
			table._player_result.ziba[seat_index] = 0; // 此处先默不在定缺中
			boolean flag = true;
			for (int player = 0; player < table.getTablePlayerNumber(); player++)
				if (table._playerStatus[player].has_action() && table._playerStatus[player].is_respone() == false) {
					flag = false;
					break;
				}
			if (flag)
				for (int player = 0; player < table.getTablePlayerNumber(); player++)
					table._player_result.ziba[player] = table.zi_da[player];

			table.operate_player_data();
			// ;

			// if (seat_index == _seat_index)
			// table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			table.player_mo_first[_seat_index] = false;
			int show_send_card = _send_card_data;

			boolean hand_have_duan_card = false;
			if (table.player_duan[seat_index] != -1) {
				if (seat_index == _seat_index && table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)
						&& !table._logic.is_magic_card(_send_card_data)) {
					hand_have_duan_card = true;
				} else {
					for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
						if (table._logic.is_magic_index(i)) {
							continue;
						}
						if (table.GRR._cards_index[seat_index][i] > 0
								&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[seat_index]) {
							hand_have_duan_card = true;
							break;
						}
					}
				}
			}
			if (hand_have_duan_card) {
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = 0;
				for (int i = 0; i < table.GRR._cards_index[seat_index][table._logic.get_magic_card_index(0)]
						&& table._logic.get_magic_card_count() == 1; i++) {
					cards[hand_card_count++] = table._logic.switch_to_card_data(table._logic.get_magic_card_index(0))
							+ GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
				}
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					if (table.GRR._cards_index[seat_index][i] == 0 || table._logic.is_magic_index(i))
						continue;

					if (seat_index == _seat_index)
						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
					for (int j = 0; j < table.GRR._cards_index[seat_index][i]; j++) {
						if (table._logic.get_card_color(table._logic.switch_to_card_data(i)) != table.player_duan[seat_index]) {
							cards[hand_card_count++] = table._logic.switch_to_card_data(i) + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
						}
					}
					if (seat_index == _seat_index)
						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

				}
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					if (table.GRR._cards_index[seat_index][i] == 0 || table._logic.is_magic_index(i))
						continue;

					if (seat_index == _seat_index)
						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
					for (int j = 0; j < table.GRR._cards_index[seat_index][i]; j++) {
						if (table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[seat_index]) {
							cards[hand_card_count++] = table._logic.switch_to_card_data(i);
						}
					}
					if (seat_index == _seat_index)
						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

				}
				table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index],
						table.GRR._weave_items[seat_index]);
			}

			table._playerStatus[seat_index].clean_action();

			for (int player = 0; player < table.getTablePlayerNumber(); player++)
				if (table._playerStatus[player].has_action() && table._playerStatus[player].is_respone() == false)
					return false;

			boolean _seat_index_hand_have_duan_card = false;
			if (table.player_duan[_seat_index] != -1) {
				if (table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data) && !table._logic.is_magic_card(_send_card_data)) {
					_seat_index_hand_have_duan_card = true;
				} else {
					for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
						if (table._logic.is_magic_index(i)) {
							continue;
						}
						if (table.GRR._cards_index[_seat_index][i] > 0
								&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[_seat_index]) {
							_seat_index_hand_have_duan_card = true;
							break;
						}
					}
				}
			}

			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI_FENG;
			if (!_seat_index_hand_have_duan_card && !table._playerStatus[_seat_index].is_bao_ting()) {
				for (int i = 0; i < card_type_count; i++) {
					if (table._logic.is_magic_index(i))
						continue;

					count = table.GRR._cards_index[_seat_index][i];

					if (count > 0) {
						table.GRR._cards_index[_seat_index][i]--;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
								table._playerStatus[_seat_index]._hu_out_cards[ting_count],
								table._playerStatus[_seat_index]._hu_out_cards_fan[ting_count], table.GRR._cards_index[_seat_index],
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
					int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards, _seat_index);

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

					// 添加动作
					table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
				}
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
			if (_seat_index_hand_have_duan_card) {
				if (table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)) {
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);
				} else {
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG },
							GameConstants.INVALID_SEAT);
				}
			} else {
				if (ting_send_card) {
					table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card + GameConstants.CARD_ESPECIAL_TYPE_TING },
							GameConstants_HZLZG.INVALID_SEAT);
				} else {
					table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants_HZLZG.INVALID_SEAT);
				}
			}

			table.player_mo_first[_seat_index] = false;
			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			int card_type = GameConstants_GY.HU_CARD_TYPE_ZI_MO;
			if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG) {
				card_type = GameConstants_GY.HU_CARD_TYPE_GANG_KAI_HUA;
			}
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			if (table.player_duan[_seat_index] != -1) {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					if (table._logic.is_magic_index(i)) {
						continue;
					}
					if (table.GRR._cards_index[_seat_index][i] > 0
							&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[_seat_index]) {
						action = GameConstants_HZLZG.WIK_NULL;
						break;
					}
				}
			}

			if (action != GameConstants_HZLZG.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants_HZLZG.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
				chr.set_empty();
			}

			table._provide_card = _send_card_data;

			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.GRR._cards_abandoned_gang[_seat_index]);

			if (cbActionMask != GameConstants_HZLZG.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants_HZLZG.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}

			if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu && table.check_ying_bao()) {
				table.exe_ying_bao(_type, m_gangCardResult);
				return true;
			}

			if (curPlayerStatus.has_action()) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);

			} else {
				// 不能换章,自动出牌
				if (table._playerStatus[_seat_index].is_bao_ting()) {
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
							TimeUnit.MILLISECONDS);
				} else {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();

				}
			}

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_GY table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards, seat_index);

		if (seat_index == _seat_index) {
			if (table.player_duan[_seat_index] != -1) {
				if (table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)) {
					table._logic.remove_card_by_data(hand_cards, _send_card_data);
				} else {
					boolean add_flag = false;
					for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
						if (table.GRR._cards_index[_seat_index][i] > 0
								&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[_seat_index]) {
							add_flag = true;
							break;
						}
					}

					if (add_flag) {
						table._logic.remove_card_by_data(hand_cards, _send_card_data + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG);
					} else {
						table._logic.remove_card_by_data(hand_cards, _send_card_data);
					}
				}
			} else {
				table._logic.remove_card_by_data(hand_cards, _send_card_data);
			}
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
		//
		// for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
		// tableResponse.addCardsData(hand_cards[i]);
		// }

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
				roomResponse.addDouliuzi(table._playerStatus[seat_index]._hu_out_cards_fan[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// table.send_response_to_player(seat_index, roomResponse);

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

		boolean hand_have_duan_card = false;
		if (table.player_duan[seat_index] != -1) {
			if (seat_index == _seat_index && table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)) {
				hand_have_duan_card = true;
			} else {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++)
					if (table.GRR._cards_index[seat_index][i] > 0
							&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[seat_index]) {
						hand_have_duan_card = true;
						break;
					}
			}
		}
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		boolean flag = true;
		for (int player = 0; player < table.getTablePlayerNumber(); player++) {
			if (table._playerStatus[player].has_action() && table._playerStatus[player].is_respone() == false
					&& table._playerStatus[player].has_action_by_code(GameConstants_MYGY.WIK_DING_WANG)) {
				flag = false;
				break;
			}
		}

		if (flag && seat_index == _seat_index) {
			if (hand_have_duan_card) {
				if (table.player_duan[_seat_index] == table._logic.get_card_color(_send_card_data)) {
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index);
				} else {
					table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG },
							seat_index);
				}
			} else {
				table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);
			}
		}

		return true;
	}
}
