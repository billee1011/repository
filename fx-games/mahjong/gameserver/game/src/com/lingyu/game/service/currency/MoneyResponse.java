package com.lingyu.game.service.currency;

import java.util.HashMap;
import java.util.Map;

import com.lingyu.common.constant.CurrencyConstant.CurrencyType;

public class MoneyResponse {

	private boolean isSuccess;

	Map<CurrencyType, MoneyChange> moneyChangeInfo = new HashMap<>();

	public MoneyResponse(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	/**
	 * @param isSuccess 是否成功
	 */
	public MoneyResponse(boolean isSuccess, CurrencyType moneyType, long changedAmount, long beforeChangedAmount, long afterChangedAmount) {
		this.isSuccess = isSuccess;
		MoneyChange moneyChange = new MoneyChange(moneyType, changedAmount, beforeChangedAmount, afterChangedAmount);
		this.moneyChangeInfo.put(moneyType, moneyChange);
	}

	/** 新增货币变更信息 */
	public void addMoneyChangeInfo(CurrencyType moneyType, long changedAmount, long beforeChangedAmount, long afterChangedAmount) {
		MoneyChange moneyChange = new MoneyChange(moneyType, changedAmount, beforeChangedAmount, afterChangedAmount);
		this.moneyChangeInfo.put(moneyType, moneyChange);
	}

	/** 是否成功 */
	public boolean isSuccess() {
		return isSuccess;
	}

	/** 获取货币消耗信息 */
	public Map<CurrencyType, MoneyChange> getMoneyChangeInfo() {
		return moneyChangeInfo;
	}

	/**
	 * 货币变化信息
	 * 
	 * @author WCM
	 * 
	 */
	public class MoneyChange {

		private CurrencyType moneyType;

		private long changedAmount;

		private long beforeChangedAmount;

		private long afterChangedAmount;

		/**
		 * 货币变化信息
		 * 
		 * @param moneyType 货币类型
		 * @param changedAmount 变化数量
		 * @param beforeChangedAmount 变化前数量
		 * @param afterChangedAmount 变化后数量
		 * 
		 * @author WCM
		 * 
		 */
		public MoneyChange(CurrencyType moneyType, long changedAmount, long beforeChangedAmount, long afterChangedAmount) {
			super();
			this.moneyType = moneyType;
			this.changedAmount = changedAmount;
			this.beforeChangedAmount = beforeChangedAmount;
			this.afterChangedAmount = afterChangedAmount;
		}

		/** 增加的货币类型 */
		public CurrencyType getMoneyType() {
			return moneyType;
		}

		/** 增加的货币值 */
		public long getChangedAmount() {
			return changedAmount;
		}

		/** 货币变化前的值 */
		public long getBeforeChangedAmount() {
			return beforeChangedAmount;
		}

		/** 货币变化后的值 */
		public long getAfterChangedAmount() {
			return afterChangedAmount;
		}
	}
}
