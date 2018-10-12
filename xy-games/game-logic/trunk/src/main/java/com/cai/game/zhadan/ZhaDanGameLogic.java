package com.cai.game.zhadan;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.zhadan.data.tagAnalyseIndexResult_ZhaDan;

public class ZhaDanGameLogic {
	public int _boom_count = 6;

	public Map<Integer, Integer> ruleMap = new HashMap<>();

	public ZhaDanGameLogic() {

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

	public int GetCardType(int cbCardData[], int cbCardCount) {

		return GameConstants.SXTH_CT_ERROR;
	}

	// 删除扑克
	public boolean RemoveCard(int cbRemoveCard[], int cbRemoveCount, int cbCardData[], int cbCardCount) {
		// 定义变量
		int cbDeleteCount = 0, cbTempCardData[] = new int[cbCardCount];
		if (cbCardCount > cbTempCardData.length)
			return false;
		for (int i = 0; i < cbCardCount; i++) {
			cbTempCardData[i] = cbCardData[i];
		}

		// 置零扑克
		for (int i = 0; i < cbRemoveCount; i++) {
			for (int j = 0; j < cbCardCount; j++) {
				if (cbRemoveCard[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}
		if (cbDeleteCount != cbRemoveCount)
			return false;

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbTempCardData[i] != 0)
				cbCardData[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		return true;
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {

		tagAnalyseIndexResult_ZhaDan card_index = new tagAnalyseIndexResult_ZhaDan();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		// 510K排前面

		int index[] = new int[GameConstants.WSK_MAX_INDEX];
		for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
			index[i] = i;
		}
		for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
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
		// 王牌后面
		int sort_num = 0;
		for (int j = 0; j < card_index.card_index[14]; j++) {
			cbCardData[sort_num++] = card_index.card_data[14][j];
		}
		for (int j = 0; j < card_index.card_index[13]; j++) {
			cbCardData[sort_num++] = card_index.card_data[13][j];
		}

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			for (int j = 0; j < card_index.card_index[index[i]]; j++) {
				cbCardData[sort_num++] = card_index.card_data[index[i]][j];
			}
		}
		return;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		// int turn_card_type = this.GetCardType(turn_card_data,
		// turn_card_count);

		return all_tip_count;
	}

	public int Get_Wang_Count(tagAnalyseIndexResult_ZhaDan AnalyseIndexResult) {
		return AnalyseIndexResult.card_index[13] + AnalyseIndexResult.card_index[14];
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

	public int switch_card_to_idnex(int card) {
		int index = GetCardLogicValue(card) - 3;
		return index;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}
}
