package com.lingyu.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.io.SimpleEncrypt;
import com.lingyu.noark.amf3.Amf3;

/** 分段解码器 */
public class Amf3SectionDecoder extends ByteToMessageDecoder {
	private static final Logger logger = LogManager.getLogger(Amf3SectionDecoder.class);
	private boolean tgwMode;
	private boolean isFirstConnection;
	private final SimpleEncrypt encrypt;
	private boolean firstPackReceived;
	private MutableBoolean common;//策略文件请求 false,正常消息true

	public Amf3SectionDecoder(boolean tgwMode, SimpleEncrypt encrypt, MutableBoolean common) {
		this.tgwMode = tgwMode;
		this.encrypt = encrypt;
		this.common = common;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> objects) throws Exception {
		if (byteBuf.readableBytes() < 4) {
			return;
		}
		
		// mark读索引
		byteBuf.markReaderIndex();
		int length = byteBuf.readInt();// 字节长度
		if (length < 0) {
			logger.error("request msg length <0,length={}", length);
			byteBuf.resetReaderIndex();
			ctx.close();
			return;
		}

		if (byteBuf.readableBytes() < length) {
			byteBuf.resetReaderIndex();
			return;
		}
		this.sectionDecoder(ctx, byteBuf, objects, length);
	}
	
	public void sectionDecoder(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> objects,int length){
		// 这套为优先读命令
		if(isFirstConnection){
			int msgType = byteBuf.readInt();
			byte[] content = new byte[length - 4];
			byteBuf.readBytes(content);
			content = encrypt.decode(content);
			if(objects!=null){
				objects.add(new Object[]{msgType,content});
			}
		}else {
			byte[] content = new byte[length];
			byteBuf.readBytes(content);
			String key = encrypt.getKey();
			logger.info("handshake from {} by {} key={}", ctx.channel().remoteAddress(), new String(content), key);
			ctx.channel().writeAndFlush(key.getBytes());
			isFirstConnection = true;
		}
	}
	
	public static void main(String[] args) {
		Object str = new Object[]{"aaaaxxxx",111};
		byte t[] = Amf3.toBytes(str);
		Object[] data = (Object[]) Amf3.parse(t);
		System.err.println("aaa");
	}
}
