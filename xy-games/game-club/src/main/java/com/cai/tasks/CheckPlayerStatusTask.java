/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.cai.common.ClubTableKickOutType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubPlayer;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubTable;
import com.cai.service.ClubService;
import com.cai.utils.Utils;

import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.s2s.ClubServerProto.ProxyClubRq;

/**
 * @author wu_hc date: 2018/8/31 16:53 <br/>
 */
public final class CheckPlayerStatusTask extends AbstractClubTask {

	private final Club club;
	private final long current;

	public CheckPlayerStatusTask(Club club, long current) {
		this.club = club;
		this.current = current;
	}

	@Override
	protected void exe() {
		ConcurrentMap<Integer, ClubRuleTable> ruleTables = club.ruleTables;

		ruleTables.forEach((ruleId, ruleTable) -> {
			for (ClubTable table : ruleTable.getTables()) {
				if (table.isGameStart()) {
					continue;
				}
				checkTable(ruleId, table);
			}
		});
	}

	private void checkTable(int ruleId, final ClubTable table) {
		Map<Long, ClubPlayer> playerMap = table.getPlayers();
		ClubRequest.Builder builder = ClubRequest.newBuilder().setClubId(club.getClubId());
		ProxyClubRq.Builder proxyClubRq = ProxyClubRq.newBuilder().setClientSessionId(club.getOwnerId());
		String msg = "由于您在牌桌内长时间未准备，已暂时将您请离牌桌";
		playerMap.forEach((accountId, player) -> {
			if (!player.isReady()) {
				ClubMemberModel memberModel = club.members.get(player.getAccountId());
				if (memberModel != null && (this.current - memberModel.getLastEnterTableTime()
						> ClubCfg.get().getAutoKickoutPlayerTime() * TimeUtil.SECOND)) {
					builder.setType(ClubRequest.ClubRequestType.CLUB_KICK_PLAYER);
					builder.setAccountId(player.getAccountId());
					builder.setJoinId(table.sceneTag().getJoinId());
					builder.setClubRuleId(ruleId);
					proxyClubRq.setClubRq(builder);
					ClubRoomModel status = ClubService.getInstance().clubKickPlayer(builder.build(), proxyClubRq.build(), ClubTableKickOutType.AUTO_KICK);
					if (status.getStatus() == Club.SUCCESS) {
						memberModel.setLastAutoKickTime(this.current);
						Utils.sendTip(player.getAccountId(), msg, ESysMsgType.NONE);
					}
				}
			}
		});
	}

}
