package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Room;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;
import com.cai.service.PlayerServiceImpl;

public class DDZOutCardHandleRunnable implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(DDZOutCardHandleRunnable.class);


	private int _room_id;
	private int _seat_index;
	private DDZHandlerOutCardOperate _handler;
	private DDZTable _table;
	public DDZOutCardHandleRunnable(int room_id,int seat_index,DDZTable table,DDZHandlerOutCardOperate handler){
		_room_id = room_id;
		_seat_index = seat_index;
		_handler=handler;
		_table=table;
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
				_table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				_table.operate_player_status();
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
		}
		
	}
	
	
	

}
