/**
 * 
 */
package com.cai.game.pdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.game.pdk.data.tagAnalyseIndexResult;

public class PDKGameLogic_SC extends PDKGameLogic {

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(PDKGameLogic_SC.class);
	private int cbIndexCount = 5;
	public int _game_rule_index; // 游戏规则
	public int _game_type_index;
	public int _laizi = GameConstants.INVALID_CARD;// 癞子牌数据

	public PDKGameLogic_SC() {

	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount, int cbRealData[]) {
		int cbTempData[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			cbTempData[i] = cbCardData[i];
		}

		if (cbCardCount == 1) {
			return GameConstants.PDK_CT_SINGLE;
		}
		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbTempData, cbCardCount, AnalyseResult);

		// 四张
		if (AnalyseResult.cbFourCount > 0) {
			if (cbCardCount == 4) {
				return GameConstants.PDK_CT_BOMB_CARD;
			} else {
				return GameConstants.PDK_CT_ERROR;
			}
		}
		// 三张
		if (AnalyseResult.cbThreeCount > 0) {
			if (cbCardCount == 3) {
				return GameConstants.PDK_CT_THREE;
			}
		}
		// 对子
		if (cbCardCount == 2) {
			if (AnalyseResult.cbDoubleCount > 0) {
				return GameConstants.PDK_CT_DOUBLE;
			} else {
				return GameConstants.PDK_CT_ERROR;
			}
		}

		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);

		// 单连双连
		for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
			if (card_index[i] == 0) {
				continue;
			} else if (card_index[i] == 1) {
				if (is_link(card_index, card_index[i], 3)) {
					return GameConstants.PDK_CT_SINGLE_LINE;
				} else {
					return GameConstants.PDK_CT_ERROR;
				}

			} else if (card_index[i] == 2) {
				int link_num = 2;
				if (has_rule(GameConstants.GAME_RULE_LIAN_DUI_THREE)) {
					link_num = 3;
				}
				if (is_link(card_index, card_index[i], link_num)) {
					return GameConstants.PDK_CT_DOUBLE_LINE;
				} else {
					return GameConstants.PDK_CT_ERROR;
				}
			} else if (card_index[i] == 3 && !has_rule(GameConstants.GAME_RULE_BOOM_THREE)
					&& has_rule(GameConstants.GAME_RULE_PLANE)) {
				int link_num = 2;
				if (is_link(card_index, card_index[i], link_num)) {
					return GameConstants.PDK_CT_THREE_LINE;
				} else {
					return GameConstants.PDK_CT_ERROR;
				}
			} else {
				return GameConstants.PDK_CT_ERROR;
			}
		}

		return GameConstants.PDK_CT_ERROR;
	}

	public int get_card_index_count(int card_index[]) {
		int count = 0;
		for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
			count += card_index[i];
		}
		return count;
	}

	// 是否连
	public boolean is_link(int card_index[], int link_num, int link_count_num) {
		int num = 0;
		int card_count = this.get_card_index_count(card_index);
		for (int i = 0; i < GameConstants.PDK_MAX_INDEX - 1; i++) {
			if (card_index[i] == 0) {
				if (num == 0) {
					continue;
				} else {
					if (num >= link_count_num && card_count == num * link_num) {
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
		if (num >= link_count_num && card_count == num * link_num) {
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

		if (cbNextType != cbFirstType) {
			if (cbNextType == GameConstants.PDK_CT_THREE) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					if (cbFirstType == GameConstants.PDK_CT_BOMB_CARD) {
						if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
							return false;
						} else {
							return true;
						}
					} else {
						return true;
					}
				} else {
					return false;
				}
			}
			if (cbNextType == GameConstants.PDK_CT_BOMB_CARD) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		} else {
			// 获取数值
			if (cbFirstCount != cbNextCount) {
				return false;
			}
			int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
			int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

			// 对比扑克
			return cbNextLogicValue > cbFirstLogicValue;
		}
	}

	public int adjustAutoOutCard(int cbHandCardData[], int cbHandCardCount) {
		int cardtype = GetCardType(cbHandCardData, cbHandCardCount, cbHandCardData);
		return cardtype;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	// 获取能出牌数据
	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		int card_count = 0;
		int out_card_type = this.GetCardType(cbOutCardData, out_card_count, cbOutCardData);

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(hand_card_data, cbHandCardCount, AnalyseResult);
		switch (out_card_type) {
		case GameConstants.PDK_CT_BOMB_CARD: {
			card_count += boom_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants.PDK_CT_THREE: {
			card_count += three_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
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
		case GameConstants.PDK_CT_THREE_LINE: {
			card_count += three_link_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data,
					cbHandCardCount);
			return card_count;
		}
		}

		return card_count;
	}

	public int boom_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (has_rule(GameConstants.GAME_RULE_BOOM_THREE) && !has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
				if (index_card.card_index[i] >= 3) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}
			} else {
				if (index_card.card_index[i] == 4 && i > max_index) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}
			}

		}
		return card_count;
	}

	public int three_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (index_card.card_index[i] > 2 && i > max_index) {
				for (int x = 0; x < index_card.card_index[i]; x++) {
					card_data[card_count++] = index_card.card_data[i][x];
				}
			} else if (index_card.card_index[i] == 4) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}

			}
		}
		return card_count;
	}

	public int double_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (index_card.card_index[i] >= 2 && i > max_index) {
				for (int x = 0; x < index_card.card_index[i]; x++) {
					card_data[card_count++] = index_card.card_data[i][x];
				}
			} else if (index_card.card_index[i] == 4) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR) || has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}
			} else if (index_card.card_index[i] >= 3) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}
			}
		}
		return card_count;
	}

	public int single_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (index_card.card_index[i] >= 1 && i > max_index) {
				for (int x = 0; x < index_card.card_index[i]; x++) {
					card_data[card_count++] = index_card.card_data[i][x];
				}
			} else if (index_card.card_index[i] == 4) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR) || has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}
			} else if (index_card.card_index[i] >= 3) {
				if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					for (int x = 0; x < index_card.card_index[i]; x++) {
						card_data[card_count++] = index_card.card_data[i][x];
					}
				}
			}
		}
		return card_count;
	}

	public int single_link_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {

		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int card_count = 0;

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		int prv_index = this.switch_card_to_idnex(hand_card_data[0]);
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			// 从比出的牌大的牌找起找出顺子
			if (index_card.card_index[i] > 0 && i > max_index) {
				for (int j = i - 1; j > min_index; j--) {
					if (index_card.card_index[j] == 0) {
						// 有顺子
						if (i - j >= out_card_count) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < index_card.card_index[x]; y++) {
									card_data[card_count++] = index_card.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x >= j; x--) {
								if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
									if (index_card.card_index[x] == 4) {
										for (int y = 0; y < index_card.card_index[x]; y++) {
											card_data[card_count++] = index_card.card_data[x][y];
										}
									}
								}
								if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
									if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
										if (index_card.card_index[x] == 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									} else {
										if (index_card.card_index[x] >= 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									}

								}

							}
						}
						i = j;
						break;
					} else if (j == min_index + 1) {
						if (i - j >= out_card_count - 1) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < index_card.card_index[x]; y++) {
									card_data[card_count++] = index_card.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x >= j; x--) {
								if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
									if (index_card.card_index[x] == 4) {
										for (int y = 0; y < index_card.card_index[x]; y++) {
											card_data[card_count++] = index_card.card_data[x][y];
										}
									}
								}
								if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
									if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
										if (index_card.card_index[x] == 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									} else {
										if (index_card.card_index[x] >= 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									}

								}

							}
						}
						i = j;
						break;
					}
				}
			} else {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
					if (index_card.card_index[i] == 4) {
						for (int y = 0; y < index_card.card_index[i]; y++) {
							card_data[card_count++] = index_card.card_data[i][y];
						}
					}
				}
				if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
						if (index_card.card_index[i] == 3) {
							for (int y = 0; y < index_card.card_index[i]; y++) {
								card_data[card_count++] = index_card.card_data[i][y];
							}
						}
					} else {
						if (index_card.card_index[i] >= 3) {
							for (int y = 0; y < index_card.card_index[i]; y++) {
								card_data[card_count++] = index_card.card_data[i][y];
							}
						}
					}

				}
			}
		}
		return card_count;
	}

	public int double_link_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {

		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int card_count = 0;
		int link_num = 1;

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			// 从比出的牌大的牌找起找出顺子
			if (index_card.card_index[i] > 1 && i > max_index) {
				for (int j = i - 1; j > min_index; j--) {
					if (index_card.card_index[j] < 2) {
						// 有顺子
						if (i - j >= out_card_count / 2) {
							for (int x = i; x > j; x--) {
								for (int y = 0; y < index_card.card_index[x]; y++) {
									card_data[card_count++] = index_card.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x >= j; x--) {
								if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
									if (index_card.card_index[x] == 4) {
										for (int y = 0; y < index_card.card_index[x]; y++) {
											card_data[card_count++] = index_card.card_data[x][y];
										}
									}
								}
								if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
									if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
										if (index_card.card_index[x] == 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									} else {
										if (index_card.card_index[x] >= 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									}

								}

							}
						}
						i = j;
						break;
					} else if (j == min_index + 1) {
						// 有顺子
						if (i - j >= out_card_count / 2 - 1) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < index_card.card_index[x]; y++) {
									card_data[card_count++] = index_card.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x >= j; x--) {
								if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
									if (index_card.card_index[x] == 4) {
										for (int y = 0; y < index_card.card_index[x]; y++) {
											card_data[card_count++] = index_card.card_data[x][y];
										}
									}
								}
								if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
									if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
										if (index_card.card_index[x] == 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									} else {
										if (index_card.card_index[x] >= 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									}

								}

							}
						}
						i = j;
						break;
					}
				}
			} else {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
					if (index_card.card_index[i] == 4) {
						for (int y = 0; y < index_card.card_index[i]; y++) {
							card_data[card_count++] = index_card.card_data[i][y];
						}
					}
				}
				if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
						if (index_card.card_index[i] == 3) {
							for (int y = 0; y < index_card.card_index[i]; y++) {
								card_data[card_count++] = index_card.card_data[i][y];
							}
						}
					} else {
						if (index_card.card_index[i] >= 3) {
							for (int y = 0; y < index_card.card_index[i]; y++) {
								card_data[card_count++] = index_card.card_data[i][y];
							}
						}
					}

				}
			}
		}
		return card_count;
	}

	public int three_link_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[],
			int cbHandCardCount) {

		tagAnalyseIndexResult index_card = new tagAnalyseIndexResult();
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, index_card);
		this.switch_to_card_index(cbOutCardData, out_card_count, card_index);
		int card_count = 0;
		int link_num = 3;

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		for (int i = GameConstants.PDK_MAX_INDEX - 2; i >= 0; i--) {
			// 从比出的牌大的牌找起找出顺子
			if (index_card.card_index[i] >= link_num && i > max_index) {
				for (int j = i - 1; j > min_index; j--) {
					if (index_card.card_index[j] < link_num) {
						// 有顺子
						if (i - j >= out_card_count / link_num) {
							for (int x = i; x > j; x--) {
								for (int y = 0; y < index_card.card_index[x]; y++) {
									card_data[card_count++] = index_card.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x >= j; x--) {
								if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
									if (index_card.card_index[x] == 4) {
										for (int y = 0; y < index_card.card_index[x]; y++) {
											card_data[card_count++] = index_card.card_data[x][y];
										}
									}
								}
								if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
									if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
										if (index_card.card_index[x] == 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									} else {
										if (index_card.card_index[x] >= 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									}

								}

							}
						}
						i = j;
						break;
					} else if (j == min_index + 1) {
						// 有顺子
						if (i - j >= out_card_count / link_num - 1) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < index_card.card_index[x]; y++) {
									card_data[card_count++] = index_card.card_data[x][y];
								}

							}
						} else {
							// 凑不成顺子，找出炸弹
							for (int x = i; x >= j; x--) {
								if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
									if (index_card.card_index[x] == 4) {
										for (int y = 0; y < index_card.card_index[x]; y++) {
											card_data[card_count++] = index_card.card_data[x][y];
										}
									}
								}
								if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
									if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
										if (index_card.card_index[x] == 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									} else {
										if (index_card.card_index[x] >= 3) {
											for (int y = 0; y < index_card.card_index[x]; y++) {
												card_data[card_count++] = index_card.card_data[x][y];
											}
										}
									}

								}

							}
						}
						i = j;
						break;
					}
				}
			} else {
				if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
					if (index_card.card_index[i] == 4) {
						for (int y = 0; y < index_card.card_index[i]; y++) {
							card_data[card_count++] = index_card.card_data[i][y];
						}
					}
				}
				if (has_rule(GameConstants.GAME_RULE_BOOM_THREE)) {
					if (has_rule(GameConstants.GAME_RULE_BOOM_FOUR)) {
						if (index_card.card_index[i] == 3) {
							for (int y = 0; y < index_card.card_index[i]; y++) {
								card_data[card_count++] = index_card.card_data[i][y];
							}
						}
					} else {
						if (index_card.card_index[i] >= 3) {
							for (int y = 0; y < index_card.card_index[i]; y++) {
								card_data[card_count++] = index_card.card_data[i][y];
							}
						}
					}

				}
			}
		}
		return card_count;
	}

	public void get_card_type_award(PDKTable table, int type_data[][], int type_count[]) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			boolean is_quan_da = false;
			boolean is_quan_xiao = false;
			boolean is_quan_hei = false;
			boolean is_quan_hong = false;
			boolean is_quan_dan = true;
			boolean is_quan_shuang = true;
			int card_index[] = new int[GameConstants.PDK_MAX_INDEX];
			switch_to_card_index(table.GRR._cards_data[i], table.GRR._card_count[i], card_index);
			if (GetCardLogicValue(table.GRR._cards_data[i][0]) >= 10) {
				is_quan_da = true;
			}
			if (GetCardLogicValue(table.GRR._cards_data[i][0]) < 10) {
				is_quan_xiao = true;
			}
			if (card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][0])] == 2
					|| card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][0])] == 4) {
				is_quan_shuang = true;
				is_quan_dan = false;
			}
			for (int index = 0; index < GameConstants.PDK_MAX_INDEX; index++) {
				if (index == 2 && card_index[index] == 4) {
					if (has_rule(GameConstants.GAME_RULE_FOUR_FIVE)) {
						type_data[i][type_count[i]++] = 5;
					}
				} else if (index == 11 && card_index[index] == 4) {
					if (has_rule(GameConstants.GAME_RULE_FOUR_A)) {
						type_data[i][type_count[i]++] = 1;
					}
				} else if (card_index[index] == 4) {
					if (has_rule(GameConstants.GAME_RULE_FOUR_K)) {
						type_data[i][type_count[i]++] = this.switch_idnex_to_data(index);
					}
				}
			}

			if (card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][0])] == 1) {
				is_quan_dan = true;
				is_quan_shuang = false;
			}
			if (get_card_color(table.GRR._cards_data[i][0]) % 2 == 0) {
				is_quan_hong = true;
			}
			if (get_card_color(table.GRR._cards_data[i][0]) % 2 == 1) {
				is_quan_hei = true;
			}
			for (int j = 1; j < table.GRR._card_count[i]; j++) {
				if (GetCardLogicValue(table.GRR._cards_data[i][j]) >= 10) {
					is_quan_da = true;
				}
				if (GetCardLogicValue(table.GRR._cards_data[i][j]) < 10) {
					is_quan_xiao = true;
				}
				if (card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][j])] == 2
						|| card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][j])] == 4) {
					if (is_quan_shuang) {
						is_quan_shuang = true;
					}
					is_quan_dan = false;
				}
				if (card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][j])] == 1) {
					if (is_quan_dan) {
						is_quan_dan = true;
					}
					is_quan_shuang = false;
				}
				if (card_index[this.switch_card_to_idnex(table.GRR._cards_data[i][j])] == 3) {
					is_quan_dan = false;
					is_quan_shuang = false;
				}
				if (get_card_color(table.GRR._cards_data[i][j]) % 2 == 0) {
					is_quan_hong = true;
				}
				if (get_card_color(table.GRR._cards_data[i][j]) % 2 == 1) {
					is_quan_hei = true;
				}
			}
			if (is_quan_da != is_quan_xiao) {
				if (is_quan_da) {
					if (has_rule(GameConstants.GAME_RULE_QUAN_DA)) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_QUAN_DA_PDK);
					}
				} else {
					if (has_rule(GameConstants.GAME_RULE_QUAN_XIAO)) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_QUAN_XIAO_PDK);
					}
				}

			}
			if (is_quan_shuang != is_quan_dan) {
				if (is_quan_shuang) {
					if (has_rule(GameConstants.GAME_RULE_QUAN_SHUANG)) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_QUAN_SHUANG_PDK);
					}
				}
				if (is_quan_dan) {
					if (has_rule(GameConstants.GAME_RULE_QUAN_DAN)) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_QUAN_DAN_PDK);
					}

				}
			}
			if (is_quan_hong != is_quan_hei) {
				if (is_quan_hong) {
					if (has_rule(GameConstants.GAME_RULE_QUAN_HONG)) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_QUAN_HONG_PDK);
					}
				} else {
					if (has_rule(GameConstants.GAME_RULE_QUAN_HEI)) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_QUAN_HEI_PDK);
					}
				}
			}

		}
	}
}
