package com.cai.game.mj.chenchuang.ningxinag;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NING_XIANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

public class HandlerDispatchCard_NING_XIANG extends MJHandlerDispatchCard<Table_NING_XIANG> {
	boolean ting_send_card = false; // 听发的牌
	protected int _seat_index;
	protected int _send_card_data;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_NING_XIANG() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_NING_XIANG table) {
		table.fa_pai_count[_seat_index]++;
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
			table._cur_banker = table.last_dispatch_card_player;
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
		if(table.GRR._left_card_count == 0)
			table.last_dispatch_card_player = _seat_index;
		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x13;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = Constants_MJ_NING_XIANG.HU_CARD_TYPE_ZI_MO;
		if(_type == GameConstants.GANG_TYPE_HONG_ZHONG)
			chr.opr_or_long(Constants_MJ_NING_XIANG.CHR_TIAN_HU);
		// 检查牌型,听牌
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		out_card_hu_card(table);
		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		// 显示牌
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;
		if(action == GameConstants.WIK_NULL && _type == GameConstants.GANG_TYPE_HONG_ZHONG){
			boolean is_qi_shou_hu = false;
			for (int p = 0; p < table.getTablePlayerNumber(); p++) {
				int i = (_seat_index + p  + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
				boolean qi_shou_hu = table.is_qi_shou_hu(table.GRR._cards_index[i],i);
				if(qi_shou_hu){
					is_qi_shou_hu = true;
					PlayerStatus playerStatus = table._playerStatus[i];
					playerStatus.add_action(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU);
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
					//return;
				}
			}
			if(is_qi_shou_hu)
				return;
			
			if(table.checkBaoTing())//检查报听
				return;
		}
		
		//检查中途起手胡
		if (table.GRR._left_card_count > 0 && table.is_start()) {
			table.is_zt_qi_shou_hu(table.GRR._cards_index[_seat_index], _seat_index, false);
		}
		// 检查能不能杠 ,开始
		checkGang(table);

		return;
	}

	private void out_card_hu_card(Table_NING_XIANG table) {
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
			}
			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// 出任意一张牌时，能胡哪些牌 -- End
	}

	@Override
	public boolean handler_player_out_card(Table_NING_XIANG table, int seat_index, int card) {
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
	public boolean handler_operate_card(Table_NING_XIANG table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}
		if(_type != GameConstants.GANG_TYPE_HONG_ZHONG){
			if (seat_index != _seat_index) {
				table.log_error("不是当前玩家操作");
				return false;
			}
		}
		if(_type != GameConstants.GANG_TYPE_HONG_ZHONG){
			if (playerStatus.is_respone()) {
				table.log_player_error(seat_index, "出牌,玩家已操作");
				return true;
			}
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		if (operate_code == GameConstants.WIK_NULL) {
			if(playerStatus.has_action_by_code(GameConstants.WIK_BAO_TING)){
				table._playerStatus[seat_index].clean_action();
				table._playerStatus[seat_index].clean_status();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					PlayerStatus ps = table._playerStatus[i];
					if(!ps.is_respone() && ps.has_action_by_code(GameConstants.WIK_BAO_TING))
						return false;
				}
				table.is_judge = true;
				checkGang(table);
			}
			
			if(!table.is_start() && _type == GameConstants.GANG_TYPE_HONG_ZHONG){
				if(seat_index == _seat_index && table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_ZI_MO)){
					table._playerStatus[seat_index].clean_action();
					table._playerStatus[seat_index].clean_status();
					boolean is_qsh = false;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						boolean is_qi_shou_hu = table.is_qi_shou_hu(table.GRR._cards_index[i],i);
						if(is_qi_shou_hu){
							is_qsh = true;
							table._playerStatus[i].add_action(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU);
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							table.operate_player_action(i, false);
						}
					}
					if(is_qsh)
						return false;
				}
				table._playerStatus[seat_index].clean_action();
				table._playerStatus[seat_index].clean_status();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					PlayerStatus ps = table._playerStatus[i];
					if(!ps.is_respone() && ps.has_action_by_code(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU))
						return false;
				}
				exe_qi_shou_hu(table,seat_index);
				return true;
				
				
				/*for (; p < table.getTablePlayerNumber(); p++) {
					int i = (seat_index + p + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
					if(i == _seat_index && p > 0)
						break;
					if(!table.is_judge[i])
						table.is_judge[i] = true;
					else
						continue;
					boolean is_qi_shou_hu = table.is_qi_shou_hu(table.GRR._cards_index[i],i);
					if(is_qi_shou_hu){
						table._playerStatus[i].add_action(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU);
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
						return true;
					}
				}
				// 检查能不能杠
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				table.check_an_add_gang(m_gangCardResult, _seat_index);
				
				if (curPlayerStatus.has_action()) {
					curPlayerStatus._response = false;
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index, false);
				} else {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}*/
			}else{
				PlayerStatus _playerStatus = table._playerStatus[seat_index];
				if(_playerStatus.has_action_by_code(Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN)
						|| _playerStatus.has_action_by_code(Constants_MJ_NING_XIANG.WIK_ZT_SI_XI)){
					if(_playerStatus.has_action_by_code(GameConstants.WIK_ZI_MO)){
						_playerStatus.clean_action();
						_playerStatus.add_action(GameConstants.WIK_ZI_MO);
						_playerStatus.add_zi_mo(_send_card_data, seat_index);
						table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(seat_index, false);
						return true;
					}
				}
				_playerStatus.clean_action();
				_playerStatus.clean_status();
				// 不能换章,自动出牌
				if(table._player_result.biaoyan[_seat_index] == 1 || table.is_gang[_seat_index]){
					GameSchedule.put(()->{
						handler_player_out_card(table, _seat_index, _send_card_data);
					},1, TimeUnit.SECONDS);
				} else {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
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
		case Constants_MJ_NING_XIANG.WIK_BU_ZHANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case GameConstants.WIK_BAO_TING:{
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BAO_TING }, 1, GameConstants.INVALID_SEAT);
			table._player_result.biaoyan[seat_index] = 1;
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_BAO_TING);
			table.operate_player_info();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				PlayerStatus ps = table._playerStatus[i];
				if(!ps.is_respone() && ps.has_action_by_code(GameConstants.WIK_BAO_TING))
					return false;
			}
			table.is_judge = true;
			checkGang(table);
			return true;
		}
		case Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU: {
			table._player_result.an_gang_count[seat_index]++;
			table.has_qi_shou_hu[seat_index] = true;
			table._playerStatus[seat_index].clean_action(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU);
			int qi_shou_hu_score = 0;
			int qi_shou_hu_count = 0;
			for(Entry<Long, Integer> entry : table.qi_shou_hu_type[seat_index].entrySet()){
				qi_shou_hu_count += entry.getValue();
				qi_shou_hu_score +=  entry.getValue() * table.getBottomScore();
				table.sx_card[seat_index].addAll(table.qssx_card[seat_index]);
				table.lls_card[seat_index].addAll(table.qslls_card[seat_index]);
			}
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if(j == seat_index)
					continue;
				int qi_shou_hu_score1 = qi_shou_hu_score;
				if(table.getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_HU_PAI_WEI_ZHUANG) == 1 || j == table._cur_banker || seat_index == table._cur_banker)
					qi_shou_hu_score1 *= 2;
				if(table.getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_QSH_ADD_BIRD) == 1){
					qi_shou_hu_score1 += (table._player_result.pao[j] +table._player_result.pao[seat_index] + table._player_result.qiang[j] + table._player_result.qiang[seat_index]) * qi_shou_hu_count;
				}
				table.GRR._start_hu_score[j] -= qi_shou_hu_score1;
				table._player_result.game_score[j] -= qi_shou_hu_score1;
				table.GRR._start_hu_score[seat_index] += qi_shou_hu_score1;
				table._player_result.game_score[seat_index] += qi_shou_hu_score1;
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				PlayerStatus ps = table._playerStatus[i];
				if(!ps.is_respone() && ps.has_action_by_code(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU))
					return false;
			}
			table.operate_player_info();
			exe_qi_shou_hu(table, _seat_index);
			
			
			
			
			/*for (int p = 1; p < table.getTablePlayerNumber(); p++) {
				int i = (seat_index + p + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
				if(i == _seat_index)
					break;
				if(!table.is_judge[i])
					table.is_judge[i] = true;
				else
					continue;
				boolean is_qi_shou_hu = table.is_qi_shou_hu(table.GRR._cards_index[i],i);
				if(is_qi_shou_hu){
					table._playerStatus[i].add_action(Constants_MJ_NING_XIANG.WIK_QI_SHOU_HU);
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
					return true;
				}
			}*/
			// 检查能不能杠
			/*PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			table.check_an_add_gang(m_gangCardResult, _seat_index);
			
			if (curPlayerStatus.has_action()) {
				curPlayerStatus._response = false;
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}*/
			return true;
		}
		case Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN: {
			table._player_result.an_gang_count[seat_index]++;
			table._playerStatus[seat_index].clean_action(Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN);
			int qi_shou_hu_score = 0;
			int qi_shou_hu_count = 0;
			Map<Long, Integer> ztqi_shou_hu = table.has_ztqi_shou_hu_type[seat_index];
			for(Entry<Long, Integer> entry : table.ztqi_shou_hu_type[seat_index].entrySet()){
				Long key = entry.getKey();
				if(key != Constants_MJ_NING_XIANG.CHR_ZT_LIU_LIU_SHUN)
					continue;
				qi_shou_hu_count += entry.getValue();
				qi_shou_hu_score +=  entry.getValue() * table.getBottomScore();
				table.lls_card[seat_index].addAll(table.qslls_card[seat_index]);
				if(ztqi_shou_hu.containsKey(key)){
					ztqi_shou_hu.put(key, ztqi_shou_hu.get(key) + entry.getValue());
				}else{
					ztqi_shou_hu.put(key, entry.getValue());
				}
				table.qsh_cards_index = new int[table.getTablePlayerNumber()][GameConstants.MAX_INDEX];
				for (int card : table.qslls_card[seat_index]) {
					table.qsh_cards_index[seat_index][table._logic.switch_to_card_index(card)] = 3;
				}
			}
			table.zt_qsh_score(seat_index, qi_shou_hu_score, qi_shou_hu_count, Constants_MJ_NING_XIANG.CHR_ZT_LIU_LIU_SHUN);
			return true;
		}
		case Constants_MJ_NING_XIANG.WIK_ZT_SI_XI: {
			table._player_result.an_gang_count[seat_index]++;
			table._playerStatus[seat_index].clean_action(Constants_MJ_NING_XIANG.WIK_ZT_SI_XI);
			int qi_shou_hu_score = 0;
			int qi_shou_hu_count = 0;
			Map<Long, Integer> ztqi_shou_hu = table.has_ztqi_shou_hu_type[seat_index];
			for(Entry<Long, Integer> entry : table.ztqi_shou_hu_type[seat_index].entrySet()){
				Long key = entry.getKey();
				if(key != Constants_MJ_NING_XIANG.CHR_ZT_SI_XI)
					continue;
				qi_shou_hu_count += entry.getValue();
				qi_shou_hu_score +=  entry.getValue() * table.getBottomScore();
				table.sx_card[seat_index].addAll(table.qssx_card[seat_index]);
				if(ztqi_shou_hu.containsKey(key)){
					ztqi_shou_hu.put(key, ztqi_shou_hu.get(key) + entry.getValue());
				}else{
					ztqi_shou_hu.put(key, entry.getValue());
				}
				table.qsh_cards_index = new int[table.getTablePlayerNumber()][GameConstants.MAX_INDEX];
				for (int card : table.qssx_card[seat_index]) {
					table.qsh_cards_index[seat_index][table._logic.switch_to_card_index(card)] = 4;
				}
			}
			table.zt_qsh_score(seat_index, qi_shou_hu_score, qi_shou_hu_count, Constants_MJ_NING_XIANG.CHR_ZT_SI_XI);
			return true;
		}
		case GameConstants.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;
			table.set_niao_card(seat_index, table.GRR._left_card_count == 0);
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
	
	private void exe_qi_shou_hu(Table_NING_XIANG table, int seat_index) {
		boolean flag = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if(table.has_qi_shou_hu[i])
				flag = true;
		}
		if(flag){
			int a = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int p = (i + seat_index + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
				if(table.has_qi_shou_hu[p])
					a = p;
			}
			int last = a;
			int j = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int p = (i + seat_index + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
				if(table.has_qi_shou_hu[p]){
					table.is_dao_pai[p] = true;
					GameSchedule.put(() -> {
						table.qi_shou_hu_player_operate(p, last);
					}, j * 4500, TimeUnit.MILLISECONDS);
					j++;
				}
			}
			table.operate_player_info();
		}else{
			if(!table.checkBaoTing())
				checkGang(table);
		}
	}
	
	public void checkGang(Table_NING_XIANG table){
		if(table.is_dao_pai[_seat_index])
			table.exe_qi_pai(_seat_index);
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		// 检查能不能杠
		if (table.GRR._left_card_count > 0 && table.is_start()) {
			table.check_an_add_gang(m_gangCardResult, _seat_index);
		}

		if (curPlayerStatus.has_action()) {
			if (table._player_result.biaoyan[_seat_index] == 1 && curPlayerStatus.has_action_by_code(GameConstants.WIK_ZI_MO)
					&& !curPlayerStatus.has_action_by_code(Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN) &&
					!curPlayerStatus.has_action_by_code(Constants_MJ_NING_XIANG.WIK_ZT_SI_XI)) {
				
				GameSchedule.put(()->{
					handler_operate_card(table, _seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
				},2, TimeUnit.SECONDS);
				return;
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			}
		} else {
			if(table._player_result.biaoyan[_seat_index] == 1 || table.is_gang[_seat_index]){
				GameSchedule.put(()->{
					handler_player_out_card(table, _seat_index, _send_card_data);
				},1, TimeUnit.SECONDS);
				return;
			}
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}
	

	@Override
	public boolean handler_player_be_in_room(Table_NING_XIANG table, int seat_index) {
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
		
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		//起手胡重连
		//table.reconnectionQiShouHu();

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
		//起手胡重连
		if(table.reconnectionQiShouHu())
			table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
