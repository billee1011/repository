package com.lingyu.common.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MahjongResultData {
	private Date startTime; // 对战开始时间
	private List<MahjongResultDetailsData> infos = new ArrayList<>(); // 玩家积分的详情
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public List<MahjongResultDetailsData> getInfos() {
		return infos;
	}
	public void setInfos(List<MahjongResultDetailsData> infos) {
		this.infos = infos;
	}
}
