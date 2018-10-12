package com.lingyu.common.template.gen;

import com.lingyu.common.template.Attribute;
import com.lingyu.common.template.CSVAnalysis;

@CSVAnalysis(res = "fan2jifen.csv")
public abstract class Fan2JifenTemplateGen {

	@Attribute("fan")
	private int fan;

	/** 字段ID */
	public int getFan() {
		return fan;
	}

	@Attribute("jifen")
	private int jifen;

	/** 字段数值 */
	public int getJifen() {
		return jifen;
	}
}