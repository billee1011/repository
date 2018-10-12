package com.cai.game.hh.handler.yzchz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchFirstCard_YongZhou extends HHHandlerDispatchCard<Table_YongZhou> {
	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_YongZhou table) {
		table._send_card_count++;
		int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE) {
			send_card_data = 0x0a;
		}

		table._send_card_data = send_card_data;
		table._provide_card = _send_card_data;

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index_yongzhou(send_card_data)]++;

		int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards);

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		GameSchedule.put(() -> {
			try {
				table.runnable_deal_with_first_card(_seat_index);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, table.time_for_deal_first_card, TimeUnit.MILLISECONDS);
	}

	@SuppressWarnings("unused")
	@Override
	public boolean handler_operate_card(Table_YongZhou table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("没有这个操作:" + operate_code);
			return false;
		}
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		if (operate_card != table._send_card_data) {
			table.log_player_error(seat_index, "操作牌，与当前牌不一样");
			return true;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YWS: {
			if (luoCode != -1)
				playerStatus.set_lou_pai_kind(luoCode);
		}
		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {

			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank_yongzhou(table._playerStatus[i].get_perform(), 1) + table.getTablePlayerNumber()
							- p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank_yongzhou(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action, 1) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank_yongzhou(table._playerStatus[target_player].get_perform(), 1) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank_yongzhou(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action, 1) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_info("优先级最高的人还没操作");
			return true;
		}

		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._long_count[_seat_index] > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
						GameConstants.INVALID_SEAT);

				int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards);

				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
			}

			int pai_count = 0;

			for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX - 1; i++) {
				if (table.GRR._cards_index[_seat_index][i] < 3)
					pai_count += table.GRR._cards_index[_seat_index][i];
			}

			if (pai_count == 0) {
				table._is_xiang_gong[_seat_index] = true;
				table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);

				int pCount = table.getTablePlayerNumber();
				int next_player = table.get_banker_next_seat(_seat_index);

				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
				return true;
			}

			// _send_card_data = 0; // 这行代码会引起重连时出错

			// TODO 庄家发第一张牌的时候，无论有什么情况，都显示抓牌，如果没人胡，在牌桌上关掉牌显示，并加到手牌
			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

			int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			curPlayerStatus.reset();

			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}
		case GameConstants.WIK_ZI_MO:
		case GameConstants.WIK_CHI_HU:
		case GameConstants.WIK_WANG_ZHA:
		case GameConstants.WIK_WANG_CHUANG:
		case GameConstants.WIK_WANG_DIAO: {
			table.change_hu_count_and_weave_items(target_action, target_player);

			if (target_action == GameConstants.WIK_WANG_ZHA || target_action == GameConstants.WIK_WANG_CHUANG
					|| target_action == GameConstants.WIK_WANG_DIAO) {
				int[] cards_index = Arrays.copyOf(table.GRR._cards_index[target_player], table.GRR._cards_index[target_player].length);

				if (target_action == GameConstants.WIK_WANG_ZHA)
					cards_index[Constants_YongZhou.MAGIC_CARD_INDEX] -= 3;
				if (target_action == GameConstants.WIK_WANG_CHUANG)
					cards_index[Constants_YongZhou.MAGIC_CARD_INDEX] -= 2;
				if (target_action == GameConstants.WIK_WANG_DIAO)
					cards_index[Constants_YongZhou.MAGIC_CARD_INDEX] -= 1;

				int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data_yongzhou(cards_index, cards);

				table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
						table.GRR._weave_items[target_player]);
			}

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			table.operate_player_get_card(target_player, 0, null, GameConstants.INVALID_SEAT, false);

			table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true);

			if (target_action == GameConstants.WIK_ZI_MO) {
				table._player_result.zi_mo_count[target_player]++;
			}

			table._player_result.hu_pai_count[target_player]++;
			table._player_result.ying_xi_count[target_player] += table._hu_xi[target_player];

			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			int delay = table.time_for_display_win_border;

			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_YongZhou table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		int pCount = table.getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == table.zuo_xing_seat) {
				int tmpI = table.GRR._banker_player;

				tableResponse.addTrustee(false);

				tableResponse.addDiscardCount(0);
				Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
				tableResponse.addDiscardCards(int_array);

				tableResponse.addWeaveCount(0);
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				tableResponse.addWeaveItemArray(weaveItem_array);

				tableResponse.addWinnerOrder(0);
				tableResponse.addHuXi(0);

				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[tmpI]));
			} else {
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
				for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
					weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
					if (seat_index != i) {
						if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
								|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
								|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
								&& table.GRR._weave_items[i][j].public_card == 0) {
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
				tableResponse.addHuXi(table._hu_xi[i]);

				// 牌
				if (i == _seat_index) {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
				} else {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
				}
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];

		int hand_card_count = 0;
		if (pCount == 4 && seat_index == table.zuo_xing_seat) {
			hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[table.GRR._banker_player], hand_cards);
		} else {
			hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[seat_index], hand_cards);
		}

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data_yong_zhou(hand_cards, _send_card_data);
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

		table.send_response_to_player(seat_index, roomResponse);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

		table.istrustee[seat_index] = false;

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
