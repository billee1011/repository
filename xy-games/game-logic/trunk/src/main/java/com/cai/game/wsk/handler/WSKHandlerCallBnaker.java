package com.cai.game.wsk.handler;
import com.cai.game.wsk.AbstractWSKTable;

public class WSKHandlerCallBnaker<T extends AbstractWSKTable> extends AbstractWSKHandler<T> {


	
	@Override
	public void exe(T table) {
	}
	

	public  boolean handler_call_banker(T table,int seat_index,int call_action){
		

		return true;
	}
	@Override
	public boolean handler_player_be_in_room(T table,int seat_index) {		
		return true;
	}
	
	

}
