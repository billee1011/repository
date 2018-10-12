package com.lingyu.common.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

public class MathUtil {
	private static final float TEN_THOUSANDS = 10000F;

	/**
	 * 计算提升万分之extremelyRatio后的值
	 * 
	 * @param originalValue
	 * @param extremelyRatio
	 * @return
	 */
	public static int calExtremelyRatioAddValue(int originalValue, int extremelyRatio) {
		return (int) (originalValue * (TEN_THOUSANDS + extremelyRatio) / TEN_THOUSANDS);
	}

	/**
	 * 计算提升万分之extremelyRatio后的值
	 * 
	 * @param originalValue
	 * @param extremelyRatio
	 * @return
	 */
	public static float calExtremelyRatioAddValue(float originalValue, int extremelyRatio) {
		return (originalValue * (TEN_THOUSANDS + extremelyRatio) / TEN_THOUSANDS);
	}

	/**
	 * 原始值乘以10000
	 * 
	 * @param originalValue
	 * @return
	 */
	public static int extremelyMulti(float originalValue) {
		return (int) (originalValue * TEN_THOUSANDS);
	}

	/**
	 * 将 cutNumber 随机分割成 count 份<br>
	 * <li>若: cutNumber 为0, 返回 [0]</li><br>
	 * <li>若: count 为 0 或者 1, 返回 [cutNumber]</li><br>
	 * <li>若: cutNumber < count, 返回 [1,1,1,..]</li><br>
	 * 
	 * @param cutNumber 待分割的数值
	 * @param count 要分割成几份
	 * @return
	 */
	public static List<Integer> radmonCutNum(int cutNumber, int count) {
		List<Integer> returnNumList = new ArrayList<>();
		if (cutNumber == 0 || count == 0 || count == 1) {
			returnNumList.add(cutNumber);
		} else if (cutNumber < count) {
			for (int i = 0; i < cutNumber; i++) {
				returnNumList.add(1);
			}
		} else {
			for (int i = 0; i < count; i++) {
				// 最后一份，不管有多少都给他。
				if (i == count - 1) {
					returnNumList.add(cutNumber);
				}
				// 前面就随便分，每次会留点给后面.
				else {
					int radmonNum = RandomUtils.nextInt(1, cutNumber - (count - i - 1));
					returnNumList.add(radmonNum);
					cutNumber = cutNumber - radmonNum;
				}
			}
		}

		return returnNumList;
	}

	// --------------------------------------------------------------
	public static void main(String[] args) {
		List<Integer> radmonCutNumList = MathUtil.radmonCutNum(5, 2);
		for (Integer num : radmonCutNumList) {
			System.err.println(num);
		}
	}
}
