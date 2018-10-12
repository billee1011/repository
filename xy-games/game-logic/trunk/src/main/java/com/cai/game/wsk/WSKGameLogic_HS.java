package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic_HS extends WSKGameLogic {

	public WSKGameLogic_HS() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		int wang_count = Get_Wang_Count(card_index);
		int er_count = card_index.card_index[12];

		if (wang_count == 1) {
			return GameConstants.HSDY_CT_SINGLE;
		}
		if (wang_count > 0) {
			// 四王
			if (wang_count == cbCardCount) {
				return GameConstants.HSDY_CT_KING_BOOM;
			} else {

			}
			return GameConstants.HSDY_CT_ERROR;
		}

		if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == cbCardCount) {
			// 所有牌都为同一种牌
			if (cbCardCount == 1) {
				return GameConstants.HSDY_CT_SINGLE;
			}
			if (cbCardCount == 2) {
				return GameConstants.HSDY_CT_DOUBLE;
			}
			if (cbCardCount >= 3) {
				return GameConstants.HSDY_CT_BOMB;
			}
			return GameConstants.HSDY_CT_ERROR;
		}

		if (cbCardCount == 3) {
			// 510K
			int one_card_value = this.GetCardLogicValue(cbCardData[0]);
			int two_card_value = this.GetCardLogicValue(cbCardData[1]);
			int three_card_value = this.GetCardLogicValue(cbCardData[2]);
			if (one_card_value == 5 && two_card_value == 10 && three_card_value == 13) {
				int color = this.GetCardColor(cbCardData[0]);
				for (int i = 1; i < cbCardCount; i++) {
					if (GetCardColor(cbCardData[i]) != color) {
						return GameConstants.HSDY_CT_510K_DC;
					}
				}
				return GameConstants.HSDY_CT_510K_SC;
			}
		}
		// 顺子
		if (this.is_link(card_index, 1, 3) && er_count == 0) {
			return GameConstants.HSDY_CT_SINGLE_LINK;
		}
		// 连对
		if (this.is_link(card_index, 2, cbCardCount / 2) && er_count == 0) {
			return GameConstants.HSDY_CT_DOUBLE_LINK;
		}
		return GameConstants.HSDY_CT_ERROR;
	}

	// 是否连
	public boolean is_link(tagAnalyseIndexResult_WSK card_data_index, int link_num, int link_count_num) {
		int pai_count = 0;
		int wang_count = this.Get_Wang_Count(card_data_index);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			pai_count += card_data_index.card_index[i];
		}
		int num = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
			if (card_data_index.card_index[i] == 0) {
				if (num == 0) {
					continue;
				} else {
					if (num >= link_count_num && (num * link_num == pai_count)) {
						return true;
					} else {
						return false;
					}
				}
			}

			if (card_data_index.card_index[i] == link_num) {
				num++;
			} else if (card_data_index.card_index[i] > link_num) {
				return false;
			} else {
				if (card_data_index.card_index[i] + wang_count >= link_num) {
					num++;
					wang_count -= link_num - card_data_index.card_index[i];
				} else {
					return false;
				}
			}
		}
		if (num >= link_count_num) {
			return true;
		} else {
			return false;
		}
	}

	public int get_liang_pai(int card_data[], int cbCardCount) {
		int liang_pai_value = GameConstants.INVALID_CARD;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(card_data, cbCardCount, card_index);
		int j = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % cbCardCount);

		for (int i = 0; i < cbCardCount; i++) {
			int index = this.switch_card_to_idnex(card_data[j]);
			if (index < 12) {
				if (card_index.card_index[index] > 1) {
					int count = 0;
					for (int x = 0; x < card_index.card_index[index]; x++) {
						if (card_data[j] == card_index.card_data[index][x]) {
							count++;
						}
					}
					if (count == 1) {
						return card_data[j];
					}
				} else {
					return card_data[j];
				}
			}
			j = (j + 1) % cbCardCount;
		}
		for (int i = 0; i < cbCardCount; i++) {
			int index = this.switch_card_to_idnex(card_data[j]);
			if (card_index.card_index[index] > 1) {
				int count = 0;
				for (int x = 0; x < card_index.card_index[index]; x++) {
					if (card_data[j] == card_index.card_data[index][x]) {
						count++;
					}
				}
				if (count == 1) {
					return card_data[j];
				}
			} else {
				return card_data[j];
			}
			j = (j + 1) % cbCardCount;
		}
		return 0;
	}

	public int get_card_type_value(int card_data[], int card_count) {
		int value = 0;
		int card_type = GetCardType(card_data, card_count);
		switch (card_type) {
		case GameConstants.HSDY_CT_SINGLE:
		case GameConstants.HSDY_CT_DOUBLE:
		case GameConstants.HSDY_CT_SINGLE_LINK:
		case GameConstants.HSDY_CT_DOUBLE_LINK: {
			return 1;
		}
		case GameConstants.HSDY_CT_BOMB: {
			return card_count;
		}
		case GameConstants.HSDY_CT_510K_DC: {
			return 9;
		}
		case GameConstants.HSDY_CT_510K_SC: {
			return 10;
		}
		case GameConstants.HSDY_CT_KING_BOOM: {
			if (card_count == 2) {
				return 11;
			}
			if (card_count == 3) {
				return 12;
			}
			if (card_count == 4) {
				return 13;
			}
		}
		}
		return 0;
	}

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);
		int fist_type_value = get_card_type_value(cbFirstCard, cbFirstCount);
		int next_type_value = get_card_type_value(cbNextCard, cbNextCount);
		// 炸弹以上一定大于单牌、对子和单龙
		if (cbNextType >= GameConstants.HSDY_CT_BOMB && cbFirstType < GameConstants.HSDY_CT_BOMB)
			return true;
		if (cbNextType < GameConstants.HSDY_CT_BOMB && cbFirstType >= GameConstants.HSDY_CT_BOMB) {
			return false;
		}
		if (cbNextType >= GameConstants.HSDY_CT_BOMB && cbFirstType >= GameConstants.HSDY_CT_BOMB) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == GameConstants.HSDY_CT_510K_SC) {
					int first_value = this.GetCardColor(cbFirstCard[0]);
					int next_value = this.GetCardColor(cbNextCard[0]);
					return next_value > first_value;
				} else if (cbNextType == GameConstants.HSDY_CT_510K_DC) {
					return false;
				} else if (cbNextType == GameConstants.HSDY_CT_KING_BOOM) {
					return false;
				} else {
					if (cbFirstCount == cbNextCount) {
						return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
					} else {
						return cbNextCount > cbFirstCount;
					}

				}
			} else {
				return next_type_value > fist_type_value;
			}
		}

		if (cbNextType != cbFirstType || cbFirstCount != cbNextCount) {
			return false;
		} else {
			int first_value = GetCardLogicValue(cbFirstCard[0]);
			int next_value = GetCardLogicValue(cbNextCard[0]);
			return next_value > first_value;
		}
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;

		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			int card_5[] = new int[cbCardCount];
			int card_10[] = new int[cbCardCount];
			int card_K[] = new int[cbCardCount];
			int num_5 = 0;
			int num_10 = 0;
			int num_K = 0;
			// 先提取510K炸弹
			if (card_index.card_index[2] > 0 && card_index.card_index[7] > 0 && card_index.card_index[10] > 0) {
				for (int j = 0; j < card_index.card_index[10]; j++) {
					card_K[num_K++] = card_index.card_data[10][j];

				}
				for (int j = 0; j < card_index.card_index[7]; j++) {
					card_10[num_10++] = card_index.card_data[7][j];

				}
				for (int j = 0; j < card_index.card_index[2]; j++) {
					card_5[num_5++] = card_index.card_data[2][j];

				}
				card_index.card_index[10] = 0;
				card_index.card_index[7] = 0;
				card_index.card_index[2] = 0;
			}

			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}
			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[index[i]] > card_index.card_index[index[j]]) {
						int temp = index[j];
						index[j] = index[i];
						index[i] = temp;
					} else if (card_index.card_index[index[i]] == card_index.card_index[index[j]]) {
						if (index[i] > index[j]) {
							int temp = index[j];
							index[j] = index[i];
							index[i] = temp;
						}
					}
				}
			}
			int sort_num = 0;
			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					cbCardData[sort_num++] = card_index.card_data[index[i]][j];
				}
			}

			// 王牌后面
			for (int j = 0; j < card_index.card_index[13]; j++) {
				cbCardData[sort_num++] = card_index.card_data[13][j];
			}
			for (int j = 0; j < card_index.card_index[14]; j++) {
				cbCardData[sort_num++] = card_index.card_data[14][j];
			}
			for (int i = 0; i < num_5; i++) {
				cbCardData[sort_num++] = card_5[i];
			}
			for (int i = 0; i < num_10; i++) {
				cbCardData[sort_num++] = card_10[i];
			}
			for (int i = 0; i < num_K; i++) {
				cbCardData[sort_num++] = card_K[i];
			}

			return;
		} else if (cbSortType == GameConstants.WSK_ST_ORDER) {
			int card_510K[] = new int[cbCardCount];
			int num_510K = 0;
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			// 510K排前面
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_510K[num_510K++] = card_index.card_data[10][j];
			}
			for (int j = 0; j < card_index.card_index[7]; j++) {
				card_510K[num_510K++] = card_index.card_data[7][j];
			}
			for (int j = 0; j < card_index.card_index[2]; j++) {
				card_510K[num_510K++] = card_index.card_data[2][j];
			}
			card_index.card_index[10] = 0;
			card_index.card_index[7] = 0;
			card_index.card_index[2] = 0;
			if (card_index.card_index[13] + card_index.card_index[14] < 2) {
				int sort_num = 0;
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					for (int j = 0; j < card_index.card_index[i]; j++) {
						cbCardData[sort_num++] = card_index.card_data[i][j];
					}
				}
				// 510K牌最后
				for (int i = 0; i < num_510K; i++) {
					cbCardData[sort_num++] = card_510K[i];
				}
			} else {
				int sort_num = 0;
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
					for (int j = 0; j < card_index.card_index[i]; j++) {
						cbCardData[sort_num++] = card_index.card_data[i][j];
					}
				}
				// 王牌后面
				for (int j = 0; j < card_index.card_index[13]; j++) {
					cbCardData[sort_num++] = card_index.card_data[13][j];
				}
				for (int j = 0; j < card_index.card_index[14]; j++) {
					cbCardData[sort_num++] = card_index.card_data[14][j];
				}
				// 510K牌最后
				for (int i = 0; i < num_510K; i++) {
					cbCardData[sort_num++] = card_510K[i];
				}

			}
		} else {
			// 转换数值
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = GetCardLogicValue(cbCardData[i]);
			}

			// 排序操作
			boolean bSorted = true;
			int cbSwitchData = 0, cbLast = cbCardCount - 1;
			do {
				bSorted = true;
				for (int i = 0; i < cbLast; i++) {
					if ((cbSortValue[i] > cbSortValue[i + 1])
							|| ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] > cbCardData[i + 1]))) {
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
		}

		return;
	}

	public int GetCardXianScore(int cbCardData[], int cbCardCount, int card_type) {
		int score = 0;
		if (card_type == GameConstants.XNDG_CT_BOMB) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			int wang_count = Get_Wang_Count(card_index);
			int bomb_count = cbCardCount - wang_count;
			if (bomb_count >= 8) {
				score = 2;
			} else if (bomb_count == 7) {
				score = 1;
			}
		}

		return score;
	}

	public void make_change_card(int cbCardData[], int cbCardCount, int cbRealData[], int type) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbRealData, cbCardCount, card_index);
		int wang_count = Get_Wang_Count(card_index);
		for (int i = 0; i < cbCardCount; i++) {
			cbCardData[i] = cbRealData[i];
		}
		if (wang_count == 0) {
			return;
		}
		if (type == -1) {
			// 四王
			if (wang_count == cbCardCount) {
				if (this.has_rule(GameConstants.GAME_RULE_XNDG_LAI_SMALL)) {
					for (int i = 0; i < cbCardCount; i++) {
						cbCardData[i] = 0x03 + 0x100;
					}
				} else {
					for (int i = 0; i < cbCardCount; i++) {
						cbCardData[i] = 0x02 + 0x100;
					}
				}
				return;
			}

			if (cbCardCount == 3) {
				int num = 0;
				boolean have_ten = false;
				boolean have_five = false;
				boolean have_k = false;
				for (int i = 0; i < cbCardCount; i++) {
					if (GetCardLogicValue(cbCardData[i]) == 5 && !have_five) {
						num++;
						have_five = true;
					}
					if (GetCardLogicValue(cbCardData[i]) == 10 && !have_ten) {
						num++;
						have_ten = true;
					}
					if (GetCardLogicValue(cbCardData[i]) == 13 && !have_k) {
						num++;
						have_k = true;
					}
					if (num == cbCardCount - wang_count) {
						if (num == 1) {
							int color = this.GetCardColor(cbCardData[0]);
							int value = this.GetCardValue(cbCardData[0]);
							if (value == 5) {
								cbCardData[1] = color * 16 + 10 + 0x100;
								cbCardData[2] = color * 16 + 13 + 0x100;
							} else if (value == 10) {
								cbCardData[1] = color * 16 + 5 + 0x100;
								cbCardData[2] = color * 16 + 13 + 0x100;
							} else {
								cbCardData[1] = color * 16 + 5 + 0x100;
								cbCardData[2] = color * 16 + 10 + 0x100;
							}

						} else {
							if (GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[1])) {
								cbCardData[2] = GetCardColor(cbCardData[0]) * 16 + 28 - GetCardValue(cbCardData[0])
										- GetCardValue(cbCardData[1]) + 0x100;
							} else {
								cbCardData[2] = 3 * 16 + 28 - GetCardValue(cbCardData[0]) - GetCardValue(cbCardData[1])
										+ 0x100;
							}
						}
						return;
					}
				}
			}
			if (wang_count + card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == cbCardCount) {
				for (int i = 0; i < cbCardCount; i++) {
					if (cbCardData[i] >= 0x4E) {
						cbCardData[i] = cbCardData[0] + 0x100;
					}
				}
				return;
			}

			int max_index = this.switch_card_to_idnex(cbCardData[cbCardCount - wang_count - 1]);
			int min_index = this.switch_card_to_idnex(cbCardData[0]);
			int max_count = cbCardCount / (max_index - min_index + 1);
			for (int i = 0; i < cbCardCount - wang_count;) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] < max_count) {
					int temp = 0;
					for (int j = cbCardCount - wang_count; j < cbCardCount;) {
						cbCardData[j] = cbCardData[i] + 0x100;
						temp++;
						wang_count--;
						j = cbCardCount - wang_count;
						if (temp >= max_count - card_index.card_index[this.switch_card_to_idnex(cbCardData[i])]
								|| wang_count <= 0) {
							break;
						}
					}
				}
				i += card_index.card_index[this.switch_card_to_idnex(cbCardData[i])];
			}
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			if (wang_count == 0) {
				return;
			} else {
				for (int i = max_index + 1; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
					for (int j = 0; j < max_count; j++) {
						cbCardData[cbCardCount - wang_count] = this.switch_idnex_to_data(i) + 0x100;
						wang_count--;
						if (wang_count <= 0) {
							return;
						}
					}

				}
				if (wang_count == 0) {
					return;
				}
				for (int i = min_index - 1; i >= 0; i--) {
					for (int j = 0; j < max_count; j++) {
						cbCardData[cbCardCount - wang_count] = this.switch_idnex_to_data(i) + 0x100;
						wang_count--;
						if (wang_count <= 0) {
							return;
						}
					}
				}
			}
		} else {
			int link_num = 2;
			if (type == GameConstants.XNDG_CT_DOUBLE_LINK) {
				link_num = 2;
			} else if (type == GameConstants.XNDG_CT_PLANE) {
				link_num = 3;
			} else {
				return;
			}
			for (int i = 0; i < cbCardCount - wang_count;) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] < link_num) {
					int temp = 0;
					for (int j = cbCardCount - wang_count; j < cbCardCount;) {
						cbCardData[j] = cbCardData[i] + 0x100;
						temp++;
						wang_count--;
						if (temp >= link_num - card_index.card_index[this.switch_card_to_idnex(cbCardData[j])]
								|| wang_count <= 0) {
							break;
						}
						j += card_index.card_index[this.switch_card_to_idnex(cbCardData[i])];
					}
				}
				i += card_index.card_index[this.switch_card_to_idnex(cbCardData[i])];
			}
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			if (wang_count == 0) {
				return;
			} else {
				int max_index = this.switch_card_to_idnex(cbCardData[cbCardCount - wang_count]);
				int min_index = this.switch_card_to_idnex(cbCardData[0]);
				for (int i = max_index + 1; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
					for (int j = 0; j < link_num; j++) {
						cbCardData[cbCardCount - wang_count] = this.switch_idnex_to_data(i) + 0x100;
						wang_count--;
						if (wang_count <= 0) {
							return;
						}
					}

				}
				if (wang_count == 0) {
					return;
				}
				for (int i = min_index - 1; i >= 0; i--) {
					for (int j = 0; j < link_num; j++) {
						cbCardData[cbCardCount - wang_count] = this.switch_idnex_to_data(i) + 0x100;
						wang_count--;
						if (wang_count <= 0) {
							return;
						}
					}
				}
			}
		}
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		int count = 0;
		int turn_card_type = this.GetCardType(turn_card_data, turn_card_count);

		switch (turn_card_type) {
		case GameConstants.HSDY_CT_KING_BOOM: {
			return search_out_card_king_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case GameConstants.HSDY_CT_510K_SC: {
			return search_out_card_real_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case GameConstants.HSDY_CT_510K_DC: {
			return search_out_card_false_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case GameConstants.HSDY_CT_BOMB: {
			return search_out_card_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case GameConstants.HSDY_CT_DOUBLE_LINK: {
			return search_out_card_double_link(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case GameConstants.HSDY_CT_SINGLE_LINK: {
			return search_out_card_single_link(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		case GameConstants.HSDY_CT_DOUBLE:
		case GameConstants.HSDY_CT_SINGLE: {
			return search_out_card_double_single(cbCardData, cbCardCount, turn_card_data, turn_card_count);
		}
		}

		return count;
	}

	public int search_out_card_double_single(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] >= 3) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] >= turn_card_count && index > turn_index) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}

	public int search_out_card_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] > turn_card_count) {
				return 1;
			} else if (hand_card_idnex.card_index[index] == turn_card_count && index > turn_index) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}

	public int search_out_card_single_link(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < turn_card_count;) {
			int index = switch_card_to_idnex(turn_card_data[i]);
			if (index < turn_index) {
				turn_index = index;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] >= 3) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] >= 1 && index > turn_index) {
				int prv_index = index;
				for (int j = i + hand_card_idnex.card_index[index]; j < cbCardCount;) {
					int other_index = this.switch_card_to_idnex(cbCardData[j]);
					if (hand_card_idnex.card_index[other_index] >= 3) {
						return 1;
					}
					if (hand_card_idnex.card_index[other_index] > 0 && prv_index == other_index - 1) {
						prv_index = other_index;
						if ((prv_index - index) + 1 >= turn_card_count) {
							return 1;
						}
					} else {
						break;
					}
					if (hand_card_idnex.card_index[other_index] > 0) {
						j += hand_card_idnex.card_index[other_index];
					} else {
						j++;
					}
				}
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}

	public int search_out_card_double_link(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] >= 2) {
			return 1;
		}
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			return 1;
		}

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < turn_card_count;) {
			int index = switch_card_to_idnex(turn_card_data[i]);
			if (index < turn_index) {
				turn_index = index;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (hand_card_idnex.card_index[index] >= 3) {
				return 1;
			}
			if (hand_card_idnex.card_index[index] >= 2 && index > turn_index) {
				int prv_index = index;
				for (int j = i + hand_card_idnex.card_index[index]; j < cbCardCount;) {
					int other_index = this.switch_card_to_idnex(cbCardData[j]);
					if (hand_card_idnex.card_index[other_index] >= 3) {
						return 1;
					}
					if (hand_card_idnex.card_index[other_index] >= 2 && prv_index == other_index - 1) {
						prv_index = other_index;
						if ((prv_index - index) + 1 >= turn_card_count / 2) {
							return 1;
						}
					}
					if (hand_card_idnex.card_index[other_index] > 0) {
						j += hand_card_idnex.card_index[other_index];
					} else {
						j++;
					}
				}
			}
			if (hand_card_idnex.card_index[index] > 0) {
				i += hand_card_idnex.card_index[index];
			} else {
				i++;
			}
		}
		return 0;
	}

	public int search_out_card_king_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		return 0;
	}

	public int search_out_card_real_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		if (card_index.card_index[13] + card_index.card_index[14] >= 2) {
			return 1;
		}

		// 正510K黑桃直接返回
		if (this.GetCardColor(cbCardData[0]) == 3) {
			return 0;
		}
		if (card_index.card_index[2] <= 0 || card_index.card_index[7] <= 0 || card_index.card_index[10] <= 0) {
			return 0;
		}

		int color = this.GetCardColor(cbCardData[0]);
		for (int color_temp = color + 1; color_temp < 4; color_temp++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < card_index.card_index[2]; i++) {
				if (this.GetCardColor(card_index.card_data[2][i]) == color_temp) {
					is_five = true;
					break;
				}
			}
			for (int i = 0; i < card_index.card_index[7]; i++) {
				if (this.GetCardColor(card_index.card_data[7][i]) == color_temp) {
					is_ten = true;
					break;
				}
			}
			for (int i = 0; i < card_index.card_index[10]; i++) {
				if (this.GetCardColor(card_index.card_data[10][i]) == color_temp) {
					is_k = true;
					break;
				}
			}

			if (is_five && is_ten && is_k) {
				return 1;
			}
		}

		return 0;
	}

	public int search_out_card_false_510K(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		if (card_index.card_index[13] + card_index.card_index[14] >= 2) {
			return 1;
		}
		if (card_index.card_index[2] <= 0 || card_index.card_index[7] <= 0 || card_index.card_index[10] <= 0) {
			return 0;
		}

		for (int color = 0; color < 4; color++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < card_index.card_index[2]; i++) {
				if (this.GetCardColor(card_index.card_data[2][i]) == color) {
					is_five = true;
				}
			}
			for (int i = 0; i < card_index.card_index[7]; i++) {
				if (this.GetCardColor(card_index.card_data[7][i]) == color) {
					is_ten = true;
				}
			}
			for (int i = 0; i < card_index.card_index[10]; i++) {
				if (this.GetCardColor(card_index.card_data[10][i]) == color) {
					is_k = true;
				}
			}
			if (is_five && is_ten && is_k) {
				return 1;
			}
		}

		return 0;
	}

	public int GetHandCardXianScore(int cbCardData[], int cbCardCount, int sheng_dang_biaozhi) {
		int score = 0;
		int wang_count = Get_Wang_Count(cbCardData, cbCardCount);
		int max_num_index = -1;
		int max_num = 0;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		//
		for (int i = 0; i < cbCardCount;) {
			int index = this.switch_card_to_idnex(cbCardData[i]);
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}

			if (card_index.card_index[index] >= 8) {
				score += 2;
			} else if (card_index.card_index[index] >= 4 && index == 2) {
				if (card_index.card_index[2] >= 6 && card_index.card_index[7] >= 6 && card_index.card_index[10] >= 6) {
					if (card_index.card_index[2] != 8 && card_index.card_index[7] != 8
							&& card_index.card_index[10] != 8) {
						score += 2;
						card_index.card_index[2] = 0;
						card_index.card_index[7] = 0;
						card_index.card_index[10] = 0;
					}
				} else if (card_index.card_index[2] >= 4 && card_index.card_index[7] >= 4
						&& card_index.card_index[10] >= 4) {
					if (card_index.card_index[2] < 7 && card_index.card_index[7] < 7 && card_index.card_index[10] < 7) {
						score += 1;
						card_index.card_index[2] = 0;
						card_index.card_index[7] = 0;
						card_index.card_index[10] = 0;
					}
				}

			} else if (card_index.card_index[index] == 7) {
				score += 1;
			}

		}
		if (this.Get_Wang_Count(card_index) == 4) {
			score += 1;
		}
		return score;
	}

	public int Get_510K_Count(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		//
		int score = 0;
		if (card_index.card_index[2] >= 4) {
			if (card_index.card_index[2] >= 6 && card_index.card_index[7] >= 6 && card_index.card_index[10] >= 6) {
				if (card_index.card_index[2] != 8 && card_index.card_index[7] != 8 && card_index.card_index[10] != 8) {
					score += 2;
					card_index.card_index[2] = 0;
					card_index.card_index[7] = 0;
					card_index.card_index[10] = 0;
				}
			} else if (card_index.card_index[2] >= 4 && card_index.card_index[7] >= 4
					&& card_index.card_index[10] >= 4) {
				if (card_index.card_index[2] < 7 && card_index.card_index[7] < 7 && card_index.card_index[10] < 7) {
					score += 1;
					card_index.card_index[2] = 0;
					card_index.card_index[7] = 0;
					card_index.card_index[10] = 0;
				}
			}

		}

		return score;
	}
}
