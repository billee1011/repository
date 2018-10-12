package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.XYCode;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.service.ClubExclusiveService;
import com.cai.util.ClubExclusiveLogUtil;

import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.s2s.S2SProto.ExclusiveGoldPB;

/**
 * 
 * 
 * @author wu_hc date: 2017年12月13日 下午5:45:26 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_COST, desc = "俱乐部专属豆处理")
public final class ClubExclusiveGoldCostRMIHandler extends IRMIHandler<ClubExclusiveRMIVo, AddGoldResultModel> {

	@Override
	public AddGoldResultModel execute(ClubExclusiveRMIVo vo) {
		long accountId = vo.getAccountId();
		long gold = vo.getValue();
		int gameId = vo.getGameId();
		AddGoldResultModel model = new AddGoldResultModel();

		if (accountId <= 0 || gold <= 0 || gameId <= 0) {
			model.setSuccess(false);
			model.setMsg("参数不合法!");
			return model;
		}
		ClubExclusiveResultModel r = ClubExclusiveService.getInstance().cost(vo);
		if (r.getStatus() == XYCode.SUCCESS) {
			model.setSuccess(true);
			ExclusiveGoldPB exclusiveGold = ClubExclusiveService.getInstance().accountExclusiveGold(accountId, gameId);
			if (null != exclusiveGold) {

				model.setAttament(CommonILI.newBuilder().setK(exclusiveGold.getGameId()).setV1(exclusiveGold.getValue())
						.setV2(exclusiveGold.getExpireE()).build());
			}

			ClubExclusiveLogUtil.exclusiveLog(vo, r, false);
		} else {
			model.setSuccess(false);
		}

		return model;
	}

}
