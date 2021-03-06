package com.cai.net.codec;

import com.cai.net.server.GameSocketServer;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import static io.netty.buffer.Unpooled.*;

/**
 * Encodes the requested <a href="http://code.google.com/p/protobuf/">Google
 * Protocol Buffers</a> {@link Message} and {@link MessageLite} into a
 * {@link ByteBuf}. A typical setup for TCP/IP would be:
 * 
 * <pre>
 * {@link ChannelPipeline} pipeline = ...;
 *
 * // Decoders
 * pipeline.addLast("frameDecoder",
 *                  new {@link LengthFieldBasedFrameDecoder}(1048576, 0, 4, 0, 4));
 * pipeline.addLast("protobufDecoder",
 *                  new {@link ProtobufDecoder}(MyMessage.getDefaultInstance()));
 *
 * // Encoder
 * pipeline.addLast("frameEncoder", new {@link LengthFieldPrepender}(4));
 * pipeline.addLast("protobufEncoder", new {@link MyProtobufEncoder}());
 * </pre>
 * 
 * and then you can use a {@code MyMessage} instead of a {@link ByteBuf} as a
 * message:
 * 
 * <pre>
 * void channelRead({@link ChannelHandlerContext} ctx, MyMessage req) {
 *     MyMessage res = MyMessage.newBuilder().setText(
 *                               "Did you say '" + req.getText() + "'?").build();
 *     ch.write(res);
 * }
 * </pre>
 */
@Sharable
public class MyProtobufEncoder extends MessageToMessageEncoder<MessageLiteOrBuilder> {
	@Override
	protected void encode(ChannelHandlerContext ctx, MessageLiteOrBuilder msg, List<Object> out) throws Exception {
		if (msg instanceof MessageLite) {
			byte[] bys = ((MessageLite) msg).toByteArray();
			int length = bys.length;
			out.add(wrappedBuffer(bys));

			// 流量统计
			GameSocketServer.statistics.getOutMessages().incrementAndGet();
			GameSocketServer.statistics.getOutBytes().addAndGet(length);

			return;
		}
		if (msg instanceof MessageLite.Builder) {
			byte[] bys = ((MessageLite.Builder) msg).build().toByteArray();
			int length = bys.length;
			out.add(wrappedBuffer(bys));
			
			// 流量统计
			GameSocketServer.statistics.getOutMessages().incrementAndGet();
			GameSocketServer.statistics.getOutBytes().addAndGet(length);
		}
	}
}