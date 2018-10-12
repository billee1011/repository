package com.cai.game.util.phz;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class SetGenerator {
	private static Set<String> set;

	private static void initialize() {
		set = new HashSet<String>();
	}

	private static boolean add(int[] cards) {
		StringBuilder keySb = new StringBuilder();

		// 根据cards数据，生成字符串的key值

		String key = keySb.toString();
		if (StringUtils.isBlank(key))
			return false;

		if (set.contains(key))
			return false;

		set.add(key);

		for (int i = 0; i < PhzUtilConstants.MAX_CARD_INDEX; i++) {
			if (cards[i] > PhzUtilConstants.CARD_COUNT_FOR_EACH_TYPE) {
				return true;
			}
		}

		SetManager.getInstance().add(key);

		return true;
	}

	private static void generateKey(int[] cards) {
		if (false == add(cards))
			return;
	}

	private static void generateSet() {
		int[] cards = new int[PhzUtilConstants.MAX_CARD_INDEX];
		for (int i = 0; i < PhzUtilConstants.MAX_CARD_INDEX; i++) {
			cards[i] = 0;
		}

		generateSubSet(cards, PhzUtilConstants.MIN_WEAVE_COUNT);
	}

	private static void generateSubSet(int[] cards, int weaveCount) {
		// 用for循环去添加有效的牌组合，分阶段进行，比如第一阶段添加坎，第二阶段添加顺子，第三阶段添加绞吃，第四阶段添加二七十
		// 下面是部分参考代码。MAX_ADD_WEAVE_LOOPS需要在常量类里修改成具体的值
		for (int i = 0; i < PhzUtilConstants.MAX_WEAVE_LOOPS; i++) {
			// 第一阶段，添加坎
			if (i < PhzUtilConstants.MAX_CARD_INDEX) {
				if (cards[i] > 3)
					continue;

				cards[i] += 3;
			}
			// 第二阶段，添加顺子
			else if (i < PhzUtilConstants.SHUN_ZI_WEAVE_LOOPS) {
				int cardIndex = i - PhzUtilConstants.MAX_CARD_INDEX;

				if (cards[cardIndex] > PhzUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
						|| cards[cardIndex + 1] > PhzUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
						|| cards[cardIndex + 2] > PhzUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
					continue;

				cards[cardIndex] += 1;
				cards[cardIndex + 1] += 1;
				cards[cardIndex + 1] += 1;
			}
			// 第三阶段
			// ...第N阶段

			// 根据牌值数据生成String类型的key值
			generateKey(cards);

			if (weaveCount < PhzUtilConstants.MAX_WEAVE_COUNT)
				generateSubSet(cards, weaveCount + 1);

			// 根据不同的阶段还原牌值数据
			// 第一阶段
			if (i < PhzUtilConstants.MAX_CARD_INDEX) {
				cards[i] -= 3;
			}
			// 第二阶段
			else if (i < PhzUtilConstants.SHUN_ZI_WEAVE_LOOPS) {
				int cardIndex = i - PhzUtilConstants.MAX_CARD_INDEX;
				cards[cardIndex] -= 1;
				cards[cardIndex + 1] -= 1;
				cards[cardIndex + 1] -= 1;
			}
			// 第三阶段
			// ...第N阶段
		}
	}
	
	private static void generate() {
		System.out.println("Generating Phz table begin ...");
		initialize();
		generateSet();
		SetManager.getInstance().dumpNormal();
		System.out.println("Generating Phz table finished ...");
	}
	
	public static void main(String[] args) {
		generate();
	}
}
