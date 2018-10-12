package com.cai.coin.excite.condition;

import com.cai.common.define.ETriggerType;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.CoinExciteModel;

/**
 * @author wu_hc date: 2018年08月10日 下午3:00:19 <br/>
 */
public abstract class AbstractExciteCondition implements IExciteCondition {

	/**
	 * 条件Model
	 */
	protected final CoinExciteModel exciteModel;

	/**
	 * 牌型model
	 */
	protected final CardCategoryModel categoryModel;

	/**
	 * 缓存，不多次计算
	 */
	private String cardCategoryCache;

	public AbstractExciteCondition(CoinExciteModel exciteModel, CardCategoryModel categoryModel) {
		this.exciteModel = exciteModel;
		this.categoryModel = categoryModel;
	}

	@Override
	public CoinExciteModel model() {
		return exciteModel;
	}

	@Override
	public CardCategoryModel cardCategoryModel() {
		return categoryModel;
	}

	@Override
	public ETriggerType triggerType() {
		return ETriggerType.of(exciteModel.getTriggerType());
	}

	@Override
	public final String cardCategory() {
		if (null == cardCategoryCache) {
			cardCategoryCache = String.format("%d:%d", categoryModel.getGameId(), toDecimal(categoryModel.getCardTypeValue()));
		}
		return cardCategoryCache;
	}

	@Override
	public long cardTypeValue() {
		return toDecimal(categoryModel.getCardTypeValue());
	}

	@Override
	public int cardCategoryId() {
		return categoryModel.getId();
	}

	@Override
	public int id() {
		return exciteModel.getId();
	}

	/**
	 * 转换成十进制
	 *
	 * @param hexValue
	 * @return
	 */
	static long toDecimal(String hexValue) {
		if (hexValue.startsWith("0x")) {
			return Long.parseLong(hexValue.substring(2), 16);
		} else {
			return Long.parseLong(hexValue);
		}
	}
}
