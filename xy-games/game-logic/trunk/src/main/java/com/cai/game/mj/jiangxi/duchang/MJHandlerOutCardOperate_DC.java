package com.cai.game.mj.jiangxi.duchang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_TDH;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate_DC extends MJHandlerOutCardOperate<MJTable_DC> {

	// 执行出牌动作
	@Override
	public void exe(MJTable_DC table) {
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		// 重置玩家状态
		table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();

		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			}
			if (table._logic.is_lai_gen_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			}
		}
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// 显示出牌
		if (table._logic.is_magic_card(_out_card_data)) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_GUI },
					GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		} else if (table._logic.is_lai_gen_card(_out_card_data)) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN },
					GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		} else {
			table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}

		// 检查听牌
		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(
				table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player], false,
				_out_card_player);

		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		// 打出花杠牌
		if ((_out_card_data == table.GRR._especial_show_cards[0])
				|| (_out_card_data == table.get_real_card(table.GRR._especial_show_cards[0]))) {
			exe_hua_gang(table);
			next_player = _out_card_player;
		}
		// 裁宝
		if ((_out_card_data == table.GRR._especial_show_cards[1])
				|| (_out_card_data == table.get_real_card(table.GRR._especial_show_cards[1]))) {
			exe_cai_bao(table);
		}

		boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data);// ,

		// 如果没有需要操作的玩家，派发扑克
		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}
			table.operate_player_action(_out_card_player, true);

			// 加入牌队
			if (table._logic.is_magic_card(_out_card_data)) {
				table.exe_add_discard(_out_card_player, 1,
						new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_GUI }, false,
						GameConstants.DELAY_SEND_CARD_DELAY);
			} else if (table._logic.is_lai_gen_card(_out_card_data)) {
				table.exe_add_discard(_out_card_player, 1,
						new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN }, false,
						GameConstants.DELAY_SEND_CARD_DELAY);
			} else {
				table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
						GameConstants.DELAY_SEND_CARD_DELAY);
			}

			// 发牌
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		} else {
			// 等待别人操作这张牌
			// int maxPlayer = table.getMaxActionPlayerIndex();
			// 告知客户端最高优先级操作的人--有优先级问题，客户端暂时只处理碰
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					// if (playerStatus.has_chi_hu()) {
					// table.operate_player_action(i, false);
					// } else {
					// boolean isNotWait = maxPlayer == i ? true : false;// 协助解决客户端卡顿问题--客户端只处理碰
					// table.operate_player_action(i, false, isNotWait);
					// }
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);

				}
			}
		}
	}

	// 花杠
	protected boolean exe_hua_gang(MJTable_DC table) {
		// 小局花杠
		table.addHuaGangNum(_out_card_player);
		// 设置用户
		table._current_player = _out_card_player;
		table._player_result.zhi_gang_count[_out_card_player]++;
		return true;
	}

	// 裁宝
	protected boolean exe_cai_bao(MJTable_DC table) {
		// 小局栽宝
		table.addCaiBaoNum(_out_card_player);
		// 设置用户
		table._current_player = _out_card_player;
		table._player_result.piao_lai_count[_out_card_player]++;
		return true;
	}

	/***
	 * //用户操作--当前玩家出牌之后 别人的操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_DC table, int seat_index, int operate_code, int operate_card) {
		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}

		// 漏碰规则
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_PENG)
				&& operate_code != GameConstants.WIK_PENG) {
			table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
		}

		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		// 修改网络导致吃碰错误 9.26 WalkerGeek
		int target_card = _out_card_data;

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_GANG: {
			table.exe_gang(target_player, _out_card_player, target_card, target_action,
					GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants.WIK_NULL: {
			table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, false, 0);

			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();

			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}

			/*
			 * table._cur_banker = target_player;
			 * 
			 * 
			 * 
			 * table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true, 0);
			 */

			int index = target_player;
			if (table.has_rule(GameConstants_ND.GAME_RULE_MA_FANG_WEI)) {
				index = table._cur_banker;
			}
			table._cur_banker = target_player;
			table.GRR._chi_hu_card[target_player][0] = target_card;
			// 下局胡牌的是庄家
			// table.set_niao_card(index, GameConstants.INVALID_VALUE, true, 0);// 结束后设置鸟牌\

			/*
			 * for (int i = 0; i < table.getTablePlayerNumber(); i++) { for (int j = 0; j <
			 * table.GRR._player_niao_count[i]; j++) { table.GRR._player_niao_cards[i][j] =
			 * table .set_ding_niao_valid(table.GRR._player_niao_cards[i][j], true);//
			 * 胡牌的鸟生效 if (!GameDescUtil.has_rule(table.gameRuleIndexEx,
			 * GameConstants.GAME_RULE_ZHUANG_NIAO)) { if (i == target_player) {
			 * table.GRR._count_pick_niao++; } } } }
			 */

			table.process_chi_hu_player_operate(target_player, target_card, false);
			table.process_chi_hu_player_score(target_player, _out_card_player, _out_card_data, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;

	}

	@Override
	public boolean handler_player_be_in_room(MJTable_DC table, int seat_index) {
		// 将花杠牌宝牌显示在牌桌的正中央
		table.showSpecialCard(seat_index);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
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
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 宝牌
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
				} else if (table._logic.is_lai_gen_card(table.GRR._discard_cards[i][j])) {
					// 花杠
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
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
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			} else if (table._logic.is_lai_gen_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		// 出牌
		if (table._logic.is_magic_card(_out_card_player)) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_GUI },
					GameConstants.OUT_CARD_TYPE_MID, seat_index);
		} else if (table._logic.is_lai_gen_card(_out_card_player)) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN },
					GameConstants.OUT_CARD_TYPE_MID, seat_index);
		} else {
			table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					seat_index);
		}

		// table.operate_player_get_card(_seat_index, 1, new
		// int[]{_send_card_data});
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
