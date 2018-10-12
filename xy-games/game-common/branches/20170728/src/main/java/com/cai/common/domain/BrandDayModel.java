package com.cai.common.domain;

import java.io.Serializable;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="brand_day_model")
public class BrandDayModel implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4693038570025786426L;
	
	
	/**
	 * 日期
	 */
	@Indexed(name = "index_notes_date")
	private Integer notes_date;
	
	private int mjtype;
	
	private String mjName;
	
	private int eight;
	
	private int sixteen;
	
	private int total;
	
	private int four;
	
	private int twenty_four;
	
	public int getTwenty_four() {
		return twenty_four;
	}

	public void setTwenty_four(int twenty_four) {
		this.twenty_four = twenty_four;
	}

	public int getFour() {
		return four;
	}

	public void setFour(int four) {
		this.four = four;
	}

	private int allTotal;

	public Integer getNotes_date() {
		return notes_date;
	}

	public void setNotes_date(Integer notes_date) {
		this.notes_date = notes_date;
	}

	public String getMjName() {
		return mjName;
	}

	public void setMjName(String mjName) {
		this.mjName = mjName;
	}

	public int getEight() {
		return eight;
	}

	public void setEight(int eight) {
		this.eight = eight;
	}

	public int getSixteen() {
		return sixteen;
	}

	public void setSixteen(int sixteen) {
		this.sixteen = sixteen;
	}

	public int getTotal() {
		return sixteen+eight;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getMjtype() {
		return mjtype;
	}

	public void setMjtype(int mjtype) {
		this.mjtype = mjtype;
	}

	public int getAllTotal() {
		return allTotal;
	}

	public void setAllTotal(int allTotal) {
		this.allTotal = allTotal;
	}

}
