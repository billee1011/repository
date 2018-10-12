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
import com.cai.game.mj.handler.MJHandlerOutCardOperate;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerOutCardOperate_LuZhouGui extends MJHandlerOutCardOperate<AbstractSiChuanMjTable> {
	@Override
	public void exe(AbstractSiChuanMjTable table) {
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		table._current_player = _out_card_player;

		int next_player = table.get_next_seat(_out_card_player);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_out_card_player], cards,
				table.ding_que_pai_se[_out_card_player]);

		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		int real_card = _out_card_data;
		if (real_card == table.magicCard)
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player, 0);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);

		boolean bAroseAction = (_out_card_data != table.magicCard) && table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}

			table.operate_player_action(_out_card_player, true);

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];

				if (playerStatus.has_action()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
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

		if (operate_code == GameConstants.WIK_SUO_PENG_1 || operate_code == GameConstants.WIK_SUO_PENG_2) {
			if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(GameConstants.WIK_PENG) == false) {
				table.log_player_error(seat_index, "出牌操作,没有动作");
				return true;
			}
		} else if (operate_code == GameConstants.WIK_SUO_GANG_1 || operate_code == GameConstants.WIK_SUO_GANG_2
				|| operate_code == GameConstants.WIK_SUO_GANG_3) {
			if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(GameConstants.WIK_GANG) == false) {
				table.log_player_error(seat_index, "出牌操作,没有动作");
				return true;
			}
		} else {
			if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
				table.log_player_error(seat_index, "出牌操作,没有动作");
				return true;
			}
		}

		if (playerStatus.is_respone() == false) {
			playerStatus.operate(operate_code, operate_card);
		}
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_CHI_HU) {
			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_out_card_player].opr_or_long(Constants_SiChuan.CHR_FANG_PAO);
			table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { Constants_SiChuan.CHR_FANG_PAO }, 1,
					GameConstants.INVALID_SEAT);

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_PENG)) {
				table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
			}
		}

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_empty();

			if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}

			// 记录过胡的时候，牌型的番数，变大了，本圈才能接炮
			table.score_when_abandoned_win[seat_index] = table.score_when_win[seat_index];
		}

		int target_player = seat_index;
		int target_action = operate_code;

		// 有胡时按通炮玩法
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank_luzhougui(table._playerStatus[i].get_perform(), table.hasRuleDuanGang)
							+ table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank_luzhougui(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action_weaves, table.hasRuleDuanGang) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank_luzhougui(table._playerStatus[target_player].get_perform(),
							table.hasRuleDuanGang) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank_luzhougui(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action_weaves, table.hasRuleDuanGang) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		operate_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		boolean has_gang = table._playerStatus[target_player].has_action_by_code(GameConstants.WIK_GANG);

		switch (target_action) {
		case GameConstants.WIK_PENG:
		case GameConstants.WIK_SUO_PENG_1:
		case GameConstants.WIK_SUO_PENG_2: {
			int cbRemoveCard[] = null;
			if (target_action == GameConstants.WIK_PENG) {
				cbRemoveCard = new int[] { operate_card, operate_card };
			} else if (target_action == GameConstants.WIK_SUO_PENG_1) {
				cbRemoveCard = new int[] { table.magicCard, operate_card };
			} else if (target_action == GameConstants.WIK_SUO_PENG_2) {
				cbRemoveCard = new int[] { table.magicCard, table.magicCard };
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			int chi_peng_type = GameConstants.CHI_PENG_TYPE_OUT_CARD;
			if (has_gang) {
				chi_peng_type = GameConstants.CHI_PENG_TYPE_ABANDONED_GANG;
				table.cur_round_abandoned_gang = _out_card_data;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, chi_peng_type);
		}
			break;
		case GameConstants.WIK_GANG:
		case GameConstants.WIK_SUO_GANG_1:
		case GameConstants.WIK_SUO_GANG_2:
		case GameConstants.WIK_SUO_GANG_3: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_gang(target_player, _out_card_player, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants.WIK_NULL: {
			_current_player = table._current_player = table.get_next_seat(_out_card_player);

			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.table_hu_cards[table.table_hu_card_count++] = operate_card;

			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.operate_remove_discard(_out_card_player, table.GRR._discard_count[_out_card_player]);

			int jie_pao_count = 0;
			int last_win_player = -1;

			WeaveItem weave_item = null;
			if (_type == GameConstants.WIK_GANG) {
				weave_item = table.GRR._weave_items[_out_card_player][table.gang_pai_weave_index];
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int next_player = table.get_banker_next_seat(_out_card_player + i);
				if (table.GRR._chi_hu_rights[next_player].is_valid() && !table.had_hu_pai[next_player]) {
					last_win_player = next_player;

					jie_pao_count++;
					table.left_player_count--;
					table.win_order[next_player] = table.getTablePlayerNumber() - table.left_player_count;
					table.win_type[next_player] = table.JIE_PAO_HU;

					if (_type == GameConstants.WIK_GANG) {
						// 杠上炮了，记录杠上跑的接炮人
						weave_item.gang_jie_pao_seat = next_player;
					}

					table.dian_pao_order[_out_card_player][table.dian_pao_count[_out_card_player]++] = table.win_order[next_player];

					table.whoProvided[next_player] = _out_card_player;
				}
			}

			if (jie_pao_count > 0) {
				if (jie_pao_count > 1) {
					if (_type == GameConstants.WIK_GANG) {
						if (weave_item.type == GameConstants.GANG_TYPE_AN_GANG) {
							table.an_gang_count[_out_card_player]--;
						}
						if (weave_item.type == GameConstants.GANG_TYPE_JIE_GANG) {
							table.zhi_gang_count[_out_card_player]--;
							table.dian_gang_count[weave_item.provide_player]--;
						}
						if (weave_item.type == GameConstants.GANG_TYPE_ADD_GANG && weave_item.is_vavild) {
							table.wan_gang_count[_out_card_player]--;
						}

						// 杠上炮，2人或3人，杠失效，不再计算分
						weave_item.is_vavild = false;
					}
				}

				if (jie_pao_count > 1) {
					if (table.left_player_count + jie_pao_count == table.getTablePlayerNumber()) {
						table.next_banker_player = _out_card_player;
					}
				} else {
					if (table.left_player_count == table.getTablePlayerNumber() - 1) {
						table.next_banker_player = target_player;
					}
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}

					if (table.had_hu_pai[i] == false) {
						table.had_hu_pai[i] = true;

						table.GRR._chi_hu_card[i][0] = operate_card;

						table.operate_player_get_card(i, 1, new int[] { table.GRR._chi_hu_card[i][0] }, i);

						table.process_chi_hu_player_operate(i, operate_card, false);
						table.process_chi_hu_player_score(i, _out_card_player, operate_card, false);

						table._player_result.jie_pao_count[i]++;
						table._player_result.dian_pao_count[_out_card_player]++;
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
			table.process_chi_hu_player_operate_reconnect(seat_index, _out_card_data, false);
		} else {
			MahjongUtils.showTingPai(table, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		// 处理断线重连时，胡牌人的胡牌显示
		table.process_duan_xian_chong_lian(seat_index);

		return true;
	}
}
