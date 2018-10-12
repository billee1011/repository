package com.cai.mj.state;

import com.cai.mj.MJTable;
import com.google.protobuf.GeneratedMessage;

public class StateOperate implements IState<MJTable> {

	private static StateOperate hs = new StateOperate();

    private StateOperate(){
    }

    public static StateOperate getInstance(){
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

	//等待玩家出牌
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
