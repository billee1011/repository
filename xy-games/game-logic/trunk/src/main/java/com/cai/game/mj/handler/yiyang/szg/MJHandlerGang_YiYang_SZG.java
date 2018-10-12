package com.cai.game.mj.handler.yiyang.szg;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_YiYang_SZG;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_YiYang_SZG extends MJHandlerGang<MJTable_YiYang_SZG> {
	@Override
	public void exe(MJTable_YiYang_SZG table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid(); // 可以胡了

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
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
	public boolean handler_operate_card(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
		if (table.has_rule(GameConstants_YiYang_SZG.GAME_RULE_YI_PAO_DUO_XIANG)) {
			return handler_operate_card_tong_pao(table, seat_index, operate_code, operate_card);
		} else {
			return handler_operate_card_bu_tong_pao(table, seat_index, operate_code, operate_card);
		}
	}

	// 通炮玩法
	public boolean handler_operate_card_tong_pao(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);

			table._playerStatus[seat_index].chi_hu_round_invalid();
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			table.process_chi_hu_player_operate(seat_index, _center_card, false);
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			if (table.is_bao_ting[seat_index]) {
				table.add_guo_pai(_center_card, seat_index);
				table.ting_state_pass_jie_pao[seat_index] = true;
			}

			table.GRR._chi_hu_rights[seat_index].set_empty();
		}

		// 通炮玩法
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		// 通炮玩法
		int target_player = seat_index;
		@SuppressWarnings("unused")
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

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		
		operate_card = _center_card;

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}

			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			if (jie_pao_count > 1) {
				table._cur_banker = _seat_index;
			} else {
				table._cur_banker = target_player;
			}

			// table.set_niao_card(table._cur_banker,
			// GameConstants.INVALID_VALUE, true, 0);
			// // 抓鸟
			table.set_niao_card_fei(table._cur_banker, GameConstants.INVALID_VALUE, true, 0); // 飞鸟

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);

				table.GRR._chi_hu_card[i][0] = operate_card;

				table.process_chi_hu_player_score(i, _seat_index, operate_card, false);

				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_seat_index]++;
			}

			table.display_bird_cards();

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), table.YY_GAME_FINISH_DELAY,
					TimeUnit.MILLISECONDS);
		} else {
			exe_gang(table);
		}

		return true;
	}

	// 不通炮玩法
	public boolean handler_operate_card_bu_tong_pao(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

		}

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			if (table.is_bao_ting[seat_index]) {
				table.add_guo_pai(_center_card, seat_index);
				table.ting_state_pass_jie_pao[seat_index] = true;
			}

			table.GRR._chi_hu_rights[seat_index].set_empty();
		}

		// 不通炮的算法
		int target_player = seat_index;
		@SuppressWarnings("unused")
		int target_action = operate_code;
		int target_p = 0;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
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

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		
		operate_card = _center_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player) {
				table.GRR._chi_hu_rights[i].set_valid(true);
			} else {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
		}

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}

			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			table._cur_banker = target_player;

			table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true, 0); // 抓鸟
			table.set_niao_card_fei(target_player, GameConstants.INVALID_VALUE, true, 0); // 飞鸟

			table.GRR._chi_hu_rights[target_player].opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score(target_player, _seat_index, operate_card, false);

			table.display_bird_cards();

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), table.YY_GAME_FINISH_DELAY,
					TimeUnit.MILLISECONDS);
		} else {
			exe_gang(table);
		}

		return true;
	}

	@Override
	protected boolean exe_gang(MJTable_YiYang_SZG table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(_provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;

					_provide_player = _seat_index;

					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		// 设置用户
		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_YI_ZI_CHUI_XI)) {
			// 执行杠牌动作之后。一字撬有喜
			if (!table.has_caculated_xi_fen[_seat_index] && hand_card_count == 1) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index) {
						continue;
					}

					table._player_result.game_score[i] -= 2;
					table._player_result.game_score[_seat_index] += 2;

					table.happy_win_score[i] -= 2;
					table.happy_win_score[_seat_index] += 2;
				}

				table.operate_player_info();
				table.has_caculated_xi_fen[_seat_index] = true;
			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				int score = GameConstants.CELL_SCORE;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= 2 * score;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 2 * score;// 一共加分
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			int score = GameConstants.CELL_SCORE;

			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += (table.getTablePlayerNumber() - 1) * score;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] -= (table.getTablePlayerNumber() - 1) * score;

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				int score = GameConstants.CELL_SCORE;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
			}

			table._player_result.ming_gang_count[_seat_index]++;
		}

		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, GameConstants.WIK_GANG, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_YiYang_SZG table, int seat_index) {
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
			tableResponse.addTrustee(table.is_bao_ting[i]);

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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int cards[] = new int[GameConstants.MAX_COUNT];
		@SuppressWarnings("unused")
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
