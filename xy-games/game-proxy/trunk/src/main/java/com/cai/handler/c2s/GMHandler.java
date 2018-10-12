/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.Account;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.google.common.primitives.Ints;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.MessageReceiveRsp;
import protobuf.clazz.c2s.C2SProto.__GMReq;

/**
 * 
 * GM
 *
 * @author wu_hc date: 2017年10月24日 上午11:03:04 <br/>
 */
//@ICmd(code = C2SCmd.TEST_TEST, desc = "GM 命令")
public final class GMHandler extends IClientHandler<__GMReq> {

	@Override
	protected void execute(__GMReq req, Request topRequest, C2SSession session) throws Exception {
		final Account account = session.getAccount();
		if (null == account) {
			return;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		if (req.getType() == 1) {
			int num = Ints.tryParse(req.getValue());
			// centerRMIServer.addAccountGold(account.getAccount_id(), num,
			// false, "压力测试添加房卡", EGoldOperateType.OSS_OPERATE_DEC);
			centerRMIServer.addAccountGoldAndMoney(account.getAccount_id(), num, false, "压力测试添加房卡", EGoldOperateType.OSS_OPERATE_DEC, 300000,
					EMoneyOperateType.ADD_MONEY);

		}

		MessageReceiveRsp.Builder builder = MessageReceiveRsp.newBuilder();
		builder.setType(req.getType());
		builder.setVar1(1);
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.TEST_TEST, builder));
	}
}
