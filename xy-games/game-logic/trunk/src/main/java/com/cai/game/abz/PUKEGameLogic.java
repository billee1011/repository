package com.cai.game.abz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;

class tagAnalyseIndexResult {
	public int card_index[] = new int[GameConstants.ABZ_MAX_INDEX];
	public int card_data[][] = new int[GameConstants.ABZ_MAX_INDEX][GameConstants.MAX_PDK_COUNT_EQ];

	public tagAnalyseIndexResult() {
		for (int i = 0; i < GameConstants.ABZ_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void Reset() {
		for (int i = 0; i < GameConstants.ABZ_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
};

public class PUKEGameLogic {
	private static Logger logger = Logger.getLogger(PUKEGameLogic.class);
	public Map<Integer, Integer> ruleMap = new HashMap<>();

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public void switch_to_card_index(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue(card_data[i]);
			card_index[index - 3]++;
		}
	}

	public int get_card_index(int card_data) {
		int index = GetCardLogicValue(card_data);
		return index - 3;
	}

	public int get_index_value(int index) {
		return index + 3;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.ABZ_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 1) {
			return GameConstants.ABZ_CT_SINGLE;
		}
		tagAnalyseIndexResult IndexResult = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		int max_index = this.get_card_index(cbCardData[0]);
		int min_idnex = get_card_index(cbCardData[cbCardCount - 1]);
		if (cbCardCount == 2) {
			if (IndexResult.card_index[max_index] == 2) {
				return GameConstants.ABZ_CT_DOUBLE;
			}
		} else if (cbCardCount == 3) {
			if (IndexResult.card_index[max_index] == 3) {
				return GameConstants.ABZ_CT_BOMB;
			}
		} else if (cbCardCount == 4) {
			if (IndexResult.card_index[max_index] == 4) {
				return GameConstants.ABZ_CT_BOMB;
			}
		}
		// 连对
		if (max_index - min_idnex < 1) {
			return GameConstants.ABZ_CT_ERROR;
		}
		if (max_index < GameConstants.ABZ_MAX_INDEX - 2) {
			for (int i = max_index; i >= min_idnex; i--) {
				if (IndexResult.card_index[i] != 2) {
					break;
				}
				if (i == min_idnex) {
					return GameConstants.ABZ_CT_DOUBLE_LINK;
				}
			}
		}
		// 顺子
		if (max_index - min_idnex < 2) {
			return GameConstants.ABZ_CT_ERROR;
		}
		if (max_index < GameConstants.ABZ_MAX_INDEX - 2) {
			for (int i = max_index; i >= min_idnex; i--) {
				if (IndexResult.card_index[i] != 1) {
					return GameConstants.ABZ_CT_ERROR;
				}
				if (i == min_idnex) {
					return GameConstants.ABZ_CT_SINGLE_LINK;
				}
			}

		}

		return GameConstants.ABZ_CT_ERROR;
	}

	public boolean comparecarddata(int first_card[], int first_count, int next_card[], int next_count) {
		tagAnalyseIndexResult IndexResult_first = new tagAnalyseIndexResult();
		tagAnalyseIndexResult IndexResult_next = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(first_card, first_count, IndexResult_first);
		AnalysebCardDataToIndex(next_card, next_count, IndexResult_next);

		int first_type = this.GetCardType(first_card, first_count);
		int next_type = this.GetCardType(next_card, next_count);

		if (next_type == first_type) {
			if (first_type == GameConstants.ABZ_CT_BOMB) {
				if (first_count == next_count) {
					return GetCardLogicValue(next_card[0]) > GetCardLogicValue(first_card[0]);
				} else {
					return next_count > first_count;
				}
			} else if (first_type == GameConstants.ABZ_CT_SINGLE_LINK
					|| first_type == GameConstants.ABZ_CT_DOUBLE_LINK) {
				if (first_count == next_count) {
					return GetCardLogicValue(next_card[0]) > GetCardLogicValue(first_card[0]);
				} else {
					return false;
				}
			} else {
				return GetCardLogicValue(next_card[0]) > GetCardLogicValue(first_card[0]);
			}
		} else {
			if (first_type == GameConstants.ABZ_CT_BOMB) {
				return false;
			} else if (next_type == GameConstants.ABZ_CT_BOMB) {
				return true;
			} else {
				return false;
			}
		}
	}

	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		tagAnalyseIndexResult IndexResult_handt = new tagAnalyseIndexResult();
		tagAnalyseIndexResult IndexResult_out = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult_handt);
		AnalysebCardDataToIndex(cbOutCardData, out_card_count, IndexResult_out);

		int out_card_type = this.GetCardType(cbOutCardData, out_card_count);
		int out_max_index = this.get_card_index(cbOutCardData[0]);
		int out_min_index = this.get_card_index(cbOutCardData[out_card_count - 1]);
		if (out_card_type == GameConstants.ABZ_CT_BOMB) {
			for (int i = GameConstants.ABZ_MAX_INDEX - 1; i >= 0; i--) {
				if (IndexResult_handt.card_index[i] > out_card_count) {
					return IndexResult_handt.card_index[i];
				} else if (IndexResult_handt.card_index[i] == out_card_count && i > out_max_index) {
					return IndexResult_handt.card_index[i];
				}
			}
		}
		if (out_card_type == GameConstants.ABZ_CT_DOUBLE_LINK) {
			for (int i = GameConstants.ABZ_MAX_INDEX - 3; i >= 0; i--) {
				if (IndexResult_handt.card_index[i] >= 2 && i > out_max_index) {
					for (int j = i; j > out_min_index; j--) {
						if (IndexResult_handt.card_index[j] < 2) {
							if (i - j >= out_card_count / 2) {
								return (i - j) * 2;
							} else {
								for (int x = i; x > j; x--) {
									if (IndexResult_handt.card_index[x] >= 3) {
										return IndexResult_handt.card_index[x];
									}
								}
								i = j;
								break;
							}
						} else if (j == out_min_index + 1) {
							if (i - j >= out_card_count / 2 - 1) {
								return (i - j + 1) * 2;
							} else {
								for (int x = i; x > j; x--) {
									if (IndexResult_handt.card_index[x] >= 3) {
										return IndexResult_handt.card_index[x];
									}
								}
								return 0;
							}
						}
					}
				} else if (IndexResult_handt.card_index[i] >= 3) {
					return IndexResult_handt.card_index[i];
				}
			}
		}
		if (out_card_type == GameConstants.ABZ_CT_SINGLE_LINK) {
			for (int i = GameConstants.ABZ_MAX_INDEX - 3; i >= 0; i--) {
				if (IndexResult_handt.card_index[i] >= 1 && i > out_max_index) {
					for (int j = i; j > out_min_index; j--) {
						if (IndexResult_handt.card_index[j] < 1) {
							if (i - j > out_card_count - 1) {
								return i - j;
							} else {
								for (int x = i; x > j; x--) {
									if (IndexResult_handt.card_index[x] >= 3) {
										return IndexResult_handt.card_index[x];
									}
								}
								i = j;
								break;
							}
						} else if (j == out_min_index + 1) {
							if (i - j >= out_card_count - 1) {
								return i - j + 1;
							} else {
								for (int x = i; x > j; x--) {
									if (IndexResult_handt.card_index[x] >= 3) {
										return IndexResult_handt.card_index[x];
									}
								}
								return 0;
							}
						}
					}
				} else if (IndexResult_handt.card_index[i] >= 3) {
					return IndexResult_handt.card_index[i];
				}
			}
		}
		if (out_card_type == GameConstants.ABZ_CT_DOUBLE) {
			for (int i = GameConstants.ABZ_MAX_INDEX - 1; i >= 0; i--) {
				if (IndexResult_handt.card_index[i] > 2) {
					return IndexResult_handt.card_index[i];
				} else if (IndexResult_handt.card_index[i] >= 2 && i > out_max_index) {
					return IndexResult_handt.card_index[i];
				}
			}
		}
		if (out_card_type == GameConstants.ABZ_CT_SINGLE) {
			for (int i = GameConstants.ABZ_MAX_INDEX - 1; i >= 0; i--) {
				if (IndexResult_handt.card_index[i] > 2) {
					return IndexResult_handt.card_index[i];
				} else if (IndexResult_handt.card_index[i] >= 1 && i > out_max_index) {
					return IndexResult_handt.card_index[i];
				}
			}
		}
		return 0;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = GetCardColor(CardData);
		int cbCardValue = GetCardValue(CardData);

		// 转换数值
		if (cbCardColor == 0x40)
			return cbCardValue + 2;
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
	}

	// 获取数值
	public int GetCardValue(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int GetCardColor(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_COLOR;
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
	public void SortCardList(int cbCardData[], int cbCardCount) {
		// 转换数值
		int cbLogicValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++)
			cbLogicValue[i] = GetCardLogicValue(cbCardData[i]);

		// 排序操作
		boolean bSorted = true;
		int cbTempData, bLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < bLast; i++) {
				if ((cbLogicValue[i] < cbLogicValue[i + 1])
						|| ((cbLogicValue[i] == cbLogicValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
					// 交换位置
					cbTempData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbTempData;
					cbTempData = cbLogicValue[i];
					cbLogicValue[i] = cbLogicValue[i + 1];
					cbLogicValue[i + 1] = cbTempData;
					bSorted = false;
				}
			}
			bLast--;
		} while (bSorted == false);

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
				if (remove_cards[i] == cbTempCardData[j] || remove_cards[i] + 0x100 == cbTempCardData[j]) {
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

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

}
