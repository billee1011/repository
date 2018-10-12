package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic_ShangRaoDaZha extends WSKGameLogic {

	public WSKGameLogic_ShangRaoDaZha() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount, boolean isLast) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		int wang_count = Get_Wang_Count(card_index);
		int er_count = card_index.card_index[12];

		// 四王
		if (wang_count == 4 && cbCardCount == 4) {
			return GameConstants.WSK_GF_CT_KING_FOUR;
		}

		// 王加4炸
		if (wang_count > 0) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] >= 4) {
				int count = card_index.card_index[this.switch_card_to_idnex(cbCardData[0])];
				if (count + wang_count == cbCardCount) {
					if (cbCardCount == 1) {
						return GameConstants.WSK_GF_CT_SINGLE;
					}
					if (cbCardCount == 2) {
						return GameConstants.WSK_GF_CT_DOUBLE;
					}
					if (cbCardCount == 3) {
						return GameConstants.WSK_GF_CT_THREE;
					}
					if (cbCardCount == 4) {
						return GameConstants.WSK_GF_CT_BOMB_4;
					}
					if (cbCardCount == 5) {
						return GameConstants.WSK_GF_CT_BOMB_5;
					}
					if (cbCardCount == 6) {
						return GameConstants.WSK_GF_CT_BOMB_6;
					}
					if (cbCardCount == 7) {
						return GameConstants.WSK_GF_CT_BOMB_7;
					}
					if (cbCardCount == 8) {
						return GameConstants.WSK_GF_CT_BOMB_8;
					}
					if (cbCardCount == 9) {
						return GameConstants.WSK_GF_CT_BOMB_9;
					}
					if (cbCardCount == 10) {
						return GameConstants.WSK_GF_CT_BOMB_10;
					}
					if (cbCardCount == 11) {
						return GameConstants.WSK_GF_CT_BOMB_11;
					}
					if (cbCardCount == 12) {
						return GameConstants.WSK_GF_CT_BOMB_12;
					}
					if (cbCardCount == 13) {
						return GameConstants.WSK_GF_CT_BOMB_13;
					}
				}
				return GameConstants.WSK_GF_CT_ERROR;
			}
		}
		if (card_index.card_index[this.switch_card_to_idnex(cbCardData[cbCardCount - 1])] == cbCardCount
				&& cbCardCount != 3) {
			// 所有牌都为同一种牌
			if (cbCardCount == 1) {
				return GameConstants.WSK_GF_CT_SINGLE;
			}
			if (cbCardCount == 2) {
				return GameConstants.WSK_GF_CT_DOUBLE;
			}
			// if (cbCardCount == 3) {
			// return GameConstants.WSK_GF_CT_THREE;
			// }
			if (cbCardCount == 4) {
				return GameConstants.WSK_GF_CT_BOMB_4;
			}
			if (cbCardCount == 5) {
				return GameConstants.WSK_GF_CT_BOMB_5;
			}
			if (cbCardCount == 6) {
				return GameConstants.WSK_GF_CT_BOMB_6;
			}
			if (cbCardCount == 7) {
				return GameConstants.WSK_GF_CT_BOMB_7;
			}
			if (cbCardCount == 8) {
				return GameConstants.WSK_GF_CT_BOMB_8;
			}
			return GameConstants.WSK_GF_CT_ERROR;
		}

		if (cbCardCount == 3) {
			// 三张
			if (isLast) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 3) {
					return GameConstants.WSK_GF_CT_THREE;
				}
			}
			// 510K
			int one_card_value = this.GetCardLogicValue(cbCardData[0]);
			int two_card_value = this.GetCardLogicValue(cbCardData[1]);
			int three_card_value = this.GetCardLogicValue(cbCardData[2]);
			if (one_card_value == 5 && two_card_value == 10 && three_card_value == 13) {
				int color = this.GetCardColor(cbCardData[0]);
				for (int i = 1; i < cbCardCount; i++) {
					if (GetCardColor(cbCardData[i]) != color) {
						return GameConstants.WSK_GF_CT_510K_DC;
					}
				}
				return GameConstants.WSK_GF_CT_510K_SC;
			}
			return GameConstants.WSK_GF_CT_ERROR;
		}
		if (cbCardCount == 4) {
			// 三带一
			if (isLast) {
				// if (has_rule(GameConstants.GAME_RULE_WSK_GF_SAN_DAI_ER)) {
				for (int i = 0; i < cbCardCount; i++) {
					if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3
							|| card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3) {
						return GameConstants.WSK_GF_CT_THREE_TAKE_TWO;
					}
					// }
				}
			}
		}
		if (cbCardCount == 5) {
			// 三带二
			// if (has_rule(GameConstants.GAME_RULE_WSK_GF_SAN_DAI_ER)) {
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3
						|| card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3) {
					return GameConstants.WSK_GF_CT_THREE_TAKE_TWO;
				}
			}
			// }
		}

		// 顺子
		if (this.is_link(card_index, 1, 5)) {
			return GameConstants.WSK_GF_CT_SINGLE_LINK;
		}

		if (cbCardCount % 2 == 0) {
			// 连对
			if (this.is_link(card_index, 2, cbCardCount / 2) && wang_count == 0 && er_count == 0) {
				return GameConstants.WSK_GF_CT_DOUBLE_LINK;
			}
		}
		// 三连
		if (cbCardCount % 3 == 0 && isLast) {
			if (this.is_link(card_index, 3, cbCardCount / 3)) {
				return GameConstants.WSK_GF_CT_PLANE;
			}
		}
		// 飞机
		int nPlane = is_plane(card_index, cbCardData, cbCardCount);
		if (nPlane == 0 && isLast) {
			return GameConstants.WSK_GF_CT_PLANE_LOST;
		} else if (nPlane == 1) {
			return GameConstants.WSK_GF_CT_PLANE;
		}
		return GameConstants.WSK_GF_CT_ERROR;
	}

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙
		if (cbNextType >= GameConstants.WSK_GF_CT_510K_DC && cbFirstType < GameConstants.WSK_GF_CT_510K_DC)
			return true;
		if (cbNextType < GameConstants.WSK_GF_CT_510K_DC && cbFirstType >= GameConstants.WSK_GF_CT_510K_DC) {
			return false;
		}
		if (cbNextType >= GameConstants.WSK_GF_CT_510K_DC && cbFirstType >= GameConstants.WSK_GF_CT_510K_DC) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == GameConstants.WSK_GF_CT_510K_SC) {
					return cbNextCard[0] > cbFirstCard[0];
				} else {
					return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
				}
			} else {
				return cbNextType > cbFirstType;
			}
		}

		if (cbNextType != cbFirstType || cbFirstCount != cbNextCount) {
			return false;
		} else {
			if (cbNextType == GameConstants.WSK_GF_CT_SINGLE || cbNextType == GameConstants.WSK_GF_CT_DOUBLE
					|| cbNextType == GameConstants.WSK_GF_CT_THREE || cbNextType == GameConstants.WSK_GF_CT_SINGLE_LINK
					|| cbNextType == GameConstants.WSK_GF_CT_DOUBLE_LINK) {
				return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
			}
			tagAnalyseIndexResult_WSK next_card_index = new tagAnalyseIndexResult_WSK();
			tagAnalyseIndexResult_WSK first_card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
			AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
			if (cbNextType == GameConstants.WSK_GF_CT_THREE_TAKE_TWO) {
				int next_index = -1;
				int first_index = -1;
				for (int i = 0; i < cbNextCount; i++) {
					if (next_index == -1 && next_card_index.card_index[this.switch_card_to_idnex(cbNextCard[i])] == 3) {
						next_index = this.switch_card_to_idnex(cbNextCard[i]);
					}
					if (first_index == -1
							&& first_card_index.card_index[this.switch_card_to_idnex(cbFirstCard[i])] == 3) {
						first_index = this.switch_card_to_idnex(cbFirstCard[i]);
					}
				}
				return next_index > first_index;
			}
			if (cbNextType == GameConstants.WSK_GF_CT_PLANE) {
				int next_index = -1;
				int first_index = -1;
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					if (next_index == -1 && next_card_index.card_index[i] == 3) {
						int link_num = 1;
						for (int j = i + 1; j < GameConstants.WSK_MAX_INDEX; j++) {
							if (next_card_index.card_index[j] == 3) {
								if ((j - i + 1) * 3 == cbFirstCount || (j - i + 1) * 5 == cbFirstCount) {
									next_index = i;
									break;
								}
							} else {
								if ((j - i) * 3 == cbFirstCount || (j - i) * 5 == cbFirstCount) {
									next_index = i;
									break;
								}
							}
						}
					}
					if (first_index == -1 && first_card_index.card_index[i] == 3) {
						int link_num = 1;
						for (int j = i + 1; j < GameConstants.WSK_MAX_INDEX; j++) {
							if (first_card_index.card_index[j] == 3) {
								if ((j - i + 1) * 3 == cbFirstCount || (j - i + 1) * 5 == cbFirstCount) {
									first_index = i;
									break;
								}
							} else {
								if ((j - i) * 3 == cbFirstCount || (j - i) * 5 == cbFirstCount) {
									first_index = i;
									break;
								}
							}
						}
					}
				}
				return next_index > first_index;
			}

		}
		return false;
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;

		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
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
		if (has_rule(GameConstants.GAME_RULE_WSK_GF_TWO_SHENG_DANG)) {
			if (this.GetCardValue(cbCardData[0]) == 2) {
				if (card_type <= GameConstants.WSK_GF_CT_BOMB_8) {
					card_type += 1;
				}
			}
		}
		if (has_rule(GameConstants.GAME_RULE_WSK_GF_SEVEN_SHENG_DANG)) {
			if (this.GetCardValue(cbCardData[0]) == 7) {
				if (card_type <= GameConstants.WSK_GF_CT_BOMB_8) {
					card_type += 1;
				}
			}
		}
		if (has_rule(GameConstants.GAME_RULE_WSK_GF_J_SHENG_DANG)) {
			if (this.GetCardValue(cbCardData[0]) == 11) {
				if (card_type <= GameConstants.WSK_GF_CT_BOMB_8) {
					card_type += 1;
				}
			}
		}

		if (card_type == GameConstants.WSK_GF_CT_BOMB_5) {
			score = 1;
		}
		if (card_type == GameConstants.WSK_GF_CT_BOMB_6) {
			score = 2;
		}
		if (card_type == GameConstants.WSK_GF_CT_BOMB_7) {
			score = 4;
		}
		if (card_type == GameConstants.WSK_GF_CT_BOMB_8) {
			score = 8;
		}
		if (card_type == GameConstants.WSK_GF_CT_BOMB_9) {
			score = 16;
		}
		if (card_type > GameConstants.WSK_GF_CT_BOMB_9) {
			if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_20)) {
				score = 20;
			} else if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_16)) {
				score = 16;
			}
		}
		return score;
	}

	public int GetHandCardXianScore(int cbCardData[], int cbCardCount, int sheng_dang_biaozhi) {
		int score = 0;
		int wang_count = Get_Wang_Count(cbCardData, cbCardCount);
		int max_num_index = -1;
		int max_num = 0;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (max_num == 0 && card_index.card_index[i] >= 4) {
				max_num_index = i;
				max_num = card_index.card_index[i];
			} else if (card_index.card_index[i] > max_num && card_index.card_index[i] >= 4) {
				max_num_index = i;
				max_num = card_index.card_index[i];
			}
		}

		if (max_num_index == -1) {
			return score;
		}

		if (sheng_dang_biaozhi > 0) {
			if ((sheng_dang_biaozhi & 1) != 0) {
				if (card_index.card_index[12] + wang_count >= max_num) {
					max_num_index = 12;
					max_num = card_index.card_index[12] + wang_count;
				}
			}

			if ((sheng_dang_biaozhi & 2) != 0) {
				if (card_index.card_index[4] + wang_count >= max_num) {
					max_num_index = 4;
					max_num = card_index.card_index[4] + wang_count;
				}
			}

			if ((sheng_dang_biaozhi & 4) != 0) {
				if (card_index.card_index[8] + wang_count >= max_num) {
					max_num_index = 8;
					max_num = card_index.card_index[8] + wang_count;
				}
			}
		}

		// 最大炸弹分数
		int card[] = new int[card_index.card_index[max_num_index] + wang_count];
		for (int i = 0; i < card_index.card_index[max_num_index]; i++) {
			card[i] = card_index.card_data[max_num_index][i];
		}
		for (int i = 0; i < card_index.card_index[13]; i++) {
			card[card_index.card_index[max_num_index] + i] = card_index.card_data[max_num_index][i];
		}
		for (int i = 0; i < card_index.card_index[14]; i++) {
			card[card_index.card_index[max_num_index] + card_index.card_index[13]
					+ i] = card_index.card_data[max_num_index][i];
		}

		int type = GetCardType(card, card.length, false);
		score += this.GetCardXianScore(card, card_index.card_index[max_num_index] + wang_count, type);

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_index.card_index[i] >= 4 && i != max_num_index) {
				int type1 = GetCardType(card_index.card_data[i], card_index.card_index[i], false);
				score += this.GetCardXianScore(card_index.card_data[i], card_index.card_index[i], type1);
			}
		}
		return score;
	}

}
