/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.coin;

import java.util.List;

import com.cai.coin.excite.condition.IExciteCondition;

/**
 * @author wu_hc date: 2018/9/4 15:00 <br/>
 */
public final class DataWrap {
	public List<IExciteCondition> cdts;
	public Type type;
	public long accountId;

	public enum Type {

		//特殊玩法
		EXCITE,
		//聚宝盆
		CORNUCOPIA,;
	}
}
