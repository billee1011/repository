package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.info.PlayerRoundLimitInfo;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubPlayerLimitRoundDataResponse;
import protobuf.clazz.ClubMsgProto.LimitRoundData;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月20日 上午9:51:48
 */
@ICmd(code = C2SCmd.CLUB_PLAYER_LIMIT_ROUND_REQ, desc = "俱乐部玩家限制局数数据请求")
public class ClubPlayerLimitRoundReqHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubRuleTable clubRuleTable = club.ruleTables.get(req.getRuleId());
			if (clubRuleTable == null) {
				return;
			}
			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator) {
				return;
			}
			if (!EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}

			ClubPlayerLimitRoundDataResponse.Builder builder = ClubPlayerLimitRoundDataResponse.newBuilder();
			builder.setClubId(req.getClubId());
			builder.setRuleId(req.getRuleId());
			for (ClubMemberModel member : club.members.values()) {
				LimitRoundData.Builder data = LimitRoundData.newBuilder();
				data.setAccountId(member.getAccount_id());
				PlayerRoundLimitInfo info = member.getPlayerRoundLimitInfoByRuleId(req.getRuleId());
				if (info == null) {
					continue;
				}
				data.setValue(info.getLimitRound());
				builder.addData(data);
			}
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_PLAYER_LIMIT_ROUND_RSP, builder));
		});
	}

}
