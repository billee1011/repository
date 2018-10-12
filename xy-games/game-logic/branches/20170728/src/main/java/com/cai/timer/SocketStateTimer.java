package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.IoStatisticsModel;
import com.cai.service.MongoDBServiceImpl;

/**
 * 客户端与代理服的socket统计
 * @author run
 *
 */
public class SocketStateTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(SocketStateTimer.class);
	
	private IoStatisticsModel statistics;
	
	//上一次的记录
	private long lastInMessages; 
	
	private long lastOutMessages;
	
	private long lastInBytes;
	
	private long lastOutBytes;
	
	
	
	public SocketStateTimer(IoStatisticsModel statistics){
		this.statistics = statistics;
	}
	
	
	@Override
	public void run() {
		//当前的
		long curInMessages = statistics.getInMessages().get();
		long curOutMessages = statistics.getOutMessages().get();
		long curInBytes = statistics.getInBytes().get();
		long curOutBytes = statistics.getOutBytes().get();
		
		//本次变化的
		long changeInMessages = curInMessages - lastInMessages;
		long changeOutMessages = curOutMessages - lastOutMessages;
		long changeInBytes = curInBytes - lastInBytes;
		long changeOutBytes = curOutBytes - lastOutBytes;
		
		//记录
		lastInMessages = curInMessages;
		lastOutMessages = curOutMessages;
		lastInBytes = curInBytes;
		lastOutBytes = curOutBytes;
		
		
		//入库
		MongoDBServiceImpl.getInstance().systemLog(ELogType.socketStatePack, null, changeInMessages,changeOutMessages, ESysLogLevelType.NONE);
		MongoDBServiceImpl.getInstance().systemLog(ELogType.socketStateBytes, null, changeInBytes,changeOutBytes, ESysLogLevelType.NONE);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
