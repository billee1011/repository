package com.cai.game.mj.chenchuang.wanzai;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_WanZai;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerFirePiao_WanZai extends AbstractMJHandler<Table_WanZai> {

	@Override
	public void exe(Table_WanZai table) {

		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		table.operate_player_data();

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setGameStatus(table._game_status);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setQiang(table._player_result.qiang[i]);
			roomResponse.setPaoMax(table._playerStatus[i]._is_pao ? -1 : 1);
			roomResponse.setQiangMax(table._playerStatus[i]._is_pao_qiang ? -1 : 1);
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(Table_WanZai table, int seat_index, int pao, int qiang) {
		if (pao != -1) {
			if (table._playerStatus[seat_index]._is_pao)
				return false;
			else {
				table._playerStatus[seat_index]._is_pao = true;
				table._player_result.pao[seat_index] = pao;
			}
		}

		if (qiang != -1) {
			if (table._playerStatus[seat_index]._is_pao_qiang)
				return false;
			else {
				if(qiang == 1)
					table.qiang_round[seat_index] = table._cur_round;
				table._playerStatus[seat_index]._is_pao_qiang = true;
				table._player_result.qiang[seat_index] = qiang;
			}
		}

		table.operate_player_data();

		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse1);

		// 发送数据
		roomResponse1.setTarget(seat_index);
		roomResponse1.setGameStatus(table._game_status);
		roomResponse1.setPao(table._player_result.pao[seat_index]);
		roomResponse1.setQiang(table._player_result.qiang[seat_index]);
		roomResponse1.setPaoMax(table._playerStatus[seat_index]._is_pao ? -1 : 1);
		roomResponse1.setQiangMax(table._playerStatus[seat_index]._is_pao_qiang ? -1 : 1);
		table.send_response_to_player(seat_index, roomResponse1);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false || table._playerStatus[i]._is_pao == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PAO) {
			table.show_tou_zi(0);
			// 都ok了,游戏开始
			table._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
			GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
			gameStartResponse.setBankerPlayer(table.GRR._banker_player);
			gameStartResponse.setCurrentPlayer(table._current_player);
			gameStartResponse.setLeftCardCount(table.GRR._left_card_count);

			int hand_cards[][] = new int[table.getTablePlayerNumber()][GameConstants.MAX_COUNT];
			// 发送数据
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards[i]);
				gameStartResponse.addCardsCount(hand_card_count);
			}

			// 发送数据
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				// 只发自己的牌
				gameStartResponse.clearCardData();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (table._logic.is_magic_card(hand_cards[i][j])) {
						hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					gameStartResponse.addCardData(hand_cards[i][j]);
				}

				// 回放数据
				table.GRR._video_recode.addHandCards(cards);

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				table.load_room_info_data(roomResponse);
				table.load_common_status(roomResponse);

				if (table._cur_round == 1) {
					// shuffle_players();
					table.load_player_info_data(roomResponse);
				}
				roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
				roomResponse.setGameStart(gameStartResponse);
				roomResponse.setCurrentPlayer(table._current_player == GameConstants.INVALID_SEAT ? table._resume_player : table._current_player);
				roomResponse.setLeftCardCount(table.GRR._left_card_count);
				roomResponse.setGameStatus(table._game_status);
				roomResponse.setLeftCardCount(table.GRR._left_card_count);
				table.send_response_to_player(i, roomResponse);
			}
			////////////////////////////////////////////////// 回放
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			table.load_room_info_data(roomResponse);
			table.load_common_status(roomResponse);
			table.load_player_info_data(roomResponse);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					cards.addItem(hand_cards[i][j]);
				}
				gameStartResponse.addCardsData(cards);
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(table.GRR._left_card_count);
			table.GRR.add_room_response(roomResponse);
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			// 检测听牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i],
						table.GRR._weave_items[i], table.GRR._weave_count[i], i);
				if (table._playerStatus[i]._hu_card_count > 0) {
					table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
				}
			}

			// 发第一张牌
			table.exe_dispatch_card(table._current_player, Constants_WanZai.HU_CARD_TYPE_TIAN_HU, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_WanZai table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table._cur_banker);

		// tableResponse.setCurrentPlayer(seat_index);
		// tableResponse.setCellScore(0);

		// 状态变量
		// tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		// tableResponse.setOutCardData(0);
		// tableResponse.setOutCardPlayer(0);

		/**
		 * for (int i = 0; i < MJtable.getTablePlayerNumber(); i++) {
		 * tableResponse.addTrustee(false);// 是否托管 // 剩余牌数
		 * tableResponse.addDiscardCount(table.GRR._discard_count[i]);
		 * Int32ArrayResponse.Builder int_array =
		 * Int32ArrayResponse.newBuilder(); for (int j = 0; j < 55; j++) {
		 * int_array.addItem(table.GRR._discard_cards[i][j]); }
		 * tableResponse.addDiscardCards(int_array);
		 * 
		 * // 组合扑克 tableResponse.addWeaveCount(table.GRR._weave_count[i]);
		 * WeaveItemResponseArrayResponse.Builder weaveItem_array =
		 * WeaveItemResponseArrayResponse.newBuilder(); for (int j = 0; j <
		 * MJGameConstants.MAX_WEAVE; j++) { WeaveItemResponse.Builder
		 * weaveItem_item = WeaveItemResponse.newBuilder();
		 * weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
		 * weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
		 * weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
		 * weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
		 * weaveItem_array.addWeaveItem(weaveItem_item); }
		 * tableResponse.addWeaveItemArray(weaveItem_array);
		 * 
		 * // tableResponse.addWinnerOrder(0);
		 * 
		 * // 牌
		 * tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		 * }
		 * 
		 * // 数据 tableResponse.setSendCardData(0); int hand_cards[] = new
		 * int[MJGameConstants.MAX_COUNT]; int hand_card_count =
		 * table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index],
		 * hand_cards);
		 * 
		 * 
		 * for (int i = 0; i < MJGameConstants.MAX_COUNT; i++) {
		 * tableResponse.addCardsData(hand_cards[i]); }
		 **/
		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO Auto-generated method stub
		table.be_in_room_trustee(seat_index);
		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(Table_WanZai table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true && table._playerStatus[seat_index]._is_pao == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		roomResponse.setQiang(table._player_result.qiang[seat_index]);
		roomResponse.setPaoMax(table._playerStatus[seat_index]._is_pao ? -1 : 1);
		roomResponse.setQiangMax(table._playerStatus[seat_index]._is_pao_qiang ? -1 : 1);
		table.send_response_to_player(seat_index, roomResponse);

		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
