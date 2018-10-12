package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class AnimationByParaRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(AnimationByParaRunnable.class);

	
	private int _room_id;
	private int _timer_id;
	private Object _obj_one;
	private Object _obj_two;
	public AnimationByParaRunnable(int room_id,int timer_id,Object obj_one,Object obj_two){
		super(room_id);
		_room_id = room_id;
		_timer_id = timer_id;
		_obj_one=obj_one;
		_obj_two=obj_two;
	}
	
	
	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("踢人操作,房间["+_room_id+"]不存在");
				return ;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.animation_timer(_timer_id,_obj_one,_obj_two);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(room!=null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e),
						0L, SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}
	}

}
