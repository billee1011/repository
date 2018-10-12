package com.lingyu.common.io;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;

public class BusMsg implements IMsg {
    private static RouteManager routeManager = GameServerContext.getBean(RouteManager.class);
    private long roleId;
    private int command;
    private JSONObject result;

    public BusMsg(long roleId, int command, JSONObject result) {
        this.roleId = roleId;
        this.command = command;
        this.result = result;
    }

    @Override
    public void flush() {
        routeManager.relay2BusCache(roleId, command, result);
    }

}