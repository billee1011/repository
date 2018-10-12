package com.cai.service;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.domain.CoinGameServerModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.FoundationGameServerModel;
import com.cai.common.domain.GateServerModel;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.MatchGameServerModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RMIUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.ServerDict;
import com.cai.net.core.ProxyConnectorListener;
import com.cai.timer.ClientSocketCheckTimer;
import com.cai.timer.ClubPingTimer;
import com.cai.timer.CoinPingTimer;
import com.cai.timer.DataStatTimer;
import com.cai.timer.GatePingTimer;
import com.cai.timer.LogicHeartTimer;
import com.cai.timer.MatchPingTimer;
import com.cai.timer.OnlineAccountCrossDayTimer;
import com.cai.timer.P2CRMIPingTimer;
import com.cai.timer.ServerStatusCheckTimer;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.transport.Connector;
import com.xianyi.framework.core.transport.UnresolvedAddress;
import com.xianyi.framework.core.transport.netty.NettySocketConnector;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.s2s.S2SProto.LoginReq;

/**
 * 
 * 和逻辑服连接管理中心
 * 
 * @author wu_hc
 */
public class ClientServiceImpl extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);

	private static ClientServiceImpl instance = new ClientServiceImpl();

	/**
	 * 逻辑服连接器[index,Connector] >>> ??
	 */
	private final Map<Integer, NettySocketConnector> logicConnectors = Maps.newConcurrentMap();

	/**
	 * club服连接器[index,Connector] >>> ??
	 */
	private final Map<Integer, NettySocketConnector> clubConnectors = Maps.newConcurrentMap();

	/**
	 * match服连接器[index,Connector] >>> ??
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
	 * 
	 */
	private final Timer timer;

	private ClientServiceImpl() {
		timer = new Timer("Timer-ClientServiceImpl Timer");
		initConnector();
	}

	public static ClientServiceImpl getInstance() {
		return instance;
	}

	/**
	 * 建立链接
	 */
	private synchronized void initConnector() {

		initLogicConnector();

		///////////////////////////////////

		if (SystemConfig.connectClub == 1) {
			// 初始化俱乐部服连接
			initClubConnector();
		}

		if (SystemConfig.gameDebug == 0
				|| (SystemConfig.gameDebug == 1 && (SystemConfig.proxy_index == 11 || SystemConfig.proxy_index == 24 || SystemConfig.proxy_index == 1
						|| SystemConfig.proxy_index == 21 || SystemConfig.proxy_index == SystemConfig.match_index || SystemConfig.proxy_index == SystemConfig.register_match_id))) {
			initMatchConnector();
		}

		if (SystemConfig.connectCoin == 1 || SystemConfig.connectCoin == SystemConfig.proxy_index
				|| SystemConfig.proxy_index == SystemConfig.needConnectCoin) {
			initCoinConnector();
		}

		initFoundationConnector();
		initOrReloadGateConnector();

	}

	/**
	 * proxy <--> club
	 */
	protected synchronized void initMatchConnector() {
		Map<Integer, MatchGameServerModel> matchServers = ServerDict.getInstance().getMatchServerDict();
		matchServers.forEach((id, matchServer) -> {
			NettySocketConnector connector = createConnector(matchServer.getInner_ip(), matchServer.getSocket_port());
			connector.setConnectedCallback((cntor) -> {
				LoginReq.Builder builder = LoginReq.newBuilder();
				builder.setSafeCode("DFASE##@546654");
				builder.setServerType(EServerType.PROXY.type());
				builder.setServerIndex(SystemConfig.proxy_index);
				connector.send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
				// ClientServiceImpl.getInstance().sendMatch(SystemConfig.match_index,
				// PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			});
			matchConnectors.put(id, connector);
			connector.connect();
		});

		logger.info("[proxy->match],###### init Connector, Connectors:{},size:{} #######", matchConnectors, matchConnectors.size());
	}

	/**
	 * proxy <--> coin
	 */
	protected synchronized void initCoinConnector() {
		Map<Integer, CoinGameServerModel> coinServers = ServerDict.getInstance().getCoinGameServerModelDict();
		coinServers.forEach((id, coinServer) -> {
			NettySocketConnector connector = createConnector(coinServer.getInner_ip(), coinServer.getSocket_port());
			connector.setDescription(coinServer.getServer_name());
			connector.setConnectedCallback((cntor) -> {
				LoginReq.Builder builder = LoginReq.newBuilder();
				builder.setSafeCode("DFASE##@546654");
				builder.setServerType(EServerType.PROXY.type());
				builder.setServerIndex(SystemConfig.proxy_index);
				connector.send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			});
			coinConnectors.put(id, connector);
			connector.connect();
		});

		logger.info("[proxy->cooin],###### init Connector, Connectors:{},size:{} #######", coinConnectors, coinConnectors.size());
	}

	/**
	 * proxy <--> foundation
	 */
	protected synchronized void initFoundationConnector() {
		Map<Integer, FoundationGameServerModel> foundationServers = ServerDict.getInstance().getFoundationServerMap();
		foundationServers.forEach((id, coinServer) -> {
			NettySocketConnector connector = createConnector(coinServer.getInner_ip(), coinServer.getSocket_port());
			connector.setDescription(coinServer.getServer_name());
			connector.setConnectedCallback((cntor) -> {
				LoginReq.Builder builder = LoginReq.newBuilder();
				builder.setSafeCode("DFASE##@546654");
				builder.setServerType(EServerType.PROXY.type());
				builder.setServerIndex(SystemConfig.proxy_index);
				connector.send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			});
			this.foundationConnectors.put(id, connector);
			connector.connect();
		});

		logger.info("[proxy->foundation],###### init Connector, Connectors:{},size:{} #######", foundationServers, foundationServers.size());
	}

	public boolean sendToFoundation(int hostIndex, Request request) {
		Connector connector = this.foundationConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(request);
			return true;
		}
		return false;
	}

	public void sendToFoundation(Request request) {
		this.foundationConnectors.forEach((index, match) -> {
			if (null != match && match.isActive()) {
				match.send(request);
			}
		});
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

	public boolean sendMatch(int hostIndex, Request request) {
		Connector connector = matchConnectors.get(hostIndex);
		if (null != connector && connector.isActive()) {
			connector.send(request);
			return true;
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

	/**
	 * proxy<-->logic
	 */
	private synchronized void initLogicConnector() {
		Collection<LogicGameServerModel> logicHosts = ServerDict.getInstance().getLogicGameServerModelList();
		if (null != logicHosts && !logicHosts.isEmpty()) {
			for (final LogicGameServerModel hostNode : logicHosts) {
				// [维护中的逻辑服也连接，防止重连到新代理服，接受不到逻辑服的牌桌数据]
				if (hostNode.getOpen() != EServerStatus.ACTIVE.getStatus() && hostNode.getOpen() != EServerStatus.REPAIR.getStatus())
					continue;
				NettySocketConnector connector = createConnector(hostNode.getPublic_ip(), hostNode.getSocket_port());
				connector.setDescription(hostNode.getLogic_game_name());
				connector.setConnectedCallback((cntor) -> {
					LoginReq.Builder builder = LoginReq.newBuilder();
					builder.setSafeCode("DFASE##@546654");
					builder.setServerType(EServerType.PROXY.type());
					builder.setServerIndex(SystemConfig.proxy_index);
					connector.send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
				});
				connector.connect();
				logicConnectors.put(hostNode.getLogic_game_id(), connector);
			}
		}

		logger.info("[proxy->logic],###### init Connector, Connectors:{} #######", logicConnectors);
	}

	/**
	 * proxy <--> club
	 */
	protected synchronized void initClubConnector() {
		final UnresolvedAddress adress = new UnresolvedAddress(SystemConfig.club_socket_host);
		NettySocketConnector connector = createConnector(adress.getHost(), adress.getPort());
		connector.setDescription("俱乐部");
		connector.setConnectedCallback((cntor) -> {
			LoginReq.Builder builder = LoginReq.newBuilder();
			builder.setSafeCode("DFASE##@546654");
			builder.setServerType(EServerType.PROXY.type());
			builder.setServerIndex(SystemConfig.proxy_index);
			ClientServiceImpl.getInstance().sendClub(1, PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
			// ((NettySocketConnector)cntor).send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ,
			// builder).build());
		});
		clubConnectors.put(1, connector);
		connector.connect();

		logger.info("[proxy->club],###### init Connector, Connectors:{} #######", clubConnectors);
	}

	/**
	 * proxy <--> gate
	 */
	protected synchronized void initOrReloadGateConnector() {

		Map<Integer, GateServerModel> gateDict = ServerDict.getInstance().getGateServerDict();
		GateUtil.initOrReloadConnector(gateDict, gateConnectors, EServerType.PROXY, SystemConfig.proxy_index, new ProxyConnectorListener());

		logger.info("[proxy->gate],###### init or reload Connector, Connectors:{} #######", gateConnectors);
	}

	/**
	 * 重载连接[######注意，如果要移出某台逻辑服务器，仅能在db logic_game_server上设置open=0，不要直接删除记录]
	 */
	public synchronized void reloadConnector() {
		Collection<LogicGameServerModel> logicHosts = ServerDict.getInstance().getLogicGameServerModelList();
		for (final LogicGameServerModel hostNode : logicHosts) {
			NettySocketConnector connector = logicConnectors.get(hostNode.getLogic_game_id());
			if (null == connector) {
				// 有新增逻辑服，发起连接[维护中的逻辑服也连接，防止重连到新代理服，接受不到逻辑服的牌桌数据]
				if (hostNode.getOpen() == EServerStatus.ACTIVE.getStatus() || hostNode.getOpen() == EServerStatus.REPAIR.getStatus()) {
					connector = createConnector(hostNode.getPublic_ip(), hostNode.getSocket_port());
					connector.setDescription(hostNode.getLogic_game_name());
					connector.setConnectedCallback((cntor) -> {
						LoginReq.Builder builder = LoginReq.newBuilder();
						builder.setSafeCode("DFASE##@546654");
						builder.setServerType(EServerType.PROXY.type());
						builder.setServerIndex(SystemConfig.proxy_index);
						logicConnectors.get(hostNode.getLogic_game_id()).send(PBUtil.toS2SRequet(S2SCmd.S2S_LOGIN_REQ, builder).build());
					});
					connector.connect();
					logicConnectors.put(hostNode.getLogic_game_id(), connector);
					logger.info("[proxy->logic],###### add Connector:{} #######", connector);
				}
			} else {
				if (hostNode.getOpen() == EServerStatus.CLOSE.getStatus()) { // 需要下线逻辑服，移出连接
					logicConnectors.remove(hostNode.getLogic_game_id());
					connector.setStatus(EServerStatus.CLOSE);
					connector.setReConnect(false);
					connector.shutdownGracefully();
					logger.info("[proxy->logic],###### remove Connector:{} #######", connector);
				} else if (hostNode.getOpen() == EServerStatus.REPAIR.getStatus()) { // 维护状态
					connector.setStatus(EServerStatus.REPAIR);
					logger.info("[proxy->logic],###### repair Connector:{} #######", connector);
				} else if (hostNode.getOpen() == EServerStatus.ACTIVE.getStatus()) {
					connector.setStatus(EServerStatus.ACTIVE);
					logger.info("[proxy->logic],###### resume active Connector:{} #######", connector);
				} else if (hostNode.getOpen() == EServerStatus.CLOSEING.getStatus()) {
					connector.setStatus(EServerStatus.CLOSEING);
					connector.setReConnect(false);
					logger.info("[proxy->logic],###### closeing Connector:{} #######", connector);
				}
			}
		}

		logger.info("[proxy->logic],reloadConnector logicConnectors:{}", logicConnectors);
	}

	/**
	 * 
	 * @param host
	 * @param port
	 * @return
	 */
	private NettySocketConnector createConnector(String host, int port) {
		NettySocketConnector connector = new NettySocketConnector(host, port);
		connector.setListener(new ProxyConnectorListener());
		connector.doInit();
		connector.setReConnect(true);
		connector.doLogin();// 未来是否需要加入链接合法性校验？？
		return connector;
	}

	@Override
	protected void startService() {
		timer.schedule(new LogicHeartTimer(), 6000L, 5000L);// 发给逻辑计算服的心跳
		timer.schedule(new ClientSocketCheckTimer(), 10000L, 10000L);// 客户端链接检测
		timer.schedule(new DataStatTimer(), 60000L, 60000L);// 在线数据统计
		timer.schedule(new P2CRMIPingTimer(), RMIUtil.RMI_PING_DELAY, RMIUtil.RMI_PING_INTERVAL); // ping中心服
		timer.schedule(new GatePingTimer(), 6000L, 5000L);// 发给Gate服的心跳
		if (SystemConfig.connectClub == 1) {
			timer.schedule(new ClubPingTimer(), 6000L, 5000L);// 发给CLUB服的心跳
		}

		timer.schedule(new MatchPingTimer(), 6000L, 5000L);// 发给Gate服的心跳
		timer.schedule(new CoinPingTimer(), 6000L, 5000L);// 发给Gate服的心跳
		timer.schedule(new ServerStatusCheckTimer(), 6000L, 10000L);
		
		//处理跨天记录玩家在线时长问题
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 20);
		timer.schedule(new OnlineAccountCrossDayTimer(), calendar.getTime(), 24*60*60*1000L);

		logger.info("ClientServiceImpl  init finish!!!=");
	}

	/**
	 * 链接是否有用
	 * 
	 * @return
	 */
	public boolean isLinkActive(int index) {
		Connector connector = logicConnectors.get(index);
		return null != connector && connector.isActive();
	}

	/**
	 * 发给所有连接的逻辑服
	 * 
	 * @param request
	 * @return
	 */
	public boolean sendAllLogic(Request request) {
		Collection<NettySocketConnector> connectors = logicConnectors.values();
		for (final Connector c : connectors) {
			c.send(request);
		}
		return true;
	}

	/**
	 * 发送消息给逻辑服
	 * 
	 * @param request
	 */
	@Deprecated
	public boolean sendMsg(Request request) {
		NettySocketConnector connector = logicConnectors.get(1);
		connector.send(request);
		return true;
	}

	public NettySocketConnector getLogic(int serverIndex) {
		return logicConnectors.get(serverIndex);
	}

	/**
	 * 发送消息给逻辑服
	 * 
	 * @param hostIndex
	 * @param request
	 * @return
	 */
	public boolean sendMsg(int hostIndex, Request request) {

		if (hostIndex < 1) {
			logger.error("Proxy========>Logic hostIndex error, logicIndex:{},requestType:{}", hostIndex, request.getRequestType());
			hostIndex = 1;
		}

		if (!isLinkActive(hostIndex)) {
			logger.error("Proxy->Logic消息发送失败,与逻辑服的连接尚未建立! logicIndex:{}", hostIndex);
			return false;
		}
		Connector connector = logicConnectors.get(hostIndex);
		connector.send(request);
		return true;
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
		}
		return false;

	}

	/**
	 * 发消息到club服务器
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
	 * 发消息到club服务器
	 * 
	 * @param request
	 * @return
	 */
	public boolean sendClub(Request request) {
		return sendClub(1, request);
	}

	/**
	 * 随机出一台逻辑服
	 * 
	 * @return
	 */
	public int allotLogicIdFromCenter(int gameId) {
		int logicSvrId = -1;
		if (SystemConfig.gameDebug == 1) {
			if (!logicConnectors.isEmpty()) {
				Integer[] logicIndexs = logicConnectors.keySet().toArray(new Integer[0]);
				logicSvrId = logicIndexs[RandomUtil.getRandomNumber(logicIndexs.length)];
			}
		} else {
			logicSvrId = SpringService.getBean(ICenterRMIServer.class).allotLogicId(gameId);
		}
		if (-1 == logicSvrId) {
			logger.warn("####################没有可用的逻辑服，请确认!!##################");
		}
		if (SystemConfig.gameDebug == 1) {
			logger.info("申请了逻辑服id:{}", logicSvrId);
		}
		return logicSvrId;
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
	}

	@Override
	public void sessionCreate(C2SSession session) {
	}

	@Override
	public void sessionFree(C2SSession session) {
	}

	@Override
	public void dbUpdate(int _userID) {
	}
}
