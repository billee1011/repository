package com.cai.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.GameStatModel;
import com.cai.common.domain.JvmMemoryModel;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.domain.ProxyStatusModel;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.ThreadUtil;
import com.cai.core.DataThreadPool;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.ServerDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.RMIServiceImpl;

import javolution.util.FastMap;


/**
 * 数据统计
 * @author run
 *
 */
public class DataStatTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(DataStatTimer.class);
	
	@Override
	public void run() {
		try{
			
			//查询所有逻辑计算服,统计各个游戏的在线人数
			PerformanceTimer timer = new PerformanceTimer();
			GameStatModel gameStatModel = new GameStatModel();
			FastMap<Integer, ProxyGameServerModel> proxyMap = ServerDict.getInstance().getProxyGameServerModelDict();
			int proxyCount = 0;
			for (ProxyGameServerModel model : proxyMap.values()) {
				if(model.getStatus() == EServerStatus.ACTIVE || model.getStatus() == EServerStatus.REPAIR){
					proxyCount++;
				}
			}
			CountDownLatch latch = new CountDownLatch(proxyCount);
			for (ProxyGameServerModel model : proxyMap.values()) {
				if(model.getStatus() == EServerStatus.ACTIVE || model.getStatus() == EServerStatus.REPAIR){
					ProxStatThread thread = new ProxStatThread(latch, model, gameStatModel);
					thread.start();
				}
			}
			latch.await();
			long countOnline = gameStatModel.getOnline_player_num();
			StringBuilder buf = new StringBuilder();
			buf.append("账号在线人数:"+countOnline).append(",本次统计代理服数量:").append(proxyCount);
			MongoDBServiceImpl.getInstance().systemLog(ELogType.accountOnline, buf.toString(), countOnline,null, ESysLogLevelType.NONE);
			//System.out.println("统计花费时间:"+timer.getStr());
			
			
			
			
			//jvm
			JvmMemoryModel jvmMemoryModel = new JvmMemoryModel();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.jvmMemory, jvmMemoryModel.info(), jvmMemoryModel.getUse(),null, ESysLogLevelType.NONE);
			
			
			
			countThreadPool(DataThreadPool.getInstance().getTpe(), ELogType.centerDatapool);
			
			
			countThreadPool(GlobalExecutor.getScheduledThreadPoolExecutor(), ELogType.GlobalExecutor);
			countThreadPool(GlobalExecutor.getScheduledThreadPoolAsyn(), ELogType.GlobalExecutorASYN);
			countThreadPool(GlobalExecutor.getScheduledThreadPoolDB(), ELogType.GlobalExecutorDBASYN);
			
		}catch(Exception e){
			logger.error("error",e);
			MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}
		
	}
	
	
	/**
	 * 統計綫程池信息
	 * 
	 * @param scheduledThreadPoolExecutor
	 * @param logType
	 */
	public static void countThreadPool(ThreadPoolExecutor scheduledThreadPoolExecutor, ELogType logType) {
		BlockingQueue<Runnable> wapayQueue = scheduledThreadPoolExecutor.getQueue();
		long activeCount = scheduledThreadPoolExecutor.getActiveCount();
		long complementtaskCount = scheduledThreadPoolExecutor.getCompletedTaskCount();
		long taskCount = scheduledThreadPoolExecutor.getTaskCount();
		long queueSize = wapayQueue.size();
		int poolSize = scheduledThreadPoolExecutor.getPoolSize();
		StringBuilder buf = new StringBuilder();
		buf.append("计划执行任务:" + taskCount).append(",已执行任务:" + complementtaskCount).append(",活动线程数:" + activeCount).append(",总线程数:").append(poolSize)
				.append(",队列长度:" + queueSize).append("中心服");
		MongoDBServiceImpl.getInstance().systemLog_queue(logType, buf.toString(), activeCount, queueSize, ESysLogLevelType.NONE);

	}


}


class ProxStatThread extends Thread {

	private static Logger logger = LoggerFactory.getLogger(ProxStatThread.class);

	private CountDownLatch latch;
	private ProxyGameServerModel proxyGameServerModel;
	private GameStatModel gameStatModel;

	public ProxStatThread(CountDownLatch latch, ProxyGameServerModel proxyGameServerModel,GameStatModel gameStatModel) {
		this.latch = latch;
		this.proxyGameServerModel = proxyGameServerModel;
		this.gameStatModel = gameStatModel;
	}

	@Override
	public void run() {

		try {
			IProxyRMIServer proxyRMIServer = RMIServiceImpl.getInstance().getIProxyRMIByIndex(proxyGameServerModel.getProxy_game_id());
			ProxyStatusModel proxyStatusModel = proxyRMIServer.getProxyStatus();
			this.gameStatModel.addsocketNum(proxyStatusModel.getSocket_connect_num());
			this.gameStatModel.addOnlinePlayerNum(proxyStatusModel.getOnline_playe_num());
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			latch.countDown();
		}

	}

}


