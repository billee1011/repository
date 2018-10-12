package com.cai.handler.s2s;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.handler.IServerHandler;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.Global;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.ClubMsgProto.ClubMemberRecord;
import protobuf.clazz.ClubMsgProto.ClubMemberRecordResponse;

/**
 * 
 *
 * @author zhanglong date: 2018年5月4日 下午2:14:50
 */
@IServerCmd(code = S2SCmd.CLUB_MEMBER_RECORD_INFO, desc = "俱乐部玩家记录数据")
public class ClubMemberRecordInfoRspHandler extends IServerHandler<ClubMemberRecordResponse> {

	@Override
	public void execute(ClubMemberRecordResponse resp, S2SSession session) throws Exception {

		C2SSession client = C2SSessionService.getInstance().getSession(resp.getAccountId());
		if (null == client) {
			return;
		}
		if (!GbCdCtrl.canHandleMust(client, Opt.CLUB_HONOUR_RECORD))
			return;
		Global.getService(Global.SERVER_LOGIC).execute(() -> {
			// 为了兼容旧数据,暂时还是通过战绩统计玩家今日、昨日、前日和八日的局数和大赢家数
			int clubId = resp.getClubId();
			int requestType = resp.getRequestType();
			long beginTime = 0;
			long endTime = 0;
			Date now = new Date();
			if (requestType == ClubRecordDayType.TODAY) {// 今天
				beginTime = TimeUtil.getTimeStart(now, 0);
				endTime = now.getTime();
			} else if (requestType == ClubRecordDayType.YESTERDAY) {// 昨天
				beginTime = TimeUtil.getTimeStart(now, -1);
				endTime = TimeUtil.getTimeEnd(now, -1);
			} else if (requestType == ClubRecordDayType.BEFORE_YESTERDAY) {// 前天
				beginTime = TimeUtil.getTimeStart(now, -2);
				endTime = TimeUtil.getTimeEnd(now, -2);
			} else if (requestType == ClubRecordDayType.EIGHT) {// 八天
				beginTime = TimeUtil.getTimeStart(now, -7);
				endTime = now.getTime();
			}
			if (beginTime == 0 || endTime == 0) {
				client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_MEMBER_RECORD_LIST_RSP, resp.toBuilder()));
				return;
			}

			List<BrandLogModel> brandLogModels = null;
			List<ClubMemberRecord> list = resp.getRecordList();
			Map<String, Object> param = null;
			if (resp.getTargetAccountId() > 0) { // 查询的单个玩家的记录
				param = Maps.newHashMap();
				param.put("accountIds", resp.getTargetAccountId());
			}
			brandLogModels = MongoDBServiceImpl.getInstance().getClubParentBrandList(null, clubId, beginTime, endTime, param);
			Map<Long, AtomicInteger> winMap = Maps.newHashMap();
			Map<Long, AtomicInteger> timeMap = Maps.newHashMap();
			if (brandLogModels == null) {
				return;
			}
			for (BrandLogModel branModel : brandLogModels) {
				GameRoomRecord grr = GameRoomRecord.to_Object(branModel.getMsg());

				// 轮数
				for (int i = 0; i < grr.getPlayers().length; i++) {
					Player player = grr.getPlayers()[i];
					if (null == player) {
						continue;
					}
					if (!timeMap.containsKey(player.getAccount_id())) {
						timeMap.put(player.getAccount_id(), new AtomicInteger(1));
					} else {
						timeMap.get(player.getAccount_id()).incrementAndGet();
					}
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
					if (idx < 0 || idx >= grr.getPlayers().length) {
						return;
					}
					Player player = grr.getPlayers()[idx];
					if (null != player) {
						if (!winMap.containsKey(player.getAccount_id())) {
							winMap.put(player.getAccount_id(), new AtomicInteger(1));
						} else {
							winMap.get(player.getAccount_id()).incrementAndGet();
						}
					}
				});
			}
			ClubMemberRecordResponse.Builder builder = ClubMemberRecordResponse.newBuilder();
			builder.setClubId(resp.getClubId());
			for (ClubMemberRecord record : list) {
				ClubMemberRecord.Builder recordBuilder = ClubMemberRecord.newBuilder();
				recordBuilder.setAccountId(record.getAccountId());
				recordBuilder.setIsLike(record.getIsLike());
				recordBuilder.setTireValue(record.getTireValue());
				long accountId = record.getAccountId();
				if (timeMap.containsKey(accountId)) {// 局数
					recordBuilder.setGameTime(timeMap.get(accountId).get());
				}
				if (winMap.containsKey(accountId)) {// 大赢家
					recordBuilder.setWinTime(winMap.get(accountId).get());
				}
				builder.addRecord(recordBuilder);
			}
			client.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_MEMBER_RECORD_LIST_RSP, builder));
		});
	}

}
