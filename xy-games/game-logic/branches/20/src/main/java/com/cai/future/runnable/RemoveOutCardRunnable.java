package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.service.PlayerServiceImpl;

public class RemoveOutCardRunnable implements Runnable {

private static Logger logger = LoggerFactory.getLogger(RemoveOutCardRunnable.class);
	
	private int _room_id;
	private int _seat_index;
	private int _type;
	
	public RemoveOutCardRunnable(int room_id,int seat_index,int type){
		_room_id = room_id;
		_seat_index = seat_index;
		_type = type;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度删除出来的牌失败,房间["+_room_id+"]不存在");
				return ;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.runnable_remove_out_cards(_seat_index,_type);
			}finally{
				roomLock.unlock();
				
			}
		
		} catch (Exception e) {
			logger.error("error",e);
		}

	}

}
