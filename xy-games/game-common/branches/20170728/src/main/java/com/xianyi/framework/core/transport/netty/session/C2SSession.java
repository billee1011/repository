/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.core.transport.netty.session;

import com.cai.common.domain.Account;
import com.xianyi.framework.core.transport.Session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * 客户端与服务器间的会话
 * 
 * @author wu_hc
 */
public final class C2SSession extends AbstractNettySession implements Session {

	/**
	 * 
	 */
	public static final AttributeKey<Integer> SESSION_LOGIC_ID = AttributeKey.valueOf("SESSION_LOGIC_ID");

	/**
	 * 
	 */
	private Account account;

	/**
	 * @param channel
	 */
	public C2SSession(Channel channel) {
		super(channel);
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public long getAccountID() {
		return null == account ? 0L : account.getAccount_id();
	}

	@Override
	protected int timeOut() {
		return 10 * 1000;
	}

	@Override
	protected void sessionException(SessionException exception) {
		if (SessionException.FREQUENT == exception) {
			log.warn("玩家 [{}]访问频率过高，请确认!", account);
		} else if (SessionException.SEND_ERR == exception) {
			log.warn("玩家 [{}]发送数据失败，请确认!", account);
		}
	}

	/**
	 * @param currentTimeMillis
	 */
	public void setAccountLoginTime(long currentTimeMillis) {
	}

}
