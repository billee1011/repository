package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.service.PlayerServiceImpl;

public class CreateTimeOutRunnable implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(CreateTimeOutRunnable.class);
	
	private int _room_id;
	
	public CreateTimeOutRunnable(int room_id){
		_room_id = room_id;
	}
	
	
	@Override
	public void run() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				return ;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.runnable_create_time_out();
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
		}
	}

}
