package com.cai.game.schcp.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.schcp.SCHCPTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class SCHCPHandlerDispatchCard<T extends SCHCPTable> extends SCHCPHandler<T> {
	protected int _seat_index;
	protected int _send_card_data;

	protected int _type;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	protected GangCardResult m_gangCardResult;

	public SCHCPHandlerDispatchCard() {
		m_gangCardResult = new GangCardResult();
	
	}

	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;

	}

	@Override
	public void exe(T table) {
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(T table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(seat_index,card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("SCHCPHandlerDispatchCard出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("SCHCPHandlerDispatchCard出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			table.log_error("SCHCPHandlerDispatchCard状态不对不能出牌");
			return false;
		}
		if (table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(card)] >= 3) {
			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			// 显示出牌
			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

			table.log_error(_seat_index + "出牌出错 SCHCPHandlerDispatchCard " + _seat_index);
			return true;
		}
		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌
		table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(T table, int seat_index) {
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
			for (int j = 0; j < GameConstants.MAX_CP_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.CP_WIK_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.CP_WIK_KAN)
							&& table._is_display == false) {
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

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_CP_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		boolean flag = false;
		if (_send_card_data != 0)
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		// 如果断线重连的人是自己
		// if(seat_index == _seat_index){
		// if(!((seat_index == table._current_player) && (_send_card_data ==
		// 0)))
		// table._logic.remove_card_by_data(hand_cards, _send_card_data);
		// }

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 摸牌
		boolean is_zhao = false;
		for(int i = 0; i< table.getTablePlayerNumber();i++)
		{
			if(table._playerStatus[i].has_cp_zhao())
			{
				is_zhao = true;
				break;
			}
		}
		if((_send_card_data != 0)&&(is_zhao==true)){
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, true);

		}
		else if ((_send_card_data != 0) && (flag == false))
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);
		table.operate_cannot_card(seat_index,false);
		table.operate_must_out_card(seat_index, false);
		return true;
	}

}
