package com.cai.rmi.handler;

import java.util.Date;
import java.util.Map;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPhoneIdentifyCodeType;
import com.cai.common.domain.MobileLogModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PhoneService;
import com.google.common.base.Strings;

/**
 * 
 * 
 *
 * @author tang date: 2018年3月6日 上午10:00:42 <br/>
 */
@IRmi(cmd = RMICmd.CUSTOMER_PRODUCT_CODE, desc = "客服通过管理系统生成验证码")
public final class CustomerCodeRMIHandler extends IRMIHandler<Map<String, String>, Integer> {

	@SuppressWarnings("unchecked")
	@Override
	public Integer execute(Map<String, String> map) {
		String mobile = map.get("mobile");
		String typeStr = map.get("type");
		if (Strings.isNullOrEmpty(mobile) || !MobileUtil.isValid(mobile)) {
			return -1;
		}
		int type = Integer.parseInt(typeStr);
		EPhoneIdentifyCodeType codeType = EPhoneIdentifyCodeType.of(type);
		if (null == codeType) {
			return -1;
		}
		int code = PhoneService.getInstance().randomIdentifyCode();
		if (code == 0) {
			return -1;
		}
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.set(codeType.exe().apply(mobile), code + "", codeType.getAlive());
		MobileLogModel model = newModel(type + "", mobile, code + "");
		MongoDBServiceImpl.getInstance().getLogQueue().add(model);
		return code;
	}

	public static MobileLogModel newModel(String log_type, String mobile, String v3) {
		MobileLogModel model = new MobileLogModel();
		model.setAccount_id(0);
		model.setCreate_time(new Date());
		model.setLog_type(ELogType.moblileIdentifyCode.getId());
		model.setMobile(mobile);
		model.setMsg("ok");
		model.setV1(1);
		model.setV2("1");
		model.setV3(v3);
		return model;
	}
}
