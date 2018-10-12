package com.lingyu.game.service.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.Role;
import com.lingyu.game.service.mahjong.MahjongManager;
import com.lingyu.game.service.role.RoleManager;

@Service
public class ChatManager {
    @Autowired
    private RoleManager roleManager;
    @Autowired
    private MahjongManager mahjongManager;

    /**
     * 房间聊天
     *
     * @param roleId
     * @param data
     * @return
     */
    public JSONObject normalChat(long roleId, JSONObject data) {
        Role role = roleManager.getRole(roleId);
        JSONObject result = new JSONObject();
        if (role == null) {
            result.put(ErrorCode.RESULT, ErrorCode.FAILED);
            result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
            return result;
        }
        int roomNum = role.getRoomNum();
        if (roomNum == 0) {
            result.put(ErrorCode.RESULT, ErrorCode.FAILED);
            result.put(ErrorCode.CODE, ErrorCode.ROOM_NOT_EXIST);
            return result;
        }

        mahjongManager.chat(roleId, roomNum, data);
        return null;
    }
}
