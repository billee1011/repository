package com.cai.game.mj.handler.lxcg;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.fls.FLSTable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPiao_LXCG extends MJHandler {

	private static Logger logger = Logger.getLogger(MJHandlerPiao_LXCG.class);

	@Override
	public void exe(MJTable table) {

		table._game_status = GameConstants.GS_MJ_PIAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_result.pao[i] < 0) {
				table._player_result.pao[i] = 0;
			}
			table.istrustee[i] = false;
		}
		table.operate_player_data();
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
			if (table._shang_zhuang_player == GameConstants.INVALID_SEAT) {
				roomResponse.setPaoMin(0);
			} else {
				if (i == table._shang_zhuang_player) {
					roomResponse.setPaoMin(table._player_result.pao[i]);
					if (table._player_result.pao[i] >= 5) {
						handler_pao_qiang(table, i, 5, 0);
						continue;
					}
				} else {
					roomResponse.setPaoMin(0);
				}
			}
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_PIAO_FLS);
			roomResponse.setPaoDes("最多飘5个");
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable table, int seat_index, int pao, int qiang) {
		table._playerStatus[seat_index]._is_pao_qiang = true;

		if (pao < 0 || pao > GameConstants.PAO_MAX_COUNT_PIAO_FLS) {
			logger.error("小聂这傻吊传了个异常" + pao);
			pao = 0;
		}

		int p = table._player_result.pao[seat_index];

		table._player_result.pao[seat_index] = pao;

		if (p != pao) {
			table.operate_player_data();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		return table.game_starte_real_lxcg();
		
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		if (table._shang_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._shang_zhuang_player);
		} else if (table._lian_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._lian_zhuang_player);
		} else {
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO Auto-generated method stub

		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(MJTable table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据
		table.istrustee[seat_index] = false;
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[i]
		if (table._shang_zhuang_player == GameConstants.INVALID_SEAT) {
			roomResponse.setPaoMin(0);
		} else {
			if (seat_index == table._shang_zhuang_player) {
				roomResponse.setPaoMin(table._player_result.pao[seat_index]);
			} else {
				roomResponse.setPaoMin(0);
			}
		}

		roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_PIAO_FLS);
		roomResponse.setPaoDes("飘5个");
		table.send_response_to_player(seat_index, roomResponse);

		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
