/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.util.PBUtil;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.ClubMsgProto.ClubRecordReqProto;
import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.ClubMsgProto.HonourRecordProto;
import protobuf.clazz.Common.CommonLI;

/**
 * 
 * date: 2018年3月20日 上午1:31:59 <br/>
 */
public class ClubHonourRecordRunnable implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private ClubRequest request;
	private long accountId;

	private long createTime;

	public ClubHonourRecordRunnable(ClubRequest request, long accountId) {
		this.request = request;
		this.accountId = accountId;

		this.createTime = System.currentTimeMillis();
	}

	@Override
	public void run() {

		long now = System.currentTimeMillis();
		long pass = now - createTime;

		C2SSession session = C2SSessionService.getInstance().getSession(accountId);
		if (null == session) {
			return;
		}

		if (pass > 10000) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("查询超时,请稍后重试").build());
			logger.error("Slow ClubHonourRecordRunnable 真正执行时间已经大于10秒........." + pass);
			return;
		}

		try {
			Account account = session.getAccount();
			if (account == null) {
				return;
			}

			int clubId = request.getClubId();
			if (clubId <= 0) {
				return;
			}

			ClubRecordReqProto req = request.getRecordReq();
			if (req.getBeginTime() > req.getEndTime()) {
				return;
			}

			Map<Long, AtomicInteger> winMap = Maps.newHashMap();
			Map<Long, AtomicInteger> timeMap = Maps.newHashMap();

			List<BrandLogModel> brandLogModels = MongoDBServiceImpl.getInstance().getClubParentBrandList(null, clubId, req.getBeginTime(),
					req.getEndTime());

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

			HonourRecordProto.Builder builder = HonourRecordProto.newBuilder();
			// 局数
			for (Map.Entry<Long, AtomicInteger> entry : timeMap.entrySet()) {
				CommonLI.Builder timeRecordsBuilder = CommonLI.newBuilder();
				timeRecordsBuilder.setK(entry.getKey().longValue());
				timeRecordsBuilder.setV(entry.getValue().get());
				builder.addTimeRecords(timeRecordsBuilder);
			}
			// 大赢家
			for (Map.Entry<Long, AtomicInteger> entry : winMap.entrySet()) {
				CommonLI.Builder winRecordsBuilder = CommonLI.newBuilder();
				winRecordsBuilder.setK(entry.getKey().longValue());
				winRecordsBuilder.setV(entry.getValue().get());
				builder.addWinRecords(winRecordsBuilder);
			}
			builder.setClubId(clubId);
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_HONOUR_RECORD, builder));
		} catch (Exception e) {
			logger.error("ClubHonourRecordRunnable error", e);
		}
	}

}
