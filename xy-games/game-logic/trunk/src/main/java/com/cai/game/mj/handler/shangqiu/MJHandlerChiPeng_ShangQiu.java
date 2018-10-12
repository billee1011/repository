package com.cai.game.mj.handler.shangqiu;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_SQ;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_ShangQiu extends MJHandlerChiPeng<MJTable_ShangQiu> {
	private GangCardResult m_gangCardResult;
	private boolean can_bao_ting;

	public MJHandlerChiPeng_ShangQiu() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(MJTable_ShangQiu table) {
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		// 遍历亮牌给碰牌变灰
		int liang_card_hand[] = table.GRR.get_player_liang_cards(_seat_index);
		for (int i = 0; i < table.GRR.get_liang_card_count(_seat_index); i++) {
			if (liang_card_hand[i] == _card) {
				int num = table.GRR._weave_items[_seat_index][wIndex].client_special_count;
				table.GRR._weave_items[_seat_index][wIndex].client_special_card[num] = _card;
				table.GRR._weave_items[_seat_index][wIndex].client_special_count++;
			}
		}

		// 设置用户
		table._current_player = _seat_index;

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];// table.GRR._weave_items[_seat_index];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;

			weaves[i].client_special_card = table.GRR._weave_items[_seat_index][i].client_special_card;
			weaves[i].client_special_count = table.GRR._weave_items[_seat_index][i].client_special_count;
		}
		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);
		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		// 刷新亮牌
		if (table.has_rule(GameConstants_SQ.GAME_RULE_SQ_LIANG_SI_DA_YI)) {
			if (table.GRR.remove_liang_pai(_card, _seat_index)) {
				table.operate_show_card_other(_seat_index, GameConstants_SQ.SEND_OTHER_TYPE1);
			}
		}
		// 回放
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		/////////////////////////////////////// 可以碰，说明没有报听
		m_gangCardResult.cbCardCount = 0;
		// 如果牌堆还有牌，判断能不能杠
		if (table.GRR._left_card_count > 0) {
			// 只检测手上的牌能不能杠
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, false);

			if (cbActionMask != 0) {
				boolean flag = true;
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上杠
					if (table.has_rule(GameConstants_SQ.GAME_RULE_SQ_YOU_SHU_WU_HUA) && 0x36 == m_gangCardResult.cbCardData[i]) {
						flag = false;
					} else {
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}
				}
				if (flag) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);// 转转就是杠
				}
			}
		}

		int count = 0;
		int ting_count = 0;
		if (table.GRR.get_liang_card_count(_seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
			// 当亮张有4张的时候只报听这4张
			for (int i = 0; i < GameConstants_SQ.GAME_LIANG_ZHANG_MAX; i++) {
				// 发财当花 牌用
				int card = table.GRR.get_player_liang_card(_seat_index, i);
				if (table.has_rule(GameConstants_SQ.GAME_RULE_SQ_YOU_SHU_WU_HUA) && card == 0x36) {
					continue;
				}
				int index = table._logic.switch_to_card_index(card);
				count = table.GRR._cards_index[_seat_index][index];
				if (count > 0) {
					// 假如打出这张牌
					table.GRR._cards_index[_seat_index][index]--;

					// 检查打出哪一张牌后可以听牌
					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(_seat_index,
							table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = card;

						// 能听牌
						ting_count++;
					}

					// 加回来
					table.GRR._cards_index[_seat_index][index]++;
				}
			}
		} else {
			int[] liang_cards = new int[] { -1, -1, -1, -1 };
			for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
				// 发财当牌用
				if (table.has_rule(GameConstants_SQ.GAME_RULE_SQ_YOU_SHU_WU_HUA) && table._logic.switch_to_card_data(i) == 0x36) {
					continue;
				}

				// 不能听亮张
				if (table.GRR.is_liang_pai(table._logic.switch_to_card_data(i), _seat_index, liang_cards)) {
					int card_count = table.GRR._cards_index[_seat_index][i];
					int liang_count = table.GRR.get_liang_pai_count_by_card(table._logic.switch_to_card_data(i), _seat_index);
					if (card_count - liang_count == 0) {
						continue;
					}
				}

				count = table.GRR._cards_index[_seat_index][i];
				if (count > 0) {
					// 假如打出这张牌
					table.GRR._cards_index[_seat_index][i]--;

					// 检查打出哪一张牌后可以听牌
					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(_seat_index,
							table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);
						// 能听牌
						ting_count++;
					}

					// 加回来
					table.GRR._cards_index[_seat_index][i]++;
				}
			}
		}

		// 如果可以报听,刷新自己的手牌
		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
		if (ting_count > 0 && table.has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING)) {

			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_COUNT];
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			// 特殊处理亮牌听牌
			if (table.GRR.get_liang_card_count(_seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
				int[] ting_card_index = new int[ting_count];
				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (ting_card_index[j] == 0 && cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
							ting_card_index[j] = 1;
						}
					}
				}
			} else {
				int[] liang_cards = new int[] { -1, -1, -1, -1 };
				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]
								&& !table.GRR.is_liang_pai(cards[i], _seat_index, liang_cards)) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						}
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, weave_count, weaves);

			can_bao_ting = true;
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
		} else {

			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_COUNT];
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves, GameConstants.INVALID_CARD);
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);

		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable_ShangQiu table, int seat_index, int card) {
		boolean liang_zhang_flag = false;
		if (card > GameConstants_SQ.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			liang_zhang_flag = true;
		}
		card = table.get_real_card(card);
		// 错误断言
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌--执行出牌切换状态
		table.exe_out_card(_seat_index, card, _action, liang_zhang_flag);

		return true;
	}

	/***
	 * //用户操作--碰了之后杆
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_ShangQiu table, int seat_index, int operate_code, int operate_card) {
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
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			// 可以报听,选择了不报听
			if (can_bao_ting) {
				// 刷新手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				// 刷新自己手牌
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null, 0);
			}

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
		case GameConstants.WIK_BAO_TING: //
		{
			// 获取真实值
			operate_card = table.get_real_card(operate_card);
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
			/*
			 * boolean liang_zhang_flag = false;
			 * if(table.GRR.get_liang_card_count(_seat_index) ==
			 * GameConstants.MAX_LAOPAI_COUNT){ liang_zhang_flag = true; }
			 * table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL,
			 * liang_zhang_flag);
			 */
			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShangQiu table, int seat_index) {
		// 重连初始化花牌亮牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.operate_show_card_other(i, GameConstants_SQ.SEND_OTHER_TYPE3);
		}

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
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					if (i != seat_index) {
						int_array.addItem(GameConstants.BLACK_CARD);

					} else {
						int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);
					}
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				// 客户端特殊处理的牌值
				for (int k = 0; k < table.GRR._weave_items[i][j].client_special_count; k++) {
					weaveItem_item.addClientSpecialCard(table.GRR._weave_items[i][j].client_special_card[k]);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// if(table.has){}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		// 亮张处理
		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD };
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = hand_cards[i];
			if (table.GRR.is_liang_pai(real_card, seat_index, index_card)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
			}
			tableResponse.addCardsData(real_card);
		}
		/*
		 * for (int k = 0; k < table.GRR.get_liang_card_count(seat_index); k++)
		 * { if (index_card[k] == GameConstants.INVALID_CARD && real_card ==
		 * table.GRR.get_player_liang_card(seat_index,k)) { real_card +=
		 * GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG; index_card[k] =
		 * table.GRR.get_player_liang_card(seat_index,k); } }
		 */
		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		if (_seat_index == seat_index) {
			if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
				// 如果可以报听,刷新自己的手牌
				int ting_count = table._playerStatus[_seat_index]._hu_out_card_count;
				if (ting_count > 0) {
					// 特殊处理亮牌听牌
					if (table.GRR.get_liang_card_count(_seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
						int[] ting_card_index = new int[ting_count];
						for (int i = 0; i < hand_card_count; i++) {
							for (int j = 0; j < ting_count; j++) {
								if (ting_card_index[j] == 0 && hand_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
									hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
									ting_card_index[j] = 1;
								}
							}
						}
					} else {
						int[] liang_cards = new int[] { -1, -1, -1, -1 };
						for (int i = 0; i < hand_card_count; i++) {
							for (int j = 0; j < ting_count; j++) {
								if (hand_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]
										&& !table.GRR.is_liang_pai(hand_cards[i], _seat_index, liang_cards)) {
									hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
								}
							}
						}
					}

					table.operate_player_cards_with_ting(_seat_index, hand_card_count, hand_cards, 0, null);
				}
			}
		} else {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}
		}

		/*
		 * // 听牌显示 int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		 * int ting_count = table._playerStatus[seat_index]._hu_card_count;
		 * 
		 * if (ting_count > 0) { table.operate_chi_hu_cards(seat_index,
		 * ting_count, ting_cards); }
		 */

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
