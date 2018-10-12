/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.lingyu.common.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.core.ServiceException;

/**
 * An HTTP server that sends back the content of the received HTTP request in a
 * pretty plaintext form.
 */
public class HttpServer {
	private static final Logger logger = LogManager.getLogger(HttpServer.class);
	private final int port;
	private ServerBootstrap bootstrap;
	private NioEventLoopGroup bossGroup;
	private NioEventLoopGroup workerGroup;

	public HttpServer(int port) {
		this.port = port;
	}

	public void run() {
		logger.info("初始化HTTP服务开始");
		// Configure the server.
		bootstrap = new ServerBootstrap();
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup(2);
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		// Set up the event pipeline factory.
		bootstrap.childHandler(new HttpServerPipelineFactory());
		// Enable TCP_NODELAY to handle pipelined requests without latency.
		bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE, false);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		// Bind and start to accept incoming connections.
		try {
			bootstrap.bind(new InetSocketAddress(port)).sync();
			logger.info("初始化HTTP服务完毕");
		} catch (Exception e) {
			logger.info("初始化HTTP服务失败");
			throw new ServiceException(e);
		}
		
	}

	public static void main(String[] args) {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		new HttpServer(port).run();
	}

	public void shutDown() {
		// Shut down all event loops to terminate all threads.
		bossGroup.shutdownGracefully();
		// Wait until all threads are terminated.
		bossGroup.terminationFuture();

		// Shut down all event loops to terminate all threads.
		workerGroup.shutdownGracefully();
		// Wait until all threads are terminated.
		workerGroup.terminationFuture();
		logger.info("HTTP服务已关闭");
	}
}
