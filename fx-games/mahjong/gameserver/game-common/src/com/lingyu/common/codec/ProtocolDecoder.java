package com.lingyu.common.codec;

import java.nio.ByteOrder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.noark.amf3.exception.MessageException;

import io.netty.buffer.ByteBuf;

public class ProtocolDecoder {

	private static final Logger logger = LogManager.getLogger(ProtocolDecoder.class);

	private final static int HEAD_LENGTH = 4;

	private final static String ENCODING = "UTF-8";

	public final static int HEADER_FLAG = 4165656;

	public static Object decode(ByteBuf in) {
		ByteBuf buff = in.order(ByteOrder.BIG_ENDIAN);
		int headError = 0;
		while (true) {// 查找消息头
			if (headError >= 1024 * 5) {// 查找5K字节找不到就抛出异常
				buff.resetReaderIndex();
				throw new MessageException("can't find message head!");
			}
			if (buff.readableBytes() < 5) {
				return null;
			}
			buff.markReaderIndex();
			if (in.readInt() == HEADER_FLAG) {
				break;
			}
			buff.resetReaderIndex();
			buff.readByte();// 跳过一个字节
			headError += 1;
		}
		if (buff.readableBytes() < 4) {
			buff.resetReaderIndex();
			return null;
		}

		int messageLen = buff.readInt();
		if (buff.readableBytes() < messageLen) {
			buff.resetReaderIndex();
			return null;
		}

		return decode(buff, messageLen);
	}

	protected static Object decode(ByteBuf in, int messageLen) {
		byte[] data = new byte[messageLen];
		in.readBytes(data);
		JSONObject jsonObject = (JSONObject) JSONObject.parse(data);
		int cmd = jsonObject.getIntValue("id");
		if (cmd > 0) {
			Protocol protocol = new Protocol();
			protocol.cmd = cmd;
			protocol.body = jsonObject;
			return protocol;
		}

		return null;
	}
}
