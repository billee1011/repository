package com.lingyu.common.template.gen;

import com.lingyu.common.template.Attribute;
import com.lingyu.common.template.CSVAnalysis;

@CSVAnalysis(res = "Config.csv")
public abstract class ConfigTemplateGen {
	@Attribute("comment")
	private String comment;

	/** 字段描述 */
	public String getComment() {
		return comment;
	}

	@Attribute("type")
	private int type;

	/** 字段ID */
	public int getType() {
		return type;
	}

	@Attribute("value")
	private String value;

	/** 字段数值 */
	public String getValue() {
		return value;
	}
}