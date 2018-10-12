package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.PerformanceTimer;
import com.cai.domain.Session;
import com.cai.service.SessionServiceImpl;

/**
 * 与代理服的心条检测
 * @author run
 *
 */
public class ProxySocketCheckTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(ProxySocketCheckTimer.class);
	
	private PerformanceTimer timer = new PerformanceTimer();
	
	@Override
	public void run() {
		//System.out.println("心跳检测");
		timer.reset();
		
		long nowTime = System.currentTimeMillis();
		
		for(Session session : SessionServiceImpl.getInstance().getSessionMap().values()){
			if(session.getChannel()==null)
				continue;
			try{
				long time = nowTime - session.getRefreshTime();
				if(time>12000L){
					logger.warn("逻辑计算服关闭没有心跳的链接:"+session.getChannel());
					session.getChannel().close();
				}
			}catch(Exception e){
				logger.error("error",e);
			}
			
			
		}
		
		
		
		
		
		
		
		
		
		
	}

}
