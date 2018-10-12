package com.cai.service;

import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerType;
import com.cai.common.domain.CoinGameServerModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.FoundationGameServerModel;
import com.cai.common.domain.GateServerModel;
import com.cai.common.domain.MatchGameServerModel;
import com.cai.common.util.GateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.ServerDict;
import com.cai.domain.Session;
import com.cai.net.ClubConnectorListener;
import com.cai.timer.ClubPingTimer;
import com.cai.timer.CoinPingTimer;
import com.cai.timer.FoundationPingTimer;
import com.cai.timer.GatePingTimer;
import com.cai.timer.MatchPingTimer;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.Connector;
import com.xianyi.framework.core.transport.UnresolvedAddress;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.NettySocketConnector;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.Builder;
import protobuf.clazz.s2s.S2SProto.LoginReq;

/**
 * 会话相关
 * 
 * @author run
 *
 */
public class SessionServiceImpl extends AbstractService {

	private static SessionServiceImpl instance = null;

	private Map<Long, Session> sessionMap = Maps.newConcurrentMap();

	/**
	 * 俱乐部连接
	 */
	private final Map<Integer, NettySocketConnector> clubConnectors = Maps.newConcurrentMap();

	/**
	 * 比赛服连接
	 */
	private final Map<Integer, NettySocketConnector> matchConnectors = Maps.newConcurrentMap();
	/**
	 * 金币场服连接器[index,Connector] >>> 
	 */
	private final Map<Integer, NettySocketConnector> coinConnectors = Maps.newConcurrentMap();
	
	/**
	 * 游戏基础服连接器[index,Connector] >>> 
	 */
	private final Map<Integer, NettySocketConnector> foundationConnectors = Maps.newConcurrentMap();

	/**
	 * gate服连接器[index,Connector] >>> ??
	 */
	private final Map<Integer, NettySocketConnector> gateConnectors = Maps.newConcurrentMap();

	/**
	 * 代理服连接
	 */
	private final Map<Integer, Session> proxyServers = Maps.newConcurrentMap();

	/**
	 * 全部
	 */
	private final Map<Long, C2SSession> allSessions = Maps.newConcurrentMap();

	private final Timer timer;

	private SessionServiceImpl() {
		timer = new Timer("SessionServiceImpl-Timer");
	}

	public static SessionServiceImpl getInstance() {
		if (null == instance) {
			instance = new SessionServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {
		if (SystemConfig.connectClub == 1) {
			initClubConnector();
			// 初始化俱乐部服连接
			timer.schedule(new ClubPingTimer(), 6000L, 5000L);// 发给CLUB服的心跳
		}
		if (SystemConfig.gameDebug == 0 || (SystemConfig.gameDebug == 1 && (SystemConfig.logic_index == 8 || 
				SystemConfig.logic_index == 11 || SystemConfig.logic_index == 1 || SystemConfig.logic_index == 21 ||
				SystemConfig.logic_index == 24 ||
				SystemConfig.logic_index == SystemConfig.match_id || 
				SystemConfig.logic_index == SystemConfig.register_match_id
				))) {
			initMatchConnector();
		}
		timer.schedule(new MatchPingTimer(), 6000L, 5000L);//
		if(SystemConfig.gameDebug == 0 || (SystemConfig.gameDebug == 1 && (
				SystemConfig.connectCoin == SystemConfig.logic_index || 
				SystemConfig.logic_index == SystemConfig.needConnectCoin))){
			initCoinConnector();
			timer.schedule(new CoinPingTimer(), 6000L, 5000L);//
		}

		//初始化游戏基础服连接器
		initFoundationConnector();
		timer.schedule(new FoundationPingTimer(), 6000L, 5000L);
		
		initGateConnector();
		timer.schedule(new GatePingTimer(), 6000L, 5000L);// 发给CLUB服的心跳
	}

	/**
	 * 连接match server
	 */
	protected synchronized void initMatchConnector() {

		Map<Integer, MatchGameServerModel> matchServers = ServerDict.getInstance().getMatchServerDict();
		matchServers.forEach((id, matchServer) -> {
			NettySocketConnector connector = createConnector(matchServer.getInner_ip(), matchServer.getSocket_port());
			connector.setConnectedCallback((cntor) -> {
				LoginReq.Builder builder = LoginReq.newBuilder();
				builder.setSafeCode("DFASE##@546654");
				builder.setServerType(EServerType.LOGIC.type());
				builder.setServerIndex(SystemConfig.logic_index);
				SessionServiceImpl.getInstance().sendMatch(id, PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			});
			matchConnectors.put(id, connector);
			connector.connect();
		});
		logger.info("##initMatchConnector connectors:{},size:{} !!", matchConnectors, matchConnectors.size());
	}
	
	/**
	 * 连接coin server
	 */
	protected synchronized void initCoinConnector() {

		Map<Integer, CoinGameServerModel> coinServers = ServerDict.getInstance().getCoinGameServerModelDict();
		coinServers.forEach((id, CoinServer) -> {
			NettySocketConnector connector = createConnector(CoinServer.getInner_ip(), CoinServer.getSocket_port());
			connector.setConnectedCallback((cntor) -> {
				LoginReq.Builder builder = LoginReq.newBuilder();
				builder.setSafeCode("DFASE##@546654");
				builder.setServerType(EServerType.LOGIC.type());
				builder.setServerIndex(SystemConfig.logic_index);
				SessionServiceImpl.getInstance().sendToCoin(id, PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			});
			coinConnectors.put(id, connector);
			connector.connect();
		});
		logger.info("##initCoinConnector connectors:{},size:{} !!", coinConnectors, coinConnectors.size());
	}
	
	/**
	 * 连接Foundation server
	 */
	protected synchronized void initFoundationConnector() {
		Map<Integer, FoundationGameServerModel> foundationServers = ServerDict.getInstance().getFoundationServerMap();
		foundationServers.forEach((id, CoinServer) -> {
			NettySocketConnector connector = createConnector(CoinServer.getInner_ip(), CoinServer.getSocket_port());
			connector.setConnectedCallback((cntor) -> {
				LoginReq.Builder builder = LoginReq.newBuilder();
				builder.setSafeCode("DFASE##@546654");
				builder.setServerType(EServerType.LOGIC.type());
				builder.setServerIndex(SystemConfig.logic_index);
				SessionServiceImpl.getInstance().sendToFoundation(id, PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			});
			foundationConnectors.put(id, connector);
			connector.connect();
		});
		logger.info("##initCoinConnector connectors:{},size:{} !!", coinConnectors, coinConnectors.size());
	}
	
	public boolean sendToCoin(int hostIndex, Request request) {
		Connector connector = coinConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(request);
			return true;
		}
		return false;
	}

	public void sendToCoin(Request request) {
		coinConnectors.forEach((index, match) -> {
			if (null != match && match.isActive()) {
				match.send(request);
			}
		});
	}

	public boolean sendMatch(int hostIndex, Request build) {
		// TODO Auto-generated method stub
		Connector connector = matchConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(build);
			return true;
		} else {
			logger.warn("比赛服[{} ,{} ]连接失效!", hostIndex, connector);
		}
		return false;
	}

	public void sendMatch(Request request) {
		matchConnectors.forEach((index, match) -> {
			if (null != match && match.isActive()) {
				match.send(request);
			}
		});
	}
	
	public boolean sendToFoundation(int hostIndex, Request request) {
		Connector connector = foundationConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(request);
			return true;
		}
		return false;
	}
	
	public void sendFoundation(Request request) {
		foundationConnectors.forEach((index, match) -> {
			if (null != match && match.isActive()) {
				match.send(request);
			}
		});
	}

	/**
	 * 连接club server
	 */
	protected synchronized void initClubConnector() {
		///////////////////////////////////
		final UnresolvedAddress address = new UnresolvedAddress(SystemConfig.club_socket_host);
		NettySocketConnector connector = createConnector(address.getHost(), address.getPort());
		connector.setConnectedCallback((cntor) -> {

			// 登陆成功才缓存连接
			clubConnectors.put(1, connector);

			LoginReq.Builder builder = LoginReq.newBuilder();
			builder.setSafeCode("DFASE##@546654");
			builder.setServerType(EServerType.LOGIC.type());
			builder.setServerIndex(SystemConfig.logic_index);
			SessionServiceImpl.getInstance().sendClub(1, PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			// ((NettySocketConnector)cntor).send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ,builder).build());

			logger.info("连接俱乐部成功!");

		});
		connector.connect();
	}

	/**
	 * proxy <--> gate
	 */
	public synchronized void initGateConnector() {
		Map<Integer, GateServerModel> gateDict = ServerDict.getInstance().getGateServerDict();
		GateUtil.initOrReloadConnector(gateDict, gateConnectors, EServerType.LOGIC, SystemConfig.logic_index, new IOEventListener<S2SSession>() {
			@Override
			public void onEvent(IOEvent<S2SSession> ioEvent) {
			}
		});

		logger.info("[logic->gate],###### init Connector, Connectors:{} #######", gateConnectors);
	}

	/**
	 * 
	 * @param host
	 * @param port
	 * @return
	 */
	private NettySocketConnector createConnector(String host, int port) {
		NettySocketConnector connector = new NettySocketConnector(host, port);
		connector.setListener(new ClubConnectorListener());
		connector.doInit();
		connector.setReConnect(true);
		connector.doLogin();// 未来是否需要加入链接合法性校验？？
		return connector;
	}

	/**
	 * 
	 * @param request
	 */
	public boolean sendClub(final Request request) {
		return sendClub(1, request);
	}

	/**
	 * 发消息到club服务器
	 * 
	 * @param hostIndex
	 * @param request
	 * @return
	 */
	public boolean sendClub(int hostIndex, Request request) {
		Connector connector = clubConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(request);
			return true;
		} else {
			logger.warn("俱乐部服[{} ,{} ]连接失效!", hostIndex, connector);
		}
		return false;

	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {

	}

	/**
	 * 查询sesson
	 * 
	 * @param session_id
	 * @return
	 */
	public Session getSession(long session_id) {
		return sessionMap.get(session_id);
	}

	public Map<Long, Session> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, Session> sessionMap) {
		this.sessionMap = sessionMap;
	}

	///////////////////////////////////////////////////////////////
	/**
	 * 
	 * @param type
	 * @param serverIndex
	 * @param session
	 */
	public void online(EServerType type, int serverIndex, final Session session) {

		Session oldSession = null;

		if (type == EServerType.PROXY) {
			oldSession = proxyServers.put(serverIndex, session);
			// setSessionInfo(type, serverIndex, session);
		} else {
			logger.error("club server unallow {} server connect!", type);
		}

		if (null != oldSession && oldSession.getSessionId() != session.getSessionId()) {
			logger.error("[{}]重复登陆，关闭旧连接! sessionid:{}", type.name(), oldSession.getSessionId());
			oldSession.shutdownGracefully();
		}
	}

	/**
	 * 
	 * @param type
	 * @param serverIndex
	 */
	public void offline(EServerType type, int serverIndex) {
		Session session = null;
		if (type == EServerType.PROXY) {
			session = proxyServers.remove(serverIndex);
		}
		logger.info("[{}<->] ###### offline, channel:{},serverIndex:{}!", type.name(), null != session ? session.getChannel() : "null", serverIndex);
	}

	/**
	 * 
	 * @param index
	 * @param cmd
	 * @param builder
	 */
	public boolean sendProxy(int index, int cmd, GeneratedMessage.Builder<?> builder) {
		Session session = getSession(EServerType.PROXY, index);
		if (null == session) {
			return false;
		}
		session.send(PBUtil.toS2SResponse(cmd, builder));
		return true;
	}

	public boolean sendAllProxy(int cmd, GeneratedMessage.Builder<?> builder) {
		proxyServers.forEach((serverIndex, session) -> {
			session.send(PBUtil.toS2SResponse(cmd, builder));
		});
		return true;
	}

	/**
	 * 
	 * @param type
	 * @param index
	 * @return
	 */
	public Session getSession(EServerType type, int index) {
		if (type == EServerType.PROXY) {
			return proxyServers.get(index);
		} else {
			logger.error("club server not hold {} server session!", type);
		}
		return null;
	}

	public void sessionCreate(C2SSession session) {
		allSessions.put(session.getSessionId(), session);
	}

	public C2SSession get(long sessionId) {
		return allSessions.get(sessionId);
	}

	public void sessionFree(C2SSession session) {
		allSessions.remove(session.getSessionId());

		Pair<EServerType, Integer> sessionInfo = SessionUtil.getAttr(session, AttributeKeyConstans.LOGIC_SESSION);
		if (null != sessionInfo) {
			offline(sessionInfo.getFirst(), sessionInfo.getSecond());
		}
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
	 * 发消息给客户端
	 * 
	 * @param accountId
	 * @param cmd
	 * @see S2CCmd
	 * @param builder
	 */
	public void sendClientFromGate(final long accountId, int cmd, final GeneratedMessage.Builder<?> builder) {
		if (accountId <= 0)
			return;
		this.sendGate(PBUtil.toS_S2CRequet(accountId, cmd, builder).build());
	}
	
	public Session getRandomSession(EServerType type) {
		if (type == EServerType.PROXY) {
			for (Session session : proxyServers.values()) {
				if (session.channel.isActive()) {
					return session;
				}
			}
		}
		return null;
	}
}
