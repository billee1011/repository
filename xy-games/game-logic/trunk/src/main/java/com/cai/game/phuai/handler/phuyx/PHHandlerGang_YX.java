package com.cai.game.phuai.handler.phuyx;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.game.phuai.PHAITable;
import com.cai.game.phuai.handler.PHHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class PHHandlerGang_YX extends PHHandlerGang<PHAITable> {

	@Override
	public void exe(PHAITable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
//		if(_depatch == false)
//			table.operate_out_card(this._provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
//				GameConstants.INVALID_SEAT);
//		else
//			table.operate_player_get_card(this._provide_player, 0, null, GameConstants.INVALID_SEAT,false);

		// 效果
		
//		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
//			this.exe_gang(table);
//			return;
//		}
		this.exe_gang(table);
		// 检查对这个杠有没有 胡
		// boolean bAroseAction = table.estimate_gang_respond_fls(_seat_index,
		// _center_card,_action);
//		boolean bAroseAction = false;
//		if (bAroseAction == false) {
	//	this.exe_gang(table);	
//		} else {
//			PlayerStatus playerStatus = null;
//			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//				playerStatus = table._playerStatus[i];
////				if (playerStatus.has_chi_hu()) {
////					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
////					table.operate_player_action(i, false);
////				}
////				if(playerStatus.has_chi_hu()){
////					if(table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_JIANPAOHU) || table.isTrutess(i)){
////						//见炮胡
////						table.exe_jian_pao_hu(i,GameConstants.WIK_CHI_HU,_center_card);
////					}else{
////						table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//操作状态
////						table.operate_player_action(i, false);
////					}
////				}
//			}

//		}

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
	public boolean handler_operate_card(PHAITable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		// 抢杠胡

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌,玩家操作已失效");
			return false;
		}
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) &&(operate_code != GameConstants.WIK_CHI_HU) &&(operate_code != GameConstants.WIK_ZI_MO))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌操作,操作牌对象出错");
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			if(playerStatus.has_zi_mo() == true&&(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON)||table._warning[seat_index] == 2))
			{
				table.log_player_error(seat_index,"强制胡牌操作空操作无效");
				return true;
			}
		}
//		if(playerStatus.has_zi_mo() == true)
//		{
//			 if(table._game_weave_score[_seat_index] != 0)
//			 {
//				 table.operate_game_mid_score(true);
//			 }
//			table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
//		}
//		else if(playerStatus.has_chi_hu() == true)
//		{
//			 if(table._game_weave_score[_seat_index] != 0)
//			 {
//				 table.operate_game_mid_score(true);
//			 }
//			table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
//		}
//			

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);
		

		

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
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
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
//			table.log_error("最高用户操作"+target_player);
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
		case GameConstants.WIK_NULL:
		{
//			table.operate_effect_action(target_player,GameConstants.EFFECT_ACTTON_CACEL_DISPLAY_CARD, _seat_index, new long[]{GameConstants.WIK_NULL}, 1,GameConstants.INVALID_SEAT);
			_score = new int[table.getTablePlayerNumber()];
			Arrays.fill(_score, 0);
			table.sao_ti_operate(_seat_index,_provide_player,_center_card,_action,_p,_score,_depatch);
//			table._logic.calculate_game_weave_score(table._game_mid_score,table._game_weave_score,_score,table.getTablePlayerNumber(),false);
//			
//			 if(table._game_weave_score[_seat_index] != 0)
//			 {
//				 table.operate_game_mid_score(true);
//			 }
//			int dispatch_time = 500;
//			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//					.get(1105);
//			if (sysParamModel1105 != null && sysParamModel1105.getVal3() > 0 && sysParamModel1105.getVal3() < 10000) {
//				dispatch_time = sysParamModel1105.getVal3();
//			}
//			SysParamModel sysParamModel1111 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//					.get(1111);
//			if (table.has_rule(GameConstants.GAME_RULE_QUICK_SPEED)&&sysParamModel1111!= null && sysParamModel1111.getVal3() > 0 && sysParamModel1111.getVal3() < 10000) {
//				dispatch_time = sysParamModel1111.getVal3();
//			}
//			if((table._is_xiang_gong[_seat_index] == false )&&(table._long_count[_seat_index] == 1||GameConstants.SAO_TYPE_MINE_SAO  == _type))
//			{    
//				//要出牌，但是没有牌出设置成相公  下家用户发牌
//				int pai_count =0;
//				for(int i = 0; i<GameConstants.MAX_HH_INDEX ;i++) {
//			       	if(table.GRR._cards_index[_seat_index][i]<3)
//			       		pai_count += table.GRR._cards_index[_seat_index][i];
//				 }
//			
//				if(pai_count == 0)
//				{
//					table._is_xiang_gong[_seat_index] = true;	 	
//		        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
//		        	int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//					// 用户状态
//					table._playerStatus[_seat_index].clean_action();
//					table._playerStatus[_seat_index].clean_status();
//					table._current_player = next_player;
//					table._last_player = next_player;
//					
//					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
//					table.log_error(next_player + "提 扫 跑 发牌"+_seat_index);
//					
//		    	
//				}
//				else{  //胡牌了不执行
//					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
//					table.operate_player_status();
//					table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
//					//table.log_player_error(_seat_index, "扫和提龙出牌状态");
//				}
//				
//			}
//			else{
//				// 用户状态
//				table._playerStatus[_seat_index].clean_action();
//				table._playerStatus[_seat_index].clean_status();
//				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//				table._current_player = next_player;
//				table._last_player = next_player;
//				
//				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
//				table.log_error(next_player + "提 扫 跑 发牌"+_seat_index);
//				
//			}
//
//
//		
		    
			
			return true;

		}
		case GameConstants.WIK_CHI_HU: // 胡
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}
			table._cur_banker = target_player;
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[target_player][0] = operate_card;
			if(table.has_rule(GameConstants.GAME_RULE_NO_XING) != true)
				table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_score_ph(target_player, this._provide_player, operate_card, false);
			table.process_chi_hu_player_operate(target_player, operate_card, false);

			_center_card = 0;
			table.countChiHuTimes_ph(target_player,  _seat_index,false);

			
			int delay =3;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
//			if(_depatch == true)
//				table.operate_player_get_card(this._provide_player, 1, new int[] { _center_card }, GameConstants.INVALID_SEAT,false,1);
			table._is_mo_da = 1;
			table._provide_player_hu = _seat_index;
			table._shang_zhuang_player = _seat_index;
			if(table.has_rule(GameConstants.GAME_RULE_NO_XING) != true)
				table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table._provide_player_hu = _seat_index;
			table.process_chi_hu_player_score_ph(target_player, _seat_index, operate_card, true);
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			
			_center_card = 0;
		
			table.countChiHuTimes_ph(target_player,_seat_index, true);

			int delay = 3;
			
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}



		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	protected boolean exe_gang(PHAITable table) {
//		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
//		boolean is_di_hu = true; 
//		boolean one_other = false;
//		int out_peng = GameConstants.WIK_NULL;
//		int provider = -1;
		if (GameConstants.PAO_TYPE_AN_LONG == _type) {
			// 暗龙
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
//			table.GRR._weave_count[_seat_index]++;
			table._long_count[_seat_index]++;

		}else if(GameConstants.PAO_TYPE_TI_MINE_LONG== _type){
			// 提龙
			// 设置变量
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_SAO)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					table._long_count[_seat_index]++;
					break;
				}
			}
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index,cbWeaveIndex+ "// 提龙 杠牌出错");
				return false;
			}
		}else if(GameConstants.PAO_TYPE_MINE_SAO_LONG== _type){
			// 提龙
			// 设置变量
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_SAO)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					table._long_count[_seat_index]++;
					break;
				}
			}
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index,cbWeaveIndex+ "杠牌出错");
				return false;
			}
		}else if (GameConstants.PAO_TYPE_OTHER_SAO_PAO == _type) {
			// 别人打的牌
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_SAO)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					table._long_count[_seat_index]++;
					break;
				}
			}
			
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
			//table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
			
			
		} 
		else if (GameConstants.PAO_TYPE_OHTER_PAO  == _type) {
			// 别人打的牌
			table._long_count[_seat_index]++;
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
//			table.GRR._weave_count[_seat_index]++;
			
			
			//table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

		}else if (GameConstants.PAO_TYPE_MINE_PENG_PAO  == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && ((cbWeaveKind == GameConstants.WIK_PENG)||(cbWeaveKind == GameConstants.WIK_PENG_OUT))) {
//					if(cbWeaveKind == GameConstants.WIK_PENG_OUT)
//					{
//						table.exe_add_discard( table.GRR._weave_items[_seat_index][i].provide_player,  -1, new int[]{cbCenterCard },true,1);
//					}
					cbWeaveIndex = i;// 第几个组合可以碰　
					table._long_count[_seat_index]++;
					break;
				}
			}
			
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index,cbWeaveIndex +"杠牌出错");
				return false;
			} 
		
			
			

		}else if (GameConstants.SAO_TYPE_MINE_SAO  == _type){
			//扫牌
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
//			table.GRR._weave_count[_seat_index]++;
		}
		
		/**
		 * sao_ti
		 * 
		 */


		
		int pai_count =0;
		int action_hu = GameConstants.WIK_NULL;
		for(int i = 0; i<GameConstants.MAX_HH_INDEX ;i++) {
	       	if(table.GRR._cards_index[_seat_index][i]<3)
	       	{
	       		if(i != table._logic.switch_to_card_index(_center_card))
	       			pai_count += table.GRR._cards_index[_seat_index][i];
	       	}
		 }
		if((_depatch == true)&&(table._ti_two_long[_seat_index] == false)&&(table._is_xiang_gong[_seat_index] == false)&&(_action != GameConstants.WIK_PAO)){
			// 变量定义
			ChiHuRight chr = new ChiHuRight();
		
			chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			
			int hu_xi[] = new int[1];
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			int peng_sao_count[] = new int[1];
			table._logic.get_weave_hu_fen(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind,peng_sao_count);
			int temp_card[] = new int[GameConstants.MAX_HH_INDEX];
			for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++)
			{
				if(i != table._logic.switch_to_card_index(_center_card))
				{
					temp_card[i] = table.GRR._cards_index[_seat_index][i];
				}
				else
					temp_card[i] = 0;
			}
			action_hu = table.analyse_chi_hu_card(temp_card, table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],_seat_index,_provide_player,0, chr, card_type,hu_xi,true);// 自摸
			if(action_hu != GameConstants.WIK_NULL||pai_count == 0){
				chr.set_empty();
				if(_action == GameConstants.WIK_TI_LONG)
				{
					chr.opr_or(GameConstants.CHR_TI_LONG_HU_PH);
				}
				if(table._warning[_seat_index] == 3)
					peng_sao_count[0] = 1;
				if(_action == GameConstants.WIK_SAO || _action == GameConstants.WIK_CHOU_SAO)
				{
					peng_sao_count[0]++;
					if(peng_sao_count[0] == 3)
						chr.opr_or(GameConstants.CHR_W_SAN_DA_HU_PH);
					else if(peng_sao_count[0] == 4)
						chr.opr_or(GameConstants.CHR_WEI_SI_HU_PH);
					else if(peng_sao_count[0] == 5)
						chr.opr_or(GameConstants.CHR_WU_WFU_HU_PH);
					else 
						chr.opr_or(GameConstants.CHR_WEI_HU_PH);
				}
				if(_action == GameConstants.WIK_PAO)
				{
					chr.opr_or(GameConstants.CHR_PAO_HU_PH);
				}
				PlayerStatus  tempPlayerStatus = table._playerStatus[_seat_index];
				tempPlayerStatus.set_operate_card(_center_card);
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_center_card, _seat_index);
				if(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON)||pai_count == 0)
				{
					SysParamModel sysParamModel13 = SysParamServerDict.getInstance()
							.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(table._game_type_index)).get(13);
					int dalay = 1000;
					if(sysParamModel13.getVal5() > 0 && sysParamModel13.getVal5() <5000)
						dalay = sysParamModel13.getVal5() ;
					GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_center_card),
							dalay, TimeUnit.MILLISECONDS);
					
					return true;
				}
				else if(pai_count != 0)
				{
					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_pass(0, _seat_index);
				}
				
				 if (tempPlayerStatus.has_action()) {
					 tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
					 //操作状态
					  table.operate_player_action(_seat_index, false);
					  table.set_timer(GameConstants.PH_OPERATE_CARD , 5, true);
					  return true;
					 }
			}
			else{
				chr.set_empty();
			}
	       
		
		}

//		if((_depatch == false)&&(table._ti_two_long[_seat_index] == false)&&(table._is_xiang_gong[_seat_index] == false)&&(_action != GameConstants.WIK_PAO)){
//			// 变量定义
//			ChiHuRight chr = new ChiHuRight();
//		
//			chr = table.GRR._chi_hu_rights[_seat_index];
//			chr.set_empty();
//			
//			int hu_xi[] = new int[1];
//			int card_type = GameConstants.HU_CARD_TYPE_PAOHU;
//			action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
//					table.GRR._weave_count[_seat_index],_seat_index,_provide_player,0, chr, card_type,hu_xi,false);// 吃胡
//
//			if(action_hu != GameConstants.WIK_NULL||pai_count == 0){
//				if(_action == GameConstants.WIK_TI_LONG || _action == GameConstants.WIK_SAO 
//						|| _action == GameConstants.WIK_CHOU_SAO||_action == GameConstants.WIK_PAO)
//				{
//					chr.set_empty();
//				}
////				if(_action == GameConstants.WIK_TI_LONG)
////				{
////					chr.opr_or(GameConstants.CHR_TI_LONG_HU_PH);
////				}
////				if(_action == GameConstants.WIK_SAO || _action == GameConstants.WIK_CHOU_SAO)
////				{
////					if(peng_sao_count[0] == 3)
////						chr.opr_or(GameConstants.CHR_W_SAN_DA_HU_PH);
////					else if(peng_sao_count[0] == 4)
////						chr.opr_or(GameConstants.CHR_WEI_SI_HU_PH);
////					else if(peng_sao_count[0] == 5)
////						chr.opr_or(GameConstants.CHR_WU_WFU_HU_PH);
////					else 
////						chr.opr_or(GameConstants.CHR_WEI_HU_PH);
////				}
////				if(_action == GameConstants.WIK_PAO)
////				{
////					chr.opr_or(GameConstants.CHR_PAO_HU_PH);
////				}
//				PlayerStatus  tempPlayerStatus = table._playerStatus[_seat_index];
//				tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
//				tempPlayerStatus.add_chi_hu(_center_card, _seat_index);
//				if(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON))
//				{
//					GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_CHI_HU,_center_card),
//							600, TimeUnit.MILLISECONDS);
//					return true;
//				}
//				if(pai_count != 2){
//					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
//					tempPlayerStatus.add_pass(0, _seat_index);
//				}
//				 if (tempPlayerStatus.has_action()) {
//						 tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
//						 //操作状态
//						  table.operate_player_action(_seat_index, false);
//						  table.set_timer(GameConstants.PH_OPERATE_CARD , 5, true);
//						  return true;
//					 }
//			}
//			else{
//				chr.set_empty();
//			}
//		}
		_score = new int[table.getTablePlayerNumber()];
		Arrays.fill(_score, 0);
		table.sao_ti_operate(_seat_index,_provide_player,_center_card,_action,_p,_score,_depatch);
//        table._logic.calculate_game_weave_score(table._game_mid_score,table._game_weave_score,_score,table.getTablePlayerNumber(),false);
//	
//		 if(table._game_weave_score[_seat_index] != 0)
//		 {
//			 table.operate_game_mid_score(true);
//		 }
//		if((table._is_xiang_gong[_seat_index] == false )&&(table._long_count[_seat_index] == 1||GameConstants.SAO_TYPE_MINE_SAO  == _type))
//		{    
//			//要出牌，但是没有牌出设置成相公  下家用户发牌
//			if(pai_count == 0)
//			{
//				table._is_xiang_gong[_seat_index] = true;	 	
//	        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
//	        	int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//				table._current_player = next_player;
//				table._last_player = next_player;
//				int dispatch_time = 1200;
//				SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//						.get(1105);
//				if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
//					dispatch_time = sysParamModel1105.getVal4();
//				}
//				SysParamModel sysParamModel1111 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//						.get(1111);
//				if (table.has_rule(GameConstants.GAME_RULE_QUICK_SPEED)&&sysParamModel1111!= null && sysParamModel1111.getVal4() > 0 && sysParamModel1111.getVal4() < 10000) {
//					dispatch_time = sysParamModel1111.getVal4();
//				}
//				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
//	    	
//			}
//			else{
//				if(table._ti_two_long[_seat_index] == false)
//				{
//					//胡牌了不执行
//					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
//					table.operate_player_status();
//					table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
//					//table.log_player_error(_seat_index, "扫和提龙出牌状态");
//				}
//				else{
//					if(table._ti_two_long[_seat_index] == true)
//						table._ti_two_long[_seat_index] = false;
//					table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
//							table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
//							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);
//
//					int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
//					int ting_count = table._playerStatus[_seat_index]._hu_card_count;
//
//					if (ting_count > 0) {
//						table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
//					} else {
//						ting_cards[0] = 0;
//						table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
//					}
//					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//					table._current_player = next_player;
//					table._last_player = next_player;
//					table._last_card = 0;
//					int dispatch_time = 500;
//					SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//							.get(1105);
//					if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
//						dispatch_time = sysParamModel1105.getVal4();
//					}
//					SysParamModel sysParamModel1111 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//							.get(1111);
//					if (table.has_rule(GameConstants.GAME_RULE_QUICK_SPEED)&&sysParamModel1111!= null && sysParamModel1111.getVal4() > 0 && sysParamModel1111.getVal4() < 10000) {
//						dispatch_time = sysParamModel1111.getVal4();
//					}
//					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
//					//table.log_player_error(_seat_index, "吃或碰出牌状态");
//					
//				}
//				
//			}
//			
//		}
//		else{
//			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
//					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
//					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);
//
//			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
//			int ting_count = table._playerStatus[_seat_index]._hu_card_count;
//
//			if (ting_count > 0) {
//				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
//			} else {
//				ting_cards[0] = 0;
//				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
//			}
//			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//			table._current_player = next_player;
//			table._last_player = next_player;
//			table._last_card = 0;
//			int dispatch_time = 500;
//			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//					.get(1105);
//			if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
//				dispatch_time = sysParamModel1105.getVal4();
//			}
//			SysParamModel sysParamModel1111 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
//					.get(1111);
//			if (table.has_rule(GameConstants.GAME_RULE_QUICK_SPEED)&&sysParamModel1111!= null && sysParamModel1111.getVal4() > 0 && sysParamModel1111.getVal4() < 10000) {
//				dispatch_time = sysParamModel1111.getVal4();
//			}
//			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
//		}

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(PHAITable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		for(int i = 0; i<table.getTablePlayerNumber();i++)
			roomResponse.addScore(table._game_mid_score[i]);
		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(table._banker_count[table.GRR._banker_player]);

		// 状态变量
		tableResponse.setActionCard(0);
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);
		table.istrustee[seat_index]=false;
		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
					//癞子
					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_HUN);
				}else{
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(seat_index!=i) {
					if((table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_GANG || table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_ZHAO) &&table.GRR._weave_items[i][j].public_card==0) {
						weaveItem_item.setCenterCard(0);
					}else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addHuXi(table._game_mid_score[i]);
			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			if(table._warning[i] == 2)
				tableResponse.addTrustee(true);
			else
				tableResponse.addTrustee(false);
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for(int j=0; j < hand_card_count; j++){
			if( table._logic.is_magic_card(cards[j])){
				cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_HUN;//将癞子转下
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		boolean b_display = false;
		for(int i = 0; i< table.getTablePlayerNumber();i++)
		{
			if(table._playerStatus[i].has_action())
			{
				b_display = true;
				break;
			}
		}

		if((_center_card != 0))
		{
			if(b_display)
			{
				
				if(_action == GameConstants.WIK_TI_LONG)
				{
					table.operate_player_get_card(_seat_index, 1, new int[] { _center_card },seat_index,true,1,2);
				}
				else{
					
					if(_action == GameConstants.WIK_CHOU_SAO)
						table.operate_player_get_card(_seat_index, 1, new int[] { _center_card }, seat_index,false,1,3);		
					else
						table.operate_player_get_card(_seat_index, 1, new int[] { _center_card }, seat_index,true,1,3);
				
				}
			}
//			else
//			{
//				table.operate_player_get_card(_seat_index, 1, new int[] { _center_card }, seat_index,false,0);
//			}
		}
		
		
		//效果
//		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,seat_index);
		
		
		if(table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone()==false){
			table.operate_player_action(seat_index, false);
		}
		
		if(table._warning[seat_index] == 1)
		{
			table.operate_is_warning(seat_index,false);
		}
//		if(table._warning[seat_index] == 2)
//			table.operate_player_xiang_gong_flag(seat_index,table._is_xiang_gong[seat_index]);
		return true;
	}
}
