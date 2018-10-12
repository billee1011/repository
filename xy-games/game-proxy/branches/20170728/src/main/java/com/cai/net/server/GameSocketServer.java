package com.cai.net.server;

import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.IoStatisticsModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.core.MyThreadFactory;
import com.cai.core.ThreadNameEnum;
import com.cai.net.codec.MyProtobufEncoder;
import com.cai.net.handler.TcpGameServerHandler;
import com.google.protobuf.ExtensionRegistry;
import com.xianyi.framework.net.server.WRServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import protobuf.clazz.Protocol;

/**
 * 中转与逻辑端连接的服务
 * 
 * @author run
 *
 */
public class GameSocketServer implements WRServer {

	private static Logger logger = LoggerFactory.getLogger(GameSocketServer.class);

	/**
	 * 服务器启动辅助类
	 */
	private ServerBootstrap bootstrap;

	/**
	 * 流量记录
	 */
	public static final IoStatisticsModel statistics = new IoStatisticsModel("中转-客户端");

	/**
	 * 端口号
	 */
	private final int port;

	/**
	 * 
	 * @param port
	 */
	public GameSocketServer(int port) {
		this.port = port;
	}

	@Override
	public void start() {
		logger.info("Netty TCP(中转-客户端)启动中,PORT:" + port + "......");
		try {
			PerformanceTimer timer = new PerformanceTimer();
			final ExtensionRegistry registry = ExtensionRegistry.newInstance();
			Protocol.registerAllExtensions(registry);

			ServerBootstrap b = new ServerBootstrap();
			b.group(new NioEventLoopGroup(1, new MyThreadFactory(ThreadNameEnum.GAME_BOSS_SERVER.getCode())),
					new NioEventLoopGroup(0, new MyThreadFactory(ThreadNameEnum.GAME_WORK_SERVER.getCode())));
			b.channel(NioServerSocketChannel.class);
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					p.addLast("frameDecoder",
							new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 30720, 0, 4, 0, 4, true));
					p.addLast("protobufDecoder", new ProtobufDecoder(Protocol.Request.getDefaultInstance(), registry));
					p.addLast("frameEncoder", new LengthFieldPrepender(4));
					p.addLast("protobufEncoder", new MyProtobufEncoder());
					p.addLast(new TcpGameServerHandler());
				}
			});

//			b.childOption(ChannelOption.SO_REUSEADDR, true);
//			b.childOption(ChannelOption.TCP_NODELAY, true);
//			b.childOption(ChannelOption.SO_LINGER, 0);
//			b.childOption(ChannelOption.SO_RCVBUF, 1024 * 16);
//			b.childOption(ChannelOption.SO_SNDBUF, 1024 * 32);

			bootstrap = b;
			b.bind(port).sync();

			logger.info("Netty TCP(中转-客户端)已启动完成" + timer.getStr());

		} catch (Exception e) {
			logger.error("Netty TCP启动异常", e);
		}

	}

	@Override
	public void shutdown() {
		if (null != bootstrap) {
			bootstrap.group().shutdownGracefully();
			bootstrap.childGroup().shutdownGracefully();
		}
	}

	@Override
	public int boundPort() {
		return this.port;
	}

}
