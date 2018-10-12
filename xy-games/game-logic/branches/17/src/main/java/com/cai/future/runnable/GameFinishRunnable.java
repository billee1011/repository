package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.mj.MJTable;
import com.cai.service.PlayerServiceImpl;

public class GameFinishRunnable implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(GameFinishRunnable.class);


	private int _room_id;
	private int _seat_index;
	private int _reason;
	
	public GameFinishRunnable(int room_id,int seat_index,int reason){
		_room_id = room_id;
		_seat_index = seat_index;
		_reason = reason;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度结束失败,房间["+_room_id+"]不存在");
				return ;
			}
			
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.handler_game_finish(_seat_index, _reason);
			}finally{
				roomLock.unlock();
				
			}
		} catch (Exception e) {
			logger.error("error",e);
		}
		
	}

}
