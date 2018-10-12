package com.cai.game.mj.yu.gd_tdh.td;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_TDH;
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

public class MJHandlerOutCardBaoTing_TDH_3D extends AbstractMJHandler<Table_TDH_3D> {
	public int _out_card_player = GameConstants_TDH.INVALID_SEAT;
	public int _out_card_data = GameConstants_TDH.INVALID_VALUE;
	public int _type;

	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	@Override
	public void exe(Table_TDH_3D table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_TDH.INVALID_VALUE);
		}

		table.operate_player_action(_out_card_player, true);

		// 设置为报听状态
		table._playerStatus[_out_card_player].set_card_status(GameConstants_TDH.CARD_STATUS_BAO_TING);

		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		// 效果
		table.operate_effect_action(_out_card_player, GameConstants_TDH.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_TDH.WIK_BAO_TING },
				1, GameConstants_TDH.INVALID_SEAT);

		// 刷新手牌
		int cards[] = new int[GameConstants_TDH.MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);

		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// 出牌
		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants_TDH.OUT_CARD_TYPE_MID,
				GameConstants_TDH.INVALID_SEAT);

		// 听的牌，保存，下次不用计算
		int ting_count = table._playerStatus[_out_card_player]._hu_out_card_count;
		for (int i = 0; i < ting_count; i++) {
			int out_card = table._playerStatus[_out_card_player]._hu_out_card_ting[i];
			if (out_card == _out_card_data) {
				int tc = table._playerStatus[_out_card_player]._hu_card_count = table._playerStatus[_out_card_player]._hu_out_card_ting_count[i];
				for (int j = 0; j < tc; j++) {
					table._playerStatus[_out_card_player]._hu_cards[j] = table._playerStatus[_out_card_player]._hu_out_cards[i][j];
				}
			}
		}

		// 引用权位
		ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];

		chr.bao_ting_index = table.GRR._discard_count[_out_card_player];
		chr.bao_ting_card = _out_card_data;

		boolean bAroseAction = false;
		if (!table._logic.is_magic_card(_out_card_data)) {
			bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, 0);
		}

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants_TDH.INVALID_VALUE);
			}

			table.operate_player_action(_out_card_player, true);
			table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants_TDH.DELAY_SEND_CARD_DELAY);
			table.exe_dispatch_card(next_player, GameConstants_TDH.WIK_NULL, GameConstants_TDH.DELAY_SEND_CARD_DELAY);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				PlayerStatus playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					table.change_player_status(i, GameConstants_TDH.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
				// handler_check_auto_behaviour(table, i, _out_card_data);
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_TDH_3D table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants_TDH.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants_TDH.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._win_order[seat_index] = 1; // 用来计算和处理吃三比的消散

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		} else if (operate_code == GameConstants_TDH.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants_TDH.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_TDH.WIK_NULL }, 1);

			// if (table._playerStatus[seat_index].has_chi_hu()) {
			// table._playerStatus[seat_index].chi_hu_round_invalid();
			// }

			if (table._playerStatus[seat_index].has_action_by_code(GameConstants_TDH.WIK_CHI_HU)) {
				table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}

			if (table._playerStatus[seat_index].has_action_by_code(GameConstants_TDH.WIK_PENG)) {
				table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		int target_player = seat_index;
		int target_action = operate_code;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
				}

				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants_TDH.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table._card_can_not_out_after_chi[target_player] = target_card;
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_TDH.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_TDH.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			// table.remove_discard_after_operate(_out_card_player,
			// _out_card_data);
			table._card_can_not_out_after_chi[target_player] = target_card;
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_TDH.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_TDH.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			// table.remove_discard_after_operate(_out_card_player,
			// _out_card_data);
			table._card_can_not_out_after_chi[target_player] = target_card;
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_TDH.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_TDH.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			// table.remove_discard_after_operate(_out_card_player,
			// _out_card_data);
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_TDH.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_TDH.WIK_GANG: {
			// 杠的吃三比放到杠牌handler里
			table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants_TDH.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants_TDH.WIK_NULL: {
			table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, false, 0);

			int next_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(next_player, GameConstants_TDH.WIK_NULL, 0);

			return true;
		}
		case GameConstants_TDH.WIK_CHI_HU: {
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}

			int jie_pao_count = 0;
			for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}
				jie_pao_count++;
			}

			if (jie_pao_count > 0) {
				if (jie_pao_count > 1) {
					table._cur_banker = _out_card_player;
				} else {
					table._cur_banker = target_player;
				}
				switch (jie_pao_count) {
				case 1:
					table.hu_dec_type[_out_card_player] = 6;
					break;
				case 2:
					table.hu_dec_type[_out_card_player] = 8;
					break;
				case 3:
					table.hu_dec_type[_out_card_player] = 7;
					break;
				}

				for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
					if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}

					table.GRR._chi_hu_card[i][0] = target_card;
					table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false, Table_TDH_3D.HU_TYPE_JIE_PAO);

					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_out_card_player]++;
					if (table.GRR._chi_hu_rights[i].da_hu_count > 0) {
						table._player_result.da_hu_jie_pao[i]++;
						table._player_result.da_hu_dian_pao[_out_card_player]++;
					} else {
						table._player_result.xiao_hu_jie_pao[i]++;
						table._player_result.xiao_hu_dian_pao[_out_card_player]++;
					}
				}

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants_TDH.Game_End_NORMAL),
						GameConstants_TDH.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
		}
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TDH_3D table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 杠之后，发的牌的张数
		roomResponse.setPageSize(table.gang_dispatch_count);
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
		// _player_action[seat_index] : MJGameConstants_TDH.WIK_NULL);

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
					// if (i != seat_index) {
					// int_array.addItem(GameConstants_TDH.BLACK_CARD);
					// } else {
					// }
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants_TDH.CARD_ESPECIAL_TYPE_BAO_TING);
				} else {
					int real_card = table.GRR._discard_cards[i][j];
					if (table.has_rule(GameConstants_TDH.GAME_RULE_HENAN_DAI_HUN)) {
						if (table._logic.is_magic_card(real_card)) {
							real_card += GameConstants_TDH.CARD_ESPECIAL_TYPE_HUN;
						}
					}
					int_array.addItem(real_card);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_TDH.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants_TDH.WEAVE_SHOW_DIRECT);
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
		int cards[] = new int[GameConstants_TDH.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		if (table.has_rule(GameConstants_TDH.GAME_RULE_HENAN_DAI_HUN)) {
			for (int i = 0; i < GameConstants_TDH.MAX_COUNT; i++) {
				if (table._logic.is_magic_card(cards[i])) {
					cards[i] += GameConstants_TDH.CARD_ESPECIAL_TYPE_HUN;
				}

				tableResponse.addCardsData(cards[i]);
			}
		} else {
			for (int i = 0; i < GameConstants_TDH.MAX_COUNT; i++) {
				tableResponse.addCardsData(cards[i]);
			}
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _out_card_data;
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants_TDH.OUT_CARD_TYPE_MID, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			if (table.has_rule(GameConstants_TDH.GAME_RULE_HENAN_DAI_HUN)) {
				for (int x = 0; x < ting_count; x++) {
					if (table._logic.is_magic_card(ting_cards[x])) {
						ting_cards[x] += GameConstants_TDH.CARD_ESPECIAL_TYPE_HUN;
					}
				}
			}
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(Table_TDH_3D table, int seat_index) {
		handler_check_auto_behaviour(table, seat_index, _out_card_data);
		return false;
	}
}
