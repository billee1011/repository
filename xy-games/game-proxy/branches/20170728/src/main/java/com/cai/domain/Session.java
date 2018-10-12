package com.cai.domain;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Account;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.concurrent.WorkerLoop;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.DefaultAttributeMap;
import protobuf.clazz.Protocol.Response;

public class Session extends DefaultAttributeMap implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(Session.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 8196240430631276557L;

	/**
	 * 构造
	 */
	public Session() {
		createTime = System.currentTimeMillis();
	}

	/**
	 * 编号
	 */
	private long sessionId;

	/**
	 * 创建时间
	 */
	private long createTime;

	/**
	 * 账号登录时间
	 */
	private long accountLoginTime;

	/**
	 * 每个会话所承载的网络会话实例
	 */
	private transient Channel channel;

	/**
	 * 帐号ID
	 */
	private long accountID;

	/**
	 * 帐号名
	 */
	private String account_name;

	/**
	 * 玩家编号
	 */
	private int userID;

	/**
	 * 最近一次会话刷新时间
	 */
	private long refreshTime;

	/**
	 * 客户端IP
	 */
	private String clientIP;

	private Account account;

	private final ReentrantLock mainLock = new ReentrantLock();

	private WorkerLoop woker;
	/**
	 * 参数
	 */
	private Map<String, Object> parameterMap = Maps.newConcurrentMap();

	// ======单个sessin全局参数===========max 300/10s
	/**
	 * ip的链接频率(每N秒最高链接次数)
	 */
	private static final int SESSION_LINK_HZ = 300;

	/**
	 * 检测频率(毫秒)
	 */
	private static final long SESSION_HZ_MS = 10000L;

	/**
	 * 最后刷新时间
	 */
	private long lastHzFlushTime = 0L;

	private long hzLinkTimes = 0L;
	//////////

	/**
	 * 检测全局请求频率
	 * 
	 * @return
	 */
	public boolean checkHz() {
		long nowTime = System.currentTimeMillis();
		if (nowTime - lastHzFlushTime > SESSION_HZ_MS) {
			lastHzFlushTime = nowTime;
			hzLinkTimes = 1;
		} else {
			hzLinkTimes++;
		}
		if (hzLinkTimes > SESSION_LINK_HZ) {
			StringBuilder buf = new StringBuilder();
			buf.append("检测到session请求频率过高,hzLinkTimes=" + hzLinkTimes);
			if (account != null) {
				buf.append(",account_id=" + account.getAccount_id());
			}
			logger.error(buf.toString());

			return false;
		} else {
			return true;
		}
	}

	/**
	 * 单个请求频率控制
	 * 
	 * @param requestType
	 * @param millisecond
	 * @return
	 */
	public synchronized boolean isCanRequest(String requestType, long millisecond) {

		// try {
		// Object object = parameterMap.get(requestType);
		// long now = System.currentTimeMillis();
		// if (object != null) {
		// Long time = (Long) object;
		// if (now - time > millisecond) {
		// parameterMap.put(requestType, now);
		// return true;
		// } else {
		// return false;
		// }
		// } else {
		// parameterMap.put(requestType, now);
		// return true;
		// }
		// } catch (Exception e) {
		// logger.error("error", e);
		// return true;
		// }

		return true;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public long getAccountID() {
		return accountID;
	}

	public void setAccountID(long accountID) {
		this.accountID = accountID;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public long getRefreshTime() {
		return refreshTime;
	}

	public void setRefreshTime(long refreshTime) {
		this.refreshTime = refreshTime;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}

	public ReentrantLock getMainLock() {
		return mainLock;
	}

	public String getAccount_name() {
		return account_name;
	}

	public void setAccount_name(String account_name) {
		this.account_name = account_name;
	}

	public long getAccountLoginTime() {
		return accountLoginTime;
	}

	public void setAccountLoginTime(long accountLoginTime) {
		this.accountLoginTime = accountLoginTime;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		if (account.getRedisLock() == null)
			account.setRedisLock(new ReentrantLock());
		this.account = account;
	}

	public Map<String, Object> getParameterMap() {
		return parameterMap;
	}

	public void setParameterMap(Map<String, Object> parameterMap) {
		this.parameterMap = parameterMap;
	}

	/**
	 * 回复数据
	 * 
	 * @param message
	 */
	public void send(final Response response) {
		if (null == channel) {
			logger.warn("代理服====>>>客户端，消息传输失败[ 链接为空] ！");
			return;
		}
//		System.err.println(response);
		ChannelFuture futrue = channel.writeAndFlush(response);
		futrue.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					logger.warn("代理服====>>>客户端，消息传输失败,responseType:{}", response.getResponseType());
				}
			}
		});
	}

	public WorkerLoop getWoker() {
		return woker;
	}

	public void setWoker(WorkerLoop woker) {
		this.woker = woker;
	}

}
