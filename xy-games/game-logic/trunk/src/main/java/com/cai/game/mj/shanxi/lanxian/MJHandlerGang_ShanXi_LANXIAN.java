package com.cai.game.mj.shanxi.lanxian;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_SXLX;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_ShanXi_LANXIAN extends MJHandlerGang<MJTable_ShanXi_LANXIAN> {

	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;
	protected int _type;

	public MJHandlerGang_ShanXi_LANXIAN() {
	}

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self,
			boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
	}

	@Override
	public void exe(MJTable_ShanXi_LANXIAN table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		// 效果
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_AN_GANG }, 1, GameConstants.INVALID_SEAT);
		} else {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_MING_GANG }, 1, GameConstants.INVALID_SEAT);
		}

		// 自己暗杆和别人打的牌自己再杠 这两种情况是不会发生胡牌的
		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

		// 检查对这个杠有没有胡
		boolean bAroseAction = false;
		int no_feng_operate_card = _center_card;
		// 当自己摸到的一张牌，牌桌上自己又碰了这张牌 这个时候自己准备去杠 就有可能发生悲剧 别人有可能要抢杠了
		bAroseAction = table.estimate_gang_respond(_seat_index, no_feng_operate_card);

		if (bAroseAction == false) {
			this.exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean exe_gang(MJTable_ShanXi_LANXIAN table) {
		int no_feng_operate_card = _center_card;
		int cbCardIndex = table._logic.switch_to_card_index(no_feng_operate_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠，结算的时候要翻倍 就是基数*2 ，三人给钱
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table.all_player_gang_count++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌，谁点谁给
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table.all_player_gang_count++;
			// 删掉出来的那张牌
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠，三个人开钱
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == no_feng_operate_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i; // 第几个组合可以碰
					break;
				}
			}
			table.all_player_gang_count++;
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		}

		// cbWeaveIndex这个地方表示没看懂
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;

		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = no_feng_operate_card;

		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type;

		// 加一个杠牌的时候 是不是报听的状态值 is_lao_gang 暂时用这个状态来代替提供杠牌玩家是否是报听的状态
		if (table._playerStatus[_provide_player].is_bao_ting()) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].is_lao_gang = true;
		} else {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].is_lao_gang = false;
		}

		if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		}

		// 设置用户
		table._current_player = _seat_index;
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;

		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].type = table.GRR._weave_items[_seat_index][i].type;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player
					+ GameConstants.WEAVE_SHOW_DIRECT;
		}

		// 检查听牌，杠之后要检查有没有换章
		if (table._playerStatus[_seat_index].is_bao_ting()) {
			table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(
					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}
		}
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		if (table.has_rule(GameConstants_SXLX.GAME_RULE_SHANXI_LAO_PAI)) {
			if (table.GRR._left_card_count >= 13) {
				if (table.GRR._left_card_count == 13 && table.all_player_gang_count == 1) {
					table.is_thirteen_and_only_one_gang = true;
				}
				table.exe_dispatch_card(_seat_index, _type, 0);
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			}

		} else {
			if (table.GRR._left_card_count >= 0) {
				table.exe_dispatch_card(_seat_index, _type, 0);
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			}
		}

		return true;
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
	public boolean handler_operate_card(MJTable_ShanXi_LANXIAN table, int seat_index, int operate_code,
			int operate_card) {
		// 抢杠胡

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU)
				&& operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(operate_card);
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			if (table._playerStatus[_provide_player].is_bao_ting()) {
				table.BaoTingDianPaoVaild(_provide_player);// 设置打牌的玩家报听时候点炮的状态
			} else {
				table.NoBaoTingDianPaoVaild(_provide_player);// 设置打牌的玩家未报听时候点炮的状态
			}
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		table.operate_player_action(seat_index, true);

		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		// 执行判断
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// int cbTargetActionRank =
				// table._logic.get_action_rank(target_action) +
				// target_p;//table._playerStatus[target_player].get_perform()

				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		// 选择了不胡
		if (target_action == GameConstants.WIK_NULL) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
			// 发牌给杠的玩家
			this.exe_gang(table);
			return true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player) {
				table.GRR._chi_hu_rights[i].set_valid(true);
			} else {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
		}
		table.process_chi_hu_player_operate(target_player, _center_card, false);

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			// table._playerStatus[i].clean_status();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {// (i ==
																	// _provide_player)
																	// ||
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			// 玩家出牌之后，有人胡牌，记录牌桌、出牌人以及胡牌人的状态信息
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			if (table._cur_banker != target_player) {
				table._cur_banker = (table._cur_banker + (table.getTablePlayerNumber() + 1))
						% table.getTablePlayerNumber(); // 若闲家胡牌则庄家的下家坐庄
			} else {
				table._cur_banker = target_player;// 庄家胡牌则继续坐庄
			}

			table.GRR._chi_hu_card[target_player][0] = _center_card;
			table.process_chi_hu_player_score(target_player, _seat_index, operate_card, false, true);

			// 记录
			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShanXi_LANXIAN table, int seat_index) {
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

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
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

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

}
