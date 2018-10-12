package com.cai.game.mj.hunan.zhuzhouwang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ZhuZhouWang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerQiShouHu_ZhuZhouWang extends AbstractMJHandler<Table_ZhuZhouWang> {
	boolean have_qi_shou_hu = false;

	/**
	 * 1 = 庄家起手14张，出牌之前；2 = 庄家起手14张，并打出第一张牌之后；
	 */
	protected int _type;
	protected int _seat_index;
	protected int _card_data;

	/**
	 * 报听的时候，类型是什么，座位好是多少，出的牌是什么
	 * 
	 * @param _seat_index
	 * @param _card_data
	 */
	public void reset_status(int _type, int _seat_index, int _card_data) {
		this._type = _type;
		this._seat_index = _seat_index;
		this._card_data = _card_data;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_ZhuZhouWang table) {
		// 下一局这个值会保留,需要初始化
		have_qi_shou_hu = false;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table._cur_banker) {
				continue;
			}

			PlayerStatus curPlayerStatus = table._playerStatus[i];
			curPlayerStatus.reset();

			ChiHuRight chr = table.GRR._chi_hu_rights[i];
			chr.set_empty();

			// 自摸时的胡牌检测
			int action = table.analyse_qi_shou_hu_card(table.GRR._cards_index[i], chr,
					GameConstants_ZhuZhouWang.HU_CARD_TYPE_QI_SHOU_HU, i);

			if (action == GameConstants.WIK_NULL) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);

			}
			if (GameConstants.WIK_NULL != action) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(table._send_card_data, i);
			} else {
				chr.set_empty();
			}

			if (curPlayerStatus.has_action()) {
				table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
				have_qi_shou_hu = true;
			} else {
				// table.change_player_status(_seat_index,
				// GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		if (!have_qi_shou_hu) {
			table.exe_bao_ting(1, table._cur_banker, GameConstants.INVALID_VALUE);
		}

	}

	@Override
	public boolean handler_operate_card(Table_ZhuZhouWang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "起手胡,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "起手胡,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "起手胡操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_ZI_MO) {
			// table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// if ((table._playerStatus[i].is_respone() == false) &&
		// (table._playerStatus[i].has_chi_hu()))
		// return false;
		// }

		int target_player = seat_index;
		int target_action = operate_code;

		int cbTargetActionRank = 0;
		for (int p = 1; p < table.getTablePlayerNumber(); p++) {
			int i = (table._cur_banker + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) - p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					cbTargetActionRank = cbUserActionRank;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		// int target_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

//		if (operate_code == GameConstants.WIK_ZI_MO) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(true);
//		}

		switch (target_action) {

		case GameConstants.WIK_NULL: {
			table.exe_bao_ting(1, table._cur_banker, GameConstants.INVALID_VALUE);
			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table._cur_banker = target_player;

			table.GRR._chi_hu_card[target_player][0] = 0;
			table._player_result.zi_mo_count[target_player]++;

			table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true);

			table.process_chi_hu_player_operate(target_player, 0, false);
			table.process_chi_hu_player_score(target_player, target_player, 0, true);

			table._player_result.men_qing[target_player]++;

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
		}

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

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin

		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
