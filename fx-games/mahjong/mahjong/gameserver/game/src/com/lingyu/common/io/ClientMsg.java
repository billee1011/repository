package com.lingyu.common.io;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;

public class ClientMsg implements IMsg {
    private static RouteManager routeManager = GameServerContext.getBean(RouteManager.class);
    private long roleId;
    private int command;
    private JSONObject result;

    public ClientMsg(long roleId, int command, JSONObject result) {
        this.roleId = roleId;
        this.command = command;
        this.result = result;
    }

    @Override
    public void flush() {
        routeManager.relayMsg(roleId, command, result);
    }

}