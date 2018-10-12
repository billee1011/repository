/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.aliyuncs.exceptions.ClientException;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPhoneIdentifyCodeType;
import com.cai.common.define.XYCode;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.config.MobileConfig;
import com.cai.redis.service.RedisService;
import com.cai.service.SmsService;
import com.cai.util.MessageResponse;
import com.cai.util.MobileLogUtil;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.PhoneIdentifyCodeReqProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月1日 下午3:02:47 <br/>
 */
@ICmd(code = C2SCmd.PHONE_IDENTIFY_CODE, desc = "手机验证码获取,可能在没登陆的时候就获取了")
public final class PhoneIdentifyCodeHandler extends IClientHandler<PhoneIdentifyCodeReqProto> {

	private static final int REQ_HZ_LIMIT = (60 + 30) * 1000; // ms

	@Override
	protected void execute(PhoneIdentifyCodeReqProto req, Request topRequest, C2SSession session) throws Exception {

		if (!MobileConfig.get().isOpenIdentifyCode()) {
			session.send(MessageResponse.getMsgAllResponse("短信功能临时关闭，请联系客服!").build());
			return;
		}
		final String mobile = req.getMobile();

		if (Strings.isNullOrEmpty(mobile) || !MobileUtil.isValid(mobile)) {
			session.send(MessageResponse.getMsgAllResponse("手机格式不对!").build());
			return;
		}

		EPhoneIdentifyCodeType codeType = EPhoneIdentifyCodeType.of(req.getType());
		if (null == codeType) {
			session.send(MessageResponse.getMsgAllResponse("验证码类型不存在!").build());
			return;
		}

		// 对单个连接的频率限制
		long lastReqTime = SessionUtil.getAttrOrDefault(session, AttributeKeyConstans.PHONE_IDENTIFY, -1L);
		if (-1 != lastReqTime && (System.currentTimeMillis() - lastReqTime) < REQ_HZ_LIMIT) {
			session.send(MessageResponse.getMsgAllResponse("验证码请求频率过快，请稍候再试!").build());
			return;
		}

		GlobalExecutor.asyn_execute(() -> {
			try {
				Integer code = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.PHONE_IDENTIFY_CODE, null);
				if (null == code) {
					return;
				}
				Pair<Integer, String> r = SmsService.getInstance().sendSms(mobile, toJson(code));
				if (r.getFirst() == XYCode.SUCCESS) {
					RedisService redis = SpringService.getBean(RedisService.class);
					redis.set(codeType.exe().apply(mobile), code.toString(), codeType.getAlive());

					SessionUtil.setAttr(session, AttributeKeyConstans.PHONE_IDENTIFY, System.currentTimeMillis());

				} else {
					logger.error("玩家[{}]请求验证码，手机[{}],但短信发送失败.[{}]!", session.getAccount(), mobile, r.getSecond());
					session.send(MessageResponse.getMsgAllResponse("验证码获取失败，请稍候再试!").build());
				}

				MobileLogUtil.log(session.getAccountID(), ELogType.moblileIdentifyCode.getId(), mobile, r.getFirst().intValue(), r.getSecond(),
						Integer.toString(req.getType()), code.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * 
	 * @param code
	 * @return
	 */
	private static final String toJson(Integer code) {
		return String.format("{\"code\":\"%d\"}", code);
	}

	public static void main(String[] args) {
		System.out.println(MobileUtil.isValid("18824304825"));
		System.out.println(toJson(666666));

		try {
			Pair<Integer, String> r = SmsService.getInstance().sendSms("18824304825", toJson(666666));
			System.out.println(r);
		} catch (ClientException e) {
			e.printStackTrace();
		}

	}
}
