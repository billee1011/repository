package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;
import protobuf.clazz.ClubMsgProto.ClubRuleTableProto;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/5 10:21
 */
@ICmd(code = C2SCmd.CLUB_RULE_TABLE_DATA_REQ, desc = "亲友圈所有包间数据请求")
public class ClubRuleTableDataReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel memberModel = club.members.get(topReq.getAccountId());
			if (memberModel == null) {
				return;
			}
			ClubRuleTableProto.Builder builder = ClubRuleTableProto.newBuilder();
			if (req.hasRuleId()) {
				ClubRuleTable ruleTable = club.ruleTables.get(req.getRuleId());
				if (ruleTable != null) {
					ClubRuleTableGroupProto.Builder b = ruleTable.toTablesBuilder(club.getClubId());
					builder.addRuleTables(b);
					builder.setTotalPackageCount(1);
					builder.setPackageIndex(1);
				}
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_RULE_TABLE_DATA_RSP, builder));
			} else {
				int size = club.ruleTables.size();
				int index = 1;
				for (ClubRuleTable ruleTable : club.ruleTables.values()) {
					builder.clearRuleTables();
					ClubRuleTableGroupProto.Builder b = ruleTable.toTablesBuilder(club.getClubId());
					builder.addRuleTables(b);
					builder.setTotalPackageCount(size);
					builder.setPackageIndex(index++);
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_RULE_TABLE_DATA_RSP, builder));
				}
			}
		});
	}
}
