/**
 * 
 */
package com.cai.game.dzd;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.dzd.DZDConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.google.common.collect.Lists;

//分析结构
class TagAnalyseResult {
	int cbCardCount[] = new int[DZDConstants.BOM_EIGHT + 1]; // 1-8的具体数值表示相同牌的数量.
																// 比如有4个对子、则下标2的值为4
	int cbCardDatas[][] = new int[DZDConstants.BOM_EIGHT + 1][DZDConstants.ONE_COLOR_CARD_COUNT + 1]; // 第一维度表示同牌数量索引、第二维度表示同牌卡牌值
	int cardDatas[][] = new int[DZDConstants.BOM_EIGHT + 1][DZDConstants.HAND_CARD + 1]; // 每张牌

	public void reset() {
		Arrays.fill(cbCardCount, 0);
		for (int i = 1; i <= DZDConstants.BOM_EIGHT; i++) {
			Arrays.fill(cbCardDatas[i], 0);
			Arrays.fill(cardDatas[i], 0);
			cbCardDatas[i][0] = Integer.MAX_VALUE;
			cardDatas[i][0] = Integer.MAX_VALUE;
		}
	}
};

// 出牌结果
class TagOutCardResult {
	int cbCardCount; // 扑克数目
	int cbResultCard[] = new int[DZDConstants.HAND_CARD]; // 结果扑克

	public void reset() {
		cbCardCount = 0;
		Arrays.fill(cbResultCard, 0);
	}
};

class tagOutCardTypeResult {
	int cbCardType[] = new int[GameConstants.MAX_TYPE_COUNT]; // 扑克类型
	int cbCardTypeCount;
	int cbEachHandCardCount[] = new int[GameConstants.MAX_TYPE_COUNT];// 每手个数
	int cbCardData[][] = new int[GameConstants.MAX_TYPE_COUNT][DZDConstants.HAND_CARD];// 扑克数据

	public tagOutCardTypeResult() {
		cbCardTypeCount = 0;
		Arrays.fill(cbCardType, 0);
		Arrays.fill(cbEachHandCardCount, 0);
		for (int i = 0; i < GameConstants.MAX_TYPE_COUNT; i++) {
			for (int j = 0; j < DZDConstants.HAND_CARD; j++) {
				cbCardData[i][j] = 0;
			}
		}
	}

	public void reset() {
		cbCardTypeCount = 0;
		Arrays.fill(cbCardType, 0);
		Arrays.fill(cbEachHandCardCount, 0);
		for (int i = 0; i < GameConstants.MAX_TYPE_COUNT; i++) {
			for (int j = 0; j < DZDConstants.HAND_CARD; j++) {
				cbCardData[i][j] = 0;
			}
		}
	}
};

public class DZDGameLogic {

	public int _game_rule_index; // 游戏规则

	public static final int MARK = 0 | (1 << 5) | (1 << 10) | (1 << 13);

	public DZDGameLogic() {

	}

	public static void main(String[] args) {
		DZDGameLogic logic = new DZDGameLogic();
		int[] test = { 34, 49, 61, 61, 29, 13, 43, 27, 42, 10, 40, 40, 24, 8, 55, 54, 38, 53, 37, 21, 20, 4, 51, 35, 35, 19 };
		logic.sort_card_data_list(test, test.length);
		for (int i = 0; i < test.length; i++) {
			System.out.print(test[i] + " ");
		}
	}

	/**
	 * 获取卡牌类型
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbRealData
	 * @return
	 */
	public int getCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		switch (cbCardCount) {
		case 0: // 空牌
			return DZDConstants.ERROR;
		case 1: // 单牌
			return DZDConstants.SINGLE;
		case 2: // 对牌
			if (getCardLogicValue(cbCardData[0]) == getCardLogicValue(cbCardData[1])) {
				return DZDConstants.DOUBLE;
			}
			return DZDConstants.ERROR;
		}

		sort_card_data_list(cbCardData, cbCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbCardData, cbCardCount, analyseResult);

		if (cbCardCount <= 8 && cbCardCount >= 4 && analyseResult.cbCardCount[cbCardCount] == 1) { // 4-8张牌炸弹判断
			return cbCardCount;
		}
		if (0 == cbCardCount % 2 && cbCardCount / 2 == analyseResult.cbCardCount[DZDConstants.DOUBLE]) { // 连对
			int len = cbCardCount / 2;
			if (analyseResult.cbCardCount[2] >= len) {
				int last = len; // 最大的牌为2时不比较
				// 连对判断
				for (int i = 1; i <= last; i++) {
					int airplanceLen = 1;
					int value = getCardLogicValue(analyseResult.cbCardDatas[2][i]);
					for (int j = i + 1; j <= last; j++) {
						if (getCardLogicValue(analyseResult.cbCardDatas[2][j]) != (value - airplanceLen)
								|| getCardLogicValue(analyseResult.cbCardDatas[2][j]) > 15) { // 不连续
							break;
						}
						airplanceLen++;
					}
					value = 0;
					int add = airplanceLen;
					if (airplanceLen >= len) {
						return DZDConstants.DOUBLE;
					}
					i += add - 1;
				}
			}
		}
		if (0 == cbCardCount % DZDConstants.THREE && cbCardCount / DZDConstants.THREE == analyseResult.cbCardCount[DZDConstants.THREE]) { // 三条
			int len = cbCardCount / DZDConstants.THREE;
			int three = DZDConstants.THREE;
			if (analyseResult.cbCardCount[three] >= len) {
				int last = 1;
				// 飞机判断
				for (int i = analyseResult.cbCardCount[three]; i >= last; i--) {
					int airplanceLen = 1;
					int value = getCardLogicValue(analyseResult.cbCardDatas[three][i]);
					for (int j = i - 1; j >= last; j--) {
						if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != value + airplanceLen
								|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
							break;
						}
						airplanceLen++;
					}
					value = 0;
					int add = airplanceLen;
					if (airplanceLen >= len) { // 三张可以压
						List<Integer> temp = Lists.newArrayList();
						for (int k = 0; k < len; k++) { // 加入三条的牌
							for (int z = 0; z < 3; z++) {
								temp.add(analyseResult.cbCardDatas[three][i - k - value]);
							}
						}
						return DZDConstants.THREE;
					}
					i -= (add - 1);
				}
			}
		}

		return DZDConstants.ERROR;
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
			for (int j = 0; j < cbSameCount; j++) {
				analyseResult.cardDatas[cbSameCount][analyseResult.cbCardCount[cbSameCount] * cbSameCount + j] = cbCardData[i + j];
			}
			analyseResult.cbCardDatas[cbSameCount][++analyseResult.cbCardCount[cbSameCount]] = cbCardData[i];

			i += cbSameCount - 1;
		}
		return;
	}

	// 对比扑克
	public boolean compareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = getCardType(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = getCardType(cbFirstCard, cbFirstCount, cbFirstCard);

		// 类型判断
		if (cbNextType == DZDConstants.ERROR) {
			return false;
		}

		if (cbFirstType == cbNextType) {
			return getCardLogicValue(cbNextCard[0]) > getCardLogicValue(cbFirstCard[0]);
		}
		if ((cbFirstType < 4 || cbFirstType > 8) && (cbNextType >= 4 && cbNextType <= 8)) { // 第一个牌型不是炸弹、第二个牌型是炸弹
			return true;
		}
		if ((cbFirstType >= 4 && cbFirstType <= 8) && (cbNextType >= 4 && cbNextType <= 8)) {
			return cbNextType > cbFirstType;
		}

		return false;
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
	public boolean searchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount, int cardType) {
		if (cardType == DZDConstants.ERROR) {
			return false;
		}
		// 搜索炸弹
		List<Integer> card = getBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, cardType);
		if (card.size() > 0) {
			return true;
		}
		if (cardType == DZDConstants.SINGLE) {
			return getSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount) != DZDConstants.ERROR;
		}
		if (cardType == DZDConstants.DOUBLE) {
			return getDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount).size() > 0;
		}
		if (cardType == DZDConstants.THREE) {
			return getThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount).size() > 0;
		}

		return false;
	}

	/**
	 * 判断是否有压牌
	 * 
	 * @return
	 */
	public List<Integer> getOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount, int cardType) {
		if (cardType == DZDConstants.ERROR || cbHandCardData.length <= 0) {
			return Collections.emptyList();
		}

		List<Integer> outCardData = getBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, cardType);
		if (outCardData.size() > 0) {
			return outCardData;
		}
		if (cardType == DZDConstants.SINGLE) {
			int card = getSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
			if (card != DZDConstants.ERROR) {
				outCardData = Lists.newArrayList();
				outCardData.add(card);
				return outCardData;
			}
		}
		if (cardType == DZDConstants.DOUBLE) {
			outCardData = getDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		if (cardType == DZDConstants.THREE) {
			outCardData = getThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * 搜索三张、飞机
	 * 
	 * @return
	 */
	public List<Integer> getThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {
		if (cbHandCardCount < cbTurnCardCount) {
			return Collections.emptyList();
		}

		sort_card_data_list(cbTurnCardData, cbTurnCardCount);
		TagAnalyseResult turnResult = new TagAnalyseResult();
		analyseCardData(cbTurnCardData, cbTurnCardCount, turnResult);

		sort_card_data_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);
		// 飞机判断: 手上的三条数量大于出牌的三条数量
		int len = cbTurnCardCount / DZDConstants.THREE;
		int three = DZDConstants.THREE;
		if (analyseResult.cbCardCount[three] >= len) {
			int last = 1;
			// 飞机判断
			for (int i = analyseResult.cbCardCount[three]; i >= last; i--) {
				int airplanceLen = 1;
				int value = getCardLogicValue(analyseResult.cbCardDatas[three][i]);
				for (int j = i - 1; j >= last; j--) {
					if (getCardLogicValue(analyseResult.cbCardDatas[three][j]) != value + airplanceLen
							|| getCardLogicValue(analyseResult.cbCardDatas[three][j]) > 15) { // 不连续
						break;
					}
					airplanceLen++;
				}
				value = 0;
				int add = airplanceLen;
				while (airplanceLen > 0
						&& getCardLogicValue(analyseResult.cbCardDatas[three][i - value]) <= getCardLogicValue(turnResult.cbCardDatas[three][len])) {
					value++;
					airplanceLen--;
				}
				if (airplanceLen >= len
						&& getCardLogicValue(analyseResult.cbCardDatas[three][i - value]) > getCardLogicValue(turnResult.cbCardDatas[three][len])) { // 三张可以压
					List<Integer> temp = Lists.newArrayList();
					for (int k = 0; k < len; k++) { // 加入三条的牌
						for (int z = 0; z < 3; z++) {
							temp.add(analyseResult.cbCardDatas[three][i - k - value]);
						}
					}
					return temp;
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
	public List<Integer> getDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {
		if (cbHandCardCount < cbTurnCardCount) {
			return Collections.emptyList();
		}

		sort_card_data_list(cbTurnCardData, cbTurnCardCount);

		sort_card_data_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);

		// 合并手上的三条跟对子(三条也可以作为对子处理)
		int d = analyseResult.cbCardCount[2], t = analyseResult.cbCardCount[3];
		int doubleCount = d + t; // 手上对子数
		int doubleCard[] = new int[doubleCount];
		for (int i = 0; i < doubleCount; i++) {
			if (getCardLogicValue(analyseResult.cbCardDatas[2][d]) > getCardLogicValue(analyseResult.cbCardDatas[3][t])) {
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
					if (getCardLogicValue(doubleCard[j]) != value + airplanceLen || getCardLogicValue(doubleCard[j]) > 15) { // 不连续
						break;
					}
					airplanceLen++;
				}
				value = 0;
				int add = airplanceLen;
				while (airplanceLen > 0 && getCardLogicValue(doubleCard[i + value]) <= getCardLogicValue(cbTurnCardData[cbTurnCardCount - 1])) {
					value++;
					airplanceLen--;
				}
				if (airplanceLen >= len && getCardLogicValue(doubleCard[i + value]) > getCardLogicValue(cbTurnCardData[cbTurnCardCount - 1])) {
					List<Integer> temp = Lists.newArrayList();
					for (int k = 0; k < len; k++) { // 加入对子牌
						temp.add(doubleCard[i + value + k]);
						temp.add(doubleCard[i + value + k]);
					}
					return temp;
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

		sort_card_data_list(cbHandCardData, cbHandCardCount);
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(cbHandCardData, cbHandCardCount, analyseResult);
		// 合并手上的三条、对子、单牌
		int s = analyseResult.cbCardCount[1], d = analyseResult.cbCardCount[2], t = analyseResult.cbCardCount[3];
		int singleCount = s + d + t; // 手上单牌数
		int singleCard[] = new int[singleCount];
		for (int i = 0; i < singleCount; i++) {
			if (getCardLogicValue(analyseResult.cbCardDatas[2][d]) > getCardLogicValue(analyseResult.cbCardDatas[3][t])) {
				if (getCardLogicValue(analyseResult.cbCardDatas[1][s]) > getCardLogicValue(analyseResult.cbCardDatas[3][t])) {
					singleCard[i] = analyseResult.cbCardDatas[3][t];
					t--;
				} else {
					singleCard[i] = analyseResult.cbCardDatas[1][s];
					s--;
				}
			} else {
				if (getCardLogicValue(analyseResult.cbCardDatas[1][s]) > getCardLogicValue(analyseResult.cbCardDatas[2][d])) {
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

		return DZDConstants.ERROR;
	}

	/**
	 * 获取炸弹
	 * 
	 * @return
	 */
	public List<Integer> getBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount, int cardType) {
		if (cbHandCardCount < 4) { // 4张以下不能构成炸弹
			return Collections.emptyList();
		}
		if ((cardType >= 4 && cardType <= 12) && cbHandCardCount < cardType) { // 如果上一手出的炸弹、且该玩家手牌数不够炸弹长度则肯定不能大
			return Collections.emptyList();
		}

		sort_card_data_list(cbHandCardData, cbHandCardCount);
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
				if (cbSameCount > 2 * 4) {
					temp.clear();
					return temp;
				}
				if (cardType > 8 || cardType < 4) { // 上一手出牌不是炸弹
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
	 * @param card_data
	 * @param card_count
	 * @return
	 */
	public void sort_card_data_list(int card_data[], int card_count) {
		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = getCardLogicValue(card_data[i]);
		}
		// 排序操作
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		do {
			sorted = true;
			for (int i = 0; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1]) || ((logic_value[i] == logic_value[i + 1]) && (card_data[i] < card_data[i + 1]))) {
					// 交换位置
					temp_date = card_data[i];
					card_data[i] = card_data[i + 1];
					card_data[i + 1] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[i + 1];
					logic_value[i + 1] = temp_date;
					sorted = false;
				}
			}
			last--;
		} while (sorted == false);

		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analyseCardData(card_data, card_count, analyseResult);
		int temp[] = new int[card_count];
		int number = 0;
		for (int i = 8; i > 0; i--) {
			for (int j = 0; j < analyseResult.cbCardCount[i]; j++) {
				for (int k = 0; k < i; k++) {
					temp[number++] = analyseResult.cardDatas[i][j * i + k];
				}
			}
		}
		for (int i = 0; i < card_count; i++) {
			card_data[i] = temp[i];
		}

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
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
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
		int cbTempCardData[] = new int[DZDConstants.HAND_CARD];

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

	public boolean remove(int cards[], int card_count, int remove_cards[], int remove_count) {
		if (card_count < remove_count) {
			return false;
		}

		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[DZDConstants.HAND_CARD];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
			cards[i] = 0;
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

	public int countCard(int cardDatas[], int cardCount, int cardData) {
		int result = 0;
		for (int i = 0; i < cardCount; i++) {
			if (getCardLogicValue(cardDatas[i]) == getCardLogicValue(cardData)) {
				result++;
			}
		}
		return result;
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

}
