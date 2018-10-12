package com.cai.coin.excite.condition;

import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.CoinExciteModel;

/**
 * 牌性条件
 *
 * @author wu_hc date: 2018年08月10日 下午3:00:19 <br/>
 */
public final class TypeExciteCondition extends AbstractExciteCondition implements IExciteCondition {

	public TypeExciteCondition(CoinExciteModel exciteModel, CardCategoryModel categoryModel) {
		super(exciteModel, categoryModel);
	}

	@Override
	public boolean isDone(int currentValue) {
		return currentValue >= exciteModel.getVar1() && currentValue <= exciteModel.getVar2();
	}
}
