package com.lingyu.common.io;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.amf3.Amf3;

import io.netty.channel.Channel;

public class SessionManager {
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
	private ConcurrentMap<Integer, Session> userMap = new ConcurrentHashMap<>();

	public synchronized Session addSession(Channel channel) {
		String sessionId = DefaultChannelId.newInstance().asLongText();
		Session result = new Session(channel, sessionId);
		logger.info("create session {}", sessionId);
		// sessionIdMap.put(sessionId, result);
		sessionMap.put(channel, result);
		return result;
	}

	public Session getSession4User(int userId) {
		return userMap.get(userId);
	}

	public void addSession4User(String pid, int userId, Session session) {
		session.setUserId(userId);
		session.setPid(pid);
		userMap.put(userId, session);
	}

	public synchronized int removeSession(Channel channel) {
		int ret = 0;
		Session session = sessionMap.remove(channel);
		if (session != null) {
			int userId = session.getUserId();
			if (userId != 0) {
				Session userSession = userMap.get(userId);
				if (userSession.equals(session)) {
					userMap.remove(userId);
					ret = userId;
				}
			}
			logger.info("remove session {}", session.getId());
		}
		return ret;
	}
	

	

	public Session getSession(Channel channel) {
		return sessionMap.get(channel);
	}

	public Collection<Session> getOnlineUserList() {
		return sessionMap.values();
	}

	/**
	 * 全服的广播
	 * 
	 * @param roleId 排除自己广播
	 * 
	 * @param msg
	 */
	public void broadcast(long roleId, int type, Object[] msg) {
		Object[] array = new Object[] { type, msg };
		Collection<Session> list = sessionMap.values();
		byte[] content = Amf3.toBytes(array);
		for (Session session : list) {
			session.sendMsg(content);
		}
	}

	public void broadcast(List<Integer> list, int type, Object[] msg) {
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		Object[] array = new Object[] { type, msg };
		byte[] content = Amf3.toBytes(array);
		for (Integer userId : list) {
			Session session = this.getSession4User(userId);
			if (session != null) {
				session.sendMsg(content);
			}

		}
	}

	public void broadcast(int type, Object[] msg) {
		this.broadcast(0, type, msg);
	}

	public void relayMsg(int command, int userId, Object[] message) {
		Session session = this.getSession4User(userId);
		if (null != session) {
			Object[] array = new Object[] { command, message };
			byte[] content = Amf3.toBytes(array);
			session.sendMsg(content);
		} else {
			logger.error("no session {}", message);
		}
	}
}
