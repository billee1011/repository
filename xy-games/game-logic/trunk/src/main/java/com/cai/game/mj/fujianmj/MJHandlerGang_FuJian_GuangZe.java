package com.cai.game.mj.fujianmj;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_MJ_GuangZe;
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

public class MJHandlerGang_FuJian_GuangZe extends MJHandlerGang<MJTable_FuJian_GuangZe> {

	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;
	protected int _type;

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
	public void exe(MJTable_FuJian_GuangZe table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		// 效果
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_GANG }, 1, GameConstants.INVALID_SEAT);
		} else {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_GANG }, 1, GameConstants.INVALID_SEAT);
		}

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

		// 检查对这个杠有没有胡
		boolean bAroseAction = false;
		// 这里会发生抢杠胡
		bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			this.exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu_guang_ze()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	protected boolean exe_gang(MJTable_FuJian_GuangZe table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// 删掉出来的那张牌
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i; // 第几个组合可以碰
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		}
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

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
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player
					+ GameConstants.WEAVE_SHOW_DIRECT;
		}

		// 检查听牌，杠之后要检查有没有换章
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

		// 算分
		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				// 暗杠每人3分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= 3 * GameConstants.CELL_SCORE;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 3 * GameConstants.CELL_SCORE;

			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 放杠3分
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 3 * GameConstants.CELL_SCORE;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] -= 3 * GameConstants.CELL_SCORE;

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;
				// 组合牌杠每人1分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= 3 * GameConstants.CELL_SCORE;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 3 * GameConstants.CELL_SCORE;

			}

			table._player_result.ming_gang_count[_seat_index]++;
		}
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		if (table.GRR._left_card_count > table.niao_num) {
			table.exe_dispatch_card(_seat_index, Constants_MJ_GuangZe.HU_GANG_SHANG_KAI_HUA, 0);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
		}

		return true;
	}

	@Override
	public boolean handler_operate_card(MJTable_FuJian_GuangZe table, int seat_index, int operate_code,
			int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if (operate_code == Constants_MJ_GuangZe.WIK_QIANG_GANG) {
			operate_code = GameConstants.WIK_CHI_HU;
		}

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU)
				&& operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(operate_card);
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_MJ_GuangZe.CHR_FANG_PAO);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_chi_hu_guang_ze()) {
				table._playerStatus[seat_index].add_cards_abandoned_hu(operate_card);
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu_guang_ze()))
				return false;
		}

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu_guang_ze()))
				return false;
		}

		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;

		// 执行判断
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action);
				}
				// 优先级别
				int cbTargetActionRank = table.get_action_rank(table._playerStatus[target_player].get_perform());
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					// if (target_action == Constants_MJ_GuangZe.WIK_DI_HU
					// || target_action == Constants_MJ_GuangZe.WIK_QI_DA_DUI
					// || target_action == Constants_MJ_GuangZe.WIK_QI_XIAO_DUI
					// || target_action == Constants_MJ_GuangZe.WIK_QING_YI_SE
					// || target_action == Constants_MJ_GuangZe.WIK_DAN_DIAO
					// || target_action == Constants_MJ_GuangZe.WIK_HHQXD
					// || target_action == Constants_MJ_GuangZe.WIK_QIANG_GANG)
					// {
					// target_action = GameConstants.WIK_CHI_HU;
					// }
				}
			}
		}
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		// 变量定义

		int target_card = _center_card;

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		if (operate_code == GameConstants.WIK_CHI_HU) {
			table.has_qiang_gang = true;
			int ting_cards[] = table._playerStatus[target_player]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(target_player, 1, ting_cards);

			table.GRR._chi_hu_rights[target_player].set_valid(true);
			table.process_chi_hu_player_operate(target_player, operate_card, false);
			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_MJ_GuangZe.CHR_FANG_PAO);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		operate_card = _center_card;

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			if (jie_pao_count > 1) {
				table._cur_banker = _seat_index;
			} else {
				table._cur_banker = target_player;
			}

			if (jie_pao_count > 1) {
				table.set_niao_card(_seat_index, 0);
			} else {
				table.set_niao_card(target_player, 0);
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {// (i ==
																		// _provide_player)
																		// ||
					continue;
				}

				table.GRR._chi_hu_card[i][0] = _center_card;

				table.process_chi_hu_player_score(i, _seat_index, _center_card, false);

				// 记录
				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_provide_player]++;
			}

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			return true;

		} else {
			this.exe_gang(table);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_FuJian_GuangZe table, int seat_index) {
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
