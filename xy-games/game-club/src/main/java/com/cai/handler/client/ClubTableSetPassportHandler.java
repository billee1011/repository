package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.tasks.db.ClubRuleDBTask;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubSetTablePassportResponse;
import protobuf.clazz.ClubMsgProto.ClubTableSetPassportProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年5月25日 下午4:23:32
 */
@ICmd(code = C2SCmd.CLUB_TABLE_SET_PASSPORT, desc = "俱乐部桌子设置密码")
public class ClubTableSetPassportHandler extends IClientExHandler<ClubTableSetPassportProto> {

	@Override
	protected void execute(ClubTableSetPassportProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator) {
				return;
			}
			if (!EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			boolean isSet = req.getIsSet();
			int passport = 0;
			if (isSet) {
				passport = req.getPassport();
				if (passport < 10 || passport > 999999) {
					Utils.sendTip(topReq.getAccountId(), "输入的数字不在范围内!", ESysMsgType.NONE, session);
					return;
				}
			}
			int ruleId = req.getRuleId();
			int tableIndex = req.getTableIndex();
			ClubRuleTable clubRuleTable = club.ruleTables.get(ruleId);
			if (null == clubRuleTable) {
				return;
			}
			clubRuleTable.setTablePassport(tableIndex, passport);
			club.runInClubLoop(new ClubRuleDBTask(clubRuleTable.getClubRuleModel()));

			String desc = "密码设置成功！";
			if (!isSet) {
				desc = "密码取消成功！";
			}
			Utils.sendTip(topReq.getAccountId(), desc, ESysMsgType.INCLUDE_ERROR, session);

			ClubSetTablePassportResponse.Builder builder = ClubSetTablePassportResponse.newBuilder();
			builder.setClubId(club.getClubId());
			builder.setRuleId(ruleId);
			builder.setTableIndex(tableIndex);
			builder.setPassportStatus((passport > 0));
			Utils.sendClubAllMembers(builder, S2CCmd.CLUB_SET_TABLE_PASSPORT_RSP, club);
		});
	}

}
