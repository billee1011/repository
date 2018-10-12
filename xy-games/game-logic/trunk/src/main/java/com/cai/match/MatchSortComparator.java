package com.cai.match;

import java.util.Comparator;

public class MatchSortComparator implements Comparator<MatchPlayer> {

	@Override
	public int compare(MatchPlayer o1, MatchPlayer o2) {
		int value1 = o1.isLeave() ? 0 : 1;
		if(value1 == 0 && o1.isVail()){
			value1 = 1;
		}
		int value2 = o2.isLeave() ? 0 : 1;
		if(value2 == 0 && o2.isVail()){
			value2 = 1;
		}
		int result = value2 - value1;
		if(result == 0){
			result = (int) (o2.getCurScore() * o2.getTopTimes() - o1.getCurScore() * o1.getTopTimes());
		}
		if(result == 0){
			result = o2.getWinNum() - o1.getWinNum();
		}
		if(result == 0){
			result = o2.getSingleNum() - o1.getSingleNum();
		}
		if(result == 0){
			result = (int) (o2.getAccount_id() - o1.getAccount_id());
		}
		
		if(result > 0){
			return 1;
		}
		if(result < 0){
			return -1;
		}
		return 0;
	}

}
