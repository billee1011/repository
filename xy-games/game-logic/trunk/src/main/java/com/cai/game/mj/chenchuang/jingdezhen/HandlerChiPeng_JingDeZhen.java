package com.cai.game.mj.chenchuang.jingdezhen;

import java.util.HashSet;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_JING_DE_ZHEN;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerChiPeng_JingDeZhen extends MJHandlerChiPeng<Table_JingDeZhen> {
	protected int _seat_index;
	protected int _action;
	protected int _card;
	protected int _provider;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerChiPeng_JingDeZhen() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int provider, int action, int card, int type) {
		_seat_index = seat_index;
		_action = action;
		_card = card;
		_provider = provider;
		_type = type;
	}

	@Override
	public void exe(Table_JingDeZhen table) {
		if(_action == GameConstants.WIK_PENG && table._logic.is_magic_card(_card))
			table.out_bao_count[_seat_index] += 2;
		// 把碰的牌添加到落地组合中
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		// if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]==1)//碰了后手上还有一张就是拆杠碰
		// table.GRR._weave_items[_seat_index][wIndex].is_vavild = false;
		table._current_player = _seat_index;
		if(table.is_bao_ding[_provider] != 2 && table.player_bai_count[_provider] == 0)
			table.cpg_count[_seat_index][_provider]++;
		// 复制落地组合
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}
		// 记录回放动作
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();
		// 刷新玩家手牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		for (int i = 0; i < hand_card_count; i++) {
			int data = cards[i] > 3000 ? cards[i] - 3000 : cards[i];
            if (table._logic.is_magic_card(data)) {
                cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
		}
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;

		int card_type_count = GameConstants.MAX_ZI_FENG;
		for (int i = 0; i < 3; i++)table.can_bai_out_card[i] = new HashSet<Integer>();
		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;
				int data = table._logic.switch_to_card_data(i);
				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, data);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = data;

					ting_count++;
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			int tmp_cards[] = new int[GameConstants.MAX_COUNT];
			int tmp_hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);

			for (int i = 0; i < tmp_hand_card_count; i++) {
				int data = tmp_cards[i] > 3000 ? tmp_cards[i] - 3000 : tmp_cards[i];
	            if (table._logic.is_magic_card(data)) {
	            	tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
	            }
				for (int j = 0; j < ting_count; j++) {
					if (data == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);
		} else {
			// 刷新手牌
			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 碰后检查有没有杠
		m_gangCardResult.cbCardCount = 0;

		int cbActionMask = table.analyse_gang(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.GRR._cards_abandoned_gang[_seat_index]);

		if (cbActionMask != GameConstants.WIK_NULL) {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if(m_gangCardResult.isPublic[i] == 1){
					if(!curPlayerStatus.has_action_by_code(Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG))
						curPlayerStatus.add_action(Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG);
					table.add_action_weave(curPlayerStatus, m_gangCardResult.cbCardData[i], _seat_index, 1, Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG);
				}
				if(m_gangCardResult.isPublic[i] == 0){
					if(!curPlayerStatus.has_action_by_code(Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG))
						curPlayerStatus.add_action(Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG);
					table.add_action_weave(curPlayerStatus, m_gangCardResult.cbCardData[i], _seat_index, 0, Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG);
					
					if(!curPlayerStatus.has_action_by_code(Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG))
						curPlayerStatus.add_action(Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG);
					table.add_action_weave(curPlayerStatus, m_gangCardResult.cbCardData[i], _seat_index, 0, Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG);
				}
			}
		}
		int baiPaiCount = table.getBaiPaiCount();
		if(table.is_bao_ding[_seat_index] != 2 && baiPaiCount > 0 && (table.an_gang_count[_seat_index] == 0 || table.has_rule(Constants_MJ_JING_DE_ZHEN.GAME_RULE_AN_GANG_KE_BAI))){//摆牌
			curPlayerStatus.add_action(Constants_MJ_JING_DE_ZHEN.WIK_BAI_PAI);
			table.add_action_weave(curPlayerStatus, 0, _seat_index, baiPaiCount, Constants_MJ_JING_DE_ZHEN.WIK_BAI_PAI);
		}
		
		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	@Override
	public boolean handler_operate_card(Table_JingDeZhen table, int seat_index, int operate_code, int operate_card) {
		int bai_count = 0;
		if(operate_card > 30000){
			bai_count = 3;
			operate_card -= 30000;
		}else if(operate_card > 20000){
			bai_count = 2;
			operate_card -= 20000;
		}else if(operate_card > 10000){
			bai_count = 1;
			operate_card -= 10000;
		}
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case Constants_MJ_JING_DE_ZHEN.WIK_BAI_PAI:{
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code + 100 + bai_count}, 1, GameConstants.INVALID_SEAT);
			table.operate_player_cards(seat_index, 0, null, 0, null);
			table._player_result.men_qing[seat_index]++;
			table._player_result.biaoyan[seat_index] = bai_count;
			table.operate_player_info();
			table.player_bai_count[_seat_index] = bai_count;
			table._playerStatus[seat_index].clean_action(Constants_MJ_JING_DE_ZHEN.WIK_BAI_PAI);
			int card = operate_card > GameConstants.CARD_ESPECIAL_TYPE_TING ? operate_card - GameConstants.CARD_ESPECIAL_TYPE_TING : operate_card;
			card = card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI ? card - GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI : card;
			int index = table._logic.switch_to_card_index(card);
			int cards[] = new int[GameConstants.MAX_COUNT];
			table.GRR._cards_index[_seat_index][index]--;
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			
			table.set_ting_card_bai(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
					_seat_index);
			table.GRR._cards_index[_seat_index][index]++;
			for (int i = 0; i < hand_card_count; i++) {
				int data = cards[i] > 3000 ? cards[i] - 3000 : cards[i];
	            if (table._logic.is_magic_card(data)) {
	                cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
	            }
			}
			
			table.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
			handler_player_out_card(table, _seat_index, operate_card);
			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_JingDeZhen table, int seat_index, int card) {
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

		table.exe_out_card(_seat_index, card, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_JingDeZhen table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setZongliuzi(table.continueBankerCount == 0 ? -1 : table.continueBankerCount);
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
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
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

			//tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			
			
			boolean is_ming_pai = table.is_bao_ding[i] == 2 || table.player_bai_count[i] > 0;
			tableResponse.addTrustee(is_ming_pai);
			tableResponse.addWinnerOrder(is_ming_pai ? 1 : 0);
			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if(is_ming_pai || i == seat_index){
					int data = hand_cards[j] > 3000 ? hand_cards[j] - 3000 : hand_cards[j];
					if (table._logic.is_magic_card(data)) 
						tableResponse.addHuXi(hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					else
						tableResponse.addHuXi(hand_cards[j]);
				}else
					tableResponse.addHuXi(0);
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					int data = hand_cards[j] > 3000 ? hand_cards[j] - 3000 : hand_cards[j];
					if (data == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			int data = hand_cards[i] > 5000 ? hand_cards[i] - 5000 : hand_cards[i];
			data = data > 3000 ? data - 3000 : data;
				if (table._logic.is_magic_card(data)) {
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
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
				int card = table._playerStatus[seat_index]._hu_out_cards[i][j];
				if (table._logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
