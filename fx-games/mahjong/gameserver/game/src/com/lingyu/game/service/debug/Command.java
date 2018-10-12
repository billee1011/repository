package com.lingyu.game.service.debug;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.io.MsgType;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;

/**
 * Debug命令抽象类.
 * <p>
 * 提供一个发送信息到客户端的方法，所有Debug命令实现类都要extends此类.
 *
 * @author 小流氓<zhoumingkai@lingyuwangluo.com>
 */
public abstract class Command {
    protected static RouteManager routeManager = GameServerContext.getBean(RouteManager.class);
    protected long roleId;

    /**
     * 发送信息到客户端.
     */
    public void send(String text) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        routeManager.relayMsg(roleId, MsgType.MAHJONG_GM_MSG, jsonObject);
    }

    void setRoleIdAndStageId(long roleId) {
        this.roleId = roleId;
    }

    /**
     * 参数都是从第3个元素开始的.
     */
    public abstract void analysis(String... args);

    /**
     * 执行Debug命令逻辑.
     */
    public abstract void exec();

    /**
     * 显示命令帮助格式.
     */
    public abstract String help();
}
