/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.S2SCmd;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto;

/**
 * 
 */
@ICmd(code = S2SCmd.CLUB_GAME_RECORD_REQ, desc = "俱乐部游戏结束")
public final class ClubGameRecordHandler extends IClientHandler<ClubGameRecordProto> {

	@Override
	public void execute(ClubGameRecordProto req, C2SSession session) throws Exception {

		switch (req.getType()) {
		case CLUB_GAME_OVER: // 俱乐部成员统计
			ClubService.getInstance().roomPlayerLog(req.getGameOver(), req.getKouDou().getCreateTime());
			break;
		case CLUB_KOU_DOU: // 俱乐部统计
			ClubService.getInstance().roomKouDou(req);
			break;
		default:
			break;
		}

	}
}
