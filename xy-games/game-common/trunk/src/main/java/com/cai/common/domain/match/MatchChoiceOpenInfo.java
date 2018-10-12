package com.cai.common.domain.match;

import java.io.Serializable;

public class MatchChoiceOpenInfo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int is_accumulate;		//免费次数是否累积
	private int show_time;			//比赛列表提前几天显示

	public int getIs_accumulate() {
		return is_accumulate;
	}
	public void setIs_accumulate(int is_accumulate) {
		this.is_accumulate = is_accumulate;
	}
	public int getShow_time() {
		return show_time;
	}
	public void setShow_time(int show_time) {
		this.show_time = show_time;
	}

}
