package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.game.mj.MJTable;
import com.cai.service.PlayerServiceImpl;

public class RevomeHunMiddleCardRunnable implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(RevomeHunMiddleCardRunnable.class);
	
	private int _room_id;
	private int _seat_index;
	
	public RevomeHunMiddleCardRunnable(int room_id,int seat_index){
		_room_id = room_id;
		_seat_index = seat_index;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度hun结束失败,房间["+_room_id+"]不存在");
				return ;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.runnable_remove_hun_middle_cards(_seat_index);
			}finally{
				roomLock.unlock();
				
			}
		
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
		}

	}

}
