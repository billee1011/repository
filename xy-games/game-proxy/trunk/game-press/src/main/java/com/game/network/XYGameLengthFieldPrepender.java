/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午11:18:39 <br/>
 */
@Sharable
public final class XYGameLengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {

	private final int lengthFieldLength;
	private final boolean lengthIncludesLengthFieldLength;
	private final int lengthAdjustment;

	/**
	 * Creates a new instance.
	 *
	 * @param lengthFieldLength
	 *            the length of the prepended length field. Only 1, 2, 3, 4, and
	 *            8 are allowed.
	 *
	 * @throws IllegalArgumentException
	 *             if {@code lengthFieldLength} is not 1, 2, 3, 4, or 8
	 */
	public XYGameLengthFieldPrepender(int lengthFieldLength) {
		this(lengthFieldLength, false);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param lengthFieldLength
	 *            the length of the prepended length field. Only 1, 2, 3, 4, and
	 *            8 are allowed.
	 * @param lengthIncludesLengthFieldLength
	 *            if {@code true}, the length of the prepended length field is
	 *            added to the value of the prepended length field.
	 *
	 * @throws IllegalArgumentException
	 *             if {@code lengthFieldLength} is not 1, 2, 3, 4, or 8
	 */
	public XYGameLengthFieldPrepender(int lengthFieldLength, boolean lengthIncludesLengthFieldLength) {
		this(lengthFieldLength, 0, lengthIncludesLengthFieldLength);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param lengthFieldLength
	 *            the length of the prepended length field. Only 1, 2, 3, 4, and
	 *            8 are allowed.
	 * @param lengthAdjustment
	 *            the compensation value to add to the value of the length field
	 *
	 * @throws IllegalArgumentException
	 *             if {@code lengthFieldLength} is not 1, 2, 3, 4, or 8
	 */
	public XYGameLengthFieldPrepender(int lengthFieldLength, int lengthAdjustment) {
		this(lengthFieldLength, lengthAdjustment, false);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param lengthFieldLength
	 *            the length of the prepended length field. Only 1, 2, 3, 4, and
	 *            8 are allowed.
	 * @param lengthAdjustment
	 *            the compensation value to add to the value of the length field
	 * @param lengthIncludesLengthFieldLength
	 *            if {@code true}, the length of the prepended length field is
	 *            added to the value of the prepended length field.
	 *
	 * @throws IllegalArgumentException
	 *             if {@code lengthFieldLength} is not 1, 2, 3, 4, or 8
	 */
	public XYGameLengthFieldPrepender(int lengthFieldLength, int lengthAdjustment, boolean lengthIncludesLengthFieldLength) {
		if (lengthFieldLength != 1 && lengthFieldLength != 2 && lengthFieldLength != 3 && lengthFieldLength != 4 && lengthFieldLength != 8) {
			throw new IllegalArgumentException("lengthFieldLength must be either 1, 2, 3, 4, or 8: " + lengthFieldLength);
		}

		this.lengthFieldLength = lengthFieldLength;
		this.lengthIncludesLengthFieldLength = lengthIncludesLengthFieldLength;
		this.lengthAdjustment = lengthAdjustment;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
		int length = msg.readableBytes() + lengthAdjustment;
		if (lengthIncludesLengthFieldLength) {
			length += lengthFieldLength;
		}

		if (length < 0) {
			throw new IllegalArgumentException("Adjusted frame length (" + length + ") is less than zero");
		}

		switch (lengthFieldLength) {
		case 1:
			if (length >= 256) {
				throw new IllegalArgumentException("length does not fit into a byte: " + length);
			}
			out.writeByte((byte) length);
			break;
		case 2:
			if (length >= 65536) {
				throw new IllegalArgumentException("length does not fit into a short integer: " + length);
			}
			out.writeShort((short) length);
			break;
		case 3:
			if (length >= 16777216) {
				throw new IllegalArgumentException("length does not fit into a medium integer: " + length);
			}
			out.writeMedium(length);
			break;
		case 4: {
			out.writeInt(bytes2int(toLH(length)));
		}

			break;
		case 8:
			out.writeLong(length);
			break;
		default:
			throw new Error("should not reach here");
		}

		out.writeBytes(msg, msg.readerIndex(), msg.readableBytes());
	}

	// 将整数按照小端存放，低字节出访低位
	public static byte[] toLH(int n) {
		byte[] b = new byte[4];
		b[0] = (byte) (n & 0xff);
		b[1] = (byte) (n >> 8 & 0xff);
		b[2] = (byte) (n >> 16 & 0xff);
		b[3] = (byte) (n >> 24 & 0xff);
		return b;
	}

	/**
	 * 将int转为大端，低字节存储高位
	 * 
	 * @param n
	 *            int
	 * @return byte[]
	 */
	public static byte[] toHH(int n) {
		byte[] b = new byte[4];
		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);
		return b;
	}

	public static void main(String[] args) {
		byte[] bb = toLH(22);
		for (int i = 0; i < 4; i++) {
			System.out.println(bb[i]);
		}
		System.out.println(bytes2int(bb));
		bb = toHH(22);
		for (int i = 0; i < 4; i++) {
			System.out.println(bb[i]);
		}
		System.out.println(bytes2int(bb));
	}

	// 高位在前，低位在后
	public static byte[] int2bytes(int num) {
		byte[] result = new byte[4];
		result[0] = (byte) ((num >>> 24) & 0xff);// 说明一
		result[1] = (byte) ((num >>> 16) & 0xff);
		result[2] = (byte) ((num >>> 8) & 0xff);
		result[3] = (byte) ((num >>> 0) & 0xff);
		return result;
	}

	// 高位在前，低位在后
	public static int bytes2int(byte[] bytes) {
		int result = 0;
		if (bytes.length == 4) {
			int a = (bytes[0] & 0xff) << 24;// 说明二
			int b = (bytes[1] & 0xff) << 16;
			int c = (bytes[2] & 0xff) << 8;
			int d = (bytes[3] & 0xff);
			result = a | b | c | d;
		}
		return result;
	}
}
