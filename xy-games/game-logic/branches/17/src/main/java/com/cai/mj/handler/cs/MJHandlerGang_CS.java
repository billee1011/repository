package com.cai.mj.handler.cs;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerGang;

public class MJHandlerGang_CS extends MJHandlerGang {

	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for(int i = 0; i < MJGameConstants.GAME_PLAYER; i++){
			if(table._playerStatus[i].has_action()){
				table.operate_player_action(i, true);
			}
			
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
	
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		//效果
		table.operate_effect_action(_seat_index,MJGameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,MJGameConstants.INVALID_SEAT);
	
		if((MJGameConstants.GANG_TYPE_AN_GANG == _type)||
				(MJGameConstants.GANG_TYPE_JIE_GANG == _type)){
			this.exe_gang(table);
			return;
		}
		
		//检查对这个杠有没有 胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);
		
		if(bAroseAction == false){
			this.exe_gang(table);
		}else{
			PlayerStatus playerStatus = null;
			for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
				playerStatus= table._playerStatus[i];
				if(playerStatus.has_chi_hu()){
					table._playerStatus[i].set_status(MJGameConstants.Player_Status_OPR_CARD);//操作状态
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
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		//抢杠胡
		
		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		 // 是否已经响应
		if(playerStatus.has_action()==false){
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}
		
		 // 是否已经响应
		if(playerStatus.is_respone()){
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}
		
		if ((operate_code != MJGameConstants.WIK_NULL) && (operate_code != MJGameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}
		
		if((operate_code!=MJGameConstants.WIK_NULL) && (operate_card != _center_card)){
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}
		
		//玩家的操作
		playerStatus.operate(operate_code,operate_card);
				
		if(operate_code == MJGameConstants.WIK_NULL){
			table.GRR._chi_hu_rights[seat_index].set_valid(false);//胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();//这一轮就不能吃胡了没过牌之前都不能胡
		}else if(operate_code == MJGameConstants.WIK_CHI_HU){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);//胡牌生效
			table.process_chi_hu_player_operate(seat_index, _center_card,false);//效果
		}else{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}
		
		//清理这个玩家状态
		table._playerStatus[seat_index].clean_action();
		table._playerStatus[seat_index].clean_status();
		table.operate_player_action(seat_index, true);
		
		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			if ((table._playerStatus[i].is_respone()== false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}
		
		int jie_pao_count = 0;
		for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
			if((table.GRR._chi_hu_rights[i].is_valid()==false )){//(i == _provide_player) ||
				continue;
			}
			jie_pao_count++;
		}
		
		if(jie_pao_count>0){
			if(jie_pao_count>1){
				//结束
				table._banker_select = _seat_index;
				//红中通炮就不加鸟了
				table.set_niao_card(_provide_player,MJGameConstants.INVALID_VALUE,true,0);// 结束后设置鸟牌
			}else if(jie_pao_count==1){
				table._banker_select = seat_index;
				
				table.set_niao_card(seat_index,MJGameConstants.INVALID_VALUE,true,0);// 结束后设置鸟牌 
			}
			for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
				if((table.GRR._chi_hu_rights[i].is_valid()==false )){//(i == _provide_player) ||
					continue;
				}
				
				table.GRR._chi_hu_card[i] = _center_card;
				
				table.process_chi_hu_player_score(i,_seat_index, _center_card,false);

				// 记录
				table._player_result.da_hu_jie_pao[i]++;
				table._player_result.da_hu_dian_pao[_seat_index]++;
			}
			
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, MJGameConstants.Game_End_NORMAL),
					MJGameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		}else{
			//选择了不胡
			//发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}
	
	/**执行杠
	 * 
	 * 
	 * ***/
	protected boolean exe_gang(MJTable table){
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		
		
		if(MJGameConstants.GANG_TYPE_AN_GANG == _type){
			//暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			
		}else if(MJGameConstants.GANG_TYPE_JIE_GANG == _type){
			//别人打的牌
			
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			
			//删掉出来的那张牌
			//table.operate_out_card(_provide_player, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
			
		}else if(MJGameConstants.GANG_TYPE_ADD_GANG == _type){
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == MJGameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					break;
				}
			}

			if (cbWeaveIndex == -1){
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
			
		}
		
	
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p==true?1:0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		
		// 设置用户
		table._current_player = _seat_index;
		
			
		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		//刷新手牌包括组合
		int cards[]= new int[MJGameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		
		//长沙麻将杠没有分
		if(_action == MJGameConstants.WIK_BU_ZHNAG){
			// 从后面发一张牌给玩家
			table.exe_dispatch_card(_seat_index,_action,0);
		}else if(_action == MJGameConstants.WIK_GANG){
			// 从后面发两张牌给玩家
			table.exe_gang_cs(_seat_index,false);
		}

		return true;
		
	}
}
