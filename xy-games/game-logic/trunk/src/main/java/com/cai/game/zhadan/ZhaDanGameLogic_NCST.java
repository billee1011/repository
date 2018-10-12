package com.cai.game.zhadan;

import com.cai.common.constant.GameConstants;
import com.cai.game.zhadan.data.tagAnalyseIndexResult_ZhaDan;

public class ZhaDanGameLogic_NCST extends ZhaDanGameLogic {

	public ZhaDanGameLogic_NCST() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult_ZhaDan out_card_idnex = new tagAnalyseIndexResult_ZhaDan();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, out_card_idnex);
		int one_idnex = this.switch_card_to_idnex(cbCardData[0]);
		if (out_card_idnex.card_index[13] + out_card_idnex.card_index[14] == cbCardCount && cbCardCount == 4) {
			return ZhaDanConstants.NCST_CT_BOMB;
		}
		if (out_card_idnex.card_index[one_idnex] == cbCardCount) {
			if (cbCardCount == 1) {
				return ZhaDanConstants.NCST_CT_SINGLE;
			} else if (cbCardCount == 2) {
				return ZhaDanConstants.NCST_CT_DOUBLE;
			} else if (cbCardCount == 3) {
				return ZhaDanConstants.NCST_CT_THREE;
			} else {
				return ZhaDanConstants.NCST_CT_BOMB;
			}
		}
		return ZhaDanConstants.NCST_CT_ERROR;
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		if (cbFirstType == ZhaDanConstants.NCST_CT_BOMB && cbNextType < ZhaDanConstants.NCST_CT_BOMB) {
			return false;
		}
		if (cbNextType == ZhaDanConstants.NCST_CT_BOMB && cbFirstType < ZhaDanConstants.NCST_CT_BOMB) {
			return true;
		}
		if (cbNextType == cbFirstType) {
			if (cbNextType == ZhaDanConstants.NCST_CT_BOMB) {
				if (cbFirstCount == cbNextCount) {
					return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
				} else {
					return cbNextCount > cbFirstCount;
				}
			} else {
				return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
			}
		}
		return false;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {

		if (turn_card_count == 0) {
			return 1;
		}
		int turn_card_type = this.GetCardType(turn_card_data, turn_card_count);
		switch (turn_card_type) {
		case ZhaDanConstants.NCST_CT_SINGLE:
		case ZhaDanConstants.NCST_CT_DOUBLE:
		case ZhaDanConstants.NCST_CT_THREE: {
			return search_out_single(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case ZhaDanConstants.NCST_CT_BOMB: {
			return search_out_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		}
		return 0;
	}

	public int search_out_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		tagAnalyseIndexResult_ZhaDan hand_index = new tagAnalyseIndexResult_ZhaDan();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_index);
		int turn_index = this.switch_card_to_index(turn_card_data[0]);
		if (hand_index.card_index[13] + hand_index.card_index[14] >= turn_card_count) {
			return 1;
		}
		for (int i = 0; i < ZhaDanConstants.ZHADAN_MAX_INDEX; i++) {
			if (i > turn_index && hand_index.card_index[i] >= turn_card_count) {
				return 1;
			}
			if (hand_index.card_index[i] > turn_card_count) {
				return 1;
			}
		}
		return 0;
	}

	public int search_out_single(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		tagAnalyseIndexResult_ZhaDan hand_index = new tagAnalyseIndexResult_ZhaDan();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_index);
		if (hand_index.card_index[13] + hand_index.card_index[14] >= 4) {
			return 1;
		}
		int turn_index = this.switch_card_to_index(turn_card_data[0]);
		for (int i = 0; i < ZhaDanConstants.ZHADAN_MAX_INDEX; i++) {

			if (i > turn_index && hand_index.card_index[i] >= turn_card_count) {
				return 1;
			}

			if (hand_index.card_index[i] >= 4) {
				return 1;
			}
		}
		return 0;
	}

	public int GetCardScore(int cbCardData[], int cbCardCount) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardLogicValue(cbCardData[i]) == 5) {
				score += 5;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 10 || this.GetCardLogicValue(cbCardData[i]) == 13) {
				score += 10;
			}
		}
		return score;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_ZhaDan AnalyseIndexResult) {
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

	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = GetCardColor(CardData);
		int cbCardValue = GetCardValue(CardData);

		// 转换数值
		if (cbCardColor == 4)
			return cbCardValue + 2;
		return (cbCardValue == 2 || cbCardValue == 1) ? (cbCardValue + 13) : cbCardValue;
	}

	// 获取数值
	public int GetCardValue(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int GetCardColor(int cbCardData) {
		return (cbCardData & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	// 获取花色
	public int GetCardColor(int cbCardData[], int cbCardCount) {
		// 效验参数
		if (cbCardCount == 0)
			return 0xF0;

		// 首牌花色
		int cbCardColor = GetCardColor(cbCardData[0]);

		// 花色判断
		for (int i = 0; i < cbCardCount; i++) {
			if (GetCardColor(cbCardData[i]) != cbCardColor)
				return 0xF0;
		}

		return cbCardColor;
	}

	public int switch_idnex_to_data(int index) {
		return index + 3;
	}

	public int switch_card_to_index(int card) {
		int index = GetCardLogicValue(card) - 3;
		return index;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}
}
