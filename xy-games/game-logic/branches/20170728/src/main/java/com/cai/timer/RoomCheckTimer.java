package com.cai.timer;

import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;


/**
 * 房间检测
 * @author run
 *
 */
public class RoomCheckTimer extends TimerTask{
	
	private static Logger logger = LoggerFactory.getLogger(RoomCheckTimer.class);
	@Override
	public void run() {
		try{
			
			 long now = System.currentTimeMillis();
			 Map<Integer, Room> roomMap = PlayerServiceImpl.getInstance().getRoomMap();
			 for(Room m : roomMap.values()){
				 long k = now - m.getLast_flush_time();
				 //1小时
				 if(k>3600000L){			 
					 //日志
					 StringBuffer buf = new StringBuffer();
					 buf.append("系统释放房间,房间id:").append(m.getRoom_id()).append(",玩家列表:");
					 int j = 0;
					 for(Player player : m.get_players()){
						 if(player!=null){
							 j++;
							 if(j>1){
								 buf.append("|");
							 }
							 buf.append(player.getAccount_id());
						 }
					 }
					 MongoDBServiceImpl.getInstance().systemLog(ELogType.sysFreeRoom, buf.toString(), (long)m.getRoom_id(), null, ESysLogLevelType.NONE);
					//TODO 释放房间

					 m.process_release_room();
				 }
			 }
			 
			 MongoDBServiceImpl.getInstance().systemLog(ELogType.roomLogicNumber, "逻辑服房间数",(long) roomMap.size(),null, ESysLogLevelType.NONE);
			
			
			
		}catch(Exception e){
			logger.error("error",e);
		}
		
	}
}


