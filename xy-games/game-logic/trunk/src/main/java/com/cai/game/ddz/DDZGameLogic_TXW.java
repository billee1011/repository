/**
 * 
 */
package com.cai.game.ddz;

import com.cai.common.constant.GameConstants;
import com.cai.game.ddz.data.tagAnalyseIndexResult_DDZ;

public class DDZGameLogic_TXW extends DDZGameLogic {

	public DDZGameLogic_TXW() {
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		if (cbCardCount == 0) {
			return DDZConstants.TXW_CT_ERROR;
		}
		if (cbCardCount == 1) {
			return DDZConstants.TXW_CT_SINGLE;
		}

		tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ real_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		AnalysebCardDataToIndex(cbRealData, cbCardCount, real_card_index);

		if (cbCardCount == 2) {
			int index = this.switch_card_to_idnex(cbCardData[cbCardCount - 1]);
			if (card_index.card_index[index] == 2) {
				return DDZConstants.TXW_CT_DOUBLE;
			}
			return DDZConstants.TXW_CT_ERROR;
		}
		if (cbCardCount == 3) {
			int index = this.switch_card_to_idnex(cbCardData[0]);
			if (card_index.card_index[index] == 3) {
				return DDZConstants.TXW_CT_BOMB_CARD;
			}
		}
		if (cbCardCount == 4) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 4) {
				return DDZConstants.TXW_CT_HONG;
			}
		}

		if (is_link(card_index.card_index, 1, 3)) {
			return DDZConstants.TXW_CT_SINGLE_LINE;
		}

		return DDZConstants.TXW_CT_ERROR;
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = GetCardType(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount, cbFirstCard);

		tagAnalyseIndexResult_DDZ first_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ next_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
		AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
		// 类型判断
		if (cbNextType == DDZConstants.TXW_CT_ERROR)
			return false;

		// 炸弹判断
		if ((cbFirstType < DDZConstants.TXW_CT_BOMB_CARD) && (cbNextType >= DDZConstants.TXW_CT_BOMB_CARD))
			return true;
		if ((cbFirstType >= DDZConstants.TXW_CT_BOMB_CARD) && (cbNextType < DDZConstants.TXW_CT_BOMB_CARD))
			return false;

		if ((cbFirstType >= DDZConstants.TXW_CT_BOMB_CARD) && (cbNextType >= DDZConstants.TXW_CT_BOMB_CARD)) {
			if (cbFirstType == cbNextType) {
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			} else {
				return cbNextType > cbFirstType;
			}
		}
		if (cbFirstType == cbNextType) {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		} else {
			return false;
		}

	}

	// 判断是否有压牌
	// 出牌搜索
	public int SearchOutCard_txw(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {
		int cbTurnType = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);

		int out_card_data[] = new int[cbHandCardCount];
		switch (cbTurnType) {
		case DDZConstants.TXW_CT_SINGLE: {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		case DDZConstants.TXW_CT_DOUBLE: {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		case DDZConstants.TXW_CT_SINGLE_LINE: {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
					out_card_data);
		}
		case DDZConstants.TXW_CT_BOMB_CARD:
		case DDZConstants.TXW_CT_HONG: {
			return SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		}

		return 0;
	}

	// 搜索单张
	public int SearchSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int out_card_data[]) {
		int out_card_count = 0;
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int count = 1; count <= 2; count++) {
			for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
				if (hand_card_index.card_index[index] == count && index > turn_index) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][0];
					return out_card_count;
				}
			}
		}

		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 3) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 4) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		return 0;
	}

	// 搜索炸弹
	public int SearchBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int out_card_data[]) {
		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);
		int out_card_count = 0;
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == cbTurnCardCount && index > turn_index) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];

				}
				return out_card_count;
			}
		}
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] > cbTurnCardCount) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		return 0;
	}

	// 搜索对子
	public int SearchDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int out_card_data[]) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int out_card_count = 0;
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 2 && index > turn_index) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 3) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 4) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		return 0;
	}

	// 搜索顺子
	public int SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int out_card_data[]) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);
		int out_card_count = 0;
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int index = GameConstants.DDZ_MAX_INDEX - 5; index >= 0; index--) {
			if (hand_card_index.card_index[index] > 0 && index > turn_index && hand_card_index.card_index[index] < 3) {
				for (int next_index = index - 1; next_index >= 0; next_index--) {
					if (hand_card_index.card_index[next_index] > 0 && hand_card_index.card_index[next_index] < 3) {
						if (index - next_index + 1 >= cbTurnCardCount) {
							for (int i = next_index; i <= index; i++) {
								out_card_data[out_card_count++] = hand_card_index.card_data[i][0];
							}
							return out_card_count;
						}
					} else {
						index = next_index;
						break;
					}
				}
			}
		}

		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 3) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
			if (hand_card_index.card_index[index] == 4) {
				for (int j = 0; j < hand_card_index.card_index[index]; j++) {
					out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
				}
				return out_card_count;
			}
		}
		return 0;
	}

	// 搜索单张
	public int SearchErrorCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int out_card_data[]) {

		int out_card_count = 0;
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);
		if (this.GetCardType(cbHandCardData, cbHandCardCount, cbHandCardData) != DDZConstants.TXW_CT_ERROR) {
			for (int i = 0; i < cbHandCardCount; i++) {
				out_card_data[out_card_count++] = cbHandCardData[i];
			}
			return out_card_count;
		}

		for (int count = 1; count <= 4; count++) {
			for (int index = 0; index < GameConstants.DDZ_MAX_INDEX - 3; index++) {
				if (hand_card_index.card_index[index] == count) {
					for (int j = 0; j < hand_card_index.card_index[index]; j++) {
						out_card_data[out_card_count++] = hand_card_index.card_data[index][j];
					}
					return out_card_count;
				}

			}
		}

		return 0;
	}

	public int AiAutoOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int out_card_data[]) {

		int out_card_count = 0;
		// 获取出牌类型
		int card_type = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);

		switch (card_type) {
		case DDZConstants.TXW_CT_HONG:
		case DDZConstants.TXW_CT_BOMB_CARD: {
			return SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		case DDZConstants.TXW_CT_SINGLE: {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		case DDZConstants.TXW_CT_DOUBLE: {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		case DDZConstants.TXW_CT_SINGLE_LINE: {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
					out_card_data);
		}
		case DDZConstants.TXW_CT_ERROR: {
			return SearchErrorCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount, out_card_data);
		}
		}
		return out_card_count;
	}
}
