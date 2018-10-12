package com.lingyu.common.constant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lingyu.common.core.ServiceException;

/**
 * 操作类型常量
 * 
 * @author WCM
 * 
 */
public class OperateConstant {

	/** 只扣绑定钻石 */
	public static final int ONLY_BIND_DIAMOND = 0;
	/** 优先扣除绑定钻石, 再扣除钻石 */
	public static final int First_BIND_DIAMOND = 1;
	/** 优先扣除指定道具, 道具不足再优先扣除绑定钻石, 再扣除钻石 */
	public static final int First_PROP_THEN_BIND_DIAMOND = 2;

	// --------------------------------
	/** 货币流向类型 */
	public enum FlowType {
		/** 空类型 */
		NONE(0, "空类型"),
		/** 从系统获得 */
		SYSTEM_CREATE(1, "从系统获得"),
		/** 系统回收 */
		SYSTEM_COLLECTION(2, "系统回收"),
		/** 系统托管 */
		SYSTEM_TRUSTEESHIP(3, "系统托管"),
		/** 系统托管结束 */
		SYSTEM_TRUSTEESHIP_END(4, "系统托管结束"),
		/** 邮件获得 */
		AGENT_CREATE(5, "从代理获得"),;

		/** 编号 */
		private int id;
		/** 描述 */
		private String description;

		private FlowType(int id, String description) {
			this.id = id;
			this.description = description;
		}

		/** 货币流向类型编号 */
		public int getId() {
			return id;
		}

		/** 货币流向类型描述 */
		public String getDescription() {
			return description;
		}
	}

	// ---------------------------------

	/**
	 * 操作类型
	 * 
	 * @author WCM
	 * 
	 */
	public enum OperateType {

		/** 空类型 */
		NONE(0, "默认类型", FlowType.NONE),
		/** 邮件获得 */
		MAIL_GET(1001, "邮件获得", FlowType.AGENT_CREATE),
		/** 补偿 */
		REDEEM(1002, "补偿", FlowType.SYSTEM_CREATE),
		/** 代理转账 */
		AGENT_GET(1003, "代理转账", FlowType.SYSTEM_CREATE),
		/** 代理消耗 */
		AGENT_CONSUME(1004, "代理消耗", FlowType.NONE);

		// ///////////////////////////////////////////////////////////////
		/** 数据源编号 */
		private int id;
		/** 数据源描述 */
		private String description;
		/** 流向类型 */
		private FlowType flowType;

		private OperateType(int id, String description, FlowType flowType) {
			this.id = id;
			this.description = description;
			this.flowType = flowType;
		}

		/** 操作类型编号 */
		public int getId() {
			return id;
		}

		/** 数据源描述 */
		public String getDescription() {
			return description;
		}

		/** 流向类型 */
		public FlowType getFlowType() {
			return flowType;
		}

		private static Map<Integer, OperateType> result = new HashMap<>();

		static {
			for (OperateType type : OperateType.values()) {
				if (result.containsKey(type.getId())) {
					throw new ServiceException("货币操作类型存在重复的编号：[{}]", type.getId());
				}
				result.put(type.getId(), type);
			}
		}

		public static OperateType getOperateType(int id) {
			return result.get(id);
		}
	}

	public static String getOperateTypeName(int id) {

		return OperateType.getOperateType(id).getDescription();
	}

	public final static List<String[]> getOperateList() {
		Collection<OperateType> collect = OperateConstant.OperateType.result.values();
		List<OperateType> list = new ArrayList<>(collect);
		Collections.sort(list, new Comparator<OperateType>() {

			@Override
			public int compare(OperateType o1, OperateType o2) {
				if (o1.getId() > o2.getId()) {
					return 1;
				} else if (o1.getId() < o2.getId()) {
					return -1;
				}
				return 0;
			}
		});
		List<String[]> result = new ArrayList<>(collect.size());
		for (OperateType e : list) {
			result.add(new String[] { String.valueOf(e.getId()), e.getDescription() });
		}
		return result;
	}

	public static String getFlowTypeName(int id) {
		FlowType[] value = FlowType.values();
		for (FlowType e : value) {
			if (e.getId() == id) {
				return e.getDescription();
			}
		}
		return null;
	}

	public static void main(String[] args) {
		for (OperateType type : OperateType.values()) {
			System.out.println(type.getId() + "\t" + type.getDescription());
		}
	}
}