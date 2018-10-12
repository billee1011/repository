package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubTable;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubTableBrandIdProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/31 11:27
 */
@ICmd(code = C2SCmd.CLUB_TABLE_BRAND_ID_REQ, desc = "亲友圈牌桌牌局id请求")
public class ClubTableBrandIdReqHandler extends IClientExHandler<ClubTableBrandIdProto> {
	@Override
	protected void execute(ClubTableBrandIdProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
			if (null == ruleTables) {
				return;
			}
			ClubTable table = ruleTables.getTable(req.getTableIndex());
			if (table == null) {
				return;
			}
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_TABLE_BRAND_ID_RSP,
					req.toBuilder().setBrandId(String.valueOf(table.getBrandId()))));
		});
	}
}
