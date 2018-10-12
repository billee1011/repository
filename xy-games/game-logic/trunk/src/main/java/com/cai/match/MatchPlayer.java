package com.cai.match;

import java.util.ArrayList;
import java.util.List;

import com.cai.ai.RobotPlayer;

public class MatchPlayer extends RobotPlayer{

	/**
	 */
	private static final long serialVersionUID = 1L;
	
	private transient int matchId;
	//当前排行 分数的名次会一样
	private transient int curRank = 1;
	private transient int curIndex = 1;
	
	private transient float roundScore;
	private transient int cardType;

	private transient float curScore;
	
	private transient boolean isOut;
	
	// 是否进入了比赛场
	private transient boolean isEnter;
	//是否线下赛
	private transient boolean isAdminMatch;
	
	//排名
	private transient int winOrder;
	private transient int applyType; //报名方式
	
	private transient boolean isLeave;
	private transient boolean isTimeOut;
	
	// 是否是临时工
	private transient boolean temporary;
	
	//当前matchType的局数
	private transient int matchTypeRound;
	private transient List<MatchRoundRecord> roundRecords = new ArrayList<>();
	
	// 下次匹配时间
	private transient long matchingTime;
	
	private transient long lastOverTime;
	
	private transient boolean isPayAccount;
	private transient boolean isNewAccount;
	private transient boolean isCreateLog;
	
	//管理员分配的桌子索引ID
	private transient boolean isVail; //是否有效玩家
	private transient int allocationId;
	private transient int winNum; //上游次数
	private transient int singleNum; //报听次数
	
	private transient String cheatNickname;
	private transient String cheatHeadIcon;
	
	public int getCurRank() {
		return curRank;
	}

	public void setCurRank(int curRank) {
		this.curRank = curRank;
	}

	public int getCurIndex() {
		return curIndex;
	}

	public void setCurIndex(int curIndex) {
		this.curIndex = curIndex;
	}

	public float getCurScore() {
		return curScore;
	}

	public void setCurScore(float curScore) {
		this.curScore = curScore;
	}

	public boolean isOut() {
		return isOut;
	}

	public void setOut(boolean isOut) {
		this.isOut = isOut;
	}

	public int getCardType() {
		return cardType;
	}

	public void setCardType(int cardType) {
		this.cardType = cardType;
	}

	public float getRoundScore() {
		return roundScore;
	}

	public void setRoundScore(float roundScore) {
		this.roundScore = roundScore;
	}

	public boolean isEnter() {
		return isEnter;
	}

	public void setEnter(boolean isEnter) {
		this.isEnter = isEnter;
	}

	public boolean isAuto() {
		return !this.isEnter || super.isAuto();
	}

	public int getWinOrder() {
		return winOrder;
	}

	public void setWinOrder(int winOrder) {
		this.winOrder = winOrder;
	}

	public boolean isLeave() {
		return isLeave;
	}

	public void setLeave(boolean isLeave) {
		this.isLeave = isLeave;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public int getMatchTypeRound() {
		return matchTypeRound;
	}

	public void setMatchTypeRound(int matchTypeRound) {
		this.matchTypeRound = matchTypeRound;
	}

	public long getMatchingTime() {
		return matchingTime;
	}

	public void setMatchingTime(long matchingTime) {
		this.matchingTime = matchingTime;
	}

	public long getLastOverTime() {
		return lastOverTime;
	}

	public void updateLastOverTime() {
		this.lastOverTime = System.currentTimeMillis();
	}
	public boolean isTimeOut() {
		return isTimeOut;
	}
	public void setTimeOut(boolean isTimeOut) {
		this.isTimeOut = isTimeOut;
	}
	
	public void reset(){
		setChannel(null);
		isOut = true;
		isEnter = false;
		isLeave = true;
	}
	
	public boolean isPayAccount() {
		return isPayAccount;
	}

	public void setPayAccount(boolean isPayAccount) {
		this.isPayAccount = isPayAccount;
	}

	public boolean isNewAccount() {
		return isNewAccount;
	}

	public void setNewAccount(boolean isNewAccount) {
		this.isNewAccount = isNewAccount;
	}
	public boolean isNoSend(){
		if(isOut || isLeave){
			return true;
		}
		return false;
	}
	public int getAllocationId() {
		return allocationId;
	}
	public void setAllocationId(int allocationId) {
		this.allocationId = allocationId;
	}
	public int getWinNum() {
		return winNum;
	}
	public void setWinNum(int winNum) {
		this.winNum = winNum;
	}
	public int getSingleNum() {
		return singleNum;
	}
	public void setSingleNum(int singleNum) {
		this.singleNum = singleNum;
	}
	public boolean isAdminMatch() {
		return isAdminMatch;
	}
	public void setAdminMatch(boolean isAdminMatch) {
		this.isAdminMatch = isAdminMatch;
	}
	public boolean isVail() {
		return isVail;
	}
	public void setVail(boolean isVail) {
		this.isVail = isVail;
	}
	public boolean isCreateLog() {
		return isCreateLog;
	}
	public void setCreateLog(boolean isCreateLog) {
		this.isCreateLog = isCreateLog;
	}
	public int getApplyType() {
		return applyType;
	}
	public void setApplyType(int applyType) {
		this.applyType = applyType;
	}
	public int getMatchId() {
		return matchId;
	}
	public void setMatchId(int matchId) {
		this.matchId = matchId;
	}
	public String getCheatNickname() {
		return cheatNickname != null ? cheatNickname : "";
	}
	public void setCheatNickname(String cheatNickname) {
		this.cheatNickname = cheatNickname;
	}
	public String getCheatHeadIcon() {
		return cheatHeadIcon != null ? cheatHeadIcon : "";
	}
	public void setCheatHeadIcon(String cheatHeadIcon) {
		this.cheatHeadIcon = cheatHeadIcon;
	}
	public int getTopTimes() {
		return getMyTimes();
	}
	public void setTopTimes(int topTimes) {
		if(topTimes < 0){
			topTimes = 1;
		}
		setMyTimes(topTimes);
	}

	public List<MatchRoundRecord> getRoundRecords() {
		return roundRecords;
	}

	public void addRoundRecords(int round, int index, int score) {
		MatchRoundRecord record = new MatchRoundRecord(round, index, score);
		this.roundRecords.add(record);
	}

	public String printInfo(){
		StringBuffer sb = new StringBuffer();
		sb.append("rankIndex:").append(curRank);
		sb.append(" curScore:").append(curScore);
		sb.append(" isLeave:").append(isLeave);
		sb.append(" isTimeOut:").append(isTimeOut);
		return sb.toString();
	}
	
}
