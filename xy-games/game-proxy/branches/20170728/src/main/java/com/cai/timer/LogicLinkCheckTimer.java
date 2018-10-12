package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.service.ClientServiceImpl;

/**
 * 中转与客户端连接检测
 * @author run
 *
 */
public class LogicLinkCheckTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(LogicLinkCheckTimer.class);
	
	private long linkTimes = 0;
	@Override
	public void run() {
		
			boolean active =  ClientServiceImpl.getInstance().isLinkActive();
			if(!active){
				linkTimes++;
				StringBuilder buf = new StringBuilder();
				boolean flag = ClientServiceImpl.getInstance().resetLink();
				buf.append("检测到逻辑服断线重新链接逻辑服,");
				if(flag){
					buf.append("链接成功!");
					linkTimes = 0;
				}else{
					buf.append("链接失败!");
					buf.append("重连次数:"+linkTimes);
				}
				
				logger.info(buf.toString());
			}else{
				linkTimes = 0;
			}
		
		
		
	}

}
