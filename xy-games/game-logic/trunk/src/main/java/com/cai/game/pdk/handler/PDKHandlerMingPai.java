package com.cai.game.pdk.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.pdk.PDKTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class PDKHandlerMingPai<T extends PDKTable> extends PDKHandler<T> {

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data = new int[GameConstants.MAX_PDK_COUNT_JD]; // 出牌扑克
	public int _out_card_count = 0;
	public int _out_type;
	public String _desc;

	public void reset_status(int seat_index, int cards[], int card_count, int is_out, String desc) {
		_out_card_player = seat_index;
		_out_cards_data = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			_out_cards_data[i] = cards[i];
		}
		_out_card_count = card_count;
		_out_type = is_out;
		_desc = desc;
	}

	@Override
	public void exe(T table) {
	}

	public boolean handler_ming_pai(T table, int seat_index, int ming_action) {
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
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
