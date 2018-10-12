package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.service.PlayerServiceImpl;
/**
 * 调度发牌
 *
 */
public class DaYiPiaoRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(DaYiPiaoRunnable.class);


	private int _room_id;
	private int _seat_index;
	private boolean _tail;
	private int _type;
	public DaYiPiaoRunnable(int room_id,int seat_index,int type,boolean tail){
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_tail = tail;
		_type = type;
	}
	
	
	
	@Override
	public void execute() {
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
				table.runnable_da_yi_piao(_seat_index,_type, _tail);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
			
		}
		
	}
	
	
	

}
