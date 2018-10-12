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
/**
 * 调度发牌
 *
 */
public class DisplayCardRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(DisplayCardRunnable.class);


	private int _room_id;
	private int _seat_index;
	private boolean _is_out_card;
	private int _card ;
	public DisplayCardRunnable(int room_id,int seat_index,int card,boolean is_out_card){
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_is_out_card = is_out_card;
		_card = card;
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
				table.display_card(_seat_index,_card, _is_out_card);
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
