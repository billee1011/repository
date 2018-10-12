package com.cai.game.mj.hunan.zhuzhouwang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerOutCardOperate_ZhuZhouWang extends MJHandlerOutCardOperate<Table_ZhuZhouWang> {
	// 执行出牌动作
	@Override
	public void exe(Table_ZhuZhouWang table) {
		// 状态切换，操作清理
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();

		// 保存当前出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		table._playerStatus[_out_card_player].chi_hu_round_valid();// 可以胡了

		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (cards[j] == table.joker_card_1 || cards[j] == table.joker_card_2) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (cards[j] == table.ding_wang_card) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// 显示出牌
		if (_out_card_data == table.joker_card_1 || _out_card_data == table.joker_card_2) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA },
					GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		} else if (_out_card_data == table.ding_wang_card) {
			table.operate_out_card(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI },
					GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		} else {
			table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}

		// 听牌数据
		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(
				table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player], _out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 0, ting_cards);
		}

		// 桌子当前用户切换
		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		// 添加弃牌区
		if (_out_card_data == table.joker_card_1 || _out_card_data == table.joker_card_2) {
			table.exe_add_discard(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA }, false,
					GameConstants.DELAY_SEND_CARD_DELAY);
		} else if (_out_card_data == table.ding_wang_card) {
			table.exe_add_discard(_out_card_player, 1,
					new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI }, false,
					GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
					GameConstants.DELAY_SEND_CARD_DELAY);
		}

		// 出牌操作判断
		boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);
		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}
			// 关闭操作提示
			table.operate_player_action(_out_card_player, true);

			// 切换发牌handler
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					// 打开操作提示
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_ZhuZhouWang table, int seat_index, int operate_code, int operate_card) {
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

		if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
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
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action);
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

		int target_card = _out_card_data;

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
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();

			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}
				jie_pao_count++;
				table.process_chi_hu_player_operate(i, operate_card, false);
			}

			if (jie_pao_count > 0) {

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

				if (jie_pao_count > 1) {
					// 结束
					table._cur_banker = _out_card_player;
					table.set_niao_card(_out_card_player, GameConstants.INVALID_VALUE, true);
				} else if (jie_pao_count == 1) {
					table._cur_banker = target_player;
					// 结束后设置鸟牌
					table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true);
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}

					table.hu_dec_type[i] = 2;

					table.GRR._chi_hu_card[i][0] = target_card;

					table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false);

					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_out_card_player]++;
				}

				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			}

			return true;
		}
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_ZhuZhouWang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		// 骰子
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table.GRR._discard_cards[i][j] == table.joker_card_1
						|| table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count=table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}
		
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _out_card_data, false); // 效果
		} else {
			// 听牌显示
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}

			// table.operate_out_card(_out_card_player, 1, new int[] {
			// _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}
}
