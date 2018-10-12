
package com.cai.game.hh.handler.xxphz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.mj.GameConstants_XiangXiang;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.game.hh.handler.HHHandlerDispatchCard;
import com.cai.redis.service.RedisService;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerDaTuo_XiangXiang extends HHHandlerDispatchCard<XiangXiangHHTable> {

	@Override
	public void exe(XiangXiangHHTable table) {

		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 有上庄
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = 0;
		}
		// table.operate_player_data();

		// return;
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
			roomResponse.setPaoMin(0);
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_HENAN);
			roomResponse.setPaoDes("当前可以撤跑,最多下跑3个");
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(XiangXiangHHTable table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		int p = table._player_result.pao[seat_index];

		table._player_result.pao[seat_index] = pao;

		RoomResponse.Builder paoBuilder = RoomResponse.newBuilder();
		paoBuilder.setType(MsgConstants.RESPONSE_PAO);
		paoBuilder.setPao(pao);
		paoBuilder.setOperatePlayer(seat_index);
		table.load_room_info_data(paoBuilder);
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.send_response_to_player(i, paoBuilder);
		}

		if (p != pao) {
			table.operate_player_data();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		// 都ok了
		// 游戏开始
		table._game_status = GameConstants_XiangXiang.GS_MJ_PLAY;
		// table.log_info("gme_status:" + table._game_status);

		// 庄家选择
		table.progress_banker_select();

		table.GRR._banker_player = table._cur_banker;
		table._current_player = table.GRR._banker_player;

		table._repertory_card = new int[GameConstants_XiangXiang.CARD_COUNT_PHZ_HS];
		table.shuffle(table._repertory_card, GameConstants_XiangXiang.CARD_PHZ_DEFAULT);


		if (table.DEBUG_CARDS_MODE || table.BACK_DEBUG_CARDS_MODE)
			table.test_cards();

		table._logic.clean_magic_cards();
		int playerCount = table.getPlayerCount();
		table.GRR._banker_player = table._current_player = table._cur_banker;
		// 游戏开始
		table._game_status = GameConstants_XiangXiang.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(table.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(table._current_player);
		gameStartResponse.setLeftCardCount(table.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants_XiangXiang.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[table.getTablePlayerNumber()];
		int ti_card_index[][] = new int[table.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = table._logic.get_action_ti_Card(table.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_XiangXiang.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			table.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			table.load_room_info_data(roomResponse);
			table.load_common_status(roomResponse);

			if (table._cur_round == 1) {
				table.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(table._current_player == GameConstants_XiangXiang.INVALID_SEAT ? table._resume_player : table._current_player);
			roomResponse.setLeftCardCount(table.GRR._left_card_count);
			roomResponse.setGameStatus(table._game_status);
			roomResponse.setLeftCardCount(table.GRR._left_card_count);

			int gameId = table.getGame_id() == 0 ? 8 : table.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
			table.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		table.load_room_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants_XiangXiang.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(table.GRR._left_card_count);
		table.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			table._playerStatus[i]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i],
					table.GRR._weave_items[i], table.GRR._weave_count[i], i, i);
			if (table._playerStatus[i]._hu_card_count > 0) {
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
			}
		}
		table._handler = table._handler_dispath_firstcards;
		table.exe_dispatch_first_card(table._current_player, GameConstants_XiangXiang.WIK_NULL, FlashTime + standTime);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(XiangXiangHHTable table, int seat_index) {

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
		 * for (int i = 0; i < XiangXiangHHTable.getTablePlayerNumber(); i++) {
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

		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(XiangXiangHHTable table, int seat_index) {
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
