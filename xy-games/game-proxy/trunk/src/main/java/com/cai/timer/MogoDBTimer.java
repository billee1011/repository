package com.cai.timer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.core.SystemConfig;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.TempSmsService;

/**
 * mongo定时入库
 * @author run
 *
 */
public class MogoDBTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(MogoDBTimer.class);
	
	private static final int LOG_SIZE = 10000;
	
	/**
	 * 临时数组
	 */
	private ArrayList logArrayList = new ArrayList<>(LOG_SIZE);
	
	private PerformanceTimer timer = new PerformanceTimer();
	
	@Override
	public void run() {
		handle();
	}
	
	public void handle(){
		try {
			timer.reset();
			//取出队列中的所有
			MongoDBServiceImpl.getInstance().getLogQueue().drainTo(logArrayList);
			if(logArrayList.size()==0)
				return;
			//批量入库
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			mongoDBService.getMongoTemplate().insertAll(logArrayList);
			
			if(timer.get()>10000L){
				String str = "Slowly process mogodb入库时间过长:"+timer.getStr();
				logger.warn(str);
				
				
				MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.mongoSlow, str, 0L, null);
				
				Set<String> phoneSet = new HashSet<String>();
				phoneSet.add("13670139534");

				String content = SystemConfig.localip + "proxy:" + SystemConfig.proxy_index + str;
				TempSmsService.batchSendMsg(phoneSet, content);

				logger.error("Slowly process " + content);
			}
			
			if(logArrayList.size()>LOG_SIZE){
				String str = "Slowly process mongodb每秒入库数量过大请调整参数,数量:"+logArrayList.size();
				logger.warn(str);
				
				Set<String> phoneSet = new HashSet<String>();
				phoneSet.add("13670139534");

				String content = SystemConfig.localip + "proxy:" + SystemConfig.proxy_index + str;
				TempSmsService.batchSendMsg(phoneSet, content);

				logger.error("Slowly process " + content);
			}
			
		} catch (Exception e) {
			logger.error("error",e);
		}finally{
			logArrayList.clear();
		}
		
	}
	
	

}
