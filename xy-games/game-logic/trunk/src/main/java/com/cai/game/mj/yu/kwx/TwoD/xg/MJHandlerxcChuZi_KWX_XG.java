package com.cai.game.mj.yu.kwx.TwoD.xg;

import java.util.List;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.util.PBUtil;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.mj.KwxProto.DZMessage;
import protobuf.clazz.mj.KwxProto.PlayerDZMessage;

public class MJHandlerxcChuZi_KWX_XG extends AbstractMJHandler<Table_KWX_XG_2D> {

	private int[] DEFAULT_CARD;

	private boolean start_game;

	private static final int NON_SELECT_CARD = 50000;

	private static final int STATUS_CHU_ZI = 0;

	private static final int STATUS_CHU_ZI_END = 1;

	public void reset() {
		DEFAULT_CARD = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
		start_game = false;
	}

	@Override
	public void exe(Table_KWX_XG_2D table) {
		table._game_status = GameConstants_KWX.GS_MJ_CHU_ZI; // 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHU_ZI);

		table.load_common_status(roomResponse);
		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);

		table.operate_player_data();

		DZMessage.Builder dzMessageBuilder = DZMessage.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 3; j++) {
				cs.addItem(table.player_chu_zi[i][j]);
			}
			dzMessageBuilder.addCard(cs);
		}
		dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI);

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			PlayerDZMessage.Builder PlayerDZMessageBuilder = PlayerDZMessage.newBuilder();
			table.player_chu_zi_status[i] = 0;
			PlayerDZMessageBuilder.setType(1);
			PlayerDZMessageBuilder.setStatus(table.player_chu_zi_status[i]);
			for (int j = 0; j < DEFAULT_CARD.length; j++) {
				PlayerDZMessageBuilder.addViewCard(DEFAULT_CARD[j]);
			}
			PlayerDZMessageBuilder.setDzMessage(dzMessageBuilder);

			roomResponse.setCommResponse(PBUtil.toByteString(PlayerDZMessageBuilder));
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_chu_zi(Table_KWX_XG_2D table, int seat_index, List<Integer> canCards) {

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (p == seat_index || table.player_chu_zi_status[p] == 0) {
				continue;
			}

			for (int c = 0; c < 3; c++) {
				int card = table.player_chu_zi[p][c];
				if (canCards.contains(card)) {
					table.send_sys_response_to_player(seat_index, "已被其他玩家选择，清重新选择 ");
					this.player_chu_zi(table, seat_index);
					return false;
				}
			}
		}

		table.player_chu_zi_count[seat_index] = canCards.size();
		if (canCards.size() == 0) {
			table.player_chu_zi_status[seat_index] = -1;
			for (int i = 0; i < 3; i++) {
				table.player_chu_zi[seat_index][i] = -1;
			}
		} else {
			table.player_chu_zi_status[seat_index] = 1;
			for (int i = 0; i < table.player_chu_zi_count[seat_index]; i++) {
				table.player_chu_zi[seat_index][i] = canCards.get(i);

				for (int j = 0; j < DEFAULT_CARD.length; j++) {
					if (DEFAULT_CARD[j] == table.player_chu_zi[seat_index][i]) {
						DEFAULT_CARD[j] += NON_SELECT_CARD;
					}
				}
			}
		}

		start_game = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.player_chu_zi_status[i] == 0) {
				start_game = false;
			}
		}

		DZMessage.Builder dzMessageBuilder = DZMessage.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 3; j++) {
				if (table.player_chu_zi[i][j] == 0) {
					continue;
				}
				cs.addItem(table.player_chu_zi[i][j]);
			}
			dzMessageBuilder.addCard(cs);
		}
		if (start_game) {
			dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI_END);
		} else {
			dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_CHU_ZI);
			table.load_room_info_data(roomResponse);

			PlayerDZMessage.Builder PlayerDZMessageBuilder = PlayerDZMessage.newBuilder();
			PlayerDZMessageBuilder.setType(1);
			PlayerDZMessageBuilder.setStatus(table.player_chu_zi_status[i]);
			for (int j = 0; j < DEFAULT_CARD.length; j++) {
				PlayerDZMessageBuilder.addViewCard(DEFAULT_CARD[j]);
			}
			PlayerDZMessageBuilder.setDzMessage(dzMessageBuilder);

			roomResponse.setCommResponse(PBUtil.toByteString(PlayerDZMessageBuilder));
			table.send_response_to_player(i, roomResponse);
		}

		if (start_game && table._game_status == GameConstants_KWX.GS_MJ_CHU_ZI) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_CHU_ZI);
			table.load_room_info_data(roomResponse);

			PlayerDZMessage.Builder PlayerDZMessageBuilder = PlayerDZMessage.newBuilder();
			PlayerDZMessageBuilder.setType(1);
			for (int j = 0; j < DEFAULT_CARD.length; j++) {
				PlayerDZMessageBuilder.addViewCard(DEFAULT_CARD[j]);
			}
			PlayerDZMessageBuilder.setDzMessage(dzMessageBuilder);

			roomResponse.setCommResponse(PBUtil.toByteString(PlayerDZMessageBuilder));
			table.GRR.add_room_response(roomResponse);

			table._game_status = GameConstants_KWX.GS_MJ_PLAY;
			table.on_game_start_real();
		}
		return true;
	}

	public boolean handler_token_chu_zi_detail(Table_KWX_XG_2D table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHU_ZI);
		table.load_room_info_data(roomResponse);

		// 发送数据
		DZMessage.Builder dzMessageBuilder = DZMessage.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 3; j++) {
				if (table.player_chu_zi[i][j] == 0) {
					continue;
				}
				cs.addItem(table.player_chu_zi[i][j]);
			}
			dzMessageBuilder.addCard(cs);
		}
		if (start_game) {
			dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI_END);
		} else {
			dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI);
		}

		PlayerDZMessage.Builder PlayerDZMessageBuilder = PlayerDZMessage.newBuilder();
		PlayerDZMessageBuilder.setType(2);
		PlayerDZMessageBuilder.setDzMessage(dzMessageBuilder);

		roomResponse.setCommResponse(PBUtil.toByteString(PlayerDZMessageBuilder));
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_KWX_XG_2D table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		this.player_chu_zi(table, seat_index);

		return true;
	}

	public void player_chu_zi(Table_KWX_XG_2D table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHU_ZI);
		table.load_room_info_data(roomResponse);

		// 发送数据
		DZMessage.Builder dzMessageBuilder = DZMessage.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 3; j++) {
				if (table.player_chu_zi[i][j] == 0) {
					continue;
				}
				cs.addItem(table.player_chu_zi[i][j]);
			}
			dzMessageBuilder.addCard(cs);
		}
		if (start_game) {
			dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI_END);
		} else {
			dzMessageBuilder.setEndChuZi(STATUS_CHU_ZI);
		}

		PlayerDZMessage.Builder PlayerDZMessageBuilder = PlayerDZMessage.newBuilder();
		PlayerDZMessageBuilder.setStatus(table.player_chu_zi_status[seat_index]);
		PlayerDZMessageBuilder.setType(1);
		for (int j = 0; j < DEFAULT_CARD.length; j++) {
			PlayerDZMessageBuilder.addViewCard(DEFAULT_CARD[j]);
		}
		PlayerDZMessageBuilder.setDzMessage(dzMessageBuilder);

		roomResponse.setCommResponse(PBUtil.toByteString(PlayerDZMessageBuilder));

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
