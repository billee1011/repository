package com.cai.game.tdz.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.BTZConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.PBUtil;
import com.cai.game.tdz.TDZTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.tdz.TDZRsp.GameStart_TDZ;
import protobuf.clazz.tdz.TDZRsp.TableResponse_TDZ;

public class TDZHandlerAddJetton<T extends TDZTable> extends TDZHandler<T> {
	protected int _seat_index;
	protected int _game_status;

	protected GangCardResult m_gangCardResult;

	public TDZHandlerAddJetton() {
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
	public boolean handler_add_jetton(TDZTable table, int seat_index, int sub_jetton) {
		if (_game_status != GameConstants.GS_OX_ADD_JETTON) {
			table.log_error("游戏状态不对 " + _game_status + "用户下注 :" + GameConstants.GS_OX_ADD_JETTON);
			return false;
		}
		if (table._add_Jetton[seat_index] != 0) {
			table.log_error("你已经开牌操作了 ");
			return false;
		}
		if (seat_index == table._cur_banker) {
			table.log_error("庄家不用下注");
			return false;
		}

		if (sub_jetton > 0) {
			table._add_Jetton[seat_index] = sub_jetton;
		} else {
			table._add_Jetton[seat_index] = table._jetton_info[0];
		}
		table.add_jetton_ox(seat_index);
		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_status[i] == true) {
				if (i == table._cur_banker) {
					continue;
				}
				if (table._add_Jetton[i] == 0) {
					flag = false;
				}
			}
		}
		// 下分结束
		if (flag == true) {
			table.send_card_data();
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(TDZTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_TDZ.Builder tableResponse = TableResponse_TDZ.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			if ((i == seat_index) && (table._add_Jetton[i] == 0) && (table._player_status[i] == true)) {
				GameStart_TDZ.Builder game_start = GameStart_TDZ.newBuilder();
				game_start.setCurBanker(table._cur_banker);

				if (seat_index != table._cur_banker) {
					for (int k = 0; k < table.getTablePlayerNumber(); k++) {
						Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
						if ((k != table._cur_banker) && (table._player_status[k] == true)) {
							for (int j = 0; j < 3; j++) {
								cards.addItem(table._jetton_info[j]);
							}
						}
						game_start.addJettonCell(k, cards);
						int level = table.initScore / 10;
						if (table.bankerScore > level * 3) {
							game_start.setLevel(level);
							game_start.setMinScore(level * 4);
							game_start.setMaxScore(table.bankerScore);
						}
					}
				}
				tableResponse.setGameStart(game_start);
			}
			tableResponse.addAddJetter(table._add_Jetton[i]);
			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}

		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

}
