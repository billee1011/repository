package com.cai.rmi.handler;

import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.ClubExclusiveService;
import com.google.common.collect.Lists;

import protobuf.clazz.ClubMsgProto.ClubExclusiveGoldProto;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.s2s.S2SProto.ExclusiveGoldPB;

/**
 * 
 * 
 * @author wu_hc date: 2017年12月13日 下午5:45:26 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_GOLD_INFO, desc = "俱乐部专属豆详情 ")
public final class ClubExclusiveGoldInfoRMIHandler extends IRMIHandler<Long, ClubExclusiveGoldProto> {

	@Override
	public ClubExclusiveGoldProto execute(Long accountId) {

		if (null == accountId || accountId.longValue() <= 0L) {
			return null;
		}

		List<ExclusiveGoldPB> accountExclusive = ClubExclusiveService.getInstance().accountExclusiveGold(accountId);

		List<CommonILI> accountExclusive_ = Lists.newArrayListWithCapacity(accountExclusive.size());
		accountExclusive.forEach((pb) -> {
			accountExclusive_.add(CommonILI.newBuilder().setK(pb.getGameId()).setV1(pb.getValue()).setV2(pb.getExpireE()).build());
		});

		ClubExclusiveGoldProto.Builder builder = ClubExclusiveGoldProto.newBuilder();
		builder.setAccountId(accountId);
		builder.addAllExclusive(accountExclusive_);
		return builder.build();
	}
}
