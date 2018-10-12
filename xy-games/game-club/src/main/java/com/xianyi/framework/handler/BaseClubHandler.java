/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.xianyi.framework.handler;

import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2018年3月10日 上午9:36:40 <br/>
 */
public abstract class BaseClubHandler<T extends GeneratedMessage> extends IClientExHandler<GeneratedMessage> {

	@SuppressWarnings("unchecked")
	@Override
	protected void execute(GeneratedMessage req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(0);
		club.runInReqLoop(() -> {
			execute(club, (T) (req), topReq, session);
		});
	}

	abstract void execute(Club club, T req, TransmitProto topReq, C2SSession session);
}
