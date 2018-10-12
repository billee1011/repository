package com.cai.game.mj.sichuan.luzhougui;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.MJHandlerGang;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerGang_LuZhouGui extends MJHandlerGang<AbstractSiChuanMjTable> {
	@Override
	public void exe(AbstractSiChuanMjTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table.score_when_abandoned_win[_seat_index] = 0;

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_GANG }, 1,
				GameConstants.INVALID_SEAT);

		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG) {
			exe_gang(table);
			return;
		}

		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	protected boolean exe_gang(AbstractSiChuanMjTable table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		int color = table._logic.get_card_color(_center_card);

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.an_gang_count[_seat_index]++;

			table.gang_fan[_seat_index] += (color == 3) ? 3 : 2;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

			table.zhi_gang_count[_seat_index]++;
			table.dian_gang_count[_provide_player]++;

			table.gang_fan[_seat_index] += (color == 3) ? 2 : 1;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG || cbWeaveKind == GameConstants.WIK_SUO_PENG_1
						|| cbWeaveKind == GameConstants.WIK_SUO_PENG_2)) {
					cbWeaveIndex = i;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

			// 及时雨，弯杠不直接杠，不计算分
			if (_center_card != table._send_card_data) {
				table.GRR._weave_items[_seat_index][cbWeaveIndex].is_vavild = false;
			} else {
				table.wan_gang_count[_seat_index]++;
			}

			table.gang_fan[_seat_index] += (color == 3) ? 2 : 1;
		}

		table.gang_pai_weave_index = cbWeaveIndex;

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type; // 记录暗杠-明杠-弯杠的类型

		// 回头杠时，提供者不更新，暗杠明杠才更新
		if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		}

		table._current_player = _seat_index;

		if (_action == GameConstants.WIK_GANG) {
			table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		} else {
			if (GameConstants.GANG_TYPE_AN_GANG == _type) {
				if (_action == GameConstants.WIK_SUO_GANG_1) {
					table.GRR._cards_index[_seat_index][cbCardIndex] -= 3;
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 1;
				}
				if (_action == GameConstants.WIK_SUO_GANG_2) {
					table.GRR._cards_index[_seat_index][cbCardIndex] -= 2;
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 2;
				}
				if (_action == GameConstants.WIK_SUO_GANG_3) {
					table.GRR._cards_index[_seat_index][cbCardIndex] -= 1;
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 3;
				}
			}
			if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
				if (_action == GameConstants.WIK_SUO_GANG_1) {
					table.GRR._cards_index[_seat_index][cbCardIndex] -= 2;
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 1;
				}
				if (_action == GameConstants.WIK_SUO_GANG_2) {
					table.GRR._cards_index[_seat_index][cbCardIndex] -= 1;
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 2;
				}
				if (_action == GameConstants.WIK_SUO_GANG_3) {
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 3;
				}
			}
			if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
				int count = table.GRR._cards_index[_seat_index][cbCardIndex];
				if (count > 0)
					table.GRR._cards_index[_seat_index][cbCardIndex] -= 1;
				else
					table.GRR._cards_index[_seat_index][table.magicCardIndex] -= 1;
			}
		}

		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
				table.ding_que_pai_se[_seat_index]);

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			table._playerStatus[p]._hu_card_count = table.get_ting_card(table._playerStatus[p]._hu_cards, table.GRR._cards_index[p],
					table.GRR._weave_items[p], table.GRR._weave_count[p], p, 0);
			int ting_cards[] = table._playerStatus[p]._hu_cards;
			int ting_count = table._playerStatus[p]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(p, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(p, 1, ting_cards);
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index)
				continue;

			if (table.win_order[i] != 0)
				continue;

			table.GRR._weave_items[_seat_index][cbWeaveIndex].gang_gei_fen_valid[i] = true;
		}

		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

	@Override
	public boolean handler_operate_card(AbstractSiChuanMjTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		if (playerStatus.is_respone() == false) {
			playerStatus.operate(operate_code, operate_card);
		}
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._win_order[seat_index] = 1;

			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_seat_index].opr_or_long(Constants_SiChuan.CHR_FANG_PAO);
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { Constants_SiChuan.CHR_FANG_PAO }, 1,
					GameConstants.INVALID_SEAT);

			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			table.GRR._chi_hu_rights[seat_index].set_empty();
		}

		// 通炮玩法
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		// 通炮玩法
		int target_player = seat_index;
		int target_action = operate_code;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
				}

				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i; // 最高级别人
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}

		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		operate_card = _center_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			exe_gang(table);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			boolean changed = false;
			if (table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_center_card)] == 0) {
				operate_card += GameConstants.CARD_ESPECIAL_TYPE_LU_ZHOU_GUI;
				changed = true;
			}

			table.table_hu_cards[table.table_hu_card_count++] = operate_card;

			if (changed) {
				int card_index = table._logic.switch_to_card_index(table.magicCard);
				table.GRR._cards_index[_seat_index][card_index]--;
			} else {
				int card_index = table._logic.switch_to_card_index(operate_card);
				table.GRR._cards_index[_seat_index][card_index]--;
			}

			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], hand_cards,
					table.ding_que_pai_se[_seat_index]);
			table.operate_player_cards(_seat_index, hand_card_count, hand_cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);

			int jie_pao_count = 0;
			int last_win_player = -1;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int next_player = table.get_banker_next_seat(_seat_index + i);
				if (table.GRR._chi_hu_rights[next_player].is_valid() && !table.had_hu_pai[next_player]) {
					last_win_player = next_player;

					jie_pao_count++;
					table.left_player_count--;
					table.win_order[next_player] = table.getTablePlayerNumber() - table.left_player_count;
					table.win_type[next_player] = table.JIE_PAO_HU;

					table.dian_pao_order[_seat_index][table.dian_pao_count[_seat_index]++] = table.win_order[next_player];

					table.whoProvided[next_player] = _seat_index;
				}
			}

			if (jie_pao_count > 0) {
				if (jie_pao_count > 1) {
					if (table.left_player_count + jie_pao_count == table.getTablePlayerNumber()) {
						table.next_banker_player = _seat_index;
					}
				} else {
					if (table.left_player_count == table.getTablePlayerNumber() - 1) {
						table.next_banker_player = target_player;
					}
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}

					if (table.had_hu_pai[i] == false) {
						table.had_hu_pai[i] = true;
						table.GRR._chi_hu_card[i][0] = operate_card;

						table.operate_player_get_card(i, 1, new int[] { table.GRR._chi_hu_card[i][0] }, i);

						table.process_chi_hu_player_operate(i, operate_card, false);
						table.process_chi_hu_player_score(i, _seat_index, operate_card, false);

						table._player_result.jie_pao_count[i]++;
						table._player_result.dian_pao_count[_seat_index]++;
					}
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
						if (!table.is_mj_type(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI)
								|| table.has_rule(Constants_SiChuan.GAME_RULE_TING_PAI_TS)) {
							if (table.had_hu_pai[i] == false && table._playerStatus[i]._hu_card_count > 0) {
								table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
							}
						}
					}

					table.exe_dispatch_card(table.get_next_seat(last_win_player), GameConstants.WIK_NULL, 0);
				}

				return true;
			}
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

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _center_card, false); // 效果
		} else {
			MahjongUtils.showTingPai(table, seat_index);

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		// 处理断线重连时，胡牌人的胡牌显示
		table.process_duan_xian_chong_lian(seat_index);

		return true;
	}
}
