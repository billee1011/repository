package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IClubRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubAndAccountVo;
import com.cai.common.util.SpringService;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_AND_ACCOUNT, desc = "查玩家是否在俱乐部里")
public final class ClubHasAccountRMIHandler extends IRMIHandler<ClubAndAccountVo, ClubAndAccountVo> {

	@Override
	public ClubAndAccountVo execute(ClubAndAccountVo vo) {
		return SpringService.getBean(IClubRMIServer.class).rmiInvoke(RMICmd.CLUB_AND_ACCOUNT, vo);
	}
}
