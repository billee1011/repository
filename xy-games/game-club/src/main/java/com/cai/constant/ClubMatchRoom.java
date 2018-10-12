/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.List;

/**
 * 
 * 俱乐部自建赛桌子
 * 
 * @author wu_hc date: 2018年7月20日 上午10:57:08 <br/>
 */
public final class ClubMatchRoom {

	private final int roomId;
	private final List<Long> accountIds;
	private final int serverIndex;
	private boolean isEnd;

	/**
	 * @param roomId
	 * @param accountIds
	 */
	public ClubMatchRoom(int roomId, List<Long> accountIds, int serverIndex) {
		this.roomId = roomId;
		this.accountIds = accountIds;
		this.serverIndex = serverIndex;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public void setEnd(boolean isEnd) {
		this.isEnd = isEnd;
	}

	public int getRoomId() {
		return roomId;
	}

	public List<Long> getAccountIds() {
		return accountIds;
	}

	public int getServerIndex() {
		return serverIndex;
	}

}
