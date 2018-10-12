package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic_SXTH extends WSKGameLogic {
	public int _boom_count = 6;

	public WSKGameLogic_SXTH() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 0) {
			return GameConstants.SXTH_CT_ERROR;
		}
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		int wang_count = Get_Wang_Count(card_index);
		int er_count = card_index.card_index[12];

		int one_idnex = this.switch_card_to_idnex(cbCardData[0]);
		if (card_index.card_index[one_idnex] == cbCardCount && cbCardCount < _boom_count) {
			if (this.has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA) && cbCardCount == 3) {
				int one_color = this.GetCardColor(cbCardData[cbCardCount - 1]);
				if (one_color == 4) {
					return GameConstants.SXTH_CT_TONG_ZHANG;
				} else {
					for (int i = 0; i < card_index.card_index[one_idnex]; i++) {
						if (one_color != GetCardColor(card_index.card_data[one_idnex][i])) {
							return GameConstants.SXTH_CT_TONG_ZHANG;
						}
					}
					return GameConstants.SXTH_CT_TONG_HUA;
				}

			} else {
				return GameConstants.SXTH_CT_TONG_ZHANG;
			}

		} else if (card_index.card_index[one_idnex] == cbCardCount) {
			return GameConstants.SXTH_CT_BOMB;
		}
		if (wang_count > 0) {
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[cbCardCount - 1])] >= _boom_count - 1) {
				// 王必须在其他牌有5张的情况下才能当癞子
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[cbCardCount - 1])]
						+ wang_count == cbCardCount) {
					// 所有牌都为同一种牌
					return GameConstants.SXTH_CT_BOMB;
				}
			}
			return GameConstants.SXTH_CT_ERROR;
		}

		if (cbCardCount == 3) {
			// 510K
			boolean five = false;
			boolean ten = false;
			boolean K = false;
			for (int i = 0; i < cbCardCount; i++) {
				if (this.GetCardValue(cbCardData[i]) == 5) {
					five = true;
				} else if (this.GetCardValue(cbCardData[i]) == 10) {
					ten = true;
				} else if (this.GetCardValue(cbCardData[i]) == 13) {
					K = true;
				}
			}
			if (five && ten && K) {
				int color = this.GetCardColor(cbCardData[0]);
				for (int i = 1; i < cbCardCount; i++) {
					if (GetCardColor(cbCardData[i]) != color) {
						return GameConstants.SXTH_CT_510K_DC;
					}
				}
				return GameConstants.SXTH_CT_510K_SC;
			}
			return GameConstants.SXTH_CT_ERROR;
		}

		return GameConstants.SXTH_CT_ERROR;
	}

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙
		if (cbNextType >= GameConstants.SXTH_CT_510K_DC && cbFirstType < GameConstants.SXTH_CT_510K_DC)
			return true;
		if (cbNextType < GameConstants.SXTH_CT_510K_DC && cbFirstType >= GameConstants.SXTH_CT_510K_DC) {
			return false;
		}
		if (cbNextType >= GameConstants.SXTH_CT_510K_DC && cbFirstType >= GameConstants.SXTH_CT_510K_DC) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == GameConstants.SXTH_CT_510K_DC || cbNextType == GameConstants.SXTH_CT_510K_DC) {
					return false;
				} else {
					if (cbFirstCount == cbNextCount) {
						return GetCardLogicValue(cbNextCard[cbNextCount - 1]) > GetCardLogicValue(
								cbFirstCard[cbFirstCount - 1]);
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
			int first_value = GetCardLogicValue(cbFirstCard[cbFirstCount - 1]);
			int next_value = GetCardLogicValue(cbNextCard[cbNextCount - 1]);
			return next_value > first_value;
		}
	}

	public void SortCardList_Count(int cbCardData[], int cbCardCount) {
		int card_510K[] = new int[cbCardCount];
		int num_510K = 0;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		// 510K排前面
		int card_temp[] = new int[card_index.card_index[2]];
		int card_count_temp = 0;
		for (int j = 0; j < card_index.card_index[2]; j++) {
			card_temp[card_count_temp++] = card_index.card_data[2][j];
		}
		this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
		for (int j = 0; j < card_count_temp; j++) {
			card_510K[num_510K++] = card_temp[j];
		}

		card_temp = new int[card_index.card_index[7]];
		card_count_temp = 0;
		for (int j = 0; j < card_index.card_index[7]; j++) {
			card_temp[card_count_temp++] = card_index.card_data[7][j];
		}
		this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
		for (int j = 0; j < card_count_temp; j++) {
			card_510K[num_510K++] = card_temp[j];
		}

		card_temp = new int[card_index.card_index[10]];
		card_count_temp = 0;
		for (int j = 0; j < card_index.card_index[10]; j++) {
			card_temp[card_count_temp++] = card_index.card_data[10][j];
		}
		this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
		for (int j = 0; j < card_count_temp; j++) {
			card_510K[num_510K++] = card_temp[j];
		}

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
		card_index.card_index[10] = 0;
		card_index.card_index[7] = 0;
		card_index.card_index[2] = 0;
		// 王牌后面
		int sort_num = 0;
		for (int j = 0; j < card_index.card_index[14]; j++) {
			cbCardData[sort_num++] = card_index.card_data[14][j];
		}
		for (int j = 0; j < card_index.card_index[13]; j++) {
			cbCardData[sort_num++] = card_index.card_data[13][j];
		}
		// 510K牌最后
		for (int i = 0; i < num_510K; i++) {
			cbCardData[sort_num++] = card_510K[i];
		}

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			card_temp = new int[card_index.card_index[index[i]]];
			card_count_temp = 0;
			for (int j = 0; j < card_index.card_index[index[i]]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[index[i]][j];
			}
			this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
			for (int j = 0; j < card_count_temp; j++) {
				cbCardData[sort_num++] = card_temp[j];
			}
		}
	}

	public void SortCardList_Order(int cbCardData[], int cbCardCount) {
		int card_510K[] = new int[cbCardCount];
		int num_510K = 0;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		// 510K排前面

		int card_temp[] = new int[card_index.card_index[2]];
		int card_count_temp = 0;
		for (int j = 0; j < card_index.card_index[2]; j++) {
			card_temp[card_count_temp++] = card_index.card_data[2][j];
		}
		this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
		for (int j = 0; j < card_count_temp; j++) {
			card_510K[num_510K++] = card_temp[j];
		}

		card_temp = new int[card_index.card_index[7]];
		card_count_temp = 0;
		for (int j = 0; j < card_index.card_index[7]; j++) {
			card_temp[card_count_temp++] = card_index.card_data[7][j];
		}
		this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
		for (int j = 0; j < card_count_temp; j++) {
			card_510K[num_510K++] = card_temp[j];
		}

		card_temp = new int[card_index.card_index[10]];
		card_count_temp = 0;
		for (int j = 0; j < card_index.card_index[10]; j++) {
			card_temp[card_count_temp++] = card_index.card_data[10][j];
		}
		this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
		for (int j = 0; j < card_count_temp; j++) {
			card_510K[num_510K++] = card_temp[j];
		}
		card_index.card_index[10] = 0;
		card_index.card_index[7] = 0;
		card_index.card_index[2] = 0;
		int sort_num = 0;
		// 王牌后面
		for (int j = 0; j < card_index.card_index[14]; j++) {
			cbCardData[sort_num++] = card_index.card_data[14][j];
		}
		for (int j = 0; j < card_index.card_index[13]; j++) {
			cbCardData[sort_num++] = card_index.card_data[13][j];
		}

		// 510K牌最后
		for (int i = 0; i < num_510K; i++) {
			cbCardData[sort_num++] = card_510K[i];
		}

		// 其他牌
		for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {

			card_temp = new int[card_index.card_index[i]];
			card_count_temp = 0;
			for (int j = 0; j < card_index.card_index[i]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[i][j];
			}
			this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
			for (int j = 0; j < card_count_temp; j++) {
				cbCardData[sort_num++] = card_temp[j];
			}
		}
	}

	public void SortCardList_Tonghau(int cbCardData[], int cbCardCount) {
		int card_temp[] = new int[cbCardCount];
		int sort_num = 0;
		int color_count[] = new int[4];
		int color_index[] = new int[4];
		for (int j = 0; j < 4; j++) {
			color_index[j] = j;
			color_count[j] = 0;
		}
		for (int j = 0; j < cbCardCount; j++) {
			color_count[this.GetCardColor(cbCardData[j])]++;
			card_temp[j] = cbCardData[j];
		}
		for (int x = 3; x >= 0; x--) {
			for (int y = x - 1; y >= 0; y--) {
				if (color_count[color_index[y]] == 3 && color_count[color_index[x]] < color_count[color_index[y]]) {
					int temp = color_count[color_index[x]];
					color_count[color_index[x]] = temp;
					color_count[color_index[y]] = color_count[color_index[x]];
					temp = color_index[x];
					color_index[x] = color_index[y];
					color_index[y] = temp;
				}
			}
		}
		for (int x = 3; x >= 0; x--) {
			for (int j = 0; j < cbCardCount; j++) {
				if (GetCardColor(card_temp[j]) == color_index[x]) {
					cbCardData[sort_num++] = card_temp[j];
				}
			}
		}
	}

	public void SortCardList_Value(int cbCardData[], int cbCardCount) {
		// 转换数值
		int cbSortValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			cbSortValue[i] = GetCardLogicValue(cbCardData[i]) + this.GetCardColor(cbCardData[i]);

		}

		// 排序操作
		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1])
						|| ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
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

	public void SortCardList_Custom(int cbCardData[], int cbCardCount) {
		int card_510K[] = new int[cbCardCount];
		int card_tong_hua[] = new int[cbCardCount];
		int num_510K = 0;
		int num_tonghua = 0;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		int card_temp[] = new int[card_index.card_index[2]];
		int card_count_temp = 0;

		if (this.has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
			for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
				if (card_index.card_index[i] >= 3 && card_index.card_index[i] < 6) {
					card_temp = new int[card_index.card_index[i]];
					card_count_temp = 0;
					int count = 0;
					for (int color = 0; color < 4; color++) {
						int color_count = 0;
						for (int j = 0; j < card_index.card_index[i]; j++) {
							if (GetCardColor(card_index.card_data[i][j]) == color) {
								color_count++;
							}
						}
						if (color_count == 3) {
							for (int j = 0; j < card_index.card_index[i]; j++) {
								if (this.GetCardColor(card_index.card_data[i][j]) == color) {
									card_temp[card_count_temp++] = card_index.card_data[i][j];
								}
							}
							this.RemoveCard(card_temp, card_count_temp, card_index.card_data[i],
									card_index.card_index[i]);
							card_index.card_index[i] -= card_count_temp;
							for (int j = 0; j < card_count_temp; j++) {
								card_tong_hua[num_tonghua++] = card_temp[j];
							}
						}
					}
				}
			}
		}

		card_count_temp = 0;
		card_temp = new int[card_index.card_index[2]];
		if (card_index.card_index[2] < 6) {
			for (int j = 0; j < card_index.card_index[2]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[2][j];
			}
			this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
			for (int j = 0; j < card_count_temp; j++) {
				card_510K[num_510K++] = card_temp[j];
			}
			card_index.card_index[2] = 0;
		}

		// 510K排前面
		card_temp = new int[card_index.card_index[7]];
		card_count_temp = 0;
		if (card_index.card_index[7] < 6) {
			for (int j = 0; j < card_index.card_index[7]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[7][j];
			}
			this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
			for (int j = 0; j < card_count_temp; j++) {
				card_510K[num_510K++] = card_temp[j];
			}
			card_index.card_index[7] = 0;
		}

		card_temp = new int[card_index.card_index[10]];
		card_count_temp = 0;
		if (card_index.card_index[10] < 6) {
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[10][j];
			}
			this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
			for (int j = 0; j < card_count_temp; j++) {
				card_510K[num_510K++] = card_temp[j];
			}
			card_index.card_index[10] = 0;
		}

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

		// 王牌后面
		int sort_num = 0;
		for (int j = 0; j < card_index.card_index[14]; j++) {
			cbCardData[sort_num++] = card_index.card_data[14][j];
		}
		for (int j = 0; j < card_index.card_index[13]; j++) {
			cbCardData[sort_num++] = card_index.card_data[13][j];
		}
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (card_index.card_index[index[i]] > 5) {
				card_temp = new int[card_index.card_index[index[i]]];
				card_count_temp = 0;
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					card_temp[card_count_temp++] = card_index.card_data[index[i]][j];
				}
				this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
				for (int j = 0; j < card_count_temp; j++) {
					cbCardData[sort_num++] = card_temp[j];
				}
			}

		}
		// 同花
		for (int i = 0; i < num_tonghua; i++) {
			cbCardData[sort_num++] = card_tong_hua[i];
		}
		// 510K牌最后
		for (int i = 0; i < num_510K; i++) {
			cbCardData[sort_num++] = card_510K[i];
		}

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (card_index.card_index[index[i]] < 6) {
				card_temp = new int[card_index.card_index[index[i]]];
				card_count_temp = 0;
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					card_temp[card_count_temp++] = card_index.card_data[index[i]][j];
				}
				this.SortCardList(card_temp, card_count_temp, GameConstants.WSK_ST_TONGHUA);
				for (int j = 0; j < card_count_temp; j++) {
					cbCardData[sort_num++] = card_temp[j];
				}
			}

		}
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		if (cbSortType == GameConstants.WSK_ST_CUSTOM) {
			SortCardList_Custom(cbCardData, cbCardCount);
			return;
		} else if (cbSortType == GameConstants.WSK_ST_COUNT) {
			SortCardList_Count(cbCardData, cbCardCount);
			return;
		} else if (cbSortType == GameConstants.WSK_ST_ORDER) {
			SortCardList_Order(cbCardData, cbCardCount);
			return;
		} else if (cbSortType == GameConstants.WSK_ST_TONGHUA) {

			SortCardList_Tonghau(cbCardData, cbCardCount);
			return;
		} else {
			SortCardList_Value(cbCardData, cbCardCount);
		}

		return;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		int turn_card_type = this.GetCardType(turn_card_data, turn_card_count);

		switch (turn_card_type) {
		case GameConstants.SXTH_CT_TONG_ZHANG: {
			return search_out_card_tong_zhang(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.SXTH_CT_510K_DC: {
			return search_out_card_false_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.SXTH_CT_510K_SC: {
			return search_out_card_real_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.SXTH_CT_TONG_HUA: {
			return search_out_card_tong_hua(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.SXTH_CT_BOMB: {
			return search_out_card_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		case GameConstants.SXTH_CT_ERROR: {
			return search_out_error(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, all_tip_count);
		}
		}
		return all_tip_count;
	}

	public int search_out_error(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		for (int count = 1; count <= 5; count++) {
			for (int i = 0; i < cbCardCount;) {
				int index = this.switch_card_to_idnex(cbCardData[i]);
				if (hand_card_idnex.card_index[index] == count) {
					for (int y = 0; y < hand_card_idnex.card_index[index]; y++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][y];
					}
					all_tip_count++;
				}
				if (hand_card_idnex.card_index[index] > 0) {
					i += hand_card_idnex.card_index[index];
				} else {
					i++;
				}
			}
		}
		// 搜索炸弹
		if (has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
			all_tip_count = search_tong_hua(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		}

		// 搜索炸弹
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < cbCardCount; i++) {
			if (turn_index != 13 && turn_index != 14) {
				break;
			}
			turn_index = this.switch_card_to_idnex(turn_card_data[i]);
		}
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 12 - turn_card_count; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
				if (add_boom_count == 0) {
					if (hand_card_idnex.card_index[i] == turn_card_count && i > turn_index) {
						for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
						}
						all_tip_count++;
					}
				} else {
					if (hand_card_idnex.card_index[i] == turn_card_count + add_boom_count) {
						for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
						}
						all_tip_count++;
					}
				}

			}
		}

		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 12 - turn_card_count; add_boom_count++) {
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
					if (hand_card_idnex.card_index[i] >= _boom_count - 1) {
						if (add_boom_count == 0) {
							if (hand_card_idnex.card_index[i] + count == turn_card_count && i > turn_index) {
								for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
								}

								int use_wang = 0;
								for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
									use_wang++;
								}
								for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
									use_wang++;
								}
								all_tip_count++;
							}
						} else {
							if (hand_card_idnex.card_index[i] + count == turn_card_count + add_boom_count) {
								for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
								}
								int use_wang = 0;
								for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
									use_wang++;
								}
								for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
									if (use_wang >= count) {
										break;
									}
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
									use_wang++;
								}
								all_tip_count++;
							}
						}
					}
				}
			}
		}

		return all_tip_count;
	}

	public int search_out_card_tong_hua(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (hand_card_idnex.card_index[i] >= 3 && i > turn_index) {

				for (int color = 0; color < 4; color++) {
					int color_count = 0;
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						if (GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
							color_count++;
						}
					}
					if (color_count >= 3) {
						for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
							if (GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
							}
						}
						all_tip_count++;
					}
				}

			}
		}
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_real_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		if (has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
			all_tip_count += search_tong_hua(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		}
		all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_false_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
		// 没有510K的情况
		if (hand_card_idnex.card_index[2] <= 0 || hand_card_idnex.card_index[7] <= 0
				|| hand_card_idnex.card_index[10] <= 0) {
			if (has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
				all_tip_count += search_tong_hua(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			}
			all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			return all_tip_count;
		}
		// 有510K的情况下
		// 纯510K
		for (int color = 0; color < 4; color++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color) {
					is_five = true;
					break;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color) {
					is_ten = true;
					break;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color) {
					is_k = true;
					break;
				}
			}

			if (is_five && is_ten && is_k) {
				for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[7][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
						break;
					}
				}
				for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[10][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
						break;
					}
				}
				for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[2][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
						break;
					}
				}
				all_tip_count++;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
			all_tip_count = search_tong_hua(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		}
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);

		return all_tip_count;
	}

	public int search_out_card_tong_zhang(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);

		for (int add_count = 0; add_count < _boom_count - turn_card_count; add_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == turn_card_count + add_count && i > turn_index) {
					if (turn_card_count >= 3 && has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)
							&& i < GameConstants.WSK_MAX_INDEX - 2) {
						boolean is_tonghua = false;
						for (int color = 0; color < 4; color++) {
							int count = 0;
							for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
								if (this.GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
									count++;
								}
							}
							if (count >= 3) {
								is_tonghua = true;
								break;
							}
						}
						if (!is_tonghua) {
							for (int j = 0; j < turn_card_count; j++) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
							}
							all_tip_count++;
						}
					} else {
						for (int j = 0; j < turn_card_count; j++) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
						}
						all_tip_count++;
					}

				}
			}
		}

		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		if (has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
			all_tip_count = search_tong_hua(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		}
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_tong_hua(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		if (!has_rule(GameConstants.GAME_RULE_SXTH_TONG_HUA)) {
			return all_tip_count;
		}
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (hand_card_idnex.card_index[i] >= 3) {
				for (int color = 0; color < 4; color++) {
					int tong_hua_count = 0;
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						if (GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
							tong_hua_count++;
						}
					}
					if (tong_hua_count >= 3) {
						for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
							if (GetCardColor(hand_card_idnex.card_data[i][j]) == color) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
							}

						}
						all_tip_count++;
					}
				}

			}
		}
		return all_tip_count;
	}

	public int search_510k(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			// 510K
			for (int color_five = 0; color_five < 4; color_five++) {
				for (int color_ten = 0; color_ten < 4; color_ten++) {
					for (int color_k = 0; color_k < 4; color_k++) {
						boolean is_five = false;
						boolean is_ten = false;
						boolean is_k = false;
						for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color_five) {
								is_five = true;
								break;
							}
						}
						for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color_ten) {
								is_ten = true;
								break;
							}
						}
						for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color_k) {
								is_k = true;
								break;
							}
						}

						if (is_five && is_ten && is_k) {
							for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[10][j]) == color_k) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
									break;
								}
							}
							for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[7][j]) == color_ten) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
									break;
								}
							}
							for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[2][j]) == color_five) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
									break;
								}
							}

							all_tip_count++;
						}

					}
				}
			}
		}
		return all_tip_count;
	}

	public int search_real_510k(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			// 510K
			for (int color = 0; color < 4; color++) {
				boolean is_five = false;
				boolean is_ten = false;
				boolean is_k = false;
				for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
					if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color) {
						is_five = true;
						break;
					}
				}
				for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
					if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color) {
						is_ten = true;
						break;
					}
				}
				for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
					if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color) {
						is_k = true;
						break;
					}
				}

				if (is_five && is_ten && is_k) {
					for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
						if (GetCardColor(hand_card_idnex.card_data[10][j]) == color) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
							break;
						}
					}
					for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
						if (GetCardColor(hand_card_idnex.card_data[7][j]) == color) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
							break;
						}
					}
					for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
						if (GetCardColor(hand_card_idnex.card_data[2][j]) == color) {
							tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
							break;
						}
					}
					all_tip_count++;
				}
			}
		}
		return all_tip_count;
	}

	public int search_false_510k(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		if (hand_card_idnex.card_index[2] > 0 && hand_card_idnex.card_index[7] > 0
				&& hand_card_idnex.card_index[10] > 0) {
			// 510K
			for (int color_five = 0; color_five < 4; color_five++) {
				boolean is_color_five_five = false;
				boolean is_color_five_ten = false;
				boolean is_color_five_k = false;
				boolean is_five = false;
				for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
					if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color_five) {
						is_color_five_five = true;
						is_five = true;
						break;
					}
				}
				for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
					if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color_five) {
						is_color_five_ten = true;
						break;
					}
				}
				for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
					if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color_five) {
						is_color_five_k = true;
						break;
					}
				}
				if ((!is_color_five_five || !is_color_five_ten || !is_color_five_k) && is_five) {
					for (int color_ten = 0; color_ten < 4; color_ten++) {
						boolean is_color_ten_five = false;
						boolean is_color_ten_ten = false;
						boolean is_color_ten_k = false;
						boolean is_ten = false;
						for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color_ten) {
								is_color_ten_five = true;
								break;
							}
						}
						for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color_ten) {
								is_color_ten_ten = true;
								is_ten = true;
								break;
							}
						}
						for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
							if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color_ten) {
								is_color_ten_k = true;
								break;
							}
						}
						if ((!is_color_ten_five || !is_color_ten_ten || !is_color_ten_k) && is_ten) {

							for (int color_k = 0; color_k < 4; color_k++) {
								boolean is_color_k_five = false;
								boolean is_color_k_ten = false;
								boolean is_color_k_k = false;
								boolean is_K = false;
								for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
									if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color_k) {
										is_color_k_five = true;
										break;
									}
								}
								for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
									if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color_k) {
										is_color_k_ten = true;
										break;
									}
								}
								for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
									if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color_k) {
										is_color_k_k = true;
										is_K = true;
										break;
									}
								}

								if ((!is_color_k_five || !is_color_k_ten || !is_color_k_k) && is_K) {
									for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
										if (GetCardColor(hand_card_idnex.card_data[10][j]) == color_k) {
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
											break;
										}
									}
									for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
										if (GetCardColor(hand_card_idnex.card_data[7][j]) == color_ten) {
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
											break;
										}
									}
									for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
										if (GetCardColor(hand_card_idnex.card_data[2][j]) == color_five) {
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
											break;
										}
									}

									all_tip_count++;
									return all_tip_count;
								}

							}
						}

					}
				}

			}
		}
		return all_tip_count;
	}

	public int search_boom(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 12 - 6; add_boom_count++) {
			for (int i = 0; i < cbCardCount;) {
				int index = this.switch_card_to_idnex(cbCardData[i]);
				if (hand_card_idnex.card_index[index] == 6 + add_boom_count) {
					for (int j = 0; j < hand_card_idnex.card_index[index]; j++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][j];
					}
					all_tip_count++;
				}
				if (hand_card_idnex.card_index[index] > 0) {
					i += hand_card_idnex.card_index[index];
				} else {
					i++;
				}
			}
		}
		// 带王的炸弹
		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 12 - _boom_count; add_boom_count++) {
				for (int i = 0; i < cbCardCount;) {
					int index = this.switch_card_to_idnex(cbCardData[i]);
					if (hand_card_idnex.card_index[index] >= _boom_count - 1) {
						if (hand_card_idnex.card_index[index] + count == _boom_count + add_boom_count) {
							int use_wang = 0;
							for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
								if (use_wang >= count) {
									break;
								}
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
								use_wang++;
							}
							for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
								if (use_wang >= count) {
									break;
								}
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
								use_wang++;
							}
							for (int j = 0; j < hand_card_idnex.card_index[index]; j++) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[index][j];
							}

							all_tip_count++;
						}
					}

					if (hand_card_idnex.card_index[index] > 0) {
						i += hand_card_idnex.card_index[index];
					} else {
						i++;
					}
				}
			}
		}
		return all_tip_count;
	}

}
