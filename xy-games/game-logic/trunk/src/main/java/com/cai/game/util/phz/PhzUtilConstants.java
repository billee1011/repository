package com.cai.game.util.phz;

public class PhzUtilConstants {
	/**
	 * 跑胡子，最大的牌值索引
	 */
	public static final int MAX_CARD_INDEX = 20;
	/**
	 * 每种字牌最多四张
	 */
	public static final int CARD_COUNT_FOR_EACH_TYPE = 4;
	/**
	 * 循环添加牌数据的时候，没张牌不能超过五张
	 */
	public static final int CRITICAL_CARD_COUNT_PER_CARD = 5;
	/**
	 * 循环生成表数据的时候，最少得有一个组合
	 */
	public static final int MIN_WEAVE_COUNT = 1;
	/**
	 * 循环生成表数据的时候，最多有七个组合
	 */
	public static final int MAX_WEAVE_COUNT = 7;
	/**
	 * 循环添加组合牌数据的时候，需要循环添加多少次，TODO 假定为50，需要修改成具体值
	 */
	public static final int MAX_WEAVE_LOOPS = 50;
	/**
	 * 循环添加组合牌数据的时候，顺子组合循环添加的临界值，TODO 假定为30，需要修改成具体值
	 */
	public static final int SHUN_ZI_WEAVE_LOOPS = 30;
	
}
