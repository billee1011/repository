/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

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
import com.cai.common.util.ServerInfo;
import com.cai.common.util.ServerRegisterUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.redis.service.RedisService;
import javolution.util.FastMap;

/**
 * 服务器处理
 * 
 * @author wu_hc
 */
public class ServerDict {
	/**
	 */
	private static final Logger logger = LoggerFactory.getLogger(ServerDict.class);

	/**
	 * 逻辑服字典
	 */
	private LogicGameServerModel logicGameServerModel;

	/**
	 * 单例
	 */
	private final static ServerDict instance = new ServerDict();

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
	 * 私有构造
	 */
	private ServerDict() {
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
		logicGameServerModel = logics.get(SystemConfig.logic_index);
		if (null == logicGameServerModel) {
			logger.error("########### DB中没有配置id:[{}]的服务器！", SystemConfig.logic_index);
			return;
		}

		ServerInfo serverInfo = ServerInfo.of(logicGameServerModel.getSafe_code(), logicGameServerModel.getInner_ip(),
				logicGameServerModel.getLogic_game_id());

		if (SystemConfig.gameDebug == 0 && !ServerRegisterUtil.doVaildServerMsg(serverInfo, SystemConfig.logic_index)) {
			logger.error("###### 逻辑服[{}]注册失败.{} ######", logicGameServerModel, SystemConfig.logic_index);
			return;
		}
		DictHolder gateHolder = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_GATE, DictHolder.class);
		if (null != gateHolder) {
			gateServerModelDict = gateHolder.getDicts();
		}
		// 测试环境下不到中心服注册 @GAME-TODO ###############################
		registerToCenter();

		matchMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_MATCH, HashMap.class);
		coinServerMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_COIN, HashMap.class);
		this.foundationServerMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR__SERVER_FOUNDATION, HashMap.class);
	}

	/**
	 * 注册到中心服
	 */
	private void registerToCenter() {
		ServerRegisterUtil.registerToCenter(EServerType.LOGIC, EServerStatus.getStatus(logicGameServerModel.getOpen()),
				logicGameServerModel.getLogic_game_id());
	}

	public LogicGameServerModel getLogicGameServerModel() {
		return logicGameServerModel;
	}
	
	public Map<Integer, MatchGameServerModel> getMatchServerDict(){
		if (null == matchMap) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(matchMap);
	}
	
	public Map<Integer, CoinGameServerModel> getCoinGameServerModelDict() {
		if (null == coinServerMap) {
			return Collections.emptyMap();
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
	 * 
	 * @return
	 */
	public Map<Integer, GateServerModel> getGateServerDict() {
		if (null == gateServerModelDict) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(gateServerModelDict);
	}
}
