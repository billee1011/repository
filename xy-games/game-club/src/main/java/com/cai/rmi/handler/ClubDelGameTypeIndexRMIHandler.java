package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubDelRuleVO;
import com.cai.service.ClubService;

/**
 * 
 * 
 * @author wu_hc date: 2018年5月22日 下午2:40:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_DELETE_RULE, desc = "下架游戏")
public final class ClubDelGameTypeIndexRMIHandler extends IRMIHandler<ClubDelRuleVO, Boolean> {
	@Override
	protected Boolean execute(ClubDelRuleVO vo) {
		ClubService.getInstance().delClubGameTypeIndex(vo.getGameTypeIndexs());
		return true;
	}
}
