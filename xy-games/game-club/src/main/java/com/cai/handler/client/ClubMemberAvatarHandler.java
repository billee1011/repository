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

import protobuf.clazz.ClubMsgProto.ClubMemberAvatarReq;
import protobuf.clazz.ClubMsgProto.ClubMemberAvatarRsp;
import protobuf.clazz.Common.CommonLS;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年3月12日 下午12:13:50 <br/>
 */
@ICmd(code = C2SCmd.CLUB_MEMBER_AVATAR, desc = "俱乐部成员头像信息")
public final class ClubMemberAvatarHandler extends IClientExHandler<ClubMemberAvatarReq> {

	@Override
	protected void execute(ClubMemberAvatarReq req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			if (!club.isMember(topReq.getAccountId())) {
				return;
			}

			ClubMemberAvatarRsp.Builder builder = ClubMemberAvatarRsp.newBuilder();
			builder.setClubId(req.getClubId());

			if (req.getAccountIdsCount() == 0) {
				club.members.forEach((account_id, model) -> {
					builder.addAvatars(CommonLS.newBuilder().setK(account_id).setV(model.getAvatar()));
				});
			} else {
				req.getAccountIdsList().forEach(account_id -> {
					ClubMemberModel model = club.members.get(account_id);
					if (null == model)
						return;
					builder.addAvatars(CommonLS.newBuilder().setK(account_id).setV(model.getAvatar()));
				});
			}

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MEMBER_AVATAR, builder));
		});
	}
}
