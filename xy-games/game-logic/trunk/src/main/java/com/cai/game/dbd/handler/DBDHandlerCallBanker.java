package com.cai.game.dbd.handler;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DDZOutCardHandleRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.dbd.AbstractDBDTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.ddz.DdzRsp.CallBankerDDZ;
import protobuf.clazz.ddz.DdzRsp.CallBankerResult;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;
import protobuf.clazz.ddz.DdzRsp.TableResponseDDZ;

public class DBDHandlerCallBanker<T extends AbstractDBDTable> extends AbstractDBDHandler<T> {
	protected int _seat_index;
	protected int _game_status;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	
	public DBDHandlerCallBanker(){
	}
	
	public void reset_status(int seat_index,int game_status){
		_seat_index = seat_index;
		_game_status= game_status;
	}
	
	@Override
	public void exe(T table) {
		
	}
	
	


	
	@Override
	public boolean handler_player_be_in_room(T table,int seat_index) {

		return true;
	}
	
	/**
	 * @param get_seat_index
	 * @param call_banker -1为没有进行叫地主操作，0为不叫地主，大于0为叫地主
	 * @param qiang_bangker -1为没有进行抢地主操作，0为不抢地主，大于0为抢地主
	 * @return
	 */
	public  boolean handler_call_banker(T table,int seat_index,int call_banker_score)
	{

		return true;
	}

}
