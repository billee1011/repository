package com.cai.game.ddz.handler;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.ddz.DDZTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.Piao_Fen;

/**
 * 飘分处理
 * 
 * @author admin
 *
 * @param <T>
 */
public class DDZHandlerPiao<T extends DDZTable> extends DDZHandler<T> {
	protected int _seat_index;
	protected int _game_status;

	public DDZHandlerPiao() {
		// TODO Auto-generated constructor stub
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(T table) {
		table._game_status = GameConstants.GS_MJ_PIAO;// 设置状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._piao_fen[i] = -1;
		}
		Arrays.fill(table._player_result.pao, -1);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_EFFECT_PIAO);
		Piao_Fen.Builder piao = Piao_Fen.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			piao.addPiao(table._piao_fen[i]);
			piao.addIsPiao(table._player_result.pao[i]);
		}
		piao.setRoomInfo(table.getRoomInfoDdz());
		piao.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(piao));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

	}

	public void handler_Piao_fen(T table, int seat_index, int piao_fen) {

		// table.operate_out_card(GameConstants.INVALID_SEAT, 0, null,
		// GameConstants.DDZ_CT_ERROR, GameConstants.INVALID_SEAT);
	}

	@Override
	public boolean handler_player_be_in_room(T table, int seat_index) {
		if (table._piao_fen[seat_index] > 0) {
			return false;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		table.load_room_info_data(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_EFFECT_PIAO);
		Piao_Fen.Builder piao = Piao_Fen.newBuilder();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i != seat_index) {
				piao.addPiao(-1);
			} else {
				piao.addPiao(table._piao_fen[i]);
			}

			piao.addIsPiao(table._player_result.pao[i]);
		}
		piao.setRoomInfo(table.getRoomInfoDdz());
		piao.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(piao));
		table.send_response_to_player(seat_index, roomResponse);

		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

}
