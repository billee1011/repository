package com.cai.game.shengji;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.shengji.data.tagAnalyseIndexResult_Wzdd;

public class SJGameLogic_Wzdd extends SJGameLogic {

	public SJGameLogic_Wzdd() {
		_chang_zhu_one = 15;
		_chang_zhu_two = 14;
		_chang_zhu_three = -1;
		_chang_zhu_four = -1;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_Wzdd AnalyseIndexResult) {

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			int color = this.GetCardColor(cbCardData[i]);
			AnalyseIndexResult.card_data[color][index - 2][AnalyseIndexResult.card_index[color][index
					- 2]] = cbCardData[i];
			AnalyseIndexResult.card_index[color][index - 2]++;

		}
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 1) {
			return GameConstants.WZDD_CT_SINGLE;
		}

		int color = this.GetCardColor(cbCardData[0]);
		for (int i = 1; i < cbCardCount; i++) {
			if (this.GetCardColor(cbCardData[i]) != color) {
				return GameConstants.WZDD_CT_ERROR;
			}

		}

		tagAnalyseIndexResult_Wzdd IndexResult = new tagAnalyseIndexResult_Wzdd();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		int min_index = -1;
		int max_index = -1;
		if (cbCardCount == 2) {
			int index = this.get_card_index(cbCardData[0]);
			if (this.get_card_index(cbCardData[0]) == 14 && this.get_card_index(cbCardData[1]) == 15) {
				return GameConstants.WZDD_CT_TWO_KING;
			} else {
				return GameConstants.WZDD_CT_ERROR;
			}
		}
		int prv_index = this.get_card_index(cbCardData[0]);
		for (int i = 1; i < cbCardCount; i++) {
			int next_index = this.get_card_index(cbCardData[i]);
			if (prv_index == next_index - 1) {
				prv_index = next_index;
			} else {
				return GameConstants.WZDD_CT_ERROR;
			}
			if (cbCardCount - 1 == i) {
				return GameConstants.WZDD_CT_SINGLE_LINK;
			}
		}

		return GameConstants.WZDD_CT_ERROR;
	}

	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		int card_out_count = 0;
		int can_out_card_data_temp[] = new int[cbHandCardCount];
		int can_out_card_count_temp = 0;
		if (out_card_count == 0) {
			for (int i = 0; i < cbHandCardCount; i++) {
				card_data[card_out_count++] = hand_card_data[i];
			}
			return card_out_count;
		}
		int first_type = this.GetCardType(cbOutCardData, out_card_count);
		int color = this.GetCardColor(cbOutCardData[0]);
		if (_zhu_type >= 0) {
			if (GetCardValue(cbOutCardData[0]) == _chang_zhu_three || GetCardValue(cbOutCardData[0]) == _chang_zhu_four
					|| GetCardValue(cbOutCardData[0]) == _chang_zhu_one
					|| GetCardValue(cbOutCardData[0]) == _chang_zhu_two) {
				color = this._zhu_type;
			}
		}

		if (GetCardColor_Count(hand_card_data, cbHandCardCount, color) >= out_card_count) {
			int hand_card_data_temp[] = new int[cbHandCardCount];
			for (int i = 0; i < cbHandCardCount; i++) {
				if (color == _zhu_type) {
					if (this.GetCardColor(hand_card_data[i]) == color
							|| this.GetCardValue(hand_card_data[i]) == _chang_zhu_one
							|| GetCardValue(hand_card_data[i]) == _chang_zhu_two
							|| this.GetCardValue(hand_card_data[i]) == _chang_zhu_three
							|| GetCardValue(hand_card_data[i]) == _chang_zhu_four) {
						can_out_card_data_temp[can_out_card_count_temp++] = hand_card_data[i];
					}
				} else {
					if (_zhu_type >= 0) {
						if (this.GetCardColor(hand_card_data[i]) == color
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_one
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_two
								&& this.GetCardValue(hand_card_data[i]) != _chang_zhu_three
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_four) {
							can_out_card_data_temp[can_out_card_count_temp++] = hand_card_data[i];
						}
					} else {
						if (this.GetCardColor(hand_card_data[i]) == color) {
							can_out_card_data_temp[can_out_card_count_temp++] = hand_card_data[i];
						}
					}

				}
			}
		} else {
			for (int i = 0; i < cbHandCardCount; i++) {
				card_data[card_out_count++] = hand_card_data[i];
			}
			return card_out_count;
		}

		for (int i = 0; i < can_out_card_count_temp; i++) {
			card_data[card_out_count++] = can_out_card_data_temp[i];
		}

		return card_out_count;
	}

	public boolean is_he_li(int first_card[], int first_count, int out_card[], int out_count, int hand_card_data[],
			int hand_card_count) {

		if (first_count != out_count) {
			return false;
		}
		int first_color = this.GetCardColor(first_card[0]);
		if (_zhu_type >= 0) {
			if (GetCardValue(first_card[0]) == _chang_zhu_one || GetCardValue(first_card[0]) == _chang_zhu_two
					|| GetCardValue(first_card[0]) == _chang_zhu_three
					|| GetCardValue(first_card[0]) == _chang_zhu_four) {
				first_color = this._zhu_type;
			}
		}

		int first_type = this.GetCardType(first_card, first_count);

		int can_out_data[] = new int[hand_card_count];
		int can_out_count = Player_Can_out_card(hand_card_data, hand_card_count, first_card, first_count, can_out_data);

		for (int i = 0; i < out_count; i++) {
			for (int j = 0; j < can_out_count; j++) {
				if (out_card[i] == can_out_data[j]) {
					can_out_data[j] = 0;
					break;
				}
				if (j == can_out_count - 1) {
					return false;
				}
			}
		}

		int hand_color_count = GetCardColor_Count(hand_card_data, hand_card_count, first_color);
		if (can_out_count == hand_card_count && hand_color_count != hand_card_count) {
			int must_count = 0;
			int must_card[] = new int[can_out_count];
			for (int i = 0; i < hand_card_count; i++) {
				if (first_color == _zhu_type) {
					if (this.GetCardColor(hand_card_data[i]) == first_color
							|| this.GetCardValue(hand_card_data[i]) == _chang_zhu_one
							|| GetCardValue(hand_card_data[i]) == _chang_zhu_two
							|| GetCardValue(hand_card_data[i]) == _chang_zhu_three
							|| GetCardValue(hand_card_data[i]) == _chang_zhu_four) {
						must_card[must_count++] = hand_card_data[i];
					}
				} else {
					if (_zhu_type >= 0) {
						if (this.GetCardColor(hand_card_data[i]) == first_color
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_one
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_two
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_three
								&& GetCardValue(hand_card_data[i]) != _chang_zhu_four) {
							must_card[must_count++] = hand_card_data[i];
						}
					} else {
						if (this.GetCardColor(hand_card_data[i]) == first_color) {
							must_card[must_count++] = hand_card_data[i];
						}
					}

				}
			}

			int out_card_temp[] = new int[out_count];
			for (int i = 0; i < out_count; i++) {
				out_card_temp[i] = out_card[i];
			}
			for (int j = 0; j < must_count; j++) {
				for (int i = 0; i < out_count; i++) {
					if (out_card_temp[i] == must_card[j]) {
						must_card[j] = 0;
						break;
					}
					if (i == out_count - 1) {
						return false;
					}
				}
			}

		}
		return true;
	}

	public boolean comparecarddata(int first_card[], int first_count, int next_card[], int next_count) {
		tagAnalyseIndexResult_Wzdd IndexResult_first = new tagAnalyseIndexResult_Wzdd();
		tagAnalyseIndexResult_Wzdd IndexResult_next = new tagAnalyseIndexResult_Wzdd();
		AnalysebCardDataToIndex(first_card, first_count, IndexResult_first);
		AnalysebCardDataToIndex(next_card, next_count, IndexResult_next);

		int color = this.GetCardColor(next_card[0]);
		for (int i = 1; i < next_count; i++) {
			if (this.GetCardColor(next_card[i]) != color) {
				return false;
			}

		}

		int first_type = this.GetCardType(first_card, first_count);
		int next_type = this.GetCardType(next_card, next_count);
		int first_color = this.GetCardColor(first_card[first_count - 1]);
		int next_color = this.GetCardColor(next_card[next_count - 1]);

		if (_zhu_type >= 0) {
			if (this.GetCardValue(first_card[0]) == _chang_zhu_one
					|| this.GetCardValue(first_card[0]) == _chang_zhu_two) {
				first_color = this._zhu_type;
			}
			if (this.GetCardValue(next_card[0]) == _chang_zhu_one || this.GetCardValue(next_card[0]) == _chang_zhu_two
					|| this.GetCardValue(next_card[0]) == _chang_zhu_three
					|| this.GetCardValue(next_card[0]) == _chang_zhu_four) {
				next_color = this._zhu_type;
			}
		}

		if (first_color == next_color) {
			if (next_type == first_type) {
				int frist_index = this.get_card_index(first_card[first_count - 1]);
				int next_index = get_card_index(next_card[next_count - 1]);

				if (frist_index >= 14 && next_index >= 14) {
					return false;
				}
				if (frist_index >= next_index) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} else {
			if (first_color == _zhu_type) {
				return false;
			} else if (next_color == _zhu_type) {
				return true;
			}
			return false;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = GetCardColor(CardData);
		int cbCardValue = GetCardValue(CardData);

		// 转换数值
		if (cbCardValue >= 14)
			return cbCardValue + 2;
		if (cbCardValue == 1)
			return cbCardValue + 13;
		return cbCardValue;
	}

	public int get_card_index(int card_data) {
		int index = GetCardLogicValue(card_data);
		return index - 2;
	}

	// 获取数值
	public int GetCardValue(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int GetCardColor(int cbCardData) {
		if (cbCardData == 0) {
			return -1;
		}
		return (cbCardData & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public int GetCardColor_Count(int cbCardData[], int card_count, int color) {
		int count = 0;
		for (int i = 0; i < card_count; i++) {
			if (color != this._zhu_type) {
				if (_zhu_type >= 0) {
					if (GetCardColor(cbCardData[i]) == color && this.GetCardValue(cbCardData[i]) != _chang_zhu_one
							&& this.GetCardValue(cbCardData[i]) != _chang_zhu_two
							&& this.GetCardValue(cbCardData[i]) != _chang_zhu_three
							&& this.GetCardValue(cbCardData[i]) != _chang_zhu_four) {
						count++;
					}
				} else {
					if (GetCardColor(cbCardData[i]) == color) {
						count++;
					}
				}

			} else {
				if (_zhu_type >= 0) {
					if (GetCardColor(cbCardData[i]) == color || this.GetCardValue(cbCardData[i]) == _chang_zhu_one
							|| this.GetCardValue(cbCardData[i]) == _chang_zhu_two
							|| this.GetCardValue(cbCardData[i]) == _chang_zhu_three
							|| this.GetCardValue(cbCardData[i]) == _chang_zhu_four) {
						count++;
					}
				} else {
					if (GetCardColor(cbCardData[i]) == color) {
						count++;
					}
				}

			}
		}
		return count;
	}

	public int GetZhu_Count(int cbCardData[], int card_count) {
		int count = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbCardData[i] > this._zhu_value) {
				count++;
			}
		}
		return count;
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
	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		// 转换数值
		int cbSortValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardValue(cbCardData[i]) == _chang_zhu_one
					|| this.GetCardValue(cbCardData[i]) == _chang_zhu_two) {
				cbSortValue[i] = (4 << 8) + GetCardLogicValue(cbCardData[i]);
			} else if (this.GetCardValue(cbCardData[i]) == _chang_zhu_four) {
				if (GetCardColor(cbCardData[i]) == this._zhu_type) {
					cbSortValue[i] = (4 << 5) + GetCardLogicValue(cbCardData[i]);
				} else {
					cbSortValue[i] = (4 << 4) + GetCardLogicValue(cbCardData[i]) + GetCardColor(cbCardData[i]);
				}

			} else if (this.GetCardValue(cbCardData[i]) == _chang_zhu_three) {
				if (GetCardColor(cbCardData[i]) == this._zhu_type) {
					cbSortValue[i] = (4 << 7) + GetCardLogicValue(cbCardData[i]);
				} else {
					cbSortValue[i] = (4 << 6) + GetCardLogicValue(cbCardData[i]) + GetCardColor(cbCardData[i]);
				}

			} else if (GetCardColor(cbCardData[i]) == this._zhu_type) {
				cbSortValue[i] = (4 << 4) + GetCardLogicValue(cbCardData[i]);
			} else {
				cbSortValue[i] = (GetCardColor(cbCardData[i]) << 4) + GetCardLogicValue(cbCardData[i]);
			}

		}

		// 排序操作
		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1]) || ((cbSortValue[i] == cbSortValue[i + 1])
						&& (GetCardLogicValue(cbCardData[i]) < GetCardLogicValue(cbCardData[i + 1])))) {
					// 设置标志
					bSorted = false;

					// 扑克数据
					cbSwitchData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbSwitchData;

					// 排序权位
					cbSwitchData = cbSortValue[i];
					cbSortValue[i] = cbSortValue[i + 1];
					cbSortValue[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
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

	public int GetCardScore(int cbCardData[], int cbCardCount) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardValue(cbCardData[i]) == 10) {
				score += 10;
			} else if (this.GetCardValue(cbCardData[i]) == 11 || this.GetCardValue(cbCardData[i]) == 12
					|| this.GetCardValue(cbCardData[i]) == 13) {
				score += 20;
			} else if (this.GetCardValue(cbCardData[i]) == 1) {
				score += 30;
			} else if (this.GetCardValue(cbCardData[i]) == 14 || this.GetCardValue(cbCardData[i]) == 15) {
				score += 50;
			}
		}
		return score;
	}

	public int get_card_double(int cbCardData[], int cbCardCount) {
		int double_count = 0;
		tagAnalyseIndexResult_Wzdd IndexResult = new tagAnalyseIndexResult_Wzdd();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < GameConstants.WZDD_MAX_INDEX; j++) {
				if (IndexResult.card_index[i][j] == 2) {
					double_count++;
				}
			}
		}

		return double_count;
	}

	// 埋牌事发后合法
	public boolean is_mai_di_right(int cbCardData[], int cbCardCount, int hand_data[], int hand_count) {
		tagAnalyseIndexResult_Wzdd IndexResult = new tagAnalyseIndexResult_Wzdd();
		tagAnalyseIndexResult_Wzdd hand_idnex = new tagAnalyseIndexResult_Wzdd();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		AnalysebCardDataToIndex(hand_data, hand_count, hand_idnex);

		int no_score_count = 0;
		int no_score_data[] = new int[3];
		for (int color = 0; color < 5; color++) {
			for (int i = 0; i < 8; i++) {
				if (hand_idnex.card_index[color][i] > 0) {
					for (int j = 0; j < hand_idnex.card_index[color][i]; j++) {
						no_score_data[no_score_count++] = hand_idnex.card_data[color][i][j];
						if (no_score_count >= 3) {
							break;
						}
					}

				}
				if (no_score_count >= 3) {
					break;
				}
			}
			if (no_score_count >= 3) {
				break;
			}
		}
		if (no_score_count >= 3) {
			for (int color = 0; color < 5; color++) {
				for (int i = 8; i < GameConstants.WZDD_MAX_INDEX; i++) {
					if (IndexResult.card_index[color][i] > 0) {
						return false;
					}
				}
			}
		} else {
			for (int i = 0; i < no_score_count; i++) {
				for (int j = 0; j < cbCardCount; j++) {
					if (no_score_data[i] == cbCardData[j]) {
						break;
					}
					if (j == cbCardCount - 1) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

}
