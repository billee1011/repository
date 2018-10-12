package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

/**
 * 牌类型缓存统计
 * 
 * @author run
 *
 */
public class CardTypeCacheTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(CardTypeCacheTimer.class);

	@Override
	public void run() {
		try {
			String msg = PlayerServiceImpl.getInstance().getCardTypeCache().stats().toString();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.cardTypdCacheStat, msg, null, null, ESysLogLevelType.NONE);
		} catch (Exception e) {
			logger.error("error", e);
		}

	}
}
