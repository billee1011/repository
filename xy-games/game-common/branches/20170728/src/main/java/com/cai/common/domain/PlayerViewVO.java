/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.common.domain;

import java.io.Serializable;

/**
 * 查看玩家数据，和PlayerViewResponse对应
 * 
 * @author wu_hc
 */
public final class PlayerViewVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4003714280868217585L;

	/**
	 * 帐号id
	 */
	private long accountId;

	/**
	 * 头像
	 */
	private String head;

	/**
	 * 昵称
	 */
	private String nickName;

	/**
	 * 闲逸豆
	 */
	private long gold;

	/**
	 * 金币
	 */
	private long money;

	/**
	 * 签名
	 */
	private String signature;

	/**
	 * 性别1男2女
	 */
	private byte sex;

	/**
	 * vip等级
	 */
	private int vipLv;

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

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

	public long getGold() {
		return gold;
	}

	public void setGold(long gold) {
		this.gold = gold;
	}

	public long getMoney() {
		return money;
	}

	public void setMoney(long money) {
		this.money = money;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public byte getSex() {
		return sex;
	}

	public void setSex(byte sex) {
		this.sex = sex;
	}

	public int getVipLv() {
		return vipLv;
	}

	public void setVipLv(int vipLv) {
		this.vipLv = vipLv;
	}

	@Override
	public String toString() {
		return "PlayerViewVO [accountId=" + accountId + ", head=" + head + ", nickName=" + nickName + ", gold=" + gold
				+ ", money=" + money + ", signature=" + signature + ", sex=" + sex + ", vipLv=" + vipLv + "]";
	}

}
