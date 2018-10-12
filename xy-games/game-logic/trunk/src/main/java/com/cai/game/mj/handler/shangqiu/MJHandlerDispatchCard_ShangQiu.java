package com.cai.game.mj.handler.shangqiu;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_SQ;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_ShangQiu extends MJHandlerDispatchCard<MJTable_ShangQiu> {

	private boolean can_bao_ting;

	private boolean flag;

	// 发牌
	@Override
	public void exe(MJTable_ShangQiu table) {
		// 用户状态 清除
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 14) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			// 连庄
			table._cur_banker = table.GRR._banker_player;// (table._banker_select
															// + 1) %
															// MJGameConstants.GAME_PLAYER;

			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			table._lian_zhuang_player = table._cur_banker;

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
		if (table.DEBUG_CARDS_MODE || table.BACK_DEBUG_CARDS_MODE) {
			_send_card_data = 0x11;
		}

		table._provide_player = _seat_index;

		// 是否进行了亮牌补张
		flag = false;
		if (_type == GameConstants_SQ.DISPATCHCARD_TYPE_BU_HUA) {
			int sendType = 3;
			// 显示补花的牌
			if (table.has_rule(GameConstants_SQ.GAME_RULE_SQ_LIANG_SI_DA_YI)) {
				if (table.GRR.change_liang_bu_pai(card, _send_card_data, _seat_index)) {
					flag = true;
				}
			}
			table.operate_show_card_other(_seat_index, sendType);
		}

		// 补牌
		if (table._logic.is_hua_card(_send_card_data)) {

			table.GRR.addHuaCard(_seat_index, _send_card_data);
			// 发送数据
			// 只有自己才有数值
			int real_card = _send_card_data;
			if (flag) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
			}
			if (!table.has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING) || table._playerStatus[_seat_index].is_bao_ting() == false) {
				// 摸上来的牌也能听牌
				if (flag && table.GRR.get_liang_card_count(_seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
					for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
						if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
							can_bao_ting = true;
							// 添加动作
							break;
						}
					}
				} else {
					for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
						if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j] && !table.GRR.is_liang_pai(real_card, _seat_index)) {
							real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
							can_bao_ting = true;
							// 添加动作
							break;
						}
					}
				}
			}

			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

			// 效果
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BU_HUA }, 1,
					GameConstants.INVALID_SEAT);

			table.GRR._chi_hu_rights[_seat_index].hua_count++;// 花牌加一

			// 补花数量
			table.bu_hua_count++;

			// 发牌
			table.exe_dispatch_card(_seat_index, GameConstants_SQ.DISPATCHCARD_TYPE_BU_HUA, _send_card_data, 1000);
			return;

		} else if (table._logic.is_hua_card_array(table.GRR._cards_index[_seat_index])) {
			// 加到手牌
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			// 获取花牌
			int index = table._logic.rt_Hua_Index(table.GRR._cards_index[_seat_index]);
			if (index == GameConstants.INVALID_SEAT) {
				table.log_error("商丘麻将补牌出错");
				return;
			}
			int card = table._logic.switch_to_card_data(index);
			// 删除花牌
			table.GRR.addHuaCard(_seat_index, card);
			table.GRR._cards_index[_seat_index][index]--;

			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_COUNT];
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null, _send_card_data);

			// 效果
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BU_HUA }, 1,
					GameConstants.INVALID_SEAT);

			table.GRR._chi_hu_rights[_seat_index].hua_count++;// 花牌加一

			// 补花数量
			table.bu_hua_count++;

			// 发牌
			table.exe_dispatch_card(_seat_index, GameConstants_SQ.DISPATCHCARD_TYPE_BU_HUA, card, 0);
			return;
		}
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		can_bao_ting = false;

		// 没报听不能胡
		if (!table.has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING) || table._playerStatus[_seat_index].is_bao_ting() == true) {
			// 胡牌检测
			int action = GameConstants.WIK_NULL;
			action = table.analyse_chi_hu_card(_seat_index, table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO);// 自摸

			if (action != GameConstants.WIK_NULL) {
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
				chr.set_empty();
			}
		} else {

			// 能不能报听
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			// 加到手牌
			table.GRR._cards_index[_seat_index][send_card_index]++;
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

			// 减掉发的牌
			table.GRR._cards_index[_seat_index][send_card_index]--;

			// 如果可以报听,刷新自己的手牌
			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
			if (ting_count > 0) {

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

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);

				can_bao_ting = true;
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
			}
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// 发送数据
		// 只有自己才有数值
		int real_card = _send_card_data;
		if (flag) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
		}
		if (!table.has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING) || table._playerStatus[_seat_index].is_bao_ting() == false) {
			// 摸上来的牌也能听牌
			if (flag && table.GRR.get_liang_card_count(_seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
				for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
					if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
						can_bao_ting = true;
						break;
					}
				}
			} else {
				for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
					if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
						can_bao_ting = true;
						break;
					}
				}
			}
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true);
			if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
				boolean has_gang = false;

				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if ((table._playerStatus[_seat_index].is_bao_ting() == false
							|| table.check_gang_huan_zhang(_seat_index, m_gangCardResult.cbCardData[i]) == false)) {
						has_gang = true;
						// 加上杠
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}
				}

				if (has_gang == true) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);// 杠
				}
			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), SysParamServerUtil.auto_out_card_time_mj(),
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
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
	public boolean handler_operate_card(MJTable_ShangQiu table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 真实值获取
		operate_card = table.get_real_card(operate_card);

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		// 用户状态
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
		table.operate_player_action(_seat_index, true);

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			// 如果已经报听了。选择了不杠
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), SysParamServerUtil.auto_out_card_time_mj(),
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			// 可以报听,选择了不报听
			if (can_bao_ting) {
				// 刷新手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				// 刷新自己手牌
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null, _send_card_data);
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
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._cur_banker = _seat_index;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;

			// 结束
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_BAO_TING: //
		{
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

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// 亮张处理
		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD };
		int card_index = 0;// 特殊处理标示只处理一张重连
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = hand_cards[i];
			for (int k = 0; k < table.GRR.get_liang_card_count(seat_index); k++) {
				if (card_index == 0 && flag && index_card[k] == GameConstants.INVALID_CARD && real_card == _send_card_data) {
					index_card[k] = table.GRR.get_player_liang_card(seat_index, k);
					card_index++;
					continue;
				}
				if (index_card[k] == GameConstants.INVALID_CARD && real_card == table.GRR.get_player_liang_card(seat_index, k)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					index_card[k] = table.GRR.get_player_liang_card(seat_index, k);
				}
			}
			tableResponse.addCardsData(real_card);
		}

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

		int real_card = _send_card_data;
		if (flag) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
		}

		// 摸上来的牌也能听牌
		if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
			if (flag && table.GRR.get_liang_card_count(_seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
				for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
					if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
						can_bao_ting = true;
						break;
					}
				}
			} else {
				for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
					if (real_card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
						can_bao_ting = true;
						break;
					}
				}
			}
		}
		// 摸牌
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable_ShangQiu table, int seat_index, int card) {
		boolean liang_zhang_flag = false;
		if (card > GameConstants_SQ.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			liang_zhang_flag = true;
		}
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		// if (card == MJGameConstants.ZZ_MAGIC_CARD &&
		// table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
		// table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
		// table.log_error("癞子牌不能出癞子");
		// return false;
		// }

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.HU_CARD_TYPE_GANG_KAI);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL, liang_zhang_flag);
		}

		return true;
	}
}
