package com.cai.mj.state;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.domain.Player;
import com.cai.mj.MJTable;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.RoomRequest;

public class StateWaitting implements IState<MJTable> {
	private static StateWaitting hs = new StateWaitting();

    private StateWaitting(){
    }

    public static StateWaitting getInstance(){
        return hs;
    }
	
	public boolean on_enter(MJTable owner){
		for(int i=0; i < MJGameConstants.GAME_PLAYER; i++){
			owner._player_ready[i]  = 0;
			
		}
		
		return true;
	}
	public boolean on_exe(MJTable owner){
		
		return true;
	}
	public boolean on_exit(MJTable owner){
		
		return true;
	}
	
	//
	public boolean on_message(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		this.handler_player_enter_room(owner, account_id, msg);
		
		this.handler_player_ready(owner, account_id, msg);
		return true;
	}
	
	
	public boolean on_global(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		
		return true;
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	private boolean handler_player_enter_room(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(account_id);
		boolean flag =  owner.handler_enter_room(player);
		
		
		return false;
		
	}
	//准备
	private boolean handler_player_ready(MJTable owner,long account_id,com.google.protobuf.GeneratedMessage msg){
		
		Player player = owner.get_player(account_id);
		if(player==null){
			return false;
		}
		
		//逻辑处理
		return owner.handler_player_ready(player.get_seat_index());
	}
	
}
