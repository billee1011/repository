package com.lingyu.game;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;

public abstract class GameAdlProcessor {
    // private static final Logger logger =
    // LogManager.getLogger(GameAdlProcessor.class);
    abstract public void heartBeat(Session session, JSONObject msg);

    abstract public void keepAlive(Session session, JSONObject msg);

    abstract public void getRoleList(Session session, JSONObject msg);

    abstract public void creatRole(Session session, JSONObject msg);

    abstract public void loginGame(Session session, JSONObject msg);

    public void dispatch(Session session, int type, JSONObject msg) {
        switch (type) {
        case MsgType.CLIENT_HEART_BEAT_MSG:
            keepAlive(session, msg);
            return;
        // case MsgType.GetRoleList_C2S_Msg:
        // getRoleList(session, msg);
        // break;
        case MsgType.LoginGame_C2S_Msg:
            loginGame(session, msg);
            break;
        // case MsgType.CREATE_USER_MSG:
        // creatRole(session, msg);
        // break;
        /*
         * case MsgType.Ping_Msg: keepAlive(session, msg); return ; case
         * MsgType.GetRoleList_C2S_Msg: getRoleList(session, msg); break; case
         * MsgType.CreatRole_C2S_Msg: creatRole(session, msg); break; case
         * MsgType.LoginGame_C2S_Msg: loginGame(session, msg); break;
         */

        default:
            // logger.error("unknown msg type: {}", type);
            break;
        }
        // 第二个值其实没有用，在中间过程中会修改掉 dest
        // NodeSwap.swap(new Object[] { String.valueOf(type), msg, 0,
        // SystemConstant.from_type_client, SystemConstant.broadcast_type_ONE,
        // null, null, session.getRoleId(), null, null, null,
        // session.getUserId() });

    }
}
