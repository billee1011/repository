package com.lingyu.game.action.bus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.message.GameAction;
import com.lingyu.common.message.GameMapping;
import com.lingyu.common.util.ConvertObjectUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.mail.MailManager;

@Controller
@GameAction(module = ModuleConstant.MODULE_MAIL, group = SystemConstant.GROUP_BUS_CACHE)
public class MailAction {
    @Autowired
    private MailManager mailManager;
    @Autowired
    private RouteManager routeManager;

    /**
     * 列表
     *
     * @param msg
     */
    @GameMapping(value = MsgType.MAIL_GETINFO)
    public void list(long roleId, JSONObject msg) {
        JSONObject result = mailManager.getMailList(roleId);
        if (result != null) {
            routeManager.relayMsg(roleId, MsgType.MAIL_GETINFO, result);
        }
    }

    /**
     * 查看某一个邮件
     *
     * @param msg
     */
    @GameMapping(value = MsgType.MAIL_OPEN)
    public void openMail(long roleId, JSONObject msg) {
        int mailId = ConvertObjectUtil.object2int(msg.get("mailId"));
        JSONObject result = mailManager.openMail(roleId, mailId);
        if (result != null) {
            routeManager.relayMsg(roleId, MsgType.MAIL_OPEN, result);
        }
    }

    /**
     * 删除邮件
     *
     * @param msg
     */
    @GameMapping(value = MsgType.MAIL_REMOVE)
    public void remove(long roleId, JSONObject msg) {
        // long[] mailIds = (long[]) msg[0];
        // JSONObject result = mailManager.remove(roleId, mailIds);
        // if (result != null) {
        // routeManager.relayMsg(roleId, MsgType.MAIL_REMOVE, result);
        // }
    }

    /**
     * 领取邮件
     *
     * @param msg
     */
    @GameMapping(value = MsgType.MAIL_GAINDIAMOND)
    public void gainDaimond(long roleId, JSONObject msg) {
        // int mailId = ConvertObjectUtil.object2int(msg[0]);
        // Object result[] = mailManager.gainDiamond(roleId, mailId);
        // if (result != null) {
        // routeManager.relayMsg(roleId, MsgType.MAIL_GAINDIAMOND, result);
        // }
    }

}
