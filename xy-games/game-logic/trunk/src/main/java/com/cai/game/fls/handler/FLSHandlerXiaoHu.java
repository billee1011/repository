package com.cai.game.fls.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.game.fls.FLSTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class FLSHandlerXiaoHu extends FLSHandler {
	private int _current_player = GameConstants.INVALID_SEAT;

	public void reset_status(int seat_index) {
		_current_player = seat_index;
	}

	@Override
	public void exe(FLSTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			PlayerStatus playerStatus = table._playerStatus[i];
			if (playerStatus._action_count > 0) {
				table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
			}
		}
	}

	@Override
	public boolean handler_operate_card(FLSTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_xiao_hu() == false) {
			table.log_error("操作失败,玩家" + seat_index + "没有小胡");
			return false;
		}

		playerStatus.operate(operate_code, 0);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._start_hu_right[seat_index].set_empty();
		} else {
			ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

			start_hu_right.set_valid(true);

			show_xiao_hu(table, seat_index);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			playerStatus = table._playerStatus[i];
			if (playerStatus.has_xiao_hu() && playerStatus.is_respone() == false) {
				return false;
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		boolean has_xiao_hu = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._start_hu_right[i].is_valid()) {
				has_xiao_hu = true;
			}
		}

		if (has_xiao_hu == false) {
			table.runnable_xiao_hu();
		} else {
			GameSchedule.put(() -> {
				try {
					table.runnable_xiao_hu();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}, 5, TimeUnit.SECONDS);
		}

		return true;
	}

	private void show_xiao_hu(FLSTable table, int seat_index) {
		ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

		table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, start_hu_right.type_count, start_hu_right.type_list, 1,
				GameConstants.INVALID_SEAT, 0, true);

		int maxIndex = table.getMaxIndex();

		int cbCardIndexTemp[] = new int[maxIndex];
		for (int i = 0; i < maxIndex; i++) {
			cbCardIndexTemp[i] = table.GRR._cards_index[seat_index][i];
		}

		int hand_card_indexs[] = new int[maxIndex];
		int show_card_indexs[] = new int[maxIndex];

		for (int i = 0; i < maxIndex; i++) {
			hand_card_indexs[i] = cbCardIndexTemp[i];
		}

		if (start_hu_right._show_all) {
			for (int i = 0; i < maxIndex; i++) {
				show_card_indexs[i] = cbCardIndexTemp[i];
				hand_card_indexs[i] = 0;
			}
		} else {
			if (start_hu_right._index_da_si_xi != GameConstants.MAX_INDEX) {
				hand_card_indexs[start_hu_right._index_da_si_xi] = 0;
				show_card_indexs[start_hu_right._index_da_si_xi] = 4;
			}
			if ((start_hu_right._index_liul_liu_shun_1 != GameConstants.MAX_INDEX)
					&& (start_hu_right._index_liul_liu_shun_2 != GameConstants.MAX_INDEX)
					&& (start_hu_right._index_zt_lls_1 != GameConstants.MAX_INDEX)) {
				int count1 = hand_card_indexs[start_hu_right._index_liul_liu_shun_1];
				int count2 = hand_card_indexs[start_hu_right._index_liul_liu_shun_2];
				int count3 = hand_card_indexs[start_hu_right._index_zt_lls_1];

				hand_card_indexs[start_hu_right._index_liul_liu_shun_1] = count1 - 3 >= 0 ? count1 - 3 : 0;
				hand_card_indexs[start_hu_right._index_liul_liu_shun_2] = count2 - 3 >= 0 ? count2 - 3 : 0;
				hand_card_indexs[start_hu_right._index_zt_lls_1] = count3 - 3 >= 0 ? count3 - 3 : 0;

				int count4 = show_card_indexs[start_hu_right._index_liul_liu_shun_1];
				int count5 = show_card_indexs[start_hu_right._index_liul_liu_shun_2];
				int count6 = show_card_indexs[start_hu_right._index_zt_lls_1];

				show_card_indexs[start_hu_right._index_liul_liu_shun_1] = count4 + 3 >= 4 ? 4 : count4 + 3;
				show_card_indexs[start_hu_right._index_liul_liu_shun_2] = count5 + 3 >= 4 ? 4 : count5 + 3;
				show_card_indexs[start_hu_right._index_zt_lls_1] = count6 + 3 >= 4 ? 4 : count6 + 3;
			}
		}

		int cards[] = new int[GameConstants.MAX_FLS_COUNT];

		int hand_card_count = table._logic.switch_to_cards_data(hand_card_indexs, cards);
		table.operate_player_cards(seat_index, hand_card_count, cards, 0, null);

		hand_card_count = table._logic.switch_to_cards_data(show_card_indexs, cards);
		table.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		// 小胡及时算分
		int score = table.get_chi_hu_action_rank(table.GRR._start_hu_right[seat_index]);
		int lStartHuScore = score * GameConstants.CELL_SCORE;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (p == seat_index)
				continue;

			int s = lStartHuScore;

			table.GRR._start_hu_score[p] -= s;
			table.GRR._start_hu_score[seat_index] += s;

			table._player_result.game_score[p] -= s;
			table._player_result.game_score[seat_index] += s;
		}

		table.handler_refresh_all_player_data();
	}

	private void show_xiao_hu_reconnect(FLSTable table, int seat_index) {
		ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

		int maxIndex = table.getMaxIndex();

		int cbCardIndexTemp[] = new int[maxIndex];
		for (int i = 0; i < maxIndex; i++) {
			cbCardIndexTemp[i] = table.GRR._cards_index[seat_index][i];
		}

		int hand_card_indexs[] = new int[maxIndex];
		int show_card_indexs[] = new int[maxIndex];

		for (int i = 0; i < maxIndex; i++) {
			hand_card_indexs[i] = cbCardIndexTemp[i];
		}

		if (start_hu_right._show_all) {
			for (int i = 0; i < maxIndex; i++) {
				show_card_indexs[i] = cbCardIndexTemp[i];
				hand_card_indexs[i] = 0;
			}
		} else {
			if (start_hu_right._index_da_si_xi != GameConstants.MAX_INDEX) {
				hand_card_indexs[start_hu_right._index_da_si_xi] = 0;
				show_card_indexs[start_hu_right._index_da_si_xi] = 4;
			}
			if ((start_hu_right._index_liul_liu_shun_1 != GameConstants.MAX_INDEX)
					&& (start_hu_right._index_liul_liu_shun_2 != GameConstants.MAX_INDEX)
					&& (start_hu_right._index_zt_lls_1 != GameConstants.MAX_INDEX)) {
				int count1 = hand_card_indexs[start_hu_right._index_liul_liu_shun_1];
				int count2 = hand_card_indexs[start_hu_right._index_liul_liu_shun_2];
				int count3 = hand_card_indexs[start_hu_right._index_zt_lls_1];

				hand_card_indexs[start_hu_right._index_liul_liu_shun_1] = count1 - 3 >= 0 ? count1 - 3 : 0;
				hand_card_indexs[start_hu_right._index_liul_liu_shun_2] = count2 - 3 >= 0 ? count2 - 3 : 0;
				hand_card_indexs[start_hu_right._index_zt_lls_1] = count3 - 3 >= 0 ? count3 - 3 : 0;

				int count4 = show_card_indexs[start_hu_right._index_liul_liu_shun_1];
				int count5 = show_card_indexs[start_hu_right._index_liul_liu_shun_2];
				int count6 = show_card_indexs[start_hu_right._index_zt_lls_1];

				show_card_indexs[start_hu_right._index_liul_liu_shun_1] = count4 + 3 >= 4 ? 4 : count4 + 3;
				show_card_indexs[start_hu_right._index_liul_liu_shun_2] = count5 + 3 >= 4 ? 4 : count5 + 3;
				show_card_indexs[start_hu_right._index_zt_lls_1] = count6 + 3 >= 4 ? 4 : count6 + 3;
			}
		}

		int cards[] = new int[GameConstants.MAX_FLS_COUNT];

		int hand_card_count = table._logic.switch_to_cards_data(hand_card_indexs, cards);
		table.operate_player_cards(seat_index, hand_card_count, cards, 0, null);

		hand_card_count = table._logic.switch_to_cards_data(show_card_indexs, cards);
		table.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards, GameConstants.INVALID_SEAT);
	}

	@Override
	public boolean handler_player_be_in_room(FLSTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_current_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_FLS; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);

				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_ZHAO) && table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		if (table.GRR._start_hu_right[seat_index].is_valid() == false) {
			int hand_cards[] = new int[GameConstants.MAX_FLS_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

			for (int i = 0; i < hand_card_count; i++) {
				tableResponse.addCardsData(hand_cards[i]);
			}
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._start_hu_right[i].is_valid())
				show_xiao_hu_reconnect(table, i);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false, false);
		}
		return true;
	}
}
