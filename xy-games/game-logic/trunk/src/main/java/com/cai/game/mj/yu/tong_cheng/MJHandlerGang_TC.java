package com.cai.game.mj.yu.tong_cheng;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.GameConstants_TC;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_TC extends MJHandlerGang<Table_TC> {

	private boolean exe_cai_gang;

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if (GameConstants_TC.TC_GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		_self = self;
		_double = d;
		exe_cai_gang = false;
	}

	@Override
	public void exe(Table_TC table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid(); // 可以胡了

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_TC.WIK_GANG }, 1,
				GameConstants.INVALID_SEAT);

		if ((GameConstants_TC.TC_GANG_TYPE_AN_GANG == _type) || (GameConstants_TC.TC_GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

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
	public boolean handler_operate_card(Table_TC table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU)) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			if (exe_cai_gang) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_action()))
						return false;
				}

				table.exe_dispatch_card(_seat_index, _type, 0);
				return true;
			}
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);

			if (table._playerStatus[seat_index].has_chi_hu()) {
				table._playerStatus[seat_index]._check_chi_pen_hu = true;
				table._playerStatus[seat_index].chi_hu_round_invalid();
				table._playerStatus[seat_index].add_cards_abandoned_hu(_center_card);
			}
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

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
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		operate_card = _center_card;

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
			if (jie_pao_count > 1) {
				table._cur_banker = _seat_index;
			} else if (jie_pao_count == 1) {
				table._cur_banker = target_player;
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table.GRR._chi_hu_card[i][0] = _center_card;
				table.process_chi_hu_player_operate(i, _center_card, false);
				table.process_chi_hu_player_score(i, _seat_index, _center_card, false);

				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_provide_player]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		} else {
			this.exe_gang(table);
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(Table_TC table, int seat_index) {
		if (!table.istrustee[seat_index])
			return false;

		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];

		if (curPlayerStatus.has_chi_hu() && _center_card != GameConstants.INVALID_VALUE) {
			// 有接炮就胡牌
			table.operate_player_action(seat_index, true);

			table.exe_jian_pao_hu(seat_index, GameConstants.WIK_CHI_HU, _center_card);

			return true;
		} else {
			if (exe_cai_gang) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_action()))
						return false;
				}

				table.exe_dispatch_card(_seat_index, _type, 0);
				return true;
			}
			// 别人出牌后，有吃、碰、接杠，等待3秒，如果3秒之内点了‘吃碰杠’操作，进行‘吃碰杠’动作并自动取消托管
			if (curPlayerStatus.has_action() && curPlayerStatus.is_respone() == false) {
				table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(seat_index, false);

				// 添加定时任务，3秒之内点了操作，取消定时任务
				table._trustee_schedule[seat_index] = GameSchedule.put(new Runnable() {
					@Override
					public void run() {
						// 关闭操作按钮
						table.operate_player_action(seat_index, true);

						table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, _center_card);
					}
				}, GameConstants_KWX.DELAY_JIAN_PAO_HU_NEW, TimeUnit.MILLISECONDS);
			} else {
				// 没接炮就过牌
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, _center_card);
			}
			return true;
		}
	}

	@Override
	protected boolean exe_gang(Table_TC table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants_TC.TC_GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants_TC.TC_GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants_TC.TC_GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;
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
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type;

		// TODO: 回头杠时，提供者不更新，暗杠明杠才更新
		if (GameConstants_TC.TC_GANG_TYPE_ADD_GANG != _type) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		}

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR.getWeaveItemsForOut(_seat_index, new WeaveItem[GameConstants.MAX_WEAVE]));

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants_TC.TC_GANG_TYPE_AN_GANG == _type) {
			int score = 2;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = score;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants_TC.TC_GANG_TYPE_JIE_GANG == _type) {
			int feng = (table.getTablePlayerNumber() - 1) * 1;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = feng;

			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = feng * GameConstants.CELL_SCORE;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -feng * GameConstants.CELL_SCORE;

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants_TC.TC_GANG_TYPE_ADD_GANG == _type) {
			int feng = GameConstants.CELL_SCORE;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = feng;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -feng;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += feng;
			}

			table._player_result.ming_gang_count[_seat_index]++;
		}

		table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[_seat_index]._hu_cards,
				table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}

		if (GameConstants_TC.TC_GANG_TYPE_AN_GANG == _type && table.check_cai_gang(_seat_index)) {
			exe_cai_gang = true;
			return table.exe_cai_gang(_seat_index, _center_card);
		}

		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TC table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			tableResponse.addTrustee(table.istrustee[i]);

			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int tmpCard = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(tmpCard))
					tmpCard += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(tmpCard);
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
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		@SuppressWarnings("unused")
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
				tableResponse.addCardsData(hand_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				tableResponse.addCardsData(hand_cards[i]);
			}
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
