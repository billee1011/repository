package com.cai.game.mj.jiangxi.nanchang;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NANCHANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_NanChang extends MJHandlerDispatchCard<MJTable_NanChang> {
	boolean ting_send_card = false;

	protected GangCardResult m_gangCardResult;

	public MJHandlerDispatchCard_NanChang() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable_NanChang table) {
		table.sendJingData();
		//记录摸牌次数
		table.addDispatchcardNum(_seat_index);
		if(_type == Constants_MJ_NANCHANG.DISPLAYER_TYPE_SUI){
			exeSui(table);
			return;
		}
		
		if (table.GRR._left_card_count == Constants_MJ_NANCHANG.END + 1) {
			table.isLast = true;
		}
		if (table.DEBUG_CARDS_MODE) {
			//table.GRR._left_card_count =Constants_MJ_NANCHANG.END;
		}
		if (table.GRR._left_card_count == Constants_MJ_NANCHANG.END) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._gang_score[i].gang_count = 0;
			}
			table.isLiuJu = true;
			table._cur_banker = (table._cur_banker + 1 + table.getTablePlayerNumber()) %table.getTablePlayerNumber() ;
			table.gameEndShowCards();
			table.handler_game_finish(table.GRR._banker_player, GameConstants.Game_End_DRAW);
			return;
		}
		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;
		table._provide_player = _seat_index;

		if (_type == GameConstants.GANG_TYPE_JIE_GANG) {
			int weave_count = table.GRR._weave_count[_seat_index];
			table._provide_player = table.GRR._weave_items[_seat_index][weave_count - 1].provide_player;
		}

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x22;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = Constants_MJ_NANCHANG.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants.GANG_TYPE_JIE_GANG) {
			card_type = Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA;
		} else if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) { // 暗杠碰杠按杠开计分
			card_type = Constants_MJ_NANCHANG.CHR_GANG_SHANG_HUA;
		}
		int action = table.analyse_chi_hu_card_new(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		// 听牌角标
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
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);
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
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards,table.getJingData(_seat_index));
			table.changeCard(cards, table.GRR._card_count[_seat_index],_seat_index);
			table.GRR._cards_index[_seat_index][send_card_index]++;
			for (int i = 0; i < hand_card_count; i++) {
				int card = table.getRealCard(cards[i]);
				for (int j = 0; j < ting_count; j++) {
					if (card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
			table.operate_player_cards_with_ting_gzcg(_seat_index, hand_card_count, cards, 0, null);
		}
		int real_card = _send_card_data;
		real_card = table.changeCard(_send_card_data, _seat_index);
		if (ting_send_card && real_card < 256) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;

		int type = estimate_gang_card_dispatch_card(table, _seat_index);
		for (int i = 0; i < type; i++) {
			curPlayerStatus.add_action(GameConstants.WIK_GANG);
			curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, 1); // 杠
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return;
	}
	
	public void exeSui(MJTable_NanChang table){
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		for(int i = 0; i < table.getTablePlayerNumber(); i++){
			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards,table.getJingData(i));
			if (_seat_index == i) {
				table._logic.remove_card_by_data(hand_cards, _send_card_data);
			}
			table.changeCard(hand_cards, hand_card_count, i);
			table.operate_player_cards_toPlayer(i, hand_card_count, hand_cards, 0, null);
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int[] card_index = Arrays.copyOf(table.GRR._cards_index[_seat_index], table.GRR._cards_index[_seat_index].length);
		//减去发牌
		card_index[table._logic.switch_to_card_index(_send_card_data)]--;
		int action = table.analyse_chi_hu_card_new(card_index, table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, Constants_MJ_NANCHANG.CHR_ZI_MO, _seat_index, _seat_index);

		//_send_card_data = table.changeCard(_send_card_data, _seat_index);
		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		// 听牌角标
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
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);
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
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards,table.getJingData(_seat_index));
			table.changeCard(cards, table.GRR._card_count[_seat_index],_seat_index);
			table.GRR._cards_index[_seat_index][send_card_index]++;
			for (int i = 0; i < hand_card_count; i++) {
				int card = table.getRealCard(cards[i]);
				for (int j = 0; j < ting_count; j++) {
					if (card == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
			table.operate_player_cards_with_ting_gzcg(_seat_index, hand_card_count, cards, 0, null);
		}
		int real_card = _send_card_data;
		real_card = table.changeCard(_send_card_data, _seat_index);
		if (ting_send_card && real_card < 256) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;

		int type = estimate_gang_card_dispatch_card(table, _seat_index);
		for (int i = 0; i < type; i++) {
			curPlayerStatus.add_action(GameConstants.WIK_GANG);
			curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, 1); // 杠
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
		return;
	}

	@Override
	public boolean handler_player_out_card(MJTable_NanChang table, int seat_index, int card) {
		card = table.get_real_card(card);
		//card = table.changeCard( card, seat_index);
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		table.exe_out_card(_seat_index, card, _type);

		return true;
	}

	@Override
	public boolean handler_operate_card(MJTable_NanChang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);
			if(_seat_index ==  table._cur_banker){
				table._cur_banker = _seat_index;
			}else{
				table._cur_banker = (table._cur_banker + 1 + table.getTablePlayerNumber()) %table.getTablePlayerNumber() ;
			}

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			// 3秒后再弹出小结算
			// int delay = GameConstants.GAME_FINISH_DELAY +
			// table.GRR._chi_hu_rights[_seat_index].type_count;
			int delay = 1500 ;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.MILLISECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_NanChang table, int seat_index) {
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT); // 客户端说这里要+1000
																																// 不知道是什么瞎操作
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
		for (int i = 0; i < 2; i++) {
			tableResponse.addWinnerOrder(table.jing[i]);
		}
		tableResponse.setActionCard(table.xiaJingNumber);

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards,table.getJingData(seat_index));


		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}
		table.changeCard(hand_cards, table.GRR._card_count[seat_index],seat_index);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				int card = table.getRealCard(hand_cards[j]);
				for (int k = 0; k < out_ting_count; k++) {
					if (card == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		roomResponse.setOutCardCount(out_ting_count);
		table.buildJingData(seat_index, roomResponse);
		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		int real_card = _send_card_data;
		real_card = table.changeCard(_send_card_data, _seat_index);
		if (ting_send_card && real_card < 256) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		return true;
	}

	public int estimate_gang_card_dispatch_card(MJTable_NanChang table, int seatIndex) {
		m_gangCardResult.cbCardCount = 0;

		// 暗杠
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table.GRR._cards_index[seatIndex][i] == 4) {
				m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_AN_GANG;
				m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = table._logic.switch_to_card_data(i);
			}
		}

		// 明杠
		for (int i = 0; i < table.GRR._weave_count[seatIndex]; i++) {
			if (table.GRR._weave_items[seatIndex][i].weave_kind == GameConstants.WIK_PENG) {
				int card = table.GRR._weave_items[seatIndex][i].center_card & 0x000FF; // 牌值还原
				int index = table._logic.switch_to_card_index(card);
				if (table.GRR._cards_index[seatIndex][index] > 0) {
					m_gangCardResult.type[m_gangCardResult.cbCardCount] = GameConstants.GANG_TYPE_ADD_GANG;
					m_gangCardResult.cbCardData[m_gangCardResult.cbCardCount++] = card;
				}
			}
		}
		return m_gangCardResult.cbCardCount;
	}
}
