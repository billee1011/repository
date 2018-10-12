package com.lingyu.game;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.codec.ProtocolDecoder;
import com.lingyu.common.codec.ProtocolEncoder;
import com.lingyu.common.io.TrafficShapingHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class GameServerChannelPipelineFactory extends ChannelInitializer<Channel> {
	private static final Logger logger = LogManager.getLogger(GameServerChannelPipelineFactory.class);
	// private ExecutionHandler executionHandler = new ExecutionHandler(new
	// OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576, 30L,
	// TimeUnit.SECONDS,
	// new GameThreadFactory("netty-executor")));
	private TrafficShapingHandler handler;

	public GameServerChannelPipelineFactory() {

	}

	public GameServerChannelPipelineFactory(TrafficShapingHandler handler) {
		this.handler = handler;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		// int a =ch.config().getOption(ChannelOption.SO_RCVBUF);
		// int b=ch.config().getOption(ChannelOption.SO_SNDBUF);

		// int
		// high=ch.config().getOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK);//64K
		// int
		// low=ch.config().getOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK);//32K
		// logger.debug("high={},low={}",high,low);
		// String key = UUID.randomUUID().toString();
		// MutableBoolean common = new MutableBoolean(true);
		// SimpleEncrypt encrypt = new SimpleEncrypt(key,
		// SystemConstant.TICKET);
		ChannelPipeline p = ch.pipeline();
		// TODO 临时被注掉 allen
		// p.addLast("idle", new IdleStateHandler(0, 0,
		// GameServerContext.getAppConfig().getHeartBeat()));
		// 既然消息会被路由处理，这里就没必要通过ExecutionHandler线程池来异步处理ChannelHandler链
		// ret.addLast("executor", executionHandler);
		boolean tgwMode = GameServerContext.getAppConfig().isTgwMode();
		if (handler != null) {
			p.addLast("traffic_counter", handler);
		}
		// p.addLast("decoder", new Amf3SectionDecoder(tgwMode,
		// encrypt,common));
		// p.addLast("encoder", new Amf3SectionEncoder(common));

		p.addLast("http-codec", new HttpServerCodec());
		p.addLast("http-chunked", new ChunkedWriteHandler());
		p.addLast("aggregator", new HttpObjectAggregator(65536));
		p.addLast("handle", new GameServerHandler());
	}
}