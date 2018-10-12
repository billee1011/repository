package com.cai.game.mj.jiangxi.ruijin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 
 * @author Administrator
 *
 */
public class MJHandlerDispatchCard_RUIJIN extends MJHandlerDispatchCard<MJTable_RUIJIN> {
	public MJHandlerDispatchCard_RUIJIN() {
		m_gangCardResult = new GangCardResult(30);
	}
	 
	@Override
	public void exe(MJTable_RUIJIN table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].chi_peng_round_valid_ext(); // 可以碰了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._cur_banker = (table._cur_banker + 1) % table.getTablePlayerNumber();
			table.changeLiangZhuangCount();
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._provide_player = _seat_index;

		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x42;
		}
		// 记录摸牌次数
		table.addDispatchcardNum(_seat_index);
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
		}
		// 胡牌检测
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);// 自摸

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		// WalkerGeek 听牌数据
		int real_card = _send_card_data;
		if (table.is_bao_pai(real_card)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_BAO;
		}
		// 发送数据 只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		// 设置变量
		table._provide_card = _send_card_data;

		int type = estimate_gang_card_dispatch_card(table, _seat_index);
		for (int i = 0; i < type; i++) {
			curPlayerStatus.add_action(GameConstants.WIK_GANG);
			curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, 1); // 杠
		}
		if (curPlayerStatus.has_action()) {
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			// 自动出牌
			table.operate_player_status();
		}

		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_RUIJIN table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			if (table._playerStatus[_seat_index].has_zi_mo() && operate_code != GameConstants.WIK_ZI_MO) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		if (table._playerStatus[seat_index].has_zi_mo() && operate_code != GameConstants.WIK_ZI_MO) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}
		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
							false);
					return true;
				}
			}

		}
			break;
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);
			// 下局胡牌的是庄家
			table._cur_banker = _seat_index;
			table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			table.changeLiangZhuangCount();

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;

			// 结束
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_RUIJIN table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
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
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(
							table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_BAO);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addWinnerOrder(0);
			// 牌
			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}
		table.changCard(hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;
		if (table.is_bao_pai(real_card)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_BAO;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	public int estimate_gang_card_dispatch_card(MJTable_RUIJIN table, int seatIndex) {
		m_gangCardResult.cbCardCount = 0;

		// 暗杠
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table.GRR._cards_index[seatIndex][i] == 4 && !table._logic.is_magic_index(i)) {
				m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_AN_GANG;
				m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = table._logic.switch_to_card_data(i);
			}
		}

		List<Integer> huaCards = new ArrayList<Integer>();
		int magicCard = table._logic.switch_to_card_data(table._logic.get_magic_card_index(0));
		int count = table.GRR._cards_index[seatIndex][table._logic.get_magic_card_index(0)];
		
		// 花牌数量
		for (int i = 0; i < 4; i++) {
			int index = table._logic.switch_to_card_index(0x38) + i;
			if (table.GRR._cards_index[seatIndex][index] > 0) {
				huaCards.add(table._logic.switch_to_card_data(index));
			}
		}
		// 花牌是万能牌的杠
		if (!table._logic.get_has_jia_bao()) {
			/*if (huaCards.size() == 4) {
				int center_card = 0;
				int index = -1;
				for (int i = 0; i < huaCards.size(); i++) {
					int[] rtArr = table._logic.build_center_card(center_card, huaCards.get(i), index);
					index = rtArr[0];
					center_card = rtArr[1];
				}
				m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_AN_GANG;
				m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = center_card;
			}*/
		} else {
			// 花牌和万能牌的杠
			if (huaCards.size() > 0 && count + huaCards.size() >= 4) {
				/*for (int k = 0; k < (count == 4 ? count - 1 : count); k++) {
					huaCards.add(magicCard);
				}*/
				
				/*if(count > 0 ){
					huaCards.add(magicCard);
				}*/
				
				int center_card = 0;
				int index = -1;
				if(huaCards.size() == 4){
					for (int i = 0; i < huaCards.size() - 3; i++) {
						int[] rtArr = table._logic.build_center_card(center_card, huaCards.get(i), index);
						int index2 = rtArr[0];
						int center_card1 = rtArr[1];
						for (int j = i + 1; j < huaCards.size() - 2; j++) {
							int[] rtArr1 = table._logic.build_center_card(center_card1, huaCards.get(j), index2);
							int center_card_hua = rtArr1[1];
							int index3 = rtArr1[0];
							for (int k = j + 1; k < huaCards.size() - 1; k++) {
								int[] rtArr2 = table._logic.build_center_card(center_card_hua, huaCards.get(k), index3);
								int center_card4 = rtArr2[1];
								int index4 = rtArr2[0];
								for (int u = k + 1; u < huaCards.size(); u++) {
									int[] rtArr3 = table._logic.build_center_card(center_card4, huaCards.get(u), index4);
									m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_AN_GANG;
									m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = rtArr3[1];
								}
							}
						}
					}
				}
				/*//移除牌
				for (int i = 0; i < huaCards.size(); i++) {
					if(huaCards.get(i) == magicCard){
						huaCards.remove(i);
					}
				}
				//二张牌宝牌的暗杆
				if(count >= 2 && huaCards.size() >= 2){
					int center_card2 = 0;
					int index2 = -1;
					for (int i = 0; i < 2; i++) {
						int[] rtArr = table._logic.build_center_card(center_card2, magicCard, index2);
						index2 = rtArr[0];
						center_card2 = rtArr[1];
					}
					
					for (int k = 0; k < huaCards.size() - 1; k++) {
						int[] rtArr2 = table._logic.build_center_card(center_card2, huaCards.get(k), index2);
						int center_card4 = rtArr2[1];
						int index4 = rtArr2[0];
						for (int u = k + 1; u < huaCards.size(); u++) {
							int[] rtArr3 = table._logic.build_center_card(center_card4, huaCards.get(u), index4);
							m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_AN_GANG;
							m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = rtArr3[1];
						}
					}
				}
				//三张宝牌的暗杆
				if(count >= 3 && huaCards.size() >= 1){
					int center_card2 = 0;
					int index2 = -1;
					for (int i = 0; i < 3; i++) {
						int[] rtArr = table._logic.build_center_card(center_card2, magicCard, index2);
						index2 = rtArr[0];
						center_card2 = rtArr[1];
					}
					
					for (int k = 0; k < huaCards.size(); k++) {
						int[] rtArr2 = table._logic.build_center_card(center_card2, huaCards.get(k), index2);
						m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_AN_GANG;
						m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = rtArr2[1];
					}
				}*/
				
			}
		}

		// 明杠
		for (int i = 0; i < table.GRR._weave_count[seatIndex]; i++) {
			if (table.GRR._weave_items[seatIndex][i].weave_kind == GameConstants.WIK_PENG) {
				int card = table.GRR._weave_items[seatIndex][i].center_card; // 牌值还原
				int has_hua = table.getTwo(card); // 取第个值，如果有值就是花碰万能牌
				if (has_hua == 0) {
					if(card == _send_card_data){
						/*int index = table._logic.switch_to_card_index(card);
						if (table.GRR._cards_index[seatIndex][index] > 0) {*/
							m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_ADD_GANG;
							m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = card;
						//}
					}
				} else {
					if (table._logic.get_has_jia_bao()) {
						/*if(count > 0){
							huaCards.add(magicCard);
						}*/

						for (Integer integer : huaCards) {
							int[] rtArr = table._logic.build_center_card(card, integer, 5);
							m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_ADD_GANG;
							m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = rtArr[1];
						}
					} /*else {
						int[] rtArr = table._logic.build_center_card(card, huaCards.get(0), 5);
						m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_ADD_GANG;
						m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = rtArr[1];
					}*/
				}
			}
		}
		return m_gangCardResult.cbCardCount;
	}
}
