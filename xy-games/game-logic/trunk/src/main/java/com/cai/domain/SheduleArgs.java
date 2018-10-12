/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.domain;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 
 *
 * @author wu_hc date: 2018年4月25日 下午5:40:26 <br/>
 */
public final class SheduleArgs {

	/**
	 * 调度器id
	 */
	private int timerId;

	/**
	 * 参数
	 */
	private Map<Object, Object> param;

	public static final SheduleArgs newArgs() {
		return new SheduleArgs();
	}

	private SheduleArgs() {
	}

	public int getTimerId() {
		return timerId;
	}

	public SheduleArgs setTimerId(int timerId) {
		this.timerId = timerId;
		return this;
	}

	public SheduleArgs set(Object key, Object value) {
		if (null == param)
			param = Maps.newHashMapWithExpectedSize(4);

		param.put(key, value);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Object key) {
		return (T) (null == param ? null : param.get(key));
	}
}
