package com.cai.game.dtz;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_DTZ;
import com.cai.common.util.RandomUtil;

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
	int cbEightCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 八张扑克
	int cbSevenCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 七张扑克
	int cbSixCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 六张扑克
	int cbFiveCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 五张扑克
	int cbFourCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 四张扑克
	int cbThreeCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 三张扑克
	int cbDoubleCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 两张扑克
	int cbSignedCardData[] = new int[GameConstants_DTZ.MAX_COUNT * 3]; // 单张扑克

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

class tagAnalyseIndexResult {
	public int card_index[] = new int[GameConstants_DTZ.MAX_INDEX];
	public int card_data[][] = new int[GameConstants_DTZ.MAX_INDEX][16];
	public int card_index_color[] = new int[GameConstants_DTZ.MAX_INDEX_COLOR];
	public int card_data_color[][] = new int[GameConstants_DTZ.MAX_INDEX_COLOR][4];

	public tagAnalyseIndexResult() {
		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX_COLOR; i++) {
			card_index_color[i] = 0;
			Arrays.fill(card_data_color[i], 0);
		}
	}

	public void Reset() {
		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX_COLOR; i++) {
			card_index_color[i] = 0;
			Arrays.fill(card_data_color[i], 0);
		}
	}
};

public class DTZGameLogic {

	public int _laizi = GameConstants.INVALID_CARD;// 癞子牌数据

	public int getMagicCardScore(int card_type, int card, int[] score_detail_count, int[] score_detail) {
		if (card_type == GameConstants_DTZ.CT_BOMB_CARD_DI) {
			score_detail_count[3] += 1;
			score_detail[3] += 400;
			return 400;
		}
		if (card_type == GameConstants_DTZ.CT_BOMB_CARD_TONG && GetCardLogicValue(card) == 13) {
			score_detail_count[4] += 1;
			score_detail[4] += 100;
			return 100;
		}
		if (card_type == GameConstants_DTZ.CT_BOMB_CARD_TONG && GetCardLogicValue(card) == 14) {
			score_detail_count[5] += 1;
			score_detail[5] += 200;
			return 200;
		}
		if (card_type == GameConstants_DTZ.CT_BOMB_CARD_TONG && GetCardLogicValue(card) == 15) {
			score_detail_count[6] += 1;
			score_detail[6] += 300;
			return 300;
		}
		return 0;
	}

	public int GetCardScore(int cbCardData[], int cbCardCount, int[] score_detail_count, int[] score_detail) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardLogicValue(cbCardData[i]) == 5) {
				score += 5;
				score_detail_count[0] += 1;
				score_detail[0] += 5;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 10) {
				score += 10;
				score_detail_count[1] += 1;
				score_detail[1] += 10;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 13) {
				score += 10;
				score_detail_count[2] += 1;
				score_detail[2] += 10;
			}
		}
		return score;
	}

	public void getCardScoreDetail(int cbCardData[], int cbCardCount, int[] score) {
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardLogicValue(cbCardData[i]) == 5) {
				score[0] += 5;
				score[3]++;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 10) {
				score[1] += 10;
				score[4]++;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 13) {
				score[2] += 10;
				score[5]++;
			}
		}
	}

	// 机器人算法
	public int Ai_Out_Card(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[], int out_card_count, int card_data[], Table_DTZ table) {
		int card_count = 0;
		// 分析扑克
		this.sort_card_date_list(cbHandCardData, cbHandCardCount);
		tagAnalyseResult CardDataResult = new tagAnalyseResult();
		AnalysebCardData(cbHandCardData, cbHandCardCount, CardDataResult);
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, card_index);

		if (out_card_count != 0) {
			int card_type = GetCardType(cbOutCardData, out_card_count);
			boolean auto_bom = false;
			// 单牌
			if (card_type == GameConstants_DTZ.CT_SINGLE) {
				auto_bom = true;
				card_count = 0;
				for (int i = CardDataResult.cbSignedCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbSignedCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbSignedCardData[i];
						return card_count;
					}
				}
				for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 2) {
					if (GetCardLogicValue(CardDataResult.cbDoubleCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i];
						return card_count;
					}
				}
				for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i = i - 3) {
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i];
						return card_count;
					}
				}
			}
			// 对子
			if (card_type == GameConstants_DTZ.CT_DOUBLE) {
				auto_bom = true;
				card_count = 0;
				for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
					if (GetCardLogicValue(CardDataResult.cbDoubleCardData[i * 2]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
						card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
						return card_count;
					}
				}
				for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i = i - 1) {
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						return card_count;
					}
				}
			}
			if (card_type == GameConstants_DTZ.CT_DOUBLE_LINE) {
				auto_bom = true;
				card_count = 0;
				for (int i = cbHandCardCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(cbHandCardData[i]) == 15) {
						continue;
					}
					if (i >= out_card_count - 1 && GetCardLogicValue(cbHandCardData[i]) > GetCardLogicValue(cbOutCardData[out_card_count - 1])
							&& GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])) {
						int count = 1;
						int index = i - 2;
						for (; index > 0; index--) {
							if (GetCardLogicValue(cbHandCardData[index]) == 15) {
								continue;
							}
							if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])
									&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(cbHandCardData[index - 1])) {
								count++;
								index--;
							}
						}
						if (count >= out_card_count / 2) {
							card_data[card_count++] = cbHandCardData[i];
							card_data[card_count++] = cbHandCardData[i - 1];
							for (int j = i; j >= index; j--) {
								if (GetCardLogicValue(cbHandCardData[j - 2]) - 1 == GetCardLogicValue(cbHandCardData[j])) {
									card_data[card_count++] = cbHandCardData[j - 2];
									card_data[card_count++] = cbHandCardData[j - 3];
									count--;
									j--;
									if (count == 1) {
										return out_card_count;
									}
								}
							}
						}
						i--;
					}
				}
			}
			// 三张
			if (card_type == GameConstants_DTZ.CT_THREE || card_type == GameConstants_DTZ.CT_THREE_TAKE_ONE
					|| card_type == GameConstants_DTZ.CT_THREE_TAKE_TWO) {
				auto_bom = true;
				card_count = 0;
				for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i--) {
					int card_1_color = get_card_color(CardDataResult.cbThreeCardData[i * 3]);
					int card_2_color = get_card_color(CardDataResult.cbThreeCardData[i * 3 + 1]);
					int card_3_color = get_card_color(CardDataResult.cbThreeCardData[i * 3 + 2]);
					if (card_1_color == card_2_color && card_2_color == card_3_color) {
						continue;
					}
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
						card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 2];
						for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbSignedCardData[j];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
						for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
							if (i == j) {
								continue;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
							if (card_count == 5) {
								return card_count;
							}
							card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
							if (card_count == 5) {
								return card_count;
							}
						}
						return card_count;
					}
				}
			}

			// 飞机
			if (card_type == GameConstants_DTZ.CT_PLANE || card_type == GameConstants_DTZ.CT_PLANE_LOST) {
				auto_bom = true;
				card_count = 0;
				for (int i = CardDataResult.cbThreeCount - 1; i > 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3]) > GetCardLogicValue(cbOutCardData[0])) {
						int count = 1;
						for (int j = i - 1; j >= count - 1; j--) {
							if (GetCardLogicValue(CardDataResult.cbThreeCardData[i * 3])
									+ count == GetCardLogicValue(CardDataResult.cbThreeCardData[j * 3])) {
								count++;
								if (count == out_card_count / 5) {
									for (int x = i; x >= j; x--) {
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3];
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 1];
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 2];
									}

									for (int x = CardDataResult.cbSignedCount - 1; x >= 0; x--) {
										card_data[card_count++] = CardDataResult.cbSignedCardData[x];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
									for (int x = CardDataResult.cbDoubleCount - 1; x >= 0; x--) {
										card_data[card_count++] = CardDataResult.cbDoubleCardData[x * 2];
										if (card_count == out_card_count) {
											return card_count;
										}
										card_data[card_count++] = CardDataResult.cbDoubleCardData[x * 2 + 1];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
									for (int x = CardDataResult.cbThreeCount - 1; x >= 0; x--) {
										if (x >= i && x <= j) {
											continue;
										}
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3];
										if (card_count == out_card_count) {
											return card_count;
										}
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 1];
										if (card_count == out_card_count) {
											return card_count;
										}
										card_data[card_count++] = CardDataResult.cbThreeCardData[x * 3 + 2];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
								}
							}
						}
					}
				}
			}

			if (auto_bom) {
				card_count = 0;
				for (int i = CardDataResult.cbFourCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
					card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
					return card_count;
				}
				for (int i = CardDataResult.cbFiveCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 1];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 2];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 3];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 4];
					return card_count;
				}
				for (int i = CardDataResult.cbSixCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 1];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 2];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 3];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 4];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 5];
					return card_count;
				}
				for (int i = CardDataResult.cbSevenCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 1];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 2];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 3];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 4];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 5];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 6];
					return card_count;
				}
				for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
					return card_count;
				}

				// 筒子炸
				for (int i = 0; i < card_index.card_index_color.length; i++) {
					if (card_index.card_index_color[i] < 3) {
						continue;
					}
					for (int j = 0; j < card_index.card_index_color[i]; j++) {
						card_data[card_count++] = GetCardLoigcValueColorData(i);
					}
					return card_count;
				}
			}

			card_count = 0;
			if (card_type == GameConstants_DTZ.CT_BOMB_CARD_4) {
				for (int i = CardDataResult.cbFourCount - 1; i >= 0; i = i - 1) {
					if (GetCardLogicValue(CardDataResult.cbFourCardData[i * 4]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
						card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
						return card_count;
					}
				}
				for (int i = CardDataResult.cbFiveCount - 1; i >= 0; i = i - 1) {
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 1];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 2];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 3];
					card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 4];
					return card_count;
				}
				for (int i = CardDataResult.cbSixCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 1];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 2];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 3];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 4];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 5];
					return card_count;
				}
				for (int i = CardDataResult.cbSevenCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 1];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 2];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 3];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 4];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 5];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 6];
					return card_count;
				}
				for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
					return card_count;
				}

				// 筒子炸
				for (int i = 0; i < card_index.card_index_color.length; i++) {
					if (card_index.card_index_color[i] < 3) {
						continue;
					}
					for (int j = 0; j < card_index.card_index_color[i]; j++) {
						card_data[card_count++] = GetCardLoigcValueColorData(i);
					}
					return card_count;
				}
			}

			if (card_type == GameConstants_DTZ.CT_BOMB_CARD_5) {
				for (int i = CardDataResult.cbFiveCount - 1; i >= 0; i = i - 1) {
					if (GetCardLogicValue(CardDataResult.cbFiveCardData[i * 5]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5];
						card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 1];
						card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 2];
						card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 3];
						card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 4];
						return card_count;
					}
				}
				for (int i = CardDataResult.cbSixCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 1];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 2];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 3];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 4];
					card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 5];
					return card_count;
				}
				for (int i = CardDataResult.cbSevenCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 1];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 2];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 3];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 4];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 5];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 6];
					return card_count;
				}
				for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
					return card_count;
				}
				// 筒子炸
				for (int i = 0; i < card_index.card_index_color.length; i++) {
					if (card_index.card_index_color[i] < 3) {
						continue;
					}
					for (int j = 0; j < card_index.card_index_color[i]; j++) {
						card_data[card_count++] = GetCardLoigcValueColorData(i);
					}
					return card_count;
				}
			}

			if (card_type == GameConstants_DTZ.CT_BOMB_CARD_6) {
				for (int i = CardDataResult.cbSixCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbSixCardData[i * 6]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbSixCardData[i * 6];
						card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 1];
						card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 2];
						card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 3];
						card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 4];
						card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 5];
					}
					return card_count;
				}
				for (int i = CardDataResult.cbSevenCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 1];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 2];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 3];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 4];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 5];
					card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 6];
					return card_count;
				}
				for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
					return card_count;
				}
				// 筒子炸
				for (int i = 0; i < card_index.card_index_color.length; i++) {
					if (card_index.card_index_color[i] < 3) {
						continue;
					}
					for (int j = 0; j < card_index.card_index_color[i]; j++) {
						card_data[card_count++] = GetCardLoigcValueColorData(i);
					}
					return card_count;
				}
			}

			if (card_type == GameConstants_DTZ.CT_BOMB_CARD_7) {
				for (int i = CardDataResult.cbSevenCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbSevenCardData[i * 7]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7];
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 1];
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 2];
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 3];
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 4];
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 5];
						card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 6];
					}
					return card_count;
				}
				for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
					card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
					return card_count;
				}
				// 筒子炸
				for (int i = 0; i < card_index.card_index_color.length; i++) {
					if (card_index.card_index_color[i] < 3) {
						continue;
					}
					for (int j = 0; j < card_index.card_index_color[i]; j++) {
						card_data[card_count++] = GetCardLoigcValueColorData(i);
					}
					return card_count;
				}
			}

			if (card_type == GameConstants_DTZ.CT_BOMB_CARD_8) {
				for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
					if (GetCardLogicValue(CardDataResult.cbEightCardData[i * 8]) > GetCardLogicValue(cbOutCardData[0])) {
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
						card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
					}
					return card_count;
				}
				// 筒子炸
				for (int i = 0; i < card_index.card_index_color.length; i++) {
					if (card_index.card_index_color[i] < 3) {
						continue;
					}
					for (int j = 0; j < card_index.card_index_color[i]; j++) {
						card_data[card_count++] = GetCardLoigcValueColorData(i);
					}
					return card_count;
				}
			}

			if (card_type == GameConstants_DTZ.CT_BOMB_CARD_TONG) {

				for (int i = 0; i < GameConstants_DTZ.MAX_INDEX - 2; i++) {
					// 计算下是否有筒子炸
					for (int j = 0; j < 4; j++) {
						if (card_index.card_index_color[(i + j * 13) % 52] < 3) {
							continue;
						}
						if (i > (GetCardLogicValue(cbOutCardData[0] - 3))
								|| (i == (GetCardLogicValue(cbOutCardData[0] - 3)) && j > get_card_color(cbOutCardData[0]))) {
							for (int x = 0; x < card_index.card_index_color[(i + j * 13) % 52]; x++) {
								card_data[card_count++] = card_index.card_data_color[(i + j * 13) % 52][x];
							}
							return card_count;
						}
					}

					// 地炸
					if (card_index.card_index_color[i] >= 2 && card_index.card_index_color[i + 1 * 13] >= 2
							&& card_index.card_index_color[i + 2 * 13] >= 2 && card_index.card_index_color[i + 3 * 13] >= 2) {
						for (int j = 0; j < 4; j++) {
							card_data[card_count++] = card_index.card_data_color[(i + j * 13) % 52][0];
							card_data[card_count++] = card_index.card_data_color[(i + j * 13) % 52][1];
						}
						return card_count;
					}
				}
			}
		} else {
			// 三张
			if (CardDataResult.cbThreeCount > 0) {
				for (int i = CardDataResult.cbThreeCount - 1; i >= 0; i--) {
					int card_1_color = get_card_color(CardDataResult.cbThreeCardData[i * 3]);
					int card_2_color = get_card_color(CardDataResult.cbThreeCardData[i * 3 + 1]);
					int card_3_color = get_card_color(CardDataResult.cbThreeCardData[i * 3 + 2]);
					if (card_1_color == card_2_color && card_2_color == card_3_color) {
						continue;
					}
					card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3];
					card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 1];
					card_data[card_count++] = CardDataResult.cbThreeCardData[i * 3 + 2];

					for (int j = CardDataResult.cbSignedCount - 1; j >= 0; j--) {
						card_data[card_count++] = CardDataResult.cbSignedCardData[j];
						if (card_count == 5) {
							return card_count;
						}
					}
					for (int j = CardDataResult.cbDoubleCount - 1; j >= 0; j--) {
						card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2];
						if (card_count == 5) {
							return card_count;
						}
						card_data[card_count++] = CardDataResult.cbDoubleCardData[j * 2 + 1];
						if (card_count == 5) {
							return card_count;
						}
					}
					for (int j = CardDataResult.cbThreeCount - 1; j >= 0; j--) {
						if (i == j) {
							continue;
						}
						card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3];
						if (card_count == 5) {
							return card_count;
						}
						card_data[card_count++] = CardDataResult.cbThreeCardData[j * 3 + 1];
						if (card_count == 5) {
							return card_count;
						}
					}
					return card_count;
				}
			}
			// 连对
			for (int i = cbHandCardCount - 1; i >= 0; i--) {
				if (GetCardLogicValue(cbHandCardData[i]) == 15) {
					continue;
				}
				if (i >= 4 - 1 && GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(cbHandCardData[i - 1])) {
					int count = 1;
					int index = i - 2;
					for (; index > 0; index--) {
						if (GetCardLogicValue(cbHandCardData[index]) == 15) {
							continue;
						}
						if (GetCardLogicValue(cbHandCardData[i]) + count == GetCardLogicValue(cbHandCardData[index])
								&& GetCardLogicValue(cbHandCardData[index]) == GetCardLogicValue(cbHandCardData[index - 1])) {
							count++;
							index--;
						}
					}
					if (count >= 2) {
						card_data[card_count++] = cbHandCardData[i];
						card_data[card_count++] = cbHandCardData[i - 1];
						for (int j = i; j >= index; j--) {
							if (GetCardLogicValue(cbHandCardData[j - 2]) - 1 == GetCardLogicValue(cbHandCardData[j])) {
								card_data[card_count++] = cbHandCardData[j - 2];
								card_data[card_count++] = cbHandCardData[j - 3];
								count--;
								j--;
								if (count == 1) {
									return card_count;
								}
							}
						}

					}
					i--;
				}
			}
			// 对子
			for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
				if (this.GetCardLogicValue(CardDataResult.cbDoubleCardData[i * 2]) < 11) {
					card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
					card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
					return card_count;
				}
			}
			// 单张
			for (int i = 0; i < CardDataResult.cbSignedCount; i++) {
				card_data[card_count++] = cbHandCardData[0];
				return card_count;
			}
			// 对子
			for (int i = CardDataResult.cbDoubleCount - 1; i >= 0; i = i - 1) {
				card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2];
				card_data[card_count++] = CardDataResult.cbDoubleCardData[i * 2 + 1];
				return card_count;
			}

			for (int i = CardDataResult.cbFourCount - 1; i >= 0; i = i - 1) {
				card_data[card_count++] = CardDataResult.cbFourCardData[i * 4];
				card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 1];
				card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 2];
				card_data[card_count++] = CardDataResult.cbFourCardData[i * 4 + 3];
				return card_count;
			}
			for (int i = CardDataResult.cbFiveCount - 1; i >= 0; i = i - 1) {
				card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5];
				card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 1];
				card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 2];
				card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 3];
				card_data[card_count++] = CardDataResult.cbFiveCardData[i * 5 + 4];
				return card_count;
			}
			for (int i = CardDataResult.cbSixCount - 1; i >= 0; i--) {
				card_data[card_count++] = CardDataResult.cbSixCardData[i * 6];
				card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 1];
				card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 2];
				card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 3];
				card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 4];
				card_data[card_count++] = CardDataResult.cbSixCardData[i * 6 + 5];
				return card_count;
			}
			for (int i = CardDataResult.cbSevenCount - 1; i >= 0; i--) {
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7];
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 1];
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 2];
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 3];
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 4];
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 5];
				card_data[card_count++] = CardDataResult.cbSevenCardData[i * 7 + 6];
				return card_count;
			}
			for (int i = CardDataResult.cbEightCount - 1; i >= 0; i--) {
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 1];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 2];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 3];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 4];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 5];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 6];
				card_data[card_count++] = CardDataResult.cbEightCardData[i * 8 + 7];
				return card_count;
			}

			// 筒子炸
			for (int i = 0; i < card_index.card_index_color.length; i++) {
				if (card_index.card_index_color[i] < 3) {
					continue;
				}
				for (int j = 0; j < card_index.card_index_color[i]; j++) {
					card_data[card_count++] = GetCardLoigcValueColorData(i);
				}
				return card_count;
			}
		}
		return card_count;
	}

	public int getPineCount(int[] _turn_out_card_data, int _turn_out_card_count) {

		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(_turn_out_card_data, _turn_out_card_count, card_index);
		// 飞机
		for (int i = GameConstants_DTZ.MAX_INDEX - 5; i >= 0; i--) {
			if (card_index.card_index[i] >= 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] >= 3) {
						if ((i - j + 1) * 5 == _turn_out_card_count) {
							return i - j + 1;
						}
						if (j == 0) {
							if ((i - j + 1) * 5 > _turn_out_card_count && i - j > 0) {
								return i - j + 1;
							} else {
								return 0;
							}
						}
					} else {
						if ((i - j) * 3 == _turn_out_card_count && i - j > 1) {
							return i - j;
						} else if ((i - j) * 5 > _turn_out_card_count && i - j > 1) {
							return i - j;
						} else {
							i = j;
							break;
						}
					}
				}
			}
		}

		return 0;
	}

	public boolean attain_magic_plane_count(int[] _turn_out_card_data, int _turn_out_card_count, int magic_count) {
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(_turn_out_card_data, _turn_out_card_count, card_index);
		// 飞机
		for (int i = GameConstants_DTZ.MAX_INDEX - 5; i >= 0; i--) {
			if (card_index.card_index[i] >= 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] >= 3) {
						if ((i - j + 1) * 5 == _turn_out_card_count) {
							if (i - j + 1 == magic_count) {
								return true;
							}
						}
						if (j == 0) {
							if ((i - j + 1) * 5 > _turn_out_card_count && i - j > 0) {
								if (i - j + 1 == magic_count) {
									return true;
								}
							} else {
								return false;
							}
						}
					} else {
						if ((i - j) * 3 == _turn_out_card_count && i - j > 1) {
							if (i - j == magic_count) {
								return true;
							}
						} else if ((i - j) * 5 > _turn_out_card_count && i - j > 1) {
							if (i - j == magic_count) {
								return true;
							}
						} else {
							i = j;
							break;
						}
					}
				}
			}
		}
		return false;
	}

	public void sort_card_data_list_plane(int card_date[], int card_count) {
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(card_date, card_count, card_index);
		int max_index = -1;
		int min_index = -1;
		int plane_num = getPineCount(card_date, card_count);
		for (int i = GameConstants_DTZ.MAX_INDEX - 4; i >= 0; i--) {
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

		int[] cards = new int[card_count];
		int c_count = 0;
		for (int i = min_index; i <= max_index; i++) {
			int n = 3;
			for (int c = 0; c < card_count; c++) {
				if (GetCardLogicValue(card_date[c]) - 3 != i) {
					continue;
				}

				cards[c_count++] = card_date[c];
				card_date[c] = 0;

				if (--n == 0) {
					break;
				}
			}
		}
		for (int c = 0; c < card_count; c++) {
			if (card_date[c] != 0) {
				cards[c_count++] = card_date[c];
			}
		}

		for (int c = 0; c < c_count; c++) {
			card_date[c] = cards[c];
		}

	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		this.sort_card_date_list(card_date, card_count);
		tagAnalyseResult Result = new tagAnalyseResult();
		AnalysebCardData(card_date, card_count, Result);

		int index = 0;
		if (type == GameConstants_DTZ.CT_SINGLE) {
			for (int i = 0; i < Result.cbSignedCount; i++) {
				card_date[index++] = Result.cbSignedCardData[i];
			}
		} else if (type == GameConstants_DTZ.CT_DOUBLE || type == GameConstants_DTZ.CT_DOUBLE_LINE) {
			for (int i = 0; i < Result.cbDoubleCount; i++) {
				for (int j = 0; j < 2; j++) {
					card_date[index++] = Result.cbDoubleCardData[i * 2 + j];
				}
			}
		} else if (type == GameConstants_DTZ.CT_PLANE || type == GameConstants_DTZ.CT_PLANE_LOST) {
			sort_card_data_list_plane(card_date, card_count);
		} else if (type == GameConstants_DTZ.CT_THREE || type == GameConstants_DTZ.CT_THREE_TAKE_ONE || type == GameConstants_DTZ.CT_THREE_TAKE_TWO) {
			// 连牌判断
			int value_add = 0;
			int CardData = Result.cbThreeCardData[0];
			int cbFirstLogicValue = GetCardLogicValue(CardData);
			int nLink_Three_Count = 0;
			int threeindex = 0;
			for (int i = 0; i < Result.cbThreeCount; i++) {
				if (nLink_Three_Count * 5 >= card_count) {
					break;
				}
				int CardDatatemp = Result.cbThreeCardData[i * 3];
				if (cbFirstLogicValue != (GetCardLogicValue(CardDatatemp) + value_add)) {
					if (nLink_Three_Count * 5 == card_count) {

						break;
					}
					cbFirstLogicValue = GetCardLogicValue(Result.cbThreeCardData[nLink_Three_Count * 3]);
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
			for (int i = 0; i < Result.cbFourCount; i++) {
				for (int j = 0; j < 4; j++) {
					card_date[index++] = Result.cbFourCardData[i * 4 + j];
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
		} else if (type >= GameConstants_DTZ.CT_BOMB_CARD_4) {
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
			for (int i = 0; i < Result.cbThreeCount; i++) {
				for (int j = 0; j < 3; j++) {
					card_date[index++] = Result.cbThreeCardData[i * 3 + j];
				}
			}
		}

		return;
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

	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 获取类型
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		// 都是炸弹
		if (cbFirstType > GameConstants_DTZ.CT_PLANE_LOST && cbNextType > GameConstants_DTZ.CT_PLANE_LOST) {
			if (cbFirstType < cbNextType) {
				return true;
			}
			if (cbFirstType == cbNextType) {
				// 获取数值
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
				int cbNextColorValue = get_card_color(cbNextCard[0]);
				int cbFirstColorValue = get_card_color(cbFirstCard[0]);

				if (cbFirstType == GameConstants_DTZ.CT_BOMB_CARD_TONG) {
					if (cbNextLogicValue > cbFirstLogicValue) {
						return true;
					} else if (cbNextLogicValue == cbFirstLogicValue) {
						return cbNextColorValue > cbFirstColorValue;
					} else if (cbNextLogicValue < cbFirstLogicValue) {
						return false;
					}
				} else {
					return cbNextLogicValue > cbFirstLogicValue;
				}
			}
			if (cbFirstType > cbNextType) {
				return false;
			}
		}
		// 炸弹压普通牌
		if (cbFirstType <= GameConstants_DTZ.CT_PLANE_LOST && cbNextType > GameConstants_DTZ.CT_PLANE_LOST) {
			return true;
		}

		// 普通牌怎么能压炸弹，错了
		if (cbNextType <= GameConstants_DTZ.CT_PLANE_LOST && cbFirstType > GameConstants_DTZ.CT_PLANE_LOST) {
			return false;
		}

		// ------------------------------不是炸弹同级比较-------------------------
		if (cbFirstType <= GameConstants_DTZ.CT_PLANE_LOST && cbNextType <= GameConstants_DTZ.CT_PLANE_LOST && cbFirstType != cbNextType) {
			// 三张
			if (cbFirstType <= GameConstants_DTZ.CT_THREE_TAKE_TWO && cbFirstType >= GameConstants_DTZ.CT_THREE
					&& cbNextType <= GameConstants_DTZ.CT_THREE_TAKE_TWO && cbNextType >= GameConstants_DTZ.CT_THREE) {
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);
				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			}
			// 三张
			if (cbFirstType <= GameConstants_DTZ.CT_PLANE_LOST && cbFirstType >= GameConstants_DTZ.CT_PLANE
					&& cbNextType <= GameConstants_DTZ.CT_PLANE_LOST && cbNextType >= GameConstants_DTZ.CT_PLANE) {
				int count = getPineCount(cbFirstCard, cbFirstCount);
				if (count > getPineCount(cbNextCard, cbNextCount)) {
					return false;
				}
				if (cbNextCount - (count * 5) > 0) {
					return false;
				}
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[3 * getPineCount(cbNextCard, cbNextCount) - 1]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[3 * count - 1]);
				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			}
			return false;
		} else {
			switch (cbFirstType) {
			case GameConstants_DTZ.CT_SINGLE:
			case GameConstants_DTZ.CT_DOUBLE:
			case GameConstants_DTZ.CT_DOUBLE_LINE: {
				// 获取数值
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[0]);

				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			}
			case GameConstants_DTZ.CT_PLANE:
			case GameConstants_DTZ.CT_PLANE_LOST: {
				int count = getPineCount(cbFirstCard, cbFirstCount);
				if (!attain_magic_plane_count(cbNextCard, cbNextCount, count)) {
					return false;
				}
				if (cbNextCount - (count * 5) > 0) {
					return false;
				}
				// 获取数值
				int cbNextLogicValue = GetCardLogicValue(cbNextCard[3 * getPineCount(cbNextCard, cbNextCount) - 1]);
				int cbFirstLogicValue = GetCardLogicValue(cbFirstCard[3 * count - 1]);

				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			}
			case GameConstants_DTZ.CT_THREE:
			case GameConstants_DTZ.CT_THREE_TAKE_ONE:
			case GameConstants_DTZ.CT_THREE_TAKE_TWO: {
				// 分析扑克
				tagAnalyseResult NextResult = new tagAnalyseResult();
				tagAnalyseResult FirstResult = new tagAnalyseResult();
				AnalysebCardData(cbNextCard, cbNextCount, NextResult);
				AnalysebCardData(cbFirstCard, cbFirstCount, FirstResult);

				// 获取数值
				int cbNextLogicValue = 0;
				if (NextResult.cbThreeCardData[0] == 0) {
					cbNextLogicValue = GetCardLogicValue(NextResult.cbFourCardData[0]);
				} else {
					cbNextLogicValue = GetCardLogicValue(NextResult.cbThreeCardData[0]);
				}
				int cbFirstLogicValue = 0;
				if (FirstResult.cbThreeCardData[0] == 0) {
					cbFirstLogicValue = GetCardLogicValue(FirstResult.cbFourCardData[0]);
				} else {
					cbFirstLogicValue = GetCardLogicValue(FirstResult.cbThreeCardData[0]);
				}

				// 对比扑克
				return cbNextLogicValue > cbFirstLogicValue;
			}
			}
		}
		// ------------------------------不是炸弹同级比较-------------------------

		return false;
	}

	// 获取能出牌数据
	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count, int card_data[]) {
		int card_count = 0;
		int out_card_type = this.GetCardType(cbOutCardData, out_card_count);

		switch (out_card_type) {
		case GameConstants_DTZ.CT_SINGLE: {
			card_count = single_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_DOUBLE: {
			card_count = double_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_THREE:
		case GameConstants_DTZ.CT_THREE_TAKE_ONE:
		case GameConstants_DTZ.CT_THREE_TAKE_TWO: {
			card_count = three_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_DOUBLE_LINE: {
			card_count = double_link_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_PLANE:
		case GameConstants_DTZ.CT_PLANE_LOST: {
			card_count = plane_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_BOMB_CARD_4:
		case GameConstants_DTZ.CT_BOMB_CARD_5:
		case GameConstants_DTZ.CT_BOMB_CARD_6:
		case GameConstants_DTZ.CT_BOMB_CARD_7:
		case GameConstants_DTZ.CT_BOMB_CARD_8: {
			card_count = boom_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_BOMB_CARD_TONG: {
			card_count = tube_card_can_out(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		case GameConstants_DTZ.CT_BOMB_CARD_DI: {
			card_count = boom_card_can_out_di(cbOutCardData, out_card_count, card_data, hand_card_data, cbHandCardCount);
			return card_count;
		}
		}
		return card_count;
	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		if (cbCardCount == 1) {
			return GameConstants_DTZ.CT_SINGLE;
		} else if (cbCardCount == 2) {
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 2) {
					return GameConstants_DTZ.CT_DOUBLE;
				}
			}
			return GameConstants_DTZ.CT_ERROR;
		} else if (cbCardCount == 3) {
			for (int i = GameConstants_DTZ.MAX_INDEX_COLOR - 1; i >= 0; i--) {
				if (card_index.card_index_color[i] == 3) {
					return GameConstants_DTZ.CT_BOMB_CARD_TONG;
				}
			}
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					return GameConstants_DTZ.CT_THREE;
				}
			}
			return GameConstants_DTZ.CT_ERROR;
		} else if (cbCardCount == 4) {
			// 三带一
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					return GameConstants_DTZ.CT_THREE_TAKE_ONE;
				}
			}

			// 炸弹
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 4) {
					return GameConstants_DTZ.CT_BOMB_CARD_4;
				}
			}
		} else if (cbCardCount == 5) {
			// 炸弹
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 5) {
					return GameConstants_DTZ.CT_BOMB_CARD_5;
				}
			}
			// 炸弹
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 4) {
					return GameConstants_DTZ.CT_THREE_TAKE_TWO;
				}
			}
			// 三带二
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					return GameConstants_DTZ.CT_THREE_TAKE_TWO;
				}
			}

		} else if (cbCardCount == 6) {
			// 炸弹
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 6) {
					return GameConstants_DTZ.CT_BOMB_CARD_6;
				}
			}
		} else if (cbCardCount == 7) {
			// 炸弹
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 7) {
					return GameConstants_DTZ.CT_BOMB_CARD_7;
				}
			}
		} else if (cbCardCount == 8) {
			// 地炸
			for (int i = GameConstants_DTZ.MAX_INDEX - 4; i >= 0; i--) {
				if (card_index.card_index_color[i] >= 2 && card_index.card_index_color[i + 1 * 13] >= 2
						&& card_index.card_index_color[i + 2 * 13] >= 2 && card_index.card_index_color[i + 3 * 13] >= 2) {
					return GameConstants_DTZ.CT_BOMB_CARD_DI;
				}
			}
			// 炸弹
			for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 8) {
					return GameConstants_DTZ.CT_BOMB_CARD_8;
				}
			}
		}

		// 飞机
		for (int i = GameConstants_DTZ.MAX_INDEX - 5; i >= 0; i--) {
			if (card_index.card_index[i] >= 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] >= 3) {
						if ((i - j + 1) * 5 == cbCardCount) {
							return GameConstants_DTZ.CT_PLANE;
						}
						if (j == 0) {
							if ((i - j + 1) * 5 > cbCardCount && i - j > 0) {
								return GameConstants_DTZ.CT_PLANE_LOST;
							} else {
								return GameConstants_DTZ.CT_ERROR;
							}
						}
					} else {
						if ((i - j) * 3 == cbCardCount && i - j > 1) {
							return GameConstants_DTZ.CT_PLANE_LOST;
						} else if ((i - j) * 5 > cbCardCount && i - j > 1) {
							return GameConstants_DTZ.CT_PLANE;
						} else {
							i = j;
							break;
						}
					}
				}
			}
		}

		// 连队
		for (int i = GameConstants_DTZ.MAX_INDEX - 5; i >= 0 && cbCardCount % 2 == 0; i--) {
			if (card_index.card_index[i] != 2 && card_index.card_index[i] != 0) {
				break;
			}
			int link_number = cbCardCount / 2;
			if (card_index.card_index[i] == 2 && i >= link_number - 1 && link_number >= 2) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] != 2 && j > i - link_number) {
						return GameConstants_DTZ.CT_ERROR;
					} else if (card_index.card_index[j] != 2 && card_index.card_index[j] != 0) {
						return GameConstants_DTZ.CT_ERROR;
					}
				}
				return GameConstants_DTZ.CT_DOUBLE_LINE;
			}
		}

		return GameConstants_DTZ.CT_ERROR;
	}

	// 筒子炸
	public int tube_card_can_out(int[] cbOutCardData, int out_card_count, int[] card_data, int[] hand_card_data, int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);
		int max_index = switch_card_to_index(cbOutCardData[0]);
		int max_color = get_card_color(cbOutCardData[0]);

		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX - 2; i++) {
			boolean flag_continue = false;
			// 计算下是否有筒子炸
			for (int j = 0; j < 4; j++) {
				if (analyseIndex.card_index_color[(i + j * 13) % 52] < 3) {
					continue;
				}
				if (i > max_index || (i == max_index && j > max_color)) {
					for (int x = 0; x < analyseIndex.card_index_color[(i + j * 13) % 52]; x++) {
						card_data[card_count++] = analyseIndex.card_data_color[(i + j * 13) % 52][x];
					}
					flag_continue = true;
				}
			}
			if (flag_continue) {
				continue;
			}

			// 地炸
			if (analyseIndex.card_index_color[i] >= 2 && analyseIndex.card_index_color[i + 1 * 13] >= 2
					&& analyseIndex.card_index_color[i + 2 * 13] >= 2 && analyseIndex.card_index_color[i + 3 * 13] >= 2) {
				for (int j = 0; j < 4; j++) {
					card_data[card_count++] = analyseIndex.card_data_color[(i + j * 13) % 52][0];
					card_data[card_count++] = analyseIndex.card_data_color[(i + j * 13) % 52][1];
				}
			}
		}
		return card_count;
	}

	public int have_tube_card(int[] hand_card_data, int cbHandCardCount, int card) {
		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);

		int card_index = GetCardLogicValueColorIndex(card);
		int count = 0;
		// 计算下是否有筒子炸
		for (int j = 0; j < 4; j++) {
			if (analyseIndex.card_index_color[(card_index + j * 13) % 52] >= 3) {
				count++;
			}
		}
		return count;
	}

	public int get_bom_di(int[] hand_card_data, int cbHandCardCount) {
		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);

		int count = 0;
		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX - 2; i++) {
			// 地炸
			if (analyseIndex.card_index_color[i] >= 2 && analyseIndex.card_index_color[i + 1 * 13] >= 2
					&& analyseIndex.card_index_color[i + 2 * 13] >= 2 && analyseIndex.card_index_color[i + 3 * 13] >= 2) {
				count++;
			}
		}
		return count;
	}

	// 炸弹
	public int boom_card_can_out_di(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);
		int max_index = GetCardLogicValue(cbOutCardData[0]) - 3;

		for (int i = 0; i < GameConstants_DTZ.MAX_INDEX - 2; i++) {
			// 地炸
			if (max_index < i && analyseIndex.card_index_color[i] >= 2 && analyseIndex.card_index_color[i + 1 * 13] >= 2
					&& analyseIndex.card_index_color[i + 2 * 13] >= 2 && analyseIndex.card_index_color[i + 3 * 13] >= 2) {
				for (int j = 0; j < 4; j++) {
					card_data[card_count++] = analyseIndex.card_data_color[(i + j * 13) % 52][0];
					card_data[card_count++] = analyseIndex.card_data_color[(i + j * 13) % 52][1];
				}
			}
		}
		return card_count;
	}

	// 炸弹
	public int boom_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;

		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);
		int max_index = this.switch_card_to_index(cbOutCardData[0]);
		out: for (int i = 0; i < GameConstants_DTZ.MAX_INDEX - 2; i++) {

			// 计算下是否有筒子炸
			for (int j = 0; j < 4; j++) {
				if (analyseIndex.card_index_color[(i + j * 13) % 52] >= 3) {
					for (int x = 0; x < analyseIndex.card_index[i]; x++) {
						card_data[card_count++] = analyseIndex.card_data[i][x];
					}
					break out;
				}
			}

			// 普通炸
			int need_card_num_min = out_card_count;
			if (i <= max_index) {
				need_card_num_min = out_card_count + 1;
			}
			if (analyseIndex.card_index[i] >= need_card_num_min) {
				for (int x = 0; x < analyseIndex.card_index[i]; x++) {
					card_data[card_count++] = analyseIndex.card_data[i][x];
				}
			}
		}
		return card_count;
	}

	// 对子
	public int double_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;

		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);
		int max_index = this.switch_card_to_index(cbOutCardData[0]);
		out: for (int i = 0; i < GameConstants_DTZ.MAX_INDEX - 2; i++) {

			// 计算下是否有筒子炸
			for (int j = 0; j < 4; j++) {
				if (analyseIndex.card_index_color[(i + j * 13) % 52] >= 3) {
					for (int x = 0; x < analyseIndex.card_index[i]; x++) {
						card_data[card_count++] = analyseIndex.card_data[i][x];
					}
					break out;
				}
			}

			int need_card_num_min = 2;
			if (i <= max_index) {
				need_card_num_min = 4;
			}
			if (analyseIndex.card_index[i] >= need_card_num_min) {
				for (int x = 0; x < analyseIndex.card_index[i]; x++) {
					card_data[card_count++] = analyseIndex.card_data[i][x];
				}
			}
		}
		return card_count;
	}

	// 对子顺子
	public int double_link_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);
		int max_index = this.switch_card_to_index(cbOutCardData[0]);
		int min_index = this.switch_card_to_index(cbOutCardData[out_card_count - 1]);

		for (int i = GameConstants_DTZ.MAX_INDEX - 4; i >= 0; i--) {
			if (i != 12 && analyseIndex.card_index[i] >= 2 && i > max_index) {
				for (int j = i - 1; j > min_index; j--) {
					if (analyseIndex.card_index[j] < 2) {
						if (i - j >= out_card_count / 2) {
							for (int x = i; x > j; x--) {
								for (int y = 0; y < analyseIndex.card_index[x]; y++) {
									card_data[card_count++] = analyseIndex.card_data[x][y];
								}
							}
						} else {
							for (int x = i; x > j; x--) {
								boolean is_out_break = false;
								// 计算下是否有筒子炸
								for (int n = 0; n < 4; n++) {
									if (analyseIndex.card_index_color[(x + n * 13) % 52] >= 3) {
										for (int an = 0; an < analyseIndex.card_index[x]; an++) {
											card_data[card_count++] = analyseIndex.card_data[x][an];
										}
										is_out_break = true;
									}
								}
								if (is_out_break) {
									continue;
								}

								if (analyseIndex.card_index[x] >= 4) {
									for (int an = 0; an < analyseIndex.card_index[x]; an++) {
										card_data[card_count++] = analyseIndex.card_data[x][an];
									}
								}
							}
						}

						i = j;
						break;
					} else if (j == min_index + 1) {
						if (i - j >= out_card_count / 2 - 1) {
							for (int x = i; x >= j; x--) {
								for (int y = 0; y < analyseIndex.card_index[x]; y++) {
									card_data[card_count++] = analyseIndex.card_data[x][y];
								}
							}
						}
						i = j;
					}
				}
			} else {
				boolean is_out_break = false;
				// 计算下是否有筒子炸
				for (int j = 0; j < 4; j++) {
					if (analyseIndex.card_index_color[(i + j * 13) % 52] >= 3) {
						for (int x = 0; x < analyseIndex.card_index[i]; x++) {
							card_data[card_count++] = analyseIndex.card_data[i][x];
						}
						is_out_break = true;
					}
				}
				if (is_out_break) {
					continue;
				}

				if (analyseIndex.card_index[i] >= 4) {
					for (int x = 0; x < analyseIndex.card_index[i]; x++) {
						card_data[card_count++] = analyseIndex.card_data[i][x];
					}
				}
			}
		}
		return card_count;
	}

	// 单张
	public int single_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult analyseIndex = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, analyseIndex);
		int max_index = this.switch_card_to_index(cbOutCardData[0]);

		for (int i = GameConstants_DTZ.MAX_INDEX - 1; i >= 0; i--) {
			if (analyseIndex.card_index[i] >= 1 && i > max_index) {
				for (int x = 0; x < analyseIndex.card_index[i]; x++) {
					card_data[card_count++] = analyseIndex.card_data[i][x];
				}
			} else {
				boolean is_out_break = false;
				// 计算下是否有筒子炸
				for (int j = 0; j < 4; j++) {
					if (analyseIndex.card_index_color[(i + j * 13) % 52] >= 3) {
						for (int x = 0; x < analyseIndex.card_index[i]; x++) {
							card_data[card_count++] = analyseIndex.card_data[i][x];
						}
						is_out_break = true;
					}
				}
				if (is_out_break) {
					continue;
				}

				if (analyseIndex.card_index[i] >= 4) {
					for (int x = 0; x < analyseIndex.card_index[i]; x++) {
						card_data[card_count++] = analyseIndex.card_data[i][x];
					}
				}
			}
		}
		return card_count;
	}

	public int three_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {

		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult out_card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, out_card_index);
		int max_index = -1;
		for (int i = GameConstants_DTZ.MAX_INDEX - 3; i >= 0; i--) {
			if (out_card_index.card_index[i] >= 3 && max_index == -1) {
				max_index = i;
				break;
			}
		}

		// 三条
		for (int i = GameConstants_DTZ.MAX_INDEX - 3; i >= 0; i--) {
			if (hand_index.card_index[i] >= 3 && i > max_index) {
				for (int x = 0; x < cbHandCardCount; x++) {
					card_data[card_count++] = hand_card_data[x];
				}
				return card_count;
			} else if (i <= max_index) {
				break;
			}
		}

		// 炸弹
		out: for (int i = GameConstants_DTZ.MAX_INDEX - 3; i >= 0; i--) {
			// 计算下是否有筒子炸
			for (int j = 0; j < 4; j++) {
				if (hand_index.card_index_color[(i + j * 13) % 52] >= 3) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
					break out;
				}
			}

			// 普通炸
			if (hand_index.card_index[i] >= 4) {
				for (int x = 0; x < hand_index.card_index[i]; x++) {
					card_data[card_count++] = hand_index.card_data[i][x];
				}
			}
		}
		return card_count;
	}

	// 飞机
	public int plane_card_can_out(int cbOutCardData[], int out_card_count, int card_data[], int hand_card_data[], int cbHandCardCount) {
		int card_count = 0;
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();

		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, hand_index);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, card_index);
		int max_index = -1;
		int min_index = -1;
		int plane_num = getPineCount(cbOutCardData, out_card_count);
		for (int i = GameConstants_DTZ.MAX_INDEX - 5; i >= 0; i--) {
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
		for (int i = GameConstants_DTZ.MAX_INDEX - 5; i >= 0; i--) {
			if (hand_index.card_index[i] >= 3 && i > max_index) {
				for (int j = i; j > min_index; j--) {
					if (hand_index.card_index[j] >= 3) {
						if (i - j == plane_num - 1) {
							for (int x = 0; x < cbHandCardCount; x++) {
								card_data[card_count++] = hand_card_data[x];
							}
							return card_count;
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
		out: for (int i = GameConstants_DTZ.MAX_INDEX - 3; i >= 0; i--) {
			// 计算下是否有筒子炸
			for (int j = 0; j < 4; j++) {
				if (hand_index.card_index_color[(i + j * 13) % 52] >= 3) {
					for (int x = 0; x < hand_index.card_index[i]; x++) {
						card_data[card_count++] = hand_index.card_data[i][x];
					}
					break out;
				}
			}

			// 普通炸
			if (hand_index.card_index[i] >= 4) {
				for (int x = 0; x < hand_index.card_index[i]; x++) {
					card_data[card_count++] = hand_index.card_data[i][x];
				}
			}
		}
		return card_count;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] == 0) {
				continue;
			}

			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

			int index_color = GetCardLogicValueColorIndex(cbCardData[i]);
			AnalyseIndexResult.card_data_color[index_color][AnalyseIndexResult.card_index_color[index_color]] = cbCardData[i];
			AnalyseIndexResult.card_index_color[index_color]++;
		}
	}

	public int switch_card_to_index(int card) {
		int index = GetCardLogicValue(card) - 3;
		return index;
	}

	/**
	 * 扑克转换--将索引 转换 实际数据
	 * 
	 * @param card_index
	 * @return
	 */
	public int switch_to_card_data(int card_index) {
		if (card_index == 52) {
			return 0x4E;
		}
		if (card_index == 53) {
			return 0x4F;
		}
		return ((card_index / 13) << 4) | (card_index % 13 + 3);
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
	public void sort_card_date_list(int card_date[], int card_count) {
		// 转换数值
		int logic_value[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = GetCardLogicValue(card_date[i]);
		}

		// 排序操作
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		int nLaiZicount = this.GetLaiZiCount(card_date, card_count);
		int index = 0;

		if (nLaiZicount > 0) {
			for (int i = 0; i < last + 1; i++) {
				if (logic_value[i] == GetCardLogicValue(this._laizi)) {
					temp_date = card_date[i];
					card_date[i] = card_date[index];
					card_date[index] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[index];
					logic_value[index] = temp_date;
					index++;
				}

			}
		}

		do {
			sorted = true;

			for (int i = index; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1]) || ((logic_value[i] == logic_value[i + 1]) && (card_date[i] < card_date[i + 1]))) {
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
	public void AnalysebCardData(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult) {
		// 设置结果
		AnalyseResult.Reset();

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
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount] = cbCardData[i];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] = cbCardData[i + 1];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] = cbCardData[i + 2];
				AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] = cbCardData[i + 3];
				// if (cbCardCount == 4 || cbCardCount == 6 || cbCardCount == 7)
				// {
				// int cbIndex = AnalyseResult.cbFourCount++;
				// AnalyseResult.cbFourCardData[cbIndex * cbSameCount] =
				// cbCardData[i];
				// AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 1] =
				// cbCardData[i + 1];
				// AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 2] =
				// cbCardData[i + 2];
				// AnalyseResult.cbFourCardData[cbIndex * cbSameCount + 3] =
				// cbCardData[i + 3];
				// } else {
				// int cbIndex = AnalyseResult.cbThreeCount++;
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1)] =
				// cbCardData[i];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1) +
				// 1] = cbCardData[i + 1];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 1) +
				// 2] = cbCardData[i + 2];
				// int cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 3];
				// }
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
				// if (cbCardCount == 5) {
				// int cbIndex = AnalyseResult.cbFiveCount++;
				// AnalyseResult.cbFiveCardData[cbIndex * cbSameCount] =
				// cbCardData[i];
				// AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 1] =
				// cbCardData[i + 1];
				// AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 2] =
				// cbCardData[i + 2];
				// AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 3] =
				// cbCardData[i + 3];
				// AnalyseResult.cbFiveCardData[cbIndex * cbSameCount + 4] =
				// cbCardData[i + 4];
				// } else {
				// int cbIndex = AnalyseResult.cbThreeCount++;
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] =
				// cbCardData[i];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 1] = cbCardData[i + 1];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 2] = cbCardData[i + 2];
				// int cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 3];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 4];
				// }

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
				// if (cbCardCount == 6) {
				// int cbIndex = AnalyseResult.cbSixCount++;
				// AnalyseResult.cbSixCardData[cbIndex * cbSameCount] =
				// cbCardData[i];
				// AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 1] =
				// cbCardData[i + 1];
				// AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 2] =
				// cbCardData[i + 2];
				// AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 3] =
				// cbCardData[i + 3];
				// AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 4] =
				// cbCardData[i + 4];
				// AnalyseResult.cbSixCardData[cbIndex * cbSameCount + 5] =
				// cbCardData[i + 5];
				// } else {
				// int cbIndex = AnalyseResult.cbThreeCount++;
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] =
				// cbCardData[i];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 1] = cbCardData[i + 1];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 2] = cbCardData[i + 2];
				// int cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 3];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 4];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 5];
				// }

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
				// if (cbCardCount == 7) {
				// int cbIndex = AnalyseResult.cbSevenCount++;
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount] =
				// cbCardData[i];
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 1] =
				// cbCardData[i + 1];
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 2] =
				// cbCardData[i + 2];
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 3] =
				// cbCardData[i + 3];
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 4] =
				// cbCardData[i + 4];
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 5] =
				// cbCardData[i + 5];
				// AnalyseResult.cbSevenCardData[cbIndex * cbSameCount + 6] =
				// cbCardData[i + 6];
				// } else {
				// int cbIndex = AnalyseResult.cbThreeCount++;
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] =
				// cbCardData[i];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 1] = cbCardData[i + 1];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 2] = cbCardData[i + 2];
				// int cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 3];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 4];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 5];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 6];
				// }

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
				// if (cbCardCount == 8) {
				// int cbIndex = AnalyseResult.cbEightCount++;
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount] =
				// cbCardData[i];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 1] =
				// cbCardData[i + 1];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 2] =
				// cbCardData[i + 2];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 3] =
				// cbCardData[i + 3];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 4] =
				// cbCardData[i + 4];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 5] =
				// cbCardData[i + 5];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 6] =
				// cbCardData[i + 6];
				// AnalyseResult.cbEightCardData[cbIndex * cbSameCount + 7] =
				// cbCardData[i + 7];
				// } else {
				// int cbIndex = AnalyseResult.cbThreeCount++;
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2)] =
				// cbCardData[i];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 1] = cbCardData[i + 1];
				// AnalyseResult.cbThreeCardData[cbIndex * (cbSameCount - 2) +
				// 2] = cbCardData[i + 2];
				// int cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 3];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 4];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 5];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 6];
				// cbSingleIndex = AnalyseResult.cbSignedCount++;
				// AnalyseResult.cbSignedCardData[cbSingleIndex] = cbCardData[i
				// + 7];
				// }

				break;
			}
			}

			// 设置索引
			i += cbSameCount - 1;
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

	public int GetCardLogicValue(int CardData) {
		if (CardData == 0) {
			return -1;
		}
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 0x04)
			return cbCardValue + 2;
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
	}

	public int GetCardLogicValueColorIndex(int CardData) {
		if (CardData == 0) {
			return -1;
		}
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 0x04)
			return cbCardValue + 38; // 0x4E---14+38=52

		cbCardValue = (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
		return cbCardColor * 13 + (cbCardValue - 3);
	}

	public int GetCardLoigcValueColorData(int card_color_index) {
		if (card_color_index == 53) {
			return 0x4F;
		}
		if (card_color_index == 52) {
			return 0x4E;
		}
		int cbCardColor = card_color_index / 13;
		int cbCardValue = card_color_index % 13 + 3;
		if (cbCardValue > 13) {
			cbCardValue -= 13;
		}
		return (cbCardColor << 4) + cbCardValue;
	}

	// 获取数值
	public int get_card_value(int card) {
		if (card == 0) {
			return -1;
		}
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		if (card == 0) {
			return -1;
		}
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}
}
