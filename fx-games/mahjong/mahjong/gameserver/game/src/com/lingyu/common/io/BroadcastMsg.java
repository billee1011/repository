package com.lingyu.common.io;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;

public class BroadcastMsg implements IMsg {
    private static RouteManager routeManager = GameServerContext.getBean(RouteManager.class);
    private int command;
    private JSONObject result;

    public BroadcastMsg(int command, JSONObject result) {
        this.command = command;
        this.result = result;
    }

    @Override
    public void flush() {
        routeManager.broadcast(command, result);
    }

}