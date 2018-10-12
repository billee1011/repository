package com.cai.game.mj.chenchuang.pingjiang;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_PING_JIANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

public class HandlerDispatchCard_PingJiang extends MJHandlerDispatchCard<Table_PingJiang> {
	boolean ting_send_card = false; //听发的牌
	protected int _seat_index;
	public static int _send_card_data;
	boolean flag = true;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_PingJiang() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_PingJiang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();//过圈清除不能碰的牌
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();

		//流局处理
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			//得到下家
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			return;
		}

		//发牌
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		//table.GRR._left_card_count = 1;
		table._current_player = _seat_index;

		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x18;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		//检查牌型,听牌
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, Constants_PING_JIANG.HU_CARD_TYPE_ZI_MO, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			/*if(table._player_result.biaoyan[_seat_index] == 1){
				table.GRR._win_order[_seat_index] = 1;
				table.set_niao_card();
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);
				table.process_chi_hu_player_operate(_seat_index, _send_card_data, true);
				table.process_chi_hu_player_score(_seat_index, _seat_index, _send_card_data, true);
				table._player_result.zi_mo_count[_seat_index]++;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return;
			}*/
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			/*if(table._player_result.biaoyan[_seat_index] == 1){
				GameSchedule.put(()->{
					table.is_da_mi_out_card = false;
					handler_player_out_card(table, _seat_index, _send_card_data);
				},GameConstants.GAME_FINISH_DELAY_FLS, TimeUnit.SECONDS);
				return;
			}*/
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI;
		table.is_bao_ting = false;
		table.bao_ting_cards = new int[card_type_count];
		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;
				//打出哪些牌可以听牌，同时得到听牌的数量，把可以胡牌的数据data型放入table._playerStatus[_seat_index]._hu_out_cards[ting_count]
				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, i);

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

		table.GRR._cards_index[_seat_index][send_card_index]--;
		
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data_bao_ting(table.GRR._cards_index[_seat_index], cards, _seat_index);
		table.GRR._cards_index[_seat_index][send_card_index]++;
		int show_send_card = _send_card_data;
		int value = table._logic.get_card_value(show_send_card);
		boolean issqys = table._logic.get_card_color(show_send_card) == table._player_result.ziba[_seat_index];
		boolean is_258 = (value == 2 || value == 5 || value == 8);
		boolean isj = (is_258 && table.has_bao_ting) || issqys;
		if(!table.has_bao_ting){
			if(issqys && table.cardqyscount == hand_card_count){
				for (int i = 0; i < hand_card_count; i++) {
					cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				isj = false;
			}
		}else{
			if(is_258 && hand_card_count == table.card258count && issqys && table.cardqyscount == hand_card_count){
				for (int i = 0; i < hand_card_count; i++) {
					cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				isj = false;
			}else if(is_258 && hand_card_count == table.card258count){
				for (int i = 0; i < hand_card_count; i++) {
					int color = table._logic.get_card_color(cards[i]);
					if(color != table._player_result.ziba[_seat_index])
						cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				if(table._logic.get_card_color(show_send_card) != table._player_result.ziba[_seat_index])
					isj = false;
			}else if(issqys && table.cardqyscount == hand_card_count){
				for (int i = 0; i < hand_card_count; i++) {
					int v = table._logic.get_card_value(cards[i]);
					if(v != 2 && v != 5 && v != 8)
						cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				if(value != 2 && value != 5 && value != 8)
					isj = false;
			}
		}
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		if (ting_count > 0) {

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
		}
		// 出任意一张牌时，能胡哪些牌 -- End
		
		if(isj){
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}else if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		//显示牌
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;
		//检查能不能杠
		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table.analyse_gang(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true);

			if (cbActionMask != GameConstants.WIK_NULL) {
				//curPlayerStatus.add_action(GameConstants.WIK_GANG);
				if(table.GRR._left_card_count >= table.get_bird_num())
					table._playerStatus[_seat_index].add_action(Constants_PING_JIANG.WIK_DA_MI_GANG);
				table._playerStatus[_seat_index].add_action(Constants_PING_JIANG.WIK_DA_MI_GANG_MO_PAI);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if(table.GRR._left_card_count >= table.get_bird_num())
						table.add_action_weave(curPlayerStatus, m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i], Constants_PING_JIANG.WIK_DA_MI_GANG);
					table.add_action_weave(curPlayerStatus, m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i], Constants_PING_JIANG.WIK_DA_MI_GANG_MO_PAI);
				}
			}
		}
		//发第一张牌的时候判断有没有板板胡
		if(_type == GameConstants.GANG_TYPE_HONG_ZHONG){
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				boolean is_ban_ban_hu = table.is_ban_ban_hu(table.GRR._cards_index[i]);
				if(is_ban_ban_hu){
					PlayerStatus playerStatus = table._playerStatus[i];
					if(!playerStatus.has_chi_hu()){
						playerStatus.add_action(GameConstants.WIK_ZI_MO);
						playerStatus.add_zi_mo(-1, _seat_index);
					}
					playerStatus.add_action(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU);
					table.GRR._chi_hu_rights[i].opr_or(Constants_PING_JIANG.CHR_BAN_BAN_HU);
				}
			}
		}
		if(table.is_bao_ting && table._player_result.biaoyan[_seat_index] != 1){
			curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
		}
		if (action != GameConstants.WIK_NULL) {
			if(table._player_result.biaoyan[_seat_index] == 1){
				/*table.GRR._win_order[_seat_index] = 1;
				table.set_niao_card();
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);
				table.process_chi_hu_player_operate(_seat_index, _send_card_data, true);
				table.process_chi_hu_player_score(_seat_index, _seat_index, _send_card_data, true);
				table._player_result.zi_mo_count[_seat_index]++;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return;*/
				GameSchedule.put(()->{
					handler_operate_card(table, _seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
				},2, TimeUnit.SECONDS);
				return;
			}
		} else {
			if((table._player_result.biaoyan[_seat_index] == 1 && !curPlayerStatus.has_action_by_bh_code(GameConstants.WIK_GANG)) || table.GRR._left_card_count == 0){
				GameSchedule.put(()->{
					table.is_da_mi_out_card = false;
					handler_player_out_card(table, _seat_index, _send_card_data);
				},2, TimeUnit.SECONDS);
				return;
			}
		}
		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				flag = false;
				table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
			}
		}
		if(flag){
			table.is_da_mi_out_card = false;
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return;
	}

	@Override
	public boolean handler_player_out_card(Table_PingJiang table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(!table.is_da_mi_out_card){
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
		}

		if (_type == GameConstants.GANG_TYPE_HONG_ZHONG) {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.GANG_TYPE_HONG_ZHONG);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, _type);
		}

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_PingJiang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index && _type != GameConstants.GANG_TYPE_HONG_ZHONG) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		
		if (operate_code == GameConstants.WIK_ZI_MO) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            // 效果
            if(table.GRR._chi_hu_rights[seat_index].opr_and(Constants_PING_JIANG.CHR_BAN_BAN_HU).is_empty()){
            	table.process_chi_hu_player_operate(seat_index, operate_card, true);
            }
            else
            	table.process_chi_hu_player_operate_ban(seat_index, -1);
        }

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		if (operate_code == GameConstants.WIK_NULL) {
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			if(_type == GameConstants.GANG_TYPE_HONG_ZHONG){
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._playerStatus[i].has_chi_hu() && !table._playerStatus[i].is_respone()) {
						return true;
					}
				}
				boolean flag = false;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int l = (i + _seat_index + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
					if (table._playerStatus[l].has_action() && table._playerStatus[l].is_respone()) {
						if(table._playerStatus[l].has_action_by_code(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU)){
							operate_code = Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU;
							seat_index = l;
							flag = true;
							break;
						}else if(table._playerStatus[l].has_action_by_code(GameConstants.WIK_ZI_MO)){
							operate_code = GameConstants.WIK_ZI_MO;
							seat_index = l;
							flag = true;
							break;
						}
					}
				}
				if(!flag){
					if(table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING) &&
							table._playerStatus[_seat_index].is_respone()){
						operate_code = GameConstants.WIK_BAO_TING;
						seat_index = _seat_index;
					}else if(table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_GANG) &&
							table._playerStatus[_seat_index].is_respone()){
						operate_code = GameConstants.WIK_GANG;
						operate_card = table._playerStatus[_seat_index].get_operate_card();
						seat_index = _seat_index;
					}else{
						table.is_da_mi_out_card = false;
						table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
						table.operate_player_status();
						return true;
					}
				}
			}else{
				// 不能换章,自动出牌
				if (table._player_result.biaoyan[_seat_index] == 1 || table.GRR._left_card_count == 0) {
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
							TimeUnit.MILLISECONDS);
				} else {
					table.is_da_mi_out_card = false;
					table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
				return true;
			}
		}
		
		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_ZI_MO);
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_BAO_TING);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_chi_hu() && !table._playerStatus[i].is_respone()) {
					return true;
				}
			}
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case Constants_PING_JIANG.WIK_DA_MI_GANG: {
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_GANG_MO_PAI);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_ZI_MO);
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_BAO_TING);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_chi_hu() && !table._playerStatus[i].is_respone()) {
					return true;
				}
			}
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case Constants_PING_JIANG.WIK_DA_MI_GANG_MO_PAI: {
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_GANG);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_ZI_MO);
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_BAO_TING);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_chi_hu() && !table._playerStatus[i].is_respone()) {
					return true;
				}
			}
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_GANG);
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_BAO_TING);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_chi_hu() && !table._playerStatus[i].is_respone()) {
					return true;
				}
			}

			//table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			table._cur_banker = _seat_index;
			table.set_niao_card();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if(table.GRR._chi_hu_rights[i].is_valid()){
					table.GRR._win_order[i] = 1;
					int card = i != _seat_index ? 0 : operate_card;
					table.process_chi_hu_player_score(i, i, card, true);
					
					table._player_result.zi_mo_count[i]++;
				}
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_BAO_TING:{
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_ZI_MO);
			table._playerStatus[seat_index].clean_action(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU);
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_GANG);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_chi_hu() && !table._playerStatus[i].is_respone()) {
					return true;
				}
			}
			table._player_result.biaoyan[_seat_index] = 1;
			table.has_bao_ting = true;
			int cards1[] = new int[GameConstants.MAX_COUNT];
			int show_send_card = _send_card_data;
			int index = table._logic.switch_to_card_index(_send_card_data);
			table.GRR._cards_index[_seat_index][index]--;
			int hand_card_count1 = table.switch_to_cards_data_bao_ting(table.GRR._cards_index[_seat_index], cards1, _seat_index);
			for (int i = 0; i < hand_card_count1; i++) {
				if(cards1[i] > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA){
					if(table.bao_ting_cards[table._logic.switch_to_card_index(cards1[i] - GameConstants.CARD_ESPECIAL_TYPE_WANG_BA)] == 1)
						cards1[i] -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
			}
			table.operate_player_cards(_seat_index, hand_card_count1, cards1, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
			table.GRR._cards_index[_seat_index][index]++;
			if(table.bao_ting_cards[index] != 1){
				show_send_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
			//显示牌
			table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if(i == _seat_index)
					continue;
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table.switch_to_cards_data_bao_ting(table.GRR._cards_index[i], cards, i);
				table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i], table.GRR._weave_items[i]);
			}
			table.operate_player_data();
			table.is_da_mi_out_card = false;
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			return true;
		}
		case Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU:{
			int p = (seat_index - _seat_index + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int l = (i - _seat_index + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
				if (l < p && table._playerStatus[l].has_action_by_code(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU) && !table._playerStatus[l].is_respone()) {
					return true;
				}
			}
			da_mi(table, seat_index);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action_by_code(GameConstants.WIK_ZI_MO) && !table._playerStatus[i].is_respone()) {
					return true;
				}
			}
			
			// 效果
            int hu_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if(table.GRR._chi_hu_rights[i].is_valid()) {
                    hu_count++;
                }
            }
            if(hu_count > 0){
            	table.set_niao_card();
            	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
    				if(table.GRR._chi_hu_rights[i].is_valid()){
    					table.GRR._win_order[i] = 1;
    					table.process_chi_hu_player_score(i, i, operate_card, true);
    					
    					table._player_result.zi_mo_count[i]++;
    				}
    			}

    			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
    					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
            }else{
            	GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_DRAW),
    					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
            }
            
			
			return true;
		}
		}

		return true;
	}
	
	private void da_mi(Table_PingJiang table, int seat_index){
		int i = (seat_index + 1) % table.getTablePlayerNumber();
		for (String cards : table.dami_cards) {
			if(Integer.valueOf(cards.split(",")[1]) == seat_index){
				if (i != _seat_index && table._playerStatus[i].has_action_by_code(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU) && table._playerStatus[i].is_respone()) {
					da_mi(table, i);
				}
				return;
			}
		}
		
		
		int get_bird_num = table.get_bird_num();
		if(table.dami_cards.size() == 0){
			flag = true;
			for (int c = 0; c < get_bird_num; c++) {
				table._send_card_count++;
				int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				/*if (table.DEBUG_CARDS_MODE) {
					send_card_data = 0x17;
				}*/
				--table.GRR._left_card_count;
				int value = table._logic.get_card_value(send_card_data);
				if(value == 2 || value == 5 || value == 8)
					flag = false;
				table.dami_cards.addLast(send_card_data+","+_seat_index);
			}
		}
		table.operate_player_da_mi_card();//客户端显示到桌面
		table._playerStatus[seat_index].clean_action(GameConstants.WIK_ZI_MO);
		if(flag){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			table.GRR._chi_hu_rights[seat_index].opr_or(Constants_PING_JIANG.CHR_DA_MI);
			if(get_bird_num == 2)
				table.GRR._chi_hu_rights[seat_index].opr_or(Constants_PING_JIANG.CHR_DA_MI_HU_2);
			if(get_bird_num == 3)
				table.GRR._chi_hu_rights[seat_index].opr_or(Constants_PING_JIANG.CHR_DA_MI_HU_3);
			table.process_chi_hu_player_operate_ban(seat_index, -1);
		}
		
		if (i != _seat_index && table._playerStatus[i].has_action_by_code(Constants_PING_JIANG.WIK_DA_MI_BAN_BAN_HU) && table._playerStatus[i].is_respone()) {
			da_mi(table, i);
		}
	}

	@Override
	public boolean handler_player_be_in_room(Table_PingJiang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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

			tableResponse.addWinnerOrder(0);

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data_bao_ting(table.GRR._cards_index[seat_index], hand_cards, seat_index);
		int show_send_card = _send_card_data;
		int value = table._logic.get_card_value(show_send_card);
		boolean issqys = table._logic.get_card_color(show_send_card) == table._player_result.ziba[_seat_index];
		boolean is_258 = (value == 2 || value == 5 || value == 8);
		boolean isj = (is_258 && table.has_bao_ting) || issqys;
		if(!table.has_bao_ting){
			if(issqys && table.cardqyscount == hand_card_count){
				for (int i = 0; i < hand_card_count; i++) {
					hand_cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				isj = false;
			}
		}else{
			if(is_258 && hand_card_count == table.card258count && issqys && table.cardqyscount == hand_card_count){
				for (int i = 0; i < hand_card_count; i++) {
					hand_cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				isj = false;
			}else if(is_258 && hand_card_count == table.card258count){
				for (int i = 0; i < hand_card_count; i++) {
					int color = table._logic.get_card_color(hand_cards[i]);
					if(color != table._player_result.ziba[_seat_index])
						hand_cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				if(table._logic.get_card_color(show_send_card) != table._player_result.ziba[_seat_index])
					isj = false;
			}else if(issqys && table.cardqyscount == hand_card_count){
				for (int i = 0; i < hand_card_count; i++) {
					int v = table._logic.get_card_value(hand_cards[i]);
					if(v != 2 && v != 5 && v != 8)
						hand_cards[i] -=GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				}
				if(value != 2 && value != 5 && value != 8)
					isj = false;
			}
		}
		if((seat_index == _seat_index) && table._player_result.biaoyan[_seat_index] == 1){
			if(table.bao_ting_cards[table._logic.switch_to_card_index(show_send_card)] != 1)
			if(is_258)
				show_send_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}else if(isj){
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}else if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}else if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, show_send_card);
			hand_card_count--;
		}

		//  出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;
		if((seat_index == _seat_index) && table._player_result.biaoyan[_seat_index] == 1){
			if(show_send_card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA){
				if(table.bao_ting_cards[table._logic.switch_to_card_index(show_send_card - GameConstants.CARD_ESPECIAL_TYPE_WANG_BA)] == 1){
					show_send_card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
					show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
				}
			}else{
				show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
			for (int i = 0; i < hand_card_count; i++) {
				if(hand_cards[i] > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA){
					if(table.bao_ting_cards[table._logic.switch_to_card_index(hand_cards[i] - GameConstants.CARD_ESPECIAL_TYPE_WANG_BA)] == 1){
						hand_cards[i] -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
						hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
					}
				}else{
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
				}
			}
		}else if ((out_ting_count > 0) && (seat_index == _seat_index)) {
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
		
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		table.operate_player_da_mi_card();//客户端显示到桌面
		return true;
	}
}
