/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.rmi.handler;

import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.SessionService;

import protobuf.clazz.ClubMsgProto.ClubExclusiveActivityUpdateProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月25日 上午11:58:53 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_ACTIVITY_UPDATE, desc = "俱乐部专属豆活动有更新")
public final class ClubExclusiveActUpdateRMIHandler extends IRMIHandler<List<Integer>, Void> {

	@Override
	public Void execute(List<Integer> message) {
		SessionService.getInstance().sendAllOnline(S2CCmd.CLUB_EXCLUSIVE_ACTIVITY_UPDATE, ClubExclusiveActivityUpdateProto.newBuilder());
		return null;
	}
}
