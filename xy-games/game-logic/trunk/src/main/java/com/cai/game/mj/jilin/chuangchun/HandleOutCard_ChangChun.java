package com.cai.game.mj.jilin.chuangchun;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.jilin.songyuan.MjTable_SongYuan;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandleOutCard_ChangChun extends AbstractMJHandler<MjTable_ChangChun> {
	public int _out_card_player = GameConstants.INVALID_SEAT;
	public int _out_card_data = GameConstants.INVALID_VALUE;
	public int _type;

	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	@Override
	public void exe(MjTable_ChangChun table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		int next_player = table.get_next_seat(_out_card_player);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		
		for(int i = 0;i < hand_card_count;i++){
			if(table._logic.is_magic_card(cards[i])){
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			}
		}
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		int real_card = _out_card_data;
        if (table._logic.is_magic_card(_out_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_GUI;
        }
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player);

		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);

		boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);

		if (bAroseAction == false) {
			//满足换宝的条件
			boolean dui_bao = false;
			if(table.is_huan_bao() && table.GRR._left_card_count > 4){
				int huan_bao_payler = GameConstants.INVALID_SEAT;
				for(int i = 0;i < table.getTablePlayerNumber(); i++){
					huan_bao_payler = (_out_card_player + GameConstants.GAME_PLAYER + i) % GameConstants.GAME_PLAYER;
					if(table._playerStatus[huan_bao_payler].is_bao_ting()){
						break;
					}
				}
				if(huan_bao_payler == GameConstants.INVALID_SEAT){
					huan_bao_payler = _out_card_player;
				}
				dui_bao = table.fan_bao_pai(huan_bao_payler,false,false,true);
			}
			if(!dui_bao){
				//报听没看宝
				if(table._playerStatus[next_player].ting_check){
					if(table.is_first_ting()){
						dui_bao = table.fan_bao_pai(table._current_player,true,false,false);
					}else{
						dui_bao = table.fan_bao_pai(table._current_player,false,true,false);
					}
					table._playerStatus[next_player].ting_check = false;
				}
				if(!dui_bao)
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
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

		operate_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		
		case GameConstants.WIK_LEFT:{
			int cbRemoveCard[] = new int[] { operate_card + 1, operate_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case GameConstants.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card - 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case GameConstants.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case GameConstants.WIK_PENG:{
			int cbRemoveCard[] = new int[] { operate_card, operate_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_gang(target_player, _out_card_player, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);

			return true;
		}
		case GameConstants.WIK_NULL: {
			table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			//满足换宝的条件
			boolean dui_bao = false;
			if(table.is_huan_bao() && table.GRR._left_card_count > 4){
				int huan_bao_payler = GameConstants.INVALID_SEAT;
				for(int i = 0;i < table.getTablePlayerNumber(); i++){
					huan_bao_payler = (_out_card_player + GameConstants.GAME_PLAYER + i) % GameConstants.GAME_PLAYER;
					if(table._playerStatus[huan_bao_payler].is_bao_ting()){
						break;
					}
				}
				if(huan_bao_payler == GameConstants.INVALID_SEAT){
					huan_bao_payler = _out_card_player;
				}
				dui_bao = table.fan_bao_pai(huan_bao_payler,false,false,true);
			}
			if(!dui_bao){
				//报听没看宝
				if(table._playerStatus[table._current_player].ting_check){
					if(table.is_first_ting()){
						dui_bao = table.fan_bao_pai(table._current_player,true,false,false);
					}else{
						dui_bao = table.fan_bao_pai(table._current_player,false,true,false);
					}
					table._playerStatus[table._current_player].ting_check = false;
				}
				if(!dui_bao)
					table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}
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

			//table.set_niao_card(target_player);

			table.GRR._chi_hu_card[target_player][0] = _out_card_data;
			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score(target_player, _out_card_player, _out_card_data, false);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(MjTable_ChangChun table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
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

		//癞子标记
		for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(hand_cards[j])) {
            	hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }
		
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
