/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.ClubRoomJsonModel;
import com.cai.common.domain.json.ClubRoomJsonModel.RoomJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.dictionary.SysParamServerDict;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 
 * 5点多更新子游戏
 * 
 * @author DIY date: 2018年1月8日 下午11:07:25 <br/>
 */
public class FiveUpgradeGameRunnable implements Runnable {

	private static Logger logger = Logger.getLogger(FiveUpgradeGameRunnable.class);

	@Override
	public void run() {
		try {
			FastMap<Integer, SysParamModel> paraMap = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6);
			if (paraMap == null) {
				logger.error("FiveUpgradeGameRunnable没有配置参数");
				return;
			}
			SysParamModel param = paraMap.get(2237);
			if (param == null || param.getVal3() == 0) {
				logger.error("FiveUpgradeGameRunnable  param 没有配置参数");
				return;
			}
			updateAppItemOnline();
			
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

			centerRMIServer.reLoadSysParamServerDict();

			centerRMIServer.reLoadAppItemDictionary();
			logger.error("加载客户端配置reLoadAppItemDictionary");

			centerRMIServer.reLoadSysParamDict();
			logger.error("加载客户端配置reLoadSysParamDict");

			centerRMIServer.reloadServerBalanceDict();
			logger.error("加载客户端配置reloadServerBalanceDict");

			centerRMIServer.reLoadGameGroupRuleDictionary();
			logger.error("加载客户端配置reLoadGameGroupRuleDictionary");

			centerRMIServer.reLoadSysGameTypeDictionary();
			logger.error("加载客户端配置reLoadSysGameTypeDictionary");

			centerRMIServer.reLoadSysParamServerDict();
			logger.error("加载客户端配置reLoadSysParamServerDict");

			centerRMIServer.reLoadGameGroupRuleDictionary();

			centerRMIServer.reLoadServerDictDictionary();
			logger.error("加载客户端配置reLoadServerDictDictionary");

			centerRMIServer.reLoadGameDescDictionary();
			logger.error("加载客户端配置reLoadGameDescDictionary");

			centerRMIServer.reLoadAppItemDictionary();
			logger.error("加载客户端配置reLoadAppItemDictionary");

			centerRMIServer.reLoadSysParamDict();

			centerRMIServer.reLoadSysGameTypeDictionary();

			centerRMIServer.reLoadGameGroupRuleDictionary();

//			for (int i = 0; i < 5; i++) {
//				Global.getDbService().schedule(new Runnable() {
//
//					@Override
//					public void run() {
//						if (param.getStr2() != null && param.getStr2() != "0") {
//							RoomJsonModel roomJson = new RoomJsonModel("text");
//							roomJson.setContent(param.getStr2());
//							ClubRoomJsonModel clubJson = new ClubRoomJsonModel(0, "10384936334@chatroom", 9130767, "游戏有更新", roomJson);
//							try {
//								HttpClientUtils.httpPostWithJSON("http://39.108.11.126/api/route/init/scheduled/data", JSON.toJSONString(clubJson));
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
//					}
//
//				}, i + i * 10, TimeUnit.MINUTES);
//			}

		} catch (Exception e) {
			logger.error("定时开启游戏异常", e);
		}

	}

	private void updateAppItemOnline() {
		try{
			PublicService publicService = SpringService.getBean(PublicService.class);
			publicService.getPublicDAO().updateAppItemOnline();
		}catch(Exception e){
			logger.error("更新并加载appitem失败 ",e);
		}
	}

}
