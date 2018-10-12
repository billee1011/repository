package com.cai.dictionary;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.config.MobileConfig;
import com.cai.redis.service.RedisService;

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

	private volatile boolean isOpenLog = true;
	
	public final static Set<String> aliMonitorIpPrefAuto = new HashSet<>();

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

	public boolean getIsOpenLog() {
		return isOpenLog;
	}

	private void init_open_log() {

		FastMap<Integer, SysParamModel> params = sysParamModelDictionary.get(EGameType.DT.getId());
		if (null == params) {
			logger.error("找不到gameId[{}]相关配置!", EGameType.DT.getId());
			return;
		}
		SysParamModel model = params.get(1198);
		if (model != null) {
			isOpenLog = model.getVal5() == 1 ? true : false;
		}
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysParamModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM_SERVER,
					FastMap.class);
			parseMobileCfg();
			init_open_log();
			initAutpWhiteIP();
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载加载字典sys_param" + timer.getStr());
	}
	
	
	public boolean isAutoWhiteIP(String ip) {
		for (final String ipPre : aliMonitorIpPrefAuto) {
			if (ip.startsWith(ipPre)) {
				return true;
			}
		}
		return false;
	}
	public void initAutpWhiteIP() {
		try {
			aliMonitorIpPrefAuto.clear();
			FastMap<Integer, SysParamModel> paraMap = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6);
			if (paraMap != null) {
				SysParamModel param = paraMap.get(2235);
				if (param != null && StringUtils.isNotEmpty(param.getStr2()) && !param.getStr2().equals("0")) {
					String[] strs = StringUtils.split(param.getStr2(), ",");
					for (String str : strs) {
						aliMonitorIpPrefAuto.add(str);
					}
				}
			}
		} catch (Exception e) {
			logger.error("is WhiteIp error", e);
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

	private void parseMobileCfg() {
		FastMap<Integer, SysParamModel> params = sysParamModelDictionary.get(EGameType.DT.getId());
		if (null == params) {
			logger.error("找不到gameId[{}]相关配置!", EGameType.DT.getId());
			return;
		}

		SysParamModel paramModel = params.get(2235);
		if (null == paramModel) {
			logger.error("############### 找不到id[2235]相关配置! ###################");
			return;
		}
		MobileConfig.get().setOpenIdentifyCode(paramModel.getVal1().intValue() == 1)
				.setLoginCodeAalive(paramModel.getVal2());

		logger.info("mobile cfg:{}", MobileConfig.get());
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
