package com.cai.game.shengji;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.shengji.data.tagAnalyseCardType;

class tagAnalyseIndexResult_Lldq {
	public int card_index[][] = new int[5][GameConstants.LLDQ_MAX_INDEX];
	public int card_data[][][] = new int[5][GameConstants.LLDQ_MAX_INDEX][GameConstants.XFGD_MAX_COUT];

	public tagAnalyseIndexResult_Lldq() {
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < GameConstants.LLDQ_MAX_INDEX; j++) {
				card_index[i][j] = 0;
				Arrays.fill(card_data[i][j], 0);
			}
		}
	}

	public void Reset() {
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < GameConstants.LLDQ_MAX_INDEX; j++) {
				card_index[i][j] = 0;
				Arrays.fill(card_data[i][j], 0);
			}
		}
	}
};

public class SJGameLogic_Lldq extends SJGameLogic {

	public SJGameLogic_Lldq() {
		_chang_zhu_one = 15;
		_chang_zhu_two = 14;
		_chang_zhu_three = 7;
		_chang_zhu_four = 2;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_Lldq AnalyseIndexResult) {

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			int color = this.GetCardColor(cbCardData[i]);
			AnalyseIndexResult.card_data[color][index - 3][AnalyseIndexResult.card_index[color][index
					- 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[color][index - 3]++;

		}
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 1) {
			return GameConstants.LLDQ_CT_SINGLE;
		}
		tagAnalyseCardType type_card = new tagAnalyseCardType();
		Analyse_card_type(cbCardData, cbCardCount, type_card);
		for (int i = 1; i < type_card.type_count; i++) {
			if (type_card.type[i] != type_card.type[0]) {
				return GameConstants.LLDQ_CT_ERROR;
			}
		}

		int color = this.GetCardColor(cbCardData[0]);

		tagAnalyseIndexResult_Lldq IndexResult = new tagAnalyseIndexResult_Lldq();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		int min_index = -1;
		int max_index = -1;
		if (cbCardCount == 2) {
			int index = this.get_card_index(cbCardData[0]);
			if (IndexResult.card_index[color][index] == 2) {
				return GameConstants.LLDQ_CT_DOUBLE;
			}
		}

		if (this._zhu_type == 4) {
			if (IndexResult.card_index[color][GameConstants.LLDQ_MAX_INDEX - 5] == 2
					&& IndexResult.card_index[color][GameConstants.LLDQ_MAX_INDEX - 4] == 2) {
				for (int i = 0; i < GameConstants.LLDQ_MAX_INDEX - 5; i++) {
					if (IndexResult.card_index[color][i] == 2) {
						if (min_index == -1) {
							min_index = i;
							max_index = i;
						} else {
							max_index = i;
						}
					} else {
						if ((max_index - min_index + 2) * 2 == cbCardCount) {
							return GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER;
						} else {
							return GameConstants.LLDQ_CT_ERROR;
						}
					}
				}
			} else {
				// 正常拖拉机
				for (int i = 0; i < GameConstants.LLDQ_MAX_INDEX - 2; i++) {
					if ((i == 4 || i == 12) && IndexResult.card_index[color][i] > 0) {
						return GameConstants.LLDQ_CT_ERROR;
					}
					if (IndexResult.card_index[color][i] == 2) {
						if (min_index == -1) {
							min_index = i;
							max_index = i;
						} else {
							max_index = i;
						}
					} else {
						if (min_index != -1) {
							if ((max_index - min_index + 1) * 2 == cbCardCount) {
								return GameConstants.LLDQ_CT_DOUBLE_LINK;
							} else {
								return GameConstants.LLDQ_CT_ERROR;
							}
						}

					}
				}
				return GameConstants.LLDQ_CT_ERROR;
			}
		} else {
			// 先过滤AA2233的
			if (IndexResult.card_index[color][0] == 2
					&& IndexResult.card_index[color][GameConstants.LLDQ_MAX_INDEX - 4] == 2) {
				if (IndexResult.card_index[color][GameConstants.LLDQ_MAX_INDEX - 5] == 2) {
					min_index = 0;
					max_index = 0;
					for (int i = 0; i < GameConstants.LLDQ_MAX_INDEX - 5; i++) {
						if (IndexResult.card_index[color][i] == 2) {
							if (min_index == -1) {
								min_index = i;
								max_index = i;
							} else {
								max_index = i;
							}
						} else {
							if ((max_index - min_index + 3) * 2 == cbCardCount) {
								return GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER;
							} else {
								return GameConstants.LLDQ_CT_ERROR;
							}
						}
					}
				}
			} else if (IndexResult.card_index[color][GameConstants.LLDQ_MAX_INDEX - 5] == 2
					&& IndexResult.card_index[color][GameConstants.LLDQ_MAX_INDEX - 4] == 2) {
				for (int i = 0; i < GameConstants.LLDQ_MAX_INDEX - 5; i++) {
					if (IndexResult.card_index[color][i] == 2) {
						if (min_index == -1) {
							min_index = i;
							max_index = i;
						} else {
							max_index = i;
						}
					} else {
						if ((max_index - min_index + 2) * 2 == cbCardCount) {
							return GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER;
						} else {
							return GameConstants.LLDQ_CT_ERROR;
						}
					}
				}
			} else {
				// 正常拖拉机
				for (int i = 0; i < GameConstants.LLDQ_MAX_INDEX - 2; i++) {
					if (color != this._zhu_type) {
						if ((i == 4 || i == 12) && IndexResult.card_index[color][i] > 0) {
							return GameConstants.LLDQ_CT_ERROR;
						}
					}
					if (IndexResult.card_index[color][i] == 2) {
						if (min_index == -1) {
							min_index = i;
							max_index = i;
						} else {
							max_index = i;
						}
					} else {
						if (min_index != -1) {
							if ((max_index - min_index + 1) * 2 == cbCardCount) {
								return GameConstants.LLDQ_CT_DOUBLE_LINK;
							} else {
								return GameConstants.LLDQ_CT_ERROR;
							}
						}

					}
				}

			}
		}

		// for (int i = IndexResult.card_index[color][prv_index]; i <
		// cbCardCount;) {
		//
		// int card_color = this.GetCardColor(cbCardData[i]);
		// int index = this.get_card_index(cbCardData[i]);
		// if (card_color != color) {
		// return GameConstants.LLDQ_CT_ERROR;
		// }
		// if (card_color != this._zhu_type && (index == 4 || index == 12)) {
		// return GameConstants.LLDQ_CT_ERROR;
		// }
		// if (IndexResult.card_index[card_color][index] != 2) {
		// return GameConstants.LLDQ_CT_ERROR;
		// }
		// if (index != prv_index - 1) {
		// return GameConstants.LLDQ_CT_ERROR;
		// }
		// if (IndexResult.card_index[card_color][index] > 0) {
		// i += IndexResult.card_index[card_color][index];
		// } else {
		// i++;
		// }
		// }
		return GameConstants.LLDQ_CT_ERROR;
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
		if (GetCardValue(cbOutCardData[0]) == _chang_zhu_three || GetCardValue(cbOutCardData[0]) == _chang_zhu_four
				|| GetCardValue(cbOutCardData[0]) == _chang_zhu_one
				|| GetCardValue(cbOutCardData[0]) == _chang_zhu_two) {
			color = this._zhu_type;
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
					if (this.GetCardColor(hand_card_data[i]) == color
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_one
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_two
							&& this.GetCardValue(hand_card_data[i]) != _chang_zhu_three
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_four) {
						can_out_card_data_temp[can_out_card_count_temp++] = hand_card_data[i];
					}
				}
			}
		} else {
			for (int i = 0; i < cbHandCardCount; i++) {
				card_data[card_out_count++] = hand_card_data[i];
			}
			return card_out_count;
		}

		if (first_type == GameConstants.LLDQ_CT_SINGLE) {
			for (int i = 0; i < can_out_card_count_temp; i++) {
				card_data[card_out_count++] = can_out_card_data_temp[i];
			}
		} else {
			tagAnalyseIndexResult_Lldq IndexResult = new tagAnalyseIndexResult_Lldq();
			this.AnalysebCardDataToIndex(can_out_card_data_temp, can_out_card_count_temp, IndexResult);
			for (int i = 0; i < can_out_card_count_temp; i++) {
				int card_color = this.GetCardColor(can_out_card_data_temp[i]);
				int index = this.get_card_index(can_out_card_data_temp[i]);
				if (IndexResult.card_index[card_color][index] >= 2) {
					card_data[card_out_count++] = can_out_card_data_temp[i];
				}
			}
			if (card_out_count < out_card_count) {
				for (int i = 0; i < can_out_card_count_temp; i++) {
					int card_color = this.GetCardColor(can_out_card_data_temp[i]);
					int index = this.get_card_index(can_out_card_data_temp[i]);
					if (IndexResult.card_index[card_color][index] == 1) {
						card_data[card_out_count++] = can_out_card_data_temp[i];
					}
				}
			}
		}

		return card_out_count;
	}

	public boolean is_he_li(int first_card[], int first_count, int out_card[], int out_count, int hand_card_data[],
			int hand_card_count) {

		if (first_count != out_count) {
			return false;
		}
		int first_color = this.GetCardColor(first_card[0]);
		if (GetCardValue(first_card[0]) == _chang_zhu_one || GetCardValue(first_card[0]) == _chang_zhu_two
				|| GetCardValue(first_card[0]) == _chang_zhu_three || GetCardValue(first_card[0]) == _chang_zhu_four) {
			first_color = this._zhu_type;
		}
		int first_type = this.GetCardType(first_card, first_count);

		int can_out_data[] = new int[hand_card_count];
		int can_out_count = Player_Can_out_card(hand_card_data, hand_card_count, first_card, first_count, can_out_data);

		if (first_type == GameConstants.LLDQ_CT_SINGLE) {
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
		} else {
			tagAnalyseCardType out_type_card = new tagAnalyseCardType();
			tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
			this.Analyse_card_type(out_card, out_count, out_type_card);
			this.Analyse_card_type(can_out_data, can_out_count, can_out_type_card);
			for (int i = 0; i < out_type_card.type_count; i++) {
				for (int j = 0; j < can_out_type_card.type_count; j++) {
					if (out_type_card.count[i] > can_out_type_card.count[j]) {
						if (this.remove_cards_by_data(out_type_card.card_data[i], out_type_card.count[i],
								can_out_type_card.card_data[j], can_out_type_card.count[j])) {
							can_out_type_card.count[j] = 0;
							break;
						}
					} else {
						if (this.remove_cards_by_data(out_type_card.card_data[i], out_type_card.count[i],
								can_out_type_card.card_data[j], out_type_card.count[i])) {
							can_out_type_card.count[j] = 0;
							break;
						}
					}

					if (j == can_out_type_card.type_count - 1) {
						return false;
					}
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
					if (this.GetCardColor(hand_card_data[i]) == first_color
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_one
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_two
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_three
							&& GetCardValue(hand_card_data[i]) != _chang_zhu_four) {
						must_card[must_count++] = hand_card_data[i];
					}
				}
			}

			boolean is_double = false;
			boolean is_single = false;
			tagAnalyseCardType out_type_card = new tagAnalyseCardType();
			tagAnalyseCardType must_out_type_card = new tagAnalyseCardType();
			this.Analyse_card_type(out_card, out_count, out_type_card);
			this.Analyse_card_type(must_card, must_count, must_out_type_card);
			if (first_type == GameConstants.LLDQ_CT_DOUBLE || first_type == GameConstants.LLDQ_CT_DOUBLE_LINK
					|| first_type == GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER) {
				for (int j = 0; j < must_out_type_card.type_count; j++) {
					if (must_out_type_card.type[j] == GameConstants.LLDQ_CT_SINGLE) {
						is_single = true;
					}
					if (must_out_type_card.type[j] == GameConstants.LLDQ_CT_DOUBLE) {
						is_double = true;
					}
				}
			}

			if (is_double && is_double) {
				for (int j = 0; j < must_out_type_card.type_count; j++) {
					for (int i = 0; i < out_type_card.type_count; i++) {
						if (this.remove_cards_by_data(out_type_card.card_data[i], out_type_card.count[i],
								must_out_type_card.card_data[j], must_out_type_card.count[j])) {
							out_type_card.count[i] = 0;
							break;
						}
						if (i == out_type_card.type_count - 1) {
							return false;
						}
					}
				}
			} else {
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

		} else {

			if (first_type == GameConstants.LLDQ_CT_DOUBLE || first_type == GameConstants.LLDQ_CT_DOUBLE_LINK
					|| first_type == GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER) {
				tagAnalyseCardType out_type_card = new tagAnalyseCardType();
				tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
				this.Analyse_card_type(out_card, out_count, out_type_card);
				this.Analyse_card_type(can_out_data, can_out_count, can_out_type_card);
				int double_count = 0;
				for (int j = 0; j < can_out_type_card.type_count; j++) {
					if (can_out_type_card.type[j] == GameConstants.LLDQ_CT_DOUBLE) {
						for (int i = 0; i < out_type_card.type_count; i++) {
							if (out_type_card.card_data[i][0] == can_out_type_card.card_data[j][0]) {
								if (out_type_card.type[i] != GameConstants.LLDQ_CT_DOUBLE) {
									return false;
								}
							}
						}
					}
				}
			}

		}
		return true;
	}

	public boolean comparecarddata(int first_card[], int first_count, int next_card[], int next_count) {
		tagAnalyseIndexResult_Lldq IndexResult_first = new tagAnalyseIndexResult_Lldq();
		tagAnalyseIndexResult_Lldq IndexResult_next = new tagAnalyseIndexResult_Lldq();
		AnalysebCardDataToIndex(first_card, first_count, IndexResult_first);
		AnalysebCardDataToIndex(next_card, next_count, IndexResult_next);

		int first_type = this.GetCardType(first_card, first_count);
		int next_type = this.GetCardType(next_card, next_count);
		int first_color = this.GetCardColor(first_card[first_count - 1]);
		int next_color = this.GetCardColor(next_card[next_count - 1]);

		if (this.GetCardValue(first_card[0]) == _chang_zhu_one || this.GetCardValue(first_card[0]) == _chang_zhu_two
				|| this.GetCardValue(first_card[0]) == _chang_zhu_three
				|| this.GetCardValue(first_card[0]) == _chang_zhu_four) {
			first_color = this._zhu_type;
		}
		if (this.GetCardValue(next_card[0]) == _chang_zhu_one || this.GetCardValue(next_card[0]) == _chang_zhu_two
				|| this.GetCardValue(next_card[0]) == _chang_zhu_three
				|| this.GetCardValue(next_card[0]) == _chang_zhu_four) {
			next_color = this._zhu_type;
		}
		for (int i = 1; i < next_count; i++) {
			if (next_color == _zhu_type) {
				if (this.GetCardColor(next_card[i]) != next_color && this.GetCardValue(next_card[0]) != _chang_zhu_one
						&& this.GetCardValue(next_card[0]) != _chang_zhu_two
						&& this.GetCardValue(next_card[0]) != _chang_zhu_three
						&& this.GetCardValue(next_card[0]) != _chang_zhu_four) {
					return false;
				}
			} else {
				if (this.GetCardColor(next_card[i]) != next_color) {
					return false;
				}
				if (this.GetCardValue(next_card[0]) == _chang_zhu_one
						|| this.GetCardValue(next_card[0]) == _chang_zhu_two
						|| this.GetCardValue(next_card[0]) == _chang_zhu_three
						|| this.GetCardValue(next_card[0]) == _chang_zhu_four) {
					return false;
				}
			}
		}
		if (first_color == next_color) {
			if (next_type == first_type) {
				int frist_index = this.get_card_index(first_card[first_count - 1]);
				int next_index = get_card_index(next_card[next_count - 1]);

				if (first_type == GameConstants.LLDQ_CT_DOUBLE_LINK) {
					for (int i = 0; i < first_count; i++) {
						if (frist_index < get_card_index(first_card[i])) {
							frist_index = get_card_index(first_card[i]);
						}
						if (next_index < get_card_index(next_card[i])) {
							next_index = get_card_index(next_card[i]);
						}
					}
					if (first_color != _zhu_type && next_color == _zhu_type) {
						tagAnalyseCardType type_card = new tagAnalyseCardType();
						Analyse_card_type(next_card, next_count, type_card);
						for (int i = 0; i < type_card.type_count; i++) {
							if (type_card.type[i] != GameConstants.LLDQ_CT_DOUBLE) {
								return false;
							}
						}
						return true;
					}

				} else if (first_type == GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER) {
					for (int i = 0; i < first_count; i++) {
						int first_temp_index = get_card_index(first_card[i]);
						int next_temp_index = get_card_index(next_card[i]);
						if (frist_index == GameConstants.LLDQ_MAX_INDEX - 6
								|| frist_index == GameConstants.LLDQ_MAX_INDEX - 5) {
							frist_index = first_temp_index;
						} else if (first_temp_index != GameConstants.LLDQ_MAX_INDEX - 6
								&& first_temp_index != GameConstants.LLDQ_MAX_INDEX - 6) {
							if (first_temp_index > first_temp_index) {
								frist_index = first_temp_index;
							}
						}
						if (next_index == GameConstants.LLDQ_MAX_INDEX - 6
								|| next_index == GameConstants.LLDQ_MAX_INDEX - 5) {
							next_index = get_card_index(next_card[i]);
						} else if (first_temp_index != GameConstants.LLDQ_MAX_INDEX - 6
								&& first_temp_index != GameConstants.LLDQ_MAX_INDEX - 6) {
							if (next_temp_index > next_index) {
								next_index = next_temp_index;
							}
						}
					}
				} else if (first_type == GameConstants.LLDQ_CT_ERROR) {
					if (first_color == _zhu_type && next_color == _zhu_type) {
						tagAnalyseCardType first_type_card = new tagAnalyseCardType();
						tagAnalyseCardType next_type_card = new tagAnalyseCardType();
						Analyse_card_type(next_card, next_count, next_type_card);
						for (int i = 0; i < next_type_card.type_count; i++) {
							if (next_type_card.type[i] != GameConstants.LLDQ_CT_DOUBLE) {
								return false;
							}
						}
						int first_value = GetCardLogicValue(first_type_card.card_data[0][0]);
						int first_color_temp = this.GetCardColor(first_type_card.card_data[0][0]);
						for (int i = 0; i < next_type_card.type_count; i++) {
							if (GetCardLogicValue(next_type_card.card_data[i][0]) > first_value) {
								return true;
							} else if (GetCardLogicValue(next_type_card.card_data[i][0]) == first_value) {
								if (first_color_temp == this._zhu_type) {
									return false;
								} else if (GetCardColor(next_type_card.card_data[i][0]) == this._zhu_type) {
									return true;
								} else {
									first_value = GetCardLogicValue(first_type_card.card_data[1][0]);
									for (int j = 0; j < next_type_card.count[i]; j++) {
										next_type_card.card_data[i][j] = 0;
									}
									i = 0;
								}

							}
						}
						return true;
					} else {
						return false;
					}
				} else {
					if (GetCardValue(first_card[0]) == this._chang_zhu_one
							|| GetCardValue(first_card[0]) == this._chang_zhu_two) {
						frist_index += 4;
					}
					if (GetCardValue(next_card[0]) == this._chang_zhu_one
							|| GetCardValue(next_card[0]) == this._chang_zhu_two) {
						next_index += 4;
					}
					if (GetCardValue(first_card[0]) == this._chang_zhu_three) {
						if (this.GetCardColor(first_card[0]) == _zhu_type) {
							frist_index = 15;
						} else {
							frist_index = 14;
						}
					}
					if (GetCardValue(next_card[0]) == this._chang_zhu_three) {
						if (this.GetCardColor(next_card[0]) == _zhu_type) {
							next_index = 15;
						} else {
							next_index = 14;
						}
					}
					if (GetCardValue(first_card[0]) == this._chang_zhu_four) {
						if (this.GetCardColor(first_card[0]) == _zhu_type) {
							frist_index = 13;
						} else {
							frist_index = 12;
						}
					}
					if (GetCardValue(next_card[0]) == this._chang_zhu_four) {
						if (this.GetCardColor(next_card[0]) == _zhu_type) {
							next_index = 13;
						} else {
							next_index = 12;
						}
					}
				}

				if (frist_index >= next_index) {
					return false;
				} else {
					return true;
				}
			} else {
				if (first_type == GameConstants.LLDQ_CT_DOUBLE_LINK
						&& next_type == GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER) {
					return false;
				} else if (first_type == GameConstants.LLDQ_CT_DOUBLE_LINK_OTHER
						&& next_type == GameConstants.LLDQ_CT_DOUBLE_LINK) {
					return true;
				}

				if (first_type == GameConstants.LLDQ_CT_DOUBLE_LINK && next_color == _zhu_type) {

				}
				return false;
			}
		} else {
			if (first_color == _zhu_type) {
				return false;
			} else if (next_color == _zhu_type) {
				if (next_type == first_type) {
					return true;
				} else {
					tagAnalyseCardType next_type_card = new tagAnalyseCardType();
					Analyse_card_type(next_card, next_count, next_type_card);
					for (int i = 0; i < next_type_card.type_count; i++) {
						if (next_type_card.type[i] != GameConstants.LLDQ_CT_DOUBLE) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}
	}

	// 分析牌型
	public void Analyse_card_type(int cbCardData[], int cbCardCount, tagAnalyseCardType type_card) {
		tagAnalyseIndexResult_Lldq IndexResult = new tagAnalyseIndexResult_Lldq();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		for (int i = GameConstants.LLDQ_MAX_INDEX - 1; i >= 0; i--) {
			for (int color = 0; color < 5; color++) {
				if (IndexResult.card_index[color][i] == 2) {
					type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
					type_card.type[type_card.type_count] = GameConstants.LLDQ_CT_DOUBLE;
					for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
						type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
					}
					type_card.type_count++;
				} else {
					for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
						type_card.count[type_card.type_count] = 1;
						type_card.type[type_card.type_count] = GameConstants.LLDQ_CT_SINGLE;
						type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
						type_card.type_count++;
					}
				}
			}
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = GetCardColor(CardData);
		int cbCardValue = GetCardValue(CardData);

		// 转换数值
		if (cbCardValue >= 14)
			return cbCardValue + 3;
		if (cbCardValue == 1 || cbCardValue == 2)
			return cbCardValue + 13;
		return cbCardValue;
	}

	public int get_card_index(int card_data) {
		int index = GetCardLogicValue(card_data);
		return index - 3;
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
				if (GetCardColor(cbCardData[i]) == color && this.GetCardValue(cbCardData[i]) != _chang_zhu_one
						&& this.GetCardValue(cbCardData[i]) != _chang_zhu_two
						&& this.GetCardValue(cbCardData[i]) != _chang_zhu_three
						&& this.GetCardValue(cbCardData[i]) != _chang_zhu_four) {
					count++;
				}
			} else {
				if (GetCardColor(cbCardData[i]) == color || this.GetCardValue(cbCardData[i]) == _chang_zhu_one
						|| this.GetCardValue(cbCardData[i]) == _chang_zhu_two
						|| this.GetCardValue(cbCardData[i]) == _chang_zhu_three
						|| this.GetCardValue(cbCardData[i]) == _chang_zhu_four) {
					count++;
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
			if (this.GetCardValue(cbCardData[i]) == 5) {
				score += 5;
			} else if (this.GetCardValue(cbCardData[i]) == 10 || this.GetCardValue(cbCardData[i]) == 13) {
				score += 10;
			}
		}
		return score;
	}

	public int get_card_double(int cbCardData[], int cbCardCount) {
		int double_count = 0;
		tagAnalyseIndexResult_Lldq IndexResult = new tagAnalyseIndexResult_Lldq();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < GameConstants.LLDQ_MAX_INDEX; j++) {
				if (IndexResult.card_index[i][j] == 2) {
					double_count++;
				}
			}
		}

		return double_count;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

}
