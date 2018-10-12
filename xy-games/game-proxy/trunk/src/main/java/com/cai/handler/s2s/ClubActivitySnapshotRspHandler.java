/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ClubActivityLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.RankModel;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.ClubUitl;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.ClubMsgProto.ClubActivityProto;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

/**
 * @author wu_hc date: 2018年1月24日 上午10:36:11 <br/>
 */
@IServerCmd(code = S2SCmd.CLUB_ACTIVITY_SNAPSHOT_BUILD, desc = "俱乐部活动结束，委托给代理服生成活动快照")
public final class ClubActivitySnapshotRspHandler extends IServerHandler<ClubActivityProto> {

	@Override
	public void execute(ClubActivityProto resp, S2SSession session) throws Exception {
		Global.getGameDispatchService().execute(newTask(resp));
	}

	/**
	 * 生成任务
	 *
	 * @param resp
	 * @return
	 */
	public Runnable newTask(final ClubActivityProto resp) {
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
					if (idx > grr.getPlayers().length - 1) {
						System.out.println(String.format("players:%s, idx:%d", grr.getPlayers(), idx));
						System.out.println(branModel);
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
			for (int rank = 0; rank < winRankList.size(); rank++) {
				winRankList.get(rank).setRank(rank + 1);
			}
			for (int rank = 0; rank < timeRankList.size(); rank++) {
				timeRankList.get(rank).setRank(rank + 1);
			}
			ClubActivityLogModel logModel = ClubUitl.activityLogModel(resp, winRankList, timeRankList);
			logModel.setProxyId(SystemConfig.proxy_index);
			MongoDBServiceImpl.getInstance().getLogQueue().add(logModel);
		};
	}
}
