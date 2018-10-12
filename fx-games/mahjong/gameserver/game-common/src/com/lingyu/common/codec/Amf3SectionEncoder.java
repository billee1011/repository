package com.lingyu.common.codec;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/** 分段编码器 */
public class Amf3SectionEncoder extends MessageToByteEncoder<byte[]> {
	private static final Logger logger = LogManager.getLogger(Amf3SectionEncoder.class);
	private MutableBoolean common;// 是否正常通信
	
	public static long last1Seconds =0l;
	public static long last1SecondTotalMsg=0;
	public static long last1SecondCountMsg=0;
	public static final int maxRequireZipSize=200;

	public Amf3SectionEncoder(MutableBoolean common) {
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
