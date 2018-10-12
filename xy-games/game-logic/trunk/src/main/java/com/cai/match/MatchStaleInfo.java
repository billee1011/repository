package com.cai.match;

import java.util.Date;

import com.cai.common.util.MyDateUtil;

public class MatchStaleInfo {
	
	private int matchId;
	private int id;
	private Date matchTime;
	
	public MatchStaleInfo(int matchId,int id){
		this.matchId = matchId;
		this.id = id;
		this.matchTime = new Date();
	}

	public int getMatchId() {
		return matchId;
	}

	public int getId() {
		return id;
	}
	
	public boolean isSameDay(){
		if(MyDateUtil.isSameDay(matchTime)){
			return true;
		}
		return false;
	}
}
