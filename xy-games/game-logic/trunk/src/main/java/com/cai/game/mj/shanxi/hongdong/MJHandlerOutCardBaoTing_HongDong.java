package com.cai.game.mj.shanxi.hongdong;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_SXHD;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 报听
 * 
 * @author Administrator
 *
 */
public class MJHandlerOutCardBaoTing_HongDong extends AbstractMJHandler<MJTable_HongDong> {
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _type;

	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	public void exe(MJTable_HongDong table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		table.operate_player_action(_out_card_player, true);

		// 设置为报听状态
		table._playerStatus[_out_card_player].set_card_status(_type);

		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		if (_type == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
			// 效果
			table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { Constants_MJ_SXHD.WIK_YING_KOU },
					1, GameConstants.INVALID_SEAT);
		} else {
			// 效果
			table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BAO_TING }, 1,
					GameConstants.INVALID_SEAT);
		}

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// 出牌
		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		// 听的牌，保存，下次不用计算
		// int ting_count =
		// table._playerStatus[_out_card_player]._hu_out_card_count;
		// for (int i = 0; i < ting_count; i++) {
		// int out_card =
		// table._playerStatus[_out_card_player]._hu_out_card_ting[i];
		// if (out_card == _out_card_data) {
		// int tc = table._playerStatus[_out_card_player]._hu_card_count =
		// table._playerStatus[_out_card_player]._hu_out_card_ting_count[i];
		// for (int j = 0; j < tc; j++) {
		// table._playerStatus[_out_card_player]._hu_cards[j] =
		// table._playerStatus[_out_card_player]._hu_out_cards[i][j];
		// }
		// }
		// }

		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		boolean check_kou_ting = false;
		if (_type == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
			if (table._playerStatus[_out_card_player]._hu_card_count == 1
					&& table.get_real_card(table._playerStatus[_out_card_player]._hu_cards[0]) == -1) {
				for (int i = 0; i < 3; i++) {
					int card = GameConstants.HZ_MAGIC_CARD + i;
					if (table.check_card_ying_kou(table.GRR._cards_index[_out_card_player], card, _out_card_player)) {
						table._ying_kou[_out_card_player].setLeft(true);
						table._ying_kou[_out_card_player].getRight().add(card);
						check_kou_ting = true;
					}
				}
			} else {
				for (int i = 0; i < table._playerStatus[_out_card_player]._hu_card_count; i++) {
					int card = table.get_real_card(table._playerStatus[_out_card_player]._hu_cards[i]);
					if (card >= GameConstants.HZ_MAGIC_CARD
							&& table.check_card_ying_kou(table.GRR._cards_index[_out_card_player], card, _out_card_player)) {
						table._ying_kou[_out_card_player].setLeft(true);
						table._ying_kou[_out_card_player].getRight().add(card);
						check_kou_ting = true;
					}
				}
			}
		} else if (table._playerStatus[_out_card_player]._hu_card_count == 1) {
			if (table.get_real_card(table._playerStatus[_out_card_player]._hu_cards[0]) == -1) {
				for (int i = 0; i < 3; i++) {
					int card = GameConstants.HZ_MAGIC_CARD + i;
					if (table.check_card_ying_kou(table.GRR._cards_index[_out_card_player], card, _out_card_player)) {
						table._ying_kou[_out_card_player].setLeft(true);
						table._ying_kou[_out_card_player].getRight().add(card);
						check_kou_ting = true;
					}
				}
			} else {
				int card = table.get_real_card(table._playerStatus[_out_card_player]._hu_cards[0]);
				if (card >= GameConstants.HZ_MAGIC_CARD
						&& table.check_card_ying_kou(table.GRR._cards_index[_out_card_player], card, _out_card_player)) {
					table._ying_kou[_out_card_player].setLeft(true);
					table._ying_kou[_out_card_player].getRight().add(card);
					check_kou_ting = true;
				}
			}
		}

		if (check_kou_ting) {
			table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
					table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
					_out_card_player);
			ting_cards = table._playerStatus[_out_card_player]._hu_cards;
			ting_count = table._playerStatus[_out_card_player]._hu_card_count;
		}

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}
		// 引用权位
		ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];

		chr.bao_ting_index = table.GRR._discard_count[_out_card_player];
		chr.bao_ting_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);

		// 玩家出牌 响应判断,是否有吃碰杠补胡
		boolean bAroseAction = false;
		// if (_type == GameConstants.WIK_BAO_TING) {
		bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data);
		// }

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

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_HongDong table, int seat_index, int operate_code, int operate_card) {
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

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {

			table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
			// table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			// 效果
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		}

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
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
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
				}

				// 优先级别
				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
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
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_GANG: {
			table.has_gang_count++;
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}

		case GameConstants.WIK_NULL: {
			int _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);
			if (target_player == table._cur_banker) {
				table.duo_xiang_has_zhuang = true;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}

			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}
				jie_pao_count++;
			}
			int one = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber() + 1;// 点炮者逆时针的下个玩家
			int two = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber() + 2;// 点炮者逆时针的下下个玩家
			int three = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber() + 3;// 点炮者逆时针的下下下个玩家

			if (one == target_player) {
				table.one = true;
			} else if (two == target_player) {
				table.two = true;
			} else if (three == target_player) {
				table.three = true;
			}

			// 如果上一局出现一炮多响，如果庄家也是胡牌者，则庄家为下一局的庄家，如果庄家未胡牌，则点炮者逆时针第一个胡牌的为下一局的庄家；
			if (jie_pao_count > 0) {
				if (jie_pao_count > 1) {
					if (table.duo_xiang_has_zhuang) {
						table._cur_banker = table._cur_banker;
					} else {
						if (table.one) {
							table._cur_banker = one;
						}
						if (table.two && !table.one) {
							table._cur_banker = two;
						}
					}
				} else {
					// 第一局之后，如果庄家胡牌，则下一局连庄，如果闲家胡牌，则庄家的下家下一局坐庄
					if (target_player == table._cur_banker) {
						table._cur_banker = target_player;
					} else {
						table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					}
				}

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

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
		}
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_HongDong table, int seat_index) {
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
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < 55; j++) {
				int iCardIndex = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {

					iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

				} else {
					// int_array.addItem(table.GRR._discard_cards[i][j]);
				}
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					if (iCardIndex > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
						iCardIndex -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
				}
				int_array.addItem(iCardIndex);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && !table.player_magic_card_show_non[i]) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 2);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
		int h = hand_card_count;
		if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
			if (table.player_magic_card_show_non[seat_index]) {
				liang_magic_count = 2;
			}
			for (int j = 0; j < h; j++) {
				if (cards[j] != table.player_magic_card[seat_index]) {
					continue;
				}

				if (liang_magic_count == 2) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					liang_magic_count++;
					hand_card_count--;
				}
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber() && table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB); p++) {
			if (!table.player_magic_card_show_non[seat_index]) {
				tableResponse.addHuXi(table.player_magic_card[p]);
			}
		}
		for (int i = 0; i < h; i++) {
			if (cards[i] > GameConstants.CARD_ESPECIAL_TYPE_GUI && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
			tableResponse.addCardsData(cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _out_card_data;
		if (table._logic.is_magic_card(_out_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		// 出牌
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}

}
