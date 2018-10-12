package com.cai.game.mj.handler.shangqiu;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPaoQiang_ShangQiu extends AbstractMJHandler<MJTable_ShangQiu> {

	// 执行炮呛
	@Override
	public void exe(MJTable_ShangQiu table) {

		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态
		// TODO Auto-generated method stub

		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			RoomResponse.Builder rp = RoomResponse.newBuilder();
			rp.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);//
			table.load_player_info_data(rp);
			table.send_response_to_player(i, rp);
		}

		if (table._shang_zhuang_player != GameConstants.INVALID_SEAT) {
			// 有上庄--把呛清掉
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._player_result.qiang[i] = 0;
			}
			table.operate_player_data();
		}

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			if (i == table._shang_zhuang_player) {
				// 上庄

				// 庄家不能呛
				roomResponse.setQiang(table._player_result.qiang[i]);
				roomResponse.setQiangMin(table._player_result.qiang[i]);
				roomResponse.setQiangMax(table._player_result.qiang[i]);
				roomResponse.setQiangDes("本局您是庄家,不能加呛");

				table._player_result.pao[i] = 0;

				// 上庄时可以撤销 跑分
				if (table.has_rule(GameConstants.GAME_RULE_HENAN_XIAPAO)) {
					roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
					roomResponse.setPaoMin(0);
					roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT);
					roomResponse.setPaoDes("当前可以撤跑,最多下跑2个");
				} else {
					// 庄家不能下跑
					roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
					roomResponse.setPaoMin(0);
					roomResponse.setPaoMax(table._player_result.pao[i]);
					roomResponse.setPaoDes("本局您是庄家,不能下跑");
				}
			} else if (i == table._lian_zhuang_player) {
				// 连庄

				// 庄家不能呛
				roomResponse.setQiang(table._player_result.qiang[i]);
				roomResponse.setQiangMin(table._player_result.qiang[i]);
				roomResponse.setQiangMax(table._player_result.qiang[i]);
				roomResponse.setQiangDes("本局您是庄家,不能加呛");

				table._player_result.pao[i] = 0;

				if (table.has_rule(GameConstants.GAME_RULE_HENAN_XIAPAO)) {
					roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
					roomResponse.setPaoMin(table._player_result.pao[i]);
					roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT);
					roomResponse.setPaoDes("最多下跑2个");
				} else {
					// 庄家不能下跑

					roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
					roomResponse.setPaoMin(table._player_result.pao[i]);
					roomResponse.setPaoMax(table._player_result.pao[i]);
					roomResponse.setPaoDes("本局您是庄家,不能下跑");
				}
			} else {
				roomResponse.setQiang(table._player_result.qiang[i]);
				roomResponse.setQiangMin(table._player_result.qiang[i]);
				roomResponse.setQiangMax(table._player_result.qiang[i] + table._qiang_max_count);
				if (table._qiang_max_count > 0) {
					roomResponse.setQiangDes("庄家连庄,当前最多下呛" + (table._player_result.qiang[i] + table._qiang_max_count) + "个");
				} else {
					roomResponse.setQiangDes("当前不能加呛");
				}

				roomResponse.setPao(table._player_result.pao[i]);
				roomResponse.setPaoMin(table._player_result.pao[i]);
				roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT);
				roomResponse.setPaoDes("最多下跑2个");

			}
			// table.GRR.add_room_response(roomResponse);
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable_ShangQiu table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		int p = table._player_result.pao[seat_index];
		int q = table._player_result.qiang[seat_index];

		table._player_result.pao[seat_index] = pao;
		table._player_result.qiang[seat_index] = qiang;

		if (p != pao || q != qiang) {// 跟上一把炮呛分不一样 刷新信息
			table.operate_player_data();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PAO) {
			table._game_status = GameConstants.GS_MJ_PLAY;

			table.GRR._banker_player = table._current_player = table._cur_banker;
			// 都ok了
			// 游戏开始

			table.real_game_start();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShangQiu table, int seat_index) {

		// 重连初始化花牌亮牌
		/*
		 * for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		 * table.operate_show_card_other(i, GameConstants_SQ.SEND_OTHER_TYPE3);
		 * }
		 */

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
		 * for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
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

	private void player_reconnect(MJTable_ShangQiu table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder rp = RoomResponse.newBuilder();
		rp.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);//
		table.load_player_info_data(rp);
		table.send_response_to_player(seat_index, rp);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据

		if (seat_index == table._shang_zhuang_player) {
			// 上庄

			// 庄家不能呛
			roomResponse.setQiang(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMin(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMax(table._player_result.qiang[seat_index]);
			roomResponse.setQiangDes("本局您是庄家,不能加呛");

			// 上庄时可以撤销 跑分
			if (table.has_rule(GameConstants.GAME_RULE_HENAN_XIAPAO)) {
				roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[seat_index]
				roomResponse.setPaoMin(0);
				roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT);
				roomResponse.setPaoDes("当前可以撤跑,最多下跑2个");
			} else {
				// 庄家不能下跑
				// table._player_result.pao[seat_index]=0;
				roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[seat_index]
				roomResponse.setPaoMin(0);
				roomResponse.setPaoMax(table._player_result.pao[seat_index]);
				roomResponse.setPaoDes("本局您是庄家,不能下跑");
			}
		} else if (seat_index == table._lian_zhuang_player) {
			// 连庄

			// 庄家不能呛
			roomResponse.setQiang(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMin(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMax(table._player_result.qiang[seat_index]);
			roomResponse.setQiangDes("本局您是庄家,不能加呛");

			if (table.has_rule(GameConstants.GAME_RULE_HENAN_XIAPAO)) {
				roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[seat_index]
				roomResponse.setPaoMin(table._player_result.pao[seat_index]);
				roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT);
				roomResponse.setPaoDes("最多下跑2个");
			} else {
				// 庄家不能下跑
				roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[seat_index]
				roomResponse.setPaoMin(table._player_result.pao[seat_index]);
				roomResponse.setPaoMax(table._player_result.pao[seat_index]);
				roomResponse.setPaoDes("本局您是庄家,不能下跑");
			}
		} else {
			roomResponse.setQiang(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMin(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMax(table._player_result.qiang[seat_index] + table._qiang_max_count);
			if (table._qiang_max_count > 0) {
				roomResponse.setQiangDes("庄家连庄,当前最多下呛" + (table._player_result.qiang[seat_index] + table._qiang_max_count) + "个");
			} else {
				roomResponse.setQiangDes("当前不能加呛");
			}

			roomResponse.setPao(table._player_result.pao[seat_index]);
			roomResponse.setPaoMin(table._player_result.pao[seat_index]);
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT);
			roomResponse.setPaoDes("最多下跑2个");

		}

		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
