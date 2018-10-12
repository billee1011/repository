/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubSimple;
import protobuf.clazz.c2s.C2SProto.EmptyReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@ICmd(code = C2SCmd.CLUB_MINI_INFO, desc = "俱乐部列表，基本信息")
public final class ClubMiniReqHandler extends IClientExHandler<EmptyReq> {

	@Override
	protected void execute(EmptyReq req, TransmitProto topReq, C2SSession session) throws Exception {
		ClubSimple.Builder builder = ClubService.getInstance().encodeSimpleClubs(topReq.getAccountId(), true);
		session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MINI_INFO, builder));
	}
}
