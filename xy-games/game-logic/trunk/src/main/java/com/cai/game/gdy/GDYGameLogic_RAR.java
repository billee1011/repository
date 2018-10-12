package com.cai.game.gdy;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.gdy.data.tagAnalyseIndexResult_GDY;

public class GDYGameLogic_RAR extends GDYGameLogic {

	public int get_magic_card_count(int card_index[]) {
		return card_index[13] + card_index[14];
	}

	public boolean is_have_card(int cbCardData[], int cbMagicCardData[], int cbCardCount) {
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int magic_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);
		switch_to_card_index(cbMagicCardData, cbCardCount, magic_index);

		int magic_count = card_index[0] + card_index[13] + card_index[14];
		int index = this.get_card_index(cbMagicCardData[cbCardCount - 1]);
		for (int i = index; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
			if (magic_index[i] > 0 && card_index[i] < magic_index[i]) {
				if (magic_index[i] - card_index[i] <= magic_count) {
					magic_count -= magic_index[i] - card_index[i];
				} else {
					return false;
				}
			} else {
				if (i == 0) {
					magic_count -= magic_index[i];
				}
			}

		}
		if (magic_count != 0) {
			return false;
		}

		return true;
	}

	// 获取类型
	public int GetCardType_GDY(int cbCardData[], int change_out_card_data[], int cbCardCount) {

		tagAnalyseIndexResult_GDY card_index = new tagAnalyseIndexResult_GDY();
		tagAnalyseIndexResult_GDY change_card_index = new tagAnalyseIndexResult_GDY();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		AnalysebCardDataToIndex(change_out_card_data, cbCardCount, change_card_index);

		int magic_count = card_index.card_index[13] + card_index.card_index[14] + card_index.card_index[0];
		if (change_card_index.card_index[13] + change_card_index.card_index[14] == cbCardCount && cbCardCount == 2) {
			return GameConstants.GDY_CT_KING_BOMB_RAR;
		}
		if (change_card_index.card_index[0] == cbCardCount && cbCardCount == 4) {
			return GameConstants.GDY_CT_MAGIC_BOMB_RAR;
		}
		if (card_index.card_index[0] > 0 && card_index.card_index[13] + card_index.card_index[14] > 0
				&& card_index.card_index[13] + card_index.card_index[14] + card_index.card_index[0] == cbCardCount) {
			return GameConstants.GDY_CT_ERROR_RAR;
		}
		if (card_index.card_index[13] + card_index.card_index[14] == cbCardCount && cbCardCount == 1) {
			return GameConstants.GDY_CT_SINGLE_RAR;
		}
		int index = this.get_card_index(change_out_card_data[0]);

		if (card_index.card_index[0] == cbCardCount) {
			if (cbCardCount == 1) {
				return GameConstants.GDY_CT_SINGLE_RAR;
			} else if (cbCardCount == 2) {
				return GameConstants.GDY_CT_DOUBLE_RAR;
			} else if (cbCardCount == 3) {
				return GameConstants.GDY_CT_THREE_RAR;
			} else if (cbCardCount == 4) {
				return GameConstants.GDY_CT_MAGIC_BOMB_RAR;
			}
		}

		if (change_card_index.card_index[index] + change_card_index.card_index[13] + change_card_index.card_index[14]
				+ change_card_index.card_index[0] == cbCardCount) {
			if (cbCardCount == 1) {
				return GameConstants.GDY_CT_SINGLE_RAR;
			} else if (cbCardCount == 2) {
				return GameConstants.GDY_CT_DOUBLE_RAR;
			} else if (cbCardCount == 3) {
				return GameConstants.GDY_CT_THREE_RAR;
			} else if (cbCardCount == 4) {
				if (magic_count == 0) {
					return GameConstants.GDY_CT_BOMB_RAR;
				} else {
					return GameConstants.GDY_CT_RUAN_BOMB_RAR;
				}
			} else if (cbCardCount == 5) {
				return GameConstants.GDY_CT_THREE_TAKE_TWO_RAR;
			} else {
				return GameConstants.GDY_CT_ERROR_RAR;
			}
		}

		if (this.has_rule(GDYConstants.GAME_RULE_GDY_THREE_TAKE_DOUBLE)) {
			if (cbCardCount == 5) {
				boolean is_three = false;
				boolean is_double = false;
				for (int i = 0; i < cbCardCount; i++) {
					if (change_card_index.card_index[get_card_index(change_out_card_data[i])] == 3) {
						is_three = true;
					}
					if (change_card_index.card_index[get_card_index(change_out_card_data[i])] == 2) {
						is_double = true;
					}
				}
				if (is_double && is_three) {
					return GameConstants.GDY_CT_THREE_TAKE_TWO_RAR;
				} else if (change_card_index.card_index[get_card_index(change_out_card_data[0])] == 5) {
					return GameConstants.GDY_CT_THREE_TAKE_TWO_RAR;
				}
			}
		}

		if (cbCardCount >= 3) {
			if (this.is_link(change_card_index, 1, cbCardCount)) {
				return GameConstants.GDY_CT_SINGLE_LINK_RAR;
			}
		}
		if (cbCardCount >= 4) {
			if (this.is_link(change_card_index, 2, cbCardCount / 2)) {
				return GameConstants.GDY_CT_DOUBLE_LINK_RAR;
			}
		}
		if (cbCardCount >= 6 && cbCardCount <= 9) {
			if (this.is_link(change_card_index, 3, cbCardCount / 3)) {
				return GameConstants.GDY_CT_THREE_LINK_RAR;
			}
		}
		if (cbCardCount == 10 && this.has_rule(GDYConstants.GAME_RULE_GDY_THREE_TAKE_DOUBLE)) {
			if (this.is_plane_take_double(change_card_index, change_out_card_data, cbCardCount) > 0) {
				return GameConstants.GDY_CT_PLANE_TAKE_TWO_RAR;
			}

		}
		return GameConstants.GDY_CT_ERROR_RAR;
	}

	public boolean comparecarddata(int first_card[], int first_change_card[], int first_count, int next_card[],
			int next_change_card[], int next_count) {
		tagAnalyseIndexResult_GDY first_index = new tagAnalyseIndexResult_GDY();
		tagAnalyseIndexResult_GDY next_index = new tagAnalyseIndexResult_GDY();
		AnalysebCardDataToIndex(first_change_card, first_count, first_index);
		AnalysebCardDataToIndex(next_change_card, next_count, next_index);

		int first_card_type = GetCardType_GDY(first_card, first_change_card, first_count);
		int next_card_type = GetCardType_GDY(next_card, next_change_card, next_count);
		if (next_card_type == GameConstants.GDY_CT_KING_BOMB_RAR) {
			return false;
		}
		if (first_card_type == GameConstants.GDY_CT_KING_BOMB_RAR) {
			return true;
		}
		if (next_card_type == GameConstants.GDY_CT_MAGIC_BOMB_RAR) {
			return false;
		}
		if (first_card_type == GameConstants.GDY_CT_MAGIC_BOMB_RAR) {
			return true;
		}

		if (next_card_type >= GameConstants.GDY_CT_RUAN_BOMB_RAR
				&& first_card_type < GameConstants.GDY_CT_RUAN_BOMB_RAR) {
			return false;
		}
		if (next_card_type < GameConstants.GDY_CT_RUAN_BOMB_RAR
				&& first_card_type >= GameConstants.GDY_CT_RUAN_BOMB_RAR) {
			return true;
		}
		if (next_card_type >= GameConstants.GDY_CT_RUAN_BOMB_RAR
				&& first_card_type >= GameConstants.GDY_CT_RUAN_BOMB_RAR) {
			if (next_card_type != first_card_type) {
				return first_card_type > next_card_type;
			} else {
				return this.GetCardLogicValue(first_change_card[0]) > this.GetCardLogicValue(next_change_card[0]);
			}
		}
		if (next_card_type != first_card_type) {
			return false;
		}
		if (first_count != next_count) {
			return false;
		}
		switch (first_card_type) {
		case GameConstants.GDY_CT_SINGLE_RAR:
		case GameConstants.GDY_CT_DOUBLE_RAR:
		case GameConstants.GDY_CT_THREE_RAR: {
			int first_value = this.GetCardLogicValue(first_change_card[0]);
			int next_value = this.GetCardLogicValue(next_change_card[0]);
			if (first_value == 15 && next_value != 15) {
				return true;
			}
			return first_value - 1 == next_value;
		}
		case GameConstants.GDY_CT_SINGLE_LINK_RAR:
		case GameConstants.GDY_CT_DOUBLE_LINK_RAR:
		case GameConstants.GDY_CT_THREE_LINK_RAR: {
			int first_value = this.GetCardLogicValue(first_change_card[0]);
			int next_value = this.GetCardLogicValue(next_change_card[0]);
			if (first_value == 15 && next_value != 15) {
				return true;
			}
			return first_value - 1 == next_value;
		}
		case GameConstants.GDY_CT_THREE_TAKE_TWO_RAR: {
			int index_first = -1;
			int index_next = -1;
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (first_index.card_index[i] == 3 || first_index.card_index[i] == 5) {
					index_first = i;
				}
				if (next_index.card_index[i] == 3 || next_index.card_index[i] == 5) {
					index_next = i;
				}
			}
			if (index_first == 12 && index_next != 12) {
				return true;
			}
			return index_first - 1 == index_next;
		}
		case GameConstants.GDY_CT_PLANE_TAKE_TWO_RAR: {
			int first_Type_index1 = get_plane_max_index(first_index, first_change_card, first_count);
			int cbNextType_index1 = get_plane_max_index(next_index, next_change_card, next_count);
			return first_Type_index1 - 1 == cbNextType_index1;
		}
		}

		return false;
	}

	public boolean search_card_data(int card_data[], int change_card_data[], int cardCount, int hand_card_data[],
			int hand_card_count) {
		int card_type = this.GetCardType_GDY(card_data, change_card_data, cardCount);
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int hand_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(change_card_data, cardCount, card_index);
		switch_to_card_index(hand_card_data, hand_card_count, hand_index);
		int magic_count = hand_index[13] + hand_index[14] + hand_index[0];
		if (card_type == GameConstants.GDY_CT_KING_BOMB_RAR) {
			return false;
		}
		if (hand_index[13] > 0 && hand_index[14] > 0) {
			return true;
		}
		switch (card_type) {
		case GameConstants.GDY_CT_SINGLE_RAR: {
			int index = get_card_index(change_card_data[0]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}
			if (this.GetCardValue(change_card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] >= 1 || hand_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_DOUBLE_RAR: {
			int index = get_card_index(change_card_data[0]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}
			if (this.GetCardValue(change_card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] + magic_count >= 2
					|| hand_index[GameConstants.GDY_MAX_INDEX - 3] + magic_count > 1) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_THREE_RAR: {
			int index = get_card_index(change_card_data[0]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}
			if (this.GetCardValue(change_card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] + magic_count >= 3
					|| hand_index[GameConstants.GDY_MAX_INDEX - 3] + magic_count >= 3) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_SINGLE_LINK_RAR: {
			int index = get_card_index(change_card_data[cardCount - 1]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			magic_count_temp = magic_count;
			for (int i = index + 1; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
				if (hand_index[i] == 0) {
					if (magic_count_temp == 0) {
						break;
					} else {
						magic_count_temp--;
						if (i - index >= cardCount) {
							return true;
						}
					}
				}
				if (i - index >= cardCount) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_DOUBLE_LINK_RAR: {
			int index = get_card_index(change_card_data[cardCount - 1]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			for (int i = index + 1; i < (GameConstants.GDY_MAX_INDEX - 3); i++) {
				if (hand_index[i] < 2) {
					if (magic_count_temp + hand_index[i] < 2) {
						break;
					} else {
						magic_count_temp -= 2 - hand_index[i];
						if (index - i >= cardCount / 2) {
							return true;
						}
					}

				}
				if (i - index >= cardCount / 2) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_THREE_LINK_RAR: {
			int index = get_card_index(change_card_data[cardCount - 1]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			for (int i = index + 1; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
				if (hand_index[i] < 2) {
					if (magic_count_temp + hand_index[i] < 3) {

						break;
					} else {
						magic_count_temp = 3 - hand_index[i];
						if (index - i >= cardCount) {
							return true;
						}
					}

				}
				if (i - index >= cardCount / 3) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_THREE_TAKE_TWO_RAR: {
			int index = get_card_index(change_card_data[(cardCount / 5) * 3]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}

			int magic_count_temp = magic_count;
			if (hand_index[index + 1] + magic_count_temp == 3) {
				magic_count_temp -= 3 - hand_index[index + 1];
				for (int j = 1; j <= GameConstants.GDY_MAX_INDEX - 3; j++) {
					if (j != index + 1) {
						if (hand_index[j] + magic_count_temp >= 2) {
							return true;
						}
					}
				}

			} else if (hand_index[12] + magic_count_temp == 3) {
				magic_count_temp -= 3 - hand_index[12];
				for (int j = 1; j <= GameConstants.GDY_MAX_INDEX - 3; j++) {
					if (j != 12) {
						if (hand_index[j] + magic_count_temp >= 2) {
							return true;
						}
					}
				}
			}

			return false;
		}
		case GameConstants.GDY_CT_PLANE_TAKE_TWO_RAR: {
			int index = get_card_index(change_card_data[(cardCount / 5) * 3]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
			}

			int magic_count_temp = magic_count;
			for (int i = index + 1; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
				if (hand_index[i] < 2) {
					if (magic_count_temp + hand_index[i] < 3) {
						if (index - i >= cardCount / 5) {
							int double_count = 0;
							for (int j = 1; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
								if (j < index + 1 || j > i) {
									if (hand_index[j] >= 2) {
										double_count++;
									} else if (hand_index[j] + magic_count_temp >= 2) {
										double_count++;
										magic_count_temp -= 2 - hand_index[j];
									}
									if (double_count == cardCount / 5) {
										return true;
									}
								}
							}
						}
						break;
					} else {
						magic_count_temp = 3 - hand_index[i];
					}

				}
				if (i - index > cardCount / 5) {
					int double_count = 0;
					for (int j = 1; j <= GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (j < index + 1 || j > i) {
							if (hand_index[j] >= 2) {
								double_count++;
							} else if (hand_index[j] + magic_count_temp >= 2) {
								double_count++;
								magic_count_temp -= 2 - hand_index[j];
							}
							if (double_count == cardCount / 5) {
								return true;
							}
						}
					}
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_RUAN_BOMB_RAR: {
			int index = get_card_index(change_card_data[0]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3 && i > index) {
					return true;
				}
				if (hand_index[i] > 3) {
					return true;
				}
			}
		}
		case GameConstants.GDY_CT_BOMB_RAR: {
			int index = get_card_index(change_card_data[0]);
			if (hand_index[0] == 4) {
				return true;
			}
			for (int i = 1; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] > 3 && i > index) {
					return true;
				}
			}
		}
		}
		return false;
	}

	public boolean isAllMagic(int cbCardData[], int cbCardCount) {
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);
		if (cbCardCount == this.get_magic_card_count(card_index)) {
			return true;
		}
		return false;
	}

	/////////////////////////////////////////////////////////////////////////// 湖北干瞪眼

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

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		switch (type) {
		case GameConstants.GDY_CT_THREE_TAKE_TWO_RAR: {
			tagAnalyseIndexResult_GDY card_data_index = new tagAnalyseIndexResult_GDY();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.get_card_index(card_date[i]);
				if (card_data_index.card_index[index] >= 3) {
					for (int j = 0; j < card_data_index.card_index[index]; j++) {
						card_date[count++] = card_data_index.card_data[index][j];
					}
					break;
				}
			}
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (card_data_index.card_index[i] < 3) {
					for (int j = 0; j < card_data_index.card_index[i]; j++) {
						card_date[count++] = card_data_index.card_data[i][j];
					}
				}
			}
			return;
		}
		case GameConstants.GDY_CT_PLANE_TAKE_TWO_RAR: {
			tagAnalyseIndexResult_GDY card_data_index = new tagAnalyseIndexResult_GDY();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
				if (card_data_index.card_index[i] >= 3) {
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							if ((i - prv_index) + 1 == card_count / 5) {
								for (int x = j; x <= i; x++) {
									for (int y = 0; y < 3; y++) {
										int card_temp[] = new int[1];
										card_temp[0] = card_data_index.card_data[x][0];
										card_date[count] = card_temp[0];
										this.remove_cards_by_data(card_data_index.card_data[x],
												card_data_index.card_index[x], card_temp, 1);
										card_data_index.card_index[x]--;
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

			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				for (int j = 0; j < card_data_index.card_index[i]; j++) {
					card_date[count++] = card_data_index.card_data[i][j];
				}
			}
			return;
		}

		}
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

}
