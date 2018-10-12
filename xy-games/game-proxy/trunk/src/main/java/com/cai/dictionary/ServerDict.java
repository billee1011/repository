/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.dictionary.DictHolder;
import com.cai.common.domain.CoinGameServerModel;
import com.cai.common.domain.FoundationGameServerModel;
import com.cai.common.domain.GateServerModel;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.MatchGameServerModel;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.util.ServerRegisterUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Maps;

import javolution.util.FastMap;

/**
 * 服务器处理
 * 
 * @author wu_hc
 */
public class ServerDict {
	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(ServerDict.class);
	/**
	 * 逻辑服字典
	 */
	private volatile FastMap<Integer, LogicGameServerModel> logicGameServerModelDict;

	/**
	 * 代理服字典
	 */
	private volatile ProxyGameServerModel proxyGameServerModel;

	/**
	 * 网关服
	 */
	private volatile Map<Integer, GateServerModel> gateServerModelDict;
	
	/**
	 * 比赛服
	 */
	private volatile Map<Integer, MatchGameServerModel> matchMap;
	/**
	 * 金币场
	 */
	private volatile Map<Integer, CoinGameServerModel> coinServerMap;
	
	//游戏基础服
	private volatile Map<Integer, FoundationGameServerModel> foundationServerMap;

	/**
	 * 单例
	 */
	private final static ServerDict instance = new ServerDict();

	/**
	 * 私有构造
	 */
	private ServerDict() {
		logicGameServerModelDict = new FastMap<Integer, LogicGameServerModel>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ServerDict getInstance() {
		return instance;
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void load() {

		RedisService redisService = SpringService.getBean(RedisService.class);

		FastMap<Integer, LogicGameServerModel> logics = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_LOGIC, FastMap.class);

		// 测试============================
		if (SystemConfig.gameDebug == 1) {
			logicGameServerModelDict.clear();
			localDubug(logics);
		} else {
			logicGameServerModelDict.clear();
			logicGameServerModelDict = logics;
		}

		FastMap<Integer, ProxyGameServerModel> proxys = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_PROXY, FastMap.class);
		proxyGameServerModel = proxys.get(SystemConfig.proxy_index);
		if (null == proxyGameServerModel) {
			logger.error("########### DB中没有配置id:[{}]的服务器！", SystemConfig.proxy_index);
			return;
		}

		DictHolder gateHolder = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_GATE, DictHolder.class);
		if (null != gateHolder) {
			gateServerModelDict = gateHolder.getDicts();
		}


		// 注册服务
		ServerRegisterUtil.registerToCenter(EServerType.PROXY, EServerStatus.getStatus(proxyGameServerModel.getOpen()),
				proxyGameServerModel.getProxy_game_id());
		
		matchMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_MATCH, HashMap.class);
		coinServerMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_COIN, HashMap.class);
		this.foundationServerMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR__SERVER_FOUNDATION, HashMap.class);
	}

	/**
	 * @return the logicGameServerModelDict
	 */
	public Map<Integer, LogicGameServerModel> getLogicGameServerModelDict() {
		if (null == logicGameServerModelDict) {
			return Maps.newHashMap();
		}
		return Collections.unmodifiableMap(logicGameServerModelDict);
	}
	
	public Map<Integer, CoinGameServerModel> getCoinGameServerModelDict() {
		if (null == coinServerMap) {
			return Maps.newHashMap();
		}
		return Collections.unmodifiableMap(coinServerMap);
	}
	
	public Map<Integer, FoundationGameServerModel> getFoundationServerMap() {
		if (null == foundationServerMap) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(foundationServerMap);
	}

	/**
	 * @param logicGameServerModelDict
	 *            the logicGameServerModelDict to set
	 */
	public void setLogicGameServerModelDict(FastMap<Integer, LogicGameServerModel> logicGameServerModelDict) {
		this.logicGameServerModelDict = logicGameServerModelDict;
	}

	/**
	 * 
	 * @return
	 */
	public Collection<LogicGameServerModel> getLogicGameServerModelList() {
		if (null == logicGameServerModelDict) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableCollection(logicGameServerModelDict.values());
	}
	
	public Map<Integer, MatchGameServerModel> getMatchServerDict(){
		if (null == matchMap) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(matchMap);
	}

	/**
	 * 
	 * @return
	 */
	public Map<Integer, GateServerModel> getGateServerDict() {
		if (null == gateServerModelDict) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(gateServerModelDict);
	}

	/**
	 * 本地测试
	 * 
	 * @param logics2
	 */
	private void localDubug(FastMap<Integer, LogicGameServerModel> logics) {
		LogicGameServerModel serverModel = logics.get(SystemConfig.proxy_index);
		if (null == serverModel) {
			logger.error("########## 在logic_game_server中加入自己的服务器配置信息! ###############");
			return;
		}
		logicGameServerModelDict.put(serverModel.getLogic_game_id(), serverModel);
	}

	public ProxyGameServerModel getProxyGameServerModel() {
		return proxyGameServerModel;
	}

	public void setProxyGameServerModel(ProxyGameServerModel proxyGameServerModel) {
		this.proxyGameServerModel = proxyGameServerModel;
	}
	
}
