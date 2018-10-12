package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ERuleSettingStatus;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubPlayerLeftLimitRoundResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月20日 下午8:01:48
 */
@ICmd(code = C2SCmd.CLUB_PLAYER_LEFT_ROUND_REQ, desc = "俱乐部玩家剩余限制局数请求")
public class ClubPlayerLeftLimitRoundReqHandler extends IClientExHandler<ClubCommon> {

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

			ClubRuleModel ruleModel = clubRuleTable.getClubRuleModel();
			if (ruleModel == null) {
				return;
			}

			final ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member) {
				return;
			}
			// 玩家可玩局数信息
			ClubPlayerLeftLimitRoundResponse.Builder builder = ClubPlayerLeftLimitRoundResponse.newBuilder();
			builder.setClubId(req.getClubId());
			builder.setRuleId(req.getRuleId());
			if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.GAME_ROUND_LIMIT_SWITCH)) {
				builder.setLeftRound(member.checkLimitRound(ruleModel));
			}
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_PLAYER_LEFT_LIMIT_ROUND_RSP, builder));
		});
	}

}
