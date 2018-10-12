package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.config.ClubCfg;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubBanSwitchResponse;
import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月21日 下午3:56:34
 */
@ICmd(code = C2SCmd.CLUB_BAN_SWITCH_REQ, desc = "请求俱乐部后台开关状态")
public class ClubBanSwitchReqHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		ClubBanSwitchResponse.Builder builder = ClubBanSwitchResponse.newBuilder();
		builder.setCloseChat(ClubCfg.get().isBanChat());
		builder.setCloseBulletin(ClubCfg.get().isBanBulletin());
		builder.setCloseMarquee(ClubCfg.get().isBanMarquee());
		builder.setDefendCheating(ClubCfg.get().isDefendCheating());
		session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_BAN_SWITCH_RSP, builder));
	}

}
