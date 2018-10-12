/**
 * 
 */
package com.cai.game.qjqf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.QJQFConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;

//经典斗地主
//分析结构
class TagAnalyseResult {
	int cbEightCount; // 八张数目
	int cbSevenCount; // 七张数目
	int cbSixCount; // 六张数目
	int cbFiveCount; // 五张数目
	int cbFourCount; // 四张数目
	int cbThreeCount; // 三张数目
	int cbDoubleCount; // 两张数目
	int cbSignedCount; // 单张数目
	int cbEightCardData[] = new int[QJQFConstants.HAND_CARD]; // 八张扑克
	int cbSevenCardData[] = new int[QJQFConstants.HAND_CARD]; // 七张扑克
	int cbSixCardData[] = new int[QJQFConstants.HAND_CARD]; // 六张扑克
	int cbFiveCardData[] = new int[QJQFConstants.HAND_CARD]; // 五张扑克
	int cbFourCardData[] = new int[QJQFConstants.HAND_CARD]; // 四张扑克
	int cbThreeCardData[] = new int[QJQFConstants.HAND_CARD]; // 三张扑克
	int cbDoubleCardData[] = new int[QJQFConstants.HAND_CARD]; // 两张扑克
	int cbSignedCardData[] = new int[QJQFConstants.HAND_CARD]; // 单张扑克

	public TagAnalyseResult() {
		// reset();
	}

	public void reset() {
		cbEightCount = 0;
		cbSevenCount = 0;
		cbSixCount = 0;
		cbFiveCount = 0;
		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		cbSignedCount = 0;
		Arrays.fill(cbEightCardData, 0);
		Arrays.fill(cbSevenCardData, 0);
		Arrays.fill(cbSixCardData, 0);
		Arrays.fill(cbFiveCardData, 0);
		Arrays.fill(cbFourCardData, 0);
		Arrays.fill(cbThreeCardData, 0);
		Arrays.fill(cbDoubleCardData, 0);
		Arrays.fill(cbSignedCardData, 0);
	}
};

// 出牌结果
class TagOutCardResult {
	int cbCardCount; // 扑克数目
	int cbResultCard[] = new int[QJQFConstants.HAND_CARD]; // 结果扑克

	public TagOutCardResult() {
		// reset();
	}

	public void reset() {
		cbCardCount = 0;
		Arrays.fill(cbResultCard, 0);
	}
};

class tagOutCardTypeResult {
	int cbCardType[] = new int[GameConstants.MAX_TYPE_COUNT]; // 扑克类型
	int cbCardTypeCount;
	int cbEachHandCardCount[] = new int[GameConstants.MAX_TYPE_COUNT];// 每手个数
	int cbCardData[][] = new int[GameConstants.MAX_TYPE_COUNT][QJQFConstants.HAND_CARD];// 扑克数据

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

	public void Reset() {
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
	public int card_index[] = new int[GameConstants.WSK_MAX_INDEX];
	public int card_data[][] = new int[GameConstants.WSK_MAX_INDEX][GameConstants.MAX_PDK_COUNT_EQ];

	public tagAnalyseIndexResult() {
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void Reset() {
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
};

public class QJQFGameLogic {

	public int _game_rule_index; // 游戏规则
	public int _laizi = GameConstants.INVALID_CARD;// 癞子牌数据

	public static final int MARK = 0 | (1 << 5) | (1 << 10) | (1 << 13);

	public QJQFGameLogic() {

	}

	/**
	 * 检查5 10 k炸弹
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public static boolean checkBomFiveTenK(int cbCardData[], int cbCardCount) {
		if (cbCardCount != 3) {
			return false;
		}

		int value = 0;
		for (int i = 0; i < cbCardCount; i++) {
			value |= (1 << get_card_value(cbCardData[i]));
		}

		return value == MARK;
	}

	public static void main(String[] args) {
		int[] test = { 0x15, 0x1a, 0x1d };
		System.out.println(checkBomFiveTenK(test, 3));
	}

	// 获取类型
	public int getCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return QJQFConstants.ERROR;
		}
		case 1: // 单牌
		{
			return QJQFConstants.SINGLE;
		}
		case 2: // 对牌
		{
			// 牌型判断
			if (GetCardLogicValue(cbCardData[0]) == GetCardLogicValue(cbCardData[1]))
				return QJQFConstants.DOUBLE;

			return QJQFConstants.ERROR;
		}
		case 3: // 对牌
		{
			// 牌型判断
			if (GetCardLogicValue(cbCardData[0]) == GetCardLogicValue(cbCardData[1])
					&& GetCardLogicValue(cbCardData[2]) == GetCardLogicValue(cbCardData[1]))
				return QJQFConstants.THREE;

			if (checkBomFiveTenK(cbCardData, cbCardCount)) {
				if (checkSameColor(cbCardData, cbCardCount)) {
					return QJQFConstants.BOM_COLOR_FIVE_TEN_K;
				}
				return QJQFConstants.BOM_FIVE_TEN_K;
			}
			break;
		}
		case 4: // 天王炸
		{
			boolean bMissileCard = true;
			for (int cbCardIdx = 0; cbCardIdx < cbCardCount; ++cbCardIdx) {
				if (this.get_card_color(cbCardData[cbCardIdx]) != 4) {
					bMissileCard = false;
					break;
				}
			}
			if (bMissileCard)
				return QJQFConstants.BOM_KING;
		}
		}

		// 分析扑克
		TagAnalyseResult analyseResult = new TagAnalyseResult();
		analysebCardData(cbCardData, cbCardCount, analyseResult);

		// 炸弹判断
		if (4 <= cbCardCount && cbCardCount <= 8) {
			// 牌型判断
			if ((analyseResult.cbFourCount == 1) && (cbCardCount == 4)) {
				return QJQFConstants.BOM_FOUR;

			}
			if ((analyseResult.cbFiveCount == 1) && (cbCardCount == 5)) {
				return QJQFConstants.BOM_BIG;

			}
			if ((analyseResult.cbSixCount == 1) && (cbCardCount == 6)) {
				return QJQFConstants.BOM_BIG;
			}
			if ((analyseResult.cbSevenCount == 1) && (cbCardCount == 7)) {
				return QJQFConstants.BOM_BIG;

			}
			if ((analyseResult.cbEightCount == 1) && (cbCardCount == 8)) {
				return QJQFConstants.BOM_EIGHT;

			}
		}

		return QJQFConstants.ERROR;
	}

	private boolean checkSameColor(int[] cbCardData, int cbCardCount) {
		int color = get_card_color(cbCardData[0]);
		for (int i = 1; i < cbCardCount; i++) {
			if (color != get_card_color(cbCardData[i])) {
				return false;
			}
		}
		return true;
	}

	// 分析扑克
	public void analysebCardData(int cbCardData[], int cbCardCount, TagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.reset();

		// 扑克分析
		for (int i = 0; i < cbCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			// 设置结果
			switch (cbSameCount) {
			case 1: // 单张
			{
				int cbIndex = AnalyseResult.cbSignedCount++;
				AnalyseResult.cbSignedCardData[cbIndex * cbSameCount] = cbCardData[i];
				break;
			}
			case 2: // 两张
			{
				int cbIndex = AnalyseResult.cbDoubleCount++;
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbDoubleCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				break;
			}
			case 3: // 三张
			{
				int cbIndex = AnalyseResult.cbThreeCount++;
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				break;
			}
			case 4: // 四张
			{
				int cbIndex = AnalyseResult.cbFourCount++;
				if (cbCardCount != 5) {
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
					AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				} else {
					AnalyseResult.cbThreeCardData[cbIndex * cbSameCount] = cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
					AnalyseResult.cbThreeCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				}
				break;
			}
			case 5: // 五张
			{
				int cbIndex = AnalyseResult.cbFiveCount++;
				AnalyseResult.cbFiveCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				break;
			}
			case 6: // 六张
			{
				int cbIndex = AnalyseResult.cbSixCount++;
				AnalyseResult.cbSixCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
				break;
			}
			case 7: // 七张
			{
				int cbIndex = AnalyseResult.cbSevenCount++;
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
				AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
				break;
			}
			case 8: // 八张
			{
				int cbIndex = AnalyseResult.cbEightCount++;
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 4] = cbCardData[i + 4];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 5] = cbCardData[i + 5];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 6] = cbCardData[i + 6];
				AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 7] = cbCardData[i + 7];
				break;
			}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return;
	}

	// 对比扑克
	public boolean compareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = getCardType(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = getCardType(cbFirstCard, cbFirstCount, cbNextCard);

		// 类型判断
		if (cbNextType == QJQFConstants.ERROR)
			return false;

		// 炸弹判断
		if ((cbFirstType < QJQFConstants.BOM_FIVE_TEN_K) && (cbNextType >= QJQFConstants.BOM_FIVE_TEN_K))
			return true;
		if ((cbFirstType >= QJQFConstants.BOM_FIVE_TEN_K) && (cbNextType <= QJQFConstants.BOM_FIVE_TEN_K))
			return false;

		// 如果都是炸弹，比谁的炸弹大 ，相同的炸弹，下面最对比
		if (cbFirstType >= QJQFConstants.BOM_FIVE_TEN_K && cbNextType >= QJQFConstants.BOM_FIVE_TEN_K
				&& cbFirstType != cbNextType)
			return cbFirstType < cbNextType;

		// 开始对比
		switch (cbNextType) {
		case QJQFConstants.SINGLE:
		case QJQFConstants.DOUBLE:
		case QJQFConstants.THREE:
		case QJQFConstants.BOM_FOUR: {
			if (cbFirstCount != cbNextCount) {
				return false;
			}
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case QJQFConstants.BOM_FIVE_TEN_K: {
			// 这两个相同的话，谁先出先赢
			return false;
		}
		case QJQFConstants.BOM_COLOR_FIVE_TEN_K: {
			// 获取数值
			int nextColor = get_card_color(cbNextCard[0]);
			int firstColor = get_card_color(cbFirstCard[0]);

			// 对比扑克
			return nextColor > firstColor;
		}
		case QJQFConstants.BOM_EIGHT:
		case QJQFConstants.BOM_BIG: {
			// 数目判断
			if (cbNextCount != cbFirstCount)
				return cbNextCount > cbFirstCount;

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		}

		return false;
	}

	// 判断是否有压牌
	// 出牌搜索
	public boolean searchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {

		// // 获取出牌类型
		int card_type = getCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
		if (card_type == QJQFConstants.ERROR)
			return false;

		// 搜索炸弹
		if (searchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, card_type)) {
			return true;
		}
		if (card_type == QJQFConstants.SINGLE) {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == QJQFConstants.DOUBLE) {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == QJQFConstants.THREE) {
			return searchThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}

		return false;
	}

	// 判断是否有压牌
	// 出牌搜索
	public List<Integer> getOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, boolean must_max) {
		List<Integer> outCardData = new ArrayList<>();
		// // 获取出牌类型
		int card_type = getCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
		if (card_type == QJQFConstants.ERROR || cbHandCardData.length <= 0) {
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);

			if (must_max) {
				int index = this.switch_card_to_idnex(cbHandCardData[0]);
				for (int i = 0; i < hand_index.card_index[index]; i++) {
					outCardData.add(hand_index.card_data[index][i]);
				}
			} else {
				int index = this.switch_card_to_idnex(cbHandCardData[cbHandCardCount - 1]);
				for (int i = 0; i < hand_index.card_index[index]; i++) {
					outCardData.add(hand_index.card_data[index][i]);
				}
			}

			return outCardData;
		}
		if (must_max) {
			if (card_type == QJQFConstants.SINGLE) {
				outCardData = getSingleCard_must_max(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
				if (outCardData.size() > 0) {
					return outCardData;
				}
			}
		} else {
			if (card_type == QJQFConstants.SINGLE) {
				outCardData = getSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
				if (outCardData.size() > 0) {
					return outCardData;
				}
			}
		}

		if (card_type == QJQFConstants.DOUBLE) {
			outCardData = getDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		if (card_type == QJQFConstants.THREE) {
			outCardData = getThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
			if (outCardData.size() > 0) {
				return outCardData;
			}
		}
		// 搜索炸弹
		outCardData = getBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, card_type);
		if (outCardData.size() > 0) {
			return outCardData;
		}
		return outCardData;
	}

	// 搜索三张
	public boolean searchThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount) {
			return false;
		}

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCard[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCard[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 3 && GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	// 搜索三张
	public List<Integer> getThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		List<Integer> outCardData = new ArrayList<>();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
			if (index > turn_index && hand_index.card_index[index] == 3) {
				for (int j = 0; j < hand_index.card_index[index]; j++) {
					outCardData.add(hand_index.card_data[index][j]);
				}
				return outCardData;
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		outCardData.clear();
		return outCardData;
	}

	// 搜索对子
	public boolean SearchDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCard[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCard[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 2 && GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	public List<Integer> getSingleCard_must_max(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		List<Integer> outCardData = new ArrayList<>();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
			if (hand_index.card_index[index] >= 4) {
				for (int j = 0; j < hand_index.card_index[index]; j++) {
					outCardData.add(hand_index.card_data[i][j]);
				}
				return outCardData;
			} else if (index > turn_index) {
				if ((index == 13 || index == 14)) {
					if (hand_index.card_index[13] + hand_index.card_index[14] != 4) {
						outCardData.add(cbHandCardData[i]);
						return outCardData;
					} else {
						for (int j = 0; j < hand_index.card_index[13]; j++) {
							outCardData.add(hand_index.card_data[i][j]);
						}
						for (int j = 0; j < hand_index.card_index[14]; j++) {
							outCardData.add(hand_index.card_data[i][j]);
						}
						return outCardData;
					}
				} else {
					outCardData.add(cbHandCardData[i]);
					return outCardData;
				}
			}
			if (hand_index.card_index[index] > 0) {
				i += hand_index.card_index[index];
			} else {
				i++;
			}
		}
		outCardData.clear();
		return outCardData;
	}

	public List<Integer> getSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		List<Integer> outCardData = new ArrayList<>();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
			if (index > turn_index && hand_index.card_index[index] == 1) {
				if ((index == 13 || index == 14)) {
					if (hand_index.card_index[13] + hand_index.card_index[14] != 4) {
						outCardData.add(cbHandCardData[i]);
						return outCardData;
					}
				} else {
					outCardData.add(cbHandCardData[i]);
					return outCardData;
				}
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
			if (index > turn_index && hand_index.card_index[index] < 4) {
				if ((index == 13 || index == 14)) {
					if (hand_index.card_index[13] + hand_index.card_index[14] != 4) {
						outCardData.add(cbHandCardData[i]);
						return outCardData;
					}
				} else {
					outCardData.add(cbHandCardData[i]);
					return outCardData;
				}
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		outCardData.clear();
		return outCardData;
	}

	// 搜索对子
	public List<Integer> getDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		List<Integer> outCardData = new ArrayList<>();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
			if (index > turn_index && hand_index.card_index[index] == 2) {

				if ((index == 13 || index == 14)) {
					if (hand_index.card_index[13] + hand_index.card_index[14] != 4) {
						for (int j = 0; j < hand_index.card_index[index]; j++) {
							outCardData.add(hand_index.card_data[index][j]);
						}
						return outCardData;
					}
				} else {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						outCardData.add(hand_index.card_data[index][j]);
					}
					return outCardData;
				}

			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
			if (index > turn_index && hand_index.card_index[index] < 4 && hand_index.card_index[index] >= 2) {
				if ((index == 13 || index == 14)) {
					if (hand_index.card_index[13] + hand_index.card_index[14] != 4) {
						for (int j = 0; j < 2; j++) {
							outCardData.add(hand_index.card_data[index][j]);
						}
						return outCardData;
					}
				} else {
					for (int j = 0; j < 2; j++) {
						outCardData.add(hand_index.card_data[index][j]);
					}
					return outCardData;
				}
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		outCardData.clear();
		return outCardData;
	}

	// 搜索单张
	public boolean SearchSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		for (int i = 0; i < cbHandCardCount; ++i) {
			if (GetCardLogicValue(cbTmpCard[i]) > GetCardLogicValue(cbTurnCardData[0])) {
				return true;
			}
		}

		return false;
	}

	// 分析炸弹
	public boolean searchBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int card_type) {

		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		if (cbHandCardCount < 3)
			return false;

		int smallKingCount = 0;
		int bigKingCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);
			if (cbTmpCardData[i] == 0x4F) {
				bigKingCount++;
			} else if (cbTmpCardData[i] == 0x4E) {
				smallKingCount++;
			}

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;
				if (cbTmpCardData[j] == 0x4F) {
					bigKingCount++;
				} else if (cbTmpCardData[j] == 0x4E) {
					smallKingCount++;
				}

				// 设置变量
				cbSameCount++;
			}
			if (cbSameCount >= 4) {
				if (cbSameCount > 8) {
					return false;
				}

				if (card_type <= QJQFConstants.BOM_KING && cbSameCount == 8) {
					// 至尊 吃4王
					return true;
				} else if (card_type == QJQFConstants.BOM_KING) {
				} else if (card_type <= QJQFConstants.BOM_FIVE_TEN_K) {
					return true;
				} else if (cbSameCount > 4 && cbSameCount > cbTurnCardCount) {
					return true;
				} else if (cbSameCount > 4 && cbSameCount == cbTurnCardCount
						&& cbLogicValue > GetCardLogicValue(cbTurnCardData[0])) {
					return true;
				}
			}
			// 设置索引
			i += cbSameCount - 1;
		}

		// 双王炸弹 只有至尊比他大
		if (smallKingCount >= 2 && bigKingCount >= 2 && card_type != QJQFConstants.BOM_EIGHT) {
			return true;
		}

		// 这里还没处理 5 10 k炸弹的问题
		if (card_type >= QJQFConstants.BOM_BIG) {
			// 五十k肯定比大炸弹小
			return false;
		}

		// 5 10 k 数据 按花色存
		int[] colorMarks = new int[4];

		// 5 10 k 分析
		for (int i = 0; i < cbHandCardCount; i++) {
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);
			// 如果有 5 10 k 把对应花色的数据 的 数据位置为1
			if (cbLogicValue == 5 || cbLogicValue == 0x0a || cbLogicValue == 0x0d) {
				int color = get_card_color(cbTmpCardData[i]);
				colorMarks[color] = colorMarks[color] | (1 << cbLogicValue);
			}
		}

		for (int i = colorMarks.length - 1; i >= 0; i--) {
			if (colorMarks[i] == MARK) {
				// 有 5 10 k同花 判断大小
				return card_type < QJQFConstants.BOM_COLOR_FIVE_TEN_K
						|| (card_type == QJQFConstants.BOM_COLOR_FIVE_TEN_K && get_card_color(cbTurnCardData[0]) < i);
			}

		}

		// 没有 5 10 k 同花 看看有没 混色 5 10 k
		int colorMark = 0;
		for (int i = colorMarks.length - 1; i >= 0; i--) {
			colorMark |= colorMarks[i];
		}

		if (colorMark == MARK) {
			// 有 5 10 k 对比一下大小
			return card_type < QJQFConstants.BOM_FIVE_TEN_K;

		}

		return false;
	}

	// 分析炸弹
	public List<Integer> getBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int card_type) {
		List<Integer> temp = new ArrayList<>();
		if (cbHandCardCount < 3)
			return temp;

		List<Integer> smallKing = new ArrayList<>();
		List<Integer> bigKing = new ArrayList<>();

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbHandCardData[i]);
			temp.clear();

			temp.add(cbHandCardData[i]);
			if (cbHandCardData[i] == 0x4F) {
				bigKing.add(cbHandCardData[i]);
			} else if (cbHandCardData[i] == 0x4E) {
				smallKing.add(cbHandCardData[i]);
			}

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbHandCardData[j]) != cbLogicValue)
					break;

				if (cbHandCardData[i] == 0x4F) {
					bigKing.add(cbHandCardData[j]);
				} else if (cbHandCardData[i] == 0x4E) {
					smallKing.add(cbHandCardData[j]);
				}

				temp.add(cbHandCardData[j]);
				// 设置变量
				cbSameCount++;
			}
			if (cbSameCount >= 4) {
				if (cbSameCount > 8) {
					temp.clear();
					return temp;
				}
				if (card_type == QJQFConstants.BOM_KING && cbSameCount == 8) {
					// 至尊 吃4王
					return temp;
				} else if (card_type <= QJQFConstants.BOM_FIVE_TEN_K) {
					return temp;
				} else if (cbSameCount == cbTurnCardCount && cbLogicValue > GetCardLogicValue(cbTurnCardData[0])) {
					return temp;
				} else if (cbSameCount > 4 && cbSameCount > cbTurnCardCount && card_type != QJQFConstants.BOM_KING) {
					return temp;
				} else if (cbSameCount > 4 && cbSameCount == cbTurnCardCount
						&& cbLogicValue > GetCardLogicValue(cbTurnCardData[0])) {
					return temp;
				}
			}
			// 设置索引
			i += cbSameCount - 1;
		}
		temp.clear();

		// 双王炸弹 只有至尊比他大
		if (smallKing.size() == 2 && bigKing.size() == 2 && card_type != QJQFConstants.BOM_EIGHT) {
			smallKing.addAll(bigKing);
			return smallKing;
		}

		// 这里还没处理 5 10 k炸弹的问题
		if (card_type >= QJQFConstants.BOM_BIG) {
			// 五十k肯定比大炸弹小
			return temp;
		}

		// 5 10 k 数据 按花色存
		int[] colorMarks = new int[4];

		List<Integer> five = new ArrayList<>();
		List<Integer> ten = new ArrayList<>();
		List<Integer> k = new ArrayList<>();
		// 5 10 k 分析
		for (int i = 0; i < cbHandCardCount; i++) {
			int cbLogicValue = GetCardLogicValue(cbHandCardData[i]);
			// 如果有 5 10 k 把对应花色的数据 的 数据位置为1
			if (cbLogicValue == 5) {
				five.add(cbHandCardData[i]);
			} else if (cbLogicValue == 0x0a) {
				ten.add(cbHandCardData[i]);
			} else if (cbLogicValue == 0x0d) {
				k.add(cbHandCardData[i]);
			}

			if (cbLogicValue == 5 || cbLogicValue == 0x0a || cbLogicValue == 0x0d) {
				int color = get_card_color(cbHandCardData[i]);
				colorMarks[color] = colorMarks[color] | (1 << cbLogicValue);
			}
		}

		for (int i = colorMarks.length - 1; i >= 0; i--) {
			if (colorMarks[i] == MARK) {
				// 有 5 10 k同花 判断大小
				if (card_type < QJQFConstants.BOM_COLOR_FIVE_TEN_K
						|| (card_type == QJQFConstants.BOM_COLOR_FIVE_TEN_K && get_card_color(cbTurnCardData[0]) < i)) {
					int fiveCard = (i << 4) + 5;
					temp.add(fiveCard);
					temp.add((i << 4) + 0x0a);
					temp.add((i << 4) + 0x0d);
					return temp;
				}
			}
		}

		// 没有 5 10 k 同花 看看有没 混色 5 10 k
		int colorMark = 0;
		for (int i = colorMarks.length - 1; i >= 0; i--) {
			colorMark |= colorMarks[i];
		}

		if (colorMark == MARK) {
			// 有 5 10 k 对比一下大小
			if (card_type < QJQFConstants.BOM_FIVE_TEN_K) {
				temp.add(five.get(0));
				temp.add(ten.get(0));
				temp.add(k.get(0));
				return temp;
			}

		}

		return temp;
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
	public void sort_card_date_list(int card_date[], int card_count, int sort_type) {

		// 按炸弹排序，510K牌前面
		if (sort_type == GameConstants.WSK_ST_510K) {
			int zheng_510K[] = new int[card_count];
			int zheng_card_num = 0;
			int fu_510K[] = new int[card_count];
			int fu_card_num = 0;
			tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(card_date, card_count, card_index);
			// 先提取510K炸弹
			if (card_index.card_index[2] > 0 && card_index.card_index[7] > 0 && card_index.card_index[10] > 0
					&& card_index.card_index[2] < 4 && card_index.card_index[7] < 4 && card_index.card_index[10] < 4) {
				for (int i = 0; i < card_index.card_index[2]; i++) {
					for (int j = 0; j < card_index.card_index[7]; j++) {
						for (int x = 0; x < card_index.card_index[10]; x++) {
							if (card_index.card_data[2][i] == 0 || card_index.card_data[7][j] == 0
									|| card_index.card_data[10][x] == 0) {
								continue;
							}

							if (get_card_color(card_index.card_data[2][i]) == get_card_color(card_index.card_data[7][j])
									&& get_card_color(card_index.card_data[2][i]) == get_card_color(
											card_index.card_data[10][x])) {
								zheng_510K[zheng_card_num++] = card_index.card_data[2][i];
								zheng_510K[zheng_card_num++] = card_index.card_data[7][j];
								zheng_510K[zheng_card_num++] = card_index.card_data[10][x];
								for (int y = 0; y < card_count; y++) {
									if (card_date[y] == card_index.card_data[2][i]) {
										card_date[y] = 0;
										break;
									}
								}
								for (int y = 0; y < card_count; y++) {
									if (card_date[y] == card_index.card_data[7][j]) {
										card_date[y] = 0;
										break;
									}
								}
								for (int y = 0; y < card_count; y++) {
									if (card_date[y] == card_index.card_data[10][x]) {
										card_date[y] = 0;
										break;
									}
								}
								card_index.card_data[2][i] = 0;
								card_index.card_data[7][j] = 0;
								card_index.card_data[10][x] = 0;
							}
						}
					}
				}

				for (int i = 0; i < card_index.card_index[2]; i++) {
					for (int j = 0; j < card_index.card_index[7]; j++) {
						for (int x = 0; x < card_index.card_index[10]; x++) {
							if (card_index.card_data[2][i] == 0 || card_index.card_data[7][j] == 0
									|| card_index.card_data[10][x] == 0) {
								continue;
							}
							fu_510K[fu_card_num++] = card_index.card_data[2][i];
							fu_510K[fu_card_num++] = card_index.card_data[7][j];
							fu_510K[fu_card_num++] = card_index.card_data[10][x];
							for (int y = 0; y < card_count; y++) {
								if (card_date[y] == card_index.card_data[2][i]) {
									card_date[y] = 0;
									break;
								}
							}
							for (int y = 0; y < card_count; y++) {
								if (card_date[y] == card_index.card_data[7][j]) {
									card_date[y] = 0;
									break;
								}
							}
							for (int y = 0; y < card_count; y++) {
								if (card_date[y] == card_index.card_data[10][x]) {
									card_date[y] = 0;
									break;
								}
							}
							card_index.card_data[2][i] = 0;
							card_index.card_data[7][j] = 0;
							card_index.card_data[10][x] = 0;
						}
					}
				}
			}
			this.sort_card_date_list(card_date, card_count, GameConstants.WSK_ST_ORDER);
			//
			card_index.Reset();
			AnalysebCardDataToIndex(card_date, card_count, card_index);
			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[index[i]] > card_index.card_index[index[j]]) {
						int temp = index[j];
						index[j] = index[i];
						index[i] = temp;
					} else if (card_index.card_index[index[i]] == card_index.card_index[index[j]]) {
						if (index[i] > index[j]) {
							int temp = index[j];
							index[j] = index[i];
							index[i] = temp;
						}
					}
				}
			}

			int sort_num = 0;
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					if (card_index.card_index[index[i]] < 4) {
						card_date[sort_num++] = card_index.card_data[index[i]][j];
					}

				}
			}
			this.sort_card_date_list(card_date, sort_num, GameConstants.WSK_ST_ORDER);
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					if (card_index.card_index[index[i]] >= 4) {
						card_date[sort_num++] = card_index.card_data[index[i]][j];
					}

				}
			}
			for (int i = 0; i < fu_card_num; i++) {
				card_date[sort_num++] = fu_510K[i];
			}
			for (int i = 0; i < zheng_card_num; i++) {
				card_date[sort_num++] = zheng_510K[i];
			}
			return;
		}

		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++)
			logic_value[i] = GetCardLogicValue(card_date[i]);
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

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.WSK_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] == 0) {
				continue;
			}
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		// TagAnalyseResult Result = new TagAnalyseResult();
		// AnalysebCardData(card_date, card_count, Result);
		//
		// int index = 0;
		// if (type == GameConstants.PDK_CT_SINGLE || type ==
		// GameConstants.PDK_CT_SINGLE_LINE || type ==
		// GameConstants.PDK_CT_HONG_HUA_SHUN) {
		// for (int i = 0; i < Result.cbSignedCount; i++) {
		// card_date[index++] = Result.cbSignedCardData[i];
		// }
		// } else if (type == GameConstants.PDK_CT_DOUBLE || type ==
		// GameConstants.PDK_CT_DOUBLE_LINE) {
		// for (int i = 0; i < Result.cbDoubleCount; i++) {
		// for (int j = 0; j < 2; j++) {
		// card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
		// }
		// }
		// } else if (type == GameConstants.PDK_CT_THREE || type ==
		// GameConstants.PDK_CT_THREE_TAKE_ONE || type ==
		// GameConstants.PDK_CT_THREE_TAKE_TWO
		// || type == GameConstants.PDK_CT_PLANE || type ==
		// GameConstants.PDK_CT_PLANE_LOST) {
		// for (int i = 0; i < Result.cbThreeCount; i++) {
		// for (int j = 0; j < 3; j++) {
		// card_date[index++] = Result.cbThreeCardData[i * 3 + j];
		// }
		// }
		// for (int i = 0; i < Result.cbSignedCount; i++) {
		// card_date[index++] = Result.cbSignedCardData[i];
		// }
		// for (int i = 0; i < Result.cbDoubleCount; i++) {
		// for (int j = 0; j < 2; j++) {
		// card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
		// }
		// }
		// } else if (type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE || type ==
		// GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO
		// || type == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE) {
		// for (int i = 0; i < Result.cbFourCount; i++) {
		// for (int j = 0; j < 4; j++) {
		// card_date[index++] = Result.cbFourCardData[i * 4 + j];
		// }
		// }
		// for (int i = 0; i < Result.cbThreeCount; i++) {
		// for (int j = 0; j < 3; j++) {
		// card_date[index++] = Result.cbThreeCardData[i * 3 + j];
		// }
		// }
		// for (int i = 0; i < Result.cbSignedCount; i++) {
		// card_date[index++] = Result.cbSignedCardData[i];
		// }
		// for (int i = 0; i < Result.cbDoubleCount; i++) {
		// for (int j = 0; j < 2; j++) {
		// card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
		// }
		// }
		// } else if (type == GameConstants.PDK_CT_BOMB_CARD) {
		// for (int i = 0; i < Result.cbEightCount; i++) {
		// for (int j = 0; j < 8; j++) {
		// card_date[index++] = Result.cbEightCardData[i * 8 + j];
		// }
		// }
		// for (int i = 0; i < Result.cbSevenCount; i++) {
		// for (int j = 0; j < 7; j++) {
		// card_date[index++] = Result.cbSevenCardData[i * 7 + j];
		// }
		// }
		// for (int i = 0; i < Result.cbSixCount; i++) {
		// for (int j = 0; j < 6; j++) {
		// card_date[index++] = Result.cbSixCardData[i * 6 + j];
		// }
		// }
		// for (int i = 0; i < Result.cbFiveCount; i++) {
		// for (int j = 0; j < 5; j++) {
		// card_date[index++] = Result.cbFiveCardData[i * 5 + j];
		// }
		// }
		// }
		//
		// return;
	}

	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 4)
			return cbCardValue + 2;
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
	}

	// 获取数值
	public static int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[QJQFConstants.HAND_CARD];

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
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	// 放走包赔
	public boolean fang_zou_bao_pei(int cbCardData[], int cbCardCount, int cbOutCardData[]) {
		// 分析扑克

		if (GetAllBomCard(cbCardData, cbCardCount) > 0) {
			return true;
		}

		if (GetAllThreeCard(cbCardData, cbCardCount) > 0) {
			return true;
		}
		if (GetAllDoubleCard(cbCardData, cbCardCount) > 0) {
			return true;
		}

		if (GetCardLogicValue(cbCardData[0]) != GetCardLogicValue(cbOutCardData[0])) {
			return true;
		}

		return false;
	}

	// 获取炸弹
	public int GetAllBomCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;

		if (cbHandCardCount < 2)
			return 0;

		// 双王炸弹
		if (0x4F == cbTmpCardData[0] && 0x4E == cbTmpCardData[1]) {
			cbBomCardCount += 2;
		}

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (4 == cbSameCount) {
				cbBomCardCount += 4;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbBomCardCount;
	}

	// 获取三条
	public int GetAllThreeCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbThreeCardCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 3) {
				cbThreeCardCount += 3;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbThreeCardCount;
	}

	// 分析对子
	public int GetAllDoubleCard(int cbHandCardData[], int cbHandCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbDoubleCardCount = 0;

		// 扑克分析
		for (int i = 0; i < cbHandCardCount; i++) {
			// 变量定义
			int cbSameCount = 1;
			int cbLogicValue = GetCardLogicValue(cbTmpCardData[i]);

			// 搜索同牌
			for (int j = i + 1; j < cbHandCardCount; j++) {
				// 获取扑克
				if (GetCardLogicValue(cbTmpCardData[j]) != cbLogicValue)
					break;

				// 设置变量
				cbSameCount++;
			}

			if (cbSameCount >= 2) {
				cbDoubleCardCount += 2;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbDoubleCardCount;
	}

	public int switch_idnex_to_data(int index) {
		return index + 3;
	}

	public int switch_card_to_idnex(int card) {
		int index = GetCardLogicValue(card) - 3;
		return index;
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

}
