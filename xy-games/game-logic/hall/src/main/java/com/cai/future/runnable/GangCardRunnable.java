package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.game.hh.HHTable;
import com.cai.service.PlayerServiceImpl;
/**
 * 调度发牌
 *
 */
public class GangCardRunnable implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(DispatchCardRunnable.class);


	private int _room_id;
	private int _seat_index;
	private int _provide_player;
	private int _center_card;
	private int _action;
	private int _type;
	private boolean _depatch;
	private boolean _self;
	private boolean _d;

	public GangCardRunnable(int room_id,int seat_index,int provide_player,int center_card, int action, int type,boolean depatch, boolean self, boolean d){
		_room_id = room_id;
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		_depatch = depatch;
		_self = self;
		_d = d;
	}
	
	
	
	@Override
	public void run() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度发牌失败,房间["+_room_id+"]不存在");
				return ;
			}
			//logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.runnable_gang_card_data(_seat_index,_provide_player,_center_card,_action,_type,_depatch,_self,_d);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
		}
		
	}
	
	
	

}
