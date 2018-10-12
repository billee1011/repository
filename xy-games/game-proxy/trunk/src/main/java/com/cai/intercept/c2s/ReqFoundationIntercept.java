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
 * 连接到游戏基础服，直接转发
 * 
 * @author chansonyan 2018年4月22日
 */
public final class ReqFoundationIntercept implements ReqIntercept {

	private static final Set<Integer> FOUNDATION_CMDS = Sets.newHashSet();

	static {
		FOUNDATION_CMDS.add(C2SCmd.LOAD_FOUNDATION_ACTIVITY);
		FOUNDATION_CMDS.add(C2SCmd.TAKE_FOUNDATION_MISSION_REWARD);
		FOUNDATION_CMDS.add(C2SCmd.ACTIVITY_RECORD);
		FOUNDATION_CMDS.add(C2SCmd.ACTIVITY_REWARD_DETAIL);
		FOUNDATION_CMDS.add(C2SCmd.REDPACKET_POOL_ACTIVITY);
		FOUNDATION_CMDS.add(C2SCmd.REDPACKET_POOL_EXCHANGE);
		FOUNDATION_CMDS.add(C2SCmd.PUSH_REPROT);
		FOUNDATION_CMDS.add(C2SCmd.ACTIVITY_MANUAL_START);
		FOUNDATION_CMDS.add(C2SCmd.TAKE_MISSION_GROUP_REWARD);
		FOUNDATION_CMDS.add(C2SCmd.LOAD_NEW_USER_ACTIVITY);
		FOUNDATION_CMDS.add(C2SCmd.TAKE_NEW_USER_REWARD);
		FOUNDATION_CMDS.add(C2SCmd.LOAD_INVITE_FRIENDS_ACTIVITY);
		FOUNDATION_CMDS.add(C2SCmd.TAKE_INVITE_FRIENDS_REWARD);
		FOUNDATION_CMDS.add(C2SCmd.QUOTA_OF_PERSION);
	}

	@Override
	public boolean intercept(CommonProto commProto, Request topRequest, C2SSession session) {
		int cmd = commProto.getCmd();
		if (FOUNDATION_CMDS.contains(cmd)) {
			final Account account = session.getAccount();
			if (null != account) {
				TransmitProto.Builder builder = TransmitProto.newBuilder();
				builder.setAccountId(account.getAccount_id());
				builder.setServerIndex(SystemConfig.proxy_index);
				builder.setCommonProto(commProto);
				ClientServiceImpl.getInstance().sendToFoundation(PBUtil.toS2SRequet(S2SCmd.C_2_FOUNDATION, builder).build());
			}
			return true;
		}
		return false;
	}

}
