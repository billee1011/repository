/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EClubActivityCategory;
import com.cai.common.define.ELifecycle;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ClubActivityLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.RankModel;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.ClubUitl;
import com.cai.common.util.PBUtil;
import com.cai.core.Global;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.ClubMsgProto.ClubActivityProto;
import protobuf.clazz.ClubMsgProto.ClubActivityRankProto;
import protobuf.clazz.s2s.ClubServerProto.ClubActivityTransfort;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

/**
 * @author wu_hc date: 2018年1月24日 上午10:36:11 <br/>
 */
@IServerCmd(code = S2SCmd.CLUB_ACTIVITY_RANK_RSP, desc = "俱乐部排行榜")
public final class ClubActivityRankRspToClientHandler extends IServerHandler<ClubActivityTransfort> {

	@Override
	public void execute(ClubActivityTransfort resp, S2SSession session) throws Exception {
		if (null == C2SSessionService.getInstance().getSession(resp.getAccountId())) {
			return;
		}

		if (resp.getStatus() == ELifecycle.ING.status()) {
			Global.getGameDispatchService().execute(newIngTask(resp.getActivityProto(), resp.getAccountId()));
		} else if (resp.getStatus() == ELifecycle.AFTER.status()) {
			Global.getGameDispatchService().execute(newEndTask(resp.getActivityProto(), resp.getAccountId()));
		}
	}

	/**
	 * 生成任务[进行中]
	 *
	 * @param resp
	 * @return
	 */
	private Runnable newIngTask(final ClubActivityProto resp, long accountId) {
		return () -> {
			int clubId = resp.getClubId();
			if (clubId <= 0) {
				return;
			}

			int activityType = resp.getActivityType();
			if (activityType <= 0 || activityType >= 3) {
				return;
			}
			long st = resp.getStartDate() * 1000L, et = resp.getEndDate() * 1000L;

			Map<Long, RankModel> winMap = Maps.newHashMap();
			Map<Long, RankModel> timeMap = Maps.newHashMap();

			List<BrandLogModel> brandLogModels = MongoDBServiceImpl.getInstance().getClubParentBrandList(null, clubId, st, et);

			for (BrandLogModel branModel : brandLogModels) {
				GameRoomRecord grr = GameRoomRecord.to_Object(branModel.getMsg());

				// 轮数
				for (int i = 0; i < grr.getPlayers().length; i++) {
					Player player = grr.getPlayers()[i];
					if (null == player) {
						continue;
					}
					RankModel timeRankModel = timeMap.get(player.getAccount_id());
					if (null == timeRankModel) {
						timeRankModel = new RankModel();
						timeRankModel.setAccountId(player.getAccount_id());
						timeRankModel.setHead(player.getAccount_icon());
						timeRankModel.setNickName(player.getNick_name());
						timeRankModel.setValue(0L);
						timeMap.put(player.getAccount_id(), timeRankModel);
					}
					timeRankModel.setValue(timeRankModel.getValue() + 1);
				}

				// 大赢家
				int bigWinIndex = 0;
				float scoreTmp = 0.0f;
				for (int i = 0; i < grr.get_player().game_score.length; i++) {
					if (grr.get_player().game_score[i] > scoreTmp) {
						scoreTmp = grr.get_player().game_score[i];
						bigWinIndex = i;
					}
				}

				// 多大赢家
				Set<Integer> bigWinIdxSet = Sets.newHashSet(bigWinIndex);
				for (int i = 0; i < grr.get_player().game_score.length; i++) {
					if (grr.get_player().game_score[i] == scoreTmp) {
						bigWinIdxSet.add(i);
					}
				}
				bigWinIdxSet.forEach((idx) -> {
					if (idx <0 || idx >= grr.getPlayers().length) {
						return;
					}
					Player player = grr.getPlayers()[idx];
					if (null != player) {
						RankModel winRankModel = winMap.get(player.getAccount_id());
						if (null == winRankModel) {
							winRankModel = new RankModel();
							winRankModel.setAccountId(player.getAccount_id());
							winRankModel.setHead(player.getAccount_icon());
							winRankModel.setNickName(player.getNick_name());
							winRankModel.setValue(0);
							winMap.put(player.getAccount_id(), winRankModel);
						}
						winRankModel.setValue(winRankModel.getValue() + 1);
					}
				});

			}

			//
			List<RankModel> winRankList = winMap.values().stream().sorted(comparingLong(RankModel::getValue).reversed()).limit(50).collect(toList());
			List<RankModel> timeRankList = timeMap.values().stream().sorted(comparingLong(RankModel::getValue).reversed()).limit(50)
					.collect(toList());
			ClubActivityRankProto.Builder builder = ClubActivityRankProto.newBuilder();
			builder.setClubId(clubId);
			builder.setActivityId(resp.getActivityId());
			for (int rank = 0; rank < winRankList.size(); rank++) {
				winRankList.get(rank).setRank(rank);

			}
			for (int rank = 0; rank < timeRankList.size(); rank++) {
				timeRankList.get(rank).setRank(rank);
			}

			if (resp.getActivityType() == EClubActivityCategory.GAME_TIME.category()) {
				builder.addAllRankInfo(ClubUitl.toRankModelListProto(timeRankList));
			} else {
				builder.addAllRankInfo(ClubUitl.toRankModelListProto(winRankList));
			}

			C2SSession client = C2SSessionService.getInstance().getSession(accountId);
			if (null != client) {
				client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_ACTIVITY_RANK, builder));
			}
		};
	}

	/**
	 * 生成任务[结束后]
	 *
	 * @param resp
	 * @return
	 */
	private Runnable newEndTask(final ClubActivityProto req, final long accountId) {
		return () -> {
			int clubId = req.getClubId();
			if (clubId <= 0) {
				return;
			}

			// 避免玩家中途下线，服务器多做了无用功
			if (null == C2SSessionService.getInstance().getSession(accountId)) {
				return;
			}

			ClubActivityRankProto.Builder builder = ClubActivityRankProto.newBuilder();
			builder.setClubId(clubId);
			builder.setActivityId(req.getActivityId());

			C2SSession client = C2SSessionService.getInstance().getSession(accountId);

			ClubActivityLogModel activityLogModel = MongoDBServiceImpl.getInstance().searchClubActivityModel(clubId, req.getActivityId());
			if (null == activityLogModel) {
				client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_ACTIVITY_RANK, builder));
				return;
			}

			EClubActivityCategory category = EClubActivityCategory.of(activityLogModel.getActivityType());
			if (EClubActivityCategory.NONE == category) {
				return;
			}

			builder.setActivityId(req.getActivityId());

			if (EClubActivityCategory.GAME_TIME == category) {
				builder.addAllRankInfo(ClubUitl.toRankModelListProto(ClubUitl.rankJsonToList(activityLogModel.getTimeRankJson())));
			} else {
				builder.addAllRankInfo(ClubUitl.toRankModelListProto(ClubUitl.rankJsonToList(activityLogModel.getWinRankJson())));
			}

			client = C2SSessionService.getInstance().getSession(accountId);
			if (null != client) {
				client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_ACTIVITY_RANK, builder));
			}
		};
	}
}
