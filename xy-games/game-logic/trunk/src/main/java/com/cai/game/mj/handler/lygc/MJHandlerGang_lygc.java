package com.cai.game.mj.handler.lygc;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_lygc extends MJHandlerGang<MJTable> {
	@Override
	public void exe(MJTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (!table.LYGC_CI_STATE) {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants.INVALID_SEAT);
		}

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

		boolean bAroseAction = false;
		if (table.has_rule(GameConstants.GAME_RULE_HENAN_QIANG_GANG_HU)) {
			bAroseAction = table.estimate_gang_respond_henan_lygc(_seat_index, _center_card);
		}

		if (bAroseAction == false) {
			this.exe_gang(table);
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
	public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);

			table._playerStatus[seat_index].chi_hu_round_invalid();
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		table.operate_player_action(seat_index, true);

		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
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

		operate_card = _center_card;

		if (target_action == GameConstants.WIK_NULL) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
			this.exe_gang(table);
			return true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player) {
				table.GRR._chi_hu_rights[i].set_valid(true);
			} else {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
		}

		table.process_chi_hu_player_operate(target_player, _center_card, false);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			table._cur_banker = target_player;

			table.GRR._chi_hu_card[target_player][0] = _center_card;

			table.process_chi_hu_player_score_henan_lygc(target_player, _seat_index, _center_card, GameConstants.HU_CARD_TYPE_PAOHU, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					table.game_finish_delay_lygc, TimeUnit.SECONDS);
		}

		return true;
	}

	@Override
	protected boolean exe_gang(MJTable table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		int bao_ci_change = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			bao_ci_change = 2;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
			bao_ci_change = 1;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;
					_provide_player = table.GRR._weave_items[_seat_index][i].provide_player;// 找到放碰到人
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
			bao_ci_change = 1;
		}

		if (table.has_rule(GameConstants.GAME_RULE_HENAN_BAOCI) && bao_ci_change != GameConstants.INVALID_SEAT
				&& table._bao_ci_start != GameConstants.LYGC_BAO_CI_SATRT) {
			table._bao_ci_start -= bao_ci_change;
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_ci_card(table._logic.switch_to_card_index(cards[j]))
					&& (table.is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI) || table.is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			int center_card = table.GRR._weave_items[_seat_index][i].center_card;
			if (table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < table.getTablePlayerNumber(); j++) {
					for (int m = 0; m < table.GRR._discard_count[j]; m++) {
						if (center_card == table.GRR._discard_cards[j][m]) {
							center_card += GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER;
						}
					}
				}
			}

			if (table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_GANG) {
				center_card += GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER;
			}

			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		if (!table.has_rule(GameConstants.GAME_RULE_HENAN_YCI)) {
			table._playerStatus[_seat_index]._hu_card_count = table.get_henan_ting_card_lygc(table._playerStatus[_seat_index]._hu_cards,
					table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}
		}

		boolean gang_pao = table.has_rule(GameConstants.GAME_RULE_HENAN_GANG_PAO);

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				int score = table.getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_GANG);

				if (gang_pao) {
					score += table._player_result.pao[i] + table._player_result.pao[_seat_index];
				}

				if (table.has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI)) {
					// 庄家翻倍，杠也要翻倍
					if (i == table._cur_banker || _seat_index == table._cur_banker) {
						score *= 2;
					}
				}

				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;

				// 新增结算页面的数据
				table.GRR.an_gang_score[_seat_index] += score;
				table.GRR.an_gang_score[i] -= score;
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {

			int score = table.getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_GANG);

			if (gang_pao) {
				score += table._player_result.pao[_provide_player] + table._player_result.pao[_seat_index];
			}

			if (table.has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI)) {
				// 庄家翻倍，杠也要翻倍
				if (_provide_player == table._cur_banker || _seat_index == table._cur_banker) {
					score *= 2;
				}
			}

			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -score;
			table._player_result.ming_gang_count[_seat_index]++;

			// 新增结算页面的数据
			table.GRR.ming_gang_score[_seat_index] += score;
			table.GRR.ming_gang_score[_provide_player] -= score;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			int score = table.getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_GANG);

			if (gang_pao) {
				score += table._player_result.pao[_provide_player] + table._player_result.pao[_seat_index];
			}

			if (table.has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI)) {
				// 庄家翻倍，杠也要翻倍
				if (table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player == table._cur_banker || _seat_index == table._cur_banker) {
					score *= 2;
				}
			}

			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player] = -score;

			table._player_result.ming_gang_count[_seat_index]++;

			// 新增结算页面的数据
			table.GRR.ming_gang_score[_seat_index] += score;
			table.GRR.ming_gang_score[_provide_player] -= score;
		}

		// 杠次
		if (table.is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI) || table.is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)) {
			int card = table._logic.switch_to_card_data(table._logic.get_ci_card_index());
			boolean bAroseAction = table.estimate_lygc_gang_ci(_seat_index, card);
			if (bAroseAction && table.LYGC_CI_STATE) {
				// 还原杠次状态
				table.LYGC_CI_STATE = false;

				if (table.GRR._banker_player != _seat_index) {
					table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				} else {
					table._cur_banker = table.GRR._banker_player;
				}

				ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
				chr.set_valid(true);

				int provide_player = GameConstants.INVALID_SEAT;
				boolean bao_ci = true;

				if (GameConstants.GANG_TYPE_AN_GANG == _type) {
					table._player_result.jie_pao_count[_seat_index]++;
					bao_ci = false;

					// 暗次次数
					table._player_result.an_ci_ci_shu[_seat_index]++;

					provide_player = _seat_index;

					// 暗次动画
					chr.opr_or(GameConstants.CHR_HENAN_HZ_DUAN_2);
				} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
					table._player_result.jie_pao_count[_seat_index]++;
					table._player_result.dian_pao_count[_provide_player]++;

					provide_player = _provide_player;

					if (table._bao_ci_state == GameConstants.LYGC_BAO_CI_SATRT) {
						// 包次次数
						table._player_result.bao_ci_ci_shu[_provide_player]++;

						// 包次动画
						chr.opr_or(GameConstants.CHR_HENAN_HZ_QISHOU_HU);
					} else {
						// 明次次数
						table._player_result.ming_ci_ci_shu[_seat_index]++;

						// 明次动画
						chr.opr_or(GameConstants.CHR_HENAN_HZ_DUAN_1);
					}

				} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
					table._player_result.jie_pao_count[_seat_index]++;
					table._player_result.dian_pao_count[table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player]++;

					provide_player = table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player;

					bao_ci = false;

					// 明次次数
					table._player_result.ming_ci_ci_shu[_seat_index]++;

					// 明次动画
					chr.opr_or(GameConstants.CHR_HENAN_HZ_DUAN_1);
				}

				table._cur_banker = _seat_index;
				table.GRR._chi_hu_card[_seat_index][0] = card;

				table.process_chi_hu_player_operate(_seat_index, card, false);
				table.process_chi_hu_player_score_henan_lygc(_seat_index, provide_player, card, GameConstants.HU_CARD_TYPE_GANG_CI, bao_ci);

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						table.game_finish_delay_lygc, TimeUnit.SECONDS);

				return true;
			}
		}

		table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {
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
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._discard_cards[i][j]))) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);

			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._weave_items[i][j].center_card))) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG) {
					for (int k = 0; k < table.getTablePlayerNumber(); k++) {
						for (int m = 0; m < table.GRR._discard_count[k]; m++) {
							if (table.GRR._weave_items[i][j].center_card == table.GRR._discard_cards[k][m]) {
								weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
							}
						}
					}
				}

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
				}

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
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_ci_card(table._logic.switch_to_card_index(cards[j]))
					&& (table.is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI) || table.is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
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
