package com.cai.game.fls.handler;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.fls.FLSTable;

public class FLSHandlerHaiDi extends FLSHandler {

	private static Logger logger = Logger.getLogger(FLSHandlerHaiDi.class);
	
	protected int _start_index;
	protected int _seat_index;
	
	public void reset_status(int start_index,int seat_index){
		_start_index = start_index;
		_seat_index = seat_index;
	}
	
	@Override
	public void exe(FLSTable table) {
		// TODO Auto-generated method stub
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		for(int i=0; i<table.getTablePlayerNumber();i++){
			table._playerStatus[i].chi_hu_round_valid();//可以胡了
		}
		
		curPlayerStatus.add_action(GameConstants.WIK_YAO_HAI_DI);
		curPlayerStatus.add_yao_hai_di();
		
		table.operate_player_action(_seat_index, false);
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
	public boolean handler_operate_card(FLSTable table,int seat_index, int operate_code, int operate_card){
		if(seat_index!=_seat_index){
			logger.error("[海底],操作失败,"+seat_index+"不是当前操作玩家");
			return false;
		}
		
		if(operate_code == GameConstants.WIK_NULL){
			//add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			
			//不要海底
			_seat_index = (_seat_index+1)%table.getTablePlayerNumber();
			if(_seat_index == _start_index){
				table._banker_select = _start_index;
				
				// 流局
				table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);
				
				return true;
			}
			
			table.exe_hai_di(_start_index, _seat_index);
		}else{
			table.exe_yao_hai_di(_seat_index);
		}
		
		return true;
	}
}
