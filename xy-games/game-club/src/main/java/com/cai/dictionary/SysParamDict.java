package com.cai.dictionary;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGameType;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.ClubRangeCostUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.base.Strings;

import javolution.util.FastMap;

/**
 * 系统参数字典
 * 
 * @author run
 *
 */
public class SysParamDict {

	private Logger logger = LoggerFactory.getLogger(SysParamDict.class);

	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary;

	/**
	 * 带有围观特性的游戏id，对应系统参数中7002，6
	 */
	private volatile Set<Integer> observerGameTypeIndexs = new HashSet<>();

	/**
	 * 围观
	 */
	private static final int OBSERVER_ID = 7002;

	/**
	 * 单例
	 */
	private static SysParamDict instance = new SysParamDict();

	/**
	 * 私有构造
	 */
	private SysParamDict() {
		sysParamModelDictionary = new FastMap<Integer, FastMap<Integer, SysParamModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SysParamDict getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysParamModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM, FastMap.class);

			this.loadObserverGameTypeIndex();
			
			// 俱乐部人数区间扣豆
			SysParamModel clubRangeCostModel = sysParamModelDictionary.get(EGameType.DT.getId()).get(7004);
			if (null != clubRangeCostModel) {
				ClubRangeCostUtil.INSTANCE.setActive(clubRangeCostModel.getVal1().intValue() == 1);
				if (ClubRangeCostUtil.INSTANCE.isActive()) {
					this.parseClubRangeCost();
				}
			}
			
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载加载字典sys_param" + timer.getStr());
	}

	public FastMap<Integer, SysParamModel> getSysParamModelDictionaryByGameId(int game_id) {
		FastMap<Integer, SysParamModel> dict = sysParamModelDictionary.get(game_id);
		return null == dict ? new FastMap<>() : dict;
	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return sysParamModelDictionary;
	}

	public void setSysParamModelDictionary(FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary) {
		this.sysParamModelDictionary = sysParamModelDictionary;
	}

	/**
	 * 需要围观的游戏id
	 */
	private void loadObserverGameTypeIndex() {

		Set<Integer> observerSet = new HashSet<>();

		for (EGameType eType : EGameType.values()) {
			FastMap<Integer, SysParamModel> dict = sysParamModelDictionary.get(eType.getId());
			if (null != dict) {
				SysParamModel sysParamModel7002 = dict.get(OBSERVER_ID);

				if (null == sysParamModel7002) {
					continue;
				}

				String str1 = sysParamModel7002.getStr1();
				if (!Strings.isNullOrEmpty(str1)) {
					String[] gameTypeIndexArray = str1.split(Symbol.COMMA);
					for (final String gameTypeIndex : gameTypeIndexArray) {
						observerSet.add(Integer.parseInt(gameTypeIndex));
					}
				}

				String str2 = sysParamModel7002.getStr2();
				if (!Strings.isNullOrEmpty(str2)) {
					String[] gameTypeIndexArray = str2.split(Symbol.COMMA);
					for (final String gameTypeIndex : gameTypeIndexArray) {
						observerSet.add(Integer.parseInt(gameTypeIndex));
					}
				}
			}
		}
		observerGameTypeIndexs = observerSet;

		logger.info("围观特性子游戏Id:{}", observerSet);
	}

	/**
	 * 
	 * @param gameTypeIndex
	 * @return
	 */
	public boolean isObserverGameTypeIndex(int gameTypeIndex) {
		return observerGameTypeIndexs.contains(gameTypeIndex);
	}

	/**
	 * 俱乐部人数区间扣豆
	 */
	private void parseClubRangeCost() {
		try {
			Map<Integer, SysGameType> sysGameTypeMap = SysGameTypeDict.getInstance().getSysGameTypeDictionary();
			sysGameTypeMap.forEach((subAppId, model) -> {

				for (int cfgId : model.getGoldIndex()) {
					FastMap<Integer, SysParamModel> map = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(model.getGameID());
					if (null == map) {
						continue;
					}

					SysParamModel clubRangeCostModel = map.get(cfgId);
					if (null != clubRangeCostModel) {
						ClubRangeCostUtil.INSTANCE.load(model.getGame_type_index(), clubRangeCostModel.getVal1(), clubRangeCostModel.getStr2());
					}
				}
			});
			ClubRangeCostUtil.INSTANCE.debugInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}