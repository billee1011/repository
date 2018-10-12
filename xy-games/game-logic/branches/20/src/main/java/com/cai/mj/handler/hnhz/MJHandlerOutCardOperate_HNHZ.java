package com.cai.mj.handler.hnhz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerOutCardOperate;

public class MJHandlerOutCardOperate_HNHZ extends MJHandlerOutCardOperate {
	
	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		
		// 重置玩家状态
		playerStatus.clean_status();
		playerStatus.clean_action();

		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		
		// 用户切换
		int next_player =  (_out_card_player + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
		table._current_player = next_player;
		
		//刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		
		//刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		//显示出牌
		table.operate_out_card(_out_card_player,1,new int[]{_out_card_data},GameConstants.OUT_CARD_TYPE_MID,GameConstants.INVALID_SEAT);
		boolean has_feng = false;
		if(table.has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)){
			has_feng=true;
		}
		//红中麻将,检查听牌
		table._playerStatus[_out_card_player]._hu_card_count = table.get_hz_ting_card(table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player], 
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],has_feng);
		
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;
		
		
		if(ting_count>0){
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		}else{
			ting_cards[0]=0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}
		
		
		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;
		

		// 玩家出牌 响应判断,是否有吃碰杠补胡
		boolean bAroseAction = table.estimate_player_out_card_respond_hnhz(_out_card_player, _out_card_data);//, EstimatKind.EstimatKind_OutCard

		// 如果没有需要操作的玩家，派发扑克
		if (bAroseAction == false) {
			for(int i=0; i < GameConstants.GAME_PLAYER; i++){
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
			}
			
			table.operate_player_action(_out_card_player, true);
			
			
			//加入牌队
//			GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _out_card_player,  1, new int[]{_out_card_data}),
//					MJGameConstants.DELAY_ADD_CARD_DELAY, TimeUnit.MILLISECONDS);
			table.runnable_add_discard( _out_card_player,  1, new int[]{_out_card_data},false);
			//发牌
			table.exe_dispatch_card(next_player,GameConstants.WIK_NULL,GameConstants.DELAY_SEND_CARD_DELAY);
			
			
		}else{
			//等待别人操作这张牌
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				playerStatus = table._playerStatus[i];
				if(playerStatus.has_action()){
					if(playerStatus.has_chi_hu()){
//						if(table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
//							//见炮胡
//							table.exe_jian_pao_hu(i,GameConstants.WIK_CHI_HU,_out_card_data);
//						}else{
//							table.operate_player_action(i, false);
//						}
						
						table.operate_player_action(i, false);
					}else{
						table.operate_player_action(i, false);
					}
					
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
		// 效验状态
		
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		 // 是否已经响应
		if(playerStatus.has_action()==false){
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		
		 // 是否已经响应
		if(playerStatus.is_respone()){
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		
		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		

		//玩家的操作
		playerStatus.operate(operate_code,operate_card);
		
		if(operate_code == GameConstants.WIK_CHI_HU){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);//胡牌生效
			//效果
			table.process_chi_hu_player_operate_hnhz(seat_index, operate_card,false);
			
		}else if(operate_code == GameConstants.WIK_NULL){
			table.GRR._chi_hu_rights[seat_index].set_valid(false);//胡牌无效
			if(table._playerStatus[seat_index].has_chi_hu()){
				table._playerStatus[seat_index].chi_hu_round_invalid();//这一轮就不能吃胡了没过牌之前都不能胡
			}
		}
		
	
		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if ((table._playerStatus[i].is_respone()== false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}
		
		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;
		
		
		// 执行判断
		for (int p = 0; p <GameConstants.GAME_PLAYER; p++){
			int i =(_out_card_player+p) % GameConstants.GAME_PLAYER;
			// 获取动作
			int cbUserActionRank =0;
			
			if(table._playerStatus[i].has_action()){
				if(table._playerStatus[i].is_respone()){
					//获取已经执行的动作的优先级
					cbUserActionRank =  table._logic.get_action_rank(table._playerStatus[i].get_perform());
				}else{
					//获取最大的动作的优先级
					cbUserActionRank =  table._logic.get_action_list_rank(table._playerStatus[i]._action_count,table._playerStatus[i]._action);
				}
				
				// 优先级别
				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;//最高级别人
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone()== false)
			return true;

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

//			// 出牌变量
//			_out_card_data = 0;
//			_out_card_player = MJGameConstants.INVALID_SEAT;
		
		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			
			table.operate_player_action(i, true);
		}
		
		// 删除扑克
		switch (target_action) {
			case GameConstants.WIK_LEFT: // 上牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "吃牌删除出错");
						return false;
					}
					table.exe_chi_peng( target_player, _out_card_player,target_action, target_card,GameConstants.CHI_PENG_TYPE_OUT_CARD);
					return true;
				}
			case GameConstants.WIK_RIGHT: // 上牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
		
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "吃牌删除出错");
						return false;
					}
					table.exe_chi_peng( target_player, _out_card_player,target_action, target_card,GameConstants.CHI_PENG_TYPE_OUT_CARD);
					return true;
				}
			case GameConstants.WIK_CENTER: // 上牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "吃牌删除出错");
						return false;
					}
					table.exe_chi_peng( target_player, _out_card_player,target_action, target_card,GameConstants.CHI_PENG_TYPE_OUT_CARD);
					return true;
				}
			case GameConstants.WIK_PENG: // 碰牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card, target_card };
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "碰牌删除出错");
						return false;
					}
		
					table.exe_chi_peng( target_player, _out_card_player,target_action, target_card,GameConstants.CHI_PENG_TYPE_OUT_CARD);
					return true;
				}
				
			case GameConstants.WIK_GANG: // 杠牌操作
			{
				//是否有抢杠胡
				table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false,false);
				return true;
			}
			case  GameConstants.WIK_NULL:{

				//删掉出来的那张牌
				//table.operate_out_card(this._out_card_player, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
				
				//加到牌堆 没有人要  
				table.runnable_add_discard(this._out_card_player, 1, new int[]{this._out_card_data},false);
				
				// 用户切换
				_current_player = table._current_player = (_out_card_player + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
				
				//发牌
				table.exe_dispatch_card(_current_player,GameConstants.WIK_NULL,0);
				
				return true;
			}
			case GameConstants.WIK_CHI_HU: //胡
			{
				int jie_pao_count = 0;
				for(int i=0; i<GameConstants.GAME_PLAYER;i++){
					if((i == _out_card_player) ||(table.GRR._chi_hu_rights[i].is_valid()==false )){
						continue;
					}
					jie_pao_count++;
				}
				
				if(jie_pao_count>0){
					int add_niao = 0;
					if(jie_pao_count>1){
						// 结束后设置鸟牌
						//红中通炮就不加鸟了
						table.set_niao_card(_out_card_player,GameConstants.INVALID_VALUE,true,add_niao);
						//结束
						table._banker_select = _out_card_player;
					}else if(jie_pao_count==1){
						table._banker_select = target_player;
						
						if(table.GRR._cards_index[target_player][table._logic.get_magic_card_index(0)]==0){
							//手上没有红中
							add_niao=1;
						}
						// 结束后设置鸟牌 
						table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true,add_niao);
						
					}
					for(int i=0; i<GameConstants.GAME_PLAYER;i++){
						if((i == _out_card_player) ||(table.GRR._chi_hu_rights[i].is_valid()==false )){
							continue;
						}
						table.process_chi_hu_player_score_hnhz(i,_out_card_player, _out_card_data,false);

						// 记录
						table._player_result.jie_pao_count[i]++;
						table._player_result.dian_pao_count[_out_card_player]++;
						
					}
					
					
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
							GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

				}
				return true;
			}
			default:
				return false;
		}
	}
	
	@Override
	public boolean handler_player_be_in_room(MJTable table,int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
	
}
