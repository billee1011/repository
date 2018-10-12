/**
 * 
 */
package com.cai.game.phz.handler.yiyangwhz;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.phz.handler.PHZHandlerDispatchCard;

public class WHZHandlerChuLiFirstCard_YiYang extends PHZHandlerDispatchCard<YiYangWHZTable> {

	@Override
	public void exe(YiYangWHZTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR != null && table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.operate_dou_liu_zi(-1,false ,0);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
					2, TimeUnit.SECONDS);
			return;
		}
		
		//刷新手牌包括组合
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		table._hand_card_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		int cards[]= new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int action = 0;
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		
		int is_tianhu=table.is_tian_hu(_seat_index,table._current_player ,chr, 0 );
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		if(is_tianhu == 2){
			int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				cbCardIndexTemp[j] = table.GRR._cards_index[_seat_index][j];
			}
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if(cbCardIndexTemp[i] == 3){
					for(int j=0;j<4;j++){
						table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]].weave_card[j]=table._logic.switch_to_card_data(i);
					}
					table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]].weave_kind = GameConstants.WIK_YYWHZ_KAN;
					table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]].hu_xi = table._logic.get_weave_hu_xi_yiyangwhz_yywzh(table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]]);
					table._hu_weave_count[_seat_index]++;
				}
				if(cbCardIndexTemp[i] == 4){
					for(int j=0;j<4;j++){
						table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]].weave_card[j]=table._logic.switch_to_card_data(i);
					}
					table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]].weave_kind = GameConstants.WIK_YYWHZ_LIU_NEI;
					table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]].hu_xi = table._logic.get_weave_hu_xi_yiyangwhz_yywzh(table._hu_weave_items[_seat_index][table._hu_weave_count[_seat_index]]);
					table._hu_weave_count[_seat_index]++;
				}
			}
		}
		// 发送数据
    	// 只有自己才有数值
		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
			
		GangCardResult gangCardResult = new GangCardResult();
		int cbActionMask=table.estimate_player_qing_nei_respond_yywhz(table.GRR._cards_index[_seat_index],table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],table._send_card_data,
				gangCardResult);
		
		
		if(cbActionMask!=GameConstants.WIK_NULL){//有溜
			for(int i= 0; i < gangCardResult.cbCardCount; i++){
				curPlayerStatus.add_liu(gangCardResult.cbCardData[i], _seat_index, gangCardResult.isPublic[i],cbActionMask);
				curPlayerStatus.add_action(cbActionMask);//溜牌
				if(is_tianhu == 1){
					// 添加动作
		        	table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
		        	table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);	
					chr.opr_or(GameConstants.CHR_ZI_MO);
					if(table.has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_TIAN_DI_HU)){
						chr.opr_or(GameConstants.CHR_TIAN_HU_WHZ_YIYANG);
					}
					
				}else if(is_tianhu == 2){
					// 添加动作
		        	table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
		        	table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);	
					chr.opr_or(GameConstants.CHR_ZI_MO);
					
				}
			}
			curPlayerStatus.add_action(GameConstants.WIK_NULL);
			curPlayerStatus.add_pass(table._send_card_data, _seat_index);
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
		}else if (is_tianhu == 1) {
			table._playerStatus[_seat_index].add_action(GameConstants.WIK_NULL);
			table._playerStatus[_seat_index].add_pass(table._send_card_data, _seat_index);
			
			// 添加动作
        	table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
        	table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);	
			chr.opr_or(GameConstants.CHR_ZI_MO);
			if(table.has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_TIAN_DI_HU)){
				chr.opr_or(GameConstants.CHR_TIAN_HU_WHZ_YIYANG);
			}
			//发 操作
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
			

		}else if(is_tianhu == 2){
			table._playerStatus[_seat_index].add_action(GameConstants.WIK_NULL);
			table._playerStatus[_seat_index].add_pass(table._send_card_data, _seat_index);
			
			// 添加动作
        	table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
        	table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);	
			chr.opr_or(GameConstants.CHR_ZI_MO);
			//发 操作
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
		}


		if(is_tianhu == 0 && cbActionMask==GameConstants.WIK_NULL){
			chr.set_empty();
			// 加到手牌
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		}

		return;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(YiYangWHZTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}



		// 出牌
		// if(_type == MJGameConstants.DispatchCard_Type_Gang){
		// table.exe_re_chong(_seat_index,card,_type);
		// }else {
		// table.exe_out_card(_seat_index,card,_type);
		// }
		table.exe_out_card(_seat_index, card, _type);

		return true;
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
	public boolean handler_operate_card(YiYangWHZTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		table.log_error(_seat_index+"  " + table._current_player +"  " +"下次 出牌用户" +seat_index+"操作用户" );
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
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
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		if(operate_code == GameConstants.WIK_YYWHZ_LIU_NEI){
			_liu_card_data[seat_index]=operate_card;
		}
		//operate_card=table._send_card_data;
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

	
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code =luoCode;
		int target_p = 0;
		int cbActionRank[] = new int[3];
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
 					cbUserActionRank = table._logic.get_action_rank_yiyang(table._playerStatus[i].get_perform(),i,_seat_index)
							+ table.getTablePlayerNumber() - p;
					if(cbUserActionRank >= 30 && cbUserActionRank<40){
 						cbUserActionRank+=30;
 					}
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank_yiyang(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action,i,_seat_index) + table.getTablePlayerNumber() - p;
					if(cbUserActionRank >= 30 && cbUserActionRank<40){
 						cbUserActionRank+=30;
 					}
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank_yiyang(table._playerStatus[target_player].get_perform(),target_player,_seat_index)
							+ target_p;
					if(cbTargetActionRank >= 30 && cbTargetActionRank<40){
						cbTargetActionRank+=30;
 					}
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank_yiyang(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action,target_player,_seat_index) + target_p;
					if(cbTargetActionRank >= 30 && cbTargetActionRank<40){
						cbTargetActionRank+=30;
 					}
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
			table.log_error("优先级最高的人还没操作");
			return true;
		}
		
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL:
		{

			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			curPlayerStatus.reset();
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();

			return true;

		}
		
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
			
			if(table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI) == false)
				table.operate_player_get_card(target_player, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT,false);
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_yywhz(target_player, target_player, operate_card, true);
			table.countChiHuTimes(target_player,true);
			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			table.GRR._chi_hu_card[target_player][0]=table._send_card_data;
			
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			for(int i=0;i<table.getTablePlayerNumber();i++){
				table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);// 出牌状态
				table._playerStatus[i].clean_action();
				table.operate_player_status();
			}
			
			return true;
		}
		case GameConstants.WIK_YIYANGWHZ_QING_NEI: // 溜牌
		{

			
			table.exe_liu(target_player, _seat_index, target_action, operate_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,0);
			table._user_out_card_count[_seat_index]++;
			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(YiYangWHZTable table, int seat_index) {
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
