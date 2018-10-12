package com.cai.game.hh.handler.sybp;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.hh.handler.HHHandlerChiPeng;
import com.cai.game.hh.handler.nxphz.NingXiangPHZUtils;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 
 * @author admin
 *
 */
public class PHZHandlerChiPeng_ShaoYangBoPi extends HHHandlerChiPeng<ShaoYangBoPiHHTable> {

	public PHZHandlerChiPeng_ShaoYangBoPi() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(ShaoYangBoPiHHTable table) {
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic
				.get_weave_hu_xi(table.GRR._weave_items[_seat_index][wIndex]);
		// 设置用户
		table._current_player = _seat_index;

		// 效果
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS;

		if (_lou_card == -1 || (eat_type & _action) == 0) {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
					1, GameConstants.INVALID_SEAT);
		} else {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_LOU }, 1, GameConstants.INVALID_SEAT);
		}
		if (_type == GameConstants.CHI_PENG_TYPE_OUT_CARD) {
			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		} else if (_type == GameConstants.CHI_PENG_TYPE_DISPATCH) {
			table.log_info(table._last_player + "CHI_PENG_TYPE_DISPATCH");
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT, false);
		}

		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		// 先注释掉，等客户端一起联调
		for (int x = 0; x < hand_card_count; x++) {
			if (table.is_card_has_wei(cards[x])) { // 如果是偎的牌
				// 判断打出这张牌是否能听牌
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]--;
				boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]++;

				if (b_is_ting_state)
					cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
				else
					cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
			}
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		int an_long_Index[] = new int[5];
		int an_long_count = 0;

		// 玩家出牌 响应判断,是否有提 暗龙
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;
			}
		}
		if (an_long_count > 0) {
			int _action = GameConstants.WIK_AN_LONG;
			// 效果
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
					1, GameConstants.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic
						.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
						.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;

				// 删除手上的牌
				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;
				table.GRR._card_count[_seat_index] = table._logic
						.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
			}
			// 刷新手牌包括组合
			cards = new int[GameConstants.MAX_HH_COUNT];
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			// 先注释掉，等客户端一起联调
			for (int x = 0; x < hand_card_count; x++) {
				if (table.is_card_has_wei(cards[x])) { // 如果是偎的牌
					// 判断打出这张牌是否能听牌
					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]--;
					boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]++;

					if (b_is_ting_state)
						cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
					else
						cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
				}
			}

			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
		}

		if (an_long_count >= 2) {
			table._ti_two_long[_seat_index] = true;
			repairCard(table, an_long_count - 1, true);
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
		} else if ((table._ti_mul_long[_seat_index] > 1)) {
			table._ti_mul_long[_seat_index]--;
			table._ti_two_long[_seat_index] = true;
		}

		if (table._ti_two_long[_seat_index] == false) {
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			// table.log_player_error(_seat_index, "吃或碰出牌状态");
		} else {
			table._ti_two_long[_seat_index] = false;
			NingXiangPHZUtils.setNextPlay(table, _seat_index, 500, 0, "吃或碰，下家发牌");
		}

	}

	public void repairCard(ShaoYangBoPiHHTable table, int anLongCount, boolean reRepair) {
		int an_long_Index[] = new int[5];
		int an_long_count = anLongCount;
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;
			}
		}
		if (reRepair) {
			an_long_count++;
		}
		if (an_long_count > 0) {
			int _action = GameConstants.WIK_AN_LONG;
			// 效果
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
					1, GameConstants.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				if (an_long_Index[i] > 0) {
					int cbWeaveIndex = table.GRR._weave_count[_seat_index];
					table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic
							.switch_to_card_data(an_long_Index[i]);
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
							.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
					table.GRR._weave_count[_seat_index]++;
					table._long_count[_seat_index]++;
					// 删除手上的牌
					table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;

					table.GRR._card_count[_seat_index] = table._logic
							.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
				}
			}

			for (int i = 1; i < an_long_count; i++) { // 超过一提以上要补张 多一提就多补一张
				table._send_card_count++;
				int card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				--table.GRR._left_card_count;
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(card)]++;
			}
			if (an_long_count > 1) {
				repairCard(table, 0, true);
			}
		}
	}

	/***
	 * 用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	public boolean handler_operate_card(ShaoYangBoPiHHTable table, int seat_index, int operate_code, int operate_card,
			int lou_pai) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("HHHandlerChiPeng_YX 没有这个操作:" + operate_code);
			return false;
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
				1);
		// if (seat_index != _seat_index) {
		// table.log_info("HHHandlerChiPeng_YX 不是当前玩家操作");
		// return false;
		// }

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(ShaoYangBoPiHHTable table, int seat_index, int card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if (playerStatus.get_status() != GameConstants.Player_Status_OUT_CARD) {
			table.log_error("状态不对不能出牌");
			return false;
		}
		// TODO Auto-generated method stub
		int cards_index[] = new int[table.GRR._cards_index[seat_index].length];
		for (int i = 0; i < cards_index.length; i++) {
			cards_index[i] = table.GRR._cards_index[seat_index][i];
		}
		cards_index[table._logic.switch_to_card_index(card)]--;
		int ting_count = table.get_hh_ting_card_twenty(table._playerStatus[seat_index]._hu_cards, cards_index,
				table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index, seat_index);
		boolean is_wei = table.is_card_has_wei(card);

		// 打牌后没有听胡 且这张牌被其他人畏了 则不能出
		if (ting_count <= 0) {
			if (is_wei) {
				table.log_info(seat_index + "出牌出错 PHZHandlerChiPeng_ShaoYangBoPi " + seat_index);
				return false;
			}
		} else if (ting_count > 0 && is_wei) {
			table.has_shoot[seat_index] = true;
		}
		return super.handler_player_out_card(table, seat_index, card);
	}

	@Override
	public boolean handler_player_be_in_room(ShaoYangBoPiHHTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);// 加载房间的玩法 状态信息
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

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
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
							&& table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						if (table.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)
								&& table.has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
								&& table._xt_display_an_long[i] == true)
							weaveItem_item.setCenterCard(0);
						else
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addHuXi(table._hu_xi[i]);
			//
			tableResponse.addWinnerOrder(0);

			// 牌
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 先注释掉，等客户端一起联调
		for (int x = 0; x < hand_card_count; x++) {
			if (table.is_card_has_wei(hand_cards[x])) { // 如果是偎的牌
				// 判断打出这张牌是否能听牌
				table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]--;
				boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[seat_index],
						table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
				table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]++;

				if (b_is_ting_state)
					hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
				else
					hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

		table.istrustee[seat_index] = false;

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
