/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubActivityCode;
import com.cai.constant.ClubActivityWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubDelActProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2018年1月23日 上午9:58:19 <br/>
 */
@ICmd(code = C2SCmd.CLUB_DEL_ACTIVITY, desc = "删除俱乐部活动")
public final class ClubDelActivityHandler extends IClientExHandler<ClubDelActProto> {

	@Override
	protected void execute(ClubDelActProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {

			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
				Utils.sendTip(topReq.getAccountId(), "不是成员或者权限不足！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}

			ClubActivityWrap wrap = null;
			if (null != (wrap = club.activitys.remove(req.getActivityId()))) {
				SpringService.getBean(ClubDaoService.class).getDao().deleteClubActivity(wrap.getId(), club.getClubId());
				Utils.notifyActivityEvent(topReq.getAccountId(), club, wrap.getId(), ClubActivityCode.DEL);
				wrap.cancelSchule();
			}
		});
	}
}
