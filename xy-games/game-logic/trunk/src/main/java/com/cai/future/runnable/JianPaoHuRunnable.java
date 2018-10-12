package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.service.PlayerServiceImpl;

public class JianPaoHuRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(JianPaoHuRunnable.class);

	
	private int _room_id;
	private int _seat_index;
	private int _card;
	private int _action;
	
	public JianPaoHuRunnable(int room_id,int seat_index,int action,int card){
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_card = card;
		_action = action;
	}
	
	
	@Override
	public void execute() {
		// TODO Auto-generated method stub
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
				table.handler_operate_card(_seat_index,_action,_card,0);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
		}
	}

}
