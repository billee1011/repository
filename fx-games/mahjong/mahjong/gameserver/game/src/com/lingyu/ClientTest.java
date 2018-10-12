package com.lingyu;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.codec.Protocol;
import com.lingyu.common.codec.ProtocolDecoder;
import com.lingyu.common.io.MsgType;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ClientTest {

    public static void main(String[] args) throws Exception {
        Channel channel = connect("127.0.0.1", 9999);
        JSONObject jsonObject = new JSONObject();
        int cmd = MsgType.LoginGame_C2S_Msg;
        jsonObject.put("id", cmd);
        Object[] data = new Object[] { 2, 333, "", 333, 333 };
        jsonObject.put("data", data);

        ByteBuf bef = UnpooledByteBufAllocator.DEFAULT.buffer();
        String json = jsonObject.toString();
        bef.writeInt(json.length());
        bef.writeBytes(json.getBytes());
        channel.writeAndFlush(bef);
        TimeUnit.SECONDS.sleep(2);
        // channel.writeAndFlush(bef);

        // protocol.cmd = MsgType.CREATE_MAHJONG_ROOM;
        // jsonObject.clear();
        // jsonObject.put("jushu", 8);
        // Object[] data = new Object[] {0};
        // protocol.body = jsonObject;
        // channel.writeAndFlush(protocol);

        // protocol.cmd = MsgType.JOIN_MAHJONG_MSG;
        // jsonObject.clear();
        // jsonObject.put("roomNum", "975787");
        // protocol.body = jsonObject;
        // channel.writeAndFlush(protocol);

        // TimeUnit.SECONDS.sleep(2);
        //
        // protocol.cmd = MsgType.MAHJONG_DISMISS_MSG;
        // jsonObject.clear();
        // protocol.body = jsonObject;
        // channel.writeAndFlush(protocol);
    }

    public static Channel connect(String host, int port) throws Exception {
        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture f = null;
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel arg0) throws Exception {
                                arg0.pipeline().addLast("decoder", new TProtocolDecoder());
                                // arg0.pipeline().addLast("encoder", new
                                // ProtocolEncoder());
                                arg0.pipeline().addLast(new TimeClientHandler());
                            }
                        });
        // 发起异步连接操作
        f = b.connect(host, port).sync();
        return f.channel();
    }
}

class TimeClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Protocol) {
            Protocol protocol = (Protocol) msg;
            System.out.println(" rec data cmd: " + protocol.cmd + "  data:  " + protocol.body);
        }
    }
}

class TProtocolDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LogManager.getLogger(ProtocolDecoder.class);

    private final static int HEAD_LENGTH = 4;

    @Override
    protected void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf byteBuf, List<Object> paramList)
                    throws Exception {
        int dataLength = 0;
        try {
            if (byteBuf.readableBytes() < HEAD_LENGTH) {
                logger.info("receData 数据小于四个字节");
                return;
            }
            byteBuf.markReaderIndex();
            dataLength = byteBuf.readInt();
            int cmd = byteBuf.readInt();
            if (dataLength == -0 || dataLength >= 1024 * 1024) {
                logger.info("exception -- cmd :" + cmd + " datalength: " + dataLength);
                return;
            }
            if (byteBuf.readableBytes() < dataLength) {
                byteBuf.resetReaderIndex();
                logger.info("数据小于可读长度");
                return;
            }
            byte[] data = new byte[dataLength];
            byteBuf.readBytes(data);
            Protocol protocol = new Protocol();
            Object object = JSON.parse(data);
            if (object instanceof JSONObject) {
                protocol.cmd = ((JSONObject) object).getIntValue("id");
                protocol.body = (JSONObject) object;
                paramList.add(protocol);
            }
        } catch (Exception e) {
            logger.info("解析数据错误", e);
        }
    }
}