package com.cai.game.mj.shanxi.hongdong;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.mj.Constants_MJ_SXHD;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandler_XuanWang extends AbstractMJHandler<MJTable_HongDong> {

	private int _seat_index;
	private int _send_card_data;
	private boolean[] has_xuan_wang;
	protected GangCardResult m_gangCardResult;
	private Map<Integer, List<Integer>> possible_wang = Maps.newHashMap();

	public void reset(int seat_index, int send_card_data, GangCardResult _gangCardResult) {
		_seat_index = seat_index;
		_send_card_data = send_card_data;
		m_gangCardResult = _gangCardResult;
	}

	@Override
	public void exe(MJTable_HongDong table) {
		table._game_status = GameConstants_KWX.GS_MJ_PIAO; // 设置状态
		has_xuan_wang = new boolean[table.getTablePlayerNumber()];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);

		table.load_common_status(roomResponse);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = 0;// 设置为报听状态
			has_xuan_wang[i] = false;
			possible_wang.put(i, Lists.newArrayList());
		}

		boolean flag = true;
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			for (int c = 0; c < table.GRR._cards_index[i].length; c++) {
				if (table.GRR._cards_index[i][c] >= 2) {
					possible_wang.get(i).add(table._logic.switch_to_card_data(c));
				}
			}
			if (possible_wang.get(i).size() == 0) {
				continue;
			}

			table._player_result.pao[i] = -2;
			has_xuan_wang[i] = true;
			flag = false;
			possible_wang.get(i).forEach(card -> {
				roomResponse.addDouliuzi(card);
			});
			roomResponse.setPaoMin(possible_wang.get(i).size());
			table.send_response_to_player(i, roomResponse);
			table.change_player_status(i, GameConstants.Player_Status_OUT_CARD);
		}
		table.operate_player_status();
		table.operate_player_data();

		if (flag) {
			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			if (curPlayerStatus.has_action()) { // 有动作
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}
	}

	public boolean handler_pao_qiang(MJTable_HongDong table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		table._player_result.pao[seat_index] = table.get_real_card(pao);
		table.player_magic_card[seat_index] = table.get_real_card(pao);
		has_xuan_wang[seat_index] = false;

		table.operate_player_data();

		if (seat_index == _seat_index) {
			table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
		}
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		if (seat_index == _seat_index) {
			table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		}
		for (int j = 0; j < hand_card_count; j++) {
			for (int k = 0; k < table._playerStatus[_seat_index]._hu_out_card_count && seat_index == _seat_index; k++) {
				if (cards[j] == table._playerStatus[_seat_index]._hu_out_card_ting[k]) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
					break;
				}
			}
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
		// 刷新手牌
		table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);

		if (seat_index == _seat_index) {
			boolean bSendCardBaoTing = false;
			for (int j = 0; j < table._playerStatus[_seat_index]._hu_out_card_count; j++) {
				if (_send_card_data == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
					bSendCardBaoTing = true;
				}
			}

			// 癞子
			int real_card = _send_card_data;
			if (table._logic.is_magic_card(_send_card_data)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else {
				if (bSendCardBaoTing) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
				}
			}
			// 摸牌
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index, 2);
		}

		table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
		table.operate_player_status();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (has_xuan_wang[i]) {
				return true;
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = 0;// 设置为报听状态
		}
		table.operate_player_data();

		table._game_status = GameConstants.GS_MJ_PLAY;
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		if (curPlayerStatus.has_action()) { // 有动作
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
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
	public boolean handler_operate_card(MJTable_HongDong table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

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
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			// 能胡不胡，要过圈
			if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			if (table._playerStatus[_seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
					|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			table.has_gang_count++;
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
			if (_seat_index == table._cur_banker) {
				table._cur_banker = _seat_index;
			} else {
				table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			}

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_hd(_seat_index, _seat_index, operate_card, true);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index) {
					continue;
				}
			}
			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);
			return true;
		}
		case Constants_MJ_SXHD.WIK_YING_KOU: //
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

			table._ying_kou[seat_index].setLeft(true);
			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}
		case GameConstants.WIK_BAO_TING: //
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
			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
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
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 色子
		if (table._cur_round == 1) {
			roomResponse.setEffectCount(4);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[2]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[3]);
		} else {
			roomResponse.setEffectCount(2);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
		}

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

			if (i == _seat_index) {
				// 牌
				if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[i] != 0) {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 3);
				} else {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
				}
			} else {
				// 牌
				if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[i] != 0) {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 2);
				} else {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
				}
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(cards, _send_card_data);
		}
		// 癞子
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		int h = hand_card_count;
		if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
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
			tableResponse.addHuXi(table.player_magic_card[p]);
		}
		for (int i = 0; i < h; i++) {
			if (cards[i] > GameConstants.CARD_ESPECIAL_TYPE_GUI && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		boolean bSendCardBaoTing = false;
		if (_seat_index == seat_index) {
			// 报听
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int baotingcards[] = new int[GameConstants.MAX_COUNT];
			int baotingcount = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], baotingcards);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
				// 如果可以报听,刷新自己的手牌
				int ting_count = table._playerStatus[_seat_index]._hu_out_card_count;
				if (ting_count > 0) {
					for (int i = 0; i < baotingcount; i++) {
						for (int j = 0; j < ting_count; j++) {
							if (table._logic.is_magic_card(baotingcards[i]))
								baotingcards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							if (baotingcards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j])
								baotingcards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							if (_send_card_data == table._playerStatus[_seat_index]._hu_out_card_ting[j])
								bSendCardBaoTing = true;
						}
					}
					table.operate_player_cards_with_ting(_seat_index, baotingcount, baotingcards, 0, null);
				}
			}
		}

		// 癞子
		int real_card = _send_card_data;
		if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else {
			if (bSendCardBaoTing) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
		}

		// 摸牌
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index, 2);

		this.player_reconnect(table, seat_index);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (has_xuan_wang[i]) {
				return true;
			}
		}
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0 && table._playerStatus[seat_index]._card_status != GameConstants.CARD_STATUS_BAO_TING
				&& table._playerStatus[seat_index]._card_status != Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}

	private void player_reconnect(MJTable_HongDong table, int seat_index) {
		if (!has_xuan_wang[seat_index] || possible_wang.get(seat_index).size() == 0) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 发送数据
		possible_wang.get(seat_index).forEach(card -> {
			roomResponse.addDouliuzi(card);
		});
		roomResponse.setPaoMin(possible_wang.get(seat_index).size());
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

	@Override
	public boolean handler_player_out_card(MJTable_HongDong table, int seat_index, int card) {
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

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}
		table.exe_out_card(_seat_index, card, GameConstants.HU_CARD_TYPE_GANG_KAI);

		return true;
	}
}
