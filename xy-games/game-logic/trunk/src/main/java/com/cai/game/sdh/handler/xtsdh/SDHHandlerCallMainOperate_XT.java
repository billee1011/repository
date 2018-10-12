package com.cai.game.sdh.handler.xtsdh;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.common.util.PBUtil;
import com.cai.game.sdh.handler.SDHHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sdh.SdhRsp.TableResponseSdh;

public class SDHHandlerCallMainOperate_XT<T extends SDHTable_XT> extends SDHHandler<T> {

	private boolean success = true;

	@Override
	public void exe(SDHTable_XT table) {

	}

	public boolean isSuccess() {
		return success;
	}

	@Override
	public boolean handler_player_be_in_room(SDHTable_XT table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SDHConstants_XT.RESPONSE_RECONNECT_DATA);

		TableResponseSdh.Builder tableResponseSdh = TableResponseSdh.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		if (table.GRR != null) {
			tableResponseSdh.setCurrentPlayer(table._current_player);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponseSdh.addOutCardsCount(table.GRR._cur_round_count[i]);
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (i == seat_index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}
				tableResponseSdh.addCardsData(i, cards);
				tableResponseSdh.addCallBankerScore(table.callScore[i]);
			}
			for (int i = 0; i < SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
				tableResponseSdh.addDiCardsData(seat_index == table._banker_select ? table.diPai[i] : -2);
			}
			tableResponseSdh.setDiCardCount(SDHConstants_XT.SDH_DIPAI_COUNT);
			
			tableResponseSdh.setStall(table.stall);
			tableResponseSdh.setRate(table.rate);
			tableResponseSdh.setDifen(table._di_fen);
			tableResponseSdh.setScore(table.allScore);
			tableResponseSdh.setBankerPlayer(table._cur_banker);
			tableResponseSdh.setMainColor(-1);
			tableResponseSdh.setGameStatus(table._game_status);
			tableResponseSdh.setDiCardCount(0);

			for (int i = 0; i <= 3; i++) { // 可以叫的主花色及每个花色的数量
				tableResponseSdh.addMainColorList(i);
				tableResponseSdh.addMainColorNumber(table.colorNumber[i]);
			}
			tableResponseSdh.addMainColorNumber(table.ntMainCount[table._banker_select]);
			tableResponseSdh.setMainColorCount(5);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponseSdh));
		table.send_response_to_player(seat_index, roomResponse);
		int endTime = (int) (SDHConstants_XT.SDH_OPERATOR_TIME - (System.currentTimeMillis() - table.beginTime) / 1000);
		if (seat_index == table._current_player) { // 通知叫庄
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_MAIN }, endTime,
					seat_index);
		}
		table.showPlayerOperate(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_MAIN }, endTime,
				GameConstants.INVALID_SEAT);

		return true;
	}

}
