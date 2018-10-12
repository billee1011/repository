package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.service.SessionServiceImpl;

import io.netty.channel.Channel;

/**
 * 客户端socket链接检测
 * @author run
 *
 */
public class ClientSocketCheckTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(ClientSocketCheckTimer.class);
	
	@Override
	public void run() {
		
		long nowTime = System.currentTimeMillis();
		//扫描当前所有链接，处理超过10s没有通信的(客户端心跳是5s)
		
		if(SystemConfig.gameDebug==1) return;
		for(Session session : SessionServiceImpl.getInstance().getSessionMap().values()){
			
			//没有登录的
			long time = 0;
			if(session.getAccountID()==0){
				time = 1000*30L;
			}else{
				time = 1000*60L;
			}
			
			if(nowTime - session.getRefreshTime() > time){
				try{
					Channel channel = session.getChannel();
					if(channel!=null){
						channel.close();
					}
				}catch(Exception e){
					logger.error("error",e);
				}
			}
			
		}
		
		
	}

}
