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

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
	private static final Logger logger = LogManager.getLogger(HttpServerHandler.class);

	public HttpServerHandler() {
		logger.info("创建HttpServerHandler");
	}
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} channelActive", ctx.channel());
	}

	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} channelInactive  ", ctx.channel());
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
		Channel ch = ctx.channel();
		if (msg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) msg;
			if (is100ContinueExpected(req)) {
				send100Continue(ctx);
			}
			boolean keepAlive = isKeepAlive(req);
			FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK);
			if (req.getMethod() == HttpMethod.GET) {
				RpcBrokerServlet.getInstance().doGet(req, resp);

			} else {
				RpcBrokerServlet.getInstance().doPost(req, resp);
			}

			resp.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
			int length = resp.content().readableBytes();
			if (length == 0) {
				resp.setStatus(HttpResponseStatus.NO_CONTENT);
			}
			resp.headers().set(CONTENT_LENGTH, length);
			if (keepAlive) {
				resp.headers().set(CONNECTION, Values.KEEP_ALIVE);
			}

			// if (!keepAlive) {
			// ChannelFuture f = Channels.future(ch);
			// f.addListener(ChannelFutureListener.CLOSE);
			// Channels.write(ctx, f, resp);
			// } else {
			// resp.headers().set(CONNECTION, Values.KEEP_ALIVE);
			// Channels.write(ctx, Channels.future(ch), resp);
			// }

			// Write the response.
			ChannelFuture future = ch.writeAndFlush(resp);

			// Close the non-keep-alive connection after the write operation is
			// done.
			if (!keepAlive || resp.getStatus() != HttpResponseStatus.OK) {
				future.addListener(ChannelFutureListener.CLOSE);
			}

		
		}
	}
	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof IOException) {
			logger.warn(cause.getMessage());
		} else {
			logger.error("server failed: " + cause.getMessage(), cause);
		}
		if (ctx.channel().isActive()) {
			ctx.channel().close();
		}
	}


}
