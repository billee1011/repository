package com.cai.game.mj.yu.tong_cheng;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_TC;
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

public class MJHandler_CaiGang extends AbstractMJHandler<Table_TC> {

	private int _gang_seat_index;
	private int _gang_card_data;
	private boolean[] cai_right_gang;

	public void reset(int gang_seat_index, int gang_card_data, Table_TC table) {
		_gang_seat_index = gang_seat_index;
		_gang_card_data = gang_card_data;
		cai_right_gang = new boolean[table.getTablePlayerNumber()];
	}

	@Override
	public void exe(Table_TC table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action() && i != _gang_seat_index) {
				table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_TC table, int seat_index, int operate_code, int operate_card) {
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

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		playerStatus.operate(operate_code, operate_card);
		if (operate_code == GameConstants_TC.WIK_CAI_GANG) {
			check_cai_gang(seat_index, operate_card, table);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_action()))
				return false;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);

			table.operate_player_action(i, true);
		}

		boolean end_game = false;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (cai_right_gang[p]) {
				end_game = true;
			}
		}

		if (end_game) {
			int jie_pao_count = 0;
			int last_cai_right = -1;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (!cai_right_gang[i]) {
					continue;
				}
				jie_pao_count++;
				last_cai_right = i;
			}

			if (jie_pao_count > 0) {
				if (jie_pao_count > 1) {
					table._cur_banker = _gang_seat_index;
				} else if (jie_pao_count == 1) {
					table._cur_banker = last_cai_right;
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _gang_seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}

					table.process_chi_hu_player_score(i, _gang_seat_index, _gang_card_data, false);
					table.GRR._chi_hu_card[i][0] = _gang_card_data;

					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_gang_seat_index]++;
				}

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			}
			return true;
		} else {
			table.exe_dispatch_card(_gang_seat_index, GameConstants.WIK_NULL, 0);
			return true;
		}
	}

	public boolean check_cai_gang(int _seat_index, int target_card, Table_TC table) {
		ChiHuRight chr = new ChiHuRight();

		table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
				target_card, chr, GameConstants_TC.HU_CARD_TYPE_JIE_PAO, _seat_index);

		if (target_card == _gang_card_data) {
			table.GRR._chi_hu_rights[_seat_index] = chr;
			cai_right_gang[_seat_index] = true;
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_TC.WIK_CAI_GANG_SUCCESS },
					1, GameConstants.INVALID_SEAT);
			return true;
		} else {
			int wFanShu = table.getFanShu(_seat_index, chr);
			table._player_result.biaoyan[_seat_index] -= wFanShu;
			table._player_result.biaoyan[_gang_seat_index] += wFanShu;
			table._player_result.ziba[_seat_index] = -wFanShu;
			table._player_result.ziba[_gang_seat_index] = wFanShu;
			table._player_result.game_score[_seat_index] -= wFanShu;
			table._player_result.game_score[_gang_seat_index] += wFanShu;
			table.GRR._game_score[_seat_index] -= wFanShu;
			table.GRR._game_score[_gang_seat_index] += wFanShu;
			table.operate_player_data();
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_TC.WIK_CAI_GANG_FAILD },
					1, GameConstants.INVALID_SEAT);
		}

		return false;
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
		tableResponse.setCurrentPlayer(_gang_seat_index);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
