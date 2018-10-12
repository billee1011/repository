package com.cai.dictionary;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GoldGameOpenRunnable;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();

		PublicService publicService = SpringService.getBean(PublicService.class);
		List<SysParamModel> sysParamModelList = publicService.getPublicDAO().getSysParamModelList();

		sysParamModelDictionary.clear();
		for (SysParamModel model : sysParamModelList) {
			if (!sysParamModelDictionary.containsKey(model.getGame_id())) {
				FastMap<Integer, SysParamModel> map = new FastMap<Integer, SysParamModel>();
				sysParamModelDictionary.put(model.getGame_id(), map);
			}

			if (SysGameTypeDict.getInstance().isGoldGameType(model.getId())) {// 是收费索引的参数
				if (model.getVal1() == 1 && model.getVal2() == 0 && model.getFinish_time() != null) {// 当前已经开放了，并且是免费的，而且是有指定时间收费的
					if (timer.getBegin() < model.getFinish_time().getTime()) {// 当前时间比指定时间小
						long leftTime = (model.getFinish_time().getTime() - timer.getBegin()) / 1000 + 10;// 10s之后检测收费
						GameSchedule.put(new GoldGameOpenRunnable(model.getGame_id(), model.getId(), model.getFinish_time().getTime()), leftTime,
								TimeUnit.SECONDS);
						logger.info("收到加载收费指令" + model.getGame_id() + "id=" + model.getId());
					}
				}
			}

			sysParamModelDictionary.get(model.getGame_id()).put(model.getId(), model);
		}

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM, sysParamModelDictionary);

		logger.info("加载字典SysParamDict,count=" + sysParamModelList.size() + timer.getStr());

		try {
			// 俱乐部人数区间扣豆
			SysParamModel clubRangeCostModel = sysParamModelDictionary.get(EGameType.DT.getId()).get(7004);
			if (null != clubRangeCostModel) {
				ClubRangeCostUtil.INSTANCE.setActive(clubRangeCostModel.getVal1().intValue() == 1);
				if (ClubRangeCostUtil.INSTANCE.isActive()) {
					this.parseClubRangeCost();
				}
			}


			parseExclusiveStatus();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return sysParamModelDictionary;
	}

	public void setSysParamModelDictionary(FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary) {
		this.sysParamModelDictionary = sysParamModelDictionary;
	}

	public FastMap<Integer, SysParamModel> getSysParamModelDictionaryByGameId(int game_id) {
		return sysParamModelDictionary.get(game_id);
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
