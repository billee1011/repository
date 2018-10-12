package com.cai.net.handler;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.IpUtil;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.RequestInvoker;
import com.cai.core.RequestWrapper;
import com.cai.core.SystemConfig;
import com.cai.domain.IpFirewallModel;
import com.cai.domain.Session;
import com.cai.net.server.GameSocketServer;
import com.cai.service.FirewallServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.xianyi.framework.handler.ReqExecutor;
import com.xianyi.framework.net.util.ProcesserManager;
import com.xianyi.framework.net.util.RequestClassHandlerBinding;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 * 
 * 客户端会话处理器
 */
public class TcpGameServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger logger = LoggerFactory.getLogger(TcpGameServerHandler.class);
	private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(1);
	private static final AttributeKey<Long> SESSION_ID = AttributeKey.valueOf("SESSION_ID");

	private final boolean USE_XML = Boolean.FALSE;

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

		Request request = (Request) msg;

		Long session_id = ctx.channel().attr(SESSION_ID).get();
		if (session_id == null) {
			logger.error("发现异常,session_id=null");
			return;
		}

		Session session = SessionServiceImpl.getInstance().getSession(session_id);
		session.setRefreshTime(System.currentTimeMillis());
		session.checkHz();

		// 心跳不打印
		if (SystemConfig.gameDebug == 1) {
			if (request.getRequestType().getNumber() != RequestType.HEAR.getNumber()) {
				System.out.println("转发服Decoder==========>" + request.toByteArray().length + "b\n" + msg);
			}
		}

		if (USE_XML) {
			doCmdByXMl(request, ctx, session);
		} else {
			doCmdByAnnotation(request, session);
		}

		// 流量统计
		GameSocketServer.statistics.getInMessages().incrementAndGet();
		GameSocketServer.statistics.getInBytes().addAndGet(request.toByteArray().length);

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		// cause.printStackTrace();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		try {
			if (SystemConfig.gameDebug == 1) {
				logger.info("新加入链接:" + ctx.channel());
			}
		} catch (Exception e) {
			logger.error("e", e);
		}

		// 防火墙
		String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
		if (!IpUtil.isWhiteIp(ip)) {
			IpFirewallModel ipFirewallModel = FirewallServiceImpl.getInstance().addNewLink(ip);
			if (ipFirewallModel != null) {
				if (!ipFirewallModel.verifyIP()) {
					logger.info("拒绝链接:" + ctx.channel());
					ctx.close();
					return;
				}
			}
		}

		long sessionid = SESSION_ID_GENERATOR.getAndIncrement();
		// 不太可能出现
		if (sessionid > Long.MAX_VALUE - 10000) {
			SESSION_ID_GENERATOR.set(1);
			logger.error("sessionid最大值了，重置");
		}
		ctx.channel().attr(SESSION_ID).set(sessionid);

		// 记录session
		Session session = new Session();
		session.setSessionId(sessionid);
		session.setCreateTime(System.currentTimeMillis());
		session.setRefreshTime(session.getCreateTime());
		session.setChannel(ctx.channel());
		session.setClientIP(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
		SessionServiceImpl.getInstance().getSessionMap().put(session.getSessionId(), session);

		// new session
		// C2SSession c2s = new C2SSession(ctx.channel());
		// c2s.setWorkerLoop(workGroup.next());
		// c2s.setSessionId(sessionid);
		// c2s.setClientIP(((InetSocketAddress)
		// ctx.channel().remoteAddress()).getAddress().getHostAddress());
		// SessionService.getInstance().addSession(c2s);
		// ctx.channel().attr(SESSION_KEY).set(c2s);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);

		if (SystemConfig.gameDebug == 1) {
			logger.info("断开链接:" + ctx.channel());
		}

		try {

			String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
			// 防火墙
			if (!IpUtil.isWhiteIp(ip)) {
				FirewallServiceImpl.getInstance().delLink(ip);
			}

			Long sessionid = ctx.channel().attr(SESSION_ID).get();
			SessionServiceImpl.getInstance().fireSessionFree(sessionid);
			ctx.channel().close();
			// ctx.channel().attr(SESSION_ID).remove();
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	/**
	 * 旧方式-通过xml映射
	 */
	private void doCmdByXMl(Request request, ChannelHandlerContext ctx, Session session) {
		int requestType = request.getRequestType().getNumber();

		// TODO 临时处理，找不到直接转到逻辑服处理
		RequestClassHandlerBinding binding = ProcesserManager.getRequestClassHandlerBinding(requestType);
		if (binding == null) {
			binding = ProcesserManager.getRequestClassHandlerBinding(0);
		}

		RequestWrapper wrapper = new RequestWrapper(request, ctx.channel(), binding, session);
		RequestHandlerThreadPool.getInstance().addTask(new RequestInvoker(wrapper, ctx));

	}

	/**
	 * 新方式-通过注解映射
	 */
	private void doCmdByAnnotation(Request request, Session session) {
		RequestHandlerThreadPool.getInstance().addTask(new ReqExecutor(request, session));
	}
}
