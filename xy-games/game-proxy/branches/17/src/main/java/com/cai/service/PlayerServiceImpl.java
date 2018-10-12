package com.cai.service;

import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.xianyi.framework.server.AbstractService;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import protobuf.clazz.Protocol.Response;

public class PlayerServiceImpl extends AbstractService {
	
	private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);

	private static PlayerServiceImpl instance = null;

	private PlayerServiceImpl() {
	}

	public static PlayerServiceImpl getInstance() {
		if (null == instance) {
			instance = new PlayerServiceImpl();
		}
		return instance;
	}
	
	
	/**
	 * 发送消息
	 * @param session
	 * @param response
	 */
	public void sendAccountMsg(Session session,Response response){
		
		if(SystemConfig.gameDebug==1){
			System.out.println("转发服Encoder2<========="+response.toByteArray().length+"b\n"+response);
		}
		
		
		ChannelFuture wf = session.getChannel().writeAndFlush(response);
		wf.addListener(new ChannelFutureListener()
		{
			public void operationComplete(ChannelFuture future) throws Exception
			{
				if (!future.isSuccess())
				{
					logger.warn("转发服给客户端消息失败:response:" + response);
				}
			}
		});
		
		
		//日志
		Long account_id = null;
		Account account = session.getAccount();
		if(false && account!=null){
			account_id = session.getAccount().getAccount_id();
			String ip = session.getClientIP();
			StringBuffer buf = new StringBuffer();
			buf.append(response.toByteArray().length).append("B")
			.append("|sessionId:").append(session.getSessionId())
			.append("|accountId:"+account.getAccount_id()).append("|")
			.append("转发服消息|")
			.append(response.toString());
			long v1 = response.getResponseType().getNumber();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.response,buf.toString(), v1, null, ip);
		}
		//
		
	}
	

	@Override
	protected void startService() {
		// TODO Auto-generated method stub

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

}
