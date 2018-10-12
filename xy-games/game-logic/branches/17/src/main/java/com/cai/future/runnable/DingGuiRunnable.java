package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.mj.MJTable;
import com.cai.service.PlayerServiceImpl;


public class DingGuiRunnable implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(DingGuiRunnable.class);
	
	private int _room_id;
	private int _seat_index;
	
	public DingGuiRunnable(int room_id,int seat_index){
		_room_id = room_id;
		_seat_index = seat_index;

	}
	
	@Override
	public void run() {
		try {
			MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度定鬼失败,房间["+_room_id+"]不存在");
				return ;
			}
			//logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.runnable_ding_gui(_seat_index);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error",e);
		}
		
	}
}
