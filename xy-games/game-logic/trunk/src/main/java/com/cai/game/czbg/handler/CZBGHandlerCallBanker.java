package com.cai.game.czbg.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.PBUtil;
import com.cai.game.czbg.CZBGTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.czbg.CZBGRsp.CallBankerInfo_CZBG;
import protobuf.clazz.czbg.CZBGRsp.TableResponse_CZBG;

public class CZBGHandlerCallBanker extends CZBGHandler {

	protected int _seat_index;
	protected int _game_status;

	protected GangCardResult m_gangCardResult;

	public CZBGHandlerCallBanker() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(CZBGTable table) {

	}

	@Override
	public boolean handler_call_banker(CZBGTable table, int seat_index, int call_banker) {
		if (table._game_status != CZBGConstants.GS_CZBG_CALL_BANKER) {
			table.log_error("游戏状态不对 " + _game_status);
			return false;
		}
		if (table._call_banker[seat_index] != -1) {
			table.log_error("你已经叫庄操作了 ");
			return false;
		}
		table._call_banker[seat_index] = call_banker;
		table.add_call_banker(seat_index);
		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_status[i] == true) {
				if (table._call_banker[i] == -1) {
					flag = false;
				}
			}
		}
		// 全部都选择了叫庄操作
		if (flag == true) {
			table.switch_add_jetton();
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(CZBGTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(CZBGConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_CZBG.Builder tableResponse = TableResponse_CZBG.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setCurrentPlayer(_seat_index);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table.isTrutess(i));// 是否托管
			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}
		tableResponse.setRoomInfo(table.getRoomInfo());

		if (table.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				tableResponse.addTrustee(false);// 是否托管
				tableResponse.addOpenCard(table._open_card[i]);

				if (i == seat_index) {
					for (int j = 0; j < 4; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}
				tableResponse.addPoint(table.cardGroup[i].point);
				tableResponse.addCardsData(cards);

				tableResponse.addEndScore(table.gameScore[i]);
				tableResponse.addScore(table.roundScore[i]);
			}
		}

		if (table._call_banker[seat_index] == 0) {
			CallBankerInfo_CZBG.Builder callBanker = CallBankerInfo_CZBG.newBuilder();
			callBanker.addCallButton(-1); // 不叫
			if (table.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) { // 先牛后怪是有倍抢庄
				for (int i = 1; i < 4; i++) {
					callBanker.addCallButton(i);
				}
			} else {
				callBanker.addCallButton(4);
			}
			tableResponse.setCallBankerInfoCzbg(callBanker);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addBeishu(table._call_banker[i]);
			tableResponse.addFenshu(table._add_Jetton[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
