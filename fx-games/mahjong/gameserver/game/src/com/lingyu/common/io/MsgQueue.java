package com.lingyu.common.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/**
 * @description 公共消息发送队列
 * @author ShiJie Chi
 * @date 2012-4-19 下午4:08:37
 */
public class MsgQueue {

    private List<IMsg> msgs = new ArrayList<IMsg>();

    private boolean fifoOrlifo = true;

    /**
     * 增加发送消息
     *
     * @param busMsg
     */
    private void addMsg(IMsg busMsg) {
        if (!fifoOrlifo && msgs.size() > 0) {
            msgs.add(0, busMsg);
        } else {
            msgs.add(busMsg);
        }
    }

    /**
     * 输出所有消息
     */
    public void flush() {

        if (msgs.size() > 0) {
            for (IMsg msg : msgs) {
                msg.flush();
            }
        }

    }

    /**
     * 增加发往客户端的单发消息
     */
    public void addMsg(long roleId, int command, JSONObject result) {
        addMsg(new ClientMsg(roleId, command, result));
    }

    /**
     * 增加发往客户端的多发消息
     */
    public void addMsg(long[] roleIds, int command, JSONObject result) {

        addMsg(new MultiClientMsg(roleIds, command, result));
    }

    /**
     * 增加广播消息
     */
    public void addBroadcastMsg(int command, JSONObject result) {
        addMsg(new BroadcastMsg(command, result));
    }

    /**
     * 增加发往业务的消息
     */
    public void addBusMsg(long roleId, int command, JSONObject result) {
        addMsg(new BusMsg(roleId, command, result));
    }

    /**
     * 增加发往业务内部的消息
     */
    public void addInnerBusMsg(long roleId, int command, JSONObject result) {
        addMsg(new BusMsg(roleId, command, result));
    }

    /**
     * 消息输出顺序规则(默认先进先出)
     *
     * @param fifoOrlifo
     *            true:先进先出 false:后进先出
     */
    public void orderRule(boolean fifoOrlifo) {
        this.fifoOrlifo = false;
    }

}
