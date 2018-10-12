package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchSetAttendListReq;
import protobuf.clazz.ClubMsgProto.ClubMatchSetAttendMemberResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月26日 下午12:28:20
 */
@ICmd(code = C2SCmd.CLUB_MATCH_SET_ATTEND_LIST_REQ, desc = "管理员设置比赛参赛人员数据请求")
public class ClubMatchSetAttendListReqHandler extends IClientExHandler<ClubMatchSetAttendListReq> {

	@Override
	protected void execute(ClubMatchSetAttendListReq req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operater = club.members.get(topReq.getAccountId());
			if (operater == null || !EClubIdentity.isManager(operater.getIdentity())) {
				return;
			}
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if (wrap == null) {
				return;
			}
			ClubMatchSetAttendMemberResponse.Builder builder = ClubMatchSetAttendMemberResponse.newBuilder();
			builder.setClubId(req.getClubId());
			builder.setMatchId(req.getMatchId());

			builder.addAllAccounts(wrap.getEnrollAccountIds());

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_LIST_RSP, builder));
		});

	}

}
