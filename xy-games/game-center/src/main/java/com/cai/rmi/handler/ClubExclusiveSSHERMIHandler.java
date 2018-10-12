package com.cai.rmi.handler;

import java.util.Arrays;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.XYCode;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.service.ClubExclusiveService;
import com.cai.util.ClubExclusiveLogUtil;
import com.cai.util.RMIMsgSender;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_GOLD_SSHE, desc = "后台操作")
public final class ClubExclusiveSSHERMIHandler extends IRMIHandler<ClubExclusiveRMIVo, ClubExclusiveResultModel> {

	@Override
	public ClubExclusiveResultModel execute(ClubExclusiveRMIVo vo) {

		logger.warn("ClubExclusiveSSHERMIHandler 俱乐部服接受到来自后台操作!{}", vo);

		ClubExclusiveResultModel model = ClubExclusiveService.getInstance().update(vo);
		if (model.getStatus() == XYCode.SUCCESS) {
			RMIMsgSender.callClub(RMICmd.CLUB_EXCLUSIVE_GOLD_SSHE_BATCH, Arrays.asList(model));
		}
		ClubExclusiveLogUtil.exclusiveLog(vo, model, vo.getValue() > 0);
		return model;
	}

}
