package com.lingyu.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.common.message.BusMsgDispatcher;
import com.lingyu.common.message.GameCommand;
import com.lingyu.common.message.PublicMsgDispatcher;

@Service
public class RouteManager {
	private static final Logger logger = LogManager.getLogger(RouteManager.class);
	@Autowired
	private PublicMsgDispatcher publicMsgDispatcher;
	@Autowired
	private BusMsgDispatcher busMsgDispatcher;
	@Autowired
	private GameAdlProcessor processor;
	private SessionManager sessionManager = SessionManager.getInstance();
	private static Map<Integer, GameCommand> commandStore = new HashMap<>();
	private static Map<Integer, GameCommand> invalidStore = new HashMap<>();

	public void handleMsg(Session session, int msgType, JSONObject msgData) {
		try {
			GameCommand command = this.getCommand(msgType);
			if (command == null) {
				logger.error("未有对客户端的协议进行处理的方法定义  type={}", msgType);
				return;
			}
			// 需要打印
			if (command.isPrint() && msgType != MsgType.CLIENT_HEART_BEAT_MSG) {
				logger.debug("recv message roleId={}, type={}", session.getRoleId(), msgType);
			}
			command.increase();// 计数
			if (!command.isValid()) {
				session.sendForbiddenMsg(msgType);
				return;
			}
			this.normalDispatch(session, msgType, command, msgData);
		} catch (Exception e) {
			logger.error("异常消息 msg={}", msgData);
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 场景模块消息分发
	 */
	public void relayStageMsgDispatch(Session session, int msgType, GameCommand command, JSONObject msg) {
		this.normalDispatch(session, msgType, command, msg);
	}

	/**
	 * 普通消息分发
	 */
	public void normalDispatch(Session session, int msgType, GameCommand command, JSONObject data) {
		processor.dispatch(session, msgType, data);
		this.dispatch(session.getUserId(), session.getRoleId(), command, data);
	}

	/*
	 * public void handleMsg(Session session, int msgType, Object[] msg) { try {
	 * GameCommand command = this.getCommand(msgType); if (command == null) {
	 * logger.error("未有对客户端的协议进行处理的方法定义  type={}", msgType); return; }
	 * command.increase();// 计数 if (!command.isValid()) {
	 * session.sendForbiddenMsg(msgType); return; } if (command.isStageGroup())
	 * { this.stageMsgDispatch(session, msgType, command, msg); } else {
	 * this.normalDispatch(session, msgType, command, msg); } } catch (Exception
	 * e) { logger.error("异常消息 msg={}", msg); logger.error(e.getMessage(), e); }
	 * }
	 */

	/** 分发来自客户端的消息 */
	public void dispatch(String userId, long roleId, GameCommand command, JSONObject msg) {
		int msgType = command.getValue();
		if (msgType == MsgType.CLIENT_HEART_BEAT_MSG) {
			return;
		}
		// if (roleId == 0 && command.getGroup() !=
		// SystemConstant.GROUP_BUS_INIT) {
		// logger.info("消息不归属任何角色，不进行处理,command={},roleId={}",
		// command.getValue(), roleId);
		// return;
		// }
		switch (command.getGroup()) {
		// case SystemConstant.GROUP_STAGE: {
		// stageMsgDispatcher.invoke(command.getValue(), roleId,
		// stageManager.getStageId(roleId), msg);
		// break;
		// }
		case SystemConstant.GROUP_BUS_CACHE: {
			if (msgType == MsgType.LoginGame_C2S_Msg) {
				busMsgDispatcher.invoke(msgType, userId, command.getGroup(), msg);
			} else {
				busMsgDispatcher.invoke(msgType, roleId, command.getGroup(), msg);
			}
			break;
		}
		case SystemConstant.GROUP_PUBLIC: {
			publicMsgDispatcher.invoke(command.getValue(), roleId, command.getModule(), msg);
			break;
		}
		// case SystemConstant.GROUP_BUS_INIT: {
		// busMsgDispatcher.invoke(command.getValue(), userId,
		// command.getGroup(), msg);
		// break;
		// }
		}
	}

	public void relay2BusInit(String userId, int command, JSONObject message) {
		busMsgDispatcher.invoke(command, userId, SystemConstant.GROUP_BUS_INIT, message);
	}

	public void relay2BusCache(long roleId, int command, JSONObject message) {
		if (SystemConstant.serverType == SystemConstant.SERVER_TYPE_GAME) {
			// 如果是游戏服，直接本服处理，如果是跨服，则找到角色所在的源服 ，往那边发消息
			busMsgDispatcher.invoke(command, roleId, SystemConstant.GROUP_BUS_CACHE, message);
		} else {
			sessionManager.relayMsg(command, roleId, message);
		}
	}

	public void relay2Public(long roleId, int command, JSONObject message) {
		GameCommand gameCommand = this.getCommand(command);
		// 如果是游戏服，直接本服处理，如果是跨服，则找到角色所在的源服 ，往那边发消息
		if (SystemConstant.serverType == SystemConstant.SERVER_TYPE_GAME) {
			publicMsgDispatcher.invoke(command, roleId, gameCommand.getModule(), message);
		} else {
			sessionManager.relayMsg(command, roleId, message);
		}
	}

	/** 用于和角色无关的行为，比如定时刷排行 榜,只会在本服发生 */
	public void relay2Public(int command, JSONObject message) {
		this.relay2Public(0, command, message);
	}

	/******************************************************/
	/**
	 * 跨服转发回来转发给客户端玩家的消息
	 * <p>
	 * <b>备注:广播的消息为amf3 byte[]</b>
	 */
	public void relayByteMsgToClient(long roleId, byte[] msg) {
		sessionManager.relayByteToClient(roleId, msg);
	}

	/**
	 * 跨服转发回来广播给客户端玩家的消息
	 * <p>
	 * <b>备注:广播的消息为amf3 byte[]</b>
	 *
	 * @param roleId
	 *            需要排除的角色id【把roleList集合中的roleId排除掉】
	 * @param roleList
	 *            需要广播的角色id
	 */
	public void broadcastByteMsgToClient(long roleId, long[] roleList, byte[] msg) {
		sessionManager.broadcastByteMsgToClient(roleId, roleList, msg);
	}

	/** 对单人 */
	public void relayMsg(String userId, int command, JSONObject message) {
		SessionManager.getInstance().relayMsg(command, userId, message);
	}

	/** 对单人 */
	public void relayMsg(long roleId, int command, JSONObject message) {
		SessionManager.getInstance().relayMsg(command, roleId, message);
	}

	/** 广播所有在线的人 */
	public void broadcast(int command, JSONObject message) {
		this.broadcast(0, command, message);
	}

	/** 广播，排除角色roleId */
	public void broadcast(long roleId, int command, JSONObject message) {
		sessionManager.broadcast(roleId, command, message);
	}

	/** 广播 被广播的列表 list */
	public void broadcast(long[] list, int command, JSONObject message) {
		this.broadcast(0, list, command, message);
	}

	/** 广播 被广播的列表 list 排除角色roleId */
	public void broadcast(long roleId, long[] list, int command, JSONObject message) {
		sessionManager.broadcast(roleId, list, command, message);
	}

	/**
	 * 注册指令
	 *
	 * @param cmdList
	 */
	public static void register(GameCommand command) {
		if (commandStore.containsKey(command.getValue())) {
			throw new ServiceException("指令注册重复 command={}", command.getValue());
		} else {
			commandStore.put(command.getValue(), command);
		}
	}

	public List<GameCommand> getCommandList() {
		List<GameCommand> ret = new ArrayList<GameCommand>();
		Collection<GameCommand> list = commandStore.values();
		for (GameCommand e : list) {
			if (e.getValue() > 0) {
				ret.add(e);
			}
		}
		return ret;
	}

	public GameCommand getCommand(int command) {
		return commandStore.get(command);
	}

	/** 改变命令的可用性 */
	public GameCommand updateCommand(int command, boolean status) {
		logger.info("命令修改状态 command={},status={}", command, status);
		GameCommand gameCommand = this.getCommand(command);
		if (gameCommand != null) {
			gameCommand.setValid(status);
			if (status) {
				invalidStore.remove(command);
			} else {
				invalidStore.put(command, gameCommand);
			}
		}
		return gameCommand;

	}

	/** 获取无效的命令 */
	public Collection<GameCommand> getInValidCommandList() {
		return invalidStore.values();
	}
}
