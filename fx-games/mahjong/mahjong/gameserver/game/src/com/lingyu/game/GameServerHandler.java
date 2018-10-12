package com.lingyu.game;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.codec.Protocol;
import com.lingyu.common.codec.ProtocolDecoder;
import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.game.service.role.RoleManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

public class GameServerHandler extends SimpleChannelInboundHandler {
	private static final Logger logger = LogManager.getLogger(GameServerHandler.class);
	private static RouteManager routeManager = GameServerContext.getBean(RouteManager.class);

	public GameServerHandler() {
		logger.info("创建GameServerHandler");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object e) throws Exception {
		if (e instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) e;
			Session session = SessionManager.getInstance().getSession(ctx.channel());
			if (session != null) {
				// logger.info("session={}", session.toString());
				logger.warn("close the channel: heartbeat {},userId={},roleId={},type={}", ctx.channel(),
				        session.getUserId(), session.getRoleId(), event.state());
			} else {
				logger.info("session 为空");
			}
			try {
				ctx.close();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}

		}
	}

	/** 建立连接 channelConnected */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		logger.info("{} connect", channel);
		ServerConfig config = GameServerContext.getAppConfig();
		// 最大连接数
		if (SessionManager.getInstance().getConnectionNum() >= config.getMaxConcurrentUser()) {
			ctx.close();
		} else {
			InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
			String ip = insocket.getAddress().getHostAddress();
			Session oldSession = SessionManager.getInstance().getSession4IP(ip);
			if (oldSession != null && oldSession.getChannel().isActive()) {
				return;
			}
			SessionManager.getInstance().addSession(SystemConstant.SESSION_TYPE_PLAYER, channel);
		}
	}

	// /** 服务器主动关闭会触发该方法 */
	// @Override
	// public void closeRequested(ChannelHandlerContext ctx, ChannelStateEvent
	// e) throws Exception {
	// //
	// logger.info("服务器主动关闭连接 closeRequested:{} ", ctx.getChannel());
	// ctx.sendDownstream(e);
	// }
	//
	// /** 当本地远程客户机与本主机建立连接后，连接断开才会触发该方法,所以客户端关闭或者服务器主动关闭都会触发这个方法 */
	// public void channelDisconnected(ChannelHandlerContext ctx,
	// ChannelStateEvent e) throws Exception {
	// logger.info("{} channelDisconnected ", ctx.getChannel());
	// }

	/** 断开连接 原先的channelClosed */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} channelClosed", ctx.channel());
		Session session = SessionManager.getInstance().getSession(ctx.channel());
		// InetSocketAddress insocket = (InetSocketAddress)
		// ctx.channel().remoteAddress();
		// String ip = insocket.getAddress().getHostAddress();
		// Session session = SessionManager.getInstance().getSession4IP(ip);
		if (session == null) {
			logger.warn("{} channel closed: no session id founded", ctx.channel());
		} else {
			RoleManager roleManager = GameServerContext.getBean(RoleManager.class);
			roleManager.logoutGame(session.getRoleId());
			SessionManager.getInstance().removeSession(ctx.channel());
			logger.info("{}/{} closed", ctx.channel(), session.getId());
		}
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		try {
			Protocol protocol = null;
			if (msg instanceof FullHttpRequest) {
				handleHttpRequest(ctx, (FullHttpRequest) msg);
			}
			if (msg instanceof BinaryWebSocketFrame) {
				BinaryWebSocketFrame bin = (BinaryWebSocketFrame) msg;
				ByteBuf buf = bin.content();
				protocol = (Protocol) ProtocolDecoder.decode(buf);
			}

			if (protocol != null) {
				int msgType = protocol.cmd;
				JSONObject instance = protocol.body;
				processMsg(ctx.channel(), msgType, instance);
			}
		} catch (ServiceException e) {
			logger.error("processMsg failed: " + e.getMessage(), e);
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("/websocket", null, false);
		WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}

	}

	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
		// 返回应答给客户端
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		// 如果是非Keep-Alive，关闭连接
		// ChannelFuture f = ctx.channel().writeAndFlush(res);
		// if (!isKeepAlive(req) || res.getStatus().code() != 200) {
		// f.addListener(ChannelFutureListener.CLOSE);
		// }
	}

	/** messageReceived */
	/*
	 * @Override public void channelRead0(ChannelHandlerContext ctx, Object[]
	 * msg) { try { int msgType = (int) msg[0]; Object instance = msg[1];
	 * Object[] content = null; if (instance != null) { if (instance instanceof
	 * Object[]) { content = (Object[]) instance; } else { content = new
	 * Object[] { instance }; logger.warn("需要客户端调整的 msgType={}", msgType); } }
	 * else { content = new Object[] {}; logger.warn("需要客户端调整的 msgType={}",
	 * msgType); } processMsg(ctx.channel(), msgType, content); } catch
	 * (ServiceException e) { logger.error("processMsg failed: " +
	 * e.getMessage(), e); } }
	 */

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
		logger.error(e.getMessage(), e);
		Session session = SessionManager.getInstance().getSession(ctx.channel());
		if (session == null) {
			logger.warn("{} channel exceptionCaught: no session id founded", ctx.channel());
		} else {
			logger.info("session={}", session.toString());
		}
		if (ctx.channel().isActive()) {
			// fix ：An exceptionCaught() event was fired, and it reached at the
			// tail of the pipeline. It usually means the last handler in the
			// pipeline did not handle the exception.: java.io.IOException: Too
			// many open files
			// Netty server does not close/release socket
			try {
				ctx.close();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	// private void processMsg(Channel channel, int msgType, Object[] content) {
	// Session session = SessionManager.getInstance().getSession(channel);
	// routeManager.handleMsg(session, msgType, content);
	// }

	private void processMsg(Channel channel, int msgType, JSONObject content) {
		InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
		String ip = insocket.getAddress().getHostAddress();
		Session session = SessionManager.getInstance().getSession4IP(ip);
		// Session session = SessionManager.getInstance().getSession(channel);
		routeManager.handleMsg(session, msgType, content);
	}
}