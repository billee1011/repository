package com.cai.handler.client;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.tasks.db.ClubRecordDBTask;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubSetPlayerLimitRoundProtoReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月20日 上午9:30:50
 */
@ICmd(code = C2SCmd.CLUB_SET_PLAYER_ROUND_LIMIT, desc = "设置俱乐部玩家限制局数")
public class ClubSetPlayerLimitRoundHandler extends IClientExHandler<ClubSetPlayerLimitRoundProtoReq> {

	@Override
	protected void execute(ClubSetPlayerLimitRoundProtoReq req, TransmitProto topReq, C2SSession session) throws Exception {
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
			List<ClubMemberModel> members = new ArrayList<>();
			List<Long> targets = req.getAccountIdsList();
			for (Long targetId : targets) {
				ClubMemberModel member = club.members.get(targetId);
				if (member == null) {
					continue;
				}
				member.updatePlayerLimitRound(req.getRuleId(), req.getValue());
				members.add(member);
			}

			club.runInDBLoop(new ClubRecordDBTask(members));

			Utils.sendTip(topReq.getAccountId(), "设置成功！", ESysMsgType.INCLUDE_ERROR, session);
		});
	}

}
