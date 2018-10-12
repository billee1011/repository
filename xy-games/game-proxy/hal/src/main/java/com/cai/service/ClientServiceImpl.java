package com.cai.service;

import java.util.SortedMap;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.net.server.GameLogicClientServer;
import com.cai.timer.ClientSocketCheckTimer;
import com.cai.timer.DataStatTimer;
import com.cai.timer.LogicHeartTimer;
import com.cai.timer.LogicLinkCheckTimer;
import com.xianyi.framework.server.AbstractService;

import io.netty.channel.Channel;
import protobuf.clazz.Protocol.Request;

public class ClientServiceImpl extends AbstractService{
	
	private static Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);
	
	private static ClientServiceImpl instance = null;
	
	private Channel channel;
	
	private Timer timer;

	private ClientServiceImpl() {
		timer = new Timer("Timer-ClientServiceImpl Timer");
		//建立链接
		channel = GameLogicClientServer.getChannel(SystemConfig.loginc_socket_ip, SystemConfig.logic_sockcet_port);
	}

	public static ClientServiceImpl getInstance() {
		if (null == instance) {
			instance = new ClientServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {
		timer.schedule(new LogicLinkCheckTimer(), 6000L, 10000L);//链接检测
		timer.schedule(new LogicHeartTimer(), 6000L, 5000L);//发给逻辑计算服的心跳
		timer.schedule(new ClientSocketCheckTimer(), 10000L, 10000L);//客户端链接检测
		timer.schedule(new DataStatTimer(), 60000L, 60000L);//在线数据统计
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 链接是否有用
	 * @return
	 */
	public boolean isLinkActive(){
		if(channel==null)
			return false;
		return channel.isActive();
	}
	
	public boolean resetLink(){
		//建立链接
		channel = GameLogicClientServer.getChannel(SystemConfig.loginc_socket_ip, SystemConfig.logic_sockcet_port);
		return isLinkActive();
	}
	
	/**
	 * 发送消息给逻辑服
	 * @param request
	 */
	public boolean sendMsg(Request request){
		boolean flag = true;
		try{
			if (channel != null) {
				channel.writeAndFlush(request).sync();
			} else {
				flag = false;
				logger.warn("消息发送失败,与逻辑服的连接尚未建立!");
			}
		}catch(Exception e){
			//logger.error("error",e);
			flag = false;
		}
		
		return flag;
	}
	
}
