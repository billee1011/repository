package com.cai.game.btz.handler.qzbtz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.BTZConstants;
import com.cai.common.domain.Player;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.handler.BTZHandlerOpenCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.btz.BTZRsp.CardType_BTZ;
import protobuf.clazz.btz.BTZRsp.TableResponse_BTZ;

public class BTZHandlerOpenCard_qzbtz extends BTZHandlerOpenCard<BTZTable> {

	@Override
	public void exe(BTZTable table) {

	}

	/***
	 * 用户开牌
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	@Override
	public boolean handler_open_cards(BTZTable table, int seat_index, boolean open_flag) {
		if (_game_status != GameConstants.GS_OX_OPEN_CARD) {
			table.log_error("游戏状态不对 " + _game_status + "用户开牌 :" + GameConstants.GS_OX_OPEN_CARD);
			return false;
		}
		if (table._open_card[seat_index] == true) {
			table.log_error("你已经开牌操作了 ");
			return false;
		}
		if (table._player_status[seat_index] == false) {
			return false;
		}
		if (open_flag == true)
			table._open_card[seat_index] = open_flag;
		else {
			table.log_error("open_flag 数据错误");
			return true;
		}

		table.open_card_ox(seat_index);

		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_status[i] == true) {
				if (table._open_card[i] == false)
					flag = false;
			}
		}
		// 游戏结束
		if (flag == true) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			table.process_openCard();

			table.countAllScore(seat_index);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

		}

		return false;
	}

	@Override
	public boolean handler_player_be_in_room(BTZTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_BTZ.Builder tableResponse = TableResponse_BTZ.newBuilder();
		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);

		// 游戏变量
		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		tableResponse.setBankerPlayer(table._cur_banker);
		CardType_BTZ.Builder send_card = CardType_BTZ.newBuilder();
		for (int k = 0; k < table.getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (table._player_status[k] != true) {
				for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}

				tableResponse.addCardType(0);
				tableResponse.addPoint(0);
				tableResponse.addGameTime(0);

			} else {
				if (k == seat_index || table._open_card[k]) {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(table.GRR._cards_data[k][j]);
					}
					tableResponse.addCardType(table.groups[k].type.get());
					tableResponse.addPoint(table.groups[k].point);
					tableResponse.addGameTime(table.groups[k].getRate());
				} else {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
					tableResponse.addCardType(0);
					tableResponse.addPoint(0);
					tableResponse.addGameTime(0);
				}

			}
			// tableResponse.addGameTime(times);
			tableResponse.addOpenCard(table._open_card[k]);

			tableResponse.addCardsData(k, cards);
			tableResponse.addCallBankerInfo(table._call_banker[k]);
			tableResponse.addAddJetter(table._add_Jetton[k]);
			tableResponse.addTrustee(table.isTrutess(k));
		}
		tableResponse.setSendCard(send_card);
		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		if (table.isTrutess(seat_index)) {
			table.reSendTrusteeToPlayer(seat_index);
		}
		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(BTZTable table, Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_BTZ.Builder tableResponse = TableResponse_BTZ.newBuilder();
		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);

		// 游戏变量
		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		CardType_BTZ.Builder send_card = CardType_BTZ.newBuilder();
		for (int k = 0; k < table.getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (table._player_status[k] != true) {
				for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}

				tableResponse.addCardType(0);
				tableResponse.addPoint(0);
				tableResponse.addGameTime(0);

			} else {
				if (table._open_card[k]) {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(table.GRR._cards_data[k][j]);
					}
					tableResponse.addCardType(table.groups[k].type.get());
					tableResponse.addPoint(table.groups[k].point);
					tableResponse.addGameTime(table.groups[k].getRate());
				} else {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
					tableResponse.addCardType(0);
					tableResponse.addPoint(0);
					tableResponse.addGameTime(0);
				}

			}
			// tableResponse.addGameTime(times);
			tableResponse.addOpenCard(table._open_card[k]);

			tableResponse.addCardsData(k, cards);
			tableResponse.addCallBankerInfo(table._call_banker[k]);
			tableResponse.addAddJetter(table._add_Jetton[k]);
			tableResponse.addTrustee(table.isTrutess(k));
		}
		tableResponse.setSendCard(send_card);
		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.observers().send(player, roomResponse);

		return true;
	}

}
