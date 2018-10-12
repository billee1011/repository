package com.cai.game.nn.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.nn.NNTable;

public class NNHandlerDispatchCard extends NNHandler{
	protected int _seat_index;
	protected int _send_card_data;
	
	protected int _type;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public NNHandlerDispatchCard(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int seat_index,int type){
		_seat_index = seat_index;
		_type= type;
	}
	
	@Override
	public void exe(NNTable table) {
	}
	
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(NNTable table,int seat_index, int card) {
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

//		if (card == MJGameConstants.ZZ_MAGIC_CARD && table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
//			table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
//			table.log_error("癞子牌不能出癞子");
//			return false;
//		}


		//出牌
		table.exe_out_card(_seat_index,card,GameConstants.WIK_NULL);

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(NNTable table,int seat_index) {
	


		return true;
	}

}
