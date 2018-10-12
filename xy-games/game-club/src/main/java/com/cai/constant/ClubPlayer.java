/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import protobuf.clazz.ClubMsgProto.ClubTablePlayerProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月30日 下午5:12:24 <br/>
 */
public final class ClubPlayer {

	// opts
	public static final int OP_ALL = 0;

	public static final int OP_USERNAME = 1 << 0;
	public static final int OP_ICON = 1 << 1;
	public static final int OP_SEX = 1 << 2;
	public static final int OP_SEAT = 1 << 3;
	public static final int OP_READY = 1 << 4;
	public static final int OP_GOLD = 1 << 5;
	public static final int OP_MONEY = 1 << 6;
	public static final int OP_JOINID = 1 << 7;

	// public static final int OP_ALL = OP_USERNAME | OP_ICON | OP_SEX | OP_SEAT
	// | OP_READY | OP_GOLD | OP_GOLD | OP_MONEY | OP_JOINID;
	private final long accountId;
	private final String userName;
	private String icon;
	private int sex;
	private int seatIndex;
	private boolean isReady;
	private long gold;
	private long money;
	private int joinId;
	private String ip;

	/**
	 * 
	 * @param pb
	 * @return
	 */
	public static ClubPlayer create(ClubTablePlayerProto pb) {
		ClubPlayer player = new ClubPlayer(pb.getAccountId(), pb.getUserName());
		return player.assign(pb);

	}

	/**
	 *
	 * @param pb
	 * @return
	 */
	public ClubPlayer assign(ClubTablePlayerProto pb) {
		ClubPlayer player = this;
		player.setGold(pb.getGold());
		player.setIcon(pb.getHeadImgUrl());
		player.setMoney(pb.getMoney());
		player.setReady(pb.getReady());
		player.setSeatIndex(pb.getSeatIndex());
		player.setSex(pb.getSex());
		player.setJoinId(pb.getClubJoinId());
		player.setIp(pb.getIp());
		return player;
	}

	/**
	 * 
	 * @return
	 */
	public ClubTablePlayerProto toPbBuilder() {
		ClubTablePlayerProto.Builder builder = ClubTablePlayerProto.newBuilder();
		builder.setAccountId(this.accountId);
		builder.setClubJoinId(joinId);
		builder.setGold(this.gold);
		builder.setHeadImgUrl(icon);
		builder.setMoney(money);
		builder.setReady(isReady);
		builder.setSeatIndex(seatIndex);
		builder.setSex(sex);
		builder.setUserName(userName);
		builder.setIp(ip);
		return builder.build();
	}

	/**
	 * 
	 * @param interestOps
	 * @return
	 */
	public ClubTablePlayerProto toInteresPbBuilder(int interestOps) {
		if (interestOps == OP_ALL) {
			return toPbBuilder();
		}
		ClubTablePlayerProto.Builder builder = ClubTablePlayerProto.newBuilder();
		builder.setAccountId(this.accountId);

		if ((interestOps & OP_USERNAME) != 0) {
			builder.setUserName(userName);
		}
		if ((interestOps & OP_ICON) != 0) {
			builder.setHeadImgUrl(icon);
		}
		if ((interestOps & OP_SEX) != 0) {
			builder.setSex(sex);
		}
		if ((interestOps & OP_SEAT) != 0) {
			builder.setSeatIndex(seatIndex);
		}
		if ((interestOps & OP_READY) != 0) {
			builder.setReady(isReady);
		}
		if ((interestOps & OP_GOLD) != 0) {
			builder.setGold(this.gold);
		}
		if ((interestOps & OP_MONEY) != 0) {
			builder.setMoney(money);
		}
		if ((interestOps & OP_JOINID) != 0) {
			builder.setClubJoinId(joinId);
		}
		return builder.build();
	}

	/**
	 * 
	 * @param accountId
	 * @param userName
	 */
	private ClubPlayer(long accountId, String userName) {
		this.accountId = accountId;
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public int getSeatIndex() {
		return seatIndex;
	}

	public void setSeatIndex(int seatIndex) {
		this.seatIndex = seatIndex;
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}

	public long getAccountId() {
		return accountId;
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

	public int getJoinId() {
		return joinId;
	}

	public void setJoinId(int joinId) {
		this.joinId = joinId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	public String toString() {
		return "ClubPlayer [accountId=" + accountId + ", userName=" + userName + ", icon=" + icon + ", sex=" + sex + ", seatIndex=" + seatIndex
				+ ", isReady=" + isReady + ", gold=" + gold + ", money=" + money + ", joinId=" + joinId + ", ip=" + ip + "]";
	}

	public static void main(String[] args) {
		ClubPlayer player = new ClubPlayer(232, "vincent");
		player.setJoinId(2323);
		player.setIcon("2345");
		player.toInteresPbBuilder(ClubPlayer.OP_ICON | ClubPlayer.OP_JOINID);

		System.out.println(1 << 0);
	}
}
