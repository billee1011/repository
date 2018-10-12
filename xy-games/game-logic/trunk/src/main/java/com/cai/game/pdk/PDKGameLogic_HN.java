/**
 * 
 */
package com.cai.game.pdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.game.pdk.data.tagAnalyseIndexResult;

public class PDKGameLogic_HN extends PDKGameLogic {

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(PDKGameLogic_HN.class);
	private int cbIndexCount = 5;
	public int _game_rule_index; // 游戏规则
	public int _game_type_index;
	public int _laizi = GameConstants.INVALID_CARD;// 癞子牌数据

	public PDKGameLogic_HN() {

	}

	// 是否连
	public boolean is_link(int card_index[], int link_num, int link_count_num) {
		int num = 0;
		for (int i = 0; i < GameConstants.PDK_MAX_INDEX - 1; i++) {
			if (card_index[i] == 0) {
				if (num == 0) {
					continue;
				} else {
					if (num >= link_count_num) {
						return true;
					} else {
						return false;
					}
				}
			}

			if (card_index[i] == link_num) {
				num++;
			} else {
				return false;
			}
		}
		if (num >= link_count_num) {
			return true;
		} else {
			return false;
		}
	}

	// 对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = GetCardType(cbNextCard, cbNextCount, cbNextCard);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount, cbFirstCard);
		// 类型判断
		if (cbNextType == GameConstants.PDK_CT_ERROR)
			return false;
		if (cbNextType == GameConstants.PDK_CT_MISSILE_CARD)
			return true;
		if (cbFirstType == GameConstants.PDK_CT_MISSILE_CARD)
			return false;

		// 炸弹判断
		if ((cbFirstType != GameConstants.PDK_CT_BOMB_CARD) && (cbNextType == GameConstants.PDK_CT_BOMB_CARD))
			return true;
		if ((cbFirstType == GameConstants.PDK_CT_BOMB_CARD) && (cbNextType != GameConstants.PDK_CT_BOMB_CARD))
			return false;

		// 规则判断
		if ((cbFirstType != cbNextType) && cbNextType == GameConstants.PDK_CT_HONG_HUA_SHUN
				&& cbFirstType == GameConstants.PDK_CT_SINGLE_LINE)
			return true;
		if ((cbNextType == GameConstants.PDK_CT_THREE_TAKE_ONE || cbNextType == GameConstants.PDK_CT_THREE)
				&& cbFirstType == GameConstants.PDK_CT_THREE_TAKE_TWO
				&& this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		} else if (cbFirstType == GameConstants.PDK_CT_PLANE && cbNextType == GameConstants.PDK_CT_PLANE_LOST
				&& this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
			if (cbNextCount > cbFirstCount) {
				return false;
			}
			// 分析扑克
			tagAnalyseResult firstResult = new tagAnalyseResult();
			tagAnalyseResult nextResult = new tagAnalyseResult();
			AnalysebCardData(cbFirstCard, cbFirstCount, firstResult);
			AnalysebCardData(cbNextCard, cbNextCount, nextResult);
			if (cbFirstCount / 5 <= nextResult.cbThreeCount + nextResult.cbFourCount) {
				// 获取数值
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			} else {
				return false;
			}
		} else if ((cbFirstType != cbNextType)
				|| (cbFirstType != GameConstants.PDK_CT_BOMB_CARD && cbFirstCount != cbNextCount))
			return false;

		// 开始对比
		switch (cbNextType) {
		case GameConstants.PDK_CT_SINGLE:
		case GameConstants.PDK_CT_DOUBLE:
		case GameConstants.PDK_CT_THREE:
		case GameConstants.PDK_CT_SINGLE_LINE:
		case GameConstants.PDK_CT_DOUBLE_LINE:
		case GameConstants.PDK_CT_PLANE:
		case GameConstants.PDK_CT_PLANE_TAKE_DOUBLE: {
			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:
		case GameConstants.PDK_CT_THREE_TAKE_TWO:
		case GameConstants.PDK_CT_THREE_TAKE_ONE: {
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
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE: {
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
		case GameConstants.PDK_CT_BOMB_CARD: {
			// 数目判断

			// 获取数值
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
		}

		return false;
	}

	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		int card_type = GameConstants.PDK_CT_ERROR;

		if (cbCardCount == 1) {
			return GameConstants.PDK_CT_SINGLE;
		} else if (cbCardCount == 2) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] != 2 && card_index.card_index[i] != 0) {
					return GameConstants.PDK_CT_ERROR;
				}
				if (card_index.card_index[i] == 2 && card_index.card_index[i] != 0) {
					return GameConstants.PDK_CT_DOUBLE;
				}
			}
		} else if (cbCardCount == 3) {
			// 三条
			if (card_index.card_index[11] == 3) {
				if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
					return GameConstants.PDK_CT_BOMB_CARD;
				} else {
					return GameConstants.PDK_CT_THREE;
				}
			} else {
				int index = this.switch_card_to_idnex(cbCardData[0]);
				if (card_index.card_index[index] == 3) {
					return GameConstants.PDK_CT_THREE;
				} else {
					return GameConstants.PDK_CT_ERROR;
				}

			}
		} else if (cbCardCount == 4) {
			// 炸弹
			if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)
					|| this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] != 4 && card_index.card_index[i] != 0) {
						break;
					}
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_BOMB_CARD;
					}
				}
			}
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					if (i == 11 && has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
						return GameConstants.PDK_CT_BOMB_CARD;
					} else {
						return GameConstants.PDK_CT_THREE_TAKE_ONE;
					}
				}
			}

		} else if (cbCardCount == 5) {

			// 三带
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					if (this.has_rule(GameConstants.GAME_RULE_THREE_TAKE_TWO)) {
						for (int j = GameConstants.PDK_MAX_INDEX - 1; j >= 0; j--) {
							if (card_index.card_index[j] == 2) {
								return GameConstants.PDK_CT_THREE_TAKE_TWO;
							}
						}
						return GameConstants.PDK_CT_ERROR;
					} else {
						return GameConstants.PDK_CT_ERROR;
					}

				} else if (card_index.card_index[i] == 4) {
					if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
						return GameConstants.PDK_CT_BOMB_CARD;
					} else if (has_rule(GameConstants.GAME_RULE_FOUR_TAKE_ONE)) {
						return GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE;
					}

				}
			}

		} else if (cbCardCount == 6) {
			// 四带2
			if (has_rule(GameConstants.GAME_RULE_FOUR_DAI_TWO)) {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO;
					}
				}
			}

		}

		// 飞机
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] == 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] == 3) {
						if ((i - j + 1) * 4 == cbCardCount) {
							return GameConstants.PDK_CT_PLANE;
						}
						if ((i - j + 1) * 3 == cbCardCount) {
							return GameConstants.PDK_CT_PLANE_LOST;
						}
						if ((i - j + 1) * 5 == cbCardCount && this.has_rule(GameConstants.GAME_RULE_THREE_TAKE_TWO)) {
							int double_count = 0;
							for (int x = GameConstants.PDK_MAX_INDEX - 1; x >= 0; x--) {
								if (card_index.card_index[x] == 2) {
									double_count++;
								}
							}
							if (double_count == i - j + 1) {
								return GameConstants.PDK_CT_PLANE_TAKE_DOUBLE;
							}

						}
						if (j == 0) {
							return GameConstants.PDK_CT_ERROR;
						}
					} else {
						i = j;
						break;
					}
				}
			}
		}
		// 连对
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] != 2 && card_index.card_index[i] != 0) {
				break;
			}
			if (cbCardCount % 2 != 0) {
				break;
			}
			int link_number = cbCardCount / 2;
			if (has_rule(GameConstants.GAME_RULE_DOUBLE_LINK_THREE)) {
				if (link_number < 3) {
					break;
				}
			} else {
				if (link_number < 2) {
					break;
				}
			}
			if (card_index.card_index[i] == 2 && i >= link_number - 1 && link_number >= 2) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] != 2 && j > i - link_number) {
						return GameConstants.PDK_CT_ERROR;
					} else if (card_index.card_index[j] != 2 && card_index.card_index[j] != 0) {
						return GameConstants.PDK_CT_ERROR;
					}
				}
				return GameConstants.PDK_CT_DOUBLE_LINE;
			}
		}
		// 顺子
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] > 0 && i == GameConstants.PDK_MAX_INDEX - 1) {
				return GameConstants.PDK_CT_ERROR;
			}
			if (card_index.card_index[i] != 1 && card_index.card_index[i] != 0) {
				break;
			}
			if (card_index.card_index[i] == 1 && i >= cbCardCount - 1 && cbCardCount >= 5) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] != 1 && j > i - cbCardCount) {
						return GameConstants.PDK_CT_ERROR;
					} else if (card_index.card_index[j] > 1) {
						return GameConstants.PDK_CT_ERROR;
					}
				}
				return GameConstants.PDK_CT_SINGLE_LINE;
			}
		}

		return GameConstants.PDK_CT_ERROR;
	}

	// 获取能出牌数据
	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		int card_count = 0;
		int out_card_type = this.GetCardType(cbOutCardData, out_card_count, cbOutCardData);

		switch (out_card_type) {
		case GameConstants.PDK_CT_BOMB_CARD: {
			card_count += boom_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO: {
			card_count += four_take_three_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_PLANE: {
			card_count += plane_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_PLANE_TAKE_DOUBLE: {
			card_count += plane_take_two_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_THREE_TAKE_ONE: {
			card_count += three_take_one_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_THREE_TAKE_TWO: {
			card_count += three_take_double_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_DOUBLE: {
			card_count += double_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_SINGLE: {
			card_count += single_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_SINGLE_LINE: {
			card_count += single_link_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_DOUBLE_LINE: {
			card_count += double_link_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		}

		return card_count;
	}

	public int boom_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;

		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		// 炸弹
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && i > max_index) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i] && i > max_index) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		} else {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && i > max_index) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		}
		return card_count;
	}

	public int four_take_three_card_can_out(int cbOutCardData[], int out_card_count, int card_data[],
			int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, card_index);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = 0; i < out_card_count;) {
			int index = this.switch_card_to_idnex(cbOutCardData[i]);
			if (card_index.card_index[index] == 4) {
				max_index = index;
				break;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_index.card_index[i] == 4 && i > max_index && cbHandCardCount >= out_card_count) {
				for (int x = 0; x < cbHandCardCount; x++) {
					card_data[card_count++] = hand_card_data[x];
				}
				return card_count;
			}
		}
		// 炸弹
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		}

		return card_count;
	}

	public int plane_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, card_index);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}

		int max_index = -1;
		int min_index = -1;
		int plane_num = out_card_count / 4;
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] >= 3) {
				for (int j = i; i >= 0; j--) {
					if (card_index.card_index[j] >= 3 && i - j == plane_num - 1) {
						max_index = i;
						min_index = j;
						break;
					} else if (card_index.card_index[j] < 3) {
						i = j;
						break;
					}
				}
			}

			if (max_index != -1 && min_index != -1) {
				break;
			}
		}
		// 飞机
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			if (hand_index.card_index[i] >= 3 && i > max_index) {
				for (int j = i; j > min_index; j--) {
					if (hand_index.card_index[j] >= 3) {
						if (i - j == plane_num - 1) {
							if (cbHandCardCount >= out_card_count) {
								for (int x = 0; x < cbHandCardCount; x++) {
									card_data[card_count++] = hand_card_data[x];
								}
								return card_count;
							} else {
								break;
							}

						} else if (j == min_index + 1) {
							i = j;
							break;
						}
					} else {
						i = j;
						break;
					}

				}
			} else if (i <= max_index) {
				break;
			}
		}
		// 炸弹
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		}
		return card_count;
	}

	public int plane_take_two_card_can_out(int cbOutCardData[], int out_card_count, int card_data[],
			int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, card_index);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}
		int max_index = -1;
		int min_index = -1;
		int plane_num = out_card_count / 5;
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] >= 3) {
				for (int j = i; i >= 0; j--) {
					if (card_index.card_index[j] >= 3 && i - j == plane_num - 1) {
						max_index = i;
						min_index = j;
						break;
					} else if (card_index.card_index[j] < 3) {
						i = j;
						break;
					}
				}
			}

			if (max_index != -1 && min_index != -1) {
				break;
			}
		}
		// 飞机
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			if (hand_index.card_index[i] >= 3 && i > max_index) {
				for (int j = i; j > min_index; j--) {
					if (hand_index.card_index[j] >= 3) {
						if (i - j == plane_num - 1) {
							int double_count = 0;
							for (int x = 0; x < GameConstants.PDK_MAX_INDEX; x++) {
								if (x > i || x < j && hand_index.card_index[x] >= 2) {
									double_count++;
								}
							}
							if (double_count >= plane_num) {
								for (int x = j; x <= i; x++) {
									for (int y = 0; y < hand_index.card_index[x]; y++) {
										card_data[card_count++] = hand_index.card_data[x][y];
									}
								}
								for (int x = 0; x < GameConstants.PDK_MAX_INDEX; x++) {
									if (x > i || x < j && hand_index.card_index[x] >= 2) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									}
								}
								return card_count;
							} else {
								break;
							}

						} else if (j == min_index + 1) {
							i = j;
							break;
						}
					} else {
						i = j;
						break;
					}

				}
			} else if (i <= max_index) {
				break;
			}
		}
		// 炸弹
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		}

		return card_count;
	}

	public int three_take_one_card_can_out(int cbOutCardData[], int out_card_count, int card_data[],
			int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, card_index);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}
		int max_index = -1;
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] >= 3 && max_index == -1) {
				max_index = i;
				break;
			}
		}
		// 三条
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_index.card_index[i] >= 3 && i > max_index) {

				if (cbHandCardCount >= out_card_count) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			} else if (i <= max_index) {
				break;
			}
		}
		// 炸弹
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		}

		return card_count;
	}

	public int three_take_double_card_can_out(int cbOutCardData[], int out_card_count, int card_data[],
			int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, card_index);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}
		int max_index = -1;
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] >= 3 && max_index == -1) {
				max_index = i;
				break;
			}
		}
		// 三条
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_index.card_index[i] >= 3 && i > max_index) {
				boolean is_have_double = false;
				for (int j = GameConstants.PDK_MAX_INDEX - 1; j >= 0; j--) {
					if (j != i && hand_index.card_index[j] >= 2) {
						is_have_double = true;
						break;
					}
				}
				if (is_have_double) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
					for (int j = GameConstants.PDK_MAX_INDEX - 1; j >= 0; j--) {
						if (j != i && hand_index.card_index[j] >= 2) {
							for (int x = 0; x < hand_index.card_index[j]; x++) {
								card_data[card_count++] = hand_index.card_data[j][x];
							}
						}
					}
				}
				return card_count;
			} else if (i <= max_index) {
				break;
			}
		}
		// 炸弹
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		} else {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
				}
			}
		}

		return card_count;
	}

	public int double_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (i == 11 && hand_index.card_index[i] >= 3) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] >= 4) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_index.card_index[i] >= 2 && i > max_index) {
				for (int x = 0; x < hand_index.card_index[i]; x++) {
					card_data[card_count++] = hand_index.card_data[i][x];
				}
			} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
				if (hand_index.card_index[i] == 4) {
					for (int y = 0; y < hand_index.card_index[i]; y++) {
						card_data[card_count++] = hand_index.card_data[i][y];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int y = 0; y < hand_index.card_index[i]; y++) {
						card_data[card_count++] = hand_index.card_data[i][y];
					}
				}
			} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
				if (hand_index.card_index[i] == 4) {
					for (int y = 0; y < hand_index.card_index[i]; y++) {
						card_data[card_count++] = hand_index.card_data[i][y];
					}
				}
			}
		}
		return card_count;
	}

	public int single_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}

		}
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (hand_index.card_index[i] >= 1 && i > max_index) {
				for (int x = 0; x < hand_index.card_index[i]; x++) {
					card_data[card_count++] = hand_index.card_data[i][x];
				}
			} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
				if (hand_index.card_index[i] == 4) {
					for (int y = 0; y < hand_index.card_index[i]; y++) {
						card_data[card_count++] = hand_index.card_data[i][y];
					}
				} else if (hand_index.card_index[i] == 3 && i == 11) {
					for (int y = 0; y < hand_index.card_index[i]; y++) {
						card_data[card_count++] = hand_index.card_data[i][y];
					}
				}
			} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAAA)) {
				if (hand_index.card_index[i] == 4) {
					for (int y = 0; y < hand_index.card_index[i]; y++) {
						card_data[card_count++] = hand_index.card_data[i][y];
					}
				}
			}
		}
		return card_count;
	}

	public int single_link_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {

		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int card_count = 0;
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			// 从比出的牌大的牌找起找出顺子
			if (hand_index.card_index[i] > 0 && i > max_index) {
				for (int j = i - 1; j > min_index; j--) {
					if (hand_index.card_index[j] == 0) {
						// 有顺子
						if (i - j >= out_card_count) {
							for (int x = i; x > j; x--) {
								for (int y = 0; y < hand_index.card_index[x]; y++) {
									card_data[card_count++] = hand_index.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x > j; x--) {
								if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
									if (hand_index.card_index[x] == 4) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									} else if (hand_index.card_index[x] == 3 && x == 11) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									}
								} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
									if (hand_index.card_index[x] == 4 && cbHandCardCount > hand_index.card_index[x]) {
										for (int y = 0; y < cbHandCardCount; y++) {
											card_data[card_count++] = hand_card_data[y];
										}
										return card_count;
									} else if (hand_index.card_index[x] == 3 && x == 11
											&& cbHandCardCount > hand_index.card_index[x]) {
										for (int y = 0; y < cbHandCardCount; y++) {
											card_data[card_count++] = hand_card_data[y];
										}
										return card_count;
									}
								} else {
									if (hand_index.card_index[x] == 4) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									}
								}
							}
						}
						i = j;
						break;
					} else if (j == min_index + 1) {
						// 有顺子
						if (i - j >= out_card_count - 1) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < hand_index.card_index[x]; y++) {
									card_data[card_count++] = hand_index.card_data[x][y];
								}

							}
						}
						i = j;
					}
				}
			} else {
				if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
					if (hand_index.card_index[i] == 4) {
						for (int y = 0; y < hand_index.card_index[i]; y++) {
							card_data[card_count++] = hand_index.card_data[i][y];
						}
					} else if (hand_index.card_index[i] == 3 && i == 11) {
						for (int y = 0; y < hand_index.card_index[i]; y++) {
							card_data[card_count++] = hand_index.card_data[i][y];
						}
					}
				} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
					if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
						for (int x = 0; x < cbHandCardCount; x++) {
							card_data[card_count++] = hand_card_data[x];
						}
						return card_count;
					} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
						for (int x = 0; x < cbHandCardCount; x++) {
							card_data[card_count++] = hand_card_data[x];
						}
						return card_count;
					}
				} else {
					if (hand_index.card_index[i] == 4) {
						for (int y = 0; y < hand_index.card_index[i]; y++) {
							card_data[card_count++] = hand_index.card_data[i][y];
						}
					}
				}

			}
		}
		return card_count;
	}

	public int double_link_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {

		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int card_count = 0;
		if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
					for (int x = 0; x < cbHandCardCount; x++) {
						card_data[card_count++] = hand_card_data[x];
					}
					return card_count;
				}
			}
		}

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			// 从比出的牌大的牌找起找出顺子
			if (hand_index.card_index[i] > 1 && i > max_index) {
				for (int j = i - 1; j > min_index; j--) {
					if (hand_index.card_index[j] < 2) {
						// 有顺子
						if (i - j >= out_card_count / 2) {
							for (int x = i; x > j; x--) {
								for (int y = 0; y < hand_index.card_index[x]; y++) {
									card_data[card_count++] = hand_index.card_data[x][y];
								}
							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x > j; x--) {
								if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
									if (hand_index.card_index[x] == 4) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									} else if (hand_index.card_index[x] == 3 && x == 11) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									}
								} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
									if (hand_index.card_index[x] == 4 && cbHandCardCount > hand_index.card_index[x]) {
										for (int y = 0; y < cbHandCardCount; y++) {
											card_data[card_count++] = hand_card_data[y];
										}
										return card_count;
									} else if (hand_index.card_index[x] == 3 && x == 11
											&& cbHandCardCount > hand_index.card_index[x]) {
										for (int y = 0; y < cbHandCardCount; y++) {
											card_data[card_count++] = hand_card_data[y];
										}
										return card_count;
									}
								} else {
									if (hand_index.card_index[x] == 4) {
										for (int y = 0; y < hand_index.card_index[x]; y++) {
											card_data[card_count++] = hand_index.card_data[x][y];
										}
									}
								}
							}
						}
						i = j;
						break;
					} else if (j == min_index + 1) {
						if (i - j >= out_card_count / 2 - 1) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < hand_index.card_index[x]; y++) {
									card_data[card_count++] = hand_index.card_data[x][y];
								}
							}
						}
						i = j;
					}
				}
			} else {
				if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_AAA)) {
					if (hand_index.card_index[i] == 4) {
						for (int y = 0; y < hand_index.card_index[i]; y++) {
							card_data[card_count++] = hand_index.card_data[i][y];
						}
					} else if (hand_index.card_index[i] == 3 && i == 11) {
						for (int y = 0; y < hand_index.card_index[i]; y++) {
							card_data[card_count++] = hand_index.card_data[i][y];
						}
					}
				} else if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
					if (hand_index.card_index[i] == 4 && cbHandCardCount > hand_index.card_index[i]) {
						for (int x = 0; x < cbHandCardCount; x++) {
							card_data[card_count++] = hand_card_data[x];
						}
						return card_count;
					} else if (hand_index.card_index[i] == 3 && i == 11 && cbHandCardCount > hand_index.card_index[i]) {
						for (int x = 0; x < cbHandCardCount; x++) {
							card_data[card_count++] = hand_card_data[x];
						}
						return card_count;
					}
				} else {
					if (hand_index.card_index[i] == 4) {
						for (int y = 0; y < hand_index.card_index[i]; y++) {
							card_data[card_count++] = hand_index.card_data[i][y];
						}
					}
				}

			}
		}
		return card_count;
	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		switch (type) {
		case GameConstants.PDK_CT_THREE_TAKE_ONE: {
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(card_date, card_count, hand_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.switch_card_to_idnex(card_date[i]);
				if (hand_index.card_index[index] >= 3) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_date[count++] = hand_index.card_data[index][j];
					}
					break;
				}
			}
			for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
				if (hand_index.card_index[i] < 3) {
					for (int j = 0; j < hand_index.card_index[i]; j++) {
						card_date[count++] = hand_index.card_data[i][j];
					}
				}
			}
			break;
		}
		case GameConstants.PDK_CT_BOMB_CARD: {
			if (this.has_rule(GameConstants.GAME_RULE_BOOM_FOUR_TAKE_ONE)) {
				tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
				AnalysebCardDataToIndex(card_date, card_count, hand_index);
				int count = 0;
				for (int i = 0; i < card_count; i++) {
					int index = this.switch_card_to_idnex(card_date[i]);
					if (hand_index.card_index[index] >= 3) {
						for (int j = 0; j < hand_index.card_index[index]; j++) {
							card_date[count++] = hand_index.card_data[index][j];
						}
						break;
					}
				}
				for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
					if (hand_index.card_index[i] < 3) {
						for (int j = 0; j < hand_index.card_index[i]; j++) {
							card_date[count++] = hand_index.card_data[i][j];
						}
					}
				}
			}

			break;
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO: {
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(card_date, card_count, hand_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.switch_card_to_idnex(card_date[i]);
				if (hand_index.card_index[index] >= 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_date[count++] = hand_index.card_data[index][j];
					}
					break;
				}
			}
			for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
				if (hand_index.card_index[i] < 4) {
					for (int j = 0; j < hand_index.card_index[i]; j++) {
						card_date[count++] = hand_index.card_data[i][j];
					}
				}
			}

			break;
		}
		case GameConstants.PDK_CT_THREE_TAKE_TWO: {
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(card_date, card_count, hand_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.switch_card_to_idnex(card_date[i]);
				if (hand_index.card_index[index] >= 3) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_date[count++] = hand_index.card_data[index][j];
					}
					break;
				}
			}
			for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
				if (hand_index.card_index[i] < 3) {
					for (int j = 0; j < hand_index.card_index[i]; j++) {
						card_date[count++] = hand_index.card_data[i][j];
					}
				}
			}
			break;
		}
		case GameConstants.PDK_CT_PLANE: {
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(card_date, card_count, hand_index);
			int count = 0;
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] >= 3) {
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (hand_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							if ((i - prv_index) + 1 == card_count / 4) {
								for (int x = i; x >= j; x--) {
									for (int y = 0; y < 3; y++) {
										int card_temp[] = new int[1];
										card_temp[0] = hand_index.card_data[x][0];
										card_date[count] = card_temp[0];
										this.remove_cards_by_data(hand_index.card_data[x], hand_index.card_index[x],
												card_temp, 1);
										hand_index.card_index[x]--;
										count++;
									}
								}
								break;
							}
						} else {
							break;
						}
					}
				}
				if (count != 0) {
					break;
				}
			}

			for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
				for (int j = 0; j < hand_index.card_index[i]; j++) {
					card_date[count++] = hand_index.card_data[i][j];
				}
			}
			break;
		}
		case GameConstants.PDK_CT_PLANE_TAKE_DOUBLE: {
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			AnalysebCardDataToIndex(card_date, card_count, hand_index);
			int count = 0;
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (hand_index.card_index[i] >= 3) {
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (hand_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							if ((i - prv_index) + 1 == card_count / 4) {
								for (int x = j; x <= i; x++) {
									for (int y = 0; y < 3; y++) {
										int card_temp[] = new int[1];
										card_temp[0] = hand_index.card_data[x][0];
										card_date[count] = card_temp[0];
										this.remove_cards_by_data(hand_index.card_data[x], hand_index.card_index[x],
												card_temp, 1);
										hand_index.card_index[x]--;
										count++;
									}
								}
								break;
							}
						} else {
							break;
						}
					}
				}
				if (count != 0) {
					break;
				}
			}

			for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
				for (int j = 0; j < hand_index.card_index[i]; j++) {
					card_date[count++] = hand_index.card_data[i][j];
				}
			}
			break;
		}
		}
	}

	public int adjustAutoOutCard(int cbHandCardData[], int cbHandCardCount) {
		int cardtype = GetCardType(cbHandCardData, cbHandCardCount, cbHandCardData);
		if (cardtype != GameConstants.PDK_CT_ERROR) {
			int cbTmpCardData[] = new int[cbHandCardCount];
			for (int i = 0; i < cbHandCardCount; i++) {
				cbTmpCardData[i] = cbHandCardData[i];
			}

			int cbBomCardCount = 0;

			if (cbHandCardCount < 2)
				return cardtype;

			// 双王炸弹
			if (0x4F == cbTmpCardData[0] && 0x4F == cbTmpCardData[1] && 0x4E == cbTmpCardData[2]
					&& 0x4E == cbTmpCardData[3]) {
				return GameConstants.PDK_CT_ERROR;
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

				if (cbSameCount >= 4 && cbHandCardCount != cbSameCount) {
					return GameConstants.PDK_CT_ERROR;
				}

				if (cbSameCount == 3 && cbLogicValue == 14 && cbHandCardCount != cbSameCount) {
					return GameConstants.PDK_CT_ERROR;
				}

				// 设置索引
				i += cbSameCount - 1;

			}
			return cardtype;
		}
		return GameConstants.PDK_CT_ERROR;
	}
}
