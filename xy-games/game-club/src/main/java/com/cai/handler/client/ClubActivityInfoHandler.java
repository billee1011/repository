/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubActivityInfoReqProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2018年1月23日 上午9:58:19 <br/>
 */
@ICmd(code = C2SCmd.CLUB_ACTIVITY_INFO, desc = "俱乐部活动列表")
public final class ClubActivityInfoHandler extends IClientExHandler<ClubActivityInfoReqProto> {

	@Override
	protected void execute(ClubActivityInfoReqProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member) {
				return;
			}

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_ACTIVITY_INFO,
					0 == req.getActivityIdsCount() ? club.toActivityListBuilder() : club.toActivityListBuilder(req.getActivityIdsList())));
		});

	}
}
