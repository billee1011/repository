package com.lingyu.common.io;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;

public class MultiClientMsg implements IMsg {
    private static RouteManager routeManager = GameServerContext.getBean(RouteManager.class);
    private long[] roleIds;
    private int command;
    private JSONObject result;

    public MultiClientMsg(long[] roleIds, int command, JSONObject result) {
        this.roleIds = roleIds;
        this.command = command;
        this.result = result;
    }

    @Override
    public void flush() {
        routeManager.broadcast(roleIds, command, result);
    }

}