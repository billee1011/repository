package com.cai.mj.handler.cs;

import org.apache.log4j.Logger;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandler;
import com.cai.mj.handler.MJHandlerHaiDi;

public class MJHandlerHaiDi_CS extends MJHandlerHaiDi {

	private static Logger logger = Logger.getLogger(MJHandlerHaiDi_CS.class);
	
	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
			table._playerStatus[i].chi_hu_round_valid();//可以胡了
		}
		
		curPlayerStatus.add_action(MJGameConstants.WIK_YAO_HAI_DI);
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
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		if(seat_index!=_seat_index){
			logger.error("[海底],操作失败,"+seat_index+"不是当前操作玩家");
			return false;
		}
		
		if(operate_code == MJGameConstants.WIK_NULL){
			//不要海底
			_seat_index = (_seat_index+1)%MJGameConstants.GAME_PLAYER;
			if(_seat_index == _start_index){
				table._banker_select = _start_index;
				
				// 流局
				table.handler_game_finish(table._banker_select, MJGameConstants.Game_End_DRAW);
				
				return true;
			}
			
			table.exe_hai_di(_start_index, _seat_index);
		}else{
			table.exe_yao_hai_di(_seat_index);
		}
		
		return true;
	}
}
