/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.ELogType;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.UserPhoneRMIVo;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RedisKeyUtil;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.util.MessageResponse;
import com.cai.util.MobileLogUtil;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.PhoneBindInfoRsp;
import protobuf.clazz.c2s.C2SProto.PhoneReqProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月1日 下午3:02:47 <br/>
 */
@ICmd(code = C2SCmd.PHONE_UN_BIND, desc = "手机解绑")
public final class PhoneUnBindHandler extends IClientHandler<PhoneReqProto> {

	@Override
	protected void execute(PhoneReqProto req, Request topRequest, C2SSession session) throws Exception {

		Account account = session.getAccount();
		if (null == account) {
			return;
		}
		final AccountModel model = account.getAccountModel();
		if (StringUtils.isEmpty(model.getMobile_phone())) {
			session.send(MessageResponse.getMsgAllResponse("帐号没有邦定记录，需要解绑操作!").build());
			return;
		}

		final String mobile = req.getMobile();

		if (!MobileUtil.isValid(mobile)) {
			session.send(MessageResponse.getMsgAllResponse("手机号码不合法!").build());
			return;
		}

		final AccountModel accountModel = account.getAccountModel();
		if (!Objects.equal(mobile, accountModel.getMobile_phone())) {
			session.send(MessageResponse.getMsgAllResponse("输入手机号和绑定的手机号不符合!").build());
			return;
		}
		String identifyCode = SpringService.getBean(RedisService.class).get(RedisKeyUtil.phoneUnBind(mobile));
		if (Strings.isNullOrEmpty(identifyCode)) {
			session.send(MessageResponse.getMsgAllResponse("验证码已失效!").build());
			return;
		}

		if (!Objects.equal(identifyCode, req.getIdentifyCode())) {
			session.send(MessageResponse.getMsgAllResponse("验证码不正确!").build());
			return;
		}

		GlobalExecutor.asyn_execute(() -> {
			UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.UN_BIND, session.getAccountID(), mobile);
			Pair<Integer, String> r = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
			if (r.getFirst().intValue() == XYCode.SUCCESS) {
				session.send(MessageResponse.getMsgAllResponse("解绑成功!").build());
				accountModel.setMobile_phone(null);

				PhoneBindInfoRsp.Builder builder = PhoneBindInfoRsp.newBuilder();
				builder.setType(IPhoneOperateType.UN_BIND);
				builder.setStatus(r.getFirst().intValue());
				builder.setMobile(mobile);
				session.send(PBUtil.toS2CCommonRsp(S2CCmd.PHONE_UN_BIND, builder));
			} else {
				session.send(MessageResponse.getMsgAllResponse(r.getSecond()).build());
			}

			MobileLogUtil.log(account.getAccount_id(), ELogType.moblileUnBind.getId(), mobile, r.getFirst().intValue(), null, null, null);
		});
	}
}
