package com.cai.match;

public class MatchRoundRecord {
	
	private int round;
	private int index;
	private int score;
	
	public MatchRoundRecord(int round, int index, int score){
		this.round = round;
		this.index = index;
		this.score = score;
	}
	
	public int getRound() {
		return round;
	}
	public void setRound(int round) {
		this.round = round;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}

}
