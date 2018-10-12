package com.cai.mj.state;

import com.cai.mj.MJTable;

public class StateStartGame implements IState<MJTable> {
	private static StateStartGame hs = new StateStartGame();

    private StateStartGame(){
    }

    public static StateStartGame getInstance(){
        return hs;
    }
    
	public boolean on_enter(MJTable owner){
		//
		owner.reset_init_data();
		
		return true;
	}
	public boolean on_exe(MJTable owner){
		boolean d = owner.handler_game_start();
		
		
		return true;
	}
	public boolean on_exit(MJTable owner){
		
		return true;
	}
	
	
	public boolean on_message(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		
		return true;
	}
	
	
	public boolean on_global(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		
		return true;
	}
	
}
