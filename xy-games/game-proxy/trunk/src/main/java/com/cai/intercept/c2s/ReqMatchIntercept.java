/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.c2s;

import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Account;
import com.cai.common.util.PBUtil;
import com.cai.core.SystemConfig;
import com.cai.service.ClientServiceImpl;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月19日 上午11:02:05 <br/>
 */
public final class ReqMatchIntercept implements ReqIntercept {
	private static final Set<Integer> MATCH_CMDS = Sets.newHashSet();

	static {
		MATCH_CMDS.add(C2SCmd.RED_HEART);
		MATCH_CMDS.add(C2SCmd.EMAIL);
		MATCH_CMDS.add(C2SCmd.ITEM);
		MATCH_CMDS.add(C2SCmd.CITY_REPORT);
	}

	@Override
	public boolean intercept(CommonProto commProto, Request topRequest, C2SSession session) {
		int cmd = commProto.getCmd();

		if (MATCH_CMDS.contains(cmd)) {
			final Account account = session.getAccount();
			if (null != account) {
				TransmitProto.Builder builder = TransmitProto.newBuilder();
				builder.setAccountId(account.getAccount_id());
				builder.setServerIndex(SystemConfig.proxy_index);
				builder.setCommonProto(commProto);

				boolean result = ClientServiceImpl.getInstance().sendMatch(SystemConfig.match_index,
						PBUtil.toS2SRequet(S2SCmd.C_2_CLUB, builder).build());
				if (!result) {
					// session.send(MessageResponse.getMsgAllResponse("该功能正在维护中，请稍微再试！").build());
				}
			}
			return true;
		}
		return false;
	}

}
