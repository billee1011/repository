package com.cai.game.ddz.handler.klddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_KL_DDZ;
import com.cai.common.util.PBUtil;
import com.cai.game.ddz.handler.DDZHandlerPiao;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.GameStartDDZ;
import protobuf.clazz.ddz.DdzRsp.Piao_Fen;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;

/**
 * 飘分
 * 
 * @author admin
 *
 */
public class DDZHandlerPiao_KL extends DDZHandlerPiao<DDZ_KL_Table> {

	@Override
	public void handler_Piao_fen(DDZ_KL_Table table, int seat_index, int piao_fen) {
		if (table._player_result.pao[seat_index] > 0) {
			return;
		}
		table._piao_fen[seat_index] = piao_fen;
		table._player_result.pao[seat_index] = 1;
		boolean select = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._piao_fen[i] < 0) {
				select = false;
			}
		}

		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setType(MsgConstants.RESPONSE_DDZ_EFFECT_PIAO);
		Piao_Fen.Builder piao2 = Piao_Fen.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			piao2.addPiao(-1);
			piao2.addIsPiao(table._player_result.pao[i]);
		}
		// piao1.setDisplayTime(10);
		piao2.setRoomInfo(table.getRoomInfoDdz());
		roomResponse2.setCommResponse(PBUtil.toByteString(piao2));
		table.send_response_to_room(roomResponse2);
		table.GRR.add_room_response(roomResponse2);

		if (!select) {
			return;
		}

		table.piao_fen_operate();
		table._current_player = table.GRR._banker_player;
		_game_status = GameConstants.GS_CALL_BANKER;// 设置状态
		// 设置玩家状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);
			table._playerStatus[i]._call_banker = -1;
			table._playerStatus[i]._qiang_banker = -1;
		}
		for (int index = 0; index < table.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_GAME_START);
			GameStartDDZ.Builder gameStartResponse = GameStartDDZ.newBuilder();
			RoomInfoDdz.Builder room_info = table.getRoomInfoDdz();
			gameStartResponse.setRoomInfo(room_info);
			if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
				gameStartResponse.setMagicCard(table._logic.switch_index_to_card(table._logic.magic_card[0]));
			} else {
				gameStartResponse.setMagicCard(-1);
			}
			table.load_player_info_data_game_start(gameStartResponse);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				gameStartResponse.addCardCount(table.GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();

				if (i == index) {
					for (int j = 0; j < table.GRR._card_count[index]; j++) {
						cards_card.addItem(table.GRR._cards_data[index][j]);
					}
				}
				gameStartResponse.addCardsData(cards_card);
				gameStartResponse.addDifenBombDes(table.get_boom_difen_des(i));
			}

			gameStartResponse.setDiPaiCardCount(table._di_pai_card_count);

			roomResponse.setCommResponse(PBUtil.toByteString(gameStartResponse));
			table.send_response_to_player(index, roomResponse);
		}
		table.set_timer(DDZ_KL_Table.ID_TIMER_START_TO_CALL_BANKER, 1500);

		table._handler = table._handler_call_banker;
		// 回放功能
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_GAME_START);
		GameStartDDZ.Builder gameStartResponse = GameStartDDZ.newBuilder();
		RoomInfoDdz.Builder room_info = table.getRoomInfoDdz();
		gameStartResponse.setRoomInfo(room_info);
		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			gameStartResponse.setMagicCard(table._logic.switch_index_to_card(table._logic.magic_card[0]));
		} else {
			gameStartResponse.setMagicCard(-1);
		}
		for (int index = 0; index < table.getTablePlayerNumber(); index++) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				gameStartResponse.addCardCount(table.GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards_card.addItem(table.GRR._cards_data[i][j]);
				}
				gameStartResponse.addCardsData(i, cards_card);
			}
			gameStartResponse.addDifenBombDes(table.get_boom_difen_des(index));
			gameStartResponse.setDiPaiCardCount(table._di_pai_card_count);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(gameStartResponse));
		table.GRR.add_room_response(roomResponse);
	}
}
