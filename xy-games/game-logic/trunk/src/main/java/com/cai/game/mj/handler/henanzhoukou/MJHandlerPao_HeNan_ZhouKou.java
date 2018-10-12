package com.cai.game.mj.handler.henanzhoukou;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.lz.MJHandlerPao;
import com.cai.service.MongoDBServiceImpl;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPao_HeNan_ZhouKou extends MJHandlerPao {
	@Override
	public void exe(MJTable table) {
		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = 0;
		}
		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setPaoMin(0);
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_HENAN);
			roomResponse.setPaoDes("当前可以撤跑,最多下跑3个");
			table.send_response_to_player(i, roomResponse);
		}
	}

	@Override
	public boolean handler_pao_qiang(MJTable table, int seat_index, int pao, int qiang) {
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

		if (table._game_status == GameConstants.GS_MJ_PAO) {
			table.GRR._banker_player = table._current_player = table._cur_banker;

			table._game_status = GameConstants.GS_MJ_PLAY;
			GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
			gameStartResponse.setBankerPlayer(table.GRR._banker_player);
			gameStartResponse.setCurrentPlayer(table._current_player);
			gameStartResponse.setLeftCardCount(table.GRR._left_card_count);

			int hand_cards[][] = new int[table.getTablePlayerNumber()][GameConstants.MAX_COUNT];

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards[i]);
				gameStartResponse.addCardsCount(hand_card_count);
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				gameStartResponse.clearCardData();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					gameStartResponse.addCardData(hand_cards[i][j]);
				}

				table.GRR._video_recode.addHandCards(cards);

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				table.load_room_info_data(roomResponse);
				table.load_common_status(roomResponse);

				if (table._cur_round == 1) {
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

			table.exe_select_magic_card(table.GRR._banker_player);

			// 检测听牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i]._hu_card_count = table.get_henan_zhou_kou_ting_card(table._playerStatus[i]._hu_cards,
						table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], false, i);
				if (table._playerStatus[i]._hu_card_count > 0) {
					table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
				}
			}

			boolean is_qishou_hu = false;
			if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)
					|| table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					// 起手4个红中
					if (table.GRR._cards_index[i][table._logic.get_magic_card_index(0)] == 4) {

						table._playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
						table._playerStatus[i].add_zi_mo(table._logic.switch_to_card_data(table._logic.get_magic_card_index(0)), i);
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QI_SHOU_HU);
						table.exe_qi_shou_hu(i);

						is_qishou_hu = true;
						MongoDBServiceImpl.getInstance().card_log(table.get_players()[i], ECardType.hongZhong4, "", 0, 0l, table.getRoom_id());
						break;
					}
				}
			}

			if (is_qishou_hu == false) {
				table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}
		}

		// this.exe_dispatch_card(_current_player,true);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		if (table._shang_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._shang_zhuang_player);
		} else if (table._lian_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._lian_zhuang_player);
		} else {
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(MJTable table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[i]
		roomResponse.setPaoMin(0);
		roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_HENAN);
		roomResponse.setPaoDes("当前可以撤跑,最多下跑3个");
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
