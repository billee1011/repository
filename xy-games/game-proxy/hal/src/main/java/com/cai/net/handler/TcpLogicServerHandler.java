package com.cai.net.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.net.server.GameSocketServer;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.SessionServiceImpl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;

public class TcpLogicServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger logger = LoggerFactory.getLogger(TcpLogicServerHandler.class);

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		
		//TODO 把逻辑服收到的消息转给客户端
		//找到session_id
		
		Request request = (Request)msg;
		//logger.info("收到逻辑服消息:"+request.toByteArray().length+"b\n"+request);
		
		int requestType = request.getRequestType().getNumber();
		Long session_id = request.getProxSeesionId();
		Session session = SessionServiceImpl.getInstance().getSession(session_id);
		if(session==null)
			return;
		
		Response response = request.getExtension(Protocol.response);
		
		if(SystemConfig.gameDebug==1){
			System.out.println("=====逻辑转发>>>>>>\n"+response.toString());
		}
		ChannelFuture wf =session.getChannel().writeAndFlush(response);
		wf.addListener(new ChannelFutureListener()
		{
			public void operationComplete(ChannelFuture future) throws Exception
			{
				if (!future.isSuccess())
				{
					logger.warn("转发逻辑服消息给客户端失败:request:" + request.getRequestType() + ",response:"+response.getResponseType());
				}
			}
		});
		
		Account account = session.getAccount();
		if(false&&account!=null){
			long account_id = account.getAccount_id();
			//日志
			StringBuilder buf = new StringBuilder();
			buf.append(response.toByteArray().length).append("B")
			.append("|sessinId:").append(session.getSessionId())
			.append("|accountId:"+account_id).append("|")
			.append("转发逻辑服消息|")
			.append(response.toString());
			long v1 = response.getResponseType().getNumber();
			MongoDBServiceImpl.getInstance().player_log(account_id, ELogType.response,buf.toString(), v1, null, session.getClientIP());
		}
		
		
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("error", cause);
		ctx.close();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);

		try {
			logger.info("新加入链接逻辑服:" + ctx.channel());
		} catch (Exception e) {
			logger.error("e", e);
		}

	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		 logger.info("断开链接逻辑服:" + ctx.channel());
	}

	// @Override
	// public void channelActive(ChannelHandlerContext ctx) throws Exception {
	// ctx.fireChannelActive();
	// }
	//
	// @Override
	// public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	//
	// ctx.fireChannelInactive();
	// }

}
