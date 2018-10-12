package com.cai.dictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGameType;
import com.cai.common.define.EPlatform;
import com.cai.common.domain.SysParamModel;
import com.cai.common.type.PropertyRspType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PlatformTag;
import com.cai.common.util.SpringService;
import com.cai.handler.PropertyHandler;
import com.cai.module.LoginModule;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.util.MessageResponse;
import com.google.common.base.Strings;

import javolution.util.FastMap;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Response;

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
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysParamModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM, FastMap.class);

			this.loadObserverGameTypeIndex();
			this.platform();
			this.sendNotifyTOClient();
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载加载字典sys_param" + timer.getStr());
	}

	private void platform() {
		SysParamModel platModel = getSysParamModelDictionaryByGameId(0).get(0);
		if (null != platModel) {
			EPlatform ePlt = EPlatform.of(platModel.getVal2());
			if (null != ePlt) {
				PlatformTag.ePlt = ePlt;
			}
		}
	}

	/**
	 * 
	 */
	private void sendNotifyTOClient() {
		List<AccountPropertyResponse> outList = MessageResponse.getSysAccountPropertyResponseList(PlatformTag.ePlt.getGameId(), true);
		if (null != outList && !outList.isEmpty()) {
			Response.Builder rspBuilder = PropertyHandler.toPropertyBuilder(outList, PlatformTag.ePlt.getGameId(), PropertyRspType.PASSIVE);
			C2SSessionService.getInstance().sendAllOLPlayers(rspBuilder);
		}
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

				if (eType == EGameType.DT) {
					LoginModule.tryEnterRoomWhenLoginSuccess = sysParamModel7002.getVal1().intValue() == 1;
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
}
