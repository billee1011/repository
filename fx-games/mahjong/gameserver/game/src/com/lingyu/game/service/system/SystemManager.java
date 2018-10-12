package com.lingyu.game.service.system;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.entity.ServerInfo;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.game.GameServerContext;

@Service
public class SystemManager {
	private static final Logger logger = LoggerFactory.getLogger(SystemManager.class);

	@Autowired
	private ServerInfoRepository serverInfoRepository;

	public void initialize() {

		ServerInfo serverInfo = serverInfoRepository.loadServerInfoFromDb();
		ServerConfig config = GameServerContext.getAppConfig();

		if (serverInfo == null) {
			serverInfo = new ServerInfo();
			serverInfo.setId(config.getWorldId());
			serverInfo.setName(config.getWorldName());
			serverInfo.setCombineTime(TimeConstant.DATE_LONG_AGO);
			serverInfo.setMaintainTime(TimeConstant.DATE_LONG_AGO);
			serverInfo.setOpenTime(new Date());
			serverInfo.setStartTime(DateUtils.addMonths(new Date(), 1));
			serverInfo.setStatus(SystemConstant.SERVER_STATUS_OPENING);
			serverInfo.setTimes(0);
			serverInfoRepository.insert(serverInfo);
			logger.info("初始化服务器信息 ServerInfo={}", serverInfo.toString());
		} else {
			serverInfo.setTimes(serverInfo.getTimes() + 1);
			serverInfo.setOpenTime(new Date());
			serverInfoRepository.update(serverInfo);
			logger.info("服务器信息更新 ServerInfo={}", serverInfo.toString());
		}
		GameServerContext.setServerInfo(serverInfo);
	}
	
	/**
	 * 更新服务器信息 
	 * @param info
	 */
	public void update(ServerInfo info) {
		if (SystemConstant.serverType == SystemConstant.SERVER_TYPE_GAME) {
			serverInfoRepository.update(info);
		}
	}

	/**
	 * 获取开服天数
	 * @return
	 */
	public int getServerStartElapseDays(){
		ServerInfo serverInfo = GameServerContext.getServerInfo();
		Date startTime = serverInfo.getStartTime();
		Date now = new Date();
		int elapseDays = TimeUtil.getIntervalDay(startTime, now);
		return elapseDays + 1;
	}
	
	/**
	 * 获取合服天数
	 * @return
	 */
	public int getServerCombineElapseDays(){
		ServerInfo serverInfo = GameServerContext.getServerInfo();
		Date combineTime = serverInfo.getCombineTime();
		Date now = new Date();
		int elapseDays = TimeUtil.getIntervalDay(combineTime, now);
		return elapseDays + 1;
	}

	/**
	 * 服务器是否在时间点之前开的
	 * @param timeLine
	 * @return
	 */
	public boolean isServerStartBefore(Date timeLine) {
		ServerInfo serverInfo = GameServerContext.getServerInfo();
		return serverInfo != null && serverInfo.getStartTime().before(timeLine);
	}
}