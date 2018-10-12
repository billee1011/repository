/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.c2s.C2SProto.ClubUpvoteProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 权限控制
 * 
 * @author wu_hc date: 2017年12月28日 上午11:18:47 <br/>
 */
@ICmd(code = C2SCmd.CLUB_RECORD_UPVOTE, desc = "俱乐部战绩点赞")
public final class ClubRecordUpvoteHandler extends IClientExHandler<ClubUpvoteProto> {

	@Override
	protected void execute(ClubUpvoteProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topReq.getAccountId(), "只有管理人员可进行点赞操作！", ESysMsgType.INCLUDE_ERROR)));
				return;
			}

			session.send(PBUtil.toS2SResponse(S2SCmd.CLUB_RECORD_UPVOTE, req.toBuilder().setAccountId(topReq.getAccountId())));
		});
	}
}
