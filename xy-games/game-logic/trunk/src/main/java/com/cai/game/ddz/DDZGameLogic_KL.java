/**
 * 
 */
package com.cai.game.ddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_KL_DDZ;
import com.cai.game.ddz.data.tagAnalyseIndexResult_DDZ;

public class DDZGameLogic_KL extends DDZGameLogic {

	public DDZGameLogic_KL() {
	}

	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		return GetCardType(cbCardData, cbCardCount, cbRealData, false, -1);
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[], boolean isLast, int cardType) {
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

		if (card_index.card_index[13] + card_index.card_index[14] == 2 && cbCardCount > 2) {
			return GameConstants.DDZ_CT_ERROR;
		}
		if (cbCardCount == 2) {
			int index = this.switch_card_to_idnex(cbCardData[cbCardCount - 1]);
			if (card_index.card_index[index] == 2) {
				return GameConstants.DDZ_CT_DOUBLE_JX;
			}
			if (card_index.card_index[13] + card_index.card_index[14] + card_index.card_index[15] == 2) {

				return GameConstants.DDZ_CT_MISSILE_CARD_JX;
			}
			return GameConstants.DDZ_CT_ERROR_JX;
		}
		if (cbCardCount == 3) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 3) {
				// if (this.get_card_value(cbCardData[0]) == 2) {
				// return GameConstants.DDZ_CT_ERROR;
				// }
				if (isLast || has_rule(Constants_KL_DDZ.GAME_RULE_3_BU_NENG_DAI)) {
					return GameConstants.DDZ_CT_THREE_JX;
				}
			}
			return GameConstants.DDZ_CT_ERROR;
		}
		if (cbCardCount == 4) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 4
					&& cardType == GameConstants.DDZ_CT_BOMB_CARD_JX) {
				return GameConstants.DDZ_CT_BOMB_CARD_JX;
			}
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3
						&& cardType == GameConstants.DDZ_CT_THREE_TAKE_ONE_JX) {
					if (has_rule(Constants_KL_DDZ.GAME_RULE_3_DAI_1)) {
						return GameConstants.DDZ_CT_THREE_TAKE_ONE_JX;
					} else {
						return GameConstants.DDZ_CT_ERROR;
					}
				}
			}
		}
		if (cbCardCount == 5 && has_rule(Constants_KL_DDZ.GAME_RULE_3_DAI_2_DUI_ZI)
				&& cardType == GameConstants.DDZ_CT_THREE_TAKE_TWO_JX) {
			int dui_zi_count = 0;
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 2) {
					dui_zi_count++;
				}
			}
			dui_zi_count = dui_zi_count / 2;
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3 && dui_zi_count == 1) {
					return GameConstants.DDZ_CT_THREE_TAKE_TWO_JX;
				} else if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 5) {
					return GameConstants.DDZ_CT_THREE_TAKE_TWO_JX;
				}
			}
		}
		if (cbCardCount == 6) {
			for (int i = 0; i < cbCardCount; i++) {
				if (has_rule(Constants_KL_DDZ.GAME_RULE_DAI_2_DAN_ZHANG)) {
					if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 4) {
						return GameConstants.DDZ_CT_FOUR_TAKE_ONE_JX;
					}
				}
			}
		}

		if (cbCardCount == 8 && has_rule(Constants_KL_DDZ.GAME_RULE_DAI_2_DUI_ZI)
				&& cardType == GameConstants.DDZ_CT_FOUR_TAKE_TWO_JX) {
			int dui_zi_count = 0;
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] % 2 == 0) {
					dui_zi_count += card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] / 2;
				} else {
					break;
				}
			}
			for (int i = 0; i < cbCardCount;) {
				int single_card_count = card_index.card_index[this.switch_card_to_idnex(cbCardData[i])];
				if (single_card_count == 4 && dui_zi_count >= 2) {
					return GameConstants.DDZ_CT_FOUR_TAKE_TWO_JX;
				}
				i += single_card_count;
			}
		}

		if (is_link(card_index.card_index, 1, 5)) {
			return GameConstants.DDZ_CT_SINGLE_LINE_JX;
		}

		if (is_link(card_index.card_index, 2, 3)) {
			return GameConstants.DDZ_CT_DOUBLE_LINE_JX;
		}

		if (is_link(card_index.card_index, 3, 2) && has_rule(Constants_KL_DDZ.GAME_RULE_3_BU_NENG_DAI)
				&& cardType == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX) {
			return GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX;
		}

		int nPlane = is_plane(card_index, cbCardData, cbCardCount);
		if (nPlane == 1 && (has_rule(Constants_KL_DDZ.GAME_RULE_3_DAI_1) || isLast)
				&& cardType == GameConstants.DDZ_CT_THREE_LINE_JX_ONE) {
			return GameConstants.DDZ_CT_THREE_LINE_JX_ONE;
		} else if (nPlane == 0 && (has_rule(Constants_KL_DDZ.GAME_RULE_3_DAI_2_DUI_ZI) || isLast)
				&& cardType == GameConstants.DDZ_CT_THREE_LINE_JX_TWO) {
			return GameConstants.DDZ_CT_THREE_LINE_JX_TWO;
		}

		return GameConstants.DDZ_CT_ERROR;
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount, int first_type,
			int next_type) {
		// 获取类型
		int cbNextType = GetCardType(cbNextCard, cbNextCount, cbNextCard, false, next_type);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount, cbFirstCard, false, first_type);

		tagAnalyseIndexResult_DDZ first_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ next_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
		AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
		// 类型判断
		if (cbNextType == GameConstants.DDZ_CT_ERROR)
			return false;

		if (cbNextType == GameConstants.DDZ_CT_MISSILE_CARD_JX) {
			// if (cbFirstType == GameConstants.DDZ_CT_BOMB_CARD_JX) {
			// if (first_card_index.card_index[0] == 4) {
			// // 4个三最大
			// return false;
			// }
			// }
			return true;
		}
		// if (cbFirstType == GameConstants.DDZ_CT_MISSILE_CARD_JX) {
		// if (cbNextType == GameConstants.DDZ_CT_BOMB_CARD_JX) {
		// if (next_card_index.card_index[0] == 4) {
		// // 4个三大过王炸
		// return true;
		// }
		// }
		// return false;
		// }

		// 炸弹判断
		if ((cbFirstType != GameConstants.DDZ_CT_BOMB_CARD_JX) && (cbNextType == GameConstants.DDZ_CT_BOMB_CARD_JX))
			return true;
		if ((cbFirstType == GameConstants.DDZ_CT_BOMB_CARD_JX) && (cbNextType != GameConstants.DDZ_CT_BOMB_CARD_JX))
			return false;

		// 规则判断
		if ((cbFirstType != cbNextType)
				|| (cbFirstType != GameConstants.DDZ_CT_BOMB_CARD_JX && cbFirstCount != cbNextCount))
			return false;
		int first_magic_count = first_card_index.card_index[magic_card[0]];
		int next_magic_count = next_card_index.card_index[magic_card[0]];
		// 开始对比
		switch (cbNextType) {
		case GameConstants.DDZ_CT_SINGLE_JX:
		case GameConstants.DDZ_CT_DOUBLE_JX:
		case GameConstants.DDZ_CT_THREE_JX:
		case GameConstants.DDZ_CT_SINGLE_LINE_JX:
		case GameConstants.DDZ_CT_DOUBLE_LINE_JX:
		case GameConstants.DDZ_CT_THREE_LINE_JX_ONE:
		case GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX:
		case GameConstants.DDZ_CT_THREE_LINE_JX_TWO: {
			// 获取数值
			int cbNextLogicValue = 0;
			for (int i = 0; i < cbFirstCount; i++) {
				cbNextLogicValue = GetCardLogicValue(cbNextCard[i]);
				break;
			}

			int cbFirstLogicValue = 0;
			for (int i = 0; i < cbNextCount; i++) {
				cbFirstLogicValue = GetCardLogicValue(cbFirstCard[i]);
				break;
			}
			// int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			// int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
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
	public boolean SearchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,
			int turntype) {

		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		if (turn_card_index.card_index[13] + turn_card_index.card_index[14] == 2
				&& turntype == GameConstants.DDZ_CT_MISSILE_CARD_JX) {
			return false;
		}

		switch (turntype) {
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
		case GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX:
		case GameConstants.DDZ_CT_THREE_LINE_JX_ONE:
		case GameConstants.DDZ_CT_THREE_LINE_JX_TWO: {
			return SearchThreeTakeOneLinkCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount,
					turntype);
		}
		case GameConstants.DDZ_CT_THREE_TAKE_TWO_JX:
		case GameConstants.DDZ_CT_THREE_TAKE_ONE_JX:
		case GameConstants.DDZ_CT_THREE_JX: {
			return SearchThreeTakeOneCard(cbHandCardData, cbHandCardCount, cbTurnCardData, cbTurnCardCount);
		}
		case GameConstants.DDZ_CT_FOUR_TAKE_ONE_JX:
		case GameConstants.DDZ_CT_FOUR_TAKE_TWO_JX: {
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
		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}
		int magic_count = hand_card_index.card_index[15];
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
			hand_card_index.card_index[(magic_card[0])] = 0;
		}
		// 花牌单牌不能出
		if (cbHandCardCount == 2 && hand_card_index.card_index[15] == 1) {
			return false;
		}
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);

			if (hand_card_index.card_index[index] + magic_count > 0 && index > turn_index && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] + magic_count > 3 && index < 13) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
			}

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
		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}

		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
			hand_card_index.card_index[(magic_card[0])] = 0;
		} else {
			magic_count += hand_card_index.card_index[15];
		}
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[index] + magic_count > 3 && index > turn_index && index < 13) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
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

		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}
		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
		} else {
			magic_count += hand_card_index.card_index[15];
		}
		// int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[index] + magic_count > 3 && index < 13) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
			}
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
		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}
		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
		} else {
			magic_count += hand_card_index.card_index[15];
		}
		int max_san_zhang_index = -1;// 三张
		int max_dan_zhang_index = -1;// 单排或对子
		for (int i = 0; i < cbTurnCardCount; i++) {
			int index = this.switch_card_to_idnex(cbTurnCardData[i]);
			if (turn_card_index.card_index[index] > 2) {
				max_san_zhang_index = index;
			}
			if (turn_card_index.card_index[index] <= 2) {
				max_dan_zhang_index = index;
			}
		}
		for (int i = 0; i < cbHandCardCount;) {
			// 过滤三张
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[index] + magic_count >= 3 && index > max_san_zhang_index && index < 13) {
				int remain_magic_count = magic_count - (3 - hand_card_index.card_index[index]);
				for (int j = 0; j < cbHandCardCount; j++) {
					int dan_zhang_index = this.switch_card_to_idnex(cbHandCardData[j]);
					// 癞子用完了
					if (is_magic_card_data(dan_zhang_index) && magic_count == 3 - hand_card_index.card_index[index]) {
						continue;
					}
					if (dan_zhang_index == index) {
						continue;
					}
					if (hand_card_index.card_index[dan_zhang_index] + remain_magic_count >= (cbTurnCardCount - 3)) {

						return true;
					}
				}
			} else if (hand_card_index.card_index[index] + magic_count >= 4 && index < 13) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
			}
		}

		return false;
	}

	// 搜索飞机
	public boolean SearchThreeTakeOneLinkCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount, int turn_type) {

		tagAnalyseIndexResult_DDZ turn_card_index = new tagAnalyseIndexResult_DDZ();
		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbTurnCardData, cbTurnCardCount, turn_card_index);
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}

		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
			hand_card_index.card_index[(magic_card[0])] = 0;
		} else {
			magic_count += hand_card_index.card_index[15];
		}
		int max_index = -1;
		for (int i = 0; i < cbTurnCardCount;) {
			int index = this.switch_card_to_idnex(cbTurnCardData[i]);

			if (turn_card_index.card_index[index] > 2) {
				for (int j = i + turn_card_index.card_index[index]; j < cbTurnCardCount;) {
					int next_index = this.switch_card_to_idnex(cbTurnCardData[j]);
					if (turn_card_index.card_index[next_index] > 2) {
						if (turn_type == GameConstants.DDZ_CT_THREE_LINE_JX_ONE) {
							if ((index - next_index + 1) * 4 == cbTurnCardCount) {
								max_index = index;
								break;
							}
						} else if (turn_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX) {
							if ((index - next_index + 1) * 3 == cbTurnCardCount) {
								max_index = index;
								break;
							}
						} else if (turn_type == GameConstants.DDZ_CT_THREE_LINE_JX_TWO) {
							int take_count = 0;
							for (int x = 0; x < cbTurnCardCount;) {
								int take_idnex = this.switch_card_to_idnex(cbTurnCardData[x]);
								if (take_idnex > index || take_idnex < next_index) {
									take_count += turn_card_index.card_index[take_idnex] / 2;
								}
								if (turn_card_index.card_index[take_idnex] > 0) {
									x += turn_card_index.card_index[take_idnex];
								} else {
									x++;
								}
							}
							if ((index - next_index + 1) * 5 == cbTurnCardCount
									&& (index - next_index + 1) == take_count) {
								max_index = index;
								break;
							}
						}

					}
					if (turn_card_index.card_index[next_index] > 0) {
						j += turn_card_index.card_index[next_index];
					} else {
						j++;
					}

				}
			}
			if (max_index != -1) {
				break;
			}
			if (turn_card_index.card_index[index] > 0) {
				i += turn_card_index.card_index[index];
			} else {
				i++;
			}

		}
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[index] + magic_count > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] + magic_count > 2 && index > max_index && index < 12
					&& cbHandCardCount >= cbTurnCardCount) {
				int magic_count_temp = magic_count - (3 - hand_card_index.card_index[index]);
				int first_index = index;
				int prv_index = index;
				for (int j = i + hand_card_index.card_index[index]; j < cbHandCardCount;) {
					int next_index = this.switch_card_to_idnex(cbHandCardData[j]);
					if (hand_card_index.card_index[next_index] + magic_count > 3 && next_index < 13) {
						return true;
					} else if (hand_card_index.card_index[next_index] + magic_count_temp > 2
							&& next_index == prv_index - 1) {
						magic_count_temp = magic_count - (3 - hand_card_index.card_index[next_index]);
						if (turn_type == GameConstants.DDZ_CT_THREE_LINE_JX_ONE) {
							if ((first_index - next_index + 1) * 4 >= cbTurnCardCount) {
								return true;
							}
							prv_index = next_index;

						} else if (turn_type == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO_JX) {
							if ((first_index - next_index + 1) * 3 >= cbTurnCardCount) {
								return true;
							}
							prv_index = next_index;
						} else if (turn_type == GameConstants.DDZ_CT_THREE_LINE_JX_TWO) {
							// 飞机带翅膀需要判断是否够对子
							int take_count = 0;
							for (int x = 0; x < cbHandCardCount;) {
								int take_index = this.switch_card_to_idnex(cbHandCardData[x]);
								if (take_index < next_index || take_index > first_index) {
									if (hand_card_index.card_index[take_index] + magic_count_temp >= 2) {
										take_count += (hand_card_index.card_index[take_index] + magic_count_temp) / 2;
										if (((hand_card_index.card_index[take_index] + magic_count_temp) / 2)
												* 2 > hand_card_index.card_index[take_index]) {
											magic_count_temp -= (((hand_card_index.card_index[take_index]
													+ magic_count_temp) / 2) * 2)
													- hand_card_index.card_index[take_index];
										}

									}
								}
								if (hand_card_index.card_index[take_index] > 0) {
									x += hand_card_index.card_index[take_index];
								} else {
									x++;
								}

							}
							if ((first_index - next_index + 1) * 3 + take_count * 2 >= cbTurnCardCount) {
								return true;
							}
							prv_index = next_index;
						}

					} else {
						break;
					}
					if (hand_card_index.card_index[next_index] > 0) {
						j += hand_card_index.card_index[next_index];
					} else {
						j++;
					}

				}
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
			}

		}
		return false;
	}

	// 搜索对子
	public boolean SearchDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}
		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
			hand_card_index.card_index[(magic_card[0])] = 0;
		} else {
			magic_count += hand_card_index.card_index[15];
		}
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[index] + magic_count >= 2 && index > turn_index && index < 13) {
				return true;
			} else if (hand_card_index.card_index[index] + magic_count > 3 && index < 13) {
				return true;
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
			}

		}
		return false;
	}

	// 搜索连对
	public boolean SearchDoubleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);
		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}

		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
			hand_card_index.card_index[(magic_card[0])] = 0;
		} else {
			magic_count += hand_card_index.card_index[15];
		}

		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		for (int i = 0; i < cbHandCardCount;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			if (hand_card_index.card_index[15] >= 2) {
				return true;
			} else if (hand_card_index.card_index[index] + magic_count > 3 && index != 15) {
				return true;
			} else if (hand_card_index.card_index[index] + magic_count > 1 && index > turn_index && index < 12) {
				int magic_count_temp = magic_count - (2 - hand_card_index.card_index[index]);
				int prv_index = index;
				for (int j = i + hand_card_index.card_index[index]; j < cbHandCardCount;) {
					int next_index = this.switch_card_to_idnex(cbHandCardData[j]);
					if (hand_card_index.card_index[next_index] + magic_count > 3 && next_index < 13) {
						return true;
					}
					// else if ((index == 0 || index == 12) &&
					// hand_card_index.card_index[next_index] >= 2) {
					// return true;
					// }
					else if (hand_card_index.card_index[next_index] + magic_count_temp > 1
							&& next_index == prv_index - 1) {
						magic_count_temp = magic_count - (2 - hand_card_index.card_index[next_index]);
						if (magic_count_temp < 0) {
							break;
						}
						if (index - next_index + 1 >= cbTurnCardCount / 2) {
							return true;
						}
						prv_index = next_index;
					} else {
						break;
					}
					if (hand_card_index.card_index[next_index] > 0) {
						j += hand_card_index.card_index[next_index];
					} else {
						j++;
					}

				}
			}
			if (hand_card_index.card_index[index] > 0) {
				i += hand_card_index.card_index[index];
			} else {
				i++;
			}
		}
		return false;
	}

	// 搜索顺子
	public boolean SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[],
			int cbTurnCardCount) {

		tagAnalyseIndexResult_DDZ hand_card_index = new tagAnalyseIndexResult_DDZ();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_card_index);

		// 王炸
		if (hand_card_index.card_index[13] + hand_card_index.card_index[14] + hand_card_index.card_index[15] >= 2) {
			return true;
		}
		int magic_count = 0;
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += hand_card_index.card_index[(magic_card[0])];
		} else {
			magic_count += hand_card_index.card_index[15];
		}
		int turn_index = this.switch_card_to_idnex(cbTurnCardData[0]);
		int magic_count_tmep = hand_card_index.card_index[15];
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count_tmep += hand_card_index.card_index[(magic_card[0])];
		}
		int next_index = -1;
		int max_idnex = -1;

		for (int i = GameConstants.DDZ_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_card_index.card_index[i] + magic_count > 3 && i < 13) {
				return true;
			} else if (hand_card_index.card_index[15] >= 2) {
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
					magic_count_tmep = magic_count;
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
		if (has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			magic_count += real_card_index.card_index[(magic_card[0])];
		}

		if (magic_count > 0) {
			for (int i = 0; i < cbCardCount; i++) {
				for (int j = 0; j < cbCardCount; j++) {
					if (cbCardData[i] == card_data_temp[j]) {
						if (is_magic_card_data(cbCardData[i])) {
							cbCardData[i] += 0x100;
						}
						card_data_temp[j] = 0;
						break;
					}
					if (j == cbCardCount - 1 && magic_count > 0) {
						if (cbCardCount == magic_count) {
							return false;
						}
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

	/***
	 * //排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	@Override
	public void sort_card_date_list(int card_date[], int card_count) {
		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			if (card_date[i] == 0x5E || (is_magic_card_data(card_date[i]))) {
				logic_value[i] = 20 + GetCardLogicValue(card_date[i]);
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

	// 飞机 0飞机缺翅膀 1飞机
	@Override
	public int is_plane(tagAnalyseIndexResult_DDZ card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount <= 6) {
			return -1;
		}
		int num = 0;
		int max_index = 0;
		int prv_index = 0;
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			// 三个2不能当做飞机
			if (card_data_index.card_index[index] >= 3 && index != 12) {
				int link_num = 1;
				max_index = index;
				prv_index = index;
				for (int j = i + card_data_index.card_index[index]; j < cbCardCount;) {
					int next_index = this.switch_card_to_idnex(cbCardData[j]);
					if (card_data_index.card_index[next_index] >= 3 && next_index != 12) {
						if (next_index != prv_index - 1) {
							break;
						}
						prv_index = next_index;
						link_num++;
						if (link_num * 4 == cbCardCount) {
							return 1;
						} else if (link_num * 5 == cbCardCount) {
							int dan_pai_count = 0;
							for (int x = 0; x < cbCardCount;) {
								int index1 = this.switch_card_to_idnex(cbCardData[x]);
								if (index1 > max_index || index1 < next_index) {
									if (card_data_index.card_index[index1] % 2 == 0) {
										dan_pai_count += card_data_index.card_index[index1];
									} else {
										break;
									}
									if (dan_pai_count == cbCardCount - (link_num * 3)) {
										return 0;
									}
									x += card_data_index.card_index[index1];
								} else {
									if ((card_data_index.card_index[index1] - 3) % 2 == 0) {
										dan_pai_count += card_data_index.card_index[index1] - 3;
									} else {
										break;
									}
									if (dan_pai_count == cbCardCount - (link_num * 3)) {
										return 0;
									}
									if (card_data_index.card_index[index1] - 3 > 0) {
										x += card_data_index.card_index[index1] - 3;
									} else {
										x++;
									}

								}

							}
						}
						j += 3;
					} else {
						j++;
					}
				}
				i += 3;
			} else {
				i++;
			}
		}

		return -1;
	}
}
