package com.lingyu.common.constant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 货币
 * 
 * @author WCM
 * 
 */
public class CurrencyConstant {
	/** 游戏币的最大值 100亿 */
	public static final long COIN_MAX = 100_0000_0000L;
	/** 绑定钻石最大值 20亿 */
	public static final int BIND_DIAMOND_MAX = 20_0000_0000;
	/** 绑定钻石最大值 20亿 */
	public static final int DIAMOND_MAX = 20_0000_0000;

	/** 货币变化类型-消耗：{@value} */
	public static final int DECR_TYPE = 0;
	/** 货币变化类型-增加：{@value} */
	public static final int INCR_TYPE = 1;
	/** 虚拟货币标识值 */
	public static final int VIRTUAL_MONEY_FLAG = 100;

	/**
	 * 验证当前货币是否为虚拟货币
	 * 
	 * @return true.是虚拟货币
	 */
	public static boolean isVirtualMoney(int currencyTypeId) {
		return currencyTypeId >= VIRTUAL_MONEY_FLAG;
	}

	/** 货币消耗类型 */
	public enum CurrencyCostType {
		/** 仅扣当前货币 */
		ONLY(1, "仅扣当前货币"),

		/** 优先扣除当前货币, 再扣除对应绑定/非绑定货币 */
		CURRENT_FIRST(2, "优先扣除当前货币, 再扣除对应绑定/非绑定货币"), ;

		/** 数据源编号 */
		private int id;
		/** 数据源描述 */
		private String description;

		private CurrencyCostType(int id, String description) {
			this.id = id;
			this.description = description;
		}

		/** 货币类型编号 */
		public int getId() {
			return id;
		}

		/** 货币类型说明 */
		public String getDescription() {
			return description;
		}

		/**
		 * 获取 {@link CurrencyCostType}
		 * 
		 * @param costTypeId 消耗类型ID
		 * @return
		 */
		public static CurrencyCostType getCurrencyCostType(int costTypeId) {
			for (CurrencyCostType costType : CurrencyCostType.values()) {
				if (costType.getId() == costTypeId) {
					return costType;
				}
			}

			return null;
		}
	}

	/**
	 * 货币类型
	 * 
	 * @author WCM
	 * 
	 */
	public enum CurrencyType {
		/** 钻石 */
		DIAMOND_NEW(1, "钻石"),
		;

		// ///////////////////////////////////////////////////////////////
		/** 数据源编号 */
		private int id;
		/** 数据源描述 */
		private String description;

		private static Map<Integer, CurrencyType> result = new HashMap<>();

		private CurrencyType(int id, String description) {
			this.id = id;
			this.description = description;
		}

		/** 货币类型编号 */
		public int getId() {
			return id;
		}

		/** 货币类型说明 */
		public String getDescription() {
			return description;
		}

		static {
			for (CurrencyType type : CurrencyType.values()) {
				result.put(type.getId(), type);
			}
		}

		/**
		 * 获取对应的货币类型(默认的)
		 * 
		 * @param id
		 * @return null:没有对应的货币类型
		 */
		public static CurrencyType getInitCurrencyType(int id) {
			return result.get(id);
		}

		public static Map<Integer, CurrencyType> getResult() {
			return result;
		}

	}

	/**
	 * 获取排序之后的结果
	 * 
	 * @return
	 */
	public static List<String[]> getCurrencyTypeList() {
		Map<Integer, CurrencyType> map = CurrencyType.getResult();
		Collection<CurrencyType> coll = map.values();
		List<CurrencyType> list = new ArrayList<>(coll);
		Collections.sort(list, new Comparator<CurrencyType>() {

			@Override
			public int compare(CurrencyType arg0, CurrencyType arg1) {
				if (arg0.getId() > arg1.getId()) {
					return 1;
				} else if (arg0.getId() < arg1.getId()) {
					return -1;
				}
				return 0;
			}
		});
		List<String[]> result = new ArrayList<>(list.size());
		for (CurrencyType e : list) {
			result.add(new String[] { String.valueOf(e.getId()), e.getDescription() });
		}
		return result;
	}
}
