package com.cai.dictionary;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 系统参数字典
 * 
 * @author run
 *
 */
public class SysParamServerDict {

	private Logger logger = LoggerFactory.getLogger(SysParamServerDict.class);

	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary;

	/**
	 * 单例
	 */
	private static SysParamServerDict instance;

	/**
	 * 私有构造
	 */
	private SysParamServerDict() {
		sysParamModelDictionary = new FastMap<Integer, FastMap<Integer, SysParamModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SysParamServerDict getInstance() {
		if (null == instance) {
			instance = new SysParamServerDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		sysParamModelDictionary.clear();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<SysParamModel> sysParamModelList = publicService.getPublicDAO().getSysParamServerModelList();
		for (SysParamModel model : sysParamModelList) {
			if (!sysParamModelDictionary.containsKey(model.getGame_id())) {
				FastMap<Integer, SysParamModel> map = new FastMap<Integer, SysParamModel>();
				sysParamModelDictionary.put(model.getGame_id(), map);
			}
			sysParamModelDictionary.get(model.getGame_id()).put(model.getId(), model);
		}

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM_SERVER, sysParamModelDictionary);

		logger.info("加载字典SysParamServerDict,count=" + sysParamModelList.size() + timer.getStr());

		// loadGameId2LogicSvrIdMap();

		parseExclusiveCfg();
	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return sysParamModelDictionary;
	}

	public void setSysParamModelDictionary(FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary) {
		this.sysParamModelDictionary = sysParamModelDictionary;
	}

	public FastMap<Integer, SysParamModel> getSysParamModelDictionaryByGameId(int game_id) {
		FastMap<Integer, SysParamModel> dict = sysParamModelDictionary.get(game_id);
		return dict == null ? new FastMap<>() : dict;
	}

	/**
	 * 
	 * @param gameTypeIndex
	 * @return
	 */
	public boolean isObserverGameTypeIndex(int gameTypeIndex) {
		return false;
	}

	/**
	 * 解析俱乐部配置
	 */
	private void parseExclusiveCfg() {

		FastMap<Integer, SysParamModel> params = sysParamModelDictionary.get(EGameType.DT.getId());
		if (null == params) {
			logger.error("找不到gameId[{}]相关配置!", EGameType.DT.getId());
			return;
		}

		SysParamModel paramModel = params.get(2236);
		if (null == paramModel) {
			logger.error("############### 找不到id[2236]相关配置! ###################");
			return;
		}
		ExclusiveGoldCfg.get().setRobotCreateRoomCostExclusiveGold(paramModel.getVal1() == 1);
		ExclusiveGoldCfg.get().setTransferActive(paramModel.getVal2() == 1);
		ExclusiveGoldCfg.get().setUseExclusiveGold(paramModel.getVal3() == 1);
		logger.info("ExclusiveGoldCfg cfg:{}", ExclusiveGoldCfg.get());
	}
	
	/**
	 * 替换提示语中的闲逸豆为豆(玩一局需求)
	 * @param msg
	 * @return
	 */
	public String replaceGoldTipsWord(String msg) {
		FastMap<Integer, SysParamModel> params = sysParamModelDictionary.get(EGameType.DT.getId());
		if (null == params) {
			return msg;
		}
		SysParamModel paramModel = params.get(2400);
		if (null == paramModel) {
			return msg;
		}
		if (paramModel.getVal1() == 0) {
			return msg;
		}
		if (StringUtils.isEmpty(paramModel.getStr1()) || StringUtils.isEmpty(paramModel.getStr2())) {
			return msg;
		}
		return msg.replace(paramModel.getStr1(), paramModel.getStr2());
	}
}
