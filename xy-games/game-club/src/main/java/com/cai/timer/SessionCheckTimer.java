/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.Collection;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.util.SessionUtil;
import com.cai.config.SystemConfig;
import com.cai.service.SessionService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 * 
 *
 * @author wu_hc date: 2017年8月30日 下午8:47:17 <br/>
 */
public final class SessionCheckTimer extends TimerTask {

	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(SessionCheckTimer.class);

	@Override
	public void run() {

		try {
			if (SystemConfig.gameDebug == 1) {
				return;
			}
			long current = System.currentTimeMillis();

			final Collection<C2SSession> sessions = SessionService.getInstance().getAllSession();
			sessions.forEach((s) -> {
				if (current - s.getLastAccessTime() > 60 * 1000L) {
					s.shutdownGracefully();
					logger.warn("连接:[{}],附加信息:{},心跳超时，被强制关闭连接!!", s.channel(), SessionUtil.getAttr(s, AttributeKeyConstans.CLUB_SESSION));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("SessionCheckTimer error ", e);
		}
	}
}
