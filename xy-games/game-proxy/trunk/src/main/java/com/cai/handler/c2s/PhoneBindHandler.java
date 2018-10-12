/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
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
import com.cai.service.FoundationService;
import com.cai.util.MessageResponse;
import com.cai.util.MobileLogUtil;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.c2s.C2SProto.PhoneBindInfoRsp;
import protobuf.clazz.c2s.C2SProto.PhoneReqProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月1日 下午3:02:47 <br/>
 */
@ICmd(code = C2SCmd.PHONE_BIND, desc = "手机绑定")
public final class PhoneBindHandler extends IClientHandler<PhoneReqProto> {

	@Override
	protected void execute(PhoneReqProto req, Request topRequest, C2SSession session) throws Exception {

		Account account = session.getAccount();
		if (null == account) {
			return;
		}
		final AccountModel model = account.getAccountModel();
		if (!MobileUtil.isMobileNull(model.getMobile_phone())) {
			session.send(MessageResponse.getMsgAllResponse("帐号已经邦定过!").build());
			return;
		}

		final String mobile = req.getMobile();
		if (!MobileUtil.isValid(mobile)) {
			session.send(MessageResponse.getMsgAllResponse("手机号码不合法!").build());
			return;
		}

		String identifyCode = SpringService.getBean(RedisService.class).get(RedisKeyUtil.phoneBind(mobile));
		if (Strings.isNullOrEmpty(identifyCode)) {
			session.send(MessageResponse.getMsgAllResponse("验证码不存在!").build());
			return;
		}

		if (!Objects.equal(identifyCode, req.getIdentifyCode())) {
			session.send(MessageResponse.getMsgAllResponse("验证码不正确!").build());
			return;
		}

		GlobalExecutor.asyn_execute(() -> {
			UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND, session.getAccountID(), mobile);
			Pair<Integer, String> r = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
			if (r.getFirst().intValue() == XYCode.SUCCESS) {
				session.send(MessageResponse.getMsgAllResponse("绑定成功!").build());
				model.setMobile_phone(mobile);
				
				//绑定手机号码完成任务
				FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(),
						ActivityMissionTypeEnum.PHONE_BOND, 1, 1);
			} else {
				session.send(MessageResponse.getMsgAllResponse(r.getSecond()).build());
			}

			PhoneBindInfoRsp.Builder builder = PhoneBindInfoRsp.newBuilder();
			builder.setType(IPhoneOperateType.BIND);
			builder.setStatus(r.getFirst().intValue());
			builder.setMobile(mobile);
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.PHONE_BIND, builder));

			AccountPropertyListResponse.Builder proBuilder = AccountPropertyListResponse.newBuilder();
			AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.PHONE.getId(),
					null, null, null, null, null, mobile, null, null);
			proBuilder.addAccountProperty(accountPropertyResponseBuilder);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.PROPERTY);
			responseBuilder.setExtension(Protocol.accountPropertyListResponse, proBuilder.build());
			session.send(responseBuilder.build());

			MobileLogUtil.log(account.getAccount_id(), ELogType.moblileBind.getId(), mobile, r.getFirst().intValue(), r.getSecond(), null, null);
			
		});
	}
}
