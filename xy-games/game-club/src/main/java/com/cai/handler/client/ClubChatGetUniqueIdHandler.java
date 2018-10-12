package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubChatUniqueIdResponse;
import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年5月29日 上午10:46:51
 */
@ICmd(code = C2SCmd.CLUB_CHAT_GET_UNIQUE_ID_REQ, desc = "获取俱乐部聊天唯一Id")
public class ClubChatGetUniqueIdHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			long uniqueId = club.getChatUniqueId();
			ClubChatUniqueIdResponse.Builder builder = ClubChatUniqueIdResponse.newBuilder();
			builder.setClubId(club.getClubId());
			builder.setUniqueId(uniqueId);
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CHAT_UNIQUE_ID_RSP, builder));
		});
	}

}
