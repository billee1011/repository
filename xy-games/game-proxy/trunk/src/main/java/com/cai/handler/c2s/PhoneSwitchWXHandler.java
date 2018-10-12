/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.EGameType;
import com.cai.core.Global;
import com.cai.tasks.UserSwitchWXTask;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.PhoneSwitchWXReq;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月1日 下午3:02:47 <br/>
 */
@ICmd(code = C2SCmd.PHONE_SWITCH_WX, desc = "切换微信")
public final class PhoneSwitchWXHandler extends IClientHandler<PhoneSwitchWXReq> {

	@Override
	protected void execute(PhoneSwitchWXReq req, Request topRequest, C2SSession session) throws Exception {
		final long accountId = session.getAccountID();
		if (accountId <= 0) {
			return;
		}
		if (Strings.isNullOrEmpty(req.getWxCode())) {
			return;
		}

		Global.getUseSwitchService().execute(new UserSwitchWXTask(req.getWxCode(), EGameType.DT.getId(), accountId));
	}
}
