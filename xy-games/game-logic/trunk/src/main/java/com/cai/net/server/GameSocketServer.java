package com.cai.net.server;

import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.IoStatisticsModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.core.SystemConfig;
import com.cai.net.codec.MyProtobufEncoder;
import com.cai.net.handler.TcpGameServerHandler;
import com.cai.timer.SocketStateTimer;
import com.google.protobuf.ExtensionRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
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
public class GameSocketServer extends WRServer {

	private static Logger logger = LoggerFactory.getLogger(GameSocketServer.class);

	// TODO 压测后再优化参数
	/** 用于分配处理业务线程的线程组个数 */
	public static final int BIZGROUPSIZE = Runtime.getRuntime().availableProcessors() * 2; // 默认
	/** 业务出现线程大小 */
	protected static final int BIZTHREADSIZE = 4;

	private static final EventLoopGroup bossGroup = new NioEventLoopGroup(1);// BIZGROUPSIZE
	private static final EventLoopGroup workerGroup = new NioEventLoopGroup();// BIZTHREADSIZE
	
	
	public static final IoStatisticsModel statistics = new IoStatisticsModel("逻辑计算-中转");

	private final int bindPort;
	
	public GameSocketServer(int bindPort) {
		this.bindPort = bindPort;
	}

	@Override
	public void start() {
		int port = SystemConfig.game_socket_port;
		logger.info("Netty TCP(逻辑计算-中转)启动中,PORT:" + port + "......");
		try {
			PerformanceTimer timer = new PerformanceTimer();
			final ExtensionRegistry registry = ExtensionRegistry.newInstance();
			Protocol.registerAllExtensions(registry);
			
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0, 4, 0, 4));
					p.addLast("protobufDecoder", new ProtobufDecoder(Protocol.Request.getDefaultInstance(), registry));
				    p.addLast("frameEncoder", new LengthFieldPrepender(4));
				    p.addLast("protobufEncoder", new MyProtobufEncoder());
					p.addLast(new TcpGameServerHandler());
				}
			});
			
			b.childOption(ChannelOption.SO_REUSEADDR, true);
			b.childOption(ChannelOption.TCP_NODELAY, true);
			b.bind(bindPort).sync();
			// 分析
			new Timer("socketProxLogicTimer").scheduleAtFixedRate(new SocketStateTimer(statistics), 60000L, 60000L);
			logger.info("Netty TCP(逻辑计算-中转)已启动完成" + timer.getStr());

		} catch (Exception e) {
			logger.error("Netty TCP启动异常", e);
		}

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

}
