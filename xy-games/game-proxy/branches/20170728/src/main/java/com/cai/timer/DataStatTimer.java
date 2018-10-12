package com.cai.timer;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.JvmMemoryModel;
import com.cai.common.domain.Room;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.SessionServiceImpl;


/**
 * 数据统计
 * @author run
 *
 */
public class DataStatTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(DataStatTimer.class);
	
	@Override
	public void run() {
		//在线玩家,socket链接数日志
		try{
			SessionServiceImpl sessionServiceImpl = SessionServiceImpl.getInstance();
			long session_count = sessionServiceImpl.getSessionMap().size();
			long online_count = sessionServiceImpl.getOnlineSessionMap().size();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.socketConnect, null, session_count,null, ESysLogLevelType.NONE);
			MongoDBServiceImpl.getInstance().systemLog(ELogType.onlinePlayer, null, online_count,null, ESysLogLevelType.NONE);

			//消息队等待情况
			ThreadPoolExecutor tpe = RequestHandlerThreadPool.getInstance().getTpe();
			long activeCount = tpe.getActiveCount();
			long queueSize = tpe.getQueue().size();
			StringBuilder buf = new StringBuilder();
			buf.append("计划执行任务:"+tpe.getTaskCount()).append(",已执行任务:"+tpe.getCompletedTaskCount()).append(",活动线程数:"+activeCount)
			.append(",总线程数:").append(tpe.getPoolSize()).append(",队列长度:"+queueSize);
			MongoDBServiceImpl.getInstance().systemLog(ELogType.requestPool, buf.toString(), activeCount,queueSize, ESysLogLevelType.NONE);
			
			
			//jvm
			JvmMemoryModel jvmMemoryModel = new JvmMemoryModel();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.jvmMemory, jvmMemoryModel.info(), jvmMemoryModel.getUse(),null, ESysLogLevelType.NONE);
			
		}catch(Exception e){
			logger.error("error",e);
		}
		
	}

}
