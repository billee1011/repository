package com.lingyu.common.template.gen;

import com.lingyu.common.template.Attribute;
import com.lingyu.common.template.CSVAnalysis;

@CSVAnalysis(res = "jifen.csv")
public abstract class JiFenTemplateGen {

	@Attribute("signType")
	private int signType;

	@Attribute("flag")
	private boolean flag;

	@Attribute("jifen")
	private int jifen;

	@Attribute("yu")
	private boolean yu;

	@Attribute("fan")
	private int fan;

	/** 标签类型 */
	public int getSignType() {
		return signType;
	}

	/** 是否收取3家的积分 */
	public boolean isFlag() {
		return flag;
	}

	/** 是否要加下的鱼 */
	public boolean isYu() {
		return yu;
	}

	/** 加减积分值（给赢的人加分，输的人减分） */
	public int getJifen() {
		return jifen;
	}

	/** 操作对应的番薯 */
	public int getFan() {
		return fan;
	}

	@Attribute("gang")
	private boolean gang;

	/** 是否扣除杠分 */
	public boolean isGang() {
		return gang;
	}
}