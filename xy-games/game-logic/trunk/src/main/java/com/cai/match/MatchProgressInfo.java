package com.cai.match;

import com.cai.common.domain.json.MatchBaseScoreJsonModel;

public class MatchProgressInfo {
	
	private int type; // 1.打立出局 2.定局赛 3.瑞士移位
	private int curRound; // 当前第几轮
	private int startCount; // 开始人数
	private int stopCount; //停止人数(打立出局)
	private int riseCount; // 晋级人数
	private int nextBili; //带分比例
	private int nextScore; //下一场分数
	
	private int round; //轮数
	private int round_num; //牌局数
	private int base_times; //倍数
	private int base_num; //基数
	private int base_score; //底分
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getCurRound() {
		return curRound;
	}
	public void setCurRound(int curRound) {
		this.curRound = curRound;
	}
	public int getStartCount() {
		return startCount;
	}
	public void setStartCount(int startCount) {
		this.startCount = startCount;
	}
	public int getStopCount() {
		return stopCount;
	}
	public void setStopCount(int stopCount) {
		this.stopCount = stopCount;
	}
	public int getRiseCount() {
		return riseCount;
	}
	public void setRiseCount(int riseCount) {
		this.riseCount = riseCount;
	}
	public int getRound() {
		return round;
	}
	public void setRound(int round) {
		this.round = round;
	}
	public int getRound_num() {
		return round_num;
	}
	public void setRound_num(int round_num) {
		this.round_num = round_num;
	}
	public int getBase_times() {
		return base_times;
	}
	public void setBase_times(int base_times) {
		this.base_times = base_times;
	}
	public int getBase_num() {
		return base_num;
	}
	public void setBase_num(int base_num) {
		this.base_num = base_num;
	}
	public int getBase_score() {
		return base_score;
	}
	public void setBase_score(int base_score) {
		this.base_score = base_score;
	}
	public int getNextBili() {
		return nextBili;
	}
	public int getNextScore() {
		return nextScore;
	}
	public void setNextScore(int nextScore) {
		this.nextScore = nextScore;
	}
	public void setNextBili(int nextBili) {
		this.nextBili = nextBili;
	}
	public MatchBaseScoreJsonModel getMatchBase(){
		MatchBaseScoreJsonModel outBase = new MatchBaseScoreJsonModel();
		outBase.setBase(base_num);
		outBase.setBaseScore(base_score);
		outBase.setTimes(base_times);
		
		return outBase;
	}

}
