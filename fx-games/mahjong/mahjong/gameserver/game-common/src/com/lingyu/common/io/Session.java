package com.lingyu.common.io;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lingyu.common.codec.Protocol;
import com.lingyu.common.codec.ProtocolEncoder;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.proto.Game.RpcMsg;
import com.lingyu.msg.rpc.DispatchEventReq;
import com.lingyu.msg.rpc.RelayMsgReq;
import com.lingyu.noark.amf3.Amf3;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class Session {
    private static final Logger logger = LogManager.getLogger(Session.class);
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_JICHU = 2;
    private Channel channel;
    private String userId;
    private long roleId;
    private String roleName;
    private String id;
    private int status = STATUS_NORMAL;
    private int interval;
    private long startTime;
    private int type;
    private int worldId;
    private String ip;
    // 第三方平台参数
    private Map<String, String> params = new HashMap<>();
    // 跨服
    private Map<Long, Boolean> roleMap = new HashMap<Long, Boolean>();
    private Map<Long, CountDownLatch> watchDog = new ConcurrentHashMap<>();
    private Map<Long, RpcMsg> responses = new ConcurrentHashMap<>();
    protected static final AtomicLong seqId = new AtomicLong(0);

    public Session(int type, Channel channel, String sessionId) {
        this.type = type;
        this.channel = channel;
        this.id = sessionId;
    }

    // 注入第三方参数

    public int getWorldId() {
        return worldId;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void setWorldId(int worldId) {
        this.worldId = worldId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void addRole4RPC(long roleId) {
        roleMap.put(roleId, true);
    }

    public void removeRole4RPC(long roleId) {
        roleMap.remove(roleId);
    }

    public Collection<Long> getRoleList4RPC() {
        return roleMap.keySet();
    }

    public void sendMsg(byte[] content) {
        // 解决底层对象池的对象经常耗尽的问题
        if (channel.isActive()) {
            channel.writeAndFlush(content);
        }
        if (channel.isWritable()) {
        } else {
            // 本次消息没法被写入[1.NIO中缓存的消息未被写完,本次NIO写时间已到,2.对端TCP缓存区已满[本时刻点],....]
            // channel.writeAndFlush(content);// ,channel.voidPromise()
            logger.debug("channel={},isActive={},isWritable={}", channel, channel.isActive(), channel.isWritable());
        }
    }

    /**
     * 推送当前协议已屏蔽
     */
    public void sendForbiddenMsg(int msgType) {
        logger.warn("该消息已临时被暂停使用  type={}, roleId={}", msgType, getRoleId());
        // this.sendMsg(new Object[] { MsgType.CommandForbidden_Msg,
        // ErrorCode.ERROR_CODE_1033 });
    }

    public void sendMsg(Object[] msg) {
        byte[] content = Amf3.toBytes(msg);
        this.sendMsg(content);
    }

    public void sendMsg(Protocol msg) {
        if (channel.isActive()) {
            BinaryWebSocketFrame blob = ProtocolEncoder.encode(msg);
            channel.writeAndFlush(blob);
        }
    }

    public void writeRequest(RpcMsg msg) throws ServiceException {
        if (!channel.isWritable()) {
            logger.debug("channel={},isActive={},isWritable={}", channel, channel.isActive(), channel.isWritable());
        }
        if (channel.isActive()) {
            channel.writeAndFlush(msg);
        }
    }

    public RpcMsg readRequest(RpcMsg rpcMsg, long seqId, CountDownLatch latch) throws ServiceException {
        logger.debug("同步监听开始 seqId={}", seqId);
        try {
            latch.await(SystemConstant.TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ServiceException(e);
        }
        logger.debug("同步监听完毕 seqId={}", seqId);
        watchDog.remove(seqId);
        RpcMsg response = responses.remove(seqId);
        if (response == null) {
            logger.error("readRequest timeout seqId={},rpcMsg={}", seqId, rpcMsg.toString());
            throw new ServiceException("readRequest timeout");
        }
        return response;
    }

    /**
     * 线程安全
     *
     * @return
     */
    private long buildSeqId() {
        return seqId.incrementAndGet();
    }
    // public void broadcastMsg(final BroadcastMsgReq req) {
    // try {
    // sendRPC(MsgType.RPC_BroadcastReq, req);
    // } catch (ServiceException e) {
    // logger.error("relayMsg failed:", e);
    // }
    // }

    public void relayMsg(long roleId, int msgType, Map<String, Object> content) {
        RelayMsgReq req = new RelayMsgReq();
        req.setRoleId(roleId);
        req.setType(msgType);
        req.setContent(content);
        this.relayMsg(req);
    }

    public void relayMsg(final RelayMsgReq req) {
        try {
            sendRPC(MsgType.RPC_RelayMsgReq, req);
        } catch (ServiceException e) {
            logger.error("relayMsg failed:", e);
        }
    }

    public <T> void dispatchEvent(DispatchEventReq<T> req) {
        try {
            sendRPCWithClassName(MsgType.RPC_DispatchEventReq, req);
        } catch (ServiceException e) {
            logger.error("relayMsg failed:", e);
        }
    }

    /** 发RPC请求 */
    public void sendRPCWithClassName(int msgType, Object msg) throws ServiceException {
        RpcMsg rpcMsg = RpcMsg.newBuilder().setServer(true).setSync(false).setSeqId(buildSeqId())
                        .setTimestamp(System.nanoTime()).setType(msgType)
                        .setContent(JSON.toJSONString(msg, SerializerFeature.WriteClassName)).build();
        writeRequest(rpcMsg);

    }

    /** 发RPC请求 */
    public void sendRPC(int msgType, Object msg) throws ServiceException {
        RpcMsg rpcMsg = RpcMsg.newBuilder().setServer(true).setSync(false).setSeqId(buildSeqId())
                        .setTimestamp(System.nanoTime()).setType(msgType)
                        .setContent(JSON.toJSONString(msg, SerializerFeature.WriteClassName)).build();
        writeRequest(rpcMsg);

    }

    /** 发RPC请求 */
    public <R> R sendRPCWithReturn(int msgType, Object msg, TypeReference<R> features) throws ServiceException {
        RpcMsg rpcMsg = RpcMsg.newBuilder().setServer(true).setSync(true).setSeqId(buildSeqId())
                        .setTimestamp(System.nanoTime()).setType(msgType)
                        .setContent(JSON.toJSONString(msg, SerializerFeature.WriteClassName)).build();
        CountDownLatch latch = new CountDownLatch(1);
        watchDog.put(rpcMsg.getSeqId(), latch);
        writeRequest(rpcMsg);
        return JSON.parseObject(readRequest(rpcMsg, rpcMsg.getSeqId(), latch).getContent(), features);
    }

    public <R> R sendRPCWithReturn(int msgType, Object msg, Class<R> features) throws ServiceException {
        RpcMsg rpcMsg = RpcMsg.newBuilder().setServer(true).setSync(true).setSeqId(buildSeqId())
                        .setTimestamp(System.nanoTime()).setType(msgType)
                        .setContent(JSON.toJSONString(msg, SerializerFeature.WriteClassName)).build();
        CountDownLatch latch = new CountDownLatch(1);
        watchDog.put(rpcMsg.getSeqId(), latch);
        writeRequest(rpcMsg);
        return JSON.parseObject(readRequest(rpcMsg, rpcMsg.getSeqId(), latch).getContent(), features);
    }

    public void setResponse(long seqId, RpcMsg response) {
        responses.put(seqId, response);
    }

    public CountDownLatch getCountDownLatch(long seqId) {
        return watchDog.get(seqId);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String sessionId) {
        this.id = sessionId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void close() {
        if (channel.isActive()) {
            // fix ：An exceptionCaught() event was fired, and it reached at the
            // tail of the pipeline. It usually means the last handler in the
            // pipeline did not handle the exception.: java.io.IOException: Too
            // many open files
            // Netty server does not close/release socket
            try {
                channel.close().awaitUninterruptibly(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        }
    }

    public String getClientIp() {
        if (ip == null) {
            InetSocketAddress remoteAddr = (InetSocketAddress) this.getChannel().remoteAddress();
            ip = remoteAddr.getAddress().getHostAddress();
        }
        return ip;
    }

    public boolean checkInterval(long time) {
        long realInterval = (time - startTime);
        return realInterval >= interval * 0.8;
    }

    public int genInterval(long time) {
        this.interval = RandomUtils.nextInt(30000, 40000);
        this.startTime = time;
        return interval;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Session))
            return false;

        Session session = (Session) o;

        return id == session.id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
