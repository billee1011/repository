package com.cai.game.mj.chenchuang.quanzhou;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_QUAN_ZHOU;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

public class HandlerDispatchCard_QuanZhou extends MJHandlerDispatchCard<Table_QuanZhou> {
	boolean ting_send_card = false; // 听发的牌
	public int _send_card_data;
	public int _seat_index;
	public int _type;

	public GangCardResult m_gangCardResult;

	public HandlerDispatchCard_QuanZhou() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@Override
	public void exe(Table_QuanZhou table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();// 过圈清除不能碰的牌
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();

		// 流局处理
		if (table.GRR._left_card_count == 16) {
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
			_send_card_data = 0x01;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		// 判断是否是杠后抓牌,杠开花只算接杠
		int card_type = Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_ZI_MO;
		// 检查牌型,听牌
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
		if(table.GRR._cards_index[_seat_index][table._logic.get_magic_card_index(0)] + (table._logic.is_magic_card(_send_card_data)?1:0) == 3){
			curPlayerStatus.add_action(Constants_MJ_QUAN_ZHOU.WIK_SAN_JIN_DAO);
			curPlayerStatus.add_action_card(1, _send_card_data, Constants_MJ_QUAN_ZHOU.WIK_SAN_JIN_DAO, _seat_index);
		}else if (action != GameConstants.WIK_NULL) {
			if(table.swim_status[_seat_index] == 0){
				table.process_swim_out_card(_seat_index, _send_card_data);
				if(table.swim_status[_seat_index] > 0)
					table.swim_status[_seat_index] = 1;
			}
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_action_card(table.swim_status[_seat_index] + 1, _send_card_data, GameConstants.WIK_ZI_MO, _seat_index);
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

			int cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			int[] copy_tmp_cards = Arrays.copyOf(cards, Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT);
			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (table._logic.is_magic_card(copy_tmp_cards[i])) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// 出任意一张牌时，能胡哪些牌 -- End

		int show_send_card = get_show_send_card(table);
		// 显示牌
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;
		table._send_card_data = _send_card_data;
		// 检查能不能杠
		if (table.GRR._left_card_count > 15) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table.analyse_gang(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.GRR._cards_abandoned_gang[_seat_index]);

			if (cbActionMask != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}
		
		if(_type ==  GameConstants.GANG_TYPE_HONG_ZHONG){
			table.execute_first_flower();
			return;
		}
		
		if(table.execute_exist_flower(_seat_index))
			return;

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
	public boolean handler_player_out_card(Table_QuanZhou table, int seat_index, int card) {
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
		table.process_swim_out_card(seat_index, card);
		table.exe_out_card(_seat_index, card, _type);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_QuanZhou table, int seat_index, int operate_code, int operate_card) {
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
			/*for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if(m_gangCardResult.isPublic[i] != 0)
					table.GRR._cards_abandoned_gang[seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])] = 1;
			}*/
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
			return true;
		}
		case Constants_MJ_QUAN_ZHOU.WIK_SAN_JIN_DAO: {
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			chr.set_valid(true);
			chr.opr_or(Constants_MJ_QUAN_ZHOU.CHR_THREE_GOLD_INVERTED);
			if(_seat_index != table._cur_banker)
				table._cur_banker = (table._cur_banker + 1) % table.getTablePlayerNumber();
			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);
			
			table._player_result.xiao_hu_zi_mo[_seat_index]++;//大结算三金倒个数
			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			ChiHuRight chr = table.GRR._chi_hu_rights[seat_index];
			if(table.swim_status[seat_index] > 0){
				chr.set_empty();
			}
			chr.set_valid(true);
			if(_seat_index != table._cur_banker)
				table._cur_banker = (table._cur_banker + 1) % table.getTablePlayerNumber();
			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;
			if(table.swim_status[seat_index] == 3){
				chr.opr_or(Constants_MJ_QUAN_ZHOU.CHR_THREE_SWIM);
				table._player_result.da_hu_dian_pao[_seat_index]++;
			}else if(table.swim_status[seat_index] == 2){
				chr.opr_or(Constants_MJ_QUAN_ZHOU.CHR_TWO_SWIM);
				table._player_result.da_hu_jie_pao[_seat_index]++;
			}else if(table.swim_status[seat_index] == 1){
				chr.opr_or(Constants_MJ_QUAN_ZHOU.CHR_SWIN_GOLD);
				table._player_result.da_hu_zi_mo[_seat_index]++;
			}
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
	
	public int get_show_send_card(Table_QuanZhou table){
		int show_send_card = _send_card_data;
		if(table.swim_status[_seat_index] > 0){
			show_send_card += 10000 + table.swim_status[_seat_index] * 1000;
		}else if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		if (table._logic.is_magic_card(_send_card_data)) 
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		return show_send_card;
	}

	@Override
	public boolean handler_player_be_in_room(Table_QuanZhou table, int seat_index) {
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
			tableResponse.addTrustee(table.istrustee[i]);
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
			for (int j = 0; j < Constants_MJ_QUAN_ZHOU.MAX_WEAVE; j++) {
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
		int hand_cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table.remove_card_by_data(hand_cards, _send_card_data);
		}
		int[] copy_tmp_cards = Arrays.copyOf(hand_cards, Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT);
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

		for (int i = 0; i < Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT; i++) {
			if (table._logic.is_magic_card(copy_tmp_cards[i])) {
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

		int show_send_card = get_show_send_card(table);
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
	
}
