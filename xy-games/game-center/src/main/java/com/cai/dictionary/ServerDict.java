package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EServerStatus;
import com.cai.common.dictionary.DictHolder;
import com.cai.common.domain.CoinGameServerModel;
import com.cai.common.domain.FoundationGameServerModel;
import com.cai.common.domain.GateServerModel;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.MatchGameServerModel;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javolution.util.FastMap;

/**
 * 服务器字典
 * 
 * @author run
 *
 */
public class ServerDict {

	private static final Logger logger = LoggerFactory.getLogger(ServerDict.class);

	/**
	 * 代理服字典
	 */
	private FastMap<Integer, ProxyGameServerModel> proxyGameServerModelDict;

	/**
	 * 逻辑服字典
	 */
	private volatile FastMap<Integer, LogicGameServerModel> logicGameServerModelDict;

	/**
	 * 可以用于分配的逻辑服
	 */
	private final Map<Integer, LogicGameServerModel> canAllotLogicServers = Maps.newConcurrentMap();

	/**
	 * 单例
	 */
	private static ServerDict instance;

	/**
	 * 私有构造
	 */
	private ServerDict() {
		proxyGameServerModelDict = new FastMap<Integer, ProxyGameServerModel>();
		logicGameServerModelDict = new FastMap<Integer, LogicGameServerModel>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ServerDict getInstance() {
		if (null == instance) {
			instance = new ServerDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();

		PublicService publicService = SpringService.getBean(PublicService.class);
		RedisService redisService = SpringService.getBean(RedisService.class);

		// 1代理服处理
		FastMap<Integer, ProxyGameServerModel> proxyMap = new FastMap<>();
		List<ProxyGameServerModel> proxyGameServerModelList = publicService.getPublicDAO().getProxyGameServerModelList();
		for (ProxyGameServerModel model : proxyGameServerModelList) {
			proxyMap.put(model.getProxy_game_id(), model);
		}
		proxyGameServerModelDict = proxyMap;

		// 2逻辑服处理
		FastMap<Integer, LogicGameServerModel> logicMap = new FastMap<>();
		List<LogicGameServerModel> logicGameServerModelList = publicService.getPublicDAO().getLogicGameServerModelList();
		for (LogicGameServerModel model : logicGameServerModelList) {
			logicMap.put(model.getLogic_game_id(), model);
		}
		logicGameServerModelDict = logicMap;

		// 放入redis缓存
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_SERVER_LOGIC, logicGameServerModelDict);
		// 放入redis缓存
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_SERVER_PROXY, proxyGameServerModelDict);

		logger.info("加载字典ServerDict,count=" + proxyGameServerModelList.size() + "," + logicGameServerModelList.size() + timer.getStr());
		

		// 2逻辑服处理
		Map<Integer, MatchGameServerModel> matchMap = new HashMap<>();
		
		List<MatchGameServerModel> matchModelList = publicService.getPublicDAO().getMatchServerModelList();
		for (MatchGameServerModel model : matchModelList) {
			matchMap.put(model.getServer_id(), model);
		}
		
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_SERVER_MATCH, matchMap);
		
		Map<Integer, CoinGameServerModel> coinMap = new HashMap<>();
		
		List<CoinGameServerModel> coinModelList = publicService.getPublicDAO().getCoinServerModelList();
		for (CoinGameServerModel model : coinModelList) {
			coinMap.put(model.getServer_id(), model);
		}
		
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_SERVER_COIN, coinMap);
		
		//加载游戏基础服
		Map<Integer, FoundationGameServerModel> foundationServerMap = new HashMap<>();
		
		List<FoundationGameServerModel> foundationServerList = publicService.getPublicDAO().getFoundationServerModelList();
		for (FoundationGameServerModel model : foundationServerList) {
			foundationServerMap.put(model.getServer_id(), model);
		}
		
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR__SERVER_FOUNDATION, foundationServerMap);
		
		loadGate();

	}

	/**
	 * 网关服加载
	 */
	public void loadGate() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		RedisService redisService = SpringService.getBean(RedisService.class);
		List<GateServerModel> gateServerModelList = publicService.getPublicDAO().getGateServerModelList();
		if (null != gateServerModelList && !gateServerModelList.isEmpty()) {
			Map<Integer, GateServerModel> gateServerModelMap = gateServerModelList.stream()
					.collect(Collectors.toMap(GateServerModel::getServer_id, GateServerModel -> GateServerModel));
			// 放入redis缓存
			redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_SERVER_GATE,
					DictHolder.newHolder(RedisConstant.DIR_SERVER_GATE, gateServerModelMap));
		}
	}

	public FastMap<Integer, ProxyGameServerModel> getProxyGameServerModelDict() {
		return proxyGameServerModelDict;
	}

	public FastMap<Integer, LogicGameServerModel> getLogicGameServerModelDict() {
		return logicGameServerModelDict;
	}

	/**
	 * 
	 * 逻辑服 --> map
	 * 
	 * @return
	 */
	public Map<Integer, LogicGameServerModel> getActiveLogicDict() {

		Map<Integer, LogicGameServerModel> logicMap = Maps.newHashMap(logicGameServerModelDict);

		Map<Integer, LogicGameServerModel> ret = Maps.newHashMap();
		for (final LogicGameServerModel serverModel : logicMap.values()) {
			if (serverModel.getOpen() == EServerStatus.ACTIVE.getStatus() && EServerStatus.ACTIVE == serverModel.getStatus()) {
				ret.put(serverModel.getLogic_game_id(), serverModel);
			}
		}
		return ret;
	}

	/**
	 * 
	 * 代理服 -> map
	 * 
	 * @return
	 */
	public Map<Integer, ProxyGameServerModel> getActiveProxyDict() {
		Map<Integer, ProxyGameServerModel> proxyMap = Maps.newHashMap(proxyGameServerModelDict);

		Map<Integer, ProxyGameServerModel> availableMap = Maps.newHashMap();
		for (final ProxyGameServerModel serverModel : proxyMap.values()) {
			if (serverModel.getOpen() == EServerStatus.ACTIVE.getStatus() && EServerStatus.ACTIVE == serverModel.getStatus()) {
				availableMap.put(serverModel.getProxy_game_id(), serverModel);
			}
		}
		return availableMap;
	}

	/**
	 * @return
	 */
	public List<LogicGameServerModel> getActiveLogicList() {
		return Lists.newArrayList(getActiveLogicDict().values());
	}

	/**
	 * GAME-TODO 待优化 增加可以用于分配的逻辑服务器
	 * 
	 * @param serverIndex
	 */
	public void addActiveLogicServer(int serverIndex) {
		final LogicGameServerModel model = logicGameServerModelDict.get(serverIndex);
		if (null != model && (model.getOpen() == EServerStatus.ACTIVE.getStatus() && EServerStatus.ACTIVE == model.getStatus())) {
			canAllotLogicServers.put(model.getLogic_game_id(), model);
		}
	}

	/**
	 * GAME-TODO 待优化 移出逻辑服，不可以用于分配
	 * 
	 * @param serverIndex
	 */
	public void removeActiveLogicServer(int serverIndex) {
		final LogicGameServerModel model = logicGameServerModelDict.get(serverIndex);
		if (null != model && (model.getOpen() != EServerStatus.ACTIVE.getStatus() && EServerStatus.ACTIVE != model.getStatus())) {
			canAllotLogicServers.remove(serverIndex);
		}
	}
}
