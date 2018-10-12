package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic_XNDG extends WSKGameLogic {

	public WSKGameLogic_XNDG() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		int wang_count = Get_Wang_Count(card_index);
		int er_count = card_index.card_index[12];

		// 四王
		if (wang_count == cbCardCount) {
			if (wang_count == 1) {
				return GameConstants.XNDG_CT_SINGLE;
			} else if (wang_count == 2) {
				return GameConstants.XNDG_CT_DOUBLE;
			} else if (wang_count == 3) {
				return GameConstants.XNDG_CT_510K_SC;
			} else {
				return GameConstants.XNDG_CT_BOMB;
			}
		}

		if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] + wang_count == cbCardCount) {
			// 所有牌都为同一种牌
			if (cbCardCount == 1) {
				return GameConstants.XNDG_CT_SINGLE;
			}
			if (cbCardCount == 2) {
				return GameConstants.XNDG_CT_DOUBLE;
			}
			if (cbCardCount == 3) {
				return GameConstants.XNDG_CT_THREE;
			}
			if (cbCardCount >= 4) {
				return GameConstants.XNDG_CT_BOMB;
			}
			return GameConstants.XNDG_CT_ERROR;
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
						return GameConstants.XNDG_CT_510K_DC;
					}
				}
				return GameConstants.XNDG_CT_510K_SC;
			}
			return GameConstants.XNDG_CT_ERROR;
		}

		// 连对
		if (this.is_link(card_index, 2, cbCardCount / 2) && er_count == 0) {
			return GameConstants.XNDG_CT_DOUBLE_LINK;
		}
		if (this.is_link(card_index, 3, cbCardCount / 3) && er_count == 0) {
			return GameConstants.XNDG_CT_PLANE;
		}
		return GameConstants.WSK_GF_CT_ERROR;
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

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙
		if (cbNextType >= GameConstants.XNDG_CT_510K_DC && cbFirstType < GameConstants.XNDG_CT_510K_DC)
			return true;
		if (cbNextType < GameConstants.XNDG_CT_510K_DC && cbFirstType >= GameConstants.XNDG_CT_510K_DC) {
			return false;
		}
		if (cbNextType >= GameConstants.XNDG_CT_510K_DC && cbFirstType >= GameConstants.XNDG_CT_510K_DC) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == GameConstants.XNDG_CT_510K_SC) {
					int first_value = this.GetCardColor(cbFirstCard[0]);
					int next_value = this.GetCardColor(cbNextCard[0]);
					tagAnalyseIndexResult_WSK first_card_index = new tagAnalyseIndexResult_WSK();
					tagAnalyseIndexResult_WSK next_card_index = new tagAnalyseIndexResult_WSK();
					AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
					AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
					int first_wang_count = Get_Wang_Count(first_card_index);
					int next_wang_count = Get_Wang_Count(next_card_index);
					if (first_wang_count == cbFirstCount) {
						first_value = 0x35;
					}
					if (next_wang_count == cbNextCount) {
						next_value = 0x35;
					}
					return next_value > first_value;
				} else {
					if (cbFirstCount == cbNextCount) {
						return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
					} else {
						return cbNextCount > cbFirstCount;
					}

				}
			} else {
				return cbNextType > cbFirstType;
			}
		}

		if (cbNextType != cbFirstType || cbFirstCount != cbNextCount) {
			return false;
		} else {
			int first_value = GetCardLogicValue(cbFirstCard[0]);
			int next_value = GetCardLogicValue(cbNextCard[0]);

			tagAnalyseIndexResult_WSK first_card_index = new tagAnalyseIndexResult_WSK();
			tagAnalyseIndexResult_WSK next_card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
			AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
			int first_wang_count = Get_Wang_Count(first_card_index);
			int next_wang_count = Get_Wang_Count(next_card_index);
			if (first_wang_count == cbFirstCount) {
				if (this.has_rule(GameConstants.GAME_RULE_XNDG_LAI_SMALL)) {
					first_value = 3;
				} else {
					first_value = 14;
				}
			}
			if (next_wang_count == cbNextCount) {
				if (this.has_rule(GameConstants.GAME_RULE_XNDG_LAI_SMALL)) {
					next_value = 3;
				} else {
					next_value = 14;
				}
			}
			return next_value > first_value;
		}
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		int zheng_510K[] = new int[cbCardCount];
		int zheng_card_num = 0;
		int fu_510K[] = new int[cbCardCount];
		int fu_card_num = 0;
		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			// 先提取510K炸弹
			if (card_index.card_index[2] > 0 && card_index.card_index[7] > 0 && card_index.card_index[10] > 0
					&& card_index.card_index[2] < 4 && card_index.card_index[7] < 4 && card_index.card_index[10] < 4) {
				for (int i = 0; i < card_index.card_index[2]; i++) {
					for (int j = 0; j < card_index.card_index[7]; j++) {
						for (int x = 0; x < card_index.card_index[10]; x++) {
							if (card_index.card_data[2][i] == 0 || card_index.card_data[7][j] == 0
									|| card_index.card_data[10][x] == 0) {
								continue;
							}

							if (GetCardColor(card_index.card_data[2][i]) == GetCardColor(card_index.card_data[7][j])
									&& GetCardColor(card_index.card_data[2][i]) == GetCardColor(
											card_index.card_data[10][x])) {
								zheng_510K[zheng_card_num++] = card_index.card_data[2][i];
								zheng_510K[zheng_card_num++] = card_index.card_data[7][j];
								zheng_510K[zheng_card_num++] = card_index.card_data[10][x];
								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == card_index.card_data[2][i]) {
										cbCardData[y] = 0;
										break;
									}
								}
								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == card_index.card_data[7][j]) {
										cbCardData[y] = 0;
										break;
									}
								}
								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == card_index.card_data[10][x]) {
										cbCardData[y] = 0;
										break;
									}
								}
								card_index.card_data[2][i] = 0;
								card_index.card_data[7][j] = 0;
								card_index.card_data[10][x] = 0;
							}
						}
					}
				}

				for (int i = 0; i < card_index.card_index[2]; i++) {
					for (int j = 0; j < card_index.card_index[7]; j++) {
						for (int x = 0; x < card_index.card_index[10]; x++) {
							if (card_index.card_data[2][i] == 0 || card_index.card_data[7][j] == 0
									|| card_index.card_data[10][x] == 0) {
								continue;
							}
							fu_510K[fu_card_num++] = card_index.card_data[2][i];
							fu_510K[fu_card_num++] = card_index.card_data[7][j];
							fu_510K[fu_card_num++] = card_index.card_data[10][x];
							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == card_index.card_data[2][i]) {
									cbCardData[y] = 0;
									break;
								}
							}
							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == card_index.card_data[7][j]) {
									cbCardData[y] = 0;
									break;
								}
							}
							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == card_index.card_data[10][x]) {
									cbCardData[y] = 0;
									break;
								}
							}
							card_index.card_data[2][i] = 0;
							card_index.card_data[7][j] = 0;
							card_index.card_data[10][x] = 0;
						}
					}
				}
			}
			this.SortCardList(cbCardData, cbCardCount, GameConstants.WSK_ST_ORDER);
			//
			card_index.Reset();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
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
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					if (card_index.card_index[index[i]] < 4) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}

				}
			}
			this.SortCardList(cbCardData, sort_num, GameConstants.WSK_ST_ORDER);
			for (int i = 0; i < fu_card_num; i++) {
				cbCardData[sort_num++] = fu_510K[i];
			}
			for (int i = 0; i < zheng_card_num; i++) {
				cbCardData[sort_num++] = zheng_510K[i];
			}
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					if (card_index.card_index[index[i]] >= 4) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}

				}
			}
			return;
		}
		// 转换数值
		int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
		for (int i = 0; i < cbCardCount; i++) {
			switch (cbSortType) {
			case GameConstants.WSK_ST_CUSTOM:
			case GameConstants.WSK_ST_ORDER: // 等级排序
			{
				cbSortValue[i] = GetCardLogicValue(cbCardData[i]);
				break;
			}
			case GameConstants.WSK_ST_VALUE: // 数值排序
			{
				cbSortValue[i] = GetCardValue(cbCardData[i]);
				break;
			}
			case GameConstants.WSK_ST_COLOR: // 花色排序
			{
				cbSortValue[i] = GetCardColor(cbCardData[i]) + GetCardLogicValue(cbCardData[i]);
				break;
			}
			case GameConstants.WSK_ST_510K: // 花色排序
			{
				cbSortValue[i] = GetCardColor(cbCardData[i]) + GetCardLogicValue(cbCardData[i]) + 0x1000;
				break;
			}
			}
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

		if (cbSortType == GameConstants.WSK_ST_CUSTOM) {
			for (int i = cbCardCount - 1; i >= 0; i--) {
				if (cbCardData[i] > 0x100) {

					for (int j = i; j < cbCardCount - 1; j++) {
						if (cbCardData[j + 1] < 0x100) {
							int temp = cbCardData[j];
							cbCardData[j] = cbCardData[j + 1];
							cbCardData[j + 1] = temp;
						} else {
							break;
						}
					}
				}
			}
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
