package com.cai.game.mj.chenchuang.jingdezhen;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_JING_DE_ZHEN;
import com.cai.common.domain.GangCardResult;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerNao_JingDeZhen extends AbstractMJHandler<Table_JingDeZhen> {

	protected GangCardResult m_gangCardResult;

	@Override
	public void exe(Table_JingDeZhen table) {
		
		if(!table.has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_MAI_MA)){
			exe_pao(table);
			return;
		}

		table._game_status = GameConstants.GS_MJ_NAO;// 设置状态
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
		table.load_room_info_data(roomResponse);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.qiang[i] = -1;
			table._player_result.pao[i] = -1;
		}
		table.operate_player_data();

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setGameStatus(table._game_status);
			roomResponse.setTarget(table._cur_banker);
			roomResponse.setNao(table._player_result.qiang[table._cur_banker]);
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_nao_zhuang(Table_JingDeZhen table, int seat_index, int nao) {
		if(seat_index != table._cur_banker)
			return false;
		if (table._is_nao_zhuang)
			return false;

		table._is_nao_zhuang = true;

		table._player_result.qiang[seat_index] = nao;

		table.operate_player_data();
		//关闭闲家等待界面
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(80);
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if(i == seat_index)
				continue;
			table.send_response_to_player(i, roomResponse);
		}
		
		exe_pao(table);
		return true;
	}

	private void exe_pao(Table_JingDeZhen table) {
		if(table.has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_MAI_PIAO)){
			//买飘
			table.set_handler(table._handler_pao);
			table._handler_pao.exe(table);
			return;
		}else{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._player_result.pao[i] = 0;
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
				roomResponse.setGameStatus(GameConstants.GAME_STATUS_PLAY);
				table.load_room_info_data(roomResponse);
				table.send_response_to_player(i, roomResponse);
			}
		}
		
		table.time_for_tou_zi_animation = 800;
		table.time_for_tou_zi_fade = 0;
		table.show_tou_zi(0);
		table._logic.clean_magic_cards();
		// 选鬼
		start(table);
		// 游戏开始
		/*GameSchedule.put(()->{
			
			GameSchedule.put(()->{}, 2, TimeUnit.SECONDS);
		}, 2, TimeUnit.SECONDS);*/
	}
	
	private void start(Table_JingDeZhen table) {
		table._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(table.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(table._current_player);
		gameStartResponse.setLeftCardCount(table.GRR._left_card_count);

		int hand_cards[][] = new int[table.getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			table.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			table.load_room_info_data(roomResponse);
			table.load_common_status(roomResponse);
			roomResponse.setZongliuzi(table.continueBankerCount == 0 ? -1 : table.continueBankerCount);

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
		table.exe_select_magic();
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
					table.GRR._weave_items[i], table.GRR._weave_count[i], i, 0);
			if (table._playerStatus[i]._hu_card_count > 0) {
				if(i != table._cur_banker)
					table.is_bao_ding[i] = 1;
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
			}
		}

		// 发第一张牌
		table.exe_dispatch_card(table._current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);
	}

	@Override
	public boolean handler_player_be_in_room(Table_JingDeZhen table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setZongliuzi(table.continueBankerCount == 0 ? -1 : table.continueBankerCount);
		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table._cur_banker);

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.be_in_room_trustee(seat_index);
		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(Table_JingDeZhen table, int seat_index) {
		if(seat_index != table._cur_banker || table._is_nao_zhuang){
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.qiang[seat_index]);
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
