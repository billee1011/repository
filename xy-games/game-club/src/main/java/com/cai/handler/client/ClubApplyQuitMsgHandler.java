package com.cai.handler.client;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubApplyExitProto;
import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年5月8日 下午3:16:53
 */
@ICmd(code = C2SCmd.CLUB_APPLY_QUIT_MSG_REQUEST, desc = "俱乐部申请退出信息 ")
public class ClubApplyQuitMsgHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {

			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			ClubProto.Builder builder = ClubProto.newBuilder();
			builder.setClubId(club.getClubId());
			List<Long> removeList = new ArrayList<>();
			club.requestQuitMembers.forEach((id, model) -> {
				ClubApplyExitProto.Builder exitBuilder = ClubApplyExitProto.newBuilder();
				exitBuilder.setCreateTime(model.getCreate_time().getTime());
				exitBuilder.setAccountId(model.getAccountId());
				exitBuilder.setAvatar(model.getAvatar());
				exitBuilder.setNickname(model.getNickname());
				exitBuilder.setApplyTime(model.getApplyTime().getTime());
				if (club.members.get(model.getAccountId()) == null) {
					removeList.add(model.getAccountId());
					model.setHandle(true);
					MongoDBServiceImpl.getInstance().updateClubApplyLogModel(model);
					return;
				}
				ClubMemberRecordModel memberRecordModel = club.getMemberRecordModelByDay(1, club.members.get(model.getAccountId()));
				exitBuilder.setTireValue(club.getMemberRealUseTire(memberRecordModel));
				builder.addApplyExitList(exitBuilder);
			});
			for (Long id : removeList) {
				club.requestQuitMembers.remove(id);
			}

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_APPLY_QUIT_MSG_RSP, builder));
		});
	}

}
