/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubIgnoreInviteType;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubIgnoreInviteProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月12日 上午10:52:23 <br/>
 */
@ICmd(code = C2SCmd.CLUB_INGORE_INVITE, desc = "忽略邀请")
public final class ClubIgnoreInviteHandler extends IClientExHandler<ClubIgnoreInviteProto> {

	@Override
	protected void execute(ClubIgnoreInviteProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {

			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member) {
				return;
			}
			ClubMemberModel target = club.members.get(req.getTargetAccountId());
			if (null == target) {
				return;
			}
			int type = ClubIgnoreInviteType.TABLE;
			if (req.hasType() && req.getType() > 0) {
				type = req.getType();
			}
			member.addIgnoreInviter(req.getTargetAccountId(),
					System.currentTimeMillis() + (req.hasTime() ? req.getTime() * 1000L : ClubCfg.get().getIgnoreInviteTime()), type);
		});
	}
}
