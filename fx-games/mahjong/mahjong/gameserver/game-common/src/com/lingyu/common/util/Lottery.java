/**
 * 
 */
package com.lingyu.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;

/**
 * @description
 * @author ShiJie Chi
 * @created 2011-12-16上午11:15:13
 */
public enum Lottery {

	TEN(10), HUNDRED(100), THOUSAND(1000), TENTHOUSAND(10000);

	private int val;

	Lottery(int val) {
		this.val = val;
	}

	public int getVal() {
		return val;
	}

	/**
	 * @param
	 */
	public static int roll(int maxNumber) {
		return RandomUtils.nextInt(0, maxNumber);
	}

	public static boolean isSuccess(float rate) {
		return RandomUtils.nextFloat(0, 1) < rate;
	}
	
	public static float rollFloat(float sumRare) {
		return RandomUtils.nextFloat(0, sumRare);
	}

	/**
	 * 在一个数据段之间获取一个随机值
	 * 
	 * @param min 随机区间最小值
	 * @param max 随机区间最大值
	 * @return 随机值
	 */
	public static int randomInt(int min, int max) {
		return RandomUtils.nextInt(min, max);
	}

	/**
	 * 在一个数据段之间获取一个随机值,并计算出一个精度范围内的小数值 <b>备注:</b> 先放大unit 倍,计算完毕后在缩小unit被
	 * 
	 * @param min 随机区间最小值
	 * @param max 随机区间最大值
	 * @param unit 扩大精确度倍数
	 * @return 随机值
	 */
	public static float randomFloat(float min, float max, Lottery unit) {
		int _min = (int) (min * unit.getVal());
		int _max = (int) (max * unit.getVal());
		int r = randomInt(_min, _max);
		return r / (unit.getVal() * 1f);
	}

	/**
	 * 按照比例先放大enlargeUnit倍后，再缩小redduceUnit倍；的一个随机值
	 * 
	 * @param min 最小值
	 * @param max 随机最大值
	 * @param enlargeUnit 扩大单位
	 * @param redduceUnit 缩小单位
	 * @return 随机值
	 */
	public static float randomReduceFloat(float min, float max, Lottery enlargeUnit, Lottery redduceUnit) {
		int _min = (int) (min * enlargeUnit.getVal());
		int _max = (int) (max * enlargeUnit.getVal());
		int r = randomInt(_min, _max);
		return r / (redduceUnit.getVal() * 1f);
	}

	/**
	 * 随机取Map的一个Key
	 * 
	 * @param map
	 * @return
	 */
	public static <T, V> T randomMapKey(Map<T, V> map) {
		List<T> keys = new ArrayList<T>(map.keySet());

		int i = roll(keys.size());
		return keys.get(i);
	}

	/**
	 * 以baseVal按unit倍放大后得到maxNumber，按unit范围roll随机数和maxNumber相比较 1、小于等于，则roll成功
	 * 2、 大于，则roll失败
	 */
	public static boolean roll(float baseVal, Lottery unit) {

		int maxNumber = (int) (baseVal * unit.getVal());

		return baseVal > 0 && roll(unit.getVal()) <= maxNumber;
	}

	/**
	 * 以baseVal按unit倍放大后得到maxNumber，在0-maxNumber之间随机取值 1、小于等于，则roll成功 2、
	 * 大于，则roll失败
	 */
	public static int rollInt(float baseVal, Lottery unit) {
		int maxNumber = (int) (baseVal * unit.getVal());
		
		return roll(maxNumber);
	}

	/**
	 * 随即获取Map中一个key(0-1).
	 * <p>
	 * <b>备注:</b>权重范围为 1
	 * @param items (key: value:比例值/权重值)
	 * @return
	 */
	public static <T> T getRandomKey(Map<T, Float> items) {

		float sum = 0f;

		float ran = RandomUtils.nextFloat(0, 1);
		for (Map.Entry<T, Float> entry : items.entrySet()) {
			if (ran >= sum && ran < sum + entry.getValue()) {
				return entry.getKey();
			}
			sum += entry.getValue();
		}
		return null;
	}

	/**
	 * 随即获取Map中一个key(0-和值).
	 * <p>
	 * <b>备注:</b>权重范围为 map中所有value值的和
	 * @param items (key: value:比例值/权重值)
	 * @return
	 */
	public static <T> T getRandomKey2(Map<T, Float> items) {
		return getRandomKey2(items, Lottery.TENTHOUSAND);
	}
	
	/**
	 * 随即获取Map中一个key(0-和值).
	 * <p>
	 * <b>备注:</b>权重范围为 map中所有value值的和
	 * @param items (key: value:比例值/权重值)
	 * @return
	 */
	public static <T> T getRandomKey2(Map<T, Float> items, Lottery lotteryUnit) {
		if (items == null || items.size() == 0) {
			return null;
		}
		// 求和(选取随机数范围)
		float total = 0;
		for (Map.Entry<T, Float> entry : items.entrySet()) {
			total += entry.getValue();
		}
		// 如果几率之和为0，随机一个key
		if (total == 0) {
			return null;
		}
		float sum = 0f;
		// 随机取值
		int ran = rollInt(total, lotteryUnit);
		for (Map.Entry<T, Float> entry : items.entrySet()) {
			float tmp = sum + entry.getValue();
			if (ran >= (sum * lotteryUnit.getVal()) && ran < (tmp * lotteryUnit.getVal())) {
				return entry.getKey();
			}
			sum = tmp;
		}
		return null;
	}

//	public static List<Integer> randomNum(Collection<Integer> source, int num) {
//		if (source == null || num < 1) {
//			// 没有源或要取的数小于1个就直接返回空列表
//			return Collections.emptyList();
//		}
//		if (source.size() <= num) {
//			List<Integer> result = new ArrayList<>(source);
//			Collections.shuffle(result);
//			return result;
//		}
//		Integer[] rs = new Integer[source.size()];
//		source.toArray(rs);
//		List<Integer> result = new ArrayList<>(num);
//		for (int i = 0; i < num; i++) {
//			int index = RandomUtils.nextInt(0, rs.length - i);
//			result.add(rs[index]);
//			rs[index] = rs[rs.length - 1 - i];
//		}
//		return result;
//	}

	/**
	 * 从一个List集合源中随机出指定数量的元素.
	 * <p>
	 * <code>
	 * source = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]<br>
	 * random(source, 5) = [5, 3, 6, 7, 2]
	 * </code>
	 * 
	 * @param source List集合源
	 * @param num 指定数量
	 * @return 如果源为空或指定数量小于1，则返回空集合，否则随机抽取元素组装新集合并返回
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> random(final Collection<T> source, int num) {
		if (source == null || num < 1) {
			// 没有源或要取的数小于1个就直接返回空列表
			return Collections.emptyList();
		}
		if (source.size() <= num) {
			final List<T> result = new ArrayList<>(source);
			Collections.shuffle(result);
			return result;
		}
		T[] rs = (T[]) source.toArray();
		List<T> result = new ArrayList<>(num);
		for (int i = 0; i < num; i++) {
			int index = RandomUtils.nextInt(0, rs.length - i);
			result.add(rs[index]);
			rs[index] = rs[rs.length - 1 - i];
		}
		return result;
	}

	/**
	 * 在集合中随机抽取一个值
	 * @param source	集合
	 * @return	集合中的随机值
	 */
	public static <T> T random(final List<T> source){
		if(source == null || source.size() == 0){
			return null;
		}
		int size = source.size();
		if(source.size() == 1){
			return source.get(0);
		}
		int nexIndex = RandomUtils.nextInt(0, size);
		return source.get(nexIndex);
	}
	
	public static void main(String[] args) {
		List<Integer> source = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			source.add(i);
		}
//		System.out.println(Arrays.toString(source.toArray()));
		
//		for(int j=0;j<100;j++){
//			try {
//				int result = random(source);
//				System.out.println("-->result:"+result);
//			} catch (Exception e) {
//				System.out.println("-----"+e);
//			}
//		}

//		System.out.println(Arrays.toString(randomNum(source, -1).toArray()));
//		System.out.println(Arrays.toString(randomNum(source, 0).toArray()));
//		System.out.println(Arrays.toString(randomNum(source, 1).toArray()));
//		System.out.println(Arrays.toString(randomNum(source, 5).toArray()));
//		System.out.println(Arrays.toString(randomNum(source, 9).toArray()));
//		System.out.println(Arrays.toString(randomNum(source, 10).toArray()));
//		System.out.println(Arrays.toString(randomNum(source, 11).toArray()));
	}

}
