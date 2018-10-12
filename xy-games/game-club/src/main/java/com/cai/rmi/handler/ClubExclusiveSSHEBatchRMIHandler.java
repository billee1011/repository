package com.cai.rmi.handler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static java.util.stream.Collectors.groupingBy;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.utils.Utils;
import com.google.common.collect.Lists;

import protobuf.clazz.Common.CommonILI;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_GOLD_SSHE_BATCH, desc = "后台操作通知俱乐部")
public final class ClubExclusiveSSHEBatchRMIHandler extends IRMIHandler<List<ClubExclusiveResultModel>, Void> {

	@Override
	public Void execute(List<ClubExclusiveResultModel> ms) {

		logger.warn("ClubExclusiveSSHERMIHandler 俱乐部服接受到来自后台操作!{}", ms);
		
		Function<List<ClubExclusiveResultModel>, List<CommonILI>> func = (ms_) -> {
			List<CommonILI> r = Lists.newArrayList();
			ms_.forEach((vo) -> {
				int expire = (int) (vo.getExclusiveEndDate().getTime() / 1000L);
				r.add(CommonILI.newBuilder().setK(vo.getGameId()).setV1(vo.getNewValue()).setV2(expire).build());
			});
			return r;
		};

		Map<Long, List<ClubExclusiveResultModel>> map = ms.stream().collect(groupingBy(ClubExclusiveResultModel::getAccountId));
		map.forEach((accountId, rmiVos) -> {
			Utils.sendExclusiveGoldUpdate(accountId, func.apply(rmiVos));
		});
		return null;
	}

}
