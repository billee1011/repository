package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Room;
import com.cai.service.PlayerServiceImpl;

public class AddDiscardRunnable implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(AddDiscardRunnable.class);


	private int _room_id;
	private int _seat_index;

	private int _card_count;
	private int _card_data[];
	
	private boolean _send_client;
	
	public AddDiscardRunnable(int room_id,int seat_index,int card_count,int card_data[],boolean send_client,int maxCount){
		_card_data = new int[maxCount];
		
		_room_id = room_id;
		_seat_index = seat_index;
		_card_count = card_count;
		
		for(int i=0; i <_card_count; i++){
			
			_card_data[i] = card_data[i];
		}
		_send_client = send_client;
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
				table.runnable_add_discard(_seat_index, _card_count, _card_data,_send_client);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error",e);
		}
		
	}
	
	
	

}
