package com.cai.game.mj.chenchuang.shanxituidaohu;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_SAN_XI_TDH;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

public class HandlerDispatchCard_Shan_Xi_TDH extends MJHandlerDispatchCard<Table_Shan_Xi_TDH> {
	boolean ting_send_card = false; // 听发的牌
	protected int _seat_index;
	protected int _send_card_data;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_Shan_Xi_TDH() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@Override
	public void exe(Table_Shan_Xi_TDH table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();// 过圈清除不能碰的牌
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();

		// 流局处理
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);
			return;
		}

		// 发牌
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x03;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		// 判断是否是杠后抓牌,杠开花只算接杠
		int card_type = Constants_MJ_SAN_XI_TDH.HU_CARD_TYPE_ZI_MO;
		// 检查牌型,听牌
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants.WIK_NULL && (!table.has_rule(Constants_MJ_SAN_XI_TDH.GAME_RULE_BAO_TING) || table.is_bao_ting[_seat_index])) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;
				// 打出哪些牌可以听牌，同时得到听牌的数量，把可以胡牌的数据data型放入table._playerStatus[_seat_index]._hu_out_cards[ting_count]
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
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
					if (table._logic.is_magic_card(cards[i])) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
			if(!table.is_bao_ting[_seat_index] && table.has_rule(Constants_MJ_SAN_XI_TDH.GAME_RULE_BAO_TING)){
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
			}
		}
		// 出任意一张牌时，能胡哪些牌 -- End

		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		// 显示牌
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;
		// 检查能不能杠
		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table.analyse_gang(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.GRR._cards_abandoned_gang[_seat_index]);

			if (cbActionMask != GameConstants.WIK_NULL) {
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if(table.is_can_gang(_seat_index, m_gangCardResult.cbCardData[i])){
						if(!curPlayerStatus.has_action_by_code(GameConstants.WIK_GANG))
							curPlayerStatus.add_action(GameConstants.WIK_GANG);
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if(table.is_bao_ting[_seat_index]){
				GameSchedule.put(()->{
					handler_player_out_card(table, _seat_index, _send_card_data);
				},1, TimeUnit.SECONDS);
			}else{
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	@Override
	public boolean handler_player_out_card(Table_Shan_Xi_TDH table, int seat_index, int card) {
		card = table.get_real_card(card);

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
		if(_type == Constants_MJ_SAN_XI_TDH.HU_CARD_TYPE_BAO_TING)
			table.bao_ting_out_card[_seat_index] = card;
		table.exe_out_card(_seat_index, card, _type);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_Shan_Xi_TDH table, int seat_index, int operate_code, int operate_card) {
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

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		if (operate_code == GameConstants.WIK_NULL) {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				table.GRR._cards_abandoned_gang[seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])] = 1;
			}
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			// 不能换章,自动出牌
			if (table.is_bao_ting[_seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
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
			return true;
		}
		case GameConstants.WIK_BAO_TING:{
			table._playerStatus[seat_index]._card_status = 2;//报听后设置状态
			table._player_result.biaoyan[seat_index] = 1;
			table.operate_player_info();
			//报听动画
			//table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1, GameConstants.INVALID_SEAT);
			table.is_bao_ting[_seat_index] = true;
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_BAO_TING);
			_type = Constants_MJ_SAN_XI_TDH.HU_CARD_TYPE_BAO_TING;
			handler_player_out_card(table, _seat_index, operate_card);
			return true;
		}
		case GameConstants.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_Shan_Xi_TDH table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
		// 杠之后，发的牌的张数
		roomResponse.setPageSize(table.gang_dispatch_count);
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
				if(table.bao_ting_out_card[i] > 0 && table.GRR._discard_cards[i][j] == table.bao_ting_out_card[i]){
					int_array.addItem(table.GRR._discard_cards[i][j] + Constants_MJ_SAN_XI_TDH.CARD_ESPECIAL_TYPE_HIDE);
				}else if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (hand_cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
				if (table._logic.is_magic_card(hand_cards[i])) {
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

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

		// if (_type != Constants_HuangZhou.LIANG_LAI_ZI) {
		// int real_card = _send_card_data;
		// if (table._logic.is_magic_card(_send_card_data)) {
		// real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		// } else if (ting_send_card) {
		// real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		// }
		//
		// table.operate_player_get_card(_seat_index, 1, new int[] { real_card
		// }, seat_index);
		// }
		// 出任意一张牌时，能胡哪些牌 -- End

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
