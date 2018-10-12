package com.lingyu.common.template.gen;

import com.lingyu.common.template.Attribute;
import com.lingyu.common.template.CSVAnalysis;

@CSVAnalysis(res = "fan.csv")
public abstract class FanTemplateGen {

	@Attribute("id")
	private int id;
	
	/** id*/
	public int getId() {
		return id;
	}
	
	@Attribute("fan")
	private int fan;
	
	/** 翻的倍数*/
	public int getFan() {
		return fan;
	}
}