package com.cai.game.tdz.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.BTZConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.tdz.TDZTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.tdz.TDZRsp.CallBankerInfo_TDZ;
import protobuf.clazz.tdz.TDZRsp.TableResponse_TDZ;

public class TDZHandlerCallBanker<T extends TDZTable> extends TDZHandler<T> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	protected GangCardResult m_gangCardResult;

	public TDZHandlerCallBanker() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(T table) {
	}

	@Override
	public boolean handler_call_banker(TDZTable table, int seat_index, int call_banker) {
		if (_game_status != GameConstants.GS_OX_CALL_BANKER) {
			table.log_error("游戏状态不对 " + _game_status + "用户开牌 :" + GameConstants.GS_OX_CALL_BANKER);
			return false;
		}
		if (table._call_banker[seat_index] != -1) {
			table.log_error("你已经叫庄操作了 ");
			return false;
		}
		table._call_banker[seat_index] = table._call_banker_info[call_banker];
		table.add_call_banker(seat_index);
		boolean flag = true;
		int banker[] = new int[table.getTablePlayerNumber()];
		int count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_status[i] == true) {
				if (table._call_banker[i] == -1) {
					flag = false;
				} else if (table._call_banker[i] == 1) {
					banker[count++] = i;
				}
			}
		}
		// 游戏开始
		if (flag == true) {
			if (count > 0) {
				table._cur_banker = banker[RandomUtil.getRandomNumber(Integer.MAX_VALUE) % count];
			} else {
				table._cur_banker = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber();
			}
			table._player_result.game_score[table._cur_banker] = table.initScore;
			table.add = true;
			table.game_start();
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(TDZTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_TDZ.Builder tableResponse = TableResponse_TDZ.newBuilder();

		table._cur_round++;
		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		tableResponse.setBankerPlayer(-1);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			if ((i == seat_index) && (table._call_banker[i] == -1) && (table._player_status[i] == true)) {
				CallBankerInfo_TDZ.Builder call_banker_info = CallBankerInfo_TDZ.newBuilder();
				for (int j = 0; j <= 1; j++) {
					call_banker_info.addCallBankerInfo(table._call_banker_info[j]);
				}
				tableResponse.setMyCallBankerInfo(call_banker_info);
			}
			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}

		// 游戏变量
		tableResponse.setSceneInfo(table._game_status);

		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		table._cur_round--;
		return true;
	}
}
