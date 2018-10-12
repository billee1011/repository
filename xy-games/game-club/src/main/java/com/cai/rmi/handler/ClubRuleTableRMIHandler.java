package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.XYCode;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubRMIVo;
import com.cai.common.rmi.vo.CommonBytesRMIVo;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.service.ClubService;

import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月14日 上午10:33:01 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_RULE_CACHE_TABLE, desc = "查看包间的桌子信息")
public final class ClubRuleTableRMIHandler extends IRMIHandler<ClubRMIVo, CommonBytesRMIVo> {
	@Override
	protected CommonBytesRMIVo execute(ClubRMIVo vo) {

		logger.warn("俱乐部服接受到来自后台查看俱乐部桌子信息请求: {}", vo);

		int clubId = vo.getClubId();

		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return CommonBytesRMIVo.newVo(XYCode.FAIL, String.format("俱乐部[%d]不存在!", clubId));
		}
		int ruleId = vo.getRuleId();
		ClubRuleTable ruleTables = club.ruleTables.get(ruleId);
		if (null == ruleTables) {
			return CommonBytesRMIVo.newVo(XYCode.FAIL, String.format("不存在包间[%d,%d]!", clubId, ruleId));
		}
		ClubRuleTableGroupProto.Builder builder = ruleTables.toTablesBuilder(clubId);
		return CommonBytesRMIVo.newVo(XYCode.SUCCESS, builder.build().toByteArray(), "-");
	}
}
