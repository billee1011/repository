package com.cai.mj.state;

import com.cai.mj.MJTable;
import com.google.protobuf.GeneratedMessage;

public class StateDispatchCard implements IState<MJTable> {

	private static StateDispatchCard hs = new StateDispatchCard();

    private StateDispatchCard(){
    }

    public static StateDispatchCard getInstance(){
        return hs;
    }
    
    
	@Override
	public boolean on_enter(MJTable owner) {
		// TODO Auto-generated method stub
		
		//发牌
		
		return false;
	}

	@Override
	public boolean on_exe(MJTable owner) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean on_exit(MJTable owner) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean on_message(MJTable owner, long account_id, GeneratedMessage msg) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean on_global(MJTable owner, long account_id, GeneratedMessage msg) {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}
