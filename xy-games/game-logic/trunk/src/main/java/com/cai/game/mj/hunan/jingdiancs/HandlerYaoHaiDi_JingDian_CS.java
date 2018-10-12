package com.cai.game.mj.hunan.jingdiancs;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_JingDian_CS;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerYaoHaiDi;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerYaoHaiDi_JingDian_CS extends MJHandlerYaoHaiDi<Table_JingDian_CS> {
	private static Logger logger = Logger.getLogger(HandlerYaoHaiDi_JingDian_CS.class);

	@Override
	public void exe(Table_JingDian_CS table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table.is_hai_di = true;

		table._cur_banker = _seat_index;

		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x05;
		}
		table._provide_player = _seat_index;

		table._send_card_data = _send_card_data;

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_YAO_HAI_DI }, 0,
				GameConstants.INVALID_SEAT);
		table.operate_out_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		boolean has_action = false;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, Constants_JingDian_CS.HU_CARD_TYPE_ZI_MO, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

			chr.opr_or_jd_cs(Constants_JingDian_CS.CHR_HAI_DI_LAO_YUE);

			has_action = true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index) {
				continue;
			}

			chr = table.GRR._chi_hu_rights[i];
			chr.set_empty();
			action = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], _send_card_data, chr,
					Constants_JingDian_CS.HU_CARD_TYPE_JIE_PAO, i);

			if (action != GameConstants.WIK_NULL) {
				table._playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				table._playerStatus[i].add_chi_hu(_send_card_data, _seat_index);

				chr.opr_or_jd_cs(Constants_JingDian_CS.CHR_HAI_DI_PAO);

				has_action = true;
			}

		}

		if (has_action == false) {
			GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _seat_index, 1, new int[] { _send_card_data }, true, table.getMaxCount()), 1,
					TimeUnit.SECONDS);
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW), 2, TimeUnit.SECONDS);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				curPlayerStatus = table._playerStatus[i];
				if (curPlayerStatus.has_action()) {
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_JingDian_CS table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			logger.error("[要海底],操作失败," + seat_index + "玩家操作已失效");
			return true;
		}

		if (playerStatus.is_respone()) {
			logger.error("[要海底],操作失败," + seat_index + "玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			logger.error("[要海底],操作失败," + seat_index + "没有动作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);
			if (table._playerStatus[seat_index].has_chi_hu()) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}
		}

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
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		if (target_action != GameConstants.WIK_ZI_MO) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}
		}

		int target_card = table._playerStatus[target_player]._operate_card;

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

			GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _seat_index, 1, new int[] { _send_card_data }, true, table.getMaxCount()), 1,
					TimeUnit.SECONDS);
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), 2, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}

			table.set_niao_card(_seat_index, true, true);
			if (table._out_card_count == 0) {
				table._provide_player = _seat_index;
				table._provide_card = target_card;
			}
			table.GRR._chi_hu_card[_seat_index][0] = target_card;
			table.process_chi_hu_player_operate(_seat_index, new int[] { _send_card_data }, 1, false);
			table.process_chi_hu_player_score(_seat_index, _seat_index, _send_card_data, true);

			table._player_result.zi_mo_count[_seat_index]++;

			table._player_result.da_hu_zi_mo[_seat_index]++;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}
			table._cur_banker = _seat_index;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_JingDian_CS.CHR_FANG_PAO);

			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}
				jie_pao_count++;
			}
			if (jie_pao_count > 1) {
				table._cur_banker = _seat_index;
				table.set_niao_card(_seat_index, true, true);
			} else {
				table._cur_banker = target_player;
				table.set_niao_card(target_player, true, true);
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table.process_chi_hu_player_operate(i, new int[] { _send_card_data }, 1, false);
				table.process_chi_hu_player_score(i, _seat_index, _send_card_data, false);

				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_seat_index]++;

				table._player_result.da_hu_jie_pao[i]++;
				table._player_result.da_hu_dian_pao[_seat_index]++;
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
				table.operate_player_action(i, true);
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			return true;
		}
		default:
			return false;
		}
	}

	@Override
	public boolean handler_player_be_in_room(Table_JingDian_CS table, int seat_index) {
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
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

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_YAO_HAI_DI }, 0,
				seat_index);
		table.operate_out_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.OUT_CARD_TYPE_LEFT, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
