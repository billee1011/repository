package com.cai.game.schcpdz.handler.dz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.schcpdz.handler.SCHCPDZHandlerDispatchCard;





/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class SCHCPHandlerDispatchCard_DZ extends SCHCPDZHandlerDispatchCard<SCHCPDZTable> {

	@Override
	public void exe(SCHCPDZTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

//		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		
		if (table.get_end()) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			// 显示胡牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
						table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

			}

//			table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		
	
		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		 table._last_card = _send_card_data;
		 
//		 _send_card_data = 0x0a;
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);

		//用户是否可以提扫
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断

	
		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()]; 
		for(int i = 0; i< table.getTablePlayerNumber();i++)
		{
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}
			
		int hu_pai = 0;
		int action_hu[] = new int[table.getTablePlayerNumber()];		
		int loop = 0;
		while(loop < table.getTablePlayerNumber()){
			int i = table.get_cur_index(_seat_index+loop) ; 
			loop ++;	
			if(table.get_is_kou_player(i))
				continue;
			PlayerStatus tempPlayerStatus = table._playerStatus[i]; 
			tempPlayerStatus.reset();
			action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
					table.GRR._weave_count[i],i,_seat_index,_send_card_data, chr[i], card_type,true);// 自摸
			if (action_hu[i] != GameConstants.WIK_NULL) {
				
				tempPlayerStatus.add_action(GameConstants.DZ_WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_send_card_data, i);
				table._playerStatus[i].add_action(GameConstants.DZ_WIK_NULL);
				table._playerStatus[i].add_pass(_send_card_data, _seat_index);
				hu_pai = 1;
			
			}
			else {
				chr[i].set_empty();
			}
			
			
		}	
		table._is_di_hu = true;
		

		// 玩家出牌 响应判断,是否有吃,碰,胡  swe  
		boolean bAroseAction = false;
		int action =  GameConstants.DZ_WIK_NULL;
		for(int i = 0; i< table.getTablePlayerNumber();i++){
			if(table.get_is_kou_player(i))
				continue;
			
			action |= table.estimate_player_gang(i, _send_card_data,false);
			action |= table.estimate_player_tu_huo_xiang(i, _send_card_data,false);
		}
		if(action ==  GameConstants.DZ_WIK_NULL)
			bAroseAction = table.estimate_player_chi(_seat_index, _send_card_data,true);
	

		table._table_cards[table._logic.switch_to_card_index(_send_card_data)]++;
		if (bAroseAction == false&&hu_pai == 0&&action == 0) {
	
				table.operate_player_action(_seat_index, true);

		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				curPlayerStatus = table._playerStatus[i];
				 if (table._playerStatus[i].has_action()) {
				    table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
				 //操作状态
				      table.operate_player_action(i, false);
				 }

			}
		}

		if (curPlayerStatus.has_action() ||bAroseAction == true || hu_pai == 1) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
			table.operate_player_status();
		} else {
			if (table.isTrutess(_seat_index)) {
				
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
	
					if(bAroseAction == false&&action == 0){
					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
//						table.operate_remove_discard(table._current_player, table.GRR._discard_count[table._current_player]);
					//没有人要就加入到牌堆
					int discard_time = 2000;
					int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
					SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
							.get(1104);
					if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
						discard_time = sysParamModel1104.getVal4();
					}
					
					if(table._last_card != 0)
					     table.exe_add_discard( _seat_index,  1, new int[]{table._last_card },true,discard_time);
					
					// 显示出牌
					int next_player = table.get_cur_index(_seat_index);
				
				
					table._current_player = next_player;
					_seat_index = next_player;

					//延时5秒发牌
					int dispatch_time = 3000;
					if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
						dispatch_time = sysParamModel1104.getVal5();
					}
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
				    table._last_card = _send_card_data;
				    table._last_player = table._current_player;
				    _send_card_data = 0;
					
				}
			}
		}

		return;
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
	public boolean handler_operate_card(SCHCPDZTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}

		
		if(table.is_operate_type( seat_index , operate_code, operate_card) == 0){
			table._playerStatus[seat_index].add_action(0);
			table._playerStatus[seat_index].add_pass(operate_card, seat_index);
			table._is_bao_zi[seat_index] = true;
			table.operate_player_xiang_gong_flag(seat_index, true);
			operate_code = GameConstants.DZ_WIK_NULL;
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.DZ_BAO_ZI }, 1,
					GameConstants.INVALID_SEAT);
		}
		if(table.is_game_end()==false)
			return true;

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		if(operate_code == GameConstants.WIK_NULL){
			if(playerStatus.has_zi_mo() == true)
				table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
		}
		else{
			table._guo_hu_pai_count[seat_index] = 0;
		}

		//if (seat_index != _seat_index) {
		//	table.log_error("DispatchCard 不是当前玩家操作");
		//	return false;
		//}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		if(operate_card != this._send_card_data && operate_code !=  GameConstants.CP_WIK_NULL)
		{
			table.log_player_error(seat_index,"DispatchCard 操作牌，与当前牌不一样 operate_card :"+operate_card+ "this._send_card_data= "+this._send_card_data);
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code =luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			return true;
		}
	
		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {	
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {

		case GameConstants.DZ_WIK_NULL:
		{
			// 用户状态
			for(int i = 0; i < table.getTablePlayerNumber();i++)
			{
				if(table._is_bao_zi[i] == true)
					continue;
				if(table._is_system_bao_zi[i] ==true)
					continue;
				if(table._is_yang[i] == true)
					continue;
				if(table.is_bao_zi(i)){
					table._is_system_bao_zi[i] = true;
					table.operate_player_xiang_gong_flag(i, true);
				}
			}
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
		
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				
				// 显示胡牌
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

				}
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {

				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
			
				int dispatch_time = 500;
				int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
				SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
						.get(1105);
				if (sysParamModel1105 != null && sysParamModel1105.getVal3() > 0 && sysParamModel1105.getVal3() < 10000) {
					dispatch_time = sysParamModel1105.getVal3();
				}

				table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
						table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}
				table.exe_add_discard(_seat_index, 1, new int[] { _send_card_data  }, true, 0);
				int next_player = table.get_cur_index(_seat_index);
				
				table._current_player = next_player;
				_seat_index = next_player;
				table._last_player = next_player;
				//没有人要就加入到牌堆
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
			    table._last_card = _send_card_data;
			}
			return true;

		}
		case GameConstants.DZ_WIK_DUI_LEFT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.DZ_WIK_DUI_LEFT);
			cbRemoveCard[1] = cbRemoveCard[0];
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			
			
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_SINGLE_LEFT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			cbRemoveCard[1] = target_card;
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.DZ_WIK_SINGLE_LEFT);
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			
			
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_DUI_RIGHT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.DZ_WIK_DUI_RIGHT);
			cbRemoveCard[1] = cbRemoveCard[0];
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
		
		
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_SINGLE_RIGHT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.DZ_WIK_SINGLE_RIGHT);
			cbRemoveCard[1] = target_card;
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
		
		
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_DUI_CENTER: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.DZ_WIK_DUI_CENTER);
			cbRemoveCard[1] = cbRemoveCard[0];
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
	 	
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_SINGLE_CENTER: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			cbRemoveCard[0] = table._logic.get_kind_card(target_card, GameConstants.DZ_WIK_SINGLE_CENTER);
			cbRemoveCard[1] = target_card;
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
	 	
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_OTHER:
		{
			// 删除扑克
			int cards[] = new int[3];
			int cards_count = table._logic.switch_to_value_to_card(7, cards);
			int cbRemoveCard[] = new int[2];
			int remove_count = 0 ;
			for(int i = 0; i< cards_count ;i++)
			{
				if(cards[i] == target_card )
					continue;
				cbRemoveCard[remove_count++] = cards[i];
			}
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(cbRemoveCard[i])]++;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
	 	
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
			
		}
		case GameConstants.DZ_WIK_PENG: // 碰牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = {target_card,target_card};
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			for(int i = 0; i< 2;i++)
			{
				table._table_cards[table._logic.switch_to_card_index(target_card)]++;
			}
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
			return true;
		}
		case GameConstants.DZ_WIK_SHA:
		case GameConstants.DZ_WIK_TUX_HUO:{
			table.exe_gang(target_player, _seat_index, _send_card_data, target_action,GameConstants.CHR_DZ_DISPATCH_CARD,true, true, false,1000);
			
			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_score_schcp(target_player, _seat_index, operate_card, true);
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			

			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);
			
			return true;
		}
		
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(SCHCPDZTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}

}
