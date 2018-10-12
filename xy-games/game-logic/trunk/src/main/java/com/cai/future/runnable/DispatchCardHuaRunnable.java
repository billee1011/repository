package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.mj.hainan.hainanmj.MJTable_HaiNan;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
/**
 * 调度发牌花牌
 *
 */
public class DispatchCardHuaRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(DispatchCardHuaRunnable.class);


	private int _room_id;
	private int _seat_index;
	private boolean _tail;
	private int _type;
	private int _card_number;
	public DispatchCardHuaRunnable(int room_id,int seat_index,int type,boolean tail,int num){
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_tail = tail;
		_type = type;
		_card_number = num;
	}
	
	
	
	@Override
	public void execute() {
		try {
			MJTable_HaiNan table = (MJTable_HaiNan)PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度发牌失败,房间["+_room_id+"]不存在");
				return ;
			}
			//logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.dispatch_card_data(_seat_index,_type, _tail,_card_number);
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