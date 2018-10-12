/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Optional;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EPlayerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.constant.ClubSeat;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.s2s.S2SProto.PlayerStatusProto;

/**
 * 
 * @author wu_hc date: 2017年10月18日 上午11:28:35 <br/>
 */
@ICmd(code = S2SCmd.PLAYER_STATUS, desc = "玩家上下线状态变更")
public class PlayerStatusHandler extends IClientHandler<PlayerStatusProto> {

	@Override
	protected void execute(PlayerStatusProto message, C2SSession session) throws Exception {

		Pair<EServerType, Integer> serverInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
		if (null == serverInfo) {
			return;
		}

		SessionService.getInstance().statusUpate(message.getAccountId(), EPlayerStatus.status(message.getStatus()), serverInfo.getSecond());
		ClubService.getInstance().playerStatus(message.getAccountId(), EPlayerStatus.status(message.getStatus()));

		if (message.getStatus() == EPlayerStatus.OFFLINE.status()) {
			PlayerService.getInstance().exit(message.getAccountId());

			Optional<ClubSeat> seatOpt = ClubCacheService.getInstance().seat(message.getAccountId());
			if (seatOpt.isPresent() && seatOpt.get().getJoinId() == ClubSeat.INVAL_ID) {
				ClubCacheService.getInstance().sit(message.getAccountId(), ClubService.currentSeat);
			}
		}
	}
}
