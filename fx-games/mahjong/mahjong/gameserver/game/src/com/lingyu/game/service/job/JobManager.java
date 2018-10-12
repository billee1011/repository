package com.lingyu.game.service.job;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.io.SessionManager;
import com.lingyu.game.service.mahjong.MahjongManager;
import com.lingyu.game.service.stat.StatManager;

@Service
public class JobManager {
	private static final Logger logger = LogManager.getLogger(JobManager.class);
	
	@Autowired
	private MahjongManager mahjongManager;
	@Autowired
	private StatManager statManager;
	
	/** 重置房间编号缓存*/
	public void resetRoomIds() {
		logger.info("resetRoomIds start");
		mahjongManager.resetMajhongRoomNum();
		logger.info("resetRoomIds end");
	}
	
	/** 在线人数 */
	public void statOnlineCount() {
		logger.info("statRealTimeJob start");
		int num = SessionManager.getInstance().getOnlineCount();
		statManager.statRealTime(num);
		logger.info("statRealTimeJob end");
	}
}
