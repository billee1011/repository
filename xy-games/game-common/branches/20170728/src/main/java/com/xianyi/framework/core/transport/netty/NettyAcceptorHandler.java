package com.xianyi.framework.core.transport.netty;

import com.cai.common.constant.AttributeKeyConstans;
import com.xianyi.framework.core.transport.event.IOCustomEvent;
import com.xianyi.framework.core.transport.event.IOEvent.Event;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 消息处理器
 * 
 * @author vincent.wu
 *
 */
public final class NettyAcceptorHandler extends SimpleChannelInboundHandler<Object> {
	/**
	 * 
	 */
	private final IOEventListener<C2SSession> listener;

	/**
	 * @param behaviour
	 */
	public NettyAcceptorHandler(IOEventListener<C2SSession> listener) {
		this.listener = listener;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		listener.onEvent(new IOCustomEvent<C2SSession>(Event.READ, getC2S(ctx), msg));
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		C2SSession session = new C2SSession(ctx.channel());
		ctx.attr(AttributeKeyConstans.PROXY_ACEEPTOR).set(session);
		listener.onEvent(new IOCustomEvent<C2SSession>(Event.REGISTERED, session));
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		listener.onEvent(new IOCustomEvent<C2SSession>(Event.UNREGISTERED, getC2S(ctx)));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		listener.onEvent(new IOCustomEvent<C2SSession>(Event.EXCEPTION, getC2S(ctx), cause));
	}

	/**
	 * 
	 * @param ctx
	 * @return
	 */
	private static C2SSession getC2S(final ChannelHandlerContext ctx) {
		return ctx.attr(AttributeKeyConstans.PROXY_ACEEPTOR).get();
	}
}
