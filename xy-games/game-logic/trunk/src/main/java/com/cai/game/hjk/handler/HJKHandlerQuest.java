package com.cai.game.hjk.handler;

import com.cai.game.hjk.HJKTable;

public  class HJKHandlerQuest extends HJKHandler{

	protected int _status;
	protected int _send_card_data;

	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	
	public HJKHandlerQuest(){
	}
	
	public void reset_status(int status){
		_status = status;
	}
	
	@Override
	public void exe(HJKTable table) {
	
	
	}
	

	/***
	 * //用户操作
	 * 
	 * @param seat_index操作用户
	 * @param operate_code
	 * @param operated_index 被操作的用户
	 */
	public boolean handler_button_operate_card(HJKTable table, int seat_index,int operate_code,int operated_index) {
		return false;
	}
	
	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
	


		return true;
	}

}
