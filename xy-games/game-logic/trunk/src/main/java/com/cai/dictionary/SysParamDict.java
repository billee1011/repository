package com.cai.dictionary;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.ClubRangeCostUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

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
	 * 单例
	 */
	private static SysParamDict instance;

	private volatile boolean isOpenLog = true;

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
		if (null == instance) {
			instance = new SysParamDict();
		}

		return instance;
	}

	public boolean getIsOpenLog() {
		return isOpenLog;
	}

	private void is_open_log() {
		if (sysParamModelDictionary != null) {
			FastMap<Integer, SysParamModel> paramMap = getSysParamModelDictionaryByGameId(1);
			if (paramMap != null) {
				SysParamModel model = paramMap.get(1000);
				if (model != null) {
					isOpenLog = model.getVal4() == 1 ? true : false;
				}
			}
		}
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysParamModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM, FastMap.class);

			// 俱乐部人数区间扣豆
			SysParamModel clubRangeCostModel = sysParamModelDictionary.get(EGameType.DT.getId()).get(7004);
			if (null != clubRangeCostModel) {
				ClubRangeCostUtil.INSTANCE.setActive(clubRangeCostModel.getVal1().intValue() == 1);
				ClubRangeCostUtil.INSTANCE.setIncludeVoice(clubRangeCostModel.getVal2().intValue() == 1);
				if (ClubRangeCostUtil.INSTANCE.isActive()) {
					this.parseClubRangeCost();
				}
			}

			parseExclusiveStatus();
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载加载字典sys_param" + timer.getStr());
	}

	public FastMap<Integer, SysParamModel> getSysParamModelDictionaryByGameId(int game_id) {
		FastMap<Integer, SysParamModel> dict = sysParamModelDictionary.get(game_id);
		return dict == null ? new FastMap<Integer, SysParamModel>() : dict;
	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return sysParamModelDictionary;
	}

	public void setSysParamModelDictionary(FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary) {
		this.sysParamModelDictionary = sysParamModelDictionary;
	}

	/**
	 * 俱乐部人数区间扣豆
	 */
	private void parseClubRangeCost() {
		try {
			ConcurrentHashMap<Integer, SysGameType> sysGameTypeMap = SysGameTypeDict.getInstance().getSysGameTypeDictionary();
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

	/**
	 * 解析专属豆使用设置
	 */
	private void parseExclusiveStatus() {
		for (EGameType type : EGameType.values()) {
			FastMap<Integer, SysParamModel> map = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(type.getId());
			if (null == map)
				return;
			SysParamModel paramModel = map.get(7005);
			if (null != paramModel) {
				ExclusiveGoldCfg.get().loadExclusiveGameStatus(type.getId(), paramModel.getVal1() == 1, paramModel.getStr1(), paramModel.getStr2());
			}
		}
	}
}
