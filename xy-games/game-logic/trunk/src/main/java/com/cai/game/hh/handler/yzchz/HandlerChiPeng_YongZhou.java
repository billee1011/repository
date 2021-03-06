package com.cai.game.hh.handler.yzchz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.hh.handler.HHHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerChiPeng_YongZhou extends HHHandlerChiPeng<Table_YongZhou> {

	@Override
	public void exe(Table_YongZhou table) {
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][wIndex]);

		table._current_player = _seat_index;

		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS | GameConstants.WIK_YWS;

		if (_lou_card == -1 || (eat_type & _action) == 0)
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants.INVALID_SEAT);
		else
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_LOU }, 1,
					GameConstants.INVALID_SEAT);

		if (_type == GameConstants.CHI_PENG_TYPE_OUT_CARD) {
			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		}
		if (_type == GameConstants.CHI_PENG_TYPE_DISPATCH) {
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT, false);
		}

		int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards);

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		if (table._ti_mul_long[_seat_index] == 0) {
			int pai_count = 0;
			for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX - 1; i++) {
				if (table.GRR._cards_index[_seat_index][i] < 3)
					pai_count += table.GRR._cards_index[_seat_index][i];
			}

			if (pai_count == 0) {
				table._is_xiang_gong[_seat_index] = true;
				table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
				int next_player = table.get_banker_next_seat(_seat_index);

				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);

				return;
			}

			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		} else {
			table._ti_mul_long[_seat_index]--;

			int next_player = table.get_banker_next_seat(_seat_index);
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
		}
	}

	@Override
	public boolean handler_operate_card(Table_YongZhou table, int seat_index, int operate_code, int operate_card, int lou_pai) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("没有这个操作:" + operate_code);
			return false;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		if (seat_index != _seat_index) {
			table.log_info("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_YongZhou table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);// 加载房间的玩法 状态信息
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
				tableResponse.addHuXi(table._hu_xi[i]);
				//
				tableResponse.addWinnerOrder(0);

				// 牌
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
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

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

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
