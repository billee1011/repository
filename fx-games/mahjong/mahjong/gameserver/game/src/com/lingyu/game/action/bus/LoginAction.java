/**
 *
 */
package com.lingyu.game.action.bus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.message.GameAction;
import com.lingyu.common.message.GameMapping;
import com.lingyu.common.util.ConvertObjectUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.game.service.user.UserManager;

@Controller
@GameAction(module = ModuleConstant.MODULE_ROLE, group = SystemConstant.GROUP_BUS_CACHE)
public class LoginAction {

	@Autowired
	private UserManager userManager;
	@Autowired
	private RouteManager routeManager;

	/**
	 * 客户端心跳
	 */
	@GameMapping(value = MsgType.CLIENT_HEART_BEAT_MSG)
	public void ewaiInfo(long roleId, JSONObject msg) {
		// 空实现就行，只是为了注册消息
	}

	@GameMapping(value = MsgType.LoginGame_C2S_Msg, relay = false)
	public void loginGame(String userId, JSONObject msg) {

		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int loginType = ConvertObjectUtil.object2int(objects[0]);
		String pid = String.valueOf(objects[1]);
		String uId = String.valueOf(objects[2]);
		String machingId = String.valueOf(objects[3]);
		String code = String.valueOf(objects[4]);

		JSONObject result = userManager.loginGame(loginType, pid, uId, machingId, code);
		if (result != null) {
			routeManager.relayMsg(userId, MsgType.LoginGame_C2S_Msg, result);
		}
	}

	/**
	 * 创建user
	 *//*
	   * @GameMapping(value = MsgType.CREATE_USER_MSG) public void
	   * createUser(String userId, Object[] msg) { Session session =
	   * SessionManager.getInstance().getSession4User(userId); String pid =
	   * String.valueOf(msg[1]); String name = String.valueOf(msg[2]); int
	   * gender = ConvertObjectUtil.object2int(msg[3]); String ip =
	   * session.getClientIp(); Object result[] = userManager.createUser(pid,
	   * userId, name, gender, ip); routeManager.relayMsg(userId,
	   * MsgType.CREATE_USER_MSG, result); }
	   */
}