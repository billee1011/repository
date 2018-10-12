/**
 * 
 */
package com.cai.game.ddz;

import com.cai.common.constant.GameConstants;
import com.cai.game.ddz.data.tagAnalyseIndexResult_DDZ;

public class DDZGameLogic_LF extends DDZGameLogic {

	public DDZGameLogic_LF() {
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		if (cbCardCount == 1) {
			return DDZConstants.DDZ_LF_CT_SINGLE;
		}

		tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ real_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		AnalysebCardDataToIndex(cbRealData, cbCardCount, real_card_index);

		if (cbCardCount == 2) {
			int index = this.switch_card_to_idnex(cbCardData[cbCardCount - 1]);
			if (card_index.card_index[index] == 2) {
				if (index == 0 || index == 12) {
					return DDZConstants.DDZ_LF_CT_BOMB_CARD;
				}
				return DDZConstants.DDZ_LF_CT_DOUBLE;
			}
			if (card_index.card_index[13] + card_index.card_index[14] == 2) {

				return DDZConstants.DDZ_LF_CT_MISSILE_CARD;
			}
			return DDZConstants.DDZ_LF_CT_ERROR;
		}
		if (cbCardCount == 3) {
			int index = this.switch_card_to_idnex(cbCardData[0]);
			if (card_index.card_index[index] == 3) {
				if (this.has_rule(DDZConstants.GAME_RULE_LF_DDZ_THREE_BOOM_YES)) {
					if (index == 0) {
						return DDZConstants.DDZ_LF_CT_BOMB_CARD;
					}
				}

				return DDZConstants.DDZ_LF_CT_THREE;
			}
			return DDZConstants.DDZ_LF_CT_ERROR;
		}
		if (cbCardCount == 4) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 4) {
				return DDZConstants.DDZ_LF_CT_BOMB_CARD;
			}
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3) {
					return DDZConstants.DDZ_LF_CT_THREE_TAKE_ONE;
				}
			}
		}
		if (cbCardCount == 5) {
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 4) {
					return DDZConstants.DDZ_LF_CT_FOUR_TAKE_TWO;
				}
			}
		}
		if (cbCardCount == 6) {
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 4) {
					return DDZConstants.DDZ_LF_CT_FOUR_TAKE_TWO;
				}
			}
		}

		if (is_link(card_index.card_index, 1, 5)) {
			return DDZConstants.DDZ_LF_CT_SINGLE_LINE;
		}

		if (is_link(card_index.card_index, 2, 3)) {
			return DDZConstants.DDZ_LF_CT_DOUBLE_LINE;
		}

		int nPlane = is_plane(card_index, cbCardData, cbCardCount);
		if (nPlane == 1) {
			return DDZConstants.DDZ_LF_CT_THREE_LINE_TAKE_ONE;
		} else {
			if (is_link(card_index.card_index, 3, 2)) {
				return DDZConstants.DDZ_LF_CT_THREE_LINE_LOST;
			}
		}

		return DDZConstants.DDZ_LF_CT_ERROR;
	}

	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane(tagAnalyseIndexResult_DDZ card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		int max_index = 0;
		int prv_index = 0;
		int min_index = 0;
		for (int index = GameConstants.DDZ_MAX_INDEX - 1; index >= 0; index--) {
			if (card_data_index.card_index[index] == 3) {
				int link_num = 1;
				max_index = index;
				for (int next_index = index - 1; next_index >= 0; next_index--) {
					if (card_data_index.card_index[next_index] == 3) {
						link_num++;
						if (next_index == 0) {
							if (link_num * 4 == cbCardCount) {
								return 1;
							} else if (link_num * 3 == cbCardCount) {
								return 0;
							}
							index = next_index;
							break;
						}
					} else {
						if (link_num * 4 == cbCardCount) {
							return 1;
						} else if (link_num * 3 == cbCardCount) {
							return 0;
						}
						index = next_index;
						break;
					}
				}
			}
		}

		return -1;
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
		if (cbNextType == DDZConstants.DDZ_LF_CT_ERROR)
			return false;

		if (cbFirstType == DDZConstants.DDZ_LF_CT_MISSILE_CARD) {
			return false;
		}
		if (cbNextType == DDZConstants.DDZ_LF_CT_MISSILE_CARD) {
			return true;
		}
		// 炸弹判断
		if ((cbFirstType != DDZConstants.DDZ_LF_CT_BOMB_CARD) && (cbNextType == DDZConstants.DDZ_LF_CT_BOMB_CARD))
			return true;
		if ((cbFirstType == DDZConstants.DDZ_LF_CT_BOMB_CARD) && (cbNextType != DDZConstants.DDZ_LF_CT_BOMB_CARD))
			return false;

		// 开始对比
		switch (cbNextType) {
		case DDZConstants.DDZ_LF_CT_SINGLE:
		case DDZConstants.DDZ_LF_CT_DOUBLE:
		case DDZConstants.DDZ_LF_CT_SINGLE_LINE:
		case DDZConstants.DDZ_LF_CT_DOUBLE_LINE: {
			// 规则判断
			if ((cbFirstType != cbNextType)
					|| (cbFirstType != DDZConstants.DDZ_LF_CT_BOMB_CARD && cbFirstCount != cbNextCount))
				return false;
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case DDZConstants.DDZ_LF_CT_THREE_LINE_LOST:
		case DDZConstants.DDZ_LF_CT_THREE_LINE_TAKE_ONE: {
			int first_index = -1;
			int link_count = 0;
			int next_index = -1;
			for (int i = GameConstants.DDZ_MAX_INDEX - 1; i >= 0; i--) {
				if (first_card_index.card_index[i] == 3) {
					int min_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (first_card_index.card_index[j] == 3) {
							min_index = j;
							if (j == 0) {
								if ((i - min_index + 1) * 3 == cbFirstCount
										|| (i - min_index + 1) * 4 == cbFirstCount) {
									first_index = i;
									link_count = i - min_index + 1;
									break;
								} else {
									i = j;
									break;
								}
							}
						} else {
							if ((i - min_index + 1) * 3 == cbFirstCount || (i - min_index + 1) * 4 == cbFirstCount) {
								first_index = i;
								link_count = i - min_index + 1;
								break;
							} else {
								i = j;
								break;
							}
						}
					}
					if (first_index != -1) {
						break;
					}
				}
			}
			for (int i = GameConstants.DDZ_MAX_INDEX - 1; i >= 0; i--) {
				if (next_card_index.card_index[i] == 3) {
					int min_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (next_card_index.card_index[j] == 3) {
							min_index = j;
							if (j == 0) {
								if ((i - min_index + 1) * 3 == cbFirstCount
										|| (i - min_index + 1) * 4 == cbFirstCount) {
									if (link_count == i - min_index + 1) {
										next_index = i;
									} else {
										i = j;
									}
									break;
								}
							}
						} else {
							if ((i - min_index + 1) * 3 == cbFirstCount || (i - min_index + 1) * 4 == cbFirstCount) {
								if (link_count == i - min_index + 1) {
									next_index = i;
								} else {
									i = j;
								}
								break;
							} else {
								i = j;
								break;
							}
						}
					}
					if (first_index != -1) {
						break;
					}
				}
			}
			if (next_index == -1) {
				return false;
			}
			return next_index > first_index;
		}
		case DDZConstants.DDZ_LF_CT_THREE:
		case DDZConstants.DDZ_LF_CT_THREE_TAKE_ONE: {
			// 分析扑克
			tagAnalyseResult NextResult = new tagAnalyseResult();
			tagAnalyseResult FirstResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, NextResult);
			AnalysebCardData(cbFirstCard, cbFirstCount, FirstResult);

			if (NextResult.cbThreeCount != FirstResult.cbThreeCount) {
				return false;
			}
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(NextResult.cbThreeCardData[0]);
			int cbFirstLogicValue = GetCardLogicValue(FirstResult.cbThreeCardData[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case DDZConstants.DDZ_LF_CT_FOUR_TAKE_TWO: {
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
		case DDZConstants.DDZ_LF_CT_BOMB_CARD: {

			// 获取数值
			if (cbFirstCount == cbNextCount) {
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			} else {
				if (cbFirstCount == 4) {
					return false;
				}
				if (cbNextCount == 4) {
					return true;
				}

				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
				if (cbFirstLogicValue == 15) {
					return false;
				}
				if (cbNextLogicValue == 15) {
					return true;
				}

				return false;
			}

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

		if (turn_card_index.card_index[13] + turn_card_index.card_index[14] == 2 && cbTurnCardCount == 2) {
			// 王炸
			return false;
		}

		switch (cbTurnType) {
		case DDZConstants.DDZ_LF_CT_SINGLE: {
			return SearchSingleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_DOUBLE: {
			return SearchDoubleCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_SINGLE_LINE: {
			return SearchSingleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_DOUBLE_LINE: {
			return SearchDoubleLineCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_THREE_LINE_TAKE_ONE: {
			return SearchThreeTakeOneLinkCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_THREE_TAKE_ONE: {
			return SearchThreeTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_FOUR_TAKE_TWO: {
			return SearchFourTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case DDZConstants.DDZ_LF_CT_BOMB_CARD: {
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
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0 && index > turn_index && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] > 3) {
				return true;
			} else if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
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
		if (cbTurnCardCount == 4) {
			for (int i = 0; i < cbHandCardCount;) {
				int index = this.switch_card_to_idnex(cbHandCardData[i]);
				if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
					return true;
				} else if (hand_card_index.card_index[index] > 3 && index > turn_index) {
					return true;
				}
				i += hand_card_index.card_index[index];
			}
		} else {
			if (turn_index == 0) {
				for (int i = 0; i < cbHandCardCount;) {
					int index = this.switch_card_to_idnex(cbHandCardData[i]);
					if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
						return true;
					} else if (hand_card_index.card_index[index] > 3) {
						return true;
					} else if (hand_card_index.card_index[index] >= 2 && index == 12) {
						return true;
					}
					i += hand_card_index.card_index[index];
				}
			} else {
				for (int i = 0; i < cbHandCardCount;) {
					int index = this.switch_card_to_idnex(cbHandCardData[i]);
					if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
						return true;
					} else if (hand_card_index.card_index[index] > 3) {
						return true;
					}
					i += hand_card_index.card_index[index];
				}
			}

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
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
			return true;
		}
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3 && index != 15) {
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
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
			return true;
		}
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
			if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3) {
				return true;
			} else if (hand_card_index.card_index[index] > 2 && index > max_index) {
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
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
			return true;
		}
		int link_count = 0;
		int max_index = -1;
		for (int index = GameConstants.DDZ_MAX_INDEX - 1; index >= 0; index--) {
			int min_index = index;
			if (turn_card_index.card_index[index] > 2) {
				for (int next_index = index - 1; next_index >= 0; next_index--) {
					if (turn_card_index.card_index[next_index] > 2) {
						min_index = next_index;
					} else {
						if ((index - min_index + 1) * 4 == cbTurnCardCount) {
							max_index = index;
							link_count = index - min_index + 1;
							break;
						}
					}
				}
			}
			if (max_index != -1) {
				break;
			}
		}
		for (int index = GameConstants.DDZ_MAX_INDEX - 1; index >= 0; index--) {
			if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3) {
				return true;
			} else if (hand_card_index.card_index[index] > 2 && index > max_index && index < 12) {
				int min_index = index;
				for (int next_index = index - 1; next_index >= 0; next_index--) {
					if (hand_card_index.card_index[next_index] > 3) {
						return true;
					} else if ((index == 0 || index == 12) && hand_card_index.card_index[next_index] >= 2) {
						return true;
					} else if (hand_card_index.card_index[next_index] > 2) {
						min_index = next_index;
					} else {
						if ((index - min_index + 1) >= link_count) {
							if (index - min_index + 1 == link_count) {
								if ((index - min_index + 1) * 3 == cbHandCardCount
										|| (index - min_index + 1) * 4 <= cbHandCardCount) {
									return true;
								} else {
									return false;
								}

							} else {
								if (cbHandCardCount - (index - min_index + 1) * 3 >= link_count
										|| (index - min_index + 1) > link_count + 1) {
									return true;
								} else {
									return false;
								}
							}

						}
						index = next_index;
						break;
					}
				}
			}
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
			if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2) {
				return true;
			} else if ((index == 0 || index == 12)
					&& hand_card_index.card_index[index] + hand_card_index.card_index[15] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 1 && index > turn_index) {
				return true;
			} else if (hand_card_index.card_index[index] + hand_card_index.card_index[15] > 3) {
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
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2 || hand_card_index.card_index[12] >= 2
				|| hand_card_index.card_index[0] >= 2) {
			return true;
		}
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int index = GameConstants.DDZ_MAX_INDEX - 5; index >= 0; index--) {
			if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] > 1 && index > turn_index && index < 12) {
				for (int next_index = index - 1; next_index >= 0; next_index--) {
					if (hand_card_index.card_index[next_index] + hand_card_index.card_index[15] > 3
							&& next_index != 15) {
						return true;
					} else if ((index == 0 || index == 12) && hand_card_index.card_index[next_index] >= 2) {
						return true;
					} else if (hand_card_index.card_index[next_index] > 1) {
						if (index - next_index + 1 >= cbTurnCardCount / 2) {
							return true;
						}
					} else {
						index = next_index;
						break;
					}
				}
			}
		}
		return false;
	}

	// 搜索顺子
	public boolean SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] == 2 || hand_card_index.card_index[12] >= 2
				|| hand_card_index.card_index[0] >= 2) {
			return true;
		}

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int index = GameConstants.DDZ_MAX_INDEX - 5; index >= 0; index--) {
			if ((index == 0 || index == 12) && hand_card_index.card_index[index] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] > 0 && index > turn_index && index < 12) {
				for (int next_index = index - 1; next_index >= 0; next_index--) {
					if (hand_card_index.card_index[next_index] + hand_card_index.card_index[15] > 3
							&& next_index != 15) {
						return true;
					} else if ((index == 0 || index == 12) && hand_card_index.card_index[next_index] >= 2) {
						return true;
					} else if (hand_card_index.card_index[next_index] > 0) {
						if (index - next_index + 1 >= cbTurnCardCount) {
							return true;
						}
					} else {
						index = next_index;
						break;
					}
				}
			}
		}
		return false;
	}

	public boolean is_auto_out(int card_date[], int card_count, int turn_card_data[], int turn_card_count) {
		if (turn_card_count == 0) {
			if (this.GetCardType(card_date, card_count, card_date) != DDZConstants.DDZ_LF_CT_ERROR) {
				tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
				AnalysebCardDataToIndex(card_date, card_count, card_index);
				if (card_index.card_index[13] + card_index.card_index[14] >= 2 && card_count != 2) {
					return false;
				}
				if (this.has_rule(DDZConstants.GAME_RULE_LF_DDZ_THREE_BOOM_YES)) {
					if (card_index.card_index[0] >= 2 && card_count != card_index.card_index[0]) {
						return false;
					}
				} else {
					if (card_index.card_index[0] >= 2 && card_count != 2) {
						return false;
					}
				}

				for (int i = 0; i < card_count; i++) {
					int index = this.switch_card_to_idnex(card_date[i]);
					if (card_index.card_index[index] >= 4 && card_count != card_index.card_index[index]) {
						return false;
					}
				}
				return true;
			}
		} else {
			if (this.GetCardType(card_date, card_count, card_date) != DDZConstants.DDZ_LF_CT_ERROR) {
				tagAnalyseIndexResult_DDZ card_index = new tagAnalyseIndexResult_DDZ();
				AnalysebCardDataToIndex(card_date, card_count, card_index);
				if (card_index.card_index[13] + card_index.card_index[14] >= 2 && card_count != 2) {
					return false;
				}
				if (card_index.card_index[0] >= 2 && card_count != card_index.card_index[0]) {
					return false;
				}
				for (int i = 0; i < card_count; i++) {
					int index = this.switch_card_to_idnex(card_date[i]);
					if (card_index.card_index[index] >= 4 && card_count != card_index.card_index[index]) {
						return false;
					}
				}
				if (this.CompareCard(turn_card_data, card_date, turn_card_count, card_count)) {
					return true;
				}
				return false;
			}
		}
		return false;
	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		tagAnalyseIndexResult_DDZ card_data_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(card_date, card_count, card_data_index);

		int index = 0;
		if (type == DDZConstants.DDZ_LF_CT_THREE || type == DDZConstants.DDZ_LF_CT_THREE_TAKE_ONE) {
			for (int i = 0; i < GameConstants.DDZ_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] == 3) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[index++] = card_data_index.card_data[i][j];
					}
				}
			}
			for (int i = 0; i < GameConstants.DDZ_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] != 3) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[index++] = card_data_index.card_data[i][j];
					}
				}
			}
		} else if (type == DDZConstants.DDZ_LF_CT_FOUR_TAKE_TWO) {
			for (int i = 0; i < GameConstants.DDZ_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] == 4) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[index++] = card_data_index.card_data[i][j];
					}
				}
			}
			for (int i = 0; i < GameConstants.DDZ_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] != 4) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[index++] = card_data_index.card_data[i][j];
					}
				}
			}
		} else if (type == DDZConstants.DDZ_LF_CT_THREE_LINE_TAKE_ONE
				|| type == DDZConstants.DDZ_LF_CT_THREE_LINE_LOST) {
			for (int i = GameConstants.DDZ_MAX_INDEX - 1; i >= 0; i--) {
				if (card_data_index.card_index[i] == 3) {
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] == 3) {
							if ((i - j + 1) * 5 >= card_count) {
								for (int x = i; x >= j; x--) {
									for (int y = 0; y < card_data_index.card_index[x]; y++) {
										card_date[index++] = card_data_index.card_data[x][y];
									}
								}
								for (int x = 0; x < GameConstants.DDZ_MAX_INDEX; x++) {
									if (x > i || x < j) {
										for (int y = 0; y < card_data_index.card_index[x]; y++) {
											card_date[index++] = card_data_index.card_data[x][y];
										}
									}
								}
								return;
							}
						} else {
							i = j;
							break;
						}

					}
				}
			}
		}
		return;
	}

}
