package com.cai.game.btz.handler.qzbtz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.BTZConstants;
import com.cai.common.domain.Player;
import com.cai.common.util.PBUtil;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.handler.BTZHandlerCallBanker;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.btz.BTZRsp.CallBankerInfo_BTZ;
import protobuf.clazz.btz.BTZRsp.TableResponse_BTZ;

public class BTZHandlerCallBanker_qzbtz extends BTZHandlerCallBanker<BTZTable> {

	@Override
	public void exe(BTZTable table) {

	}

	/***
	 * 用户下注
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	public boolean handler_call_banker(BTZTable table, int seat_index, int call_banker) {
		if (_game_status != GameConstants.GS_OX_CALL_BANKER) {
			table.log_error("游戏状态不对 " + _game_status + "用户开牌 :" + GameConstants.GS_OX_CALL_BANKER);
			return false;
		}
		if (table._call_banker[seat_index] != -1) {
			table.log_error("你已经叫庄操作了 ");
			return false;
		}
		if (call_banker < 0 || call_banker >= table._call_banker_info.length) {
			table.log_error("您下注已经越界了");
		}
		// 现在叫庄都是1分
		table._call_banker[seat_index] = table._call_banker_info[call_banker];
		table.add_call_banker(seat_index);
		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_status[i] == true) {
				if (table._call_banker[i] == -1)
					flag = false;
			}
		}
		// 游戏开始
		if (flag == true) {
			table.game_start();

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(BTZTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_BTZ.Builder tableResponse = TableResponse_BTZ.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		tableResponse.setBankerPlayer(-1);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			if ((i == seat_index) && (table._call_banker[i] == -1) && (table._player_status[i] == true)) {
				CallBankerInfo_BTZ.Builder call_banker_info = CallBankerInfo_BTZ.newBuilder();
				for (int j = 0; j <= table._banker_max_times; j++) {
					call_banker_info.addCallBankerInfo(table._call_banker_info[j]);
				}
				tableResponse.setMyCallBankerInfo(call_banker_info);
			}
			tableResponse.addCallBankerInfo(table._call_banker[i]);
			tableResponse.addTrustee(table.isTrutess(i));
		}

		// 游戏变量
		tableResponse.setSceneInfo(table._game_status);

		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		if (table.isTrutess(seat_index)) {
			table.reSendTrusteeToPlayer(seat_index);
		}
		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(BTZTable table, Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_BTZ.Builder tableResponse = TableResponse_BTZ.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		tableResponse.setBankerPlayer(-1);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._call_banker[i] == -1) && (table._player_status[i] == true)) {
				CallBankerInfo_BTZ.Builder call_banker_info = CallBankerInfo_BTZ.newBuilder();
				for (int j = 0; j <= table._banker_max_times; j++) {
					call_banker_info.addCallBankerInfo(table._call_banker_info[j]);
				}
				tableResponse.setMyCallBankerInfo(call_banker_info);
			}
			tableResponse.addCallBankerInfo(table._call_banker[i]);
			tableResponse.addTrustee(table.isTrutess(i));
		}

		// 游戏变量
		tableResponse.setSceneInfo(table._game_status);

		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		
		table.observers().send(player, roomResponse);
		return true;
	}
}
