package com.cai.game.mj.shanxi.xiangyuan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_XIANG_YUAN;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_ShanXi_XiangYuan extends MJHandlerDispatchCard<MJTable_ShanXi_XiangYuan> {
	protected GangCardResult m_gangCardResult;
	private boolean ting_send_card = false;
	protected int _type;

	public MJHandlerDispatchCard_ShanXi_XiangYuan() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@Override
	public void exe(MJTable_ShanXi_XiangYuan table) {
		// 用户状态 清除
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		// 没有任何操作的情况下 是12张为荒庄 荒庄结束
		if (table.GRR._left_card_count <= table.huang_zhuang_count + table.gang_count * 2) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			if (table.has_rule(Constants_MJ_XIANG_YUAN.GAME_RULE_LUN_ZHUANG)) {
				table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1)
						% table.getTablePlayerNumber();
			} else {
				table._cur_banker = table._cur_banker; // 上一家的庄家继续坐庄
			}
			// 流局
			table.last_cur_banker = table._cur_banker;
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index; // 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x21;
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int action = GameConstants.WIK_NULL;
		// 胡牌检测
		int card_type = Constants_MJ_XIANG_YUAN.GAME_RULE_ZIMOHU;
		action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index, false); // 自摸

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			// 如果有四杠 最后杠牌这自摸没有胡 直接流局
			if (table.gang_count == 4) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				if (table.has_rule(Constants_MJ_XIANG_YUAN.GAME_RULE_LUN_ZHUANG)) {
					table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1)
							% table.getTablePlayerNumber();
				} else {
					table._cur_banker = table._cur_banker; // 上一家的庄家继续坐庄
				}
				// 流局
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

				return;
			}
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		// 如果这一轮有人杠 就有切换状态的机会了 并且离庄离扣的时候 胡的牌型是平胡才可以激活带杠把外
		if (table._playerStatus[_seat_index].is_lzlk() && table.gang_change_state[_seat_index]
				&& table.only_has_ping_pu[_seat_index] && !table.has_rule(Constants_MJ_XIANG_YUAN.GAME_RULE_DA_HU)) {

			int count = 0;
			int ting_count = 0;
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI_FENG;

			for (int i = 0; i < card_type_count; i++) {
				count = table.GRR._cards_index[_seat_index][i];

				if (count > 0) {
					table.GRR._cards_index[_seat_index][i]--;

					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
							table._playerStatus[_seat_index]._hu_out_cards[ting_count],
							table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic
								.switch_to_card_data(i);

						ting_count++;

						if (send_card_index == i) {
							ting_send_card = true;
						}
					}

					table.GRR._cards_index[_seat_index][i]++;
				}
			}

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

			if (ting_count > 0) {
				table.GRR._cards_index[_seat_index][send_card_index]--;

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							// 如果既是听牌 又是癞子的牌
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
					if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
						if (table._logic.is_magic_card(cards[i]))
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);

				table._playerStatus[_seat_index].add_action(Constants_MJ_XIANG_YUAN.WIK_DGBW_ACTION);

			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End

		} else if (table._playerStatus[_seat_index].is_lzlk()) {

		} else {
			int count = 0;
			int ting_count = 0;
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI_FENG;

			for (int i = 0; i < card_type_count; i++) {
				count = table.GRR._cards_index[_seat_index][i];

				if (count > 0) {
					table.GRR._cards_index[_seat_index][i]--;

					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
							table._playerStatus[_seat_index]._hu_out_cards[ting_count],
							table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic
								.switch_to_card_data(i);

						ting_count++;

						if (send_card_index == i) {
							ting_send_card = true;
						}
					}

					table.GRR._cards_index[_seat_index][i]++;
				}
			}

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

			if (ting_count > 0) {
				table.GRR._cards_index[_seat_index][send_card_index]--;

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							// 如果既是听牌 又是癞子的牌
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
					if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
						if (table._logic.is_magic_card(cards[i]))
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
				// 勾选离庄离扣
				if (table.has_rule(Constants_MJ_XIANG_YUAN.GAME_RULE_LI_ZHUANG_LI_KOU)) {
					// 闲家可以离庄离扣，庄家不可以
					if (_seat_index != table._cur_banker) {
						table._playerStatus[_seat_index].add_action(Constants_MJ_XIANG_YUAN.WIK_LZLK_ACTION);
					}
				}

			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End

		}
		table.gang_change_state[_seat_index] = false;// 每轮摸牌的时候将这个状态切换回来
		// 发送数据
		// 只有自己才有数值
		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		// 设置变量
		table._provide_card = _send_card_data; // 提供的牌

		if (table.GRR._left_card_count >= 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = 0;
			cbActionMask = table.analyse_gang_exclude_magic_card(table.GRR._cards_index[_seat_index], _send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,
					table._playerStatus[_seat_index].get_cards_abandoned_gang(), _seat_index);

			if (cbActionMask != GameConstants.WIK_NULL) { // 有杠
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上杠
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		// 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
		if (curPlayerStatus.has_action()) { // 有动作
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table._playerStatus[_seat_index].is_lzlk()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), table.delay,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}
		return;
	}

	@Override
	public boolean handler_operate_card(MJTable_ShanXi_XiangYuan table, int seat_index, int operate_code,
			int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 当弹出带杠把外和过的按钮的的情况下 如果是离庄离扣模式 点了过 这个时候就要继续维持离庄离扣的状态了 需要自动打出牌
		if (table._playerStatus[_seat_index].is_lzlk() && operate_code == GameConstants.WIK_NULL) {
			table._playerStatus[_seat_index].set_card_status(GameConstants.CARD_STATUS_LI_ZHUANG_LI_KOU);
			GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), table.delay,
					TimeUnit.MILLISECONDS);
			return true;
		}

		// 若点击“带杠把外”按钮，此时摸到的牌 需要自己手动打出去 并且解除了离庄离扣的模式 恢复到正常模式了
		if (table._playerStatus[_seat_index].is_lzlk() && operate_code == Constants_MJ_XIANG_YUAN.WIK_DGBW_ACTION) {
			table._playerStatus[_seat_index].set_card_status(GameConstants.CARD_STATUS_NORMAL);
			// operate_code = GameConstants.WIK_NULL;
		}

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

		if (operate_code == Constants_MJ_XIANG_YUAN.WIK_DGBW_ACTION) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { operate_code }, 1);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

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

			// 首轮随机坐庄，以后谁胡牌谁坐庄，流局后上局庄家继续坐庄 如果勾选轮庄规则 则首轮随机坐庄，下局由上局庄家的下家坐庄
			table.last_cur_banker = table._cur_banker;
			if (table.has_rule(Constants_MJ_XIANG_YUAN.GAME_RULE_LUN_ZHUANG)) {
				table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1)
						% table.getTablePlayerNumber();
			} else {
				table._cur_banker = _seat_index;
			}

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);

			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true); // 自摸胡算分
			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			table.is_zm_or_dp[_seat_index] = 1;
			// 结束
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		case Constants_MJ_XIANG_YUAN.WIK_LZLK_ACTION: // 离庄离扣
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

			// 离庄离扣
			table.exe_out_card_lzlk(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}
		case Constants_MJ_XIANG_YUAN.WIK_DGBW_ACTION: // 带杠把外
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

			// 离庄离扣
			table.exe_out_card_lzlk(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShanXi_XiangYuan table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {

			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (hand_cards[j] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
					if (table._logic.is_magic_card(hand_cards[j]))
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(
					table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int tmp_card = table._playerStatus[seat_index]._hu_out_cards[i][j];
				if (table._logic.is_magic_card(tmp_card)) {
					tmp_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				int_array.addItem(tmp_card);
			}

			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		int show_send_card = _send_card_data;
		if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
