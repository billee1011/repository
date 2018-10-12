package com.lingyu.common.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class ProtocolEncoder {

    private final static String ENCODING = "UTF-8";

    public static BinaryWebSocketFrame encode(Protocol message) {
        try {
            ByteBuf protoBuf = PooledByteBufAllocator.DEFAULT.buffer();
            Protocol protocol = message;
            byte[] bodyBytes = protocol.body.toString().getBytes(ENCODING);
            protoBuf.writeInt(ProtocolDecoder.HEADER_FLAG); // 数据部分长度（4字节）
            protoBuf.writeInt(bodyBytes.length); // 数据部分长度（4字节）
            protoBuf.writeInt(protocol.cmd); // 操作ID（4字节）
            protoBuf.writeBytes(bodyBytes); // 数据部分
            return new BinaryWebSocketFrame(protoBuf);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 压缩
     *
     * @param input
     * @return
     */
    private byte[] compress(byte[] input) {
        byte[] output = new byte[0];
        Deflater compresser = new Deflater();
        compresser.reset();
        compresser.setInput(input);
        compresser.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        try {
            byte[] buf = new byte[1024];
            while (!compresser.finished()) {
                int i = compresser.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        } catch (Exception e) {
            output = input;
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
            }
        }
        compresser.end();
        return output;
    }
}
