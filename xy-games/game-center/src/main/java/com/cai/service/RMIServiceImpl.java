package com.cai.service;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.domain.Event;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.common.rmi.IRMIServer;
import com.cai.common.util.RMIUtil;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.ServerDict;
import com.cai.domain.Session;
import com.google.common.collect.Maps;

/**
 * 
 * 中心服->逻辑服&代理服的RMI管理中心
 *
 */
public class RMIServiceImpl extends AbstractService {

	/**
	 * 
	 */
	private static Logger logger = LoggerFactory.getLogger(RMIServiceImpl.class);

	/**
	 * 
	 */
	private static RMIServiceImpl instance = new RMIServiceImpl();

	/**
	 * 所有在线代理服的rmi
	 */
	private final Map<Integer, IProxyRMIServer> onlineProxyRMIs = Maps.newConcurrentMap();

	/**
	 * 所有在线逻辑服的rmi
	 */
	private final Map<Integer, ILogicRMIServer> onlineLogicRMIs = Maps.newConcurrentMap();

	/**
	 * RMI接口 //
	 */
	// private final Map<EServerType, Map<Integer, IRMIServer>> rmis =
	// Maps.newConcurrentMap();

	/**
	 * 服务状态
	 * 
	 * @param serverType
	 * @param status
	 * @param serverIndex
	 */
	public void serverStatusUpdate(EServerType serverType, EServerStatus status, int serverIndex) {
		if (EServerType.LOGIC == serverType) {
			logicServerStatusUpdate(status, serverIndex);
		} else if (EServerType.PROXY == serverType) {
			proxyServerStatusUpdate(status, serverIndex);
		} else if (EServerType.CLUB == serverType) {
			clubServerStatusUpdate(status, serverIndex);
		} else if (EServerType.MATCH == serverType) {
			logger.info("[center<->match],###### RMI:{} #######", status.name());
			// clubServerStatusUpdate(status, serverIndex);
		} else if (EServerType.FOUNDATION == serverType) {
			logger.info("[center<->foundation],###### RMI:{} #######", status.name());
		} else {
			logger.error("该服务器类型不在状态更新支持列表中! {}", serverType.name());
		}
	}

	/**
	 * 逻辑服状态
	 * 
	 * @param status
	 * @param serverIndex
	 */
	private void logicServerStatusUpdate(EServerStatus status, int serverIndex) {

		LogicGameServerModel serverModel = ServerDict.getInstance().getLogicGameServerModelDict().get(serverIndex);
		if (null == serverModel) {
			logger.error("######### 找不到逻辑服，index:{} ##########", serverIndex);
			return;
		}

		ILogicRMIServer rmiServer = onlineLogicRMIs.get(serverIndex);
		if (null == rmiServer) {
			if (EServerStatus.ACTIVE == status || EServerStatus.REPAIR == status) { // 需要新建rmi连接
				rmiServer = RMIUtil.createLogicRMIBean(serverModel);
				try {
					rmiServer.test();
					serverModel.setStatus(status);
				} catch (Exception e) {
					e.printStackTrace();
					serverModel.setStatus(EServerStatus.CLOSE);
				}
				onlineLogicRMIs.put(serverModel.getLogic_game_id(), rmiServer);

				logger.info("[center<->logic],###### online RMI:{} ,status:{} #######", rmiServer, status);
			}

		} else {
			if (EServerStatus.ACTIVE == status) {
				if (EServerStatus.CLOSE == serverModel.getStatus()) {
					logger.info("[center<->logic],###### online RMI:{} #######", rmiServer);
				}
				serverModel.setStatus(EServerStatus.ACTIVE);
			} else if (status == EServerStatus.CLOSE) { // 需要下线rmi连接
				serverModel.setStatus(EServerStatus.CLOSE);
				onlineLogicRMIs.remove(serverIndex);
				logger.info("[center<->logic],###### offline RMI:{} #######", rmiServer);

			} else if (status == EServerStatus.REPAIR) { // 等待关闭的rmi连接
				serverModel.setStatus(EServerStatus.REPAIR);
				logger.info("[center<->logic],###### repair RMI:{} #######", rmiServer);
			} else if (status == EServerStatus.CLOSEING) { // 等待关闭的rmi连接
				serverModel.setStatus(EServerStatus.CLOSEING);
				logger.info("[center<->logic],###### closeing RMI:{} #######", rmiServer);
			}
		}
	}

	/**
	 * 代理服状态
	 * 
	 * @param status
	 * @param serverIndex
	 */
	private void proxyServerStatusUpdate(EServerStatus status, int serverIndex) {

		ProxyGameServerModel serverModel = ServerDict.getInstance().getProxyGameServerModelDict().get(serverIndex);
		if (null == serverModel) {
			logger.error("######### 找不到代理服，index:{} #########", serverIndex);
			return;
		}

		IProxyRMIServer rmiServer = onlineProxyRMIs.get(serverIndex);
		if (null == rmiServer) {
			if (EServerStatus.ACTIVE == status) { // 需要新建rmi连接

				rmiServer = RMIUtil.createProxyRMIBean(serverModel);
				try {
					rmiServer.test();
					serverModel.setStatus(EServerStatus.ACTIVE);
				} catch (Exception e) {
					e.printStackTrace();
					serverModel.setStatus(EServerStatus.CLOSE);
				}
				onlineProxyRMIs.put(serverModel.getProxy_game_id(), rmiServer);

				logger.info("[center<->proxy],###### online RMI:{} #######", rmiServer);
			} else {
				logger.info("不需要处理的状态！");
			}

		} else {
			if (EServerStatus.ACTIVE == status) {
				serverModel.setStatus(EServerStatus.ACTIVE);
			} else if (status == EServerStatus.CLOSE) { // 需要下线rmi连接
				serverModel.setStatus(EServerStatus.CLOSE);
				onlineProxyRMIs.remove(serverIndex);
				logger.info("[center<->proxy],###### offline RMI:{} #######", rmiServer);

			} else if (status == EServerStatus.REPAIR) { // 等待关闭的rmi连接
				serverModel.setStatus(EServerStatus.REPAIR);
				logger.info("[center<->proxy],###### repair RMI:{} #######", rmiServer);
			} else if (status == EServerStatus.CLOSEING) { // 正在关闭的rmi连接
				serverModel.setStatus(EServerStatus.REPAIR);
				logger.info("[center<->proxy],###### closeing RMI:{} #######", rmiServer);
			}
		}
	}

	/**
	 * 
	 * @param status
	 * @param serverIndex
	 */
	private void clubServerStatusUpdate(EServerStatus status, int serverIndex) {
		// GAME-TODO club服务器注册待处理
		logger.info("[center<->club],###### RMI:{} #######", status.name());
	}

	/**
	 * @param serverType
	 * @param serverIndex
	 */
	public void serverPing(EServerType serverType, int serverIndex) {
		if (EServerType.LOGIC == serverType) {
			logicServerPing(serverIndex);
		} else if (EServerType.PROXY == serverType) {
			proxyServerPing(serverIndex);
		} else if (EServerType.CLUB == serverType) {
			clubServerPing(serverIndex);
		} else if (EServerType.MATCH == serverType) {
			// matchServerPing(serverIndex);
		} else {
			logger.error("该服务器不在PING支持列表中,serverType:{}, serverIndex:{}!", serverType.name(), serverIndex);
		}
	}

	/**
	 * 
	 * @param serverIndex
	 */
	private void proxyServerPing(int serverIndex) {
		ProxyGameServerModel serverModel = ServerDict.getInstance().getProxyGameServerModelDict().get(serverIndex);
		if (null != serverModel) {
			serverModel.setLastPingTime(System.currentTimeMillis());
		}
	}

	/**
	 * 
	 * @param serverIndex
	 */
	private void logicServerPing(int serverIndex) {
		LogicGameServerModel serverModel = ServerDict.getInstance().getLogicGameServerModelDict().get(serverIndex);
		if (null != serverModel) {
			serverModel.setLastPingTime(System.currentTimeMillis());
		}
	}

	private void clubServerPing(int serverIndex) {
	}

	private RMIServiceImpl() {
	}

	public static RMIServiceImpl getInstance() {
		return instance;
	}

	@Override
	protected void startService() {

		// for (EServerType type : EServerType.values()) {
		// rmis.put(type, Maps.newConcurrentMap());
		// }
	}

	public Map<Integer, IProxyRMIServer> getProxyRMIServerMap() {
		return Collections.unmodifiableMap(onlineProxyRMIs);
	}

	public Map<Integer, ILogicRMIServer> getLogicRMIServerMap() {
		return Collections.unmodifiableMap(onlineLogicRMIs);
	}

	public IProxyRMIServer getIProxyRMIByIndex(int serverIndex) {
		return onlineProxyRMIs.get(serverIndex);
	}

	public ILogicRMIServer getLogicRMIByIndex(int serverIndex) {
		return onlineLogicRMIs.get(serverIndex);
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

}
