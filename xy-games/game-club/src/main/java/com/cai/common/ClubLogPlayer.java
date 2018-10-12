package com.cai.common;

import java.util.Date;

public class ClubLogPlayer {

	/**
	 * 玩家Id
	 */
	private long accountId;
	/**
	 * 玩家注册时间
	 */
	private Date registerDate;

	/**
	 * 玩家首次加入俱乐部时间
	 */
	private Date firstJoinClubDate;

	public ClubLogPlayer() {

	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public Date getRegisterDate() {
		return registerDate;
	}

	public void setRegisterDate(Date registerDate) {
		this.registerDate = registerDate;
	}

	public Date getFirstJoinClubDate() {
		return firstJoinClubDate;
	}

	public void setFirstJoinClubDate(Date firstJoinClubDate) {
		this.firstJoinClubDate = firstJoinClubDate;
	}
}
