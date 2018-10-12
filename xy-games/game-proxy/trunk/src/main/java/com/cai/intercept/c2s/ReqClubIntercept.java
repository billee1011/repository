/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.c2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Account;
import com.cai.common.handler.C2ClubCmdRegister;
import com.cai.common.util.PBUtil;
import com.cai.core.SystemConfig;
import com.cai.service.ClientServiceImpl;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月19日 上午11:02:05 <br/>
 */
public final class ReqClubIntercept implements ReqIntercept {

	@Override
	public boolean intercept(CommonProto commProto, Request topRequest, C2SSession session) {
		int cmd = commProto.getCmd();

		if (C2ClubCmdRegister.C2Club_cmds.contains(cmd)) {
			final Account account = session.getAccount();
			if (null != account) {
				TransmitProto.Builder builder = TransmitProto.newBuilder();
				builder.setAccountId(account.getAccount_id());
				builder.setServerIndex(SystemConfig.proxy_index);
				builder.setCommonProto(commProto);

				boolean result = ClientServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.C_2_CLUB, builder).build());
				if (!result) {
//					session.send(MessageResponse.getMsgAllResponse("该功能正在维护中，请稍微再试！").build());
				}
			}
			return true;
		}
		return false;
	}

}
