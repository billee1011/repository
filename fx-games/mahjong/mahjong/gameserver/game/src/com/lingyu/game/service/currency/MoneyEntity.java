package com.lingyu.game.service.currency;

import java.util.ArrayList;
import java.util.List;

import com.lingyu.common.constant.CurrencyConstant.CurrencyCostType;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.util.StringUtil;

/**
 * 货币变化类
 * 
 * @author WCM
 * 
 */
public class MoneyEntity {

	/** 货币类型 */
	private CurrencyType currencyType;

	/** 货币变化数量 */
	private long amount;

	/** 货币消耗类型 */
	private CurrencyCostType costType;

	// ----------------------------------------------------------------------------
	/**
	 * 货币消耗实体类
	 * 
	 * @param currencyType 货币类型
	 * @param amount 货币变化数量
	 * @param costType 货币消耗类型
	 */
	public MoneyEntity(CurrencyType currencyType, long amount, CurrencyCostType costType) {
		this.currencyType = currencyType;
		this.amount = amount;
		this.costType = costType;
	}

	/**
	 * 货币消耗实体类
	 * 
	 * @param currencyType 货币类型
	 * @param amount 货币变化数量
	 * @param costType 货币消耗类型
	 */
	public MoneyEntity(CurrencyType currencyType, long amount) {
		this.currencyType = currencyType;
		this.amount = amount;
		this.costType = CurrencyCostType.ONLY;
	}

	// ----------------------------------------------------------------------------
	/** 货币类型 */
	public CurrencyType getCurrencyType() {
		return currencyType;
	}

	/** 变化数量 */
	public long getAmount() {
		return amount;
	}

	/** 货币消耗类型 */
	public CurrencyCostType getCostType() {
		return costType;
	}

	// ----------------------------------------------------------------------------
	/**
	 * 解析封装 货币消耗实体类
	 * 
	 * @param moneyCostEntityStr 待解析的字段
	 * @return null 没有货币信息
	 */
	public static List<MoneyEntity> initMoneyCostEntity(String moneyCostEntityStr) {
		if (StringUtil.isEmpty(moneyCostEntityStr)) {
			return null;
		}

		List<MoneyEntity> entityList = new ArrayList<>();

		String[] moneyCostEntityArray = moneyCostEntityStr.split(";");
		try {
			for (String moneyCostEntityPartStr : moneyCostEntityArray) {
				MoneyEntity moneyCostEntity = initOnlyMoneyCostEntity(moneyCostEntityPartStr);
				if (moneyCostEntity != null) {
					entityList.add(moneyCostEntity);
				}
			}
		} catch (Exception e) {
			throw new ServiceException("货币配置信息错误[{}]:", moneyCostEntityStr);
		}

		return entityList;
	}

	/**
	 * 解析封装 货币消耗实体类
	 * 
	 * @param moneyCostEntityStr 待解析的字段
	 * @return null 没有货币信息
	 */
	public static MoneyEntity initOnlyMoneyCostEntity(String moneyCostEntityPartStr) throws ServiceException {
		if (StringUtil.isEmpty(moneyCostEntityPartStr)) {
			return null;
		}

		try {
			String[] moneyCostEntityPartArray = moneyCostEntityPartStr.split(":");
			// 货币类型
			CurrencyType currencyType = CurrencyType.getInitCurrencyType(Integer.parseInt(moneyCostEntityPartArray[0]));
			if (currencyType == null) {
				throw new ServiceException("货币配置信息错误[{}]:", moneyCostEntityPartStr);
			}
			// 变化金额
			long amount = Long.parseLong(moneyCostEntityPartArray[1]);

			if (moneyCostEntityPartArray.length == 2) {
				return new MoneyEntity(currencyType, amount);
			} else if (moneyCostEntityPartArray.length == 3) {
				CurrencyCostType costType = CurrencyCostType.getCurrencyCostType(Integer.parseInt(moneyCostEntityPartArray[2]));
				if (costType == null) {
					throw new ServiceException("货币配置信息错误[{}]:", moneyCostEntityPartStr);
				}

				return new MoneyEntity(currencyType, amount, costType);
			} else {
				throw new ServiceException("货币配置信息错误[{}]:", moneyCostEntityPartStr);
			}
		} catch (Exception e) {
			throw new ServiceException("货币配置信息错误[{}]:", moneyCostEntityPartStr);
		}
	}

}
