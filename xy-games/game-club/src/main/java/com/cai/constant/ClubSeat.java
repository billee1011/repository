/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

/**
 * 俱乐部中的座位
 * 
 *
 * @author wu_hc date: 2018年1月5日 上午10:34:41 <br/>
 */
public final class ClubSeat {

	/**
	 * 无效id
	 */
	public static final int INVAL_ID = -1;

	private final int clubId;
	private final int ruleId;
	private final int joinId;

	public static ClubSeat newSeat(int clubId, int ruleId, int joinId) {
		return new ClubSeat(clubId, ruleId, joinId);
	}

	public static ClubSeat newSeat(int clubId, int ruleId) {
		return new ClubSeat(clubId, ruleId, INVAL_ID);
	}

	public static ClubSeat newSeat(int clubId) {
		return new ClubSeat(clubId, INVAL_ID, INVAL_ID);
	}

	public static ClubSeat newSeat() {
		return new ClubSeat(INVAL_ID, INVAL_ID, INVAL_ID);
	}

	private ClubSeat(int clubId, int ruleId, int joinId) {
		this.clubId = clubId;
		this.ruleId = ruleId;
		this.joinId = joinId;
	}

	public boolean eq(ClubSeat seat) {
		if (null == seat) {
			return false;
		}
		return (this == seat) || (seat.clubId == clubId && seat.ruleId == ruleId && seat.joinId == joinId);
	}

	public static boolean eq(ClubSeat seat1, ClubSeat seat2) {
		return (null != seat1 && seat1.eq(seat2));
	}

	public int getClubId() {
		return clubId;
	}

	public int getRuleId() {
		return ruleId;
	}

	public int getJoinId() {
		return joinId;
	}

	public int getSeatIndex() {
		return joinId & 0x0000ffff;
	}

	public int getTableIndex() {
		return (joinId & 0xffff0000) >> 16;
	}

	public boolean isOnSeat() {
		return joinId != INVAL_ID;
	}
}
