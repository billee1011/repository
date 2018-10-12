package com.cai.game.xpbh.handler.bh;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.xpbh.XPBHTable;
import com.cai.game.xpbh.handler.BHHandlerDispatchCard;





/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class BHHandlerDispatchCard_XP extends BHHandlerDispatchCard<XPBHTable> {

	@Override
	public void exe(XPBHTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

//		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count <= 2) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			// 显示胡牌
			for (int i = 0; i <  table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
						table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

			}
			table._shang_zhuang_player = table._cur_banker;
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		//// 玩家出牌 响应判断,是否有提 暗龙
	
//			
//		table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
//				table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
//				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);
//
//		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
//		int ting_count = table._playerStatus[_seat_index]._hu_card_count;
//
//		if (ting_count > 0) {
//			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
//		} else {
//			ting_cards[0] = 0;
//			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
//		}
			
		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table._is_dispatch = 1;
//		if(table.DEBUG_CARDS_MODE) {
//			_send_card_data = 0x04;
//		}

		table._can_hu_pai_card = _send_card_data;
		--table.GRR._left_card_count;
		 table._last_card = _send_card_data;
		 ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()]; 
		for(int i = 0; i<  table.getTablePlayerNumber();i++)
		{
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}
		int bHupai = 0;
			
		
		int action_hu[] = new int[table.getTablePlayerNumber()];
		int action_pao[] = new int[table.getTablePlayerNumber()];
		int pao_type[][] = new int[table.getTablePlayerNumber()][1];
		//用户是否可以提扫
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int ti_sao = table.estimate_player_ti_wei_respond_phz_chd(_seat_index,_send_card_data);
		if(ti_sao != GameConstants.WIK_NULL){
			int hu_xi_chi[] = new int[1];
			PlayerStatus tempPlayerStatus = table._playerStatus[_seat_index]; 
			action_hu[_seat_index] = 	table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],_seat_index,_seat_index,_send_card_data, chr[_seat_index], card_type,hu_xi_chi,true);// 自摸
			if(table._is_xiang_gong[_seat_index] == true)
				action_hu[_seat_index] = GameConstants.WIK_NULL;
			if (action_hu[_seat_index] != GameConstants.WIK_NULL) {
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
				
				 if (tempPlayerStatus.has_action()) {
					 tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
				 //操作状态
				      table.operate_player_action(_seat_index, false);
				      return ;
				 }

//					GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), i, GameConstants.WIK_ZI_MO,_send_card_data),
//							600, TimeUnit.MILLISECONDS);
//					return ;

				bHupai = 1;
			
			}
			else {
				chr[_seat_index].set_empty();
			}
			
			if((ti_sao&GameConstants.WIK_BH_SHE)!=0)
			{
				if(ti_sao ==GameConstants.WIK_BH_SHE )
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,true);
				else
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
				table.exe_gang(_seat_index, _seat_index, _send_card_data, ti_sao, GameConstants.XPBH_TYPE_SHE, true, true, false, 1000);
				
			}
			else
			{
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
				table.exe_gang(_seat_index, _seat_index, _send_card_data, GameConstants.WIK_BH_ZHUA_LONG, GameConstants.XPBH_TYPE_ZHUA_LONG, true, true, false, 1000);
				
			}	
			return ;
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
			
		
		
		for(int i = 0; i< table.getTablePlayerNumber();i++){
			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;
			PlayerStatus tempPlayerStatus = table._playerStatus[i]; 
			tempPlayerStatus.reset();
//			action_hu[i] = table.estimate_player_hu_pai(i, _send_card_data, true);
			action_hu[i] = 	table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
					table.GRR._weave_count[i],i,_seat_index,_send_card_data, chr[i], card_type,hu_xi_chi,true);// 自摸
			action_pao[i] = table.estimate_player_respond_phz_chd(i,_seat_index ,_send_card_data, pao_type[i],true);
			if(table._is_xiang_gong[i] == true)
				action_hu[i] = GameConstants.WIK_NULL;
			if (action_hu[i] != GameConstants.WIK_NULL) {
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_send_card_data, i);

				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);

//					GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), i, GameConstants.WIK_ZI_MO,_send_card_data),
//							600, TimeUnit.MILLISECONDS);
//					return ;

				bHupai = 1;
			
			}
			else {
				chr[i].set_empty();
			}
			
		}	
		for(int i = 0; i< table.getTablePlayerNumber();i++){  
		 if((action_pao[i] != GameConstants.WIK_NULL)&& (bHupai == 0)){
			 	ti_sao = action_pao[i];
			 	table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
				table.exe_gang(i, _seat_index, _send_card_data, action_pao[i],pao_type[i][0],true, true, false,1000);
				return ;
		 	}
		}
		
		

		// 玩家出牌 响应判断,是否有吃,碰,胡  swe  
		boolean bAroseAction = false;
		if(ti_sao ==  GameConstants.WIK_NULL&&bHupai == 0){				
				bAroseAction = table.estimate_player_out_card_respond_hh(_seat_index, _send_card_data,true);
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
		}

		
		if ((bAroseAction == false)&&(bHupai == GameConstants.WIK_NULL)) {
	
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

		if (curPlayerStatus.has_action() ) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
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
			} else if(ti_sao == GameConstants.WIK_NULL&&bHupai == 0) {
						if(bAroseAction == false){
							
							if(table._logic.is_yao_card(_send_card_data)){
								table._yao_weave_item[_seat_index].weave_kind = GameConstants.WIK_BH_LUO_YAO;
								table._yao_weave_item[_seat_index].center_card = _send_card_data;
								table._yao_weave_item[_seat_index].weave_card[table._yao_weave_item[_seat_index].weave_card_count++] = _send_card_data;
								table._yao_weave_item[_seat_index].hu_xi = table._yao_weave_item[_seat_index].weave_card_count;
								table.exe_chi_peng(_seat_index, _seat_index, GameConstants.WIK_BH_LUO_YAO, _send_card_data,
										GameConstants.CHI_PENG_TYPE_DISPATCH,-1);
								return ;
							}
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
						int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
						//过张的牌都不可以
						table._cannot_chi[_seat_index][table._logic.switch_to_card_index(_send_card_data)] ++;
						table._cannot_chi[next_player][table._logic.switch_to_card_index(_send_card_data)] ++;
					
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
	public boolean handler_operate_card(XPBHTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
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
		if(operate_card != this._send_card_data)
		{
			table.log_player_error(seat_index,"DispatchCard 操作牌，与当前牌不一样");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		
		playerStatus.clean_status();

	
		//吃操作后，是否有落
		
		if(luoCode != -1)
			playerStatus.set_lou_pai_kind(luoCode);
		



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
		int cbMaxActionRand = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table.GRR._weave_items[i],table.GRR._weave_count[i],table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table.GRR._weave_items[i],table.GRR._weave_count[i],table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table.GRR._weave_items[target_player],table.GRR._weave_count[target_player],table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(table.GRR._weave_items[target_player],table.GRR._weave_count[target_player],
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
					cbMaxActionRand = cbUserActionRank;
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
		int eat_type = GameConstants.WIK_BH_CHI_L|GameConstants.WIK_BH_CHI_C|GameConstants.WIK_BH_CHI_R|GameConstants.WIK_BH_CHI_H
				|GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119
				|GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228;
		int same_type = GameConstants.WIK_BH_PENG|GameConstants.WIK_BH_SHE|GameConstants.WIK_BH_KAIZ|GameConstants.WIK_BH_DAGUN
				|GameConstants.WIK_BH_ZHUA_LONG;
		int heng_type = GameConstants.WIK_BH_CHI_H
				|GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119
				|GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228;
		int shang_type = GameConstants.WIK_BH_SHANG|GameConstants.WIK_BH_XIA|GameConstants.WIK_BH_SHANG_TWO|GameConstants.WIK_BH_XIA_TWO;
		if(target_action == GameConstants.WIK_NULL)
		{
			
			// 显示出牌
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			//过张的牌都不可以吃
			table._cannot_chi[_seat_index][table._logic.switch_to_card_index(_send_card_data)] ++;
			table._cannot_chi[next_player][table._logic.switch_to_card_index(_send_card_data)] ++;
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if((table._playerStatus[i]._action[j]&GameConstants.WIK_BH_PENG)!=0)
					{
						table._cannot_peng[i][table._logic.switch_to_card_index(_send_card_data)] ++;
					}
				}
			}
		}
		else if((target_action&(eat_type|shang_type))!=0)
		{	
			
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if((table._playerStatus[i]._action[j]&GameConstants.WIK_BH_PENG)!=0)
					{
						table._cannot_peng[i][table._logic.switch_to_card_index(_send_card_data)] ++;
					}
				}
			}
			if(_seat_index != target_player)
				table._cannot_chi[_seat_index][table._logic.switch_to_card_index(_send_card_data)] ++;
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
          
			
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		
		// 执行动作
		if( GameConstants.WIK_NULL == target_action)
		{
			
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				// 显示胡牌
				for (int i = 0; i <  table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

				}
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				for(int i = 0; i<  table.getTablePlayerNumber();i++){
					int pao_type[] =  new int[1];
					int action = table.estimate_player_respond_phz_chd(i,_seat_index ,_send_card_data, pao_type,true);	
					if(action != GameConstants.WIK_NULL){
						
						table.exe_gang(i, _seat_index, _send_card_data, action,pao_type[0],true, true, false,1000);
						return true;
					}
				}
				if(table._logic.is_yao_card(_send_card_data)){
					table._yao_weave_item[_seat_index].weave_kind = GameConstants.WIK_BH_LUO_YAO;
					table._yao_weave_item[_seat_index].center_card = _send_card_data;
					table._yao_weave_item[_seat_index].weave_card[table._yao_weave_item[_seat_index].weave_card_count++] = _send_card_data;
					table._yao_weave_item[_seat_index].hu_xi = table._yao_weave_item[_seat_index].weave_card_count;
					table.exe_chi_peng(_seat_index, _seat_index, GameConstants.WIK_BH_LUO_YAO, _send_card_data,
							GameConstants.CHI_PENG_TYPE_DISPATCH,-1);
					return true;
				}
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
				//要出牌，但是没有牌出设置成相公  下家用户发牌
				int pai_count =0;
				for(int i = 0; i<GameConstants.XPBH_MAX_INDEX ;i++) {
			       	if(table.GRR._cards_index[_seat_index][i]<3)
			       		pai_count += table.GRR._cards_index[_seat_index][i];
				 }
				
				if(pai_count == 0)
				{
					table._is_xiang_gong[_seat_index] = true;	 	
		        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
		        	int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;
					
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					return true;
				}
				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}
				table.exe_add_discard(_seat_index, 1, new int[] { _send_card_data  }, true, 0);
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				
				table._current_player = next_player;
				_seat_index = next_player;
				table._last_player = next_player;
				//没有人要就加入到牌堆
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 0);
			    table._last_card = _send_card_data;
			}
			return true;

		}
		else if((target_action&GameConstants.WIK_BH_DAGUN)!=0 || (target_action&GameConstants.WIK_BH_KAIZ)!=0)
		{
			int pao_type[] =  new int[1];
			int action = table.estimate_player_respond_phz_chd(target_player,_seat_index ,_send_card_data, pao_type,true);	
			if(action != GameConstants.WIK_NULL){
				table.exe_gang(target_player, _seat_index, _send_card_data, action,pao_type[0],true, true, false,1000);
			}

			return true;

		}
		else if((GameConstants.WIK_BH_PENG&target_action)!=0)
		{
			
			
			int index = target_action&(0xF);
			if(index == 0){
				int cbRemoveCard[] = {target_card,target_card};
				if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
					table.log_player_error(target_player, "碰牌删除出错");
					return false;
				}
				int wIndex = table.GRR._weave_count[target_player]++;
				table.GRR._weave_items[target_player][wIndex].public_card = 1;
				table.GRR._weave_items[target_player][wIndex].center_card = target_card;
				table.GRR._weave_items[target_player][wIndex].weave_kind |= target_action;
				table.GRR._weave_items[target_player][wIndex].weave_card[table.GRR._weave_items[target_player][wIndex].weave_card_count++] = target_card|GameConstants.WIK_BH_PENG;
				table.GRR._weave_items[target_player][wIndex].weave_score = target_action;
				for(int j = table.GRR._weave_items[target_player][wIndex].weave_card_count; j<table.GRR._weave_items[target_player][wIndex].weave_card_count+2;j++){
					table.GRR._weave_items[target_player][wIndex].weave_card[j] = target_card|GameConstants.WIK_BH_PENG;
				}
				table.GRR._weave_items[target_player][wIndex].weave_card_count += 2;
				table.GRR._weave_items[target_player][wIndex].provide_player = _seat_index;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], table.GRR._weave_count[target_player]-1, table.GRR._weave_count[target_player]);
				
				table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
						GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
				return true;
			}
			else{
					index --;
					int cbRemoveCard[] = new int[2];
					cbRemoveCard[0] = target_card;
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 1)) {
						table.log_player_error(target_player, "碰牌删除出错");
						return false;
					}
					table.GRR._weave_items[target_player][index].weave_kind |= GameConstants.WIK_BH_PENG;
					table.GRR._weave_items[target_player][index].provide_player = _seat_index;
					table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card|GameConstants.WIK_BH_PENG;
					table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card|GameConstants.WIK_BH_PENG;
					table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], index, table.GRR._weave_count[target_player]);
					
					table.exe_chi_peng(target_player, _seat_index, GameConstants.WIK_BH_PENG, target_card,
							GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
					return true;
				}
				
		}
		
		else if((target_action & eat_type)!=0) // 上牌操作
		{
				int weave_sub_index[] = new int[table.GRR._weave_count[target_player]];
				for(int i = 0;i<table.GRR._weave_count[target_player];i++)
					weave_sub_index[i] = i;
			    int index = target_action&(0xF);
				if(index == 0){
					// 删除扑克
					int weave_card[] = new int[3];
					int weave_card_count = table._logic.get_kind_card(target_action, target_card, weave_card);
					if((target_action & heng_type) != 0)
					{
						int remove_card[] = new int[3];
						int remove_count = 0;
						int card_action[] = new int[3];
						int card_count[] = new int[3];
						int card_data[] = new int[3];
						int two_delete_card[] = new int[1];
						int target_card_index = -1;
						for(int i = 0; i< weave_card_count;i++)
						{
							if(table.GRR._cards_index[target_player][table._logic.switch_to_card_index(weave_card[i])] == 0){
								boolean flag = false;
								for(int j = 0; j < table._yao_weave_item[target_player].weave_card_count;j++){
									if(table._yao_weave_item[target_player].weave_card[j] == weave_card[i])
									{
										flag = true;
										break;
									}
								}
								if(flag == false)
									remove_card[remove_count++] = weave_card[i];
							}
							else 
								two_delete_card[0]  = weave_card[i];
					
						}
						if(remove_count >= 2)
						{
							int weave_count = 0;
							int user_card_count = 0;
							
							for(int i = 0; i<table.GRR._weave_count[target_player];i++){
								if((table.GRR._weave_items[target_player][i].weave_kind&same_type)!=0){
									boolean is_index = false;
									for(int k = 0; k<remove_count;k++){
										if((table.GRR._weave_items[target_player][i].weave_card[0]&0xff)==remove_card[k])
										{
											is_index = true;
											break;
										}
									}
									
									
									if(is_index == true){
										if((table.GRR._weave_items[target_player][i].weave_card[0]&0xff) == target_card )
											target_card_index = user_card_count;
										card_action[user_card_count] = table.GRR._weave_items[target_player][i].weave_kind;
										card_count[user_card_count] = table.GRR._weave_items[target_player][i].weave_card_count;
										card_data[user_card_count++] = table.GRR._weave_items[target_player][i].weave_card[0];
										table.GRR._weave_items[target_player][i].weave_card_count = 0;
										table.GRR._weave_items[target_player][i].hu_xi = 0;
										table.GRR._weave_items[target_player][i].weave_kind = 0;
										weave_sub_index[i] = i; 
									}
									else {
										if(weave_count!=i){
											table.GRR._weave_items[target_player][weave_count].center_card = table.GRR._weave_items[target_player][i].center_card;
											table.GRR._weave_items[target_player][weave_count].weave_card_count = table.GRR._weave_items[target_player][i].weave_card_count;
											for (int j = 0; j < table.GRR._weave_items[target_player][weave_count].weave_card_count; j++) {
												table.GRR._weave_items[target_player][weave_count].weave_card[j] = table.GRR._weave_items[target_player][i].weave_card[j];
											}
											table.GRR._weave_items[target_player][weave_count].weave_kind = table.GRR._weave_items[target_player][i].weave_kind;
											table.GRR._weave_items[target_player][i].weave_card_count = 0;
											table.GRR._weave_items[target_player][i].weave_kind = 0;
											table.GRR._weave_items[target_player][i].hu_xi = 0;
											weave_sub_index[i] = weave_count; 
										}
										weave_count++;
									}
					
									
								} else {
									if(weave_count!=i){
										table.GRR._weave_items[target_player][weave_count].center_card = table.GRR._weave_items[target_player][i].center_card;
										table.GRR._weave_items[target_player][weave_count].weave_card_count = table.GRR._weave_items[target_player][i].weave_card_count;
										
										for (int j = 0; j < table.GRR._weave_items[target_player][weave_count].weave_card_count; j++) {
											table.GRR._weave_items[target_player][weave_count].weave_card[j] = table.GRR._weave_items[target_player][i].weave_card[j];
										}
										table.GRR._weave_items[target_player][weave_count].weave_kind = table.GRR._weave_items[target_player][i].weave_kind;
										table.GRR._weave_items[target_player][i].weave_card_count = 0;
										table.GRR._weave_items[target_player][i].weave_kind = 0;
										table.GRR._weave_items[target_player][i].hu_xi = 0;
										weave_sub_index[i] = weave_count; 
									}
									weave_count++;
								}
							}
							if(weave_count + user_card_count != table.GRR._weave_count[target_player]){
								table.log_player_error(target_player, "吃牌删除出错");
								return false;
							}
							boolean is_delete = true;
							if(two_delete_card[0] != 0&&two_delete_card[0] != target_card){
								
								if(table._logic.get_card(two_delete_card[0], table._yao_weave_item[target_player].weave_card, table._yao_weave_item[target_player].weave_card_count)){
									int yao_count = 0;
									boolean flag = false;
									for(int i = 0; i<table._yao_weave_item[target_player].weave_card_count;i++){
										if(table._yao_weave_item[target_player].weave_card[i] != two_delete_card[0]|| flag == true)
											table._yao_weave_item[target_player].weave_card[yao_count++] = table._yao_weave_item[target_player].weave_card[i];
										else 
											flag = true;
									}
									if(flag == false)
									{
										table.log_player_error(target_player, "吃牌删除出错");
											return false;
										
									}
									table._yao_weave_item[target_player].weave_card_count = yao_count;
								}
						    	else{
						    		is_delete = false;
						    		if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], two_delete_card, 1)) {
										table.log_player_error(target_player, "碰牌删除出错");
										return false;
						    		}
										
						    	}
								if(two_delete_card[0] != target_card){
									is_delete = false;
								}
							}
							else if(remove_count == 3){
									is_delete = false;
								
							}
							table.GRR._weave_count[target_player] -= user_card_count;
							int wIndex = table.GRR._weave_count[target_player]++;
							table.GRR._weave_items[target_player][wIndex].public_card = 1;
							table.GRR._weave_items[target_player][wIndex].center_card = target_card;
							table.GRR._weave_items[target_player][wIndex].weave_kind = target_action|card_action[0]|card_action[1]|card_action[2];
							
							table.GRR._weave_items[target_player][wIndex].weave_card_count = 0;
							if(target_card_index != -1 ){
								for(int j = table.GRR._weave_items[target_player][wIndex].weave_card_count; j<table.GRR._weave_items[target_player][wIndex].weave_card_count+card_count[target_card_index];j++){
									if(j == 0)
										table.GRR._weave_items[target_player][wIndex].weave_card[j] = target_card;
									else
										table.GRR._weave_items[target_player][wIndex].weave_card[j] = target_card|card_action[target_card_index];
								}
								table.GRR._weave_items[target_player][wIndex].weave_card_count += card_count[target_card_index];
								if( two_delete_card[0] != 0)
									table.GRR._weave_items[target_player][wIndex].weave_card[table.GRR._weave_items[target_player][wIndex].weave_card_count++] = two_delete_card[0];
								
							}
							else{
								table.GRR._weave_items[target_player][wIndex].weave_card[table.GRR._weave_items[target_player][wIndex].weave_card_count++] = target_card;
							}
							for(int i = 0; i< remove_count;i++){
								if( (card_data[i]&0xff) == target_card)
									continue;
								for(int j = table.GRR._weave_items[target_player][wIndex].weave_card_count; j<table.GRR._weave_items[target_player][wIndex].weave_card_count+card_count[i];j++){
									if(j == table.GRR._weave_items[target_player][wIndex].weave_card_count)
										table.GRR._weave_items[target_player][wIndex].weave_card[j] = card_data[i]&0xff;
									else
										table.GRR._weave_items[target_player][wIndex].weave_card[j] = card_data[i];
								}
								table.GRR._weave_items[target_player][wIndex].weave_card_count += card_count[i];
							}
							table.GRR._weave_items[target_player][wIndex].provide_player = _seat_index;
							table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], table.GRR._weave_count[target_player]-1, table.GRR._weave_count[target_player]);
							table.set_xian_ming_zhao(target_player, table.GRR._weave_items[target_player][wIndex].weave_kind, table.GRR._weave_items[target_player][wIndex].weave_card,table.GRR._weave_items[target_player][wIndex].weave_card_count );
							table.add_lou_weave(target_lou_code,target_player,target_card,_seat_index,target_action,is_delete,weave_sub_index);
							
							table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
									GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
							return true;
						}
					}
					
					int cbRemoveCard[] = new int[3];
					int dis_weave_card[] = new int[3];
					int count = 0;
 					int dis_count = 0; 
 					for(int j = 0; j <weave_card_count ;j ++){
						if(weave_card[j] != target_card)
						{
							if(table.GRR._cards_index[target_player][table._logic.switch_to_card_index(weave_card[j])] == 0){
								if(table._logic.get_card(weave_card[j], table._yao_weave_item[target_player].weave_card, table._yao_weave_item[target_player].weave_card_count)){
									int yao_count = 0;
									boolean flag = false;
 									for(int i = 0; i<table._yao_weave_item[target_player].weave_card_count;i++){
										if(table._yao_weave_item[target_player].weave_card[i] != weave_card[j]|| flag == true)
											table._yao_weave_item[target_player].weave_card[yao_count++] = table._yao_weave_item[target_player].weave_card[i];
										else 
  											flag = true;
									}
									if(flag == false)
									{
										table.log_player_error(target_player, "吃牌删除出错");
											return false;
										
									}
									table._yao_weave_item[target_player].weave_card_count = yao_count;
								}
								
									
							}
							else
								cbRemoveCard[count++] = weave_card[j]; 
							dis_weave_card[dis_count++] = weave_card[j]; 
						}
							
					}
						
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, count)) {
						table.log_player_error(target_player, "吃牌删除出错");
						return false;
					}
					int wIndex = table.GRR._weave_count[target_player]++;
					table.GRR._weave_items[target_player][wIndex].public_card = 1;
					table.GRR._weave_items[target_player][wIndex].center_card = target_card;
					table.GRR._weave_items[target_player][wIndex].weave_kind |= target_action;
					table.GRR._weave_items[target_player][wIndex].weave_card[table.GRR._weave_items[target_player][wIndex].weave_card_count++] = target_card;
					for(int j = table.GRR._weave_items[target_player][wIndex].weave_card_count; j<table.GRR._weave_items[target_player][wIndex].weave_card_count+2;j++){
						table.GRR._weave_items[target_player][wIndex].weave_card[j] = dis_weave_card[j-table.GRR._weave_items[target_player][wIndex].weave_card_count];
					}
					table.GRR._weave_items[target_player][wIndex].weave_card_count += 2;
					table.GRR._weave_items[target_player][wIndex].provide_player = _seat_index;
					table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], table.GRR._weave_count[target_player]-1, table.GRR._weave_count[target_player]);
					table.set_xian_ming_zhao(target_player, table.GRR._weave_items[target_player][wIndex].weave_kind, table.GRR._weave_items[target_player][wIndex].weave_card
							,table.GRR._weave_items[target_player][wIndex].weave_card_count);
					table.add_lou_weave(target_lou_code,target_player,target_card,_seat_index,target_action,true,weave_sub_index);
					
					table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
							GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
				
					return true;
				}
				else{
					index --;
					int cbRemoveCard[] = new int[3];
					int chi_type = target_action - (index+1);
				    int remove_count = table._logic.get_must_kind_card(chi_type,table.GRR._weave_items[target_player][index].center_card,operate_card,cbRemoveCard);
				    for(int j = 0; j<remove_count;j++){
				    	if(table._logic.get_card(cbRemoveCard[j], table._yao_weave_item[target_player].weave_card, table._yao_weave_item[target_player].weave_card_count)){
							int yao_count = 0;
							boolean flag = false;
							for(int i = 0; i<table._yao_weave_item[target_player].weave_card_count;i++){
								if(table._yao_weave_item[target_player].weave_card[i] != cbRemoveCard[j]|| flag == true)
									table._yao_weave_item[target_player].weave_card[yao_count++] = table._yao_weave_item[target_player].weave_card[i];
								else 
									flag = true;
							}
							if(flag == false)
							{
								table.log_player_error(target_player, "吃牌删除出错");
									return false;
								
							}
							table._yao_weave_item[target_player].weave_card_count = yao_count;
						}
				    	else{
				    		int temp_remove[] = new int[1];
				    		temp_remove[0] = cbRemoveCard[j];
				    		if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], temp_remove, 1)) {
								table.log_player_error(target_player, "碰牌删除出错");
								return false;
				    		}
								
				    	}
				    }
				      
				 
				    int kind_count = table.GRR._weave_items[target_player][index].weave_card_count-1;
				    int kind_card [] = new int[kind_count];
				    for(int i = 0; i< kind_count; i++){
				    	kind_card[i] = table.GRR._weave_items[target_player][index].weave_card[i];
				    }
				    table.GRR._weave_items[target_player][index].weave_card_count = 0;
					table.GRR._weave_items[target_player][index].weave_kind |= chi_type;
					table.GRR._weave_items[target_player][index].provide_player = _seat_index;
					boolean is_delete = true;
					if(target_card == (kind_card[0]&0xff)){
						table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = kind_card[0]&0xff;
						for(int i =table.GRR._weave_items[target_player][index].weave_card_count;i < table.GRR._weave_items[target_player][index].weave_card_count+kind_count;i++) 	
							table.GRR._weave_items[target_player][index].weave_card[i] = kind_card[i- table.GRR._weave_items[target_player][index].weave_card_count] ;
						table.GRR._weave_items[target_player][index].weave_card_count += kind_count;
						table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = cbRemoveCard[0];
						table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = cbRemoveCard[1];
						is_delete = false;
					}else{
						table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card;
						table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = cbRemoveCard[0];
						table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = kind_card[0]&0xff;
						for(int i =table.GRR._weave_items[target_player][index].weave_card_count;i < table.GRR._weave_items[target_player][index].weave_card_count+kind_count;i++) 	
							table.GRR._weave_items[target_player][index].weave_card[i] = kind_card[i- table.GRR._weave_items[target_player][index].weave_card_count] ;
						table.GRR._weave_items[target_player][index].weave_card_count += kind_count;
					}
					
					
					table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], index, table.GRR._weave_count[target_player]);
					table.set_xian_ming_zhao(target_player, table.GRR._weave_items[target_player][index].weave_kind, table.GRR._weave_items[target_player][index].weave_card,
							table.GRR._weave_items[target_player][index].weave_card_count);
					table.add_lou_weave(target_lou_code,target_player,target_card,_seat_index,target_action,is_delete,weave_sub_index);
						
					table.exe_chi_peng(target_player, _seat_index, chi_type, target_card,
								GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
					
					
					return true;
				}
		}else if((target_action&GameConstants.WIK_BH_SHANG)!= 0 ||
			(target_action&GameConstants.WIK_BH_XIA)!= 0){
			int index = target_action&(0xF);
			if(index == 0)
				return false;
			index --;
			table.GRR._weave_items[target_player][index].weave_kind |= target_action - (index+1);
			table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card;
			table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], index, table.GRR._weave_count[target_player]);
			
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
		}else if((target_action&GameConstants.WIK_BH_SHANG_TWO)!= 0 ||
			(target_action&GameConstants.WIK_BH_XIA_TWO)!= 0){
			int index = target_action&(0xF);
			if(index == 0)
				return false;
			index --;
			int remove_card[] = new int[1];
			if((target_action&GameConstants.WIK_BH_SHANG_TWO)!= 0)
				remove_card[0] = target_card-1;
			else
				remove_card[0] = target_card+1;
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], remove_card, 1)) {
				table.log_player_error(target_player, "上牌删除出错");
				return false;
    		}
			if((target_action&GameConstants.WIK_BH_SHANG_TWO)!= 0 )
			table.GRR._weave_items[target_player][index].weave_kind |= target_action - (index+1);
			table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card;
			table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++]  = remove_card[0];
			table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], index, table.GRR._weave_count[target_player]);
			
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
		}
		else if((target_action&GameConstants.WIK_ZI_MO)!=0){// 自摸
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;


			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_bh(target_player, _seat_index, operate_card, true);
			if(table._cur_banker != target_player)
				table._cur_banker = (table._cur_banker+1)%table.getTablePlayerNumber();
			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}
			table.countChiHuTimes(target_player, true,_seat_index);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}


		return true;
	}

	@Override
	public boolean handler_player_be_in_room(XPBHTable table, int seat_index) {
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
