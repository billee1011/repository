package com.cai.game.mj.state;

import com.cai.game.mj.MJTable;

public class StateXiaohu implements IState<MJTable> {
	private static StateXiaohu hs = new StateXiaohu();

    private StateXiaohu(){
    }

    public static StateXiaohu getInstance(){
        return hs;
    }
    
	public boolean on_enter(MJTable owner){
		
		
		return true;
	}
	public boolean on_exe(MJTable owner){
		
		return true;
	}
	public boolean on_exit(MJTable owner){
		
		return true;
	}
	
	//等待小胡操作
	public boolean on_message(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		
		return true;
	}
	public boolean on_global(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		
		return true;
	}
}
