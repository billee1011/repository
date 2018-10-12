/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Objects;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.define.LoginType;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.UserPhoneRMIVo;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RedisKeyUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.module.LoginMsgSender;
import com.cai.redis.service.RedisService;
import com.cai.tasks.PhoneLoginTask;
import com.cai.util.MessageResponse;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.LoginRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.PhoneLoginReq;
import protobuf.clazz.c2s.C2SProto.PhoneReqProto;

/**
 * 
 * 
 * 
 * @author wu_hc date: 2017年11月30日 下午5:02:34 <br/>
 */
@ICmd(code = C2SCmd.PHONE_LOGIN_REQ, desc = "手机登陆")
public final class PhoneLoginHandler extends IClientHandler<PhoneLoginReq> {

	@Override
	protected void execute(PhoneLoginReq req, Request topRequest, C2SSession session) throws Exception {

		final LoginRequest loginReq = req.getLoginReq();
		final PhoneReqProto phoneReq = req.getPhoneInfo();

		final String mobile = phoneReq.getMobile();
		if (!MobileUtil.isValid(mobile)) {
			session.send(MessageResponse.getMsgAllResponse("手机号码不合法!").build());
			return;
		}

		SessionUtil.setAttr(session, AttributeKeyConstans.LOGIN_TYPE, LoginType.MOBILE);
		RedisService redisService = SpringService.getBean(RedisService.class);
		String identifyCode = redisService.get(RedisKeyUtil.phoneLogin(mobile));
		if (Strings.isNullOrEmpty(identifyCode)) {
			LoginMsgSender.sendLoginFailedRsp(session, 1, 1);
			return;
		}

		if (!Objects.equals(identifyCode, phoneReq.getIdentifyCode())) {
			session.send(MessageResponse.getMsgAllResponse("验证码不正确!").build());
			return;
		}
		UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND_INFO, 0L, mobile);
		Pair<Integer, String> r = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);

		if (r.getFirst().intValue() > 0) {
			long accountId = Longs.tryParse(r.getSecond());
			Global.getPtLoginService().execute(new PhoneLoginTask(session, loginReq, accountId));
		} else {
			session.send(MessageResponse.getMsgAllResponse("手机未进行绑定!").build());
		}
	}

}
