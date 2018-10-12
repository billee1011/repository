/**
 * 
 */
package com.cai.game.yyqf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_YYQF;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.google.common.collect.Lists;

//分析结构
class TagAnalyseResult {
	int cbCardCount[] = new int[Constants_YYQF.BOM_TWELVE + 1]; // 1-12的具体数值表示相同牌的数量.
																// 比如有4个对子、则下标2的值为4
	int cbCardDatas[][] = new int[Constants_YYQF.BOM_TWELVE + 1][Constants_YYQF.ONE_COLOR_CARD_COUNT + 1]; // 第一维度表示同牌数量索引、第二维度表示同牌卡牌值

	public void reset() {
		Arrays.fill(cbCardCount, 0);
		for (int i = 1; i <= Constants_YYQF.BOM_TWELVE; i++) {
			Arrays.fill(cbCardDatas[i], 0);
			cbCardDatas[i][0] = Integer.MAX_VALUE;
		}
	}
};

// 出牌结果
class TagOutCardResult {
	int cbCardCount; // 扑克数目
	int cbResultCard[] = new int[Constants_YYQF.HAND_CARD]; // 结果扑克

	public void reset() {
		cbCardCount = 0;
		Arrays.fill(cbResultCard, 0);
	}
};

class tagOutCardTypeResult {
	int cbCardType[] = new int[GameConstants.MAX_TYPE_COUNT]; // 扑克类型
	int cbCardTypeCount;
	int cbEachHandCardCount[] = new int[GameConstants.MAX_TYPE_COUNT];// 每手个数
	int cbCardData[][] = new int[GameConstants.MAX_TYPE_COUNT][Constants_YYQF.HAND_CARD];// 扑克数据

	public tagOutCardTypeResult() {
		cbCardTypeCount = 0;
		Arrays.fill(cbCardType, 0);
		Arrays.fill(cbEachHandCardCount, 0);
		for (int i = 0; i < GameConstants.MAX_TYPE_COUNT; i++) {
			for (int j = 0; j < GameConstants.PDK_MAX_COUNT; j++) {
				cbCardData[i][j] = 0;
			}
		}
	}

	public void reset() {
		cbCardTypeCount = 0;
		Arrays.fill(cbCardType, 0);
		Arrays.fill(cbEachHandCardCount, 0);
		for (int i = 0; i < GameConstants.MAX_TYPE_COUNT; i++) {
			for (int j = 0; j < GameConstants.PDK_MAX_COUNT; j++) {
				cbCardData[i][j] = 0;
			}
		}
	}
};

class tagAnalyseIndexResult {
	public int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
	public int card_data[][] = new int[GameConstants.PDK_MAX_INDEX][GameConstants.MAX_PDK_COUNT_EQ];

	public tagAnalyseIndexResult() {
		for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void Reset() {
		for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
};

public class YYQFGameLogic {

	public int _game_rule_index; // 游戏规则

	public static final int MARK = 0 | (1 << 5) | (1 << 10) | (1 << 13);

	public YYQFGameLogic() {

	}

	public static void main(String[] args) {
		YYQFGameLogic logic = new YYQFGameLogic();
		int[] test = { 0x0c, 0x0b, 0x0a, 0x0d, 0x01, 0x09 };
		int[] hand = { 0x1a, 0x1b, 0x1c, 0x1d, 0x11, 0x12 };
		// int[] hand = { 0x1a, 0x2a, 0x3a, 0x15, 0x15 };
		// List<Integer> result = logic.getDoubleCard(hand, hand.length, test,
		// test.length);
		List<Integer> result = logic.getOutCard(hand, hand.length, test, test.length, 13, false);
		if (result.size() > 0) {
			result.forEach(value -> {
				System.out.print(get_card_value(value) + " ");
			});
		}

		// System.out.println(logic.getOutCard(test, test.length, test, true));
		// System.out.println(logic.getMaxSingleCardValue(test, test.length));
		// System.out.println(logic.searchOutCard(hand, hand.length, test,
		// test.length, 2));
		// System.out.println(logic.getCardType(test, test.length, test));
	}

	/**
	 * 获取卡牌类型
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbRealData
	 * @return
	 */
	public int getCardType(int cbCardData[], int cbCardCount, int cbRealData[], boolean hasSixAndSeven) {
		switch (cbCardCount) {
		case 0: // 空牌
			return Constants_YYQF.ERROR;
		case 1: // 单牌
			return Constants_YYQF.SINGLE;
		case 2: // 对牌
			if (getCardLogicValue(cbCardData[0]) == getCardLogicValue(cbCardData[1])) {
				return Constants_YYQF.DOUBLE;
			}
			return Constants_YYQF.ERROR;
		}

		sort_card_date_list(cbCardData, cbCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbCardData, cbCardCount, analyseResult);

		if (cbCardCount <= 12 && analyseResult.cbCardCount[cbCardCount] == 1) { // 4-12张牌炸弹判断
			return cbCardCount;
		}
		if (5 <= cbCardCount && cbCardCount <= 12) { // 5-12张顺子判断
			int single = Constants_YYQF.SINGLE;
			if (analyseResult.cbCardCount[single] == cbCardCount) {
				int len = cbCardCount;
				if (analyseResult.cbCardCount[single] >= len) {
					int last = len;
					for (int i = 1; i <= last; i++) {
						int airplanceLen = 1;
						int value = getCardLogicValue(analyseResult.cbCardDatas[single][i]);
						for (int j = i + 1; j <= last; j++) {
							if (hasSixAndSeven) { // 有6和7
								if (getCardLogicValue(analyseResult.cbCardDatas[single][j]) != (value - airplanceLen)
										|| getCardLogicValue(analyseResult.cbCardDatas[single][j]) > 15) { // 不连续
									break;
								}
							} else {
								if (getCardLogicValue(analyseResult.cbCardDatas[single][j]) == 5
										&& getCardLogicValue(analyseResult.cbCardDatas[single][j]) == value - 2
												- airplanceLen) {
									// 去掉6和7的情况下5588可以连
								} else if (getCardLogicValue(
										analyseResult.cbCardDatas[single][j]) != (value - airplanceLen)
										|| getCardLogicValue(analyseResult.cbCardDatas[single][j]) > 15) { // 不连续
									break;
								}
							}
							airplanceLen++;
						}
						value = 0;
						int add = airplanceLen;
						if (airplanceLen >= len) {
							return Constants_YYQF.CONTINUITY;
						}
						i += add - 1;
					}
				}
			}
		}
		if (0 == cbCardCount % 2 && cbCardCount / 2 == analyseResult.cbCardCount[Constants_YYQF.DOUBLE]) { // 连对
			int len = cbCardCount / 2;
			if (analyseResult.cbCardCount[2] >= len) {
				int last = len; // 最大的牌为2时不比较
				// 连对判断
				for (int i = 1; i <= last; i++) {
					int airplanceLen = 1;
					int value = getCardLogicValue(analyseResult.cbCardDatas[2][i]);
					for (int j = i + 1; j <= last; j++) {
						if (hasSixAndSeven) { // 有6和7
							if (getCardLogicValue(analyseResult.cbCardDatas[2][j]) != (value - airplanceLen)
									|| getCardLogicValue(analyseResult.cbCardDatas[2][j]) > 15) { // 不连续
								break;
							}
						} else {
							if (getCardLogicValue(analyseResult.cbCardDatas[2][j]) == 5
									&& getCardLogicValue(analyseResult.cbCardDatas[2][j]) == value - 2 - airplanceLen) {
								// 去掉6和7的情况下5588可以连
							} else if (getCardLogicValue(analyseResult.cbCardDatas[2][j]) != (value - airplanceLen)
									|| getCardLogicValue(analyseResult.cbCardDatas[2][j]) > 15) { // 不连续
								break;
							}
						}
						airplanceLen++;
					}
					value = 0;
					int add = airplanceLen;
					if (airplanceLen >= len) {
						return Constants_YYQF.DOUBLE;
					}
					i += add - 1;
				}
			}
		}
		if (0 == cbCardCount % 5 && cbCardCount / 5 <= analyseResult.cbCardCount[Constants_YYQF.THREE]) { // 三带二
			int len = cbCardCount / 5;
			int three = Constants_YYQF.THREE;
			if (analyseResult.cbCardCount[three] >= len) {
				int last = 1;
				// 飞机判断
				for (int i = analyseResult.cbCardCount[three]; i >= last; i--) {
					int airplanceLen = 1;
					int value = getCardLogicValue(analyseResult.cbCardDatas[three][i]);
					for (int j = i - 1; j >= last; j--) {
						// if
						// (getCardLogicValue(analyseResult.cbCardDatas[three][j])
						// != value + airplanceLen
						// ||
						// getCardLogicValue(analyseResult.cbCardDatas[three][j])
						// > 15) { // 不连续
						// break;
						// }
						// airplanceLen++;
						if (hasSixAndSeven) { // 有6和7
							if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != (value + airplanceLen)
									|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
								break;
							}
						} else {
							if (value == 5 && getCardLogicValue(analyseResult.cbCardDatas[three][j]) == value + 2
									+ airplanceLen) {
								// 去掉6和7的情况下5588可以连
							} else if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != (value + airplanceLen)
									|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
								break;
							}
						}
						airplanceLen++;
					}
					value = 0;
					int add = airplanceLen;
					if (airplanceLen >= len) { // 三张可以压、则必须要另外具有len*2张牌用来带(炸弹不能拆开带)
						int single = analyseResult.cbCardCount[1] + analyseResult.cbCardCount[2] * 2
								+ (analyseResult.cbCardCount[3] - len) * 3;
						if (single >= len * 2) {
							List<Integer> temp = Lists.newArrayList();
							for (int k = 0; k < len; k++) { // 加入三条的牌
								for (int z = 0; z < 3; z++) {
									temp.add(analyseResult.cbCardDatas[three][i - k - value]);
								}
							}
							int singleNumber = 0; // 开始加入单牌
							for (int k = analyseResult.cbCardCount[1]; k > 0 && singleNumber < len * 2; k--) {
								temp.add(analyseResult.cbCardDatas[1][k]);
								singleNumber++;
							}
							for (int k = analyseResult.cbCardCount[2]; k > 0 && singleNumber < len * 2; k--) {
								for (int z = 0; z < 2 && singleNumber < len * 2; z++) {
									temp.add(analyseResult.cbCardDatas[2][k]);
									singleNumber++;
								}
							}
							for (int k = analyseResult.cbCardCount[3]; k > 0 && singleNumber < len * 2; k--) {
								if (k <= (i - value) && k > (i - value) - len) { // 已被选中的三条牌不需要拆开放进来
									continue;
								}
								for (int z = 0; z < 3 && singleNumber < len * 2; z++) {
									temp.add(analyseResult.cbCardDatas[3][k]);
									singleNumber++;
								}
							}
							return Constants_YYQF.THREE;
						}
					}
					i -= (add - 1);
				}
			}
		}

		return Constants_YYQF.ERROR;
	}

	/**
	 * 分析扑克
	 * 
	 * @param cbCardData
	 *            扑克数据
	 * @param cbCardCount
	 *            扑克数量
	 * @param analyseResult
	 *            分析结果
	 */
	public void analyseCardData(int cbCardData[], int cbCardCount, TagAnalyseResult analyseResult) {
		analyseResult.reset();

		for (int i = 0; i < cbCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = getCardLogicValue(cbCardData[i]);
			// 搜索同牌
			for (int j = i + 1; j < cbCardCount; j++) {
				// 获取扑克
				if (getCardLogicValue(cbCardData[j]) != cbLogicValue) {
					break;
				}
				cbSameCount++;
			}
			analyseResult.cbCardDatas[cbSameCount][++analyseResult.cbCardCount[cbSameCount]] = cbCardData[i];
			i += cbSameCount - 1;
		}
		return;
	}

	// 对比扑克
	public boolean compareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,
			boolean hasSixAndSeven) {
		// 获取类型
		int cbNextType = getCardType(cbNextCard, cbNextCount, cbNextCard, hasSixAndSeven);
		int cbFirstType = getCardType(cbFirstCard, cbFirstCount, cbFirstCard, hasSixAndSeven);

		// 类型判断
		if (cbNextType == Constants_YYQF.ERROR) {
			return false;
		}

		if (cbFirstType == cbNextType) {
			if (cbFirstType == Constants_YYQF.THREE || cbFirstType == Constants_YYQF.DOUBLE
					|| cbFirstType == Constants_YYQF.CONTINUITY) { // 三张的比对不一样
				if (cbNextCount != cbFirstCount) {
					return false;
				}
				return this.searchOutCard(cbNextCard, cbNextCount, cbFirstCard, cbFirstCount, cbFirstType,
						hasSixAndSeven);
			}
			return getCardLogicValue(cbNextCard[0]) > getCardLogicValue(cbFirstCard[0]);
		}
		if ((cbFirstType < 4 || cbFirstType > 12) && (cbNextType >= 4 && cbNextType <= 12)) { // 第一个牌型不是炸弹、第二个牌型是炸弹
			return true;
		}
		if ((cbFirstType >= 4 && cbFirstType <= 12) && (cbNextType >= 4 && cbNextType <= 12)) {
			return cbNextType > cbFirstType;
		}

		return false;
	}

	public int countCard(int cardDatas[], int cardCount, int cardData) {
		int result = 0;
		for (int i = 0; i < cardCount; i++) {
			if (getCardLogicValue(cardDatas[i]) == getCardLogicValue(cardData)) {
				result++;
			}
		}
		return result;
	}

	/**
	 * 出牌搜索 判断是否有压牌
	 * 
	 * @param cbHandCardData
	 *            手牌
	 * @param cbHandCardCount
	 *            手牌数量
	 * @param cbTurnCardData
	 *            上一家出牌数据
	 * @param cbTurnCardCount
	 *            上一家出牌数量
	 * @return
	 */
	public boolean searchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int cardType, boolean hasSixAndSeven) {
		if (cardType == Constants_YYQF.ERROR) {
			return false;
		}
		// 搜索炸弹
		List<Integer> card = getBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, cardType);
		if (card.size() > 0) {
			return true;
		}
		if (cardType == Constants_YYQF.SINGLE) {
			return getSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData,
					cbTurnCardCount) != Constants_YYQF.ERROR;
		}
		if (cardType == Constants_YYQF.DOUBLE) {
			return getDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, hasSixAndSeven)
					.size() > 0;
		}
		if (cardType == Constants_YYQF.THREE) {
			return getThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, hasSixAndSeven)
					.size() > 0;
		}
		if (cardType == Constants_YYQF.CONTINUITY) {
			return getContinuityCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, hasSixAndSeven)
					.size() > 0;
		}

		return false;
	}

	/**
	 * 判断是否有压牌
	 * 
	 * @return
	 */
	public List<Integer> getOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int cardType, boolean hasSixAndSeven) {
		if (cardType == Constants_YYQF.ERROR || cbHandCardData.length <= 0) {
			return Collections.emptyList();
		}

		List<Integer> outCardData = getBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
				cardType);
		if (outCardData.size() > 0) {
			return outCardData;
		}
		if (cardType == Constants_YYQF.SINGLE) {
			int card = getSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
			if (card != Constants_YYQF.ERROR) {
				outCardData = Lists.newArrayList();
				outCardData.add(card);
				return outCardData;
			}
		}
		if (cardType == Constants_YYQF.DOUBLE) {
			outCardData = getDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
					hasSixAndSeven);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		if (cardType == Constants_YYQF.THREE) {
			outCardData = getThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
					hasSixAndSeven);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		if (cardType == Constants_YYQF.CONTINUITY) {
			outCardData = getContinuityCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
					hasSixAndSeven);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		return Collections.emptyList();
	}

	public int getMaxLianXuValue(TagAnalyseResult analyseResult, int len, boolean hasSixAndSeven) {
		int three = Constants_YYQF.THREE;
		int last = 1;
		for (int i = analyseResult.cbCardCount[three]; i >= last; i--) {
			int airplanceLen = 1;
			int value = getCardLogicValue(analyseResult.cbCardDatas[three][i]);
			for (int j = i - 1; j >= last; j--) {
				if (hasSixAndSeven) { // 有6和7
					if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != (value + airplanceLen)
							|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
						break;
					}
				} else {
					if (value == 5
							&& getCardLogicValue(analyseResult.cbCardDatas[three][j]) == value + 2 + airplanceLen) {
						// 去掉6和7的情况下5588可以连
					} else if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != (value + airplanceLen)
							|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
						break;
					}
				}
				airplanceLen++;
			}
			if (airplanceLen >= len) {
				return value + airplanceLen - len;
			}
			i -= (airplanceLen - 1);
		}
		return 0;
	}

	/**
	 * 搜索三张、飞机
	 * 
	 * @return
	 */
	public List<Integer> getThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, boolean hasSixAndSeven) {
		if (cbHandCardCount < cbTurnCardCount) {
			return Collections.emptyList();
		}

		sort_card_date_list(cbTurnCardData, cbTurnCardCount);
		TagAnalyseResult turnResult = new TagAnalyseResult();
		analyseCardData(cbTurnCardData, cbTurnCardCount, turnResult);

		sort_card_date_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);
		// 飞机判断: 手上的三条数量大于出牌的三条数量
		int len = cbTurnCardCount / 5;
		int three = Constants_YYQF.THREE;
		if (analyseResult.cbCardCount[three] >= len) {
			int maxValue = getMaxLianXuValue(turnResult, len, hasSixAndSeven);
			int last = 1;
			// 飞机判断
			for (int i = analyseResult.cbCardCount[three]; i >= last; i--) {
				int airplanceLen = 1;
				int value = getCardLogicValue(analyseResult.cbCardDatas[three][i]);
				for (int j = i - 1; j >= last; j--) {
					if (hasSixAndSeven) { // 有6和7
						if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != (value + airplanceLen)
								|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
							break;
						}
					} else {
						if (value == 5
								&& getCardLogicValue(analyseResult.cbCardDatas[three][j]) == value + 2 + airplanceLen) {
							// 去掉6和7的情况下5588可以连
						} else if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != (value + airplanceLen)
								|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
							break;
						}
					}
					airplanceLen++;
				}
				value = 0;
				int add = airplanceLen;
				while (airplanceLen > 0 && getCardLogicValue(analyseResult.cbCardDatas[three][i - value]) <= maxValue) {
					value++;
					airplanceLen--;
				}
				if (airplanceLen >= len && getCardLogicValue(analyseResult.cbCardDatas[three][i - value]) > maxValue) { // 三张可以压、则必须要另外具有len*2张牌用来带(炸弹不能拆开带)
					int single = analyseResult.cbCardCount[1] + analyseResult.cbCardCount[2] * 2
							+ (analyseResult.cbCardCount[3] - len) * 3;
					if (single >= len * 2) {
						List<Integer> temp = Lists.newArrayList();
						for (int k = 0; k < len; k++) { // 加入三条的牌
							for (int z = 0; z < 3; z++) {
								temp.add(analyseResult.cbCardDatas[three][i - k - value]);
							}
						}
						int singleNumber = 0; // 开始加入单牌
						for (int k = analyseResult.cbCardCount[1]; k > 0 && singleNumber < len * 2; k--) {
							temp.add(analyseResult.cbCardDatas[1][k]);
							singleNumber++;
						}
						for (int k = analyseResult.cbCardCount[2]; k > 0 && singleNumber < len * 2; k--) {
							for (int z = 0; z < 2 && singleNumber < len * 2; z++) {
								temp.add(analyseResult.cbCardDatas[2][k]);
								singleNumber++;
							}
						}
						for (int k = analyseResult.cbCardCount[3]; k > 0 && singleNumber < len * 2; k--) {
							if (k <= (i - value) && k > (i - value) - len) { // 已被选中的三条牌不需要拆开放进来
								continue;
							}
							for (int z = 0; z < 3 && singleNumber < len * 2; z++) {
								temp.add(analyseResult.cbCardDatas[3][k]);
								singleNumber++;
							}
						}
						return temp;
					}
				}
				i -= (add - 1);
			}
		}

		return Collections.emptyList();
	}

	/**
	 * 搜索对子、连对
	 * 
	 * @return
	 */
	public List<Integer> getDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, boolean hasSixAndSeven) {
		if (cbHandCardCount < cbTurnCardCount) {
			return Collections.emptyList();
		}

		sort_card_date_list(cbTurnCardData, cbTurnCardCount);

		sort_card_date_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);

		// 合并手上的三条跟对子(三条也可以作为对子处理)
		int d = analyseResult.cbCardCount[2], t = analyseResult.cbCardCount[3];
		int doubleCount = d + t; // 手上对子数
		int doubleCard[] = new int[doubleCount];
		for (int i = 0; i < doubleCount; i++) {
			if (getCardLogicValue(analyseResult.cbCardDatas[2][d]) > getCardLogicValue(
					analyseResult.cbCardDatas[3][t])) {
				doubleCard[i] = analyseResult.cbCardDatas[3][t];
				t--;
			} else {
				doubleCard[i] = analyseResult.cbCardDatas[2][d];
				d--;
			}
		}

		// 连对判断: 手上的对子数量大于出牌的对子数量
		int len = cbTurnCardCount / 2;
		if (doubleCount >= len) {
			int last = doubleCount - 1; // 最大的牌为2时不比较
			// 连对判断
			for (int i = 0; i <= last; i++) {
				int airplanceLen = 1;
				int value = getCardLogicValue(doubleCard[i]);
				for (int j = i + 1; j <= last; j++) {
					if (hasSixAndSeven) { // 有6和7
						if ((getCardLogicValue(doubleCard[j]) != value + airplanceLen)
								|| getCardLogicValue(doubleCard[j]) > 15) { // 不连续
							break;
						}
					} else {
						if (value == 5 && getCardLogicValue(doubleCard[j]) == value + 2 + airplanceLen) {
							// 去掉6和7的情况下5588可以连
						} else if (getCardLogicValue(doubleCard[j]) != value + airplanceLen
								|| getCardLogicValue(doubleCard[j]) > 15) { // 不连续
							break;
						}
					}
					airplanceLen++;
				}
				value = 0;
				int add = airplanceLen;
				while (airplanceLen > 0 && getCardLogicValue(doubleCard[i + value]) <= getCardLogicValue(
						cbTurnCardData[cbTurnCardCount - 1])) {
					value++;
					airplanceLen--;
				}
				if (airplanceLen >= len && getCardLogicValue(doubleCard[i + value]) > getCardLogicValue(
						cbTurnCardData[cbTurnCardCount - 1])) {
					List<Integer> temp = Lists.newArrayList();
					for (int k = 0; k < len; k++) { // 加入对子牌
						temp.add(doubleCard[i + value + k]);
						temp.add(doubleCard[i + value + k]);
					}
					return temp;
				}
				if (add <= 0) {
					add = 1;
				}
				i += add - 1;
			}
		}

		return Collections.emptyList();
	}

	/**
	 * 搜索单牌
	 * 
	 * @return
	 */
	public int getSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}

		sort_card_date_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);
		// 合并手上的三条、对子、单牌
		int s = analyseResult.cbCardCount[1], d = analyseResult.cbCardCount[2], t = analyseResult.cbCardCount[3];
		int singleCount = s + d + t; // 手上单牌数
		int singleCard[] = new int[singleCount];
		for (int i = 0; i < singleCount; i++) {
			if (getCardLogicValue(analyseResult.cbCardDatas[2][d]) > getCardLogicValue(
					analyseResult.cbCardDatas[3][t])) {
				if (getCardLogicValue(analyseResult.cbCardDatas[1][s]) > getCardLogicValue(
						analyseResult.cbCardDatas[3][t])) {
					singleCard[i] = analyseResult.cbCardDatas[3][t];
					t--;
				} else {
					singleCard[i] = analyseResult.cbCardDatas[1][s];
					s--;
				}
			} else {
				if (getCardLogicValue(analyseResult.cbCardDatas[1][s]) > getCardLogicValue(
						analyseResult.cbCardDatas[2][d])) {
					singleCard[i] = analyseResult.cbCardDatas[2][d];
					d--;
				} else {
					singleCard[i] = analyseResult.cbCardDatas[1][s];
					s--;
				}
			}
		}

		for (int i = 0; i < singleCount; i++) {
			if (getCardLogicValue(singleCard[i]) > getCardLogicValue(cbTurnCardData[0])) {
				return singleCard[i];
			}
		}

		return Constants_YYQF.ERROR;
	}

	/**
	 * 搜索顺子
	 * 
	 * @return
	 */
	public List<Integer> getContinuityCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, boolean hasSixAndSeven) {
		if (cbHandCardCount < cbTurnCardCount) {
			return Collections.emptyList();
		}

		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}

		sort_card_date_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);
		// 合并手上的三条、对子、单牌
		int s = analyseResult.cbCardCount[1], d = analyseResult.cbCardCount[2], t = analyseResult.cbCardCount[3];
		int singleCount = s + d + t; // 手上单牌数
		int singleCard[] = new int[singleCount];
		for (int i = 0; i < singleCount; i++) {
			if (getCardLogicValue(analyseResult.cbCardDatas[2][d]) > getCardLogicValue(
					analyseResult.cbCardDatas[3][t])) {
				if (getCardLogicValue(analyseResult.cbCardDatas[1][s]) > getCardLogicValue(
						analyseResult.cbCardDatas[3][t])) {
					singleCard[i] = analyseResult.cbCardDatas[3][t];
					t--;
				} else {
					singleCard[i] = analyseResult.cbCardDatas[1][s];
					s--;
				}
			} else {
				if (getCardLogicValue(analyseResult.cbCardDatas[1][s]) > getCardLogicValue(
						analyseResult.cbCardDatas[2][d])) {
					singleCard[i] = analyseResult.cbCardDatas[2][d];
					d--;
				} else {
					singleCard[i] = analyseResult.cbCardDatas[1][s];
					s--;
				}
			}
		}

		sort_card_date_list(cbTurnCardData, cbTurnCardCount);
		int len = cbTurnCardCount;
		if (singleCount >= len) {
			int last = singleCount - 1; // 最大的牌为2时不比较
			// 顺子判断
			for (int i = 0; i <= last; i++) {
				int airplanceLen = 1;
				int value = getCardLogicValue(singleCard[i]);
				for (int j = i + 1; j <= last; j++) {
					if (hasSixAndSeven) { // 有6和7
						if ((getCardLogicValue(singleCard[j]) != value + airplanceLen)
								|| getCardLogicValue(singleCard[j]) > 15) { // 不连续
							break;
						}
					} else {
						if (value == 5 && getCardLogicValue(singleCard[j]) == value + 2 + airplanceLen) {
							// 去掉6和7的情况下5588可以连
						} else if (getCardLogicValue(singleCard[j]) != value + airplanceLen
								|| getCardLogicValue(singleCard[j]) > 15) { // 不连续
							break;
						}
					}
					airplanceLen++;
				}
				value = 0;
				while (airplanceLen > 0 && getCardLogicValue(singleCard[i + value]) <= getCardLogicValue(
						cbTurnCardData[cbTurnCardCount - 1])) {
					value++;
					airplanceLen--;
				}
				if (airplanceLen >= len && getCardLogicValue(singleCard[i + value]) > getCardLogicValue(
						cbTurnCardData[cbTurnCardCount - 1])) {
					List<Integer> temp = Lists.newArrayList();
					for (int k = 0; k < len; k++) { // 加入对子牌
						temp.add(singleCard[i + value + k]);
					}
					return temp;
				}
				if (airplanceLen <= 0) {
					airplanceLen = 1;
				}
				i += airplanceLen - 1;
			}
		}

		return Collections.emptyList();
	}

	/**
	 * 获取炸弹
	 * 
	 * @return
	 */
	public List<Integer> getBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int cardType) {
		if (cbHandCardCount < 4) { // 4张以下不能构成炸弹
			return Collections.emptyList();
		}
		if ((cardType >= 4 && cardType <= 12) && cbHandCardCount < cardType) { // 如果上一手出的炸弹、且该玩家手牌数不够炸弹长度则肯定不能大
			return Collections.emptyList();
		}

		sort_card_date_list(cbHandCardData, cbHandCardCount);
		List<Integer> temp = Lists.newArrayList();
		for (int i = 0; i < cbHandCardCount; i++) {
			int cbSameCount = 1;
			int cbLogicValue = getCardLogicValue(cbHandCardData[i]);
			temp.clear();

			temp.add(cbHandCardData[i]);
			for (int j = i + 1; j < cbHandCardCount; j++) {
				if (getCardLogicValue(cbHandCardData[j]) != cbLogicValue) {
					break;
				}
				temp.add(cbHandCardData[j]);
				cbSameCount++;
			}
			if (cbSameCount >= 4) {
				if (cbSameCount > 3 * 4) {
					temp.clear();
					return temp;
				}
				if (cardType > 12 || cardType < 4) { // 上一手出牌不是炸弹
					return temp;
				} else {
					if (cbSameCount > cardType) {
						return temp;
					} else if (cbSameCount == cbTurnCardCount && cbLogicValue > getCardLogicValue(cbTurnCardData[0])) {
						return temp;
					}
				}
			}
			i += cbSameCount - 1;
		}

		return Collections.emptyList();
	}

	/**
	 * 获取所有炸弹卡牌数 (1个4炸就是4张牌)
	 * 
	 * @param cbHandCardData
	 *            手牌数据
	 * @param cbHandCardCount
	 *            手牌数
	 * @return
	 */
	public int getAllBomCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;
		if (cbHandCardCount < 4) {
			return 0;
		}

		for (int i = 0; i < cbHandCardCount; i++) {
			int cbSameCount = 1;
			int cbLogicValue = getCardLogicValue(cbTmpCardData[i]);

			for (int j = i + 1; j < cbHandCardCount; j++) {
				if (getCardLogicValue(cbTmpCardData[j]) != cbLogicValue) {
					break;
				}
				cbSameCount++;
			}
			if (4 <= cbSameCount) {
				cbBomCardCount += cbSameCount;
			}
			i += cbSameCount - 1;
		}
		return cbBomCardCount;
	}

	// 洗牌
	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);

	}

	// 混乱准备
	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);

	}

	/***
	 * //排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public void sort_card_date_list(int card_date[], int card_count) {
		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = getCardLogicValue(card_date[i]);
		}
		// 排序操作
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		do {
			sorted = true;
			for (int i = 0; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1])
						|| ((logic_value[i] == logic_value[i + 1]) && (card_date[i] < card_date[i + 1]))) {
					// 交换位置
					temp_date = card_date[i];
					card_date[i] = card_date[i + 1];
					card_date[i + 1] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[i + 1];
					logic_value[i + 1] = temp_date;
					sorted = false;
				}
			}
			last--;
		} while (sorted == false);

		return;
	}

	/**
	 * 获取卡牌的逻辑值
	 * 
	 * @param cardData
	 * @return
	 */
	public int getCardLogicValue(int cardData) {
		int cbCardValue = get_card_value(cardData);
		return (cbCardValue <= 2 && cbCardValue > 0) ? (cbCardValue + 13) : cbCardValue;
	}

	/**
	 * 获取卡牌的实际值
	 * 
	 * @param card
	 * @return
	 */
	public static int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	public int getMaxSingleCardValue(int cards[], int cardCount) {
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cards, cardCount, analyseResult);
		int card = 0;
		if (analyseResult.cbCardCount[1] > 0 || analyseResult.cbCardCount[2] > 0 || analyseResult.cbCardCount[3] > 0) {
			card = analyseResult.cbCardDatas[1][1];
			card = getCardLogicValue(card) > getCardLogicValue(analyseResult.cbCardDatas[2][1]) ? card
					: analyseResult.cbCardDatas[2][1];
			card = getCardLogicValue(card) > getCardLogicValue(analyseResult.cbCardDatas[3][1]) ? card
					: analyseResult.cbCardDatas[3][1];
		}
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	/**
	 * 获取花色
	 * 
	 * @param card
	 * @return
	 */
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	/**
	 * 删除扑克
	 * 
	 * @param cards
	 *            手牌
	 * @param card_count
	 *            手牌数
	 * @param remove_cards
	 *            待删除卡牌
	 * @param remove_count
	 *            待删除卡牌数
	 * @return
	 */
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		if (card_count < remove_count) {
			return false;
		}

		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[Constants_YYQF.HAND_CARD];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < remove_count; i++) {
			for (int j = 0; j < card_count; j++) {
				if (remove_cards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}
		// 成功判断
		if (cbDeleteCount != remove_count) {
			return false;
		}
		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0) {
				cards[cbCardPos++] = cbTempCardData[i];
			}
		}

		return true;
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	public int get_xi_qian_num(int cbCardData[][], int cbCardCount[]) {
		int num = 0;

		for (int i = 0; i < 3; i++) {
			tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(cbCardData[i], cbCardCount[i], card_index);
			for (int j = 0; j < cbCardCount[i];) {
				int index = switch_card_to_idnex(cbCardData[i][j]);
				if (card_index.card_index[index] > 6) {
					num++;
				}
				if (card_index.card_index[index] > 0) {
					j += card_index.card_index[index];
				} else {
					j++;
				}
			}
		}

		return num;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			int index = getCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	public int switch_card_to_idnex(int card) {
		int index = getCardLogicValue(card) - 3;
		return index;
	}
}
