package com.cai.util;

import java.util.Date;

import com.cai.common.domain.MobileLogModel;
import com.cai.service.MongoDBServiceImpl;

/**
 * 
 * 
 *
 */
public final class MobileLogUtil {

	/*
	 * 
	 */
	public static void log(long account_id, String log_type, String mobile) {
		MongoDBServiceImpl.getInstance().mobileLog(MobileLogUtil.newModel(account_id, log_type, mobile, null, null, null, null));
	}

	public static void log(long account_id, String log_type, String mobile, Integer v1, String msg, String v2, String v3) {
		MongoDBServiceImpl.getInstance().mobileLog(MobileLogUtil.newModel(account_id, log_type, mobile, v1, msg, v2, v3));
	}

	/**
	 * 
	 * @param accountId
	 * @param log_type
	 * @param mobile
	 * @return
	 */
	public static MobileLogModel newModel(long account_id, String log_type, String mobile, Integer v1, String msg, String v2, String v3) {
		MobileLogModel model = new MobileLogModel();
		model.setAccount_id(account_id);
		model.setCreate_time(new Date());
		model.setLog_type(log_type);
		model.setMobile(mobile);
		model.setMsg(msg);
		model.setV1(v1);
		model.setV2(v2);
		model.setV3(v3);
		return model;
	}
}
