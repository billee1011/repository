package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubTable;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubTableDisbandInRoomResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * @author zhanglong date: 2018年7月19日 下午12:02:53
 */
@ICmd(code = C2SCmd.CLUB_TABLE_DISBAND_IN_ROOM_REQ, desc = "亲友圈桌子房间内解散请求")
public class ClubTableDisbandInRoomReqHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		ClubTableDisbandInRoomResponse.Builder builder = ClubTableDisbandInRoomResponse.newBuilder();
		builder.setClubId(req.getClubId());
		if (club == null) {
			builder.setRet(1);
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_TABLE_DISBAND_IN_ROOM_RSP, builder));
			return;
		}
		club.runInClubLoop(() -> {
			ClubRuleTable clubRuleTable = club.ruleTables.get(req.getRuleId());
			if (null == clubRuleTable) {
				builder.setRet(2);
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_TABLE_DISBAND_IN_ROOM_RSP, builder));
				return;
			}

			int tableIndex = (req.getJoinId() & 0xffff0000) >> 16;
			ClubTable table = clubRuleTable.getTable(tableIndex);
			if (null == table || table.getRoomId() <= 0 || !table.playerIds().contains(topReq.getAccountId())) {
				builder.setRet(3);
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_TABLE_DISBAND_IN_ROOM_RSP, builder));
				return;
			}

			builder.setOnlyManager(club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_DISBAND_ROOM));
			builder.setIsManager(EClubIdentity.isManager(club.getIdentify(topReq.getAccountId()).identify()));
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_TABLE_DISBAND_IN_ROOM_RSP, builder));
		});
	}

}
