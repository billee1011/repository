package com.cai.game.xpbh.handler.bh;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.game.xpbh.XPBHTable;
import com.cai.game.xpbh.handler.BHHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class BHHandlerOutCardOperate_XP extends BHHandlerOutCardOperate<XPBHTable> {
	@Override
	public void exe(XPBHTable table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		table._playerStatus[i].clean_action();
		table._playerStatus[i].clean_status();
		
	}
		if(table.GRR._cards_index[_out_card_player][table._logic.switch_to_card_index(_out_card_data)] >= 3)
		{
			table.log_info(_out_card_player+"出牌出错 HHHandlerOutCardOperate_YX "+_out_card_data);
			return ;
		}
		//
		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		table._last_card = _out_card_data;
		table._cannot_chi[_out_card_player][table._logic.switch_to_card_index(_out_card_data)] ++;
		table._can_hu_pai_card = _out_card_data;
		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;
		table._is_dispatch = 2;
		// 刷新手牌
		int cards[] = new int[GameConstants.XPBH_MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		table.operate_player_cards(_out_card_player, hand_card_count, cards, table.GRR._weave_count[_out_card_player],table.GRR._weave_items[_out_card_player]);

		// 显示出牌
		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);

		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}
		int bHupai = 0;

		int card_type = GameConstants.HU_CARD_TYPE_PAOHU;
		int action_hu[] = new int[table.getTablePlayerNumber()];
		int action_pao[] = new int[table.getTablePlayerNumber()];
		int pao_type[][] = new int[table.getTablePlayerNumber()][1];

		int loop = 0;
		int ti_pao = GameConstants.WIK_NULL ;
		while (loop < table.getTablePlayerNumber()) {
			int i = (_out_card_player + loop) % table.getTablePlayerNumber();
			loop++;
			if (i == _out_card_player)
				continue;
			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;
			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();
//			action_hu[i] = table.estimate_player_hu_pai(i, _out_card_data, false);
			action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i, _out_card_player,
					_out_card_data, chr[i], card_type, hu_xi_chi, false);// 自摸
			action_pao[i] = table.estimate_player_respond_phz_chd(i, _out_card_player, _out_card_data, pao_type[i], false);
			
			if (table._is_xiang_gong[i] == true)
				action_hu[i] = GameConstants.WIK_NULL;
			if (action_hu[i] != GameConstants.WIK_NULL) {
				tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
				tempPlayerStatus.add_chi_hu(_out_card_data, i);
//				if (table.has_rule(GameConstants.GAME_RULE_QIANG_HU_CHD) ) {
//					int hucard_time = 600;
//					int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
//					SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
//					if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
//						hucard_time = sysParamModel1105.getVal4();
//					}
////
//					GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), i, GameConstants.WIK_CHI_HU, _out_card_data), hucard_time, TimeUnit.MILLISECONDS);
//					return;
//				}

				bHupai = 1;

			} else {
				chr[i].set_empty();
			}

		}
//		table._provide_player = _out_card_player;
//		table._provide_card = _out_card_data;
//		table._playerStatus[_out_card_player]._hu_card_count = table.get_hh_ting_card_twenty(
//				table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
//				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],_out_card_player,_out_card_player);
//
//		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
//		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;
//
//		if (ting_count > 0) {
//			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
//		} else {
//			ting_cards[0] = 0;
//			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
//		}
		//// 玩家出牌 响应判断,是否有跑 ，提， 扫   swe 
		//int pao_type[] = new int[1];
		
	
	    ti_pao =  GameConstants.WIK_NULL;
		for(int i = 0; i<table.getTablePlayerNumber();i++)
		{
			if(i==_out_card_player) continue;
			int temp_pao_type[] = new int[1];
			ti_pao = table.estimate_player_respond_phz_chd(i,_out_card_player ,_out_card_data, temp_pao_type,false);
			if(ti_pao != GameConstants.WIK_NULL&&bHupai == 0){
				table.exe_gang(i, _out_card_player, _out_card_data, ti_pao,temp_pao_type[0], false,true, false,1000);
				return ;
			}
			

		}

		
		
		

		// 玩家出牌 响应判断,是否有吃,碰,胡  swe  
		boolean bAroseAction = false;
		if(ti_pao == GameConstants.WIK_BH_NULL && bHupai == 0)
			bAroseAction = table.estimate_player_out_card_respond_hh(_out_card_player,_out_card_data,false);// ,
		
																													// EstimatKind.EstimatKind_OutCar
		
		// 如果没有需要操作的玩家，派发扑克
		
		if (bAroseAction == false&&bHupai == 0&&ti_pao == GameConstants.WIK_NULL ) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
			}
			
			table.operate_player_action(_out_card_player, true);
			table._cannot_chi[next_player][table._logic.switch_to_card_index(_out_card_data)] ++;
			//
			table.operate_out_card(_out_card_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
			// 加入牌队 客户端250毫秒没有收到加入牌堆。自己会添加一张牌到牌队
			int discard_time = 2000;
			int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1105);
			if (sysParamModel1105 != null && sysParamModel1105.getVal1() > 0 && sysParamModel1105.getVal1() < 10000) {
				discard_time = sysParamModel1105.getVal1();
			}
			table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, true,discard_time);

			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			_out_card_data = 0;
			table._last_player = _current_player;
			int dispatch_time = 3000;
			if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
				dispatch_time = sysParamModel1105.getVal2();
			}
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				 if (table._playerStatus[i].has_action()) {
				    table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
				 //操作状态
				      table.operate_player_action(i, false);
				 }

			}
		}
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	
	public boolean handler_operate_card(XPBHTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		// 效验状态

		PlayerStatus playerStatus = table._playerStatus[seat_index];

		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HHHandlerOutCardOperate_YX 出牌,玩家操作已失效");
			return true;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "HHHandlerOutCardOperate_YX 出牌,玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			table.log_player_error(seat_index, "HHHandlerOutCardOperate_YX 出牌操作,没有动作");
			return true;
		}
		if((operate_card != table._out_card_data))
		{
			table.log_player_error(seat_index,"HHHandlerOutCardOperate_YX 操作牌，与当前牌不一样");
			return true;
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);
		
		
		//吃操作后，是否有落
		
		if(luoCode != -1)
			playerStatus.set_lou_pai_kind(luoCode);
	
		
		
//		if (operate_code == GameConstants.WIK_CHI_HU) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
//			// 效果
//			// table.process_chi_hu_player_operate_cs(seat_index,operate_card,1,false);
//
//		} else if (operate_code == GameConstants.WIK_NULL) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
//			if (table._playerStatus[seat_index].has_chi_hu()) {
//				table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
//			}
//		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		int cbMaxActionRand = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
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
		//判断可不可以吃的上家用户
		if(target_action == GameConstants.WIK_NULL)
		{
			//过张的牌都不可以吃
			int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._cannot_chi[next_player][table._logic.switch_to_card_index(_out_card_data)] ++;
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if((table._playerStatus[i]._action[j]&GameConstants.WIK_BH_PENG)!=0)
					{
						table._cannot_peng[i][table._logic.switch_to_card_index(_out_card_data)] ++;
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
					if((table._playerStatus[i]._action[j] & GameConstants.WIK_BH_PENG)!=0)
					{
						table._cannot_peng[i][table._logic.switch_to_card_index(_out_card_data)] ++;
					}
				}
			}
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
	
		// 删除扑克
		if(GameConstants.WIK_NULL == target_action){

			// 加到牌堆 没有人要
			// 显示出牌
			table._is_di_hu = false;
			table.operate_out_card(_out_card_player, 0,null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
			table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, true, 0);

			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			

			// 发牌
			table._last_player = _current_player;
			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 500);
				
			_out_card_data = 0;

			return true;
		}
		else if((target_action&GameConstants.WIK_BH_DAGUN)!=0 || (target_action&GameConstants.WIK_BH_KAIZ)!=0)
		{
			int pao_type[] =  new int[1];
			int action = table.estimate_player_respond_phz_chd(target_player,_out_card_player, _out_card_data, pao_type,true);	
			if(action != GameConstants.WIK_NULL){
				table.exe_gang(target_player, _out_card_player, _out_card_data, action,pao_type[0],true, true, false,1000);
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
				for(int j = table.GRR._weave_items[target_player][wIndex].weave_card_count; j<table.GRR._weave_items[target_player][wIndex].weave_card_count+2;j++){
					table.GRR._weave_items[target_player][wIndex].weave_card[j] = target_card|GameConstants.WIK_BH_PENG;
				}
				table.GRR._weave_items[target_player][wIndex].weave_card_count += 2;
				table.GRR._weave_items[target_player][wIndex].provide_player = _out_card_player;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], table.GRR._weave_count[target_player]-1, table.GRR._weave_count[target_player]);
				
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
						GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
				return true;
			}
			else{
				index --;
				int cbRemoveCard[] = new int[4];
				int kind_type = table.GRR._weave_items[target_player][index].weave_kind & eat_type;
				if(kind_type != 0)
				{	int count = 0;
					cbRemoveCard[count++] = target_card;
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard,count)) {
						table.log_player_error(target_player, "碰牌删除出错");
						return false;
					}
				
					table.GRR._weave_items[target_player][index].provide_player = _out_card_player;
					table.GRR._weave_items[target_player][index].weave_kind |= target_action - (index+1);
					table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card|GameConstants.WIK_BH_PENG;
					table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card|GameConstants.WIK_BH_PENG;
					table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], index, table.GRR._weave_count[target_player]);
					
					table.exe_chi_peng(target_player, _out_card_player, GameConstants.WIK_BH_PENG, target_card,
							GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
					return true; 
				}
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
				int weave_card[] = new int[4];
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
						if(table.GRR._cards_index[target_player][table._logic.switch_to_card_index(weave_card[i])] == 0)
						{
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
									table.GRR._weave_items[target_player][i].weave_card_count = 0;
									for (int j = 0; j < table.GRR._weave_items[target_player][weave_count].weave_card_count; j++) {
										table.GRR._weave_items[target_player][weave_count].weave_card[j] = table.GRR._weave_items[target_player][i].weave_card[j];
									}
									table.GRR._weave_items[target_player][weave_count].weave_kind = table.GRR._weave_items[target_player][i].weave_kind;
									table.GRR._weave_items[target_player][i].weave_card_count = 0;
									table.GRR._weave_items[target_player][i].weave_kind = 0;
									table.GRR._weave_items[target_player][i].hu_xi = 0;
									weave_sub_index[i] = user_card_count;
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
							{
								table.GRR._weave_items[target_player][wIndex].weave_card[table.GRR._weave_items[target_player][wIndex].weave_card_count++] = two_delete_card[0];
								is_delete = false;
							}
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
							table.GRR._weave_items[target_player][wIndex].provide_player = _out_card_player;
						table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], table.GRR._weave_count[target_player]-1, table.GRR._weave_count[target_player]);
						table.set_xian_ming_zhao(target_player, table.GRR._weave_items[target_player][wIndex].weave_kind, table.GRR._weave_items[target_player][wIndex].weave_card,
								 table.GRR._weave_items[target_player][wIndex].weave_card_count);
						table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,target_action,is_delete,weave_sub_index);
						
						table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
								GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
						return true;
					}
				}
				int cbRemoveCard[] = new int[3];
				int count = 0;			
				int dis_weave_card[] = new int[3];
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
				for(int j = table.GRR._weave_items[target_player][wIndex].weave_card_count; j<table.GRR._weave_items[target_player][wIndex].weave_card_count+dis_count;j++){
					table.GRR._weave_items[target_player][wIndex].weave_card[j] = dis_weave_card[j-table.GRR._weave_items[target_player][wIndex].weave_card_count];
				}
				table.GRR._weave_items[target_player][wIndex].weave_card_count += dis_count;
				table.GRR._weave_items[target_player][wIndex].provide_player = _out_card_player;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], table.GRR._weave_count[target_player]-1, table.GRR._weave_count[target_player]);
				table.set_xian_ming_zhao(target_player, table.GRR._weave_items[target_player][wIndex].weave_kind,  table.GRR._weave_items[target_player][wIndex].weave_card,
						 table.GRR._weave_items[target_player][wIndex].weave_card_count);
				table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,target_action,true,weave_sub_index);
				
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
						GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			
			}
			else{
				
				int cbRemoveCard[] = new int[4];
				int chi_kind = target_action - index;
				index --;
			    int remove_count = table._logic.get_must_kind_card(chi_kind,table.GRR._weave_items[target_player][index].center_card,operate_card,cbRemoveCard);
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
						table._yao_weave_item[target_player].weave_card_count -= table._yao_weave_item[target_player].weave_card_count-1;
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
				table.GRR._weave_items[target_player][index].weave_kind |= chi_kind;
				table.GRR._weave_items[target_player][index].provide_player = _out_card_player;
				boolean is_delete = true;
				if(target_card == (kind_card[0]&0xff)){
					table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = kind_card[0]&0xff;
					for(int i =table.GRR._weave_items[target_player][index].weave_card_count;i < table.GRR._weave_items[target_player][index].weave_card_count+kind_count;i++) 	
						table.GRR._weave_items[target_player][index].weave_card[i] = kind_card[0] ;
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
				table.set_xian_ming_zhao(target_player, table.GRR._weave_items[target_player][index].weave_kind,  table.GRR._weave_items[target_player][index].weave_card,
						 table.GRR._weave_items[target_player][index].weave_card_count);
				table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,target_action,is_delete,weave_sub_index);
				
				table.exe_chi_peng(target_player, _out_card_player, chi_kind, target_card,
						GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			}	
			return true;
		}
		
		else if((target_action&GameConstants.WIK_BH_SHANG)!= 0 ||
				(target_action&GameConstants.WIK_BH_XIA)!= 0)
		{
			int index = target_action&(0xF);
			if(index == 0)
				return false;
			index --;
			table.GRR._weave_items[target_player][index].weave_kind |= target_action - (index+1);
			table.GRR._weave_items[target_player][index].weave_card[table.GRR._weave_items[target_player][index].weave_card_count++] = target_card;
			table._logic.get_weave_hu_xi(table.GRR._weave_items[target_player], index, table.GRR._weave_count[target_player]);
			
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
		}
		else if((target_action&GameConstants.WIK_BH_SHANG_TWO)!= 0 ||
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
				
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
						GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			}
		else if((target_action&GameConstants.DZ_WIK_CHI_HU)!=0)// 上牌操作
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score_bh(target_player, _out_card_player, _out_card_data, false);
			if(table._cur_banker != target_player)
				table._cur_banker = (table._cur_banker+1)%table.getTablePlayerNumber();
			// 记录
			// table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;
			table.countChiHuTimes(target_player, false,_out_card_player);

			
			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay+=table.GRR._chi_hu_rights[target_player].type_count-2;
			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(XPBHTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		table.istrustee[seat_index]=false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.XPBH_MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(seat_index!=i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_BH_SHE ) && table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						for(int k = 0; k< table.GRR._weave_items[i][j].weave_card_count;k++)
						{
							weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
						}
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				}else {
					for(int k = 0; k< table.GRR._weave_items[i][j].weave_card_count;k++)
					{
						weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
					}
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			if(table._yao_weave_item[i].weave_card_count>0){
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table._yao_weave_item[i].provide_player);
				weaveItem_item.setPublicCard(table._yao_weave_item[i].public_card);
				weaveItem_item.setWeaveKind(table._yao_weave_item[i].weave_kind);
				weaveItem_item.setHuXi(table._yao_weave_item[i].hu_xi);
				for(int k = 0; k< table._yao_weave_item[i].weave_card_count;k++)
				{
					weaveItem_item.addWeaveCard(table._yao_weave_item[i].weave_card[k]);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			//
			tableResponse.addWinnerOrder(0);

			// 牌
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
		int hand_card_count = table.switch_index_to_card(seat_index, cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}
		}
		for (int i = 0; i < GameConstants.XPBH_MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _out_card_data;
		if (table._logic.is_magic_card(_out_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
		}
		// 出牌
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
				seat_index);

		if(table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index,table._is_xiang_gong[seat_index]);
		// table.operate_player_get_card(_seat_index, 1, new
		// int[]{_send_card_data});
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
	

		return true;
	}
}
