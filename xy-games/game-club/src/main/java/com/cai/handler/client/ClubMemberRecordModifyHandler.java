package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMemberRecordModifyProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年4月26日 上午11:50:23
 */
@ICmd(code = C2SCmd.MODIFY_CLUB_MEMBER_RECORD, desc = "修改俱乐部玩家记录")
public class ClubMemberRecordModifyHandler extends IClientExHandler<ClubMemberRecordModifyProto> {

	@Override
	protected void execute(ClubMemberRecordModifyProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
				Utils.sendTip(topReq.getAccountId(), "只有管理人员可以进行该操作！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}

			int opeDay = req.getRequestType();
			if (opeDay != 1 && opeDay != 2 && opeDay != 3) { // 只能对今日、昨日、前日记录进行操作
				Utils.sendTip(topReq.getAccountId(), "不能进行该操作！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}
			club.managerModifyMemberRecordModelByDay(req, member, session);
		});
	}

}
