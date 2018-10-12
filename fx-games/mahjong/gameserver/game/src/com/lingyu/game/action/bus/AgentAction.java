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
import com.lingyu.common.util.ObjectUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.agent.AgentManager;
import com.lingyu.game.service.mahjong.MahjongConstant;

@Controller
@GameAction(module = ModuleConstant.MODULE_AGENT, group = SystemConstant.GROUP_BUS_CACHE)
public class AgentAction {
	@Autowired
	private AgentManager agentManager;
	@Autowired
	private RouteManager routeManager;

	/**
	 * 转账前准备
	 * 
	 * @param roleId
	 * @param msg
	 */
	@GameMapping(value = MsgType.AGENT_TRANSFER_READY)
	public void queryRoleAndTransFerLog(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		long toRoleId = ObjectUtil.obj2long(objects[0]);
		int haveTransferLog = ObjectUtil.obj2int(objects[0]); // 0 不需要转账记录 1，需要

		JSONObject result = agentManager.queryRoleAndTransFerLog(roleId, toRoleId, haveTransferLog);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER_READY, result);
		}
	}

	/**
	 * 转账
	 *
	 * @param msg
	 */
	@GameMapping(value = MsgType.AGENT_TRANSFER)
	public void transfer(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		long toRoleId = ObjectUtil.obj2long(objects[0]);
		long diamoud = ObjectUtil.obj2long(objects[1]);

		JSONObject result = agentManager.transfer(roleId, toRoleId, diamoud);
		if (result != null) {
			routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER, result);
		}
	}

	/**
	 * 查询代理自己的转账记录
	 * 
	 * @param roleId
	 * @param msg
	 */
	@GameMapping(value = MsgType.AGENT_TRANSFER_LOG)
	public void queryTransferLog(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		String beginTime = String.valueOf(objects[0]);
		String endTime = String.valueOf(objects[1]);

		JSONObject res = agentManager.queryTransferLog(roleId, beginTime, endTime);
		if (res != null) {
			routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER_LOG, res);
		}
	}

	/**
	 * 开通代理
	 * 
	 * @param roleId
	 * @param msg
	 */
	@GameMapping(value = MsgType.AGENT_OPEN)
	public void openAgent(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		long toRoleId = ObjectUtil.obj2long(objects[0]);
		int type = ObjectUtil.obj2int(objects[1]); // 1 授权代理，2授权玩家

		JSONObject res = agentManager.openAgent(roleId, toRoleId, type);
		if (res != null) {
			routeManager.relayMsg(roleId, MsgType.AGENT_OPEN, res);
		}
	}

	/**
	 * 解散房间
	 * 
	 * @param roleId
	 * @param msg
	 */
	@GameMapping(value = MsgType.AGENT_DIS_MISS_ROOM)
	public void agentDisMissRoom(long roleId, JSONObject msg) {
		JSONArray jsonArray = (JSONArray) msg.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int roomNum = ObjectUtil.obj2int(objects[0]);

		JSONObject res = agentManager.agentDisMissRoom(roleId, roomNum);
		if (res != null) {
			routeManager.relayMsg(roleId, MsgType.AGENT_DIS_MISS_ROOM, res);
		}
	}
}
