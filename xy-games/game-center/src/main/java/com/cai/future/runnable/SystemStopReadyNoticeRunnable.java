package com.cai.future.runnable;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.GlobalModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.PublicServiceImpl;

/**
 * 停服准备定时通知
 * @author run
 *
 */
public class SystemStopReadyNoticeRunnable implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(SystemStopReadyNoticeRunnable.class);

	@Override
	public void run() {

		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		if(!globalModel.isSystemStopReady())
			return;
		
		Date stopDate = globalModel.getStopDate();
		if(stopDate==null)
			return;
		
		try{
			int k = MyDateUtil.minuteBetween(MyDateUtil.getNow(), stopDate);
			CenterRMIServerImpl centerRMIServerImpl = SpringService.getBean(CenterRMIServerImpl.class);
			if(k>=1){
				centerRMIServerImpl.sendGameNotice(0, "亲爱的玩家你们好,系统将在"+k+"分钟后停机维护,牌局将会强制结算", 2);
			}else{
				centerRMIServerImpl.sendGameNotice(0, "亲爱的玩家你们好,系统即将停机维护,牌局将会强制结算", 2);
			}
			
			ScheduledFuture future = GameSchedule.put(new SystemStopReadyNoticeRunnable(), 60L, TimeUnit.SECONDS);
			globalModel.setSystemStopNoticeFuture(future);
			
			
		}catch(Exception e){
			logger.error("error",e);
		}
		
		
		
	}
	
	

}
