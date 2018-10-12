/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.LogicRoomInfo;
import com.cai.common.domain.SubGameOnline;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;

/**
 * 
 *
 * @author DIY date: 2017年11月20日 下午7:52:19 <br/>
 */
public class SubGameOnlineTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(SubGameOnlineTimer.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		try {
			SysParamModel sysparamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(1198);
			if(sysparamModel==null || sysparamModel.getVal1()==0) return;
			
			
			PerformanceTimer timer = new PerformanceTimer();
			
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			List<LogicRoomInfo> roomInfos = centerRMIServer.getLogicRoomInfoList();
			

			Map<Integer,SubGameOnline> subGameMap = new HashMap<Integer,SubGameOnline>();
			
			for (LogicRoomInfo roomInfo: roomInfos) {
				
				int curNum = roomInfo.getCurNum();
				boolean full =roomInfo.isFull();
				
				SubGameOnline gameOnline = subGameMap.get(roomInfo.get_game_type_index());
				if(gameOnline== null) {
					gameOnline= new SubGameOnline();
					gameOnline.setCreate_time(new Date());
					gameOnline.setGame_type_index(roomInfo.get_game_type_index());
					SysGameType gameType = SysGameTypeDict.getInstance().getSysGameType(roomInfo.get_game_type_index());
					if(gameType!=null) {
						gameOnline.setGameName(gameType.getDesc());
					}
					subGameMap.put(roomInfo.get_game_type_index(), gameOnline);
				}
				gameOnline.setNumber(gameOnline.getNumber()+1);
				if(full) {
					gameOnline.setFullNumber(gameOnline.getFullNumber()+1);
				}
				gameOnline.setPlayerTotalNumber(gameOnline.getPlayerTotalNumber()+curNum);
				
				
				
				try{
					Map<Integer,HashSet<Integer>> gameTypeNumberMap = PublicServiceImpl.getInstance().getGameTypeNumberMap();
					HashSet<Integer> set = gameTypeNumberMap.get(roomInfo.get_game_type_index());
					if(set==null) {
						set = new HashSet<Integer>();
						gameTypeNumberMap.put(roomInfo.get_game_type_index(), set);
					}
					for(int accountID:roomInfo.getPlayerIDs()) {
						set.add(accountID);
					}
				}catch(Exception e) {
					logger.error("SubGameOnlineTimer error", e);
					MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
				}
			}

			for(SubGameOnline subGameOnline :subGameMap.values()) {
				MongoDBServiceImpl.getInstance().insertSubGameOnline(subGameOnline);
			}
			
			logger.warn("SubGameOnlineTimer cost time"+timer.duration());
		
			
			MongoDBServiceImpl.getInstance().systemLog(ELogType.countLogicroom, "SubGameOnlineTimer cost time"+timer.duration(), (long) roomInfos.size() , null, ESysLogLevelType.NONE);
		} catch (Exception e) {
			logger.error("error", e);
			MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}
	}

}
