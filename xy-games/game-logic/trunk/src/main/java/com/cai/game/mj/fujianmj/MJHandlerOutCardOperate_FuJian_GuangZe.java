package com.cai.game.mj.fujianmj;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_MJ_GuangZe;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate_FuJian_GuangZe extends MJHandlerOutCardOperate<MJTable_FuJian_GuangZe> {

	@Override
	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	@Override
	public void exe(MJTable_FuJian_GuangZe table) {
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

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
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// 显示出牌
		if (table._logic.is_magic_card(_out_card_data)) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI },
					GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		} else {
			table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}

		// 检查听牌
		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(
				table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player], _out_card_player);
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

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
				GameConstants.DELAY_SEND_CARD_DELAY);

		// 玩家出牌 响应判断,是否有吃碰杠补胡
		boolean bAroseAction = false;
		bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data);

		// 如果没有需要操作的玩家，派发扑克
		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}

			table.operate_player_action(_out_card_player, true);

			// 发牌
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					table.operate_player_action(i, false);
				}
			}
		}
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

		playerStatus.operate(operate_code, operate_card);

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
				1);

		// if (operate_code == Constants_MJ_GuangZe.WIK_DI_HU || operate_code ==
		// Constants_MJ_GuangZe.WIK_QI_DA_DUI
		// || operate_code == Constants_MJ_GuangZe.WIK_QI_XIAO_DUI
		// || operate_code == Constants_MJ_GuangZe.WIK_QING_YI_SE
		// || operate_code == Constants_MJ_GuangZe.WIK_DAN_DIAO || operate_code
		// == Constants_MJ_GuangZe.WIK_HHQXD
		// || operate_code == Constants_MJ_GuangZe.WIK_TIAN_HU) {
		// operate_code = GameConstants.WIK_CHI_HU;
		// }

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
			// 能碰不碰的，加到废弃的碰的数组里面去，保证碰要过圈
			if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_PENG)) {
				table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
			}
		}
		// if
		// ((table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_DI_HU)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_QI_DA_DUI)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_QI_XIAO_DUI)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_QING_YI_SE)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_DAN_DIAO)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_HHQXD)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_TIAN_HU)
		// ||
		// table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_CHI_HU))
		// && operate_code != GameConstants.WIK_CHI_HU) {
		//
		// table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
		// table._playerStatus[seat_index].chi_hu_round_invalid();
		// }
		if ((table._playerStatus[seat_index].has_action_by_code(Constants_MJ_GuangZe.WIK_CHI_HU))
				&& operate_code != GameConstants.WIK_CHI_HU) {

			table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			// 效果
			table.has_dian_pao = true;
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
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
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
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
					// || target_action == Constants_MJ_GuangZe.WIK_TIAN_HU) {
					// target_action = GameConstants.WIK_CHI_HU;
					// }

				}
			}
		}
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		int target_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_gang(target_player, _out_card_player, target_card, target_action,
					GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}

		case GameConstants.WIK_NULL: {
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();

			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu_guang_ze()))
					return false;
			}

			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}
				jie_pao_count++;
			}

			int niao_seat = target_player;
			if (jie_pao_count > 0) {
				if (jie_pao_count > 1) {
					table._cur_banker = _out_card_player;
					niao_seat = _out_card_player;
				} else {
					table._cur_banker = target_player;
				}
				table.set_niao_card(niao_seat, 0);

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}
					table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false);
					table.GRR._chi_hu_card[i][0] = target_card;
					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_out_card_player]++;
				}

				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
		}
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
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false); // 是否托管
			// 剩余牌数
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
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(hand_cards[j])) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
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

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
