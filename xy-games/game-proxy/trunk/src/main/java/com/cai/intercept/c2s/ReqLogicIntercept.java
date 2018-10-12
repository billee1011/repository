/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.c2s;

import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Account;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RoomIDUtil;
import com.cai.common.util.SessionUtil;
import com.cai.core.SystemConfig;
import com.cai.service.ClientServiceImpl;
import com.cai.util.RoomUtil;
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
public final class ReqLogicIntercept implements ReqIntercept {

	private static final Set<Integer> LOGIC_CMDS = Sets.newHashSet();
	static {
		LOGIC_CMDS.add(C2SCmd.VOICE_STATUS);
	}

	@Override
	public boolean intercept(CommonProto commProto, Request topRequest, C2SSession session) {
		int cmd = commProto.getCmd();

		if (LOGIC_CMDS.contains(cmd)) {
			final Account account = session.getAccount();
			if (null != account) {

				int roomId = RoomUtil.getRoomId(account.getAccount_id());
				if (roomId <= 0) {
					return true;
				}
				int serverIndex = SessionUtil.getLogicSvrId(session, roomId);
				if (serverIndex == -1) {
					serverIndex = RoomIDUtil.ID_CACHE.getUnchecked(roomId).intValue();
				}
				if (serverIndex == -1) {
					return true;
				}
				TransmitProto.Builder builder = TransmitProto.newBuilder();
				builder.setAccountId(account.getAccount_id());
				builder.setServerIndex(SystemConfig.proxy_index);
				builder.setCommonProto(commProto);
				ClientServiceImpl.getInstance().sendMsg(serverIndex, PBUtil.toS2SRequet(S2SCmd.C_2_CLUB, builder).build());
			}
			return true;
		}
		return false;
	}

}
