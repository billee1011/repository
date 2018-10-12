/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.service.InviteRedpacketService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.activity.InviteRedpacketProto.ActivityRuleResp;
import protobuf.clazz.activity.InviteRedpacketProto.GetInviteRedpacketResp;
import protobuf.clazz.activity.InviteRedpacketProto.InviteRecordResp;
import protobuf.clazz.activity.InviteRedpacketProto.InviteRedpacketReq;
import protobuf.clazz.activity.InviteRedpacketProto.RankListInviteRedpacketResp;


@ICmd(code = C2SCmd.INVITE_REDPACKET_ACTIVITY, desc = "邀请红包活动")
@SuppressWarnings("unused")
public final class InviteRedpacketHandler extends IClientHandler<InviteRedpacketReq> {

	private static final Logger logger = LoggerFactory.getLogger(InviteRedpacketHandler.class);
	private static final int MY_INVITE_REDPACKET = 1; //我的红包
	private static final int RANK_DATA  = 2;		  // 排行榜数据
	private static final int INVITE_RECORD = 3;		  // 邀请记录
	private static final int INVITE_ACTIVITY  = 4;    // 红包活动，包括奖品展示、规则说明
	
	@Override
	protected void execute(InviteRedpacketReq req, Request topRequest, C2SSession session) throws Exception {
		int paramType = req.getParamType();
		long accountId = session.getAccountID();
		switch (paramType) {
		case MY_INVITE_REDPACKET:
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.MY_INVITE_REDPACKET, processGetInviteRedpacketResp(accountId)));
			break;
		case RANK_DATA:
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.RANK_DATA, processRankListInviteRedpacketResp(accountId)));
			break;
		case INVITE_RECORD:
			int curPage = req.getCurPage();
			int pageSize = req.getPageSize();
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.INVITE_RECORD, processInviteRecordResp(accountId,curPage,pageSize)));
			break;
		case INVITE_ACTIVITY:
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.INVITE_ACTIVITY, processGetActivityRuleResp(accountId)));
			break;
		default:
			break;
		}
	}
	public GetInviteRedpacketResp processGetInviteRedpacketResp(long accountId){
		return InviteRedpacketService.getInstance().processGetInviteRedpacketResp(accountId);
	}
	public RankListInviteRedpacketResp processRankListInviteRedpacketResp(long accountId){
		return  InviteRedpacketService.getInstance().processRankListInviteRedpacketResp(accountId);
	}
	public InviteRecordResp processInviteRecordResp(long accountId,int curPage,int pageSize){
		return  InviteRedpacketService.getInstance().processInviteRecordResp(accountId, curPage, pageSize);
	}
	public ActivityRuleResp processGetActivityRuleResp(long accountId){
		return  InviteRedpacketService.getInstance().processGetActivityRuleResp(accountId);
	}
}
