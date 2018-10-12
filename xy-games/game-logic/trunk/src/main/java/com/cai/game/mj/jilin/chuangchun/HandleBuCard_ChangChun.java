package com.cai.game.mj.jilin.chuangchun;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandleBuCard_ChangChun extends AbstractMJHandler<MjTable_ChangChun> {
	protected int _seat_index = GameConstants.INVALID_SEAT;
	protected int _action;
	protected int _card;
	protected int _provider;
	protected int _type;

	public void reset_status(int seat_index, int provider, int action, int card, int type) {
		_seat_index = seat_index;
		_action = action;
		_card = card;
		_provider = provider;
		_type = type;
	}

	@Override
	public void exe(MjTable_ChangChun table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();

			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		table._current_player = _seat_index;
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);
		
		boolean bAroseAction = table.estimate_player_bu_card_respond(_seat_index, _card,_type);
		
		if (bAroseAction == false) {
			exe_bu_card(table);
		} else {
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	
	}
	
	
	protected boolean exe_bu_card(MjTable_ChangChun table) {
		
		boolean hava_this_action = false;
		for (int i = 0; i < table.m_BuCardResult.cbCardCount; i++) {
			if(_card == table.m_BuCardResult.card[i] && _type == table.m_BuCardResult.type[i]){
				hava_this_action = true;
			}
		}
		if(!hava_this_action){
			table.log_player_error(_seat_index, "补牌没有这个组合");
			return false;
		}
		
		int cbCardIndex = table._logic.switch_to_card_index(_card);
		int cbWeaveIndex = -1;
		//table.GRR._weave_count[_seat_index]++;
		//table.operate_remove_discard(_seat_index, table.GRR._discard_count[_seat_index]);

		
		for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
			int cbWeavetype = table.GRR._weave_items[_seat_index][i].type;
			if (_type == cbWeavetype) {
				cbWeaveIndex = i;
				break;
			}
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "补牌出错");
				return false;
			}
		}
		
		if(_type == GameConstants.GANG_TYPE_XIAO_JI_FEI_DAN){
			if(_card == 0x31 || _card == 0x32 || _card == 0x33 || _card == 0x34){
				table.GRR._weave_items[_seat_index][cbWeaveIndex].type = GameConstants.GANG_TYPE_XUAN_FENG_DAN;
			}else if(_card == 0x35 || _card == 0x36 || _card == 0x37){
				table.GRR._weave_items[_seat_index][cbWeaveIndex].type = GameConstants.GANG_TYPE_XI_DAN;
			}
		}
		
		//table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
		//table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		int len = table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card.length;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[len+1] = _card;

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] -= 1;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
        
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
			
			for (int n = 0; n < table.GRR._weave_items[_seat_index][i].weave_card.length; n++) {
				if (table.GRR._weave_items[_seat_index][i].weave_card[n] == 0) {
					continue;
				}
				weaves[i].weave_card[n] = table.GRR._weave_items[_seat_index][i].weave_card[n];
			}
	
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[_seat_index]._hu_cards,
				table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index)
				continue;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= GameConstants.CELL_SCORE ;
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += GameConstants.CELL_SCORE;

		}
		table._player_result.ming_gang_count[_seat_index]++;
		
		table.exe_dispatch_card(_seat_index, GameConstants.DISPATCH_CARD_TYPE_GANG, 0);
		
		return true;
	}

	@Override
	public boolean handler_operate_card(MjTable_ChangChun table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}
		
		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		// 优先级判断，不通炮玩法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();

			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank_sy(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank_sy(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank_sy(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank_sy(table._playerStatus[target_player]._action_count,
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

		operate_card = _card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { operate_card, operate_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_seat_index, _card);

			table.exe_chi_peng(target_player, _seat_index, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_seat_index, _card);

			table.exe_gang(target_player, _seat_index, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);

			return true;
		}
		case GameConstants.WIK_NULL: {
			
			exe_bu_card(table);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != target_player) {
					table.GRR._chi_hu_rights[i].set_valid(false);
					table.GRR._chi_hu_rights[i].set_empty();
				}
			}

			table.GRR._chi_hu_rights[target_player].set_valid(true);

            if(target_player == table._cur_banker){
                table._cur_banker = target_player;
            }
            else{
            	table._cur_banker = (table._cur_banker + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
            }

			table.GRR._chi_hu_card[target_player][0] = _card;
			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score(target_player, _seat_index, _card, false);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(MjTable_ChangChun table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._playerStatus[_seat_index].get_status() == GameConstants.Player_Status_NULL) {
			table.log_error("重复出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		table.exe_out_card(_seat_index, card, GameConstants.DISPATCH_CARD_TYPE_NORMAL);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MjTable_ChangChun table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setIsGoldRoom(table.is_sys());

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
		tableResponse.setSendCardData(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
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
				for (int n = 0; n < table.GRR._weave_items[i][j].weave_card.length; n++) {
					if (table.GRR._weave_items[i][j].weave_card[n] == 0) {
						continue;
					}
					weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[n]);
				}
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].type);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0); 

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;
		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				int iTempCard = hand_cards[j];
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						iTempCard += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (table._logic.is_magic_card(hand_cards[j])) {
					iTempCard += GameConstants.CARD_ESPECIAL_TYPE_GUI;
				}
				tableResponse.addCardsData(iTempCard);
			}
		}
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
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
