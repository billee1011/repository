package com.cai.constant;

import java.util.List;

import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/7 10:29
 */
public class ClubMatchTable {

	private int roomId;

	private List<Long> players;

	private int curRound;

	private boolean isEnd;

	private boolean isDisband;

	public ClubMatchTable(int roomId, List<Long> players) {
		this.roomId = roomId;
		this.players = players;
	}

	public int getCurRound() {
		return curRound;
	}

	public void setCurRound(int curRound) {
		this.curRound = curRound;
	}

	public List<Long> getPlayers() {
		return players;
	}

	public void setPlayers(List<Long> players) {
		this.players = players;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public void setEnd(boolean end) {
		isEnd = end;
	}

	public void release(String msg) {
		if (this.roomId > 0) {
			SpringService.getBean(ICenterRMIServer.class).delRoomById(roomId, msg);
		}
	}

	public boolean isDisband() {
		return isDisband;
	}

	public void setDisband(boolean disband) {
		isDisband = disband;
	}
}
