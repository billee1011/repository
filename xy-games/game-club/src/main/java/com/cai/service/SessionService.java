/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.define.EPlayerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.domain.GateServerModel;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.GateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RMIUtil;
import com.cai.common.util.ServerRandomUtil;
import com.cai.common.util.SessionUtil;
import com.cai.config.SystemConfig;
import com.cai.constant.ServiceOrder;
import com.cai.dictionary.ServerDict;
import com.cai.timer.C2CRMIPingTimer;
import com.cai.timer.SessionCheckTimer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;
import com.xianyi.framework.core.transport.Connector;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.NettySocketConnector;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.Builder;

/**
 * 
 *
 * @author wu date: 2017年8月29日 下午4:07:15 <br/>
 */
@IService(order = ServiceOrder.SESSION, desc = "会话管理")
public final class SessionService extends AbstractService {

	private static final SessionService M = new SessionService();

	/**
	 * 代理服
	 */
	private final Map<Integer, C2SSession> proxyServers = Maps.newConcurrentMap();

	/**
	 * 逻辑服
	 */
	private final Map<Integer, C2SSession> logicServers = Maps.newConcurrentMap();

	/**
	 * 全部
	 */
	private final Map<Long, C2SSession> allSessions = Maps.newConcurrentMap();

	/**
	 * accountid,serverIndex
	 */
	private final Map<Long, Integer> playerProxys = Maps.newConcurrentMap();

	/**
	 * gate服连接器[index,Connector] >>> ??
	 */
	private final Map<Integer, NettySocketConnector> gateConnectors = Maps.newConcurrentMap();

	/**
	 * 
	 */
	private final Timer timer;

	private SessionService() {
		timer = new Timer("club-SessionService-Timer");
	}

	public static SessionService getInstance() {
		return M;
	}

	@Override
	public void start() throws Exception {
		timer.schedule(new C2CRMIPingTimer(), RMIUtil.RMI_PING_DELAY, RMIUtil.RMI_PING_INTERVAL);
		timer.schedule(new SessionCheckTimer(), 5 * 1000L, 10 * 1000L);

		initOrReloadGateConnector();
	}

	@Override
	public void stop() throws Exception {
		allSessions.forEach((K, V) -> {
			V.shutdownGracefully();
		});

		allSessions.clear();
		logicServers.clear();
		proxyServers.clear();
	}

	/**
	 * 
	 * @param type
	 * @param serverIndex
	 * @param session
	 */
	public void online(EServerType type, int serverIndex, final C2SSession session) {

		C2SSession oldSession = null;

		if (type == EServerType.PROXY) {
			oldSession = proxyServers.put(serverIndex, session);
			setSessionInfo(type, serverIndex, session);
		} else if (type == EServerType.LOGIC) {
			oldSession = logicServers.put(serverIndex, session);
			setSessionInfo(type, serverIndex, session);
		} else {
			logger.error("club server unallow {} server connect!", type);
			return;
		}

		if (null != oldSession && oldSession.getSessionId() != session.getSessionId()) {
			logger.error("[{},{}]重复登陆俱乐部，关闭旧连接! sessionid:{}", type.name(), serverIndex, oldSession.getSessionId());
			oldSession.shutdownGracefully();
		}
		logger.info("[{}<->club] ###### online, channel:{},serverIndex:{}!", type.name(), session.channel(), serverIndex);
	}

	/**
	 * 
	 * @param type
	 * @param serverIndex
	 */
	public void offline(EServerType type, int serverIndex) {
		C2SSession session = null;
		if (type == EServerType.PROXY) {
			session = proxyServers.remove(serverIndex);
			removePlayerInProxy(serverIndex);
		} else if (type == EServerType.LOGIC) {
			session = logicServers.remove(serverIndex);
		}
		logger.info("[{}<->club] ###### offline, channel:{},serverIndex:{}!", type.name(), null != session ? session.channel() : "null", serverIndex);
	}

	/**
	 * 把对应代理服的玩家下线,GAME-TODO
	 * 
	 * @param serverIndex
	 */
	private void removePlayerInProxy(int serverIndex) {

	}

	/**
	 * proxy <--> gate
	 */
	protected synchronized void initOrReloadGateConnector() {

		Map<Integer, GateServerModel> gateDict = ServerDict.getInstance().getGateServerDict();
		final EServerType eType = AbstractServer.getServer().getServerType();
		GateUtil.initOrReloadConnector(gateDict, gateConnectors, eType, SystemConfig.club_index, new IOEventListener<S2SSession>() {
			@Override
			public void onEvent(IOEvent<S2SSession> ioEvent) {
			}
		});
		logger.info("[club->gate],###### init or reload Connector, Connectors:{} #######", gateConnectors);
	}

	/**
	 * 
	 * @param type
	 * @param index
	 * @return
	 */
	public C2SSession getSession(EServerType type, int index) {
		if (type == EServerType.PROXY) {
			return proxyServers.get(index);
		} else if (type == EServerType.LOGIC) {
			return logicServers.get(index);
		} else {
			logger.error("club server not hold {} server session!", type);
		}
		return null;
	}

	public void sessionCreate(C2SSession session) {
		allSessions.put(session.getSessionId(), session);
	}

	public void sessionFree(C2SSession session) {
		allSessions.remove(session.getSessionId());

		Pair<EServerType, Integer> sessionInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
		if (null != sessionInfo) {
			offline(sessionInfo.getFirst(), sessionInfo.getSecond());
		}
	}

	/**
	 * @param type
	 * @param serverIndex
	 * @param session
	 */
	private static void setSessionInfo(EServerType type, int serverIndex, final C2SSession session) {
		Pair<EServerType, Integer> sessinInfo = Pair.of(type, serverIndex);
		SessionUtil.setAttr(session, AttributeKeyConstans.CLUB_SESSION, sessinInfo);
	}

	public List<C2SSession> getAllSession() {
		return Lists.newArrayList(allSessions.values());
	}

	public boolean sendMsg(EServerType type, int index, Builder s2sResponse) {
		// TODO Auto-generated method stub
		C2SSession session = getSession(type, index);
		if (session == null) {
			logger.error("找不到对应服务器:[{},{}]", type.name(), index);
			return false;
		}
		session.send(s2sResponse);
		return true;
	}

	/**
	 * 
	 * @param accountId
	 */
	public void statusUpate(long accountId, EPlayerStatus status, int proxyServerIndex) {
		if (EPlayerStatus.ONLINE == status) {
			playerProxys.put(accountId, proxyServerIndex);
		} else if (EPlayerStatus.OFFLINE == status) {
			playerProxys.remove(accountId);
		}
		if (SystemConfig.gameDebug == 1) {
			logger.info("account[{}] status:{} ,fromProxy:{}", accountId, status.name(), proxyServerIndex);
		}
	}

	/**
	 * -1 无效
	 * 
	 * @param accountId
	 * @return
	 */
	public int getProxyByServerIndex(long accountId) {
		Integer proxyServerIndex = playerProxys.get(accountId);
		return null == proxyServerIndex ? -1 : proxyServerIndex.intValue();
	}

	public void sendMsgToProxy(Builder s2sResponse) {
		proxyServers.forEach((index, session) -> {
			session.send(s2sResponse);
		});
	}

	/**
	 * 发消息到gate服务器
	 * 
	 * @param hostIndex
	 * @param request
	 * @return
	 */
	public boolean sendGate(int hostIndex, Request request) {
		Connector connector = gateConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(request);
			return true;
		}
		return false;

	}

	/**
	 * 默认推到第一台，挂了备用服务器上
	 * 
	 * @param request
	 * @return
	 */
	public boolean sendGate(final Request request) {
		for (Map.Entry<Integer, NettySocketConnector> entry : gateConnectors.entrySet()) {
			NettySocketConnector connector = entry.getValue();
			if (connector.isActive()) {
				connector.send(request);
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * 
	 * @param request
	 * @return
	 */
	public boolean sendAllGate(final Request request) {
		for (Map.Entry<Integer, NettySocketConnector> entry : gateConnectors.entrySet()) {
			NettySocketConnector connector = entry.getValue();
			if (connector.isActive()) {
				connector.send(request);
			}
		}
		return true;
	}

	/**
	 * 不经过网关服代转[]
	 * 
	 * @param accountId
	 * @param cmd
	 * @param builder
	 * @return
	 */
	public boolean sendClient(final long accountId, int cmd, final GeneratedMessage.Builder<?> builder) {
		int pIndex = getProxyByServerIndex(accountId);
		if (pIndex > 0) {
			sendMsg(EServerType.PROXY, pIndex, PBUtil.toS_S2CRequet(accountId, cmd, builder));
		}
		return false;
	}

	/**
	 * 不经过网关服，同步数据给所有玩家
	 * 
	 * @param cmd
	 * @param builder
	 */
	public void sendAllOnline(int cmd, final GeneratedMessage.Builder<?> builder) {
		playerProxys.forEach((accountId, proxyId) -> {
			sendClient(accountId, cmd, builder);
		});
	}

	/**
	 * 随机在线代理服
	 * 
	 * @return
	 */
	public Optional<C2SSession> randomProxy() {
		return ServerRandomUtil.randomSession(proxyServers.values());
	}
}
