package com.lingyu.game.action.bus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.message.GameAction;
import com.lingyu.common.message.GameMapping;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.versionnotice.VersionNoticeManager;

@Controller
@GameAction(module = ModuleConstant.MODULE_VERSION_NOTICE, group = SystemConstant.GROUP_BUS_CACHE)
public class VersionNoticeAction {
    @Autowired
    private VersionNoticeManager versionNoticeManager;
    @Autowired
    private RouteManager routeManager;

    /**
     * 拉取版本公告
     *
     * @param roleId
     * @param msg
     */
    @GameMapping(value = MsgType.VERSION_NOTICE_REFRESH)
    public void list(long roleId, JSONObject msg) {
        JSONObject result = versionNoticeManager.getAllVersionNotice(roleId);
        if (result != null) {
            routeManager.relayMsg(roleId, MsgType.VERSION_NOTICE_REFRESH, result);
        }
    }
}
