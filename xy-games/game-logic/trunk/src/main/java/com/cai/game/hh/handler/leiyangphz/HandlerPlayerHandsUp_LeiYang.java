package com.cai.game.hh.handler.leiyangphz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.hh.handler.HHHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerPlayerHandsUp_LeiYang extends HHHandler<Table_LeiYang> {

	@Override
	public void exe(Table_LeiYang table) {
		table._game_status = GameConstants.GS_MJ_PAO_QIANG;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		
		table.load_room_info_data(roomResponse);

		table.operate_player_data();

		// 每个能听牌的闲家在客户端弹出举手的界面
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table.GRR._banker_player) {
				table._playerStatus[i]._is_pao_qiang = true;
			} else {
				boolean ting_state = table.is_ting_state(table.GRR._cards_index[i], table.GRR._weave_items[i],
						table.GRR._weave_count[i], i);
				if (ting_state == true) { // 能听牌的才在客户端弹出举手的界面
					roomResponse.setTarget(i);
					roomResponse.setPao(0);
					roomResponse.setPaoMin(0);
					roomResponse.setPaoMax(0);
					roomResponse.setPaoDes("");
					table.send_response_to_player(i, roomResponse);
				} else {
					table._playerStatus[i]._is_pao_qiang = true;
				}
			}
		}

		int tmp_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == true)
				tmp_pao_count++;
		}
		if (tmp_pao_count == table.getTablePlayerNumber()) {
			table._game_status = GameConstants.GS_MJ_PLAY;
			table.exe_chuli_first_card(table.GRR._banker_player, GameConstants.WIK_NULL, 1000);
		}
	}

	public boolean handler_hands_up(Table_LeiYang table, int seat_index, int pao, int qiang) {
		if(seat_index == table.GRR._banker_player){
			if (pao != 0) {
				table.is_hands_up[seat_index] = true;
			}
		}else{
			if (table._playerStatus[seat_index]._is_pao_qiang)
				return false;
			
			table._playerStatus[seat_index]._is_pao_qiang = true;
			
			if (seat_index != table.GRR._banker_player) {
				if (pao != 0) { // 再客户端点了举手之后，传一个非0整型值过来，如果点了不举手，传0过来
					table.is_hands_up[seat_index] = true;
				}
			}
			
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i]._is_pao_qiang == false) {
					return true;
				}
			}
			
			table._game_status = GameConstants.GS_MJ_PLAY;
			
			table.exe_chuli_first_card(table.GRR._banker_player, GameConstants.WIK_NULL, 1000);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_LeiYang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);

		if (table._playerStatus[seat_index]._is_pao_qiang == false) {
			if (seat_index == table.GRR._banker_player) {
				table._playerStatus[seat_index]._is_pao_qiang = true;
			} else {
				boolean ting_state = table.is_ting_state(table.GRR._cards_index[seat_index],
						table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
				if (ting_state == true) { // 能听牌的才在客户端弹出举手的界面
					roomResponse.setTarget(seat_index);
					roomResponse.setPao(0);
					roomResponse.setPaoMin(0);
					roomResponse.setPaoMax(0);
					roomResponse.setPaoDes("");
					table.send_response_to_player(seat_index, roomResponse);
				} else {
					table._playerStatus[seat_index]._is_pao_qiang = true;
				}
			}
		}

		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._hu_xi[i]);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		@SuppressWarnings("unused")
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

		return true;
	}
}
