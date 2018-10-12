package com.cai.game.mj.shanxi.weinan;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDingQue_WEIHE extends AbstractMJHandler<MJTable_WEIHE> {
	@Override
	public void exe(MJTable_WEIHE table) {
		table._game_status = GameConstants.GS_MJ_DING_QUE;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_DING_QUE);
		table.load_room_info_data(roomResponse);

		table.send_response_to_room(roomResponse);
	}

	public boolean handler_ding_que(MJTable_WEIHE table, int seat_index, int pai_se) {

		if (table.had_ding_que[seat_index]) {
			return false;
		}

		table.had_ding_que[seat_index] = true;

		if (pai_se == 1 || pai_se == 2 || pai_se == 3) {
			table.ding_que_pai_se[seat_index] = pai_se;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
 		int hand_card_count  = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], cards, pai_se);
 		table.changCards(cards, seat_index);
		table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);

		table.operate_player_info(seat_index);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.had_ding_que[i] == false) {
				return true;
			}
		}
		table.operate_player_info(GameConstants.INVALID_SEAT);

		table._game_status = GameConstants.GS_MJ_PLAY;

		/*GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
			}
		}, 1, TimeUnit.SECONDS);*/
		table.exe_dispatch_card(table.GRR._banker_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	@SuppressWarnings("unused")
	@Override
	public boolean handler_player_be_in_room(MJTable_WEIHE table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		/**/

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.had_ding_que[i] == false) {
				flag =false;
			}
		}
		
		table.load_player_info_data(roomResponse,flag ? GameConstants.INVALID_SEAT: seat_index);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(MJTable_WEIHE table, int seat_index) {
		if (table.had_ding_que[seat_index]) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_DING_QUE);
		table.load_room_info_data(roomResponse);

		table.send_response_to_player(seat_index, roomResponse);
	}
}
