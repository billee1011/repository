package com.cai.net.handler;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.IoStatisticsModel;
import com.cai.core.Global;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.RequestInvoker;
import com.cai.core.RequestWrapper;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.net.server.GameSocketServer;
import com.cai.net.util.ProcesserManager;
import com.cai.net.util.RequestClassHandlerBinding;
import com.cai.service.C2SHandlerServiceImpl;
import com.cai.service.S2SHandlerServiceImp;
import com.cai.service.SessionServiceImpl;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.ReqExecutor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.S2SCommonProto;

public class TcpGameServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger logger = LoggerFactory.getLogger(TcpGameServerHandler.class);
	private static final AtomicInteger count = new AtomicInteger(0);
	
	private static AtomicLong actomicLong = new AtomicLong(1);
	public final static AttributeKey<Long> SESSION_ID = AttributeKey.valueOf("SESSION_ID");
	

	/**
	 * 
	 */
	private final FieldDescriptor fieldDescriptor;
	
	public TcpGameServerHandler() {
		ExtensionRegistry registry = ExtensionRegistry.newInstance();
		Protocol.registerAllExtensions(registry);
		fieldDescriptor = registry.findExtensionByName("s2sRequest").descriptor;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		

		//父类
		Request request = (Request)msg;
		int requestType = request.getRequestType().getNumber();
		
		Long session_id  = ctx.channel().attr(SESSION_ID).get();
		if(session_id==null){
			logger.error("发现异常,session_id=null");
			return;
		}

		Session session = SessionServiceImpl.getInstance().getSession(session_id);
		session.setRefreshTime(System.currentTimeMillis());
		
		
		if(request.getRequestType()== RequestType.ROOM) {
			RequestClassHandlerBinding binding = ProcesserManager.getRequestClassHandlerBinding(requestType);
			if(binding==null){
				logger.warn("decoder fail,requestType="+requestType+"is not find!");
				return;
			}
			RequestWrapper wrapper = new RequestWrapper((Request)msg,ctx.channel(),binding,session);
			Global.executeLogicRoomThread(new RequestInvoker(wrapper,ctx));
			return;
		}
		
		if(request.getRequestType() == RequestType.S2S){
			session.setRefreshTime(System.currentTimeMillis());
			RequestHandlerThreadPool.getInstance().addTask(new ReqExecutor((S2SCommonProto) request.getField(fieldDescriptor),session));
			//流量统计
			GameSocketServer.statistics.getInMessages().incrementAndGet();
			GameSocketServer.statistics.getInBytes().addAndGet(request.toByteArray().length);
			return;
		}
		
		
		RequestClassHandlerBinding binding = ProcesserManager.getRequestClassHandlerBinding(requestType);
		if(binding==null){
			logger.warn("decoder fail,requestType="+requestType+"is not find!");
			return;
		}
	
		session.setRefreshTime(System.currentTimeMillis());
		RequestWrapper wrapper = new RequestWrapper((Request)msg,ctx.channel(),binding,session);
		
		if(SystemConfig.gameDebug==1){
			if(requestType!=RequestType.HEAR.getNumber()){
//				System.out.println("逻辑计算服Decoder==========>"+msg);
			}
		}
		
		RequestHandlerThreadPool.getInstance().addTask(new RequestInvoker(wrapper,ctx));
		
		//流量统计
		GameSocketServer.statistics.getInMessages().incrementAndGet();
		GameSocketServer.statistics.getInBytes().addAndGet(request.toByteArray().length);
		
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//super.exceptionCaught(ctx, cause);
		//logger.error("error", cause);
		ctx.close();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		try {
			logger.warn("新加入链接:" + ctx.channel());
		} catch (Exception e) {
			logger.error("e", e);
		}
		
		long sessionid = actomicLong.getAndIncrement();
		//不太可能出现
		if(sessionid>Long.MAX_VALUE-10000){
			actomicLong.set(1);
			logger.error("sessionid最大值了，重置");
		}
		
		ctx.channel().attr(SESSION_ID).set(sessionid);
		//记录session
		Session session = new Session();
		session.sessionId = sessionid;
		session.channel = ctx.channel();
		session.clientIP = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
		
		SessionServiceImpl.getInstance().getSessionMap().put(session.sessionId, session);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		logger.warn("断开链接:" + ctx.channel());
		
		Long sessionid = ctx.channel().attr(SESSION_ID).get();
		SessionServiceImpl.getInstance().getSessionMap().remove(sessionid);
	}

//	@Override
//	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		ctx.fireChannelActive();
//		count.incrementAndGet();
//	}
//
//	@Override
//	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//
//		ctx.fireChannelInactive();
//		count.decrementAndGet();
//	}

}
