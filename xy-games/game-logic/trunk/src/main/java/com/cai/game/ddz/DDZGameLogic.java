/**
 * 
 */
package com.cai.game.ddz;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.game.ddz.data.tagAnalyseIndexResult_DDZ;

//经典斗地主
//分析结构
class tagAnalyseResult {
	int cbEightCount; // 八张数目
	int cbSevenCount; // 七张数目
	int cbSixCount; // 六张数目
	int cbFiveCount; // 五张数目
	int cbFourCount; // 四张数目
	int cbThreeCount; // 三张数目
	int cbDoubleCount; // 两张数目
	int cbSignedCount; // 单张数目
	int cbEightCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 八张扑克
	int cbSevenCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 七张扑克
	int cbSixCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 六张扑克
	int cbFiveCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 五张扑克
	int cbFourCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 四张扑克
	int cbThreeCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 三张扑克
	int cbDoubleCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 两张扑克
	int cbSignedCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 单张扑克

	public tagAnalyseResult() {
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

	public void Reset() {
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
class tagOutCardResult {
	int cbCardCount; // 扑克数目
	int cbResultCard[] = new int[GameConstants.DDZ_MAX_COUNT_JD]; // 结果扑克

	public tagOutCardResult() {
		cbCardCount = 0;
		Arrays.fill(cbResultCard, 0);
	}

	public void Reset() {
		cbCardCount = 0;
		Arrays.fill(cbResultCard, 0);
	}
};

class tagOutCardTypeResult {
	int cbCardType[] = new int[GameConstants.MAX_TYPE_COUNT]; // 扑克类型
	int cbCardTypeCount; // 牌型数目
	int cbEachHandCardCount[] = new int[GameConstants.MAX_TYPE_COUNT];// 每手个数
	int cbCardData[][] = new int[GameConstants.MAX_TYPE_COUNT][GameConstants.DDZ_MAX_COUNT_JD];// 扑克数据

	public tagOutCardTypeResult() {
		cbCardTypeCount = 0;

		Arrays.fill(cbCardType, 0);
		Arrays.fill(cbEachHandCardCount, 0);
		for (int i = 0; i < GameConstants.MAX_TYPE_COUNT; i++) {
			Arrays.fill(cbCardData[i], 0);
		}

	}

	public void Reset() {
		cbCardTypeCount = 0;

		Arrays.fill(cbCardType, 0);
		Arrays.fill(cbEachHandCardCount, 0);
		for (int i = 0; i < GameConstants.MAX_TYPE_COUNT; i++) {
			Arrays.fill(cbCardData[i], 0);
		}
	}
};

public class DDZGameLogic {

	private int cbIndexCount = 5;
	public int _game_rule_index; // 游戏规则
	public int _laizi = GameConstants.INVALID_CARD;// 癞子牌数据
	public int[] magic_card;
	private int magic_card_count;

	public DDZGameLogic() {
		magic_card = new int[GameConstants.MAX_COUNT];
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		int nlaizicount = this.GetLaiZiCount(cbRealData, cbCardCount);
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return GameConstants.DDZ_CT_ERROR;
		}
		case 1: // 单牌
		{
			return GameConstants.DDZ_CT_SINGLE;
		}
		case 2: // 对牌
		{
			// 牌型判断
			if (GetCardLogicValue(cbCardData[0]) == GetCardLogicValue(cbCardData[1]))
				return GameConstants.DDZ_CT_DOUBLE;
			boolean bMissileCard = true;
			for (int cbCardIdx = 0; cbCardIdx < cbCardCount; ++cbCardIdx) {
				int cbCardColor = get_card_color(cbCardData[cbCardIdx]);
				if (this.get_card_color(cbCardData[cbCardIdx]) != 0x04) {
					bMissileCard = false;
					break;
				}
			}
			if (bMissileCard)
				return GameConstants.DDZ_CT_MISSILE_CARD;

			return GameConstants.DDZ_CT_ERROR;
		}
		case 4: // 天王炸
		{
			boolean bMissileCard = true;
			for (int cbCardIdx = 0; cbCardIdx < cbCardCount; ++cbCardIdx) {
				if (this.get_card_color(cbCardData[cbCardIdx]) != 0x40) {
					bMissileCard = false;
					break;
				}
			}
			if (bMissileCard)
				return GameConstants.DDZ_CT_MISSILE_CARD;
		}
		}

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbCardData, cbCardCount, AnalyseResult);

		// 炸弹判断
		if (4 >= cbCardCount && cbCardCount <= 8) {
			// 牌型判断
			if ((AnalyseResult.cbFourCount == 1) && (cbCardCount == 4)) {
				if (nlaizicount == 0) {
					return GameConstants.DDZ_CT_BOMB_CARD;
				} else if (nlaizicount == 4) {
					return GameConstants.DDZ_CT_MAGIC_BOOM;
				} else {
					return GameConstants.DDZ_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbFiveCount == 1) && (cbCardCount == 5)) {
				if (nlaizicount == 0) {
					return GameConstants.DDZ_CT_BOMB_CARD;
				} else {
					return GameConstants.DDZ_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbSixCount == 1) && (cbCardCount == 6)) {
				if (nlaizicount == 0) {
					return GameConstants.DDZ_CT_BOMB_CARD;
				} else {
					return GameConstants.DDZ_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbSevenCount == 1) && (cbCardCount == 7)) {
				if (nlaizicount == 0) {
					return GameConstants.DDZ_CT_BOMB_CARD;
				} else {
					return GameConstants.DDZ_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbEightCount == 1) && (cbCardCount == 8)) {
				if (nlaizicount == 0) {
					return GameConstants.DDZ_CT_BOMB_CARD;
				} else {
					return GameConstants.DDZ_CT_RUAN_BOMB;
				}
			}
		}
		if (AnalyseResult.cbFourCount > 0) {
			if (cbCardCount == 6)
				return GameConstants.DDZ_CT_FOUR_TAKE_ONE;
			if (cbCardCount == 8 && AnalyseResult.cbDoubleCount == 2)
				return GameConstants.DDZ_CT_FOUR_TAKE_TWO;

			if (AnalyseResult.cbThreeCount == AnalyseResult.cbFourCount * 2 + AnalyseResult.cbDoubleCount
					&& AnalyseResult.cbSignedCount == 0) {
				for (int i = 0; i < AnalyseResult.cbFourCount; i++) {
					AnalyseResult.cbDoubleCardData[AnalyseResult.cbDoubleCount * 2 + 0] = AnalyseResult.cbFourCardData[i
							* 4];
					AnalyseResult.cbDoubleCardData[AnalyseResult.cbDoubleCount * 2
							+ 1] = AnalyseResult.cbFourCardData[i * 4 + 1];
					AnalyseResult.cbDoubleCount++;
					AnalyseResult.cbDoubleCardData[AnalyseResult.cbDoubleCount * 2
							+ 0] = AnalyseResult.cbFourCardData[i * 4 + 2];
					AnalyseResult.cbDoubleCardData[AnalyseResult.cbDoubleCount * 2
							+ 1] = AnalyseResult.cbFourCardData[i * 4 + 3];
					AnalyseResult.cbDoubleCount++;
				}

			} else {
				for (int i = 0; i < AnalyseResult.cbFourCount; i++) {
					AnalyseResult.cbThreeCardData[AnalyseResult.cbThreeCount * 3
							+ 0] = AnalyseResult.cbFourCardData[i * 4 + 0];
					AnalyseResult.cbThreeCardData[AnalyseResult.cbThreeCount * 3
							+ 1] = AnalyseResult.cbFourCardData[i * 4 + 1];
					AnalyseResult.cbThreeCardData[AnalyseResult.cbThreeCount * 3
							+ 2] = AnalyseResult.cbFourCardData[i * 4 + 2];
					AnalyseResult.cbSignedCardData[(AnalyseResult.cbSignedCount)] = AnalyseResult.cbFourCardData[i * 4];
					AnalyseResult.cbThreeCount++;
					AnalyseResult.cbSignedCount++;
				}
				this.sort_card_date_list(AnalyseResult.cbThreeCardData, AnalyseResult.cbThreeCount * 3);
			}

			AnalyseResult.cbFourCount = 0;

		}
		// 三牌判断
		if (AnalyseResult.cbThreeCount > 0) {
			// 三条类型
			if (AnalyseResult.cbThreeCount == 1 && cbCardCount == 3)
				return GameConstants.DDZ_CT_THREE;
			if (AnalyseResult.cbThreeCount == 1 && cbCardCount == 4)
				return GameConstants.DDZ_CT_THREE_TAKE_ONE;
			if (AnalyseResult.cbThreeCount == 1 && cbCardCount == 5
					&& AnalyseResult.cbDoubleCount == AnalyseResult.cbThreeCount)
				return GameConstants.DDZ_CT_THREE_TAKE_TWO;

			// 连牌判断
			if (AnalyseResult.cbThreeCount > 1) {
				// 变量定义
				int CardData = AnalyseResult.cbThreeCardData[0];
				int cbFirstLogicValue = GetCardLogicValue(CardData);

				// 错误过虑
				if (cbFirstLogicValue >= 15)
					return GameConstants.DDZ_CT_ERROR;
				int ThreelinkCount = 0;
				int linkindex = 0;
				// 连牌判断
				for (int i = 0; i < AnalyseResult.cbThreeCount; i++) {
					ThreelinkCount++;
					linkindex++;
					if (ThreelinkCount * 3 == cbCardCount) {
						int CardDatatemp = AnalyseResult.cbThreeCardData[i * 3];
						if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + (ThreelinkCount - 1))) {
							ThreelinkCount = 0;
							linkindex = 0;
						} else {
							return GameConstants.DDZ_CT_THREE_LINE;
						}
					} else if (ThreelinkCount * 4 == cbCardCount) {
						int CardDatatemp = AnalyseResult.cbThreeCardData[i * 3];
						int value = GetCardLogicValue(CardDatatemp);
						if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + (ThreelinkCount - 1))) {
							ThreelinkCount = 0;
							linkindex = 0;
						} else {
							return GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE;
						}
					} else if (ThreelinkCount * 5 == cbCardCount && AnalyseResult.cbDoubleCount == ThreelinkCount) {
						int CardDatatemp = AnalyseResult.cbThreeCardData[i * 3];
						if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + (ThreelinkCount - 1))) {
							ThreelinkCount = 0;
							linkindex = 0;
						} else {
							return GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO;
						}
					}
					if (ThreelinkCount > 1) {
						int CardDatatemp = AnalyseResult.cbThreeCardData[i * 3];
						if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + (ThreelinkCount - 1))) {
							cbFirstLogicValue = GetCardLogicValue(
									AnalyseResult.cbThreeCardData[(ThreelinkCount - 1) * 3]);
							ThreelinkCount = 1;
							linkindex = 0;

						}
					}

				}
			}

			return GameConstants.DDZ_CT_ERROR;
		}

		// 两张类型
		if (AnalyseResult.cbDoubleCount >= 3) {
			// 变量定义
			int CardData = AnalyseResult.cbDoubleCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);

			// 错误过虑
			if (cbFirstLogicValue >= 15)
				return GameConstants.DDZ_CT_ERROR;

			// 连牌判断
			for (int i = 1; i < AnalyseResult.cbDoubleCount; i++) {
				int CardDatatemp = AnalyseResult.cbDoubleCardData[i * 2];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + i))
					return GameConstants.DDZ_CT_ERROR;
			}

			// 二连判断
			if ((AnalyseResult.cbDoubleCount * 2) == cbCardCount)
				return GameConstants.DDZ_CT_DOUBLE_LINE;

			return GameConstants.DDZ_CT_ERROR;
		}

		// 单张判断
		if ((AnalyseResult.cbSignedCount >= 5) && (AnalyseResult.cbSignedCount == cbCardCount)) {
			// 变量定义
			int CardData = AnalyseResult.cbSignedCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);

			// 错误过虑
			if (cbFirstLogicValue >= 15)
				return GameConstants.DDZ_CT_ERROR;

			// 连牌判断
			for (int i = 1; i < AnalyseResult.cbSignedCount; i++) {
				int CardDatatemp = AnalyseResult.cbSignedCardData[i];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + i))
					return GameConstants.DDZ_CT_ERROR;
			}

			return GameConstants.DDZ_CT_SINGLE_LINE;
		}

		return GameConstants.DDZ_CT_ERROR;
	}

	// 分析扑克
	public void AnalysebCardData(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.Reset();

		// 扑克分析
		for (int i = 0; i < cbCardCount; i++) {
			// 变量定义
			int cbSameCount = 1, cbCardValueTemp = 0;
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
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
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

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		tagAnalyseResult Result = new tagAnalyseResult();
		AnalysebCardData(card_date, card_count, Result);

		int index = 0;
		if (type == GameConstants.DDZ_CT_SINGLE || type == GameConstants.DDZ_CT_SINGLE_LINE) {
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
		} else if (type == GameConstants.DDZ_CT_DOUBLE || type == GameConstants.DDZ_CT_DOUBLE_LINE) {
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.DDZ_CT_THREE || type == GameConstants.DDZ_CT_THREE_TAKE_ONE
				|| type == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE || type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO
				|| type == GameConstants.DDZ_CT_THREE_TAKE_TWO || type == GameConstants.DDZ_CT_THREE_LINE) {

			if (Result.cbThreeCount == Result.cbFourCount * 2) {
				for (int i = 0; i < Result.cbFourCount; i++) {
					Result.cbDoubleCardData[Result.cbDoubleCount * 2 + 0] = Result.cbFourCardData[i * 4];
					Result.cbDoubleCardData[Result.cbDoubleCount * 2 + 1] = Result.cbFourCardData[i * 4 + 1];
					Result.cbDoubleCount++;
					Result.cbDoubleCardData[Result.cbDoubleCount * 2 + 0] = Result.cbFourCardData[i * 4 + 2];
					Result.cbDoubleCardData[Result.cbDoubleCount * 2 + 1] = Result.cbFourCardData[i * 4 + 3];
					Result.cbDoubleCount++;
				}

			} else {
				for (int i = 0; i < Result.cbFourCount; i++) {
					Result.cbThreeCardData[Result.cbThreeCount * 3 + 0] = Result.cbFourCardData[i * 4 + 0];
					Result.cbThreeCardData[Result.cbThreeCount * 3 + 1] = Result.cbFourCardData[i * 4 + 1];
					Result.cbThreeCardData[Result.cbThreeCount * 3 + 2] = Result.cbFourCardData[i * 4 + 2];
					Result.cbSignedCardData[(Result.cbSignedCount)] = Result.cbFourCardData[i * 4 + 3];
					Result.cbThreeCount++;
					Result.cbSignedCount++;
				}
				this.sort_card_date_list(Result.cbThreeCardData, Result.cbThreeCount * 3);
			}
			// 连牌判断
			int value_add = 0;
			int CardData = Result.cbThreeCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);
			int nLink_Three_Count = 0;
			int threeindex = 0;
			for (int i = 0; i < Result.cbThreeCount; i++) {
				if (nLink_Three_Count * 5 > card_count) {
					break;
				}
				int CardDatatemp = Result.cbThreeCardData[i * 3];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + value_add)) {
					if (nLink_Three_Count * 4 == card_count) {

						break;
					}
					if (nLink_Three_Count * 5 == card_count && (nLink_Three_Count == Result.cbDoubleCount
							|| nLink_Three_Count == Result.cbFourCount * 2)) {
						break;
					}
					cbFirstLogicValue = GetCardLogicValue(Result.cbThreeCardData[i * 3]);
					nLink_Three_Count = 1;
					value_add = 1;
					threeindex = i;
					continue;
				}
				value_add++;
				nLink_Three_Count++;

			}
			for (int i = threeindex; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}

			for (int i = 0; i < threeindex; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}

		} else if (type == GameConstants.DDZ_CT_FOUR_TAKE_ONE || type == GameConstants.DDZ_CT_FOUR_TAKE_TWO) {
			for (int i = 0; i < Result.cbFourCount; i++) {
				for (int j = 0; j < 4; j++) {
					card_date[index++] = Result.cbFourCardData[i * 4 + j];
				}
			}
			for (int i = 0; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.DDZ_CT_BOMB_CARD) {
			for (int i = 0; i < Result.cbEightCount; i++) {
				for (int j = 0; j < 8; j++) {
					card_date[index++] = Result.cbEightCardData[i * 8 + j];
				}
			}
			for (int i = 0; i < Result.cbSevenCount; i++) {
				for (int j = 0; j < 7; j++) {
					card_date[index++] = Result.cbSevenCardData[i * 7 + j];
				}
			}
			for (int i = 0; i < Result.cbSixCount; i++) {
				for (int j = 0; j < 6; j++) {
					card_date[index++] = Result.cbSixCardData[i * 6 + j];
				}
			}
			for (int i = 0; i < Result.cbFiveCount; i++) {
				for (int j = 0; j < 5; j++) {
					card_date[index++] = Result.cbFiveCardData[i * 5 + j];
				}
			}
		}
		return;
	}

	// 赖子数目
	public int GetLaiZiCount(int cbHandCardData[], int cbHandCardCount) {
		if (_laizi == GameConstants.INVALID_CARD) {
			return 0;
		}
		int bLaiZiCount = 0;
		for (int i = 0; i < cbHandCardCount; i++) {
			if (GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(_laizi))
				bLaiZiCount++;
		}

		return bLaiZiCount;
	}

	// 获取底牌类型
	public int GetDipaiType(int cbCardData[], int cbCardCount) {
		if (cbCardCount != 3) {
			return GameConstants.DDZ_CT_DI_PAI_ERROR;
		}
		// 变量定义
		boolean cbSameColor = true, bLineCard = true;
		int cbFirstColor = get_card_color(cbCardData[0]);
		int cbFirstValue = GetCardLogicValue(cbCardData[0]);

		// 牌形分析
		for (int i = 1; i < cbCardCount; i++) {
			// 数据分析
			if (get_card_color(cbCardData[i]) != cbFirstColor)
				cbSameColor = false;
			if (cbFirstValue != (GetCardLogicValue(cbCardData[i]) + i))
				bLineCard = false;

			// 结束判断
			if ((cbSameColor == false) && (bLineCard == false))
				break;
		}
		// 顺金类型
		if ((cbSameColor) && (bLineCard))
			return GameConstants.DDZ_CT_DI_PAI_TONGHUA_SUNN_ZI;
		// 金花类型
		if ((cbSameColor) && (!bLineCard))
			return GameConstants.DDZ_CT_DI_PAI_TONGHUA;
		// 顺子
		if ((!cbSameColor) && (bLineCard) && (cbFirstColor != 4)) {
			return GameConstants.DDZ_CT_DI_PAI_SUNN_ZI;
		}

		int fristvalue = GetCardLogicValue(cbCardData[0]);
		// 同牌分析
		for (int i = 0; i < cbCardCount; i++) {
			if (GetCardLogicValue(cbCardData[i]) != fristvalue) {
				break;
			}
			if (i == cbCardCount - 1) {
				return GameConstants.DDZ_CT_DI_PAI_BAOZI;
			}
		}
		// 王牌分析
		int nKingCount = 0;
		for (int i = 0; i < cbCardCount; i++) {
			int cbCardColor = get_card_color(cbCardData[i]);
			if (cbCardColor == 0x04) {
				nKingCount++;
			}
		}
		if (nKingCount == 1) {
			return GameConstants.DDZ_CT_DI_PAI_DAN_WANG;
		} else if (nKingCount == 2) {
			return GameConstants.DDZ_CT_DI_PAI_DUI_WANG;
		}

		return GameConstants.DDZ_CT_DI_PAI_ERROR;
	}

	// 是否必叫
	public boolean is_must_call(int cbCardData[], int cbCardCount) {
		// 王牌分析
		int nKingCount = 0;
		int count_2 = 0;
		for (int i = 0; i < cbCardCount; i++) {
			int cbCardColor = get_card_color(cbCardData[i]);
			if (cbCardColor == 0x04) {
				nKingCount++;
			}
			if (GetCardLogicValue(cbCardData[i]) == 15) {
				count_2++;
			}
		}
		if (nKingCount >= 2) {
			return true;
		}
		if (count_2 >= 4) {
			return true;
		}

		return false;
	}

	public int get_type_times(int type) {
		switch (type) {
		case GameConstants.DDZ_CT_DI_PAI_ERROR:
			return 1;
		case GameConstants.DDZ_CT_DI_PAI_BAOZI:
		case GameConstants.DDZ_CT_DI_PAI_DUI_WANG:
			return 4;
		case GameConstants.DDZ_CT_DI_PAI_SUNN_ZI:
		case GameConstants.DDZ_CT_DI_PAI_TONGHUA:
		case GameConstants.DDZ_CT_DI_PAI_TONGHUA_SUNN_ZI:
			return 3;
		case GameConstants.DDZ_CT_DI_PAI_DAN_WANG:
			return 2;
		default:
			return 1;
		}
	}

	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount, int first_type,
			int next_type) {
		return false;
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = GetCardType(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount, cbFirstCard);

		// 类型判断
		if (cbNextType == GameConstants.DDZ_CT_ERROR)
			return false;
		if (cbNextType == GameConstants.DDZ_CT_MISSILE_CARD)
			return true;
		if (cbFirstType == GameConstants.DDZ_CT_MISSILE_CARD)
			return false;

		// 炸弹判断
		if ((cbFirstType != GameConstants.DDZ_CT_BOMB_CARD) && (cbNextType == GameConstants.DDZ_CT_BOMB_CARD))
			return true;
		if ((cbFirstType == GameConstants.DDZ_CT_BOMB_CARD) && (cbNextType != GameConstants.DDZ_CT_BOMB_CARD))
			return false;

		// 规则判断
		if ((cbFirstType != cbNextType)
				|| (cbFirstType != GameConstants.DDZ_CT_BOMB_CARD && cbFirstCount != cbNextCount))
			return false;

		// 开始对比
		switch (cbNextType) {
		case GameConstants.DDZ_CT_SINGLE:
		case GameConstants.DDZ_CT_DOUBLE:
		case GameConstants.DDZ_CT_THREE:
		case GameConstants.DDZ_CT_SINGLE_LINE:
		case GameConstants.DDZ_CT_DOUBLE_LINE:
		case GameConstants.DDZ_CT_THREE_LINE:
		case GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE:
		case GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO: {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.DDZ_CT_THREE_TAKE_ONE:
		case GameConstants.DDZ_CT_THREE_TAKE_TWO: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCard, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbThreeCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbThreeCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.DDZ_CT_FOUR_TAKE_ONE:
		case GameConstants.DDZ_CT_FOUR_TAKE_TWO: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCard, cbFirstCount, FirstResult);

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbFourCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbFourCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.DDZ_CT_BOMB_CARD: {
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

	public void GetCardTypeLaizi(int cbCardData[], int cbCardCount, tagOutCardTypeResult out_card_type_result) {
		int bLaiZiCount = GetLaiZiCount(cbCardData, cbCardCount);

		int tempCardData[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			tempCardData[i] = cbCardData[i];
		}
		for (int i = 0; i < cbCardCount; i++) {
			if (GetCardLogicValue(cbCardData[i]) == GetCardLogicValue(this._laizi)) {
				for (int valueone = 1; valueone < 14; valueone++) {
					// 一个癞子变牌
					tempCardData[i] = (0x0F - valueone) % 13 + 1;
					if (bLaiZiCount >= 2) {
						for (int j = i + 1; j < cbCardCount; j++) {
							if (GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(this._laizi)) {
								for (int valuetwo = 1; valuetwo < 14; valuetwo++) {
									// 两个癞子变牌
									tempCardData[j] = (0x0F - valuetwo) % 13 + 1;
									if (bLaiZiCount >= 3) {
										for (int x = j + 1; x < cbCardCount; x++) {
											if (GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(this._laizi)) {
												for (int valuethree = 1; valuethree < 14; valuethree++) {
													// 三个癞子变牌
													tempCardData[x] = (0x0F - valuethree) % 13 + 1;
													if (bLaiZiCount >= 4) {
														for (int y = x + 1; y < cbCardCount; y++) {
															if (GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(
																	this._laizi)) {
																for (int valuefour = 1; valuefour < 14; valuefour++) {
																	// 四个癞子变牌
																	tempCardData[y] = (0x0F - valuefour) % 13 + 1;
																	int card_type = this.GetCardType(tempCardData,
																			cbCardCount, cbCardData);
																	if (cbCardCount == 4) {
																		// 四癞子炸弹直接返回
																		int count = out_card_type_result.cbCardTypeCount;
																		out_card_type_result.cbCardType[count] = GameConstants.DDZ_CT_MAGIC_BOOM;
																		for (int card_index = 0; card_index < cbCardCount; card_index++) {
																			out_card_type_result.cbCardData[count][card_index] = cbCardData[card_index];
																		}
																		out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
																		out_card_type_result.cbCardTypeCount++;
																		return;
																	}
																	if (cbCardCount > 4
																			&& card_type == GameConstants.DDZ_CT_BOMB_CARD) {
																		continue;
																	}
																	if (card_type != GameConstants.DDZ_CT_ERROR) {
																		int count = out_card_type_result.cbCardTypeCount;
																		out_card_type_result.cbCardType[count] = card_type;
																		for (int card_index = 0; card_index < cbCardCount; card_index++) {
																			out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
																		}
																		out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
																		out_card_type_result.cbCardTypeCount++;
																	}
																}
															}
														}
													}
													int card_type = this.GetCardType(tempCardData, cbCardCount,
															cbCardData);
													if (cbCardCount > 4
															&& card_type == GameConstants.DDZ_CT_BOMB_CARD) {
														continue;
													}
													if (card_type != GameConstants.DDZ_CT_ERROR) {
														int count = out_card_type_result.cbCardTypeCount;
														out_card_type_result.cbCardType[count] = card_type;
														for (int card_index = 0; card_index < cbCardCount; card_index++) {
															out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
														}
														out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
														out_card_type_result.cbCardTypeCount++;
													}
												}
											}
										}
									}
									int card_type = this.GetCardType(tempCardData, cbCardCount, cbCardData);
									if (cbCardCount > 4 && card_type == GameConstants.DDZ_CT_BOMB_CARD) {
										continue;
									}
									if (card_type != GameConstants.DDZ_CT_ERROR) {
										int count = out_card_type_result.cbCardTypeCount;
										out_card_type_result.cbCardType[count] = card_type;
										for (int card_index = 0; card_index < cbCardCount; card_index++) {
											out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
										}
										out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
										out_card_type_result.cbCardTypeCount++;
									}
								}
							}
						}
					}
					int card_type = this.GetCardType(tempCardData, cbCardCount, cbCardData);
					if (cbCardCount > 4 && card_type == GameConstants.DDZ_CT_BOMB_CARD) {
						continue;
					}
					if (card_type != GameConstants.DDZ_CT_ERROR) {
						int count = out_card_type_result.cbCardTypeCount;
						out_card_type_result.cbCardType[count] = card_type;
						for (int card_index = 0; card_index < cbCardCount; card_index++) {
							out_card_type_result.cbCardData[count][card_index] = tempCardData[card_index];
						}
						out_card_type_result.cbEachHandCardCount[count] = cbCardCount;
						out_card_type_result.cbCardTypeCount++;
					}
				}
			}
		}

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

	// 混乱准备
	private static void random_cards_good(int card_data[], int return_cards[], int card_count) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0, brand_once_count = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			brand_once_count = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (2)) + 2;
			if (bPosition + brand_once_count > card_count - bRandCount) {
				brand_once_count = card_count - bRandCount - bPosition;
			}
			for (int i = 0; i < brand_once_count; i++) {
				return_cards[bRandCount++] = card_data[bPosition + i];
				card_data[bPosition + i] = card_data[card_count - bRandCount];
			}

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
			if (card_date[i] == 0x5E) {
				logic_value[i] = 20;
			} else {
				logic_value[i] = GetCardLogicValue(card_date[i]);
			}

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

	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 0x04 || cbCardColor == 0x05 || (cbCardValue == 14 || cbCardValue == 15))
			return cbCardValue + 2;
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
	}

	// 获取数值
	public int get_card_value(int card) {
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
		int cbTempCardData[] = new int[card_count];

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

	public boolean SearchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int turntype) {
		return false;
	}

	// 判断是否有压牌
	// 出牌搜索
	public boolean SearchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {

		// 获取出牌类型
		int card_type = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
		if (card_type == GameConstants.DDZ_CT_MISSILE_CARD)
			return false;

		if (SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
			return true;
		}

		// 搜索顺子
		if (card_type == GameConstants.DDZ_CT_SINGLE_LINE) {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_DOUBLE_LINE) {
			return SearchDoubleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE_LINE || card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE
				|| card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO) {
			return SearchThreeLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_SINGLE) {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_DOUBLE) {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE) {
			return SearchThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE_TAKE_ONE) {
			return SearchThreeTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE_TAKE_TWO) {
			return SearchThreeTakeTwoCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}

		return false;
	}

	// 判断是否有压牌
	// 出牌搜索，六盘水
	public boolean SearchOutCard_LPS(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		// 获取出牌类型
		int card_type = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
		if (card_type == GameConstants.DDZ_CT_MISSILE_CARD)
			return false;

		if (SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount)) {
			return true;
		}

		// 搜索顺子
		if (card_type == GameConstants.DDZ_CT_SINGLE_LINE) {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_DOUBLE_LINE) {
			return SearchDoubleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE_LINE || card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE
				|| card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO) {
			return SearchThreeLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_SINGLE) {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_DOUBLE) {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE) {
			return SearchThreeCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE_TAKE_ONE) {
			return SearchThreeTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		if (card_type == GameConstants.DDZ_CT_THREE_TAKE_TWO) {
			return SearchThreeTakeTwoCard_LPS(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}

		return false;
	}

	// 搜索三张
	public boolean SearchThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
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
	public boolean SearchThreeTakeOneCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount) {
			return false;
		}
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbHandCardData, cbHandCardCount, AnalyseResult);
		if (AnalyseResult.cbThreeCount <= 0) {
			return false;
		}
		for (int i = 0; i < AnalyseResult.cbThreeCount; i++) {
			if (GetCardLogicValue(AnalyseResult.cbThreeCardData[i * 3]) > GetCardLogicValue(cbTurnCardData[0])) {
				if (cbHandCardCount >= 4) {
					return true;
				}
			}
		}
		return false;
	}

	// 搜索三张
	public boolean SearchThreeTakeTwoCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount) {
			return false;
		}
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbHandCardData, cbHandCardCount, AnalyseResult);
		if (AnalyseResult.cbThreeCount < 0) {
			return false;
		}
		for (int i = 0; i < AnalyseResult.cbThreeCount; i++) {
			if (GetCardLogicValue(AnalyseResult.cbThreeCardData[i * 3]) > GetCardLogicValue(cbTurnCardData[0])) {
				if (AnalyseResult.cbDoubleCount > 0) {
					return true;
				}
				if (AnalyseResult.cbThreeCount >= 1) {
					return true;
				}
			}
		}
		return false;
	}

	// 六盘水搜索三代二
	public boolean SearchThreeTakeTwoCard_LPS(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount) {
			return false;
		}
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbHandCardData, cbHandCardCount, AnalyseResult);
		if (AnalyseResult.cbThreeCount < 0) {
			return false;
		}
		for (int i = 0; i < AnalyseResult.cbThreeCount; i++) {
			if (GetCardLogicValue(AnalyseResult.cbThreeCardData[i * 3]) > GetCardLogicValue(cbTurnCardData[0])) {
				if (AnalyseResult.cbDoubleCount > 0) {
					return true;
				}
				if (AnalyseResult.cbThreeCount > 1) {
					return true;
				}
			}
		}
		return false;
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

	// 搜索飞机
	public boolean SearchThreeLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		if (cbHandCardCount < cbTurnCardCount)
			return false;
		// 连牌判断
		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbLeftCardCount = cbHandCardCount - cbFirstCard;
		boolean bFindThreeLine = true;
		int cbThreeLineCount = 0;
		int cbThreeLineCard[] = new int[20];
		// 开始判断
		while (bFindThreeLine) {
			int cbLastCard = cbTmpCard[cbFirstCard];
			int cbSameCount = 1;
			cbThreeLineCount = 0;
			bFindThreeLine = false;
			for (int i = cbFirstCard + 1; i < cbLeftCardCount + cbFirstCard; ++i) {
				// 搜索同牌
				while (i < cbLeftCardCount + cbFirstCard
						&& get_card_value(cbLastCard) == get_card_value(cbTmpCard[i])) {
					++cbSameCount;
					++i;
				}
				if (i >= cbLeftCardCount + cbFirstCard && cbSameCount < 3) {
					break;
				}

				int cbLastThreeCardValue = 0;
				if (cbThreeLineCount > 0)
					cbLastThreeCardValue = GetCardLogicValue(cbThreeLineCard[cbThreeLineCount - 1]);

				// 重新开始
				if ((cbSameCount < 3
						|| (cbThreeLineCount > 0 && (cbLastThreeCardValue - GetCardLogicValue(cbLastCard)) != 1))
						&& i <= cbLeftCardCount + cbFirstCard) {
					if (cbThreeLineCount >= cbTurnCardCount)
						break;

					if (cbSameCount >= 3)
						i -= 3;
					cbLastCard = cbTmpCard[i];
					cbThreeLineCount = 0;
				}
				// 保存数据
				else if (cbSameCount >= 3) {
					cbThreeLineCard[cbThreeLineCount] = cbTmpCard[i - cbSameCount];
					cbThreeLineCard[cbThreeLineCount + 1] = cbTmpCard[i - cbSameCount + 1];
					cbThreeLineCard[cbThreeLineCount + 2] = cbTmpCard[i - cbSameCount + 2];
					cbThreeLineCount += 3;
					int card_type = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
					if (card_type == GameConstants.DDZ_CT_THREE_LINE) {
						// 保存数据
						if (cbThreeLineCount >= cbTurnCardCount) {
							if (GetCardLogicValue(cbThreeLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])) {
								return true;
							}
						}
					} else if (card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE) {
						// 保存数据
						if (cbThreeLineCount >= cbTurnCardCount / 4 * 3) {
							if (GetCardLogicValue(cbThreeLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])) {
								return true;
							}
						}
					} else if (card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO) {
						int handlinecount = cbThreeLineCount / 3;
						int turnlinecount = cbTurnCardCount / 5;
						tagAnalyseResult AnalyseResult = new tagAnalyseResult();
						AnalysebCardData(cbHandCardData, cbHandCardCount, AnalyseResult);
						// 保存数据
						if (handlinecount >= turnlinecount && AnalyseResult.cbDoubleCount
								+ (AnalyseResult.cbThreeCount - handlinecount) >= turnlinecount) {
							if (GetCardLogicValue(cbThreeLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])) {
								return true;
							}
						}
					}
					if (i >= cbLeftCardCount + cbFirstCard) {
						break;
					}
				}
				cbLastCard = cbTmpCard[i];
				cbSameCount = 1;
			}

		}
		return false;
	}

	// 搜索连对
	public boolean SearchDoubleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		// 连牌判断
		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbLeftCardCount = cbHandCardCount - cbFirstCard;
		boolean bFindDoubleLine = true;
		int cbDoubleLineCount = 0;
		int cbDoubleLineCard[] = new int[24];
		// 开始判断
		while (cbLeftCardCount >= cbTurnCardCount && bFindDoubleLine) {
			int cbLastCard = cbTmpCard[cbFirstCard];
			int cbSameCount = 1;
			cbDoubleLineCount = 0;
			bFindDoubleLine = false;
			for (int i = cbFirstCard + 1; i < cbLeftCardCount + cbFirstCard; ++i) {
				// 搜索同牌
				while (get_card_value(cbLastCard) == get_card_value(cbTmpCard[i])
						&& i < cbLeftCardCount + cbFirstCard) {
					++cbSameCount;
					if (i == cbLeftCardCount + cbFirstCard - 1)
						break;
					++i;
				}

				int cbLastDoubleCardValue = 0;
				if (cbDoubleLineCount > 0)
					cbLastDoubleCardValue = GetCardLogicValue(cbDoubleLineCard[cbDoubleLineCount - 1]);
				// 重新开始
				if ((cbSameCount < 2
						|| (cbDoubleLineCount > 0 && (cbLastDoubleCardValue - GetCardLogicValue(cbLastCard)) != 1))
						&& i <= cbLeftCardCount + cbFirstCard) {
					if (cbDoubleLineCount >= cbTurnCardCount)
						break;

					if (cbSameCount >= 2)
						i -= cbSameCount;

					cbLastCard = cbTmpCard[i];
					cbDoubleLineCount = 0;
				}
				// 保存数据
				else if (cbSameCount >= 2) {
					cbDoubleLineCard[cbDoubleLineCount] = cbTmpCard[i - cbSameCount];
					cbDoubleLineCard[cbDoubleLineCount + 1] = cbTmpCard[i - cbSameCount + 1];
					cbDoubleLineCount += 2;

					// 结尾判断
					if (i == (cbLeftCardCount + cbFirstCard - 2))
						if ((GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbTmpCard[i])) == 1
								&& (GetCardLogicValue(cbTmpCard[i]) == GetCardLogicValue(cbTmpCard[i + 1]))) {
							cbDoubleLineCard[cbDoubleLineCount] = cbTmpCard[i];
							cbDoubleLineCard[cbDoubleLineCount + 1] = cbTmpCard[i + 1];
							cbDoubleLineCount += 2;
							break;
						}

				}

				cbLastCard = cbTmpCard[i];
				cbSameCount = 1;
			}

			// 保存数据
			if (cbDoubleLineCount >= cbTurnCardCount) {
				if (GetCardLogicValue(cbDoubleLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])) {
					return true;
				}
			}
		}
		return false;
	}

	// 分析顺子
	public boolean SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}
		this.sort_card_date_list(cbTmpCard, cbHandCardCount);
		int cbLineCardCount = 0;

		// 数据校验
		if (cbHandCardCount < cbTurnCardCount)
			return false;

		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i) {
			int value = GetCardLogicValue(cbTmpCard[i]);
			if (value < 15) {
				cbFirstCard = i;
				break;
			}
		}

		int cbSingleLineCard[] = new int[12];
		int cbSingleLineCount = 0;
		int cbLeftCardCount = cbHandCardCount;
		boolean bFindSingleLine = true;

		// 连牌判断
		while (cbLeftCardCount >= cbTurnCardCount && bFindSingleLine) {
			cbSingleLineCount = 1;
			bFindSingleLine = false;
			int cbLastCard = cbTmpCard[cbFirstCard];
			cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[cbFirstCard];
			for (int i = cbFirstCard + 1; i < cbLeftCardCount; i++) {
				int cbCardData = cbTmpCard[i];

				// 连续判断
				if (1 != (GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbCardData))
						&& get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbTmpCard[i];
					if (cbSingleLineCount < cbTurnCardCount) {
						cbSingleLineCount = 1;
						cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[i];
						continue;
					} else
						break;
				}
				// 同牌判断
				else if (get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbCardData;
					cbSingleLineCard[cbSingleLineCount] = cbCardData;
					++cbSingleLineCount;
				}
			}

			// 保存数据
			if (cbSingleLineCount >= cbTurnCardCount) {
				if (GetCardLogicValue(cbTurnCardData[0]) < GetCardLogicValue(cbSingleLineCard[0])) {
					return true;
				}
			}
		}
		return false;
	}

	// 分析炸弹
	public boolean SearchBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		int cbBomCardCount = 0;

		if (cbHandCardCount < 2)
			return false;

		// 双王炸弹
		if (0x4F == cbTmpCardData[0] && 0x4E == cbTmpCardData[1]) {
			return true;
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
			if (cbSameCount >= 4 && cbSameCount > cbTurnCardCount) {
				return true;
			} else if (cbSameCount >= 4) {
				int cbBomCardData[] = new int[cbSameCount];
				for (int j = 0; j < cbSameCount; j++) {
					cbBomCardData[j] = cbTmpCardData[i + j];
				}
				if (CompareCard(cbTurnCardData, cbBomCardData, cbTurnCardCount, cbSameCount)) {
					return true;
				}
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return false;
	}

	public int AiAutoOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int out_card_data[], int seat_index, int seat_banker, int turn_out_card_player) {
		if (seat_banker != turn_out_card_player) {
			if (seat_index != seat_banker && turn_out_card_player != seat_index) {
				return 0;
			}
		}
		int out_card_count = 0;
		// 获取出牌类型
		int card_type = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);
		if (card_type == GameConstants.DDZ_CT_MISSILE_CARD)
			return out_card_count;
		int cbBomCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD];
		tagOutCardTypeResult out_card_result = new tagOutCardTypeResult();
		GetAllLineCard(cbHandCardData, cbHandCardCount, out_card_result);
		GetAllThreeCard(cbHandCardData, cbHandCardCount, out_card_result);
		GetAllDoubleCard(cbHandCardData, cbHandCardCount, out_card_result);
		GetAllSingleCard(cbHandCardData, cbHandCardCount, out_card_result);
		if (cbTurnCardCount == 0) {
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_SINGLE) {
					for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
						out_card_data[j] = out_card_result.cbCardData[i][j];
					}
					return out_card_result.cbEachHandCardCount[i];
				}
			}
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_DOUBLE) {
					for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
						out_card_data[j] = out_card_result.cbCardData[i][j];
					}
					return out_card_result.cbEachHandCardCount[i];
				}
			}
			// 搜索顺子
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_SINGLE_LINE) {
					for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
						out_card_data[j] = out_card_result.cbCardData[i][j];
					}
					return out_card_result.cbEachHandCardCount[i];
				}
			}
			GetAllLinkDoubleCard(cbHandCardData, cbHandCardCount, out_card_result);
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_DOUBLE_LINE) {
					for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
						out_card_data[j] = out_card_result.cbCardData[i][j];
					}
					return out_card_result.cbEachHandCardCount[i];
				}
			}
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE) {
					for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
						out_card_data[j] = out_card_result.cbCardData[i][j];
					}
					return out_card_result.cbEachHandCardCount[i];
				}
			}
			return out_card_count;
		}

		if (cbHandCardCount < cbTurnCardCount) {
			return cbTurnCardCount;
		}

		if (card_type == GameConstants.DDZ_CT_SINGLE) {
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_DOUBLE) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < 1; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return 1;
					}
				}
			}
		}
		if (card_type == GameConstants.DDZ_CT_DOUBLE) {
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < 2; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return 2;
					}
				}
			}
		}
		if (card_type == GameConstants.DDZ_CT_THREE) {
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
		}
		// 搜索顺子
		if (card_type == GameConstants.DDZ_CT_SINGLE_LINE) {
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
		}
		if (card_type == GameConstants.DDZ_CT_DOUBLE_LINE) {
			GetAllLinkDoubleCard(cbHandCardData, cbHandCardCount, out_card_result);
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
		}
		if (card_type == GameConstants.DDZ_CT_THREE_LINE) {
			GetAllThreeLinkCard(cbHandCardData, cbHandCardCount, out_card_result);
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
		}
		if (card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE) {
			GetAllThreeLinkCardTakeOne(cbHandCardData, cbHandCardCount, cbTurnCardCount, out_card_result);
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
		}
		if (card_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO) {
			GetAllThreeLinkCardTakeTwo(cbHandCardData, cbHandCardCount, cbTurnCardCount, out_card_result);
			for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
				if (out_card_result.cbCardType[i] == card_type) {
					if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount, cbTurnCardCount)) {
						for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
							out_card_data[j] = out_card_result.cbCardData[i][j];
						}
						return out_card_result.cbEachHandCardCount[i];
					}
				}
			}
		}

		GetAllBomCard(cbHandCardData, cbHandCardCount, out_card_result);
		for (int i = out_card_result.cbCardTypeCount - 1; i >= 0; i--) {
			if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_BOMB_CARD) {
				if (CompareCard(cbTurnCardData, out_card_result.cbCardData[i], cbTurnCardCount,
						out_card_result.cbEachHandCardCount[i])) {
					for (int j = 0; j < out_card_result.cbEachHandCardCount[i]; j++) {
						out_card_data[j] = out_card_result.cbCardData[i][j];
					}
					return out_card_result.cbEachHandCardCount[i];
				}
			}
		}
		return out_card_count;
	}

	// 分析炸弹
	public int GetAllBomCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbBomCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}

		cbBomCardCount = 0;

		if (cbHandCardCount < 2)
			return cbBomCardCount;

		// 双王炸弹
		if (0x4F == cbTmpCardData[0] && 0x4E == cbTmpCardData[1]) {
			out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_MAGIC_BOOM;
			out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 2;
			out_card_result.cbCardData[out_card_result.cbCardTypeCount][0] = cbTmpCardData[0];
			out_card_result.cbCardData[out_card_result.cbCardTypeCount][1] = cbTmpCardData[0];
			out_card_result.cbCardTypeCount++;
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
				out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_BOMB_CARD;
				out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 4;
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][0] = cbTmpCardData[i];
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][1] = cbTmpCardData[i + 1];
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][2] = cbTmpCardData[i + 2];
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][3] = cbTmpCardData[i + 3];
				out_card_result.cbCardTypeCount++;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbBomCardCount;
	}

	// 分析顺子
	public int GetAllLineCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbLineCardCount = 0;
		int cbTmpCard[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCard[i] = cbHandCardData[i];
		}

		// 数据校验
		if (cbHandCardCount < 5)
			return cbLineCardCount;

		int cbFirstCard = 0;
		// 去除2和王
		for (int i = 0; i < cbHandCardCount; ++i)
			if (GetCardLogicValue(cbTmpCard[i]) < 15) {
				cbFirstCard = i;
				break;
			}

		int cbSingleLineCard[] = new int[12];
		int cbSingleLineCount = 0;
		int cbLeftCardCount = cbHandCardCount;
		boolean bFindSingleLine = true;

		// 连牌判断
		while (cbLeftCardCount >= 5 && bFindSingleLine) {
			cbSingleLineCount = 1;
			bFindSingleLine = false;
			int cbLastCard = cbTmpCard[cbFirstCard];
			cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[cbFirstCard];
			for (int i = cbFirstCard + 1; i < cbLeftCardCount; i++) {
				int cbCardData = cbTmpCard[i];

				// 连续判断
				if (1 != (GetCardLogicValue(cbLastCard) - GetCardLogicValue(cbCardData))
						&& get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbTmpCard[i];
					if (cbSingleLineCount < 5) {
						cbSingleLineCount = 1;
						cbSingleLineCard[cbSingleLineCount - 1] = cbTmpCard[i];
						continue;
					} else
						break;
				}
				// 同牌判断
				else if (get_card_value(cbLastCard) != get_card_value(cbCardData)) {
					cbLastCard = cbCardData;
					cbSingleLineCard[cbSingleLineCount] = cbCardData;
					++cbSingleLineCount;
				}
			}

			// 保存数据
			if (cbSingleLineCount >= 5) {
				this.remove_cards_by_data(cbTmpCard, cbLeftCardCount, cbSingleLineCard, cbSingleLineCount);
				for (int i = 0; i < cbSingleLineCount; i++) {
					out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_SINGLE_LINE;
					out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = cbSingleLineCount;
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][i] = cbSingleLineCard[cbLineCardCount
							+ i];

				}
				out_card_result.cbCardTypeCount++;
				cbLineCardCount += cbSingleLineCount;
				cbLeftCardCount -= cbSingleLineCount;
				bFindSingleLine = true;
			}
		}
		return cbLineCardCount;
	}

	// 分析三条
	public int GetAllThreeCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbThreeCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
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

			if (cbSameCount >= 3) {
				out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_THREE;
				out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 3;
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][0] = cbTmpCardData[i];
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][1] = cbTmpCardData[i + 1];
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][2] = cbTmpCardData[i + 2];
				out_card_result.cbCardTypeCount++;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbThreeCardCount;
	}

	// 分析对子
	public int GetAllThreeLinkCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbDoubleCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}
		int firstcardvalue = 0;
		int link_count = 0;
		for (int i = 0; i < out_card_result.cbCardTypeCount; i++) {
			if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE) {
				if (firstcardvalue == 0) {
					firstcardvalue = this.get_card_value(out_card_result.cbCardData[i][0]);
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 0] = out_card_result.cbCardData[i][0];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 1] = out_card_result.cbCardData[i][1];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 2] = out_card_result.cbCardData[i][2];
					link_count++;
				} else {
					if (firstcardvalue != this.get_card_value(out_card_result.cbCardData[i][0]) + link_count) {
						if (link_count > 1) {
							out_card_result.cbCardTypeCount++;
						} else {
							Arrays.fill(out_card_result.cbCardData[out_card_result.cbCardTypeCount], 0);
							out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 0;
						}
						firstcardvalue = get_card_value(out_card_result.cbCardData[i][0]);
						link_count = 0;
					} else {
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 0] = out_card_result.cbCardData[i][0];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 1] = out_card_result.cbCardData[i][1];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 2] = out_card_result.cbCardData[i][2];
						out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] += 3;
						link_count++;
						if (link_count > 1) {
							out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_THREE;
						}
					}
				}
			}
		}
		return cbDoubleCardCount;
	}

	// 分析对子
	public int GetAllThreeLinkCardTakeOne(int cbHandCardData[], int cbHandCardCount, int cbTurnCardCount,
			tagOutCardTypeResult out_card_result) {
		int cbDoubleCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}
		int firstcardvalue = 0;
		int link_count = 0;
		int total_link_count = cbTurnCardCount / 4;

		for (int i = 0; i < out_card_result.cbCardTypeCount; i++) {
			if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE) {
				if (firstcardvalue == 0) {
					firstcardvalue = this.get_card_value(out_card_result.cbCardData[i][0]);
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 0] = out_card_result.cbCardData[i][0];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 1] = out_card_result.cbCardData[i][1];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 2] = out_card_result.cbCardData[i][2];
					link_count++;
				} else {
					if (firstcardvalue != this.get_card_value(out_card_result.cbCardData[i][0]) + link_count) {
						if (link_count > total_link_count) {
							int take_count = 0;
							for (int j = out_card_result.cbCardTypeCount - 1; j > 0; j--) {
								if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_SINGLE) {
									out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][0];
									out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
									take_count++;
									if (take_count >= link_count) {
										break;
									}
								}
							}
							if (take_count < link_count) {
								for (int j = out_card_result.cbCardTypeCount - 1; j >= 0; j--) {
									if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_DOUBLE) {
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][0];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][1];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}
									}
								}
							}
							for (int j = out_card_result.cbCardTypeCount - 1; j > 0; j--) {
								if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_SINGLE) {
									out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][0];
									out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
									take_count++;
									if (take_count >= link_count) {
										break;
									}
								}
							}
							if (take_count < link_count) {
								for (int j = out_card_result.cbCardTypeCount - 1; j >= 0; j--) {
									if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_SINGLE_LINE) {
										while (out_card_result.cbEachHandCardCount[i] > 5) {
											out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][out_card_result.cbEachHandCardCount[i]
													- 1];
											out_card_result.cbEachHandCardCount[i]--;
											out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
											take_count++;
											if (take_count >= link_count) {
												break;
											}
										}
										if (take_count >= link_count) {
											break;
										}
									}
								}
							}
							if (take_count < link_count) {
								for (int j = out_card_result.cbCardTypeCount - 1; j >= 0; j--) {
									if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE) {
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][0];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][1];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][2];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}
									}
								}
							}
							out_card_result.cbCardTypeCount++;
						} else {
							Arrays.fill(out_card_result.cbCardData[out_card_result.cbCardTypeCount], 0);
							out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 0;
						}
						firstcardvalue = get_card_value(out_card_result.cbCardData[i][0]);
						link_count = 0;
					} else {
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 0] = out_card_result.cbCardData[i][0];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 1] = out_card_result.cbCardData[i][1];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 2] = out_card_result.cbCardData[i][2];
						out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] += 3;
						link_count++;
						if (link_count > total_link_count) {
							out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE;
						}
					}
				}
			}
		}
		return cbDoubleCardCount;
	}

	// 分析对子
	public int GetAllThreeLinkCardTakeTwo(int cbHandCardData[], int cbHandCardCount, int cbTurnCardCount,
			tagOutCardTypeResult out_card_result) {
		int cbDoubleCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}
		int firstcardvalue = 0;
		int link_count = 0;
		int total_link_count = cbTurnCardCount / 4;

		for (int i = 0; i < out_card_result.cbCardTypeCount; i++) {
			if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO) {
				if (firstcardvalue == 0) {
					firstcardvalue = this.get_card_value(out_card_result.cbCardData[i][0]);
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 0] = out_card_result.cbCardData[i][0];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 1] = out_card_result.cbCardData[i][1];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 2] = out_card_result.cbCardData[i][2];
					link_count++;
				} else {
					if (firstcardvalue != this.get_card_value(out_card_result.cbCardData[i][0]) + link_count) {
						if (link_count > total_link_count) {
							int take_count = 0;
							if (take_count < link_count) {
								for (int j = out_card_result.cbCardTypeCount - 1; j >= 0; j--) {
									if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_DOUBLE) {
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][0];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][1];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}
									}
								}
							}
							if (take_count < link_count) {
								for (int j = out_card_result.cbCardTypeCount - 1; j >= 0; j--) {
									if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_THREE) {
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][0];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										out_card_result.cbCardData[out_card_result.cbCardTypeCount][out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]] = out_card_result.cbCardData[j][1];
										out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount]++;
										take_count++;
										if (take_count >= link_count) {
											break;
										}

									}
								}
							}
							out_card_result.cbCardTypeCount++;
						} else {
							Arrays.fill(out_card_result.cbCardData[out_card_result.cbCardTypeCount], 0);
							out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 0;
						}
						firstcardvalue = get_card_value(out_card_result.cbCardData[i][0]);
						link_count = 0;
					} else {
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 0] = out_card_result.cbCardData[i][0];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 1] = out_card_result.cbCardData[i][1];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 2] = out_card_result.cbCardData[i][2];
						out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] += 3;
						link_count++;
						if (link_count > total_link_count) {
							out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO;
						}
					}
				}
			}
		}
		return cbDoubleCardCount;
	}

	// 分析对子
	public int GetAllDoubleCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbDoubleCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
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

			if (cbSameCount >= 2) {
				out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_DOUBLE;
				out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 2;
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][0] = cbTmpCardData[i];
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][1] = cbTmpCardData[i + 1];
				out_card_result.cbCardTypeCount++;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbDoubleCardCount;
	}

	// 分析连对
	public int GetAllLinkDoubleCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbDoubleCardCount = 0;
		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
		}
		int firstcardvalue = 0;
		int link_count = 0;
		for (int i = 0; i < out_card_result.cbCardTypeCount; i++) {
			if (out_card_result.cbCardType[i] == GameConstants.DDZ_CT_DOUBLE) {
				if (firstcardvalue == 0) {
					firstcardvalue = this.get_card_value(out_card_result.cbCardData[i][0]);
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 0] = out_card_result.cbCardData[i][0];
					out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
							+ 1] = out_card_result.cbCardData[i][1];
					link_count++;
				} else {
					if (firstcardvalue != this.get_card_value(out_card_result.cbCardData[i][0]) + link_count) {
						if (link_count > 2) {
							out_card_result.cbCardTypeCount++;
						} else {
							Arrays.fill(out_card_result.cbCardData[out_card_result.cbCardTypeCount], 0);
							out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 0;
						}
						firstcardvalue = get_card_value(out_card_result.cbCardData[i][0]);
						link_count = 0;
					} else {
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 0] = out_card_result.cbCardData[i][0];
						out_card_result.cbCardData[out_card_result.cbCardTypeCount][link_count
								+ 1] = out_card_result.cbCardData[i][1];
						out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] += 2;
						link_count++;
						if (link_count > 2) {
							out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_DOUBLE_LINE;
						}
					}
				}
			}
		}
		return cbDoubleCardCount;
	}

	// 分析单牌
	public int GetAllSingleCard(int cbHandCardData[], int cbHandCardCount, tagOutCardTypeResult out_card_result) {
		int cbSingleCardCount = 0;

		int cbTmpCardData[] = new int[cbHandCardCount];
		for (int i = 0; i < cbHandCardCount; i++) {
			cbTmpCardData[i] = cbHandCardData[i];
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

			if (cbSameCount == 1) {
				out_card_result.cbCardType[out_card_result.cbCardTypeCount] = GameConstants.DDZ_CT_SINGLE;
				out_card_result.cbEachHandCardCount[out_card_result.cbCardTypeCount] = 1;
				out_card_result.cbCardData[out_card_result.cbCardTypeCount][0] = cbTmpCardData[i];
				out_card_result.cbCardTypeCount++;
			}

			// 设置索引
			i += cbSameCount - 1;
		}
		return cbSingleCardCount;
	}

	public void switch_to_card_index(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue(card_data[i]);
			card_index[index - 3]++;
		}
	}

	public int switch_card_to_idnex(int card) {
		int index = GetCardLogicValue(card) - 3;
		if (card == 0x5E) {
			index += 2;
		}
		return index;
	}

	public int switch_index_to_card(int index) {
		int value = index + 3;
		if (value == 14 || value == 15) {
			value -= 13;
		}
		if (value == 16 || value == 17) {
			value -= 2;
			value += 4 * 16;
		}
		return value;
	}

	public int get_card_index_count(int card_index[]) {
		int count = 0;
		for (int i = 0; i < GameConstants.DDZ_MAX_INDEX; i++) {
			count += card_index[i];
		}
		return count;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_DDZ AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.DDZ_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			if (cbCardData[i] == 0x5E) {
				index += 2;
			}
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	public boolean is_have_card(int cbCardData[], int cbMagicCardData[], int cbCardCount) {
		int card_index[] = new int[GameConstants.DDZ_MAX_INDEX];
		int magic_index[] = new int[GameConstants.DDZ_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);
		switch_to_card_index(cbMagicCardData, cbCardCount, magic_index);

		int magic_count = magic_index[15];
		for (int i = 0; i < GameConstants.DDZ_MAX_INDEX - 3; i++) {
			if (magic_index[i] - card_index[i] <= magic_count && magic_index[i] - card_index[i] >= 0) {
				magic_count -= magic_index[i] - card_index[i];
			} else {
				return false;
			}
		}
		if (magic_count != 0) {
			return false;
		}

		return true;
	}

	// 是否连
	public boolean is_link(int card_index[], int link_num, int link_count_num) {
		int num = 0;
		int magic_count = card_index[15];
		int init_card_count = get_card_index_count(card_index);
		int card_count = init_card_count;
		for (int i = 0; i < GameConstants.DDZ_MAX_INDEX - 4; i++) {
			if (card_index[i] + magic_count < link_num) {
				if (num == 0) {
					continue;
				} else {
					if (num >= link_count_num && card_count == 0) {
						return true;
					} else {
						return false;
					}
				}
			}

			if (card_index[i] == link_num) {
				num++;
				card_count -= card_index[i];
			} else {
				if (magic_count > 0 && card_index[i] + magic_count >= link_num && card_index[i] < link_num) {
					num++;
					magic_count -= link_num - card_index[i];
					card_count -= link_num;
				} else {
					return false;
				}

			}
		}
		if (num >= link_count_num && card_count == 0) {
			return true;
		} else {
			if (card_count == magic_count && magic_count == link_num) {
				if (init_card_count / link_num > 12) {
					return false;
				} else {
					return true;
				}
			}
			return false;
		}
	}

	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane(tagAnalyseIndexResult_DDZ card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount <= 6) {
			return -1;
		}
		int magic_count = card_data_index.card_index[15];
		int num = 0;
		int max_index = 0;
		int prv_index = 0;
		int min_index = 0;
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (card_data_index.card_index[index] >= 3) {
				int link_num = 1;
				max_index = index;
				prv_index = index;
				for (int j = i + card_data_index.card_index[index]; j < cbCardCount;) {
					int next_index = this.switch_card_to_idnex(cbCardData[j]);
					if (card_data_index.card_index[next_index] == 3 && prv_index - 1 == next_index) {
						prv_index = next_index;
						link_num++;
						if (link_num * 4 == cbCardCount) {
							return 1;
						} else if (link_num * 4 > cbCardCount) {
							return 0;
						}
					} else if (card_data_index.card_index[next_index] + magic_count >= 3
							&& prv_index - 1 == next_index) {
						prv_index = next_index;
						link_num++;
						if (link_num * 4 == cbCardCount) {
							return 1;
						} else if (link_num * 4 > cbCardCount) {
							return 0;
						}

					} else {
						i = j;
						break;
					}
					j += card_data_index.card_index[next_index];
				}
			} else if (card_data_index.card_index[index] + magic_count >= 3) {
				magic_count -= 3 - card_data_index.card_index[index];
				int link_num = 1;
				max_index = index;
				for (int j = i + card_data_index.card_index[index]; j < cbCardCount; j++) {
					int next_index = this.switch_card_to_idnex(cbCardData[j]);
					if (card_data_index.card_index[next_index] == 3) {
						link_num++;
						if (link_num * 4 == cbCardCount) {
							return 1;
						} else if (link_num * 4 > cbCardCount) {
							return 0;
						}
					} else if (card_data_index.card_index[next_index] + magic_count >= 3) {
						link_num++;
						if (link_num * 4 == cbCardCount) {
							return 1;
						} else if (link_num * 4 > cbCardCount) {
							return 0;
						}
					} else {
						i = j;
						break;
					}
					j += card_data_index.card_index[next_index];
				}

			}
			i += card_data_index.card_index[index];
		}

		return -1;
	}

	public boolean make_hua_card(int cbCardData[], int cbCardCount, int cbRealData[]) {
		return true;
	}

	public void make_change_card(int cbCardData[], int cbCardCount) {

	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	public void add_magic_card(int card) {
		magic_card[magic_card_count] = card;
		magic_card_count++;
	}

	public void clear_magic_card() {
		magic_card_count = 0;
	}

	/**
	 * 
	 * 
	 * @param card
	 * @return
	 */
	public boolean is_magic_card_data(int card) {
		for (int i = 0; i < magic_card_count; i++) {
			if (GetCardLogicValue(switch_index_to_card(magic_card[i])) == GetCardLogicValue(card)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param _out_change_cards_data
	 * @param _out_card_count
	 * @param _out_cards_data
	 * @param isLast
	 *            最后一手牌
	 * @param cardType
	 *            客户端发过来的牌型
	 * @return
	 */
	public int GetCardType(int[] _out_change_cards_data, int _out_card_count, int[] _out_cards_data, boolean isLast,
			int cardType) {
		return 0;
	}

	//
	public int get_remanent_cards(int[] hand_cards, int hand_card_count, int[] out_cards, int out_cards_count,
			int[] remanent_cards) {
		int cbtemp[] = new int[GameConstants.CARD_COUNT_DDZ_JD];
		int cbtemp_remanent_cards[] = new int[GameConstants.CARD_COUNT_DDZ_JD];
		Arrays.fill(cbtemp, 0);
		Arrays.fill(cbtemp_remanent_cards, 0);
		int count = 0;
		if (out_cards_count == 0) {
			for (int i = 0; i < hand_card_count; i++) {
				cbtemp[i] = hand_cards[i];
				count++;
			}
		} else if (hand_card_count == 0) {
			for (int i = 0; i < out_cards_count; i++) {
				cbtemp[i] = out_cards[i];
				count++;
			}
		} else {
			for (int i = 0; i < hand_card_count; i++) {
				cbtemp[i] = hand_cards[i];
				count++;
			}
			for (int i = 0; i < out_cards_count; i++) {
				cbtemp[i + hand_card_count] = out_cards[i];
				count++;
			}
		}
		this.switch_to_card_index(cbtemp, count, cbtemp_remanent_cards);

		for (int i = GameConstants.CARD_KIND_DDZ - 1; i >= 0; i--) {
			if (cbtemp_remanent_cards[i] != 0) {
				if (i >= GameConstants.CARD_KIND_DDZ - 2) {
					remanent_cards[GameConstants.CARD_KIND_DDZ - 1 - i] = 0;
				} else {
					remanent_cards[GameConstants.CARD_KIND_DDZ - 1 - i] = 4 - cbtemp_remanent_cards[i];
				}
			} else {
				if (i >= GameConstants.CARD_KIND_DDZ - 2) {
					remanent_cards[GameConstants.CARD_KIND_DDZ - 1 - i] = 1;
				} else {
					remanent_cards[GameConstants.CARD_KIND_DDZ - 1 - i] = 4;
				}

			}
		}

		/*
		 * for(int i = 0; i < GameConstants.CARD_COUNT_DDZ_JD;i++){
		 * if(cbtemp_remanent_cards[i] != 0){ if(i >= 2){ remanent_cards[i] = 0;
		 * }else{ remanent_cards[i] = 4 - cbtemp_remanent_cards[i]; } } }
		 */

		return 0;
	}
}
