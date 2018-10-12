package com.lingyu.game.action.publik;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.message.GameAction;
import com.lingyu.common.message.GameMapping;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.announce.AnnounceManager;
import com.lingyu.game.service.chat.ChatManager;

@Controller
@GameAction(module = ModuleConstant.MODULE_CHAT, group = SystemConstant.GROUP_PUBLIC)
public class ChatAction {
    @Autowired
    private ChatManager chatManager;
    @Autowired
    private RouteManager routeManager;
    @Autowired
    private AnnounceManager announceManager;

    /**
     * 聊天
     */
    @GameMapping(value = MsgType.MAHJONG_CHAT_MSG)
    public void normalChat(long roleId, JSONObject data) {
        JSONObject result = chatManager.normalChat(roleId, data);
        if (result != null) {
            routeManager.relayMsg(roleId, MsgType.MAHJONG_CHAT_MSG, result);
        }
    }

    /***
     * 拉取公告信息
     *
     * @param roleId
     * @param inMsg
     */
    @GameMapping(value = MsgType.Announce_List_Msg)
    public void getAnnounceList(long roleId, JSONObject inMsg) {
        JSONObject ret = announceManager.getAnnouncingOrFutureListMsg();
        routeManager.relayMsg(roleId, MsgType.Announce_List_Msg, ret);
    }

}
