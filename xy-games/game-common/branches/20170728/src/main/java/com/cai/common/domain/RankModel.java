/**
 *Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.common.domain;

import java.io.Serializable;

/**
 * 排行榜数据
 * 
 * @author wu_hc
 */
public final class RankModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7903631651949465522L;

	/**
	 * 排名
	 */
	private int rank;

	/**
	 * 头像
	 */
	private String head;

	/**
	 * 昵称
	 */
	private String nickName;

	/**
	 * 帐号ID
	 */
	private long accountId;

	/**
	 * 签名
	 */
	private String signature;

	/**
	 * 值
	 */
	private long value;

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "RankModel [rank=" + rank + ", head=" + head + ", nickName=" + nickName + ", accountId=" + accountId
				+ ", signature=" + signature + ", value=" + value + "]";
	}

}
