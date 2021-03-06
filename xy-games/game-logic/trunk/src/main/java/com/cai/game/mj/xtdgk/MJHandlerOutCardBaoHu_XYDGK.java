package com.cai.game.mj.xtdgk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 报胡
 * 
 * @author Administrator
 *
 */
public class MJHandlerOutCardBaoHu_XYDGK extends AbstractMJHandler<MJTable_XTDGK> {
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _type;

	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	@Override
	public void exe(MJTable_XTDGK table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		// 解除过手胡
		table.clear_jie_pao_hu_fan(_out_card_player);
		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		table.add_out_card_num(_out_card_player);

		// 用户切换
		int next_player = table.getNextPalyerIndex(_out_card_player);
		table._current_player = next_player;
		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0,
				table.GRR.getWeaveItemsForOut(_out_card_player, new WeaveItem[GameConstants.MAX_WEAVE]));

		// 出牌
		if (table.is_first_dispatchcard_num(_out_card_player) && table._player_result.is_bao_hu(_out_card_player)) {
			table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_CARD);
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
		// 引用权位
		ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];

		chr.bao_ting_index = table.GRR._discard_count[_out_card_player];
		chr.bao_ting_card = _out_card_data;
		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
				GameConstants.DELAY_SEND_CARD_DELAY);

		boolean bAroseAction = table.estimate_player_out_card_respond_bao_hu(_out_card_player, _out_card_data);
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
			int maxPlayer = table.getMaxActionPlayerIndex();
			// 告知客户端最高优先级操作的人--有优先级问题，客户端暂时只处理碰
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					if (playerStatus.has_chi_hu()) {
						table.operate_player_action(i, false);
					} else {
						// 协助解决客户端卡顿问题--客户端只处理碰 所以问题不大
						boolean isNotWait = maxPlayer == i ? true : false;
						table.operate_player_action(i, false, isNotWait);
					}

				}
			}
		}
	}

	/***
	 * 用户操作--当前玩家出牌之后 别人的操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_XTDGK table, int seat_index, int operate_code, int operate_card) {
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

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}
		// 玩家有杠不杠先碰后加入过杠
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_GANG)
				&& operate_code == GameConstants.WIK_PENG) {
			table.addPassedGang(seat_index, operate_card);
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
			// 记录过胡的时候，牌型的番数，变大了，本圈才能接炮
			table.fan_shu_when_abandoned_jie_pao[seat_index] = table.fan_shu_when_jie_pao_hu[seat_index];
			if(table._player_result.is_bao_hu(seat_index)){
				table.need_clear[seat_index ] = false;
			} 
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
			boolean needHu = true;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _out_card_player) {
					continue;
				}
				if (table._playerStatus[i]._perfrom_action != GameConstants.WIK_NULL) {
					needHu = false;
				}
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}

			if (needHu) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table._playerStatus[i].clean_action();
					table.change_player_status(i, GameConstants.INVALID_VALUE);
				}

				table.operate_player_action(_out_card_player, true);

				table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
						GameConstants.DELAY_SEND_CARD_DELAY);
				ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];
				chr.set_empty();
				int action = table.analyse_chi_hu_card(table.GRR._cards_index[_out_card_player],
						table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
						_out_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, _out_card_player,
						GameConstants.INVALID_SEAT);// 自摸
				if (table._player_result.is_qing_hu(_out_card_player)) {
					table._playerStatus[_out_card_player].add_action(GameConstants.WIK_ZI_MO);
					table._playerStatus[_out_card_player].add_zi_mo(_out_card_data, _out_card_player);// 吃胡的组合
					GameSchedule.put(new Runnable() {
						@Override
						public void run() {
							handler_operate_card(table, _out_card_player, GameConstants.WIK_ZI_MO, _out_card_data);
						}
					}, 3, TimeUnit.SECONDS);
				} else {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						table._playerStatus[i].clean_action();
						table.change_player_status(i, GameConstants.INVALID_VALUE);
					}

					table.operate_player_action(_out_card_player, true);
					int next_player = table.getNextPalyerIndex(_out_card_player);
					// 发牌
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
				}
			}

		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.chang_zhang(seat_index, false);
			table.ISHUVaild(seat_index, operate_card);
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		}

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
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
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
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
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
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
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
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
		case GameConstants.WIK_CHI_HU: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.operate_remove_discard(_out_card_player, table.GRR._discard_count[_out_card_player]);
			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false) || table.getISHU(i)) {
					continue;
				}
				jie_pao_count++;
			}

			if (jie_pao_count > 0) {
				int zhuang = GameConstants.INVALID_SEAT;
				if (jie_pao_count == table.getTablePlayerNumber() - 1) {
					zhuang = _out_card_player;
					table.chang_zhang(zhuang, true);
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)
							|| table.getISHU(i)) {
						continue;
					}
					table.ISHUVaild(i);
					table.operate_player_get_card(i, 1, new int[] { table.getHuCard(i) }, i);
					// table.process_chi_hu_player_operate(i, target_card,
					// false);
					table.process_chi_hu_player_score(i, _out_card_player, target_card, false);

					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_out_card_player]++;

				}

				// 结束
				int index = _out_card_player;
				if (jie_pao_count > 1 && table.getMoreHuIndex(_out_card_player)) {
					index = _out_card_player;
				} else if (table.getHuIndex() != -1) {
					index = table.getHuIndex();
				}
				int next_player = table.getNextPalyerIndex(index);
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}

			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.operate_remove_discard(_out_card_player, table.GRR._discard_count[_out_card_player]);
			table.GRR._chi_hu_rights[_out_card_player].set_valid(true);

			table.chang_zhang(_out_card_player, false);
			table.ISHUVaild(_out_card_player, operate_card);
			table.ISHUVaild(_out_card_player);
			table.GRR._chi_hu_card[_out_card_player][0] = operate_card;
			table.process_chi_hu_player_score(_out_card_player, _out_card_player, operate_card, true);
			table.process_chi_hu_player_operate(_out_card_player, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_out_card_player]++;

			// 结束
			int next_player = table.getNextPalyerIndex(_out_card_player);
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_XTDGK table, int seat_index) {
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
			if (i == seat_index) {
				int_array.addItem(_out_card_data);
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

			tableResponse.addWinnerOrder(0);

			// 牌
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

		/*
		 * if(table.is_first_dispatchcard_num(seat_index) &&
		 * table._player_result.is_bao_hu(seat_index)){
		 * table.operate_out_card(_out_card_player, 1, new int[] {
		 * _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
		 * GameConstants.INVALID_CARD); }else{
		 * table.operate_out_card(_out_card_player, 1, new int[] {
		 * _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
		 * GameConstants.INVALID_SEAT); }
		 */

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		table.roomResponseShowHuCard(seat_index);

		return true;
	}

}
