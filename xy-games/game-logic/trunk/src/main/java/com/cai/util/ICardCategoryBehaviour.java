/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.util;

import com.cai.common.define.ETriggerType;

/**
 * @author wu_hc date: 2018年08月16日 下午3:00:19 <br/>
 */
public interface ICardCategoryBehaviour {

	/**
	 * @param triggerType   触发式时间节点
	 * @param cardTypeValue 牌型值
	 * @param value         值
	 */
	void triggerEvent(ETriggerType triggerType, long cardTypeValue, int value);

	/**
	 * 上报牌值，起手牌，结束牌
	 *
	 * @param triggerType
	 * @param cardArray
	 */
	default void triggerCardEvent(ETriggerType triggerType, int[] cardArray) {
	}

	/**
	 * 上报事件结束
	 *
	 * @param triggerType
	 */
	default void triggerEventOver(ETriggerType triggerType) {
	}
}
