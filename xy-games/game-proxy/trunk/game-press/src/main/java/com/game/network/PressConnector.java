/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import com.cai.common.define.EServerStatus;
import com.google.protobuf.ExtensionRegistry;
import com.xianyi.framework.core.transport.AbstractConnector;
import com.xianyi.framework.core.transport.Connector;
import com.xianyi.framework.core.transport.UnresolvedAddress;
import com.xianyi.framework.core.transport.netty.NettyConnectorHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import protobuf.clazz.Protocol;

/**
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午4:22:24 <br/>
 */
public class PressConnector extends AbstractConnector implements Connector {

	/**
	 * 
	 */
	private Bootstrap bootstrap;

	/**
	 * 
	 */
	private Channel channel;

	private static final EventLoopGroup group = new NioEventLoopGroup(8);

	/**
	 * 
	 * @param hostNode
	 */
	public PressConnector(final String hostNode) {
		super(new UnresolvedAddress(hostNode));
	}

	/**
	 * @param address
	 * @param port
	 */
	public PressConnector(String address, int port) {
		super(new UnresolvedAddress(address, port));
	}

	@Override
	public boolean doLogin() {
		return true;
	}

	@Override
	public boolean doInit() {

		checkNotNull(listener, "Connector listener CAN NOT be a nil value!!");

		// EventLoopGroup group = new NioEventLoopGroup(1);
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		final ExtensionRegistry registry = ExtensionRegistry.newInstance();
		Protocol.registerAllExtensions(registry);
		b.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
				p.addLast("protobufDecoder", new ProtobufDecoder(Protocol.Response.getDefaultInstance(), registry));
				p.addLast("frameEncoder", new XYGameLengthFieldPrepender(4));
				p.addLast("protobufEncoder", new ProtobufEncoder());
				p.addLast("handler", new NettyConnectorHandler(listener, PressConnector.this));
			}
		});
		bootstrap = b;
		return true;
	}

	@Override
	public boolean connect() {
		if (isActive())
			return true;
		try {
			ChannelFuture future = bootstrap.connect(address.getHost(), address.getPort());
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						channel = future.channel();
						status = EServerStatus.ACTIVE;
						log.info("##### Connect to server[{}] successfully!", address.toString());
						if (null != connectedCallback) {
							connectedCallback.onEvent(PressConnector.this);
						}
					} else {
						status = EServerStatus.CLOSE;
						log.info("##### Failed to connect to server[{}], try connect after 5s", address.toString());
						future.channel().eventLoop().schedule(new Runnable() {
							@Override
							public void run() {
								reConnect();
							}
						}, 5, TimeUnit.SECONDS);
					}
				}
			});
		} catch (Exception e) {
			log.error(String.format("连接服务器失败(IP:%s,PORT:%s)", address.getHost(), address.getPort()));
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public boolean isActive() {
		return (null != channel
				&& channel.isActive() /*
										 * && (status == EServerStatus.ACTIVE ||
										 * status == EServerStatus.REPAIR)
										 */);
	}

	@Override
	public void send(Object request) {
		if (isActive()) {
			channel.writeAndFlush(request);
		}
	}

	@Override
	public void shutdownGracefully() {
		if (null != bootstrap) {
			bootstrap.group().shutdownGracefully();
		}
	}

	public Channel getChannel() {
		return this.channel;
	}

}
