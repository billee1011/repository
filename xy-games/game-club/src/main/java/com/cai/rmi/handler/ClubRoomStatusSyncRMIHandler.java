package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.XYCode;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubRMIVo;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubTable;
import com.cai.service.ClubService;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月26日 下午6:10:52 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_REQ_LOGIC_SYNC_ROOM, desc = "俱乐部请求刷新房间数据")
public final class ClubRoomStatusSyncRMIHandler extends IRMIHandler<ClubRMIVo, Integer> {
	@Override
	protected Integer execute(ClubRMIVo vo) {

		Club club = ClubService.getInstance().getClub(vo.getClubId());
		if (null != club) {
			ClubRuleTable ruleTable = club.ruleTables.get(vo.getRuleId());
			if (null != ruleTable) {
				ClubTable table = ruleTable.getTable(vo.getTableIndex());
				if (null != table) {
					club.runInClubLoop(() -> {
						table.reqLogicSyncRoomStatus();
					});

					return XYCode.SUCCESS;
				}
			}
		}
		return XYCode.FAIL;
	}

}
