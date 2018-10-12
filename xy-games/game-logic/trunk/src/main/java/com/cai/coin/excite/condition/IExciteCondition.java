package com.cai.coin.excite.condition;

import com.cai.common.define.ETriggerType;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.CoinExciteModel;

/**
 * @author wu_hc date: 2018年08月10日 下午3:00:19 <br/>
 */
public interface IExciteCondition {

	/**
	 * @return
	 */
	int id();

	/**
	 * 是否达成
	 *
	 * @return
	 */
	boolean isDone(int currentValue);

	/**
	 * @return
	 */
	CoinExciteModel model();

	/**
	 * 对应的牌型model
	 *
	 * @return
	 */
	CardCategoryModel cardCategoryModel();

	/**
	 * 触发类型
	 *
	 * @return
	 */
	ETriggerType triggerType();

	/**
	 * 牌型类型[gameId:cardTypeValue]
	 *
	 * @return
	 */
	String cardCategory();

	/**
	 * 牌值
	 *
	 * @return
	 */
	long cardTypeValue();

	/**
	 * 牌型ID
	 *
	 * @return
	 */
	int cardCategoryId();
}
