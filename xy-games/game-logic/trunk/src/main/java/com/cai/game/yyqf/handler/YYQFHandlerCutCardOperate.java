package com.cai.game.yyqf.handler;

import org.apache.log4j.Logger;

import com.cai.common.constant.game.Constants_YYQF;
import com.cai.common.util.PBUtil;
import com.cai.game.yyqf.YYQFTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.yyqf.YYQFRsp.CutCard;

/**
 * 切牌
 * 
 * @author hexinqi
 *
 */
public class YYQFHandlerCutCardOperate extends YYQFHandler {

	protected static Logger logger = Logger.getLogger(YYQFHandlerCutCardOperate.class);

	private int cut_card_player;

	public void reset_status(int seat_index) {
		this.cut_card_player = seat_index;
	}

	@Override
	public void exe(YYQFTable table) {
		if (table._game_status != Constants_YYQF.STATUS_CUT_CARD) {
			logger.error("YYQFHandlerCutCardOperate不是切牌状态: " + table._game_status);
			return;
		}
		if (this.cut_card_player != table.cutCardPlayer) {
			logger.error("YYQFHandlerCutCardOperate不是切牌玩家: " + this.cut_card_player);
			return;
		}
		if (table._game_scheduled != null) {
			table.kill_timer();
		}
		table.game_start();
	}

	@Override
	public boolean handler_player_be_in_room(YYQFTable table, int seat_index) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_status[i] = table.get_players()[i] != null;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_YYQF.RESPONSE_YYQF_CUT_CARD);

		table.load_room_info_data(roomResponse);
		if (table._cur_round == 1) {
			table.load_player_info_data(roomResponse);
		}
		CutCard.Builder cutCard = CutCard.newBuilder();
		cutCard.setCutCardData(table.cutCardData);
		cutCard.setCutCardPlayer(table.cutCardPlayer);
		cutCard.setDisplayTime(table._cur_operate_time);
		roomResponse.setCommResponse(PBUtil.toByteString(cutCard));
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

}
