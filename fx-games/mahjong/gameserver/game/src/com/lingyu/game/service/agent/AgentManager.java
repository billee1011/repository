package com.lingyu.game.service.agent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.CurrencyConstant.CurrencyCostType;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;
import com.lingyu.common.constant.OperateConstant.OperateType;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.Agent;
import com.lingyu.common.entity.AgentLog;
import com.lingyu.common.entity.Role;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.currency.MoneyManager;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.game.service.mahjong.MahjongManager;
import com.lingyu.game.service.role.RoleManager;

@Service
public class AgentManager {

	@Autowired
	private AgentRepository agentRepository;
	@Autowired
	private AgentLogRepository agentLogRepository;
	@Autowired
	private MoneyManager moneyManager;
	@Autowired
	private IdManager idManager;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private RouteManager routeManager;
	@Autowired
	private MahjongManager mahjongManager;

	public void init() {
		agentRepository.loadAll();
	}

	public boolean isAgent(Long roleId) {
		return agentRepository.getAgent(roleId) == null ? false : true;
	}

	public JSONObject openAgent(long roleId, long toRoleId, int type) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);

		if (!isAgent(roleId)) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.IS_NOT_AGENT);
			return res;
		}
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		Role toRole = roleManager.getRole(toRoleId);
		if (toRole == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		Agent agentVO = agentRepository.getAgent(roleId);
		if (agentVO.getType() == AgentConstant.AGENT_LEVEL_MAX) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.NO_JUR_OPEN_AGENT);
			return res;
		}
		if (type == 1) {

			Agent toRoleAgent = new Agent();
			toRoleAgent.setId(idManager.newId(TableNameConstant.AGENT));
			toRoleAgent.setRoleId(toRoleId);
			toRoleAgent.setType(agentVO.getType() + 1);
			toRoleAgent.setAddTime(new Date());
			agentRepository.cacheInsert(toRoleAgent);
			res.put(MahjongConstant.CLIENT_DATA,
			        new Object[] { 1, roleId, role.getName(), toRoleId, toRole.getName() });

		} else if (type == 2) {
			Agent toAgentVO = agentRepository.getAgent(toRoleId);
			if (toAgentVO == null) {
				res.put(ErrorCode.RESULT, ErrorCode.FAILED);
				res.put(ErrorCode.CODE, ErrorCode.IS_NOT_AGENT);
				return res;
			}
			if (agentVO.getType() >= toAgentVO.getType()) {
				res.put(ErrorCode.RESULT, ErrorCode.FAILED);
				res.put(ErrorCode.CODE, ErrorCode.JURISDICTION_INSUF);
				return res;
			}
			agentRepository.cacheDelete(toAgentVO);
			res.put(MahjongConstant.CLIENT_DATA,
			        new Object[] { 2, roleId, role.getName(), toRoleId, toRole.getName() });
		}

		routeManager.relayMsg(roleId, MsgType.AGENT_OPEN, res);
		routeManager.relayMsg(toRoleId, MsgType.AGENT_OPEN, res);
		return null;
	}

	/**
	 * 代理个人转账记录
	 * 
	 * @param beginTime
	 * @param endTime
	 * @return
	 */
	public JSONObject queryTransferLog(Long roleId, String beginTime, String endTime) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		if (beginTime.length() != 8 || endTime.length() != 8) {
			res.put(ErrorCode.CODE, ErrorCode.PATTEN_ERROR);
			return res;
		}
		String pattern = "yyyyMMdd";
		long start = TimeUtil.getTimeStart(TimeUtil.parse(beginTime, pattern), 0);
		long end = TimeUtil.getTimeEnd(TimeUtil.parse(endTime, pattern), 0);
		int startNum = 0;
		int endNum = MahjongConstant.ZHANJI_RESULT_PAGE_SIZE;

		List<AgentLog> list = agentLogRepository.getAllResultLog(roleId, new Date(start), new Date(end), startNum,
		        endNum);

		List<Object[]> resData = new ArrayList<>();

		for (AgentLog log : list) {
			Role toRole = roleManager.getRole(log.getToRoleId());
			resData.add(new Object[] { TimeUtil.format(log.getAddTime(), "yyyy-MM-dd"), toRole.getName(),
			        toRole.getId(), log.getDiamond() });
		}

		res.put(MahjongConstant.CLIENT_DATA, resData.toArray());
		routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER_LOG, res);
		return null;
	}

	/**
	 * 转账
	 * 
	 * @param roleId
	 * @param toRoleId
	 * @param diamond
	 * @return
	 */
	public JSONObject transfer(long roleId, long toRoleId, long diamond) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);

		// if (!isAgent(roleId)) {
		// res.put(ErrorCode.RESULT, ErrorCode.FAILED);
		// res.put(ErrorCode.CODE, ErrorCode.IS_NOT_AGENT);
		// return res;
		// }
		if (diamond <= 0) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.TRANSFER_DIAMOUD_ERROR);
			return res;
		}
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		role.setDiamond(100000);
		if (role.getDiamond() < diamond) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.DIAMOND_NOT_ENOUGH);
			return res;
		}
		Role toRole = roleManager.getRole(toRoleId);
		if (toRole == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}

		moneyManager.incr(toRoleId, CurrencyType.DIAMOND_NEW, diamond, OperateType.AGENT_GET);
		moneyManager.decr(roleId, CurrencyType.DIAMOND_NEW, diamond, OperateType.AGENT_CONSUME, CurrencyCostType.ONLY);
		creatTransferLog(roleId, toRoleId, diamond);

		res.put(MahjongConstant.CLIENT_DATA, new Object[] {});
		routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER, res);

		return null;
	}

	public JSONObject queryRoleAndTransFerLog(long roleId, long toRoleId, int haveTransferLog) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		Role toRole = roleManager.getRole(toRoleId);
		if (toRole == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		Object[] toRoleInfo = new Object[] { toRole.getName(), toRole.getDiamond() };

		if (haveTransferLog == 0) {
			res.put(MahjongConstant.CLIENT_DATA, new Object[] { toRoleInfo });
			routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER_READY, res);
			return null;
		}

		List<AgentLog> agentLogs = agentLogRepository.getList4RoleIdAndToRoleId(roleId, toRoleId);
		List infos = new ArrayList<>();
		for (AgentLog log : agentLogs) {
			infos.add(new Object[] { log.getDiamond(), TimeUtil.format(log.getAddTime(), "yyyy-MM-dd") });
		}

		res.put(MahjongConstant.CLIENT_DATA, new Object[] { toRoleInfo, infos.toArray() });
		routeManager.relayMsg(roleId, MsgType.AGENT_TRANSFER_READY, res);

		return null;
	}

	public JSONObject agentDisMissRoom(Long roleId, int roomNum) {
		Role leaderRole = roleManager.getRole(roleId);
		long allRoleIds[] = mahjongManager.removeRoom(roomNum);
		// 告诉所有玩家 房间已经解散了。
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		res.put(MahjongConstant.CLIENT_DATA, new Object[] { leaderRole.getName(), leaderRole.getId() });

		routeManager.broadcast(allRoleIds, MsgType.DISSOLVED_ROOM_MSG, res);
		return null;
	}

	/**
	 * 记录转账日志
	 * 
	 * @param roleId
	 * @param toRoleId
	 * @param diamond
	 */
	public void creatTransferLog(long roleId, long toRoleId, long diamond) {
		Role role = roleManager.getRole(roleId);
		Role toRole = roleManager.getRole(toRoleId);
		AgentLog log = new AgentLog();
		log.setId(idManager.newId(TableNameConstant.AGENT_LOG));
		log.setRoleId(roleId);
		log.setToRoleId(toRoleId);
		log.setDiamond(diamond);
		log.setToRoleLastDiamond(toRole.getDiamond());
		log.setRoleLastDiamond(role.getDiamond());
		log.setAddTime(new Date());
		agentLogRepository.insert(log);
	}
}
