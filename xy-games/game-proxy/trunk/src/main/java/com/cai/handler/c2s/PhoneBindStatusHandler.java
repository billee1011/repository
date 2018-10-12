/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.AccountModel;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.PBUtil;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.EmptyReq;
import protobuf.clazz.c2s.C2SProto.PhoneBindInfoRsp;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月1日 下午3:02:47 <br/>
 */
@ICmd(code = C2SCmd.PHONE_BIND_STATUS, desc = "玩家手机绑定情况")
public final class PhoneBindStatusHandler extends IClientHandler<EmptyReq> {

	@Override
	protected void execute(EmptyReq req, Request topRequest, C2SSession session) throws Exception {
		if (session.getAccountID() <= 0) {
			return;
		}

		PhoneBindInfoRsp.Builder builder = PhoneBindInfoRsp.newBuilder();

		final AccountModel accountModel = session.getAccount().getAccountModel();
		if (!MobileUtil.isMobileNull(accountModel.getMobile_phone())) {
			builder.setMobile(accountModel.getMobile_phone()).setStatus(XYCode.SUCCESS);
		} else {
			builder.setStatus(XYCode.FAIL);
		}
		builder.setType(IPhoneOperateType.BIND_INFO);
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.PHONE_BIND_INFO, builder));
	}
}
