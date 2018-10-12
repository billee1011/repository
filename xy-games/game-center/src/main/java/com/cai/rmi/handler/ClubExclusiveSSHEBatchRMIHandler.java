package com.cai.rmi.handler;

import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.XYCode;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.util.FilterUtil;
import com.cai.service.ClubExclusiveService;
import com.cai.util.ClubExclusiveLogUtil;
import com.cai.util.RMIMsgSender;
import com.google.common.collect.Lists;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_GOLD_SSHE_BATCH, desc = "后台操作")
public final class ClubExclusiveSSHEBatchRMIHandler extends IRMIHandler<List<ClubExclusiveRMIVo>, List<ClubExclusiveResultModel>> {

	@Override
	public List<ClubExclusiveResultModel> execute(List<ClubExclusiveRMIVo> vos) {

		logger.warn("ClubExclusiveSSHERMIHandler 俱乐部服接受到来自后台操作!{}", vos);

		List<ClubExclusiveResultModel> resultModels = Lists.newArrayList();

		vos.forEach(vo -> {
			ClubExclusiveResultModel model = ClubExclusiveService.getInstance().update(vo);
			resultModels.add(model);
			ClubExclusiveLogUtil.exclusiveLog(vo, model, vo.getValue() > 0);
		});

		List<ClubExclusiveResultModel> sucessUpdate = FilterUtil.filter(resultModels, m -> m.getStatus() == XYCode.SUCCESS);
		if (!sucessUpdate.isEmpty()) {
			RMIMsgSender.callClub(RMICmd.CLUB_EXCLUSIVE_GOLD_SSHE_BATCH, sucessUpdate);
		}

		return resultModels;
	}

}
