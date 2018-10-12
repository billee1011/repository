package com.lingyu.common.io;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.codec.Protocol;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.util.Lottery;
import com.lingyu.msg.rpc.DispatchEventReq;

import io.netty.channel.Channel;

public class SessionManager {
	private static int peakCount;
	private static final Logger logger = LogManager.getLogger(SessionManager.class);

	private SessionManager() {
	}

	public static SessionManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final SessionManager INSTANCE = new SessionManager();
	}

	private ConcurrentMap<Channel, Session> sessionMap = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Session> userMap = new ConcurrentHashMap<>();
	private ConcurrentMap<Long, Session> roleMap = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Session> sessionIdMap = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Session> sessionId2IPMap = new ConcurrentHashMap<>();

	private final ReentrantLock LOCK = new ReentrantLock();

	public synchronized Session addSession(int type, Channel channel) {
		String sessionId = DefaultChannelId.newInstance().asLongText();
		Session result = new Session(type, channel, sessionId);
		logger.info("create session {}", sessionId);
		sessionIdMap.put(sessionId, result);
		sessionMap.put(channel, result);

		InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
		String host = insocket.getAddress().getHostAddress();
		sessionId2IPMap.put(host, result);
		return result;
	}

	public boolean isOnline(String userId) {
		return userMap.containsKey(userId);
	}

	public boolean isOnline(long roleId) {
		return roleMap.containsKey(roleId);
	}

	public void addSession4Role(Session session, long roleId) {
		// logger.info("加入session 管理 sessionId={}", sessionId);
		roleMap.put(roleId, session);
		int size = roleMap.size();
		if (peakCount < size) {
			peakCount = size;
			logger.info("在线创造峰值 peakCount={}", peakCount);
		}
		if (session.getType() == SystemConstant.SESSION_TYPE_RPC) {
			session.addRole4RPC(roleId);
		}

	}

	public Session getSession4User(String userId) {
		return userMap.get(userId);
	}

	public Session getSession4IP(String ip) {
		return sessionId2IPMap.get(ip);
	}

	public long getOnlineRoleId(String userId) {
		long ret = 0;
		Session session = this.getSession4User(userId);
		if (session != null) {
			ret = session.getRoleId();
		}
		return ret;
	}

	public Session getSession4Role(long roleId) {
		return roleMap.get(roleId);

	}

	public void addSession4User(String userId, Session session) {
		userMap.put(userId, session);
	}

	/***
	 * 替换session
	 *
	 * @param oldUserId
	 * @param newUserId
	 */
	public void replaceSession4User(String oldUserId, String newUserId) {
		Session session = userMap.remove(oldUserId);
		userMap.put(newUserId, session);
	}

	public void removeRole(Session session, long roleId) {
		logger.info("清除角色 roleId={}", roleId);
		roleMap.remove(roleId);
		if (session.getType() == SystemConstant.SESSION_TYPE_RPC) {
			session.removeRole4RPC(roleId);
		}
	}

	public List<Long> getRandomList(int num) {
		return Lottery.random(roleMap.keySet(), num);
	}

	/** 用户获取RPC 过来的用户列表 */
	public Collection<Long> getRoleList(String sessionId) {
		Session session = sessionIdMap.get(sessionId);
		Collection<Long> list = session.getRoleList4RPC();
		return list;
	}

	private Collection<Long> removeRoleList(Session session) {
		Collection<Long> list = session.getRoleList4RPC();
		for (Long roleId : list) {
			roleMap.remove(roleId);
		}
		if (CollectionUtils.isNotEmpty(list)) {
			logger.info("由于连接断开，玩家被从跨服区清除 roleIds={}", list);
		}
		return list;
	}

	// public void removeSession(long roleId) {
	// Session session = roleMap.get(roleId);
	// if (session != null) {
	// this.removeSession(session.getChannel());
	// }
	// }

	public synchronized Collection<Long> removeSession(Channel channel) {
		Collection<Long> ret = new ArrayList<>();
		InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
		String host = insocket.getAddress().getHostAddress();
		sessionId2IPMap.remove(host);
		Session session = sessionMap.remove(channel);
		if (session != null) {
			sessionIdMap.remove(session.getId());
			String sessionId = session.getId();
			if (session.getType() == SystemConstant.SESSION_TYPE_PLAYER) {
				String userId = session.getUserId();
				if (userId != null) {
					Session userSession = userMap.get(userId);
					if (userSession.equals(session)) {
						userMap.remove(userId);
					}
				}
				long roleId = session.getRoleId();
				if (roleId != 0) {
					Session roleSession = roleMap.get(roleId);
					// fix bug 解决新的session被误干掉的问题
					if (roleSession != null && roleSession.equals(session)) {
						roleMap.remove(roleId);
					}
				}
			} else if (session.getType() == SystemConstant.SESSION_TYPE_RPC) {
				// 清理掉通过这个session通讯的所有玩家
				ret = this.removeRoleList(session);
			}
			logger.info("remove session {}", sessionId);
		}

		return ret;
	}

	public Session getSession(Channel channel) {
		return sessionMap.get(channel);
	}

	public Collection<Session> getOnlineRoleList() {
		return sessionMap.values();
	}

	/** 根据人名搜索角色ID */
	public List<Long> getOnlineRoleIdLikeName(long roleId, String name, int maxCount) {
		List<Long> ret = new ArrayList<>();
		int i = 0;
		Collection<Session> list = this.getOnlineRoleList();
		for (Session e : list) {
			if (roleId != e.getRoleId()) {
				if (e.getRoleName() != null && (StringUtils.isEmpty(name) || e.getRoleName().indexOf(name) >= 0)) {
					ret.add(e.getRoleId());
					i++;
					if (i >= maxCount) {
						return ret;
					}
				}
			}
		}
		return ret;
	}

	public void closeAllConnections() {
		logger.info("closeAllConnections");
		LOCK.lock();
		try {
			for (Session session : sessionMap.values()) {
				session.getChannel().disconnect();
			}
		} finally {
			LOCK.unlock();
		}
	}

	/**
	 * 在线玩家个数
	 *
	 * @return
	 */
	public int getOnlineCount() {
		return roleMap.size();
	}

	/** 连接数 */
	public int getConnectionNum() {
		return sessionIdMap.size();
	}

	public Set<Long> getOnlineRoleIdList() {
		return roleMap.keySet();
	}

	/**
	 * 广播消息给玩家
	 *
	 * @param roleId
	 *            需要排除的角色id【把roleList集合中的roleId排除掉】
	 * @param roleList
	 *            需要广播的角色id
	 */
	public void broadcastByteMsgToClient(long roleId, long[] roleList, byte[] msg) {
		for (long otherId : roleList) {
			if (roleId == otherId) {
				continue;
			}
			this.relayByteToClient(otherId, msg);
			// try {
			// Session session = this.getSession4Role(otherId);
			// if (session != null) {
			// session.sendMsg(msg);
			// }
			// } catch (Exception e) {
			// logger.error(e.getMessage(), e);
			// }
		}
	}

	public void relayByteToClient(long roleId, byte[] msg) {
		try {
			Session session = this.getSession4Role(roleId);
			if (session != null) {
				session.sendMsg(msg);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 全服的广播
	 *
	 * @param roleId
	 *            排除自己广播
	 *
	 * @param msg
	 */
	public void broadcast(long roleId, int type, JSONObject msg) {
		Protocol protocol = new Protocol();
		protocol.cmd = type;
		protocol.body = msg;
		Collection<Session> list = sessionMap.values();
		for (Session session : list) {
			try {
				if (session.getType() == SystemConstant.SESSION_TYPE_PLAYER) {
					if (session.getRoleId() != roleId) {
						session.sendMsg(protocol);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		}
	}

	/**
	 * 有接受列表的广播
	 *
	 * @param roleId
	 *            排除自己广播
	 */
	public void broadcast(long roleId, long[] list, int type, JSONObject msg) {
		if (list == null || list.length == 0) {
			return;
		}
		Protocol protocol = new Protocol();
		protocol.cmd = type;
		protocol.body = msg;
		Map<Session, List<Long>> map = null;
		if (SystemConstant.serverType != SystemConstant.SERVER_TYPE_GAME) {
			map = new HashMap<>();
		}
		for (long otherId : list) {
			if (roleId == otherId) {
				continue;
			}
			try {
				Session session = this.getSession4Role(otherId);
				if (session == null) {
					continue;
				}
				if (session.getType() == SystemConstant.SESSION_TYPE_PLAYER) {
					session.sendMsg(protocol);
				} else {
					if (SystemConstant.serverType == SystemConstant.SERVER_TYPE_GAME) {
						session.relayMsg(otherId, type, msg);
					} else {
						List<Long> roleList = map.get(session);
						if (roleList == null) {
							roleList = new ArrayList<>();
							map.put(session, roleList);
						}
						roleList.add(otherId);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	public void relayMsg(int command, long roleId, JSONObject message) {
		Session session = this.getSession4Role(roleId);
		if (null != session) {
			if (session.getType() == SystemConstant.SESSION_TYPE_PLAYER) {
				Protocol protocol = new Protocol();
				protocol.cmd = command;
				protocol.body = message;
				session.sendMsg(protocol);
			} else {
				if (command < 0) {
					session.relayMsg(roleId, command, message);// 系统的内部指令
				}
			}
		} else {
			logger.error("no session roleId={},type={}, msg={}", roleId, command, message);
		}
	}

	public void relayMsg(int command, String userId, JSONObject message) {
		Session session = this.getSession4User(userId);
		if (null != session) {
			Protocol protocol = new Protocol();
			protocol.cmd = command;
			protocol.body = message;
			session.sendMsg(protocol);
		} else {
			logger.error("no session {}", message);
		}
	}

	public <T> void dispatchEvent(long roleId, DispatchEventReq<T> req) {
		Session session = this.getSession4Role(roleId);
		if (null != session) {
			session.dispatchEvent(req);
		}

	}
}
