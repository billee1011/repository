/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.Optional;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.ClubIgnoreInviteType;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubSeat;
import com.cai.constant.ClubTable;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubAccountProto;
import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubInviteJoinTableProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月12日 上午10:52:23 <br/>
 */
@ICmd(code = C2SCmd.CLUB_INVITE_JOIN_TABLE, desc = "亲友圈邀请加入桌子")
public final class ClubInviteJoinTableHandler extends IClientExHandler<ClubInviteJoinTableProto> {

	@Override
	protected void execute(ClubInviteJoinTableProto req, TransmitProto topReq, C2SSession session) throws Exception {

		final ClubCommon common = req.getClubCommon();
		if (null == common) {
			return;
		}

		Club club = ClubService.getInstance().getClub(common.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {

			if (!club.members.containsKey(topReq.getAccountId())) {
//				Utils.sendTip(topReq.getAccountId(), "您不是亲友圈成员!", ESysMsgType.NONE, session);
				return;
			}

			// 被邀请者
			ClubAccountProto account = req.getAccount();
			final long targetAccountId = account.getAccountId();
			ClubMemberModel target = club.members.get(targetAccountId);
			if (null == target || target.isIgnoreInvite(topReq.getAccountId(), ClubIgnoreInviteType.TABLE)) {
				return;
			}

			ClubRuleTable ruleTable = club.ruleTables.get(common.getRuleId());
			if (null == ruleTable) {
//				Utils.sendTip(topReq.getAccountId(), "包间不存在，或者已经被管理员删除！", ESysMsgType.NONE, session);
				return;
			}
			int tableIndex = (common.getJoinId() & 0xffff0000) >> 16;
			ClubTable table = ruleTable.getTable(tableIndex);
			if (null == table) {
//				Utils.sendTip(topReq.getAccountId(), String.format("桌子编号[%d]不存在！", tableIndex), ESysMsgType.NONE, session);
				return;
			}

			if (table.playerSize() >= ruleTable.getPlayerLimit() || table.isGameStart()) {
//				Utils.sendTip(topReq.getAccountId(), "桌子已经满员或者开局，不可邀请！", ESysMsgType.NONE, session);
				return;
			}
			
			Optional<ClubSeat> seatOpt = ClubCacheService.getInstance().seat(targetAccountId);
			if (seatOpt.isPresent() && seatOpt.get().isOnSeat()) {
				return;
			}
			
//			Utils.sendTip(topReq.getAccountId(), "邀请消息已发送！", ESysMsgType.NONE, session);

			SessionService.getInstance().sendClient(targetAccountId, S2CCmd.CLUB_INVITE_JOIN_TABLE, req.toBuilder().setAccount(req.getAccount()
					.toBuilder().setAccountId(topReq.getAccountId()).setAvatar(target.getAvatar()).setNickname(target.getNickname())));
		});
	}
}
