package com.cai.game.mj.shanxi.tuidaohu;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_SXTDH extends MJHandlerChiPeng<MJTable_SXTDH> {
	protected GangCardResult m_gangCardResult;

	public MJHandlerChiPeng_SXTDH() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(MJTable_SXTDH table) {
		// 清空所有玩家的动作
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		// 获取当前玩家的状态，并且重置一部分的玩家状态，像弃胡这些状态是不能再reset方法里重置的
		PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
		currentPlayerStatus.reset();

		// 有杠不杠来碰，以后不让你杠了
		if (table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)] == 1) {// 碰完之后手上还有一张这个牌
			table._playerStatus[_seat_index].add_cards_abandoned_gang(table._logic.switch_to_card_index(_card));
		}

		// 将吃或者碰的牌，加入到落地牌组合
		int weave_index = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][weave_index].public_card = 1;
		table.GRR._weave_items[_seat_index][weave_index].center_card = _card;
		table.GRR._weave_items[_seat_index][weave_index].weave_kind = _action;
		table.GRR._weave_items[_seat_index][weave_index].provide_player = _provider;

		// 牌桌上当前的玩家，就是进行了吃或者碰的玩家
		table._current_player = _seat_index;

		// 玩家点了吃或者碰之后，客户端界面会弹出来响应的‘吃’或者‘碰’的动画
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		// 从牌桌上删除出牌人出的那张牌
		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		// 刷新进行吃或者碰的玩家手里的牌，把吃牌的牌显示在落地牌里，更新cards_index
		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		// 将手上牌的索引转变成实际的牌值，返回值是吃碰之前的手牌数目
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], hand_cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			if (table._logic.is_magic_index(i)) {
				continue;
			}

			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

					ting_count++;

				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			int tmp_cards[] = new int[GameConstants.MAX_COUNT];
			int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);

			for (int i = 0; i < tmp_hand_card_count; i++) {
				if (table._logic.is_magic_card(tmp_cards[i])) {
					tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else {
					for (int j = 0; j < ting_count; j++) {
						if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}
			}
			// 给牌加角标然后刷新
			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
			// 通知客户端报听
			if (table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING)) {
				currentPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
			}
		} else {
			// 刷新手牌
			table.operate_player_cards(_seat_index, hand_card_count, hand_cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);

		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 碰完牌是不能直接开杠的，特殊情况（直杠不杠先碰，以后不能杠；补杠过圈后可以杠，暗杠过圈后也可以杠。）
		m_gangCardResult.cbCardCount = 0;

		// 吃碰之后不进行杠牌判断

		// 判断玩家有没有杠牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
		if (currentPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	@Override
	public boolean handler_operate_card(MJTable_SXTDH table, int seat_index, int operate_code, int operate_card) {
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

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}

		}
			break;
		case GameConstants.WIK_BAO_TING: // 报听
		{
			operate_card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			// 效验参数
			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}
		default:
			return false;

		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_SXTDH table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

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
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		/*
		 * tableResponse.setSendCardData(0); int cards[] = new
		 * int[GameConstants.MAX_COUNT];
		 */
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		/**
		 * // 听牌显示 int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		 * int ting_count = table._playerStatus[seat_index]._hu_card_count;
		 * 
		 * if (ting_count > 0) { table.operate_chi_hu_cards(seat_index,
		 * ting_count, ting_cards); }
		 **/

		table.send_response_to_player(seat_index, roomResponse);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(MJTable_SXTDH table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		table.exe_out_card(_seat_index, card, 0);

		return true;
	}
}
