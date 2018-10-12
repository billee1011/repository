package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AccountMatchRedis;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.CoinPlayerMatchRedis;
import com.cai.common.domain.CoinPlayerRedis;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchOpenType;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubService;
import com.cai.utils.RoomUtil;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import static protobuf.clazz.ClubMsgProto.ClubMatchCommon;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/22 17:12
 */
@ICmd(code = C2SCmd.CLUB_MATCH_START_IMMEDIATE_REQ, desc = "自建赛立即开赛")
public class ClubMatchStartImmediateReqHandler extends IClientExHandler<ClubMatchCommon> {
	@Override
	protected void execute(ClubMatchCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if (wrap == null) {
				return;
			}

			if (wrap.getModel().getStatus() != ClubMatchWrap.ClubMatchStatus.PRE.status()) {
				return;
			}

			String msg = checkCanStart(wrap, club);
			if (!"".equals(msg)) {
				Utils.sendTip(topReq.getAccountId(), msg, ESysMsgType.INCLUDE_ERROR, session);
				return;
			}
			//立即开赛
			wrap.start();
		});
	}

	private String checkCanStart(ClubMatchWrap wrap, Club club) {
		String msg = "";
		//检查是否关闭报名入口
		if (!wrap.isBanEnroll()) {
			msg = "关闭报名入口才可立即开始比赛";
			return msg;
		}

		//报名人数是否符合牌桌对应人数
		int tablePlayerNum = RoomComonUtil.getMaxNumber(wrap.ruleModel().getRuleParams());
		boolean canStart = true;
		int enrollCount = wrap.getEnrollAccountIds().size();
		if (wrap.getModel().getOpenType() == ClubMatchOpenType.TIME_MODE) {
			if (enrollCount <= 0 || enrollCount < wrap.getModel().getMinPlayerCount() || (enrollCount % tablePlayerNum) != 0) {
				canStart = false;
			}
		} else if (wrap.getModel().getOpenType() == ClubMatchOpenType.COUNT_MODE) {
			if (enrollCount <= 0 || enrollCount % tablePlayerNum != 0) {
				canStart = false;
			}
		}
		if (!canStart) {
			msg = "当前报名人数不足或不是牌桌最大人数整数倍，无法开赛";
			return msg;
		}

		//报名玩家在其他房间中
		long accountId = 0;
		for (Long targetId : wrap.getEnrollAccountIds()) {
			accountId = targetId;
			if (!club.members.containsKey(targetId)) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 已经不在俱乐部，立即开赛失败！", club.getClubId(), wrap.id(), targetId);
				canStart = false;
				break;
			}
			int roomId = RoomUtil.getRoomId(targetId);
			if (roomId > 0) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 有房间[{}]，立即开赛失败！", club.getClubId(), wrap.id(), targetId, roomId);
				canStart = false;
				break;
			}
			CoinPlayerMatchRedis redis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.COIN_PLAYER_MATCH_INFO, targetId + "", CoinPlayerMatchRedis.class);
			if (redis != null) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 有金币场游戏正在匹配中，立即开赛失败！", club.getClubId(), wrap.id(), targetId);
				canStart = false;
				break;
			}
			CoinPlayerRedis coinRedis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.COIN_PLAYER_INFO, targetId + "", CoinPlayerRedis.class);
			if (coinRedis != null) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 有未完成的金币场游戏，立即开赛失败！", club.getClubId(), wrap.id(), targetId);
				canStart = false;
				break;
			}
			AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.MATCH_ROOM_ACCOUNT, targetId + "", AccountMatchRedis.class);
			if (accountMatchRedis != null && accountMatchRedis.isStart()) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 已经报名比赛场了，立即开赛失败！", club.getClubId(), wrap.id(), targetId);
				canStart = false;
				break;
			}
		}
		if (!canStart) {
			ClubMemberModel member = club.members.get(accountId);
			if (member == null) {
				msg = "玩家 " + accountId + " 已不在该亲友圈";
			} else {
				msg = "包含 " + member.getNickname() + " 在内的玩家处于其他房间中，无法参与比赛";
			}

			return msg;
		}

		return msg;
	}
}
