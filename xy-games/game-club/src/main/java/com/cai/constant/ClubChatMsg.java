/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

/**
 * 俱乐部聊天
 * 
 *
 * @author wu_hc date: 2018年1月5日 上午10:34:41 <br/>
 */
public final class ClubChatMsg {

	private int type;
	private long accountId;
	private String content;
	private long uniqueId;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(long uniqueId) {
		this.uniqueId = uniqueId;
	}

}
