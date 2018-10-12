package com.cai.game.mj.handler.henansmx;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPao_HeNan_smx extends AbstractMJHandler<MJTable_SMX> {

	@Override
	public void exe(MJTable_SMX table) {

		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 有上庄
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = 0;
			table._player_result.biaoyan[i] = 0;
		}
		table.operate_player_data();
		// for (int i = 0; i < MJtable.getTablePlayerNumber(); i++) {
		// handler_pao_qiang(table,i,0,0);
		// }

		// return;
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
			roomResponse.setPaoMin(0);
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_HENAN_SMX);
			roomResponse.setPaoDes("当前可以撤跑,最多下跑5个");
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable_SMX table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		int p = table._player_result.pao[seat_index];

		table._player_result.pao[seat_index] = pao;

		if (p != pao) {
			table.operate_player_data();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_SMX_BIAO_YAN_FEN)) {
			table.exe_biaoyan();
			return true;
		} else {
			if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)
					|| GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_SMX_SHANG_HUN)) {
				table.exe_hun(table.GRR._banker_player);
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PAO) {
			table._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
			table.GRR._banker_player = table._current_player = table._cur_banker;
			// 都ok了
			// 游戏开始

			GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
			// gameStartResponse.setSiceIndex(rand);
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

			// 检测听牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i]._hu_card_count = table.get_henan_smx_ting_card(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i],
						table.GRR._weave_items[i], table.GRR._weave_count[i]);
				if (table._playerStatus[i]._hu_card_count > 0) {
					table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
				}
			}

			table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_SMX table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		if (table._shang_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._shang_zhuang_player);
		} else if (table._lian_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._lian_zhuang_player);
		} else {
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO Auto-generated method stub

		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(MJTable_SMX table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[i]
		roomResponse.setPaoMin(0);
		roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_HENAN);
		roomResponse.setPaoDes("当前可以撤跑,最多下跑3个");
		table.send_response_to_player(seat_index, roomResponse);

		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
