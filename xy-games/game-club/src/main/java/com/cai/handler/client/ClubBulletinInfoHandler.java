/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubBulletinInfoReqProto;
import protobuf.clazz.ClubMsgProto.ClubBulletinInfoRspProto;
import protobuf.clazz.ClubMsgProto.ClubBulletinProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * @author wu_hc date: 2018年4月12日 上午10:52:23 <br/>
 */
@ICmd(code = C2SCmd.CLUB_BULLETIN_INFO, desc = "公告信息详情")
public final class ClubBulletinInfoHandler extends IClientExHandler<ClubBulletinInfoReqProto> {

	@Override
	protected void execute(ClubBulletinInfoReqProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member) {
				return;
			}

			List<ClubBulletinProto.Builder> clubBulletins =
					req.getBulletinIdsCount() == 0 ? club.toAllBulletinBuilder() : club.toBulletinBuilder(req.getBulletinIdsList());
			ClubBulletinInfoRspProto.Builder builder = ClubBulletinInfoRspProto.newBuilder().setClubId(req.getClubId());
			clubBulletins.forEach((pb) -> {
				builder.addBulletins(pb.build());
			});
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_BULLETIN_INFO, builder));
		});
	}
}
