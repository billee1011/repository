package com.cai.future.runnable;

import java.util.concurrent.TimeUnit;
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
import com.cai.future.GameSchedule;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.CallBankerResult;

public class DDZAutoOpreateRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(DDZAutoOpreateRunnable.class);


	private int _room_id;
	private int _seat_index;
	private DDZTable _table;
	public DDZAutoOpreateRunnable(int room_id,DDZTable table,int seat_index){
		super(room_id);
		_room_id = room_id;
		_table=table;
		_seat_index=seat_index;
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
				if(_table._game_status == GameConstants.GS_CALL_BANKER){
					_table._auto_call_banker_scheduled=GameSchedule.put(new DDZAutoCallbankerRunnable(_table.getRoom_id(), _table,_table._current_player,1),
							1, TimeUnit.SECONDS);
				}else if(_table._game_status == GameConstants.GS_DDZ_ADD_TIMES){
					_table.auto_add_time(_seat_index);
				}else if(_table._game_status == GameConstants.GS_MJ_PLAY){
					_table.auto_out_card(_seat_index);
				}
				
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
