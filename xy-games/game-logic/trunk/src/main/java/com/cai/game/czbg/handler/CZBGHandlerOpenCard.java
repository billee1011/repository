package com.cai.game.czbg.handler;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.czbg.CZBGTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.czbg.CZBGRsp.TableResponse_CZBG;

public class CZBGHandlerOpenCard extends CZBGHandler {

	private static Logger logger = Logger.getLogger(CZBGHandlerOpenCard.class);
	protected int _game_status;

	public void reset_status(int game_status) {
		_game_status = game_status;
	}

	@Override
	public void exe(CZBGTable table) {
	}

	@Override
	public boolean handler_open_cards(CZBGTable table, int seat_index, boolean open_flag) {
		if (table._game_status != CZBGConstants.GS_CZBG_OPEN_CARD && table._game_status != CZBGConstants.GS_CZBG_OPEN_CARD_GUAI) {
			logger.error("CZBGHandlerOpenCard： 不是开牌状态: " + table._game_status);
			return false;
		}
		if (table._game_status == CZBGConstants.GS_CZBG_OPEN_CARD) {
			table.open_card_ox(seat_index);
		} else if (table._game_status == CZBGConstants.GS_CZBG_OPEN_CARD_GUAI) {
			if (open_flag) { // 如果是报道开牌则不用做以下校验判断
				if (!table._logic.get_eight_card(table.cardGroup[seat_index])) {
					table.send_error_notify(seat_index, 2, "请选择正确的牌型");
					return false;
				}
			}
			table.open_card_eight(seat_index);
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(CZBGTable table, int seat_index) {
		if (table._game_status != CZBGConstants.GS_CZBG_OPEN_CARD && table._game_status != CZBGConstants.GS_CZBG_OPEN_CARD_GUAI) {
			logger.error("CZBGHandlerOpenCard： 重连不是开牌状态: " + table._game_status);
			return false;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(CZBGConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_CZBG.Builder tableResponse = TableResponse_CZBG.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setRoomInfo(table.getRoomInfo());

		// 游戏变量
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setCurrentPlayer(seat_index);
		tableResponse.setCellScore(0);

		int maxCount = CZBGConstants.MAX_CARD_COUNT;
		if (table._game_status == CZBGConstants.GS_CZBG_OPEN_CARD) {
			maxCount = CZBGConstants.OX_CARD_COUNT;
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			tableResponse.addTrustee(false);// 是否托管
			tableResponse.addOpenCard(table._open_card[i]);

			int begin = maxCount > CZBGConstants.OX_CARD_COUNT ? CZBGConstants.OX_CARD_COUNT : 0;

			if (begin > 0) {
				for (int j = 0; j < begin; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}

			if (table._open_card[i] || i == seat_index) { // 已开牌或者自己的牌
				for (int j = begin; j < maxCount; j++) {
					cards.addItem(table._open_card[i] ? table.cardGroup[i].cards[j] : table.GRR._cards_data[i][j]);
				}
				tableResponse.addPoint(table.cardGroup[i].point);
			} else {
				for (int j = begin; j < maxCount; j++) {
					cards.addItem(GameConstants.BLACK_CARD);
				}
				tableResponse.addPoint(CZBGConstants.CZBG_CARD_TYPE_ERROR);
			}
			tableResponse.addCardsData(cards);

			tableResponse.addEndScore(table.gameScore[i]);
			tableResponse.addScore(table.roundScore[i]);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addBeishu(table._call_banker[i]);
			tableResponse.addFenshu(table._add_Jetton[i]);
		}
		tableResponse.setCanBaoDao(table.canBaoDao[seat_index]);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
