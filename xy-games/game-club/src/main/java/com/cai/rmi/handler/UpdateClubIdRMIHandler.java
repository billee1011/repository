package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.UpdateClubIdVo;
import com.cai.service.ClubService;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月25日 上午11:58:53 <br/>
 */
@IRmi(cmd = RMICmd.UPDATE_CLUB_ID, desc = "修改俱乐部id")
public final class UpdateClubIdRMIHandler extends IRMIHandler<UpdateClubIdVo, UpdateClubIdVo> {

	@Override
	public UpdateClubIdVo execute(final UpdateClubIdVo vo) {
		logger.warn("来自后台的修改俱乐部id操作:{}", vo);
		ClubService.getInstance().updateClubId(vo);
		logger.warn("来自后台的修改俱乐部id结果:{}", vo);

		return vo;
	}
}
