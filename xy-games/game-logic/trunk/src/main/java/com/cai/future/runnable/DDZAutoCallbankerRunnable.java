package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.PBUtil;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.CallBankerResult;

public class DDZAutoCallbankerRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(DDZAutoCallbankerRunnable.class);


	private int _room_id;
	private int _seat_index;
	private int _type;
	private DDZTable _table;
	public DDZAutoCallbankerRunnable(int room_id,DDZTable table,int seat_index,int type){
		super(room_id);
		_room_id = room_id;
		_table=table;
		_seat_index=seat_index;
		_type=type;
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
				_table.call_banker(_seat_index,_type);
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
