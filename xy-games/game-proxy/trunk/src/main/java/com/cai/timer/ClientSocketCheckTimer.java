package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.core.SystemConfig;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 * 客户端socket链接检测
 * 
 * @author run
 *
 */
public class ClientSocketCheckTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(ClientSocketCheckTimer.class);

	/**
	 * 已经完成登陆的连接超时
	 */
	private static final long ACCOUNT_SESSION_TIMEOUT = 1000 * 120/* 30L */;

	/**
	 * 未登陆的连接超时时间
	 */
	private static final long SESSION_TIMEOUT = 1000 * 60L;

	@Override
	public void run() {
		
		// 扫描当前所有链接，处理超过10s没有通信的(客户端心跳是5s)
		if (SystemConfig.gameDebug == 1)
			return;

		// 1
		checkUnLoginSession();

		// 2
		checkLoginedSession();
	}

	/**
	 * 未进行登陆的session检测
	 */
	private void checkUnLoginSession() {

		long nowTime = System.currentTimeMillis();
		for (C2SSession session : C2SSessionService.getInstance().getAllSession()) {
			if (session.getAccountID() != 0L) {
				continue;
			}
			if (nowTime - session.getLastAccessTime() > SESSION_TIMEOUT) {
				try {
					session.shutdownGracefully();
				} catch (Exception e) {
					logger.error("checkUnLoginSession error", e);
				}
			}
		}
	}

	/**
	 * 检测已经登陆的会话是否超时
	 */
	private void checkLoginedSession() {
		long nowTime = System.currentTimeMillis();
		for (C2SSession session : C2SSessionService.getInstance().getAllOnlieSession()) {
			if (nowTime - session.getLastAccessTime() > ACCOUNT_SESSION_TIMEOUT) {
				try {
					C2SSessionService.getInstance().offline(session);
					session.shutdownGracefully();
				} catch (Exception e) {
					logger.error("checkLoginedSession error", e);
				}
			}
		}
	}
}
