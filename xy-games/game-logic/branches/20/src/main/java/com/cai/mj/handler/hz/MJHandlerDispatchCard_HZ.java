package com.cai.mj.handler.hz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerDispatchCard;

public class MJHandlerDispatchCard_HZ extends MJHandlerDispatchCard {
	
	@Override
	public void exe(MJTable table) {
		// 用户状态--清除状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		int llcard = table.get_niao_card_num(true,0);
		table._banker_select=_seat_index;
		
		// 荒庄结束
		if (table.GRR._left_card_count == llcard) {
			for(int i=0; i < GameConstants.GAME_PLAYER; i++){
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
		
		--table.GRR._left_card_count;

		table._provide_player = _seat_index;
		
		if(table.DEBUG_CARDS_MODE){
			_send_card_data = 0x16;
		}
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,GameConstants.HU_CARD_TYPE_ZIMO);// 自摸

		if(action != GameConstants.WIK_NULL){
			//添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data,_seat_index);
			
		}else{
			chr.set_empty();
		}
		
		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				
		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index,1,new int[]{_send_card_data},GameConstants.INVALID_SEAT);
		
		if(curPlayerStatus.has_zi_mo() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
			//见炮胡
			table.exe_jian_pao_hu(_seat_index,GameConstants.WIK_ZI_MO,_send_card_data);
			return ;
		}

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌
		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count > llcard) {
			int cbActionMask=table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index],_send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult);
			if(cbActionMask!=GameConstants.WIK_NULL){//有杠
				curPlayerStatus.add_action(GameConstants.WIK_GANG);//转转就是杠
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					//加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}
		
		if(curPlayerStatus.has_action()){//有动作
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
		}else{
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
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
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("没有这个操作");
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("不是当前玩家操作");
			return false;
		}
		
		// 是否已经响应
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			
			if(table._playerStatus[_seat_index].lock_huan_zhang()){
				 GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			}else{
				table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			
			return true;
		}

		// 执行动作
		switch (operate_code) {
			case GameConstants.WIK_GANG: // 杠牌操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//是否有抢杠胡
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
			}
			break;
			case GameConstants.WIK_ZI_MO: // 自摸
			{
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);
				
				int add_niao = 0;
				if(table.is_mj_type(GameConstants.GAME_TYPE_HZ)){
					if(table.GRR._cards_index[_seat_index][table._logic.get_magic_card_index(0)]==0){
						//手上没有红中
						add_niao=1;
					}
				}
				// 下局胡牌的是庄家
				table.set_niao_card(_seat_index,GameConstants.INVALID_VALUE,true,add_niao);// 结束后设置鸟牌
	
				table.GRR._chi_hu_card[_seat_index][0] = operate_card;
				
				table._banker_select = _seat_index;
				table.process_chi_hu_player_operate(_seat_index, operate_card,true);
				table.process_chi_hu_player_score(_seat_index,_seat_index,operate_card, true);
	
				// 记录
				table._player_result.zi_mo_count[_seat_index]++;
				
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	
				return true;
			}
		}

		return true;
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
