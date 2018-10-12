/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.ClubEventLogModel;
import com.cai.config.ClubCfg;
import com.cai.service.MongoDBServiceImpl;

/**
 * 俱乐部事件日志
 *
 * @author wu_hc date: 2018年4月4日 上午10:42:29 <br/>
 */
public final class ClubEventLog {

	private static final Logger logger = LoggerFactory.getLogger(ClubEventLog.class);

	public static void event(ClubEventLogModel model) {
		logger.warn(model.buildDesc().getDesc());

		if (ClubCfg.get().isSaveClubEventDB()) {
			MongoDBServiceImpl.getInstance().getLogQueue().add(model);
		}
	}
}
