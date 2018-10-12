/**
 * 
 */
package com.cai.game.ddz;

import com.cai.common.constant.GameConstants;
import com.cai.game.ddz.data.tagAnalyseIndexResult_DDZ;

public class DDZGameLogic_JX extends DDZGameLogic {

	public DDZGameLogic_JX() {
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		if (cbCardCount == 1) {
			if (cbCardData[0] == 0x5E) {
				return GameConstants.DDZ_CT_ERROR_JX;
			}
			return GameConstants.DDZ_CT_SINGLE_JX;
		}

		tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ real_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		AnalysebCardDataToIndex(cbRealData, cbCardCount, real_card_index);

		if (cbCardCount == 2) {
			int index = this.switch_card_to_idnex(cbCardData[cbCardCount - 1]);
			if (card_index.card_index[index] == 2) {
				if (index == 0 || index == 12) {
					return GameConstants.DDZ_CT_BOMB_CARD_JX;
				}
				return GameConstants.DDZ_CT_DOUBLE_JX;
			}
			if (card_index.card_index[13] + card_index.card_index[14] + card_index.card_index[15] == 2) {

				return GameConstants.DDZ_CT_MISSILE_CARD_JX;
			}
			return GameConstants.DDZ_CT_ERROR_JX;
		}
		if (cbCardCount == 3) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 3) {
				if (this.get_card_value(cbCardData[0]) == 2) {
					return GameConstants.DDZ_CT_ERROR;
				}
				return GameConstants.DDZ_CT_THREE_JX;
			}
			return GameConstants.DDZ_CT_ERROR;
		}
		if (cbCardCount == 4) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 4) {
				return GameConstants.DDZ_CT_BOMB_CARD_JX;
			}
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3) {
					return GameConstants.DDZ_CT_THREE_TAKE_ONE_JX;
				}
			}
		}
		if (cbCardCount == 6) {
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 4) {
					return GameConstants.DDZ_CT_FOUR_TAKE_ONE_JX;
				}
			}
		}

		if (this.has_rule(GameConstants.GAME_RULE_JX_DDZ_PLAYER_THREE)) {
			if (is_link(card_index.card_index, 1, 5)) {
				return GameConstants.DDZ_CT_SINGLE_LINE_JX;
			}

			if (is_link(card_index.card_index, 2, 3)) {
				return GameConstants.DDZ_CT_DOUBLE_LINE_JX;
			}
		} else {
			if (is_link(card_index.card_index, 1, 4)) {
				return GameConstants.DDZ_CT_SINGLE_LINE_JX;
			}
			if (is_link(card_index.card_index, 2, 2)) {
				return GameConstants.DDZ_CT_DOUBLE_LINE_JX;
			}
		}

		if (is_link(card_index.card_index, 3, 2)) {
			return GameConstants.DDZ_CT_THREE_LINE_JX_ONE;
		}
		int nPlane = is_plane(card_index, cbCardData, cbCardCount);
		if (nPlane == 0) {
			return GameConstants.DDZ_CT_THREE_LINE_JX_ONE;
		} else if (nPlane == 1) {
			return GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX;
		}
		return GameConstants.DDZ_CT_ERROR;
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
		if (cbNextType == GameConstants.DDZ_CT_ERROR)
			return false;

		if (cbNextType == GameConstants.DDZ_CT_MISSILE_CARD_JX) {
			if (cbFirstType == GameConstants.DDZ_CT_BOMB_CARD_JX) {
				if (first_card_index.card_index[0] == 4) {
					// 4个三最大
					return false;
				}
			}
			return true;
		}
		if (cbFirstType == GameConstants.DDZ_CT_MISSILE_CARD_JX) {
			if (cbNextType == GameConstants.DDZ_CT_BOMB_CARD_JX) {
				if (next_card_index.card_index[0] == 4) {
					// 4个三大过王炸
					return true;
				}
			}
			return false;
		}

		// 炸弹判断
		if ((cbFirstType != GameConstants.DDZ_CT_BOMB_CARD_JX) && (cbNextType == GameConstants.DDZ_CT_BOMB_CARD_JX))
			return true;
		if ((cbFirstType == GameConstants.DDZ_CT_BOMB_CARD_JX) && (cbNextType != GameConstants.DDZ_CT_BOMB_CARD_JX))
			return false;

		// 规则判断
		if ((cbFirstType != cbNextType)
				|| (cbFirstType != GameConstants.DDZ_CT_BOMB_CARD_JX && cbFirstCount != cbNextCount))
			return false;

		// 开始对比
		switch (cbNextType) {
		case GameConstants.DDZ_CT_SINGLE_JX:
		case GameConstants.DDZ_CT_DOUBLE_JX:
		case GameConstants.DDZ_CT_THREE_JX:
		case GameConstants.DDZ_CT_SINGLE_LINE_JX:
		case GameConstants.DDZ_CT_DOUBLE_LINE_JX:
		case GameConstants.DDZ_CT_THREE_LINE_JX_ONE:
		case GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX: {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.DDZ_CT_THREE_TAKE_ONE_JX:
		case GameConstants.DDZ_CT_THREE_TAKE_TWO_JX: {
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
		case GameConstants.DDZ_CT_FOUR_TAKE_ONE_JX:
		case GameConstants.DDZ_CT_FOUR_TAKE_TWO_JX: {
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
		case GameConstants.DDZ_CT_BOMB_CARD_JX: {

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
	public boolean SearchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount) {
		int cbTurnType = GetCardType(cbTurnCardData, cbTurnCardCount, cbTurnCardData);

		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] == 2) {
			if (cbTurnType == GameConstants.DDZ_CT_BOMB_CARD_JX) {
				if (turn_card_index.card_index[0] != 4) {
					// 4个三最大
					return true;
				}
			} else {
				// 王炸
				return true;
			}

		}
		if (turn_card_index.card_index[13] + turn_card_index.card_index[14] == 2) {
			if (hand_card_index.card_index[0] + hand_card_index.card_index[15] == 4) {
				// 4个三最大
				return true;
			}
			// 王炸
			return false;
		}

		switch (cbTurnType) {
		case GameConstants.DDZ_CT_SINGLE_JX: {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_DOUBLE_JX: {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_SINGLE_LINE_JX: {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_DOUBLE_LINE_JX: {
			return SearchDoubleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX: {
			return SearchThreeTakeOneLinkCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_THREE_TAKE_ONE_JX: {
			return SearchThreeTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_FOUR_TAKE_ONE_JX: {
			return SearchFourTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_BOMB_CARD_JX: {
			return SearchBoomCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		}

		return false;
	}

	// 搜索单张
	public boolean SearchSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0 && index > turn_index && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
				return true;
			} else if ((index == 0 || index == 12)
					&& hand_card_index.card_index[index] + hand_card_index.card_index[15] >= 2) {
				return true;
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索炸弹
	public boolean SearchBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index > turn_index
					&& index != 15) {
				return true;
			} else if (index == 12 && hand_card_index.card_index[index] >= 2 && index > turn_index) {
				return true;
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索四带一
	public boolean SearchFourTakeOneCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] == 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
				return true;
			} else if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索三带一
	public boolean SearchThreeTakeOneCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {
		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int max_index = -1;
		for (int i = 0; i < cbTurnCardCount; i++) {
			int index = this.switch_card_to_idnex(cbTurnCardData[i]);
			if (turn_card_index.card_index[index] > 2) {
				max_index = index;
				break;
			}
		}
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] == 2) {
				return true;
			} else if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] > 2 && index > max_index && index != 15
					&& cbHandCardCount >= 4) {
				return true;
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索飞机
	public boolean SearchThreeTakeOneLinkCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int max_index = -1;
		for (int i = 0; i < cbTurnCardCount; i++) {
			int index = this.switch_card_to_idnex(cbTurnCardData[i]);
			if (turn_card_index.card_index[index] > 2) {
				for (int j = i + turn_card_index.card_index[index]; j < cbTurnCardCount;) {
					int next_index = this.switch_card_to_idnex(cbTurnCardData[j]);
					if (turn_card_index.card_index[next_index] > 2) {
						if ((index - next_index) * 4 == cbTurnCardCount) {
							max_index = index;
							break;
						}
					}
					j += turn_card_index.card_index[next_index];
				}
			}
			if (max_index != -1) {
				break;
			}
			i += turn_card_index.card_index[index];
		}
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] == 2) {
				return true;
			} else if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] > 2 && index > max_index && index < 12
					&& cbHandCardCount >= cbTurnCardCount) {
				int first_index = index;
				for (int j = i + hand_card_index.card_index[index]; j < cbHandCardCount;) {
					int next_index = this.switch_card_to_idnex(cbHandCardData[j]);
					if (hand_card_index.card_index[next_index] + hand_card_index.card_index[15] > 3
							&& next_index != 15) {
						return true;
					} else if ((index == 0 || index == 12) && hand_card_index.card_index[next_index] >= 2) {
						return true;
					} else if (hand_card_index.card_index[next_index] > 2 && next_index == first_index - 1) {
						if ((first_index - next_index) * 4 >= cbTurnCardCount) {
							return true;
						}
						first_index = next_index;
					} else {
						break;
					}
					j += hand_card_index.card_index[next_index];
				}
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索连对
	public boolean SearchDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] == 2) {
				return true;
			} else if ((index == 0 || index == 12)
					&& hand_card_index.card_index[index] + hand_card_index.card_index[15] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 1 && index > turn_index
					&& index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
				return true;
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索连对
	public boolean SearchDoubleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] == 2) {
				return true;
			} else if ((index == 0 || index == 12)
					&& hand_card_index.card_index[index] + hand_card_index.card_index[15] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] > 1 && index > turn_index && index < 12) {
				int prv_index = index;
				for (int j = i + hand_card_index.card_index[index]; j < cbHandCardCount;) {
					int next_index = this.switch_card_to_idnex(cbHandCardData[j]);
					if (hand_card_index.card_index[next_index] + hand_card_index.card_index[15] > 3
							&& next_index != 15) {
						return true;
					} else if ((index == 0 || index == 12) && hand_card_index.card_index[next_index] >= 2) {
						return true;
					} else if (hand_card_index.card_index[next_index] > 1 && next_index == prv_index - 1) {
						if (index - next_index >= cbTurnCardCount / 2) {
							return true;
						}
						prv_index = next_index;
					} else {
						break;
					}
					j += hand_card_index.card_index[next_index];
				}
			}
			i += hand_card_index.card_index[index];
		}
		return false;
	}

	// 搜索顺子
	public boolean SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		int magic_count_tmep = hand_card_index.card_index[15];
		int next_index = -1;
		int max_idnex = -1;
		for (int i = GameConstants.DDZ_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_card_index.card_index[i] + hand_card_index.card_index[15] > 3 && i != 15) {
				return true;
			} else if (hand_card_index.card_index[i] + hand_card_index.card_index[15] > 3 && i != 15) {
				return true;
			} else if ((i == 0 || i == 12) && hand_card_index.card_index[i] + hand_card_index.card_index[15] >= 2) {
				return true;
			} else if (max_idnex > turn_index && max_idnex != -1 && hand_card_index.card_index[i] + magic_count_tmep > 0
					&& i < GameConstants.DDZ_MAX_INDEX - 4) {
				if (hand_card_index.card_index[i] == 0) {
					magic_count_tmep--;
				}
				next_index = i;
			} else if (i > turn_index && max_idnex == -1 && hand_card_index.card_index[i] + magic_count_tmep > 0
					&& i < GameConstants.DDZ_MAX_INDEX - 4) {
				if (hand_card_index.card_index[i] == 0) {
					magic_count_tmep--;
				}
				max_idnex = i;
				next_index = i;
			} else {
				if (max_idnex - next_index + 1 >= cbTurnCardCount) {
					return true;
				} else {
					magic_count_tmep = hand_card_index.card_index[15];
					max_idnex = -1;
					next_index = -1;
				}
			}

			if (i == 0) {
				if (max_idnex - next_index + 1 >= cbTurnCardCount) {
					return true;
				}
			}
		}
		return false;
	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		tagAnalyseResult Result = new tagAnalyseResult();
		AnalysebCardData(card_date, card_count, Result);

		int index = 0;
		if (type == GameConstants.DDZ_CT_SINGLE_JX || type == GameConstants.DDZ_CT_SINGLE_LINE_JX) {
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
		} else if (type == GameConstants.DDZ_CT_DOUBLE_JX || type == GameConstants.DDZ_CT_DOUBLE_LINE_JX) {
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants.DDZ_CT_THREE_JX || type == GameConstants.DDZ_CT_THREE_TAKE_ONE_JX
				|| type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX || type == GameConstants.DDZ_CT_THREE_TAKE_TWO_JX
				|| type == GameConstants.DDZ_CT_THREE_LINE_JX_ONE) {

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

		} else if (type == GameConstants.DDZ_CT_FOUR_TAKE_ONE_JX || type == GameConstants.DDZ_CT_FOUR_TAKE_TWO_JX) {
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
		} else if (type == GameConstants.DDZ_CT_BOMB_CARD_JX) {
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

	public void make_change_card(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		int magic_count = card_index.card_index[15];
		if (magic_count == 0) {
			return;
		}
		int magic_card_idnex = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] == 0x5E) {
				magic_card_idnex = i;
			}
		}

		if (cbCardCount == 2) {
			if (card_index.card_index[13] + card_index.card_index[14] + card_index.card_index[15] == 2) {
				if (card_index.card_index[13] == 0) {
					cbCardData[1] = 0x4E;
					cbCardData[0] = 0x4F;
				}
				if (card_index.card_index[14] == 0) {
					cbCardData[1] = 0x4E;
					cbCardData[0] = 0x4F;
				}
				return;
			} else {
				for (int i = 0; i < cbCardCount; i++) {
					if (cbCardData[i] != 0x5E) {
						cbCardData[magic_card_idnex] = cbCardData[i];
						return;
					}
				}
			}
		} else if (cbCardCount == 3) {
			for (int i = 0; i < cbCardCount; i++) {
				if (cbCardData[i] != 0x5E) {
					int index = this.switch_card_to_idnex(cbCardData[i]);
					if (card_index.card_index[index] == 2) {
						cbCardData[magic_card_idnex] = cbCardData[i];
						return;
					} else {
						return;
					}
				}
			}
		} else if (cbCardCount == 4) {
			for (int i = 0; i < cbCardCount; i++) {
				if (cbCardData[i] != 0x5E) {
					int index = this.switch_card_to_idnex(cbCardData[i]);
					if (card_index.card_index[index] == 2) {
						for (int j = i + card_index.card_index[index]; j < cbCardCount; j++) {
							if (cbCardData[j] != 0x5E) {
								cbCardData[magic_card_idnex] = cbCardData[j];
							}
						}
						return;
					} else if (card_index.card_index[index] == 3) {
						cbCardData[magic_card_idnex] = cbCardData[i];
						return;
					}
				}
			}
		} else if (cbCardCount == 5) {
			for (int i = 0; i < cbCardCount; i++) {
				if (cbCardData[i] != 0x5E) {
					int index = this.switch_card_to_idnex(cbCardData[i]);
					if (card_index.card_index[index] == 2) {
						cbCardData[magic_card_idnex] = cbCardData[i];
						return;
					} else if (card_index.card_index[index] == 3) {
						for (int j = i + card_index.card_index[index]; j < cbCardCount; j++) {
							if (cbCardData[j] != 0x5E) {
								cbCardData[magic_card_idnex] = cbCardData[j];
							}
						}
						return;
					}
				}
			}
		} else if (cbCardCount == 6) {
			for (int i = 0; i < cbCardCount; i++) {
				if (cbCardData[i] != 0x5E) {
					int index = this.switch_card_to_idnex(cbCardData[i]);
					if (card_index.card_index[index] == 3) {
						cbCardData[magic_card_idnex] = cbCardData[i];
						return;
					} else if (card_index.card_index[index] == 4) {
						for (int j = i + card_index.card_index[index]; j < cbCardCount; j++) {
							if (cbCardData[j] != 0x5E) {
								cbCardData[magic_card_idnex] = cbCardData[j];
							}
						}
						return;
					}
				}
			}
		}

		// 顺子
		int max_count = 0;
		for (int i = 0; i < cbCardCount;) {
			if (card_index.card_index[switch_card_to_idnex(cbCardData[i])] > max_count) {
				max_count = card_index.card_index[switch_card_to_idnex(cbCardData[i])];
			}
			i += card_index.card_index[switch_card_to_idnex(cbCardData[i])];
		}
		if (max_count == 1) {
			// 顺子
			int max_index = switch_card_to_idnex(cbCardData[1]);
			int prv_index = switch_card_to_idnex(cbCardData[1]);
			int magic_change_index = 0;
			magic_card_idnex = 0;
			for (int i = 2; i < cbCardCount;) {
				int index = switch_card_to_idnex(cbCardData[i]);
				if (card_index.card_index[index] == 1) {
					if (index + 1 == prv_index) {
						prv_index = index;
						i += card_index.card_index[index];
					} else {
						if (magic_count > 0) {
							magic_change_index = prv_index - 1;
							prv_index = magic_change_index;
							magic_count--;
						} else {
							return;
						}
					}
				} else {
					return;
				}

			}
			if (this.has_rule(GameConstants.GAME_RULE_JX_DDZ_PLAYER_THREE)) {
				if (max_index - prv_index >= 4) {
					cbCardData[0] = this.switch_index_to_card(magic_change_index);
				}
			} else {
				if (max_index - prv_index >= 3) {
					cbCardData[0] = this.switch_index_to_card(magic_change_index);
				}
			}

		} else if (max_count == 2) {
			// 连对
			int max_index = switch_card_to_idnex(cbCardData[1]);
			int prv_index = -1;
			int magic_change_index = 0;
			magic_card_idnex = 0;
			for (int i = 1; i < cbCardCount;) {
				int index = switch_card_to_idnex(cbCardData[i]);
				if (card_index.card_index[index] <= 2) {
					if (index + 1 == prv_index || prv_index == -1) {
						if (card_index.card_index[index] == 2) {
							prv_index = index;
						} else {
							if (magic_count > 0) {
								magic_change_index = index;
								prv_index = index;
								magic_count--;
							} else {
								return;
							}
						}
						i += card_index.card_index[index];
					} else {
						return;
					}
				} else {
					return;
				}

			}
			if (this.has_rule(GameConstants.GAME_RULE_JX_DDZ_PLAYER_THREE)) {
				if (max_index - prv_index >= 2) {
					cbCardData[0] = this.switch_index_to_card(magic_change_index);
				}
			} else {
				if (max_index - prv_index >= 1) {
					cbCardData[0] = this.switch_index_to_card(magic_change_index);
				}
			}

		} else if (max_count > 2) {
			// 飞机
			int max_index = -1;
			int prv_index = -1;
			int magic_change_index = 0;
			magic_card_idnex = 0;
			int magic_count_temp = magic_count;
			for (int i = 1; i < cbCardCount;) {
				int index = switch_card_to_idnex(cbCardData[i]);
				if (card_index.card_index[index] + magic_count_temp >= 3 && max_index == -1) {
					max_index = index;
					prv_index = index;
					if (card_index.card_index[index] < 3) {
						magic_count_temp -= 3 - card_index.card_index[index];
						magic_change_index = index;
					}
				} else if (card_index.card_index[index] + magic_count_temp >= 3 && index + 1 == prv_index) {
					prv_index = index;
					if (card_index.card_index[index] < 3) {
						magic_count_temp -= 3 - card_index.card_index[index];
						magic_change_index = index;
					}
				} else {
					if (max_index != -1) {
						if (max_index - prv_index >= 1 && (max_index - prv_index + 1) * 4 >= cbCardCount) {
							cbCardData[0] = this.switch_index_to_card(magic_change_index);
							return;
						} else {
							max_index = -1;
							prv_index = -1;
						}
					}
				}
				i += card_index.card_index[index];
			}
			if (max_index - prv_index >= 1 && (max_index - prv_index + 1) * 4 >= cbCardCount) {
				cbCardData[0] = this.switch_index_to_card(magic_change_index);
				return;
			}
		}

	}

	public boolean make_hua_card(int cbCardData[], int cbCardCount, int cbRealData[]) {
		tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ real_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		AnalysebCardDataToIndex(cbRealData, cbCardCount, real_card_index);
		int card_data_temp[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			card_data_temp[i] = cbRealData[i];
		}
		int magic_count = real_card_index.card_index[15];
		if (magic_count > 0) {
			for (int i = 0; i < cbCardCount; i++) {
				for (int j = 0; j < cbCardCount; j++) {
					if (cbCardData[i] == card_data_temp[j]) {
						card_data_temp[j] = 0;
						break;
					}
					if (j == cbCardCount - 1 && magic_count > 0) {
						cbCardData[i] += 0x100;
						magic_count--;
						if (magic_count < 0) {
							return false;
						}
						break;
					}
				}
			}
		}
		return true;
	}
}
