package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.log.ClubApplyLogModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubApplyQuitRejectResponse;
import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年5月8日 下午5:03:08
 */
@ICmd(code = C2SCmd.CLUB_QUIT_APPLY_REJECT, desc = "俱乐部申请退出拒绝")
public class ClubApplyQuitRejectHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubApplyQuitRejectResponse.Builder builder = ClubApplyQuitRejectResponse.newBuilder();
			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
				builder.setIsSuccess(false);
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_QUIT_APPLY_REJECT_RSP, builder));
				return;
			}
			ClubMemberModel target = club.members.get(req.getTargetId());
			if (target == null) {
				builder.setIsSuccess(false);
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_QUIT_APPLY_REJECT_RSP, builder));
				return;
			}

			ClubApplyLogModel model = club.requestQuitMembers.remove(req.getTargetId());
			if (model != null && !model.isHandle()) {
				model.setHandle(true);
				MongoDBServiceImpl.getInstance().updateClubApplyLogModel(model);
			}
			builder.setClubId(req.getClubId());
			builder.setTargetId(req.getTargetId());
			builder.setIsSuccess(true);
			
			Utils.sendClient(club.getManagerIds(), S2CCmd.CLUB_QUIT_APPLY_REJECT_RSP, builder);
		});
	}

}
