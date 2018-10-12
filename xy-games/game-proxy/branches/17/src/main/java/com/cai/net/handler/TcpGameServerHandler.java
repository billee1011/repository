package com.cai.net.handler;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.RequestInvoker;
import com.cai.core.RequestWrapper;
import com.cai.core.SystemConfig;
import com.cai.domain.IpFirewallModel;
import com.cai.domain.Session;
import com.cai.net.server.GameSocketServer;
import com.cai.service.FirewallServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.xianyi.framework.net.util.ProcesserManager;
import com.xianyi.framework.net.util.RequestClassHandlerBinding;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

public class TcpGameServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger logger = LoggerFactory.getLogger(TcpGameServerHandler.class);
	private static final AtomicInteger count = new AtomicInteger(0);
	
	
	private static AtomicLong actomicLong = new AtomicLong(1);
	public final static AttributeKey<Long> SESSION_ID = AttributeKey.valueOf("SESSION_ID");
	

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		
		
		//request与处理器
		//System.out.println("test:::"+msg);
		//父类
		Request request = (Request)msg;
		int requestType = request.getRequestType().getNumber();
		
		//TODO 临时处理，找不到直接转到逻辑服处理
		RequestClassHandlerBinding binding = ProcesserManager.getRequestClassHandlerBinding(requestType);
		if(binding==null){
			binding = ProcesserManager.getRequestClassHandlerBinding(0);
		}
		
		Long session_id  = ctx.channel().attr(SESSION_ID).get();
		if(session_id==null){
			logger.error("发现异常,session_id=null");
			return;
		}
		//session
		Session session = SessionServiceImpl.getInstance().getSession(session_id);
		session.setRefreshTime(System.currentTimeMillis());
		
		RequestWrapper wrapper = new RequestWrapper(request,ctx.channel(),binding,session);
		
		//心跳不打印
		if(SystemConfig.gameDebug == 1){
			if(requestType!=RequestType.HEAR.getNumber()){
				System.out.println("转发服Decoder==========>"+request.toByteArray().length+"b\n"+msg);
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
			if(SystemConfig.gameDebug == 1){
				logger.info("新加入链接:" + ctx.channel());
			}
		} catch (Exception e) {
			logger.error("e", e);
		}
		
		String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
		//防火墙
		IpFirewallModel ipFirewallModel= FirewallServiceImpl.getInstance().addNewLink(ip);
		if(ipFirewallModel!=null){
			if(!ipFirewallModel.verifyIP()){
				logger.info("拒绝链接:"+ctx.channel());
				ctx.close();
				return;
			}
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
		session.setSessionId(sessionid);
		session.setCreateTime(System.currentTimeMillis());
		session.setRefreshTime(session.getCreateTime());
		session.setChannel(ctx.channel());
		session.setClientIP(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
		SessionServiceImpl.getInstance().getSessionMap().put(session.getSessionId(), session);
		
		
		
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		
		if(SystemConfig.gameDebug == 1){
			logger.info("断开链接:" + ctx.channel());
		}
		
		try{
		
			String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
			//防火墙
			FirewallServiceImpl.getInstance().delLink(ip);
			
			Long sessionid = ctx.channel().attr(SESSION_ID).get();
			SessionServiceImpl.getInstance().fireSessionFree(sessionid);
			ctx.channel().close();
			//ctx.channel().attr(SESSION_ID).remove();
		}catch(Exception e){
			logger.error("error",e);
		}
		
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
