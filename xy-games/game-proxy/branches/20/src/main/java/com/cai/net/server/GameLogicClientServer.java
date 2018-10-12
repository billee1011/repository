package com.cai.net.server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.net.handler.TcpLogicServerHandler;
import com.google.protobuf.ExtensionRegistry;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import protobuf.clazz.Protocol;


/**
 * 游戏连接逻辑的客户端
 * @author run
 *
 */
public class GameLogicClientServer{
	private static final Logger log = LoggerFactory.getLogger(GameLogicClientServer.class);
	public static String HOST = "127.0.0.1";
	public static int PORT = 18761;

	public static Bootstrap bootstrap = getBootstrap();




	/**
	 * 初始化Bootstrap
	 * 
	 * @return
	 */
	public static final Bootstrap getBootstrap() {
		EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true); 
		final ExtensionRegistry registry = ExtensionRegistry.newInstance();
		Protocol.registerAllExtensions(registry);
		b.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0, 4, 0, 4));
				p.addLast("protobufDecoder", new ProtobufDecoder(Protocol.Request.getDefaultInstance(), registry));
			    p.addLast("frameEncoder", new LengthFieldPrepender(4));
			    p.addLast("protobufEncoder", new ProtobufEncoder());
				p.addLast("handler", new TcpLogicServerHandler());
			}
		});
		return b;
	}

	public static final Channel getChannel(String host, int port) {

		Channel channel = null;
		try {
			channel = bootstrap.connect(host, port).sync().channel();
		} catch (Exception e) {
			log.error(String.format("连接逻辑服失败(IP:%s,PORT:%s)", host, port));
			return null;
		}
		return channel;
	}


}
