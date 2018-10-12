package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Room;
import com.cai.game.mj.MJTable;
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
		//这两个是申请解散的时候用的，不能切换状态
		if((GameConstants.Game_End_RELEASE_WAIT_TIME_OUT==reason) || (GameConstants.Game_End_RELEASE_PLAY_TIME_OUT == reason)){
			return ;
		}
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
		
		if(table==null){
			logger.info("调度创建失败,房间["+_room_id+"]不存在");
			return ;
		}
		table.exe_finish(reason);
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
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
			logger.error("error"+_room_id,e);
		}
		
	}

}
