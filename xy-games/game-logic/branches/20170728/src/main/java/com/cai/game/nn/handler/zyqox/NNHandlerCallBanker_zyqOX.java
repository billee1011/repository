package com.cai.game.nn.handler.zyqox;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.nn.NNTable;
import com.cai.game.nn.handler.NNHandlerCallBanker;

import protobuf.clazz.Protocol.CallBankerInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.TableResponseOX;
import protobuf.clazz.Protocol.Timer_OX;

public class NNHandlerCallBanker_zyqOX extends NNHandlerCallBanker {

	@Override
	public void exe(NNTable table) {

	}

	/***
	 * 用户下注
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	public boolean handler_call_banker(NNTable table, int seat_index, int call_banker) {
		if (_game_status != GameConstants.GS_OX_CALL_BANKER) {
			table.log_error("游戏状态不对 " + _game_status + "用户开牌 :" + GameConstants.GS_OX_CALL_BANKER);
			return false;
		}
		if (table._call_banker[seat_index] != -1) {
			table.log_error("你已经叫庄操作了 ");
			return false;
		}
		if (call_banker < 0 || call_banker > 4) {
			table.log_error("您下注已经越界了");
		}
		table._call_banker[seat_index] = table._call_banker_info[call_banker];
		table.add_call_banker(seat_index);
		boolean flag = true;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (table._player_status[i] == true) {
				if (table._call_banker[i] == -1)
					flag = false;
			}
		}
		// 游戏结束
		if (flag == true) {
			table.game_start_ZYQOX();

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(NNTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		tableResponse.setBankerPlayer(-1);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

			if ((i == seat_index) && (table._call_banker[i] == -1) && (table._player_status[i] == true)) {
				CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
				for (int j = 0; j <= table._banker_max_times; j++) {
					call_banker_info.addCallBankerInfo(table._call_banker_info[j]);
				}
				roomResponse_ox.setCallBankerInfo(call_banker_info);
			}
			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}

		// 游戏变量
		tableResponse.setSceneInfo(table._game_status);

		roomResponse_ox.setTableResponseOx(tableResponse);
		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(display_time);
			roomResponse_ox.setDisplayTime(timer);
		}
		roomResponse.setRoomResponseOx(roomResponse_ox);
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(NNTable table, Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		// tableResponse.setPlayerStatus(table._player_status[seat_index]);
		tableResponse.setBankerPlayer(-1);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}

		// 游戏变量
		tableResponse.setSceneInfo(table._game_status);

		roomResponse_ox.setTableResponseOx(tableResponse);
		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(display_time);
			roomResponse_ox.setDisplayTime(timer);
		}
		roomResponse.setRoomResponseOx(roomResponse_ox);
		table.observers().send(player, roomResponse);
		return true;
	}

}
