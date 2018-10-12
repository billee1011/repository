/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.s2s.ClubServerProto.ProxyClubRq;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年1月12日 下午8:54:46 <br/>
 */
public class ClubBaseHandler extends IClientHandler<ProxyClubRq> {

	@Override
	protected void execute(final ProxyClubRq req, final C2SSession session) throws Exception {

		final ClubRequest clubReq = req.getClubRq();
		final Club club = ClubService.getInstance().getClub(clubReq.getClubId());
		if (null != club) {
			club.runInClubLoop(new Runnable() {
				@Override
				public void run() {
					try {
						doExecute(req, session);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			doExecute(req, session);
		}

	}

	/**
	 * 
	 * @param req
	 * @param session
	 * @throws Exception
	 */
	protected void doExecute(final ProxyClubRq req, final C2SSession session) throws Exception {
	};
}
