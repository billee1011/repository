/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubActivityCategory;
import com.cai.common.domain.ClubActivityLogModel;
import com.cai.common.util.ClubUitl;
import com.cai.common.util.PBUtil;
import com.cai.core.Global;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.ClubActRankProto;
import protobuf.clazz.ClubMsgProto.ClubActivityRankProto;
import protobuf.clazz.Protocol.Request;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年1月24日 上午11:44:13 <br/>
 */
@ICmd(code = C2SCmd.CLUB_ACTIVITY_RANK_SNAPSHOT, desc = "历史(已经结束)活动排名快照")
public final class ClubActivityRankSnapshotHandler extends IClientHandler<ClubActRankProto> {

	@Override
	protected void execute(ClubActRankProto req, Request topRequest, C2SSession session) throws Exception {

		if (null == session.getAccount()) {
			return;
		}
		Global.getGameDispatchService().execute(newTask(req, session.getAccountID()));
	}

	/**
	 * 生成任务
	 * 
	 * @param resp
	 * @return
	 */
	private Runnable newTask(final ClubActRankProto req, final long accountId) {
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
