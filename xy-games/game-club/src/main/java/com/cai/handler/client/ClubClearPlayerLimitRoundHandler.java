package com.cai.handler.client;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.tasks.db.ClubRecordDBTask;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubClearPlayerLimitRoundReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月28日 下午5:15:50
 */
@ICmd(code = C2SCmd.CLUB_CLEAR_PLAYER_ROUND_LIMIT, desc = "清除俱乐部玩家限制局数设置")
public class ClubClearPlayerLimitRoundHandler extends IClientExHandler<ClubClearPlayerLimitRoundReq> {

	@Override
	protected void execute(ClubClearPlayerLimitRoundReq req, TransmitProto topReq, C2SSession session) throws Exception {
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

			ClubMemberModel member = club.members.get(req.getTargetId());
			if (member != null) {
				List<ClubMemberModel> members = new ArrayList<>();
				boolean result = member.clearPlayerLimitRound(req.getRuleId());
				if (result) {
					members.add(member);
				}

				club.runInDBLoop(new ClubRecordDBTask(members));
			}
		});
	}

}
