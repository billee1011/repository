package com.lingyu.common.codec;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/** 编码器 */
public class Amf3Encoder extends MessageToByteEncoder<byte[]> {
	private static final Logger logger = LogManager.getLogger(Amf3Encoder.class);
	private MutableBoolean common;// 是否正常通信

	public Amf3Encoder(MutableBoolean common) {
		this.common = common;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf byteBuf) throws Exception {
		//logger.trace("Amf3Encoder");
		if (msg != null) {
			if (common.isTrue()) {
				byteBuf.writeInt(msg.length);
			}
			byteBuf.writeBytes(msg);
		}
	}
}
