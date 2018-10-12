package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.gfWsk.gfWskRsp.UserCardData;

public class WSKGameLogic_PXGT_TWO extends WSKGameLogic {

	private static final int VALUE_SINGLE = 1;
	private static final int VALUE_DOUBLE = 2;
	private static final int VALUE_SINGLE_LINK = 3;
	private static final int VALUE_DOUBLE_LINK = 4;
	private static final int VALUE_THREE_TAKE_ONE = 5;
	private static final int VALUE_PLANE = 6;
	private static final int VALUE_510K_DC = 7;
	private static final int VALUE_510K_SC = 8;
	private static final int VALUE_GUN_TONG_DC = 9;
	private static final int VALUE_GUN_TONG_SC = 10;
	private static final int VALUE_BOMB = 11;

	public WSKGameLogic_PXGT_TWO() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount, boolean isLast) {
		if (cbCardCount == 0) {
			return WSKConstants.PXGT_CT_ERROR;
		}
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		if (card_index.card_index[this.switch_card_to_idnex(cbCardData[cbCardCount - 1])] == cbCardCount
				&& cbCardCount != 3) {
			// 所有牌都为同一种牌
			if (cbCardCount == 1) {
				return WSKConstants.PXGT_CT_SINGLE;
			}
			if (cbCardCount == 2) {
				return WSKConstants.PXGT_CT_DOUBLE;
			}
			// if (cbCardCount == 3) {
			if (cbCardCount >= 4) {
				return WSKConstants.PXGT_CT_BOMB;
			}

			return WSKConstants.PXGT_CT_ERROR;
		}

		if (cbCardCount == 3) {
			// 三张
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 3) {
				return WSKConstants.PXGT_CT_THREE;
			}
			// 510K
			if (card_index.card_index[2] > 0 && card_index.card_index[7] > 0 && card_index.card_index[10] > 0) {
				int color = this.GetCardColor(cbCardData[0]);
				for (int i = 1; i < cbCardCount; i++) {
					if (GetCardColor(cbCardData[i]) != color) {
						return WSKConstants.PXGT_CT_510K_DC;
					}
				}
				return WSKConstants.PXGT_CT_510K_SC;
			}
			return WSKConstants.PXGT_CT_ERROR;
		}
		if (cbCardCount == 4) {
			// 三带一
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3) {
					return WSKConstants.PXGT_CT_THREE_TAKE_TWO;
				}
			}
		}
		if (cbCardCount == 5) {
			// 三带二
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3
						|| card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] >= 3) {
					return WSKConstants.PXGT_CT_THREE_TAKE_TWO;
				}
			}
		}

		// 顺子
		if (cbCardCount >= 5) {
			if (this.is_link_other(cbCardData, cbCardCount, 1, cbCardCount)) {
				return WSKConstants.PXGT_CT_SINGLE_LINK;
			}
			if (this.is_link_other(cbCardData, cbCardCount, 2, cbCardCount / 2)) {
				return WSKConstants.PXGT_CT_DOUBLE_LINK;
			}
		}

		// 飞机
		int nPlane = is_plane_other(card_index, cbCardData, cbCardCount);
		if (nPlane == 0) {
			return WSKConstants.PXGT_CT_PLANE;
		} else if (nPlane == 1) {
			return WSKConstants.PXGT_CT_PLANE;
		}
		// 三连
		if (cbCardCount % 3 == 0) {
			if (this.is_link_other(cbCardData, cbCardCount, 3, cbCardCount / 3)) {
				return WSKConstants.PXGT_CT_PLANE;
			}
		}

		if (card_index.card_index[2] > 1 && card_index.card_index[2] < 6
				&& card_index.card_index[2] == card_index.card_index[7]
				&& card_index.card_index[7] == card_index.card_index[10]
				&& card_index.card_index[2] + card_index.card_index[7] + card_index.card_index[10] == cbCardCount) {
			for (int i = 0; i < card_index.card_index[2]; i++) {
				for (int j = 0; j < card_index.card_index[7]; j++) {
					if (this.GetCardColor(card_index.card_data[2][i]) == this
							.GetCardColor(card_index.card_data[7][j])) {

						for (int x = 0; x < card_index.card_index[10]; x++) {
							if (this.GetCardColor(card_index.card_data[7][j]) == this
									.GetCardColor(card_index.card_data[10][x])) {
								card_index.card_data[2][i] = 0;
								card_index.card_data[7][j] = 0;
								card_index.card_data[10][x] = 0;
								break;
							}
							if (x == card_index.card_index[10] - 1) {
								return WSKConstants.PXGT_CT_GUN_TONG_DC;
							}
						}
						break;
					}
					if (j == card_index.card_index[7] - 1) {
						return WSKConstants.PXGT_CT_GUN_TONG_DC;
					}
				}
			}
			return WSKConstants.PXGT_CT_GUN_TONG_SC;
		}
		return WSKConstants.PXGT_CT_ERROR;
	}

	public int get_three_link_count(int cbCardData[], int cbCardCount, int type) {
		tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_data_index);
		if (type == GameConstants.WSK_GF_CT_PLANE_LOST) {
			if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] >= 3) {
				for (int i = 13; i >= 1; i--) {
					int idnex = this.switch_card_to_idnex(i);
					if (card_data_index.card_index[idnex] >= 3) {
						int link_num = 1;
						int prv_index = i;
						for (int j = i - 1; j >= 1; j--) {
							int other_index = this.switch_card_to_idnex(j);
							if (card_data_index.card_index[other_index] >= 3 && prv_index == j + 1) {
								prv_index = j;
								link_num++;
							} else {
								if (link_num > 1 && link_num * 5 >= cbCardCount) {
									return link_num;
								}
							}
							if (j == 1) {
								if (link_num > 1 && link_num * 5 >= cbCardCount) {
									return link_num;
								}
							}
						}
					}
				}
			} else {
				for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
					// 三个2不能当做飞机
					if (card_data_index.card_index[i] >= 3) {
						int link_num = 1;
						int prv_index = i;
						for (int j = i - 1; j >= 0; j--) {
							if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
								prv_index = j;
								link_num++;
							} else {
								if (link_num > 1 && link_num * 5 >= cbCardCount) {
									return link_num;
								}
							}
							if (j == 0) {
								if (link_num > 1 && link_num * 5 >= cbCardCount) {
									return link_num;
								}
							}
						}
					}
				}
			}

		} else {
			if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] >= 3) {
				for (int i = 13; i >= 1; i--) {
					int idnex = this.switch_card_to_idnex(i);
					if (card_data_index.card_index[idnex] >= 3) {
						int link_num = 1;
						int prv_index = i;
						for (int j = i - 1; j >= 1; j--) {
							int other_index = this.switch_card_to_idnex(j);
							if (card_data_index.card_index[other_index] >= 3 && prv_index == j + 1) {
								prv_index = j;
								link_num++;
								if (link_num * 5 == cbCardCount) {
									return link_num;
								}
							}
							if (j == 1) {
								if (link_num > 1 && link_num * 5 == cbCardCount) {
									return link_num;
								}
							}
						}
					}
				}
			} else {
				for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
					// 三个2不能当做飞机
					if (card_data_index.card_index[i] >= 3) {
						int link_num = 1;
						int prv_index = i;
						for (int j = i - 1; j >= 0; j--) {
							if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
								prv_index = j;
								link_num++;
								if (link_num * 5 == cbCardCount) {
									return link_num;
								}
							}
							if (j == 0) {
								if (link_num > 1 && link_num * 5 == cbCardCount) {
									return link_num;
								}
							}
						}
					}
				}
			}

		}
		return 0;
	}

	// 飞机 0飞机缺翅膀 1飞机
	@Override
	public int get_plane_max_index(tagAnalyseIndexResult_WSK card_data_index, int cbCardData[], int cbCardCount,
			int three_link_count) {
		for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num == three_link_count) {
							return i;
						}
					} else {
						break;
					}
				}
			}
		}

		return -1;
	}

	public int get_type_value(int type, int cbCardCount) {
		int value = 0;
		switch (type) {
		case WSKConstants.PXGT_CT_SINGLE: {
			value = VALUE_SINGLE;
			break;
		}
		case WSKConstants.PXGT_CT_DOUBLE: {
			value = VALUE_DOUBLE;
			break;
		}
		case WSKConstants.PXGT_CT_SINGLE_LINK: {
			value = VALUE_SINGLE_LINK;
			break;
		}
		case WSKConstants.PXGT_CT_DOUBLE_LINK: {
			value = VALUE_DOUBLE_LINK;
			break;
		}
		case WSKConstants.PXGT_CT_THREE:
		case WSKConstants.PXGT_CT_THREE_TAKE_TWO:
		case WSKConstants.PXGT_CT_FOUR_TAKE_ONE: {
			value = VALUE_THREE_TAKE_ONE;
			break;
		}
		case WSKConstants.PXGT_CT_PLANE: {
			value = VALUE_PLANE;
			break;
		}
		case WSKConstants.PXGT_CT_510K_DC: {
			value = VALUE_510K_DC;
			break;
		}
		case WSKConstants.PXGT_CT_510K_SC: {
			value = VALUE_510K_SC;
			break;
		}
		case WSKConstants.PXGT_CT_GUN_TONG_DC: {
			value = VALUE_510K_SC + 2 + (cbCardCount / 3 - 2) * 3;
			break;
		}
		case WSKConstants.PXGT_CT_GUN_TONG_SC: {
			value = VALUE_510K_SC + 3 + (cbCardCount / 3 - 2) * 3;
			break;
		}
		case WSKConstants.PXGT_CT_BOMB: {
			value = VALUE_510K_SC + 1 + (cbCardCount - 4) * 3;
			break;
		}
		}
		return value;
	}

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,
			int three_link_count) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		int next_type_value = get_type_value(cbNextType, cbNextCount);
		int first_type_value = get_type_value(cbFirstType, cbFirstCount);
		// 炸弹以上一定大于单牌、对子和单龙
		if (next_type_value >= VALUE_510K_DC && first_type_value < VALUE_510K_DC)
			return true;
		if (next_type_value < VALUE_510K_DC && first_type_value >= VALUE_510K_DC) {
			return false;
		}
		if (next_type_value >= VALUE_510K_DC && first_type_value >= VALUE_510K_DC) {
			if (next_type_value == first_type_value) {
				if (cbNextType == WSKConstants.PXGT_CT_BOMB) {
					return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
				} else {
					return false;
				}
			} else {
				return next_type_value > first_type_value;
			}

		}
		tagAnalyseIndexResult_WSK next_card_index = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK first_card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
		AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
		switch (first_type_value) {
		case VALUE_SINGLE:
		case VALUE_DOUBLE: {
			if (first_type_value != next_type_value) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case VALUE_SINGLE_LINK:
		case VALUE_DOUBLE_LINK: {
			if (cbFirstType != cbNextType || cbFirstCount != cbNextCount) {
				return false;
			}
			if (first_card_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0
					&& next_card_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
				return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
			} else if (first_card_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0
					&& next_card_index.card_index[GameConstants.WSK_MAX_INDEX - 3] == 0) {
				return true;
			} else if (first_card_index.card_index[GameConstants.WSK_MAX_INDEX - 3] == 0
					&& next_card_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case VALUE_PLANE: {
			if (first_type_value != next_type_value) {
				return false;
			}
			if (three_link_count * 5 < cbNextCount) {
				return false;
			}
			for (int i = 0; i < three_link_count * 3; i++) {
				if (this.GetCardValue(cbNextCard[i]) == 2 || this.GetCardValue(cbFirstCard[i]) == 2) {
					return GetCardValue(cbNextCard[0]) > GetCardValue(cbFirstCard[0]);
				}
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case VALUE_THREE_TAKE_ONE: {
			if (first_type_value != next_type_value) {
				return false;
			}
			int next_index = -1;
			int first_index = -1;
			for (int i = 0; i < cbNextCount; i++) {
				if (next_index == -1 && next_card_index.card_index[this.switch_card_to_idnex(cbNextCard[i])] >= 3) {
					next_index = this.switch_card_to_idnex(cbNextCard[i]);
				}

			}
			for (int i = 0; i < cbFirstCount; i++) {
				if (first_index == -1 && first_card_index.card_index[this.switch_card_to_idnex(cbFirstCard[i])] >= 3) {
					first_index = this.switch_card_to_idnex(cbFirstCard[i]);
				}
			}
			return next_index > first_index;
		}
		}
		return false;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int three_link_count, int tip_out_card[][], int tip_out_count[], int tip_type_count) {
		int count = 0;
		int turn_card_type = this.GetCardType(turn_card_data, turn_card_count);

		switch (turn_card_type) {
		case WSKConstants.PXGT_CT_SINGLE:
		case WSKConstants.PXGT_CT_DOUBLE: {
			tip_type_count += search_out_single_double(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_SINGLE_LINK: {
			tip_type_count += search_out_card_single_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_DOUBLE_LINK: {
			tip_type_count += search_out_card_double_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_THREE:
		case WSKConstants.PXGT_CT_THREE_TAKE_TWO: {
			tip_type_count += search_out_card_three(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_PLANE: {
			tip_type_count += search_out_card_plane(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					three_link_count, tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_510K_SC: {
			tip_type_count += search_out_card_real_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_510K_DC: {
			tip_type_count += search_out_card_false_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_BOMB: {
			tip_type_count += search_out_card_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_GUN_TONG_DC: {
			tip_type_count += search_out_card_false_gun_tong(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.PXGT_CT_GUN_TONG_SC: {
			tip_type_count += search_out_card_real_tuntong(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		}

		return tip_type_count;
	}

	public int search_out_card_real_tuntong(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count, int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK turn_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, turn_card_idnex);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int min_boom_count = 5;
		if (turn_card_idnex.card_index[2] >= 3) {
			min_boom_count = 6;
		} else if (turn_card_idnex.card_index[2] >= 4) {
			min_boom_count = 7;
		} else if (turn_card_idnex.card_index[2] >= 5) {
			min_boom_count = 8;
		}
		// 搜索炸弹
		for (int add_boom_count = min_boom_count - 4; add_boom_count <= 8 - 4; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == 4 + add_boom_count) {
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
					}
					all_tip_count++;
				}
			}
		}
		return all_tip_count;
	}

	public int search_out_card_false_gun_tong(int cbCardData[], int cbCardCount, int turn_card_data[],
			int turn_card_count, int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK turn_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, turn_card_idnex);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		// 有510K的情况下
		// 纯510K
		int color_count = 0;
		int color_record[] = new int[4];
		for (int color = 0; color < 4; color++) {
			boolean is_five = false;
			boolean is_ten = false;
			boolean is_k = false;
			for (int i = 0; i < hand_card_idnex.card_index[2]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[2][i]) == color) {
					is_five = true;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color) {
					is_ten = true;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color) {
					is_k = true;
				}
			}
			if (is_five && is_ten && is_k) {
				color_record[color_count++] = color;
			}

		}
		if (color_count >= turn_card_count / 3) {
			for (int color = 0; color < color_count; color++) {
				for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[2][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
						break;
					}
				}
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
				all_tip_count++;
			}
		}

		int min_boom_count = 5;
		if (turn_card_idnex.card_index[2] >= 3) {
			min_boom_count = 6;
		} else if (turn_card_idnex.card_index[2] >= 4) {
			min_boom_count = 7;
		} else if (turn_card_idnex.card_index[2] >= 5) {
			min_boom_count = 8;
		}
		// 搜索炸弹
		for (int add_boom_count = min_boom_count - 4; add_boom_count <= 8 - 4; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == 4 + add_boom_count) {
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
					}
					all_tip_count++;
				}
			}
		}

		return all_tip_count;
	}

	public int search_out_error(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		for (int count = 1; count <= 8; count++) {
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
		all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_single_double(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (hand_card_idnex.card_index[i] >= turn_card_count && hand_card_idnex.card_index[i] < 4
					&& i > turn_index) {
				for (int y = 0; y < turn_card_count; y++) {
					tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][y];
				}
				all_tip_count++;
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_three(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < turn_card_count;) {
			int index = switch_card_to_idnex(turn_card_data[i]);
			if (card_index.card_index[index] >= 3) {
				turn_index = index;
				break;
			}
			if (card_index.card_index[index] > 0) {
				i += card_index.card_index[index];
			} else {
				i++;
			}
		}
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (hand_card_idnex.card_index[i] == 3 && i > turn_index) {
				int take_count = 0;
				for (int index_num = 1; index_num < 4; index_num++) {
					for (int take_index = 0; take_index < GameConstants.WSK_MAX_INDEX; take_index++) {
						if (take_count >= 2) {
							break;
						}
						if (take_index != i && hand_card_idnex.card_index[take_index] == index_num) {
							for (int y = 0; y < hand_card_idnex.card_index[take_index]; y++) {
								tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[take_index][y];
								take_count++;
								if (take_count >= 2) {
									break;
								}
							}
						}

					}
				}

				for (int y = 0; y < hand_card_idnex.card_index[i]; y++) {
					tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][y];
				}
				all_tip_count++;
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_single_link(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_index.card_index[i] > 0) {
				turn_index = i;
				break;
			}
		}
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
			if (hand_card_idnex.card_index[i] >= 1 && hand_card_idnex.card_index[i] < 4 && i > turn_index && i != 12) {
				for (int j = i + 1; j < GameConstants.WSK_MAX_INDEX - 3; j++) {
					if (hand_card_idnex.card_index[j] >= 1 && hand_card_idnex.card_index[j] < 4) {
						if ((j - i) + 1 == turn_card_count) {
							for (int x = j; x >= i; x--) {
								for (int y = 0; y < 1; y++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
								}
							}
							all_tip_count++;
							break;
						}
					} else {
						break;
					}
				}
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_double_link(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_index.card_index[i] > 0) {
				turn_index = i;
				break;
			}
		}
		// 不拆炸弹
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 3; i++) {
			if (hand_card_idnex.card_index[i] >= 2 && hand_card_idnex.card_index[i] < 4 && i > turn_index) {
				for (int j = i + 1; j < GameConstants.WSK_MAX_INDEX - 3; j++) {
					if (hand_card_idnex.card_index[j] >= 2 && hand_card_idnex.card_index[j] < 4) {
						if ((j - i) + 1 == turn_card_count / 2) {
							for (int x = j; x >= i; x--) {
								for (int y = 0; y < 2; y++) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
								}
							}
							all_tip_count++;
							break;
						}
					} else {
						break;
					}
				}
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		return all_tip_count;
	}

	public int search_out_card_plane(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int three_link_count, int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];

		// 找出飞机的最大值
		int turn_index = -1;
		for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {

			if (card_index.card_index[i] >= 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] >= 3) {
						if ((i - j) + 1 == three_link_count) {
							turn_index = i;
							break;
						}
					}
				}
			}
			if (turn_index != -1) {
				break;
			}
		}
		// 搜索飞机
		for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {

			if (cbCardCount > three_link_count * 5) {
				// 如果牌还够的情况下，先不考虑拆炸弹
				if (hand_card_idnex.card_index[i] == 3 && i > turn_index) {
					for (int j = i - 1; j >= 0; j--) {
						if (hand_card_idnex.card_index[j] == 3) {
							if ((i - j) + 1 == three_link_count) {
								int take_count = 0;
								for (int index_num = 1; index_num < 4; index_num++) {
									for (int take_index = 0; take_index < GameConstants.WSK_MAX_INDEX; take_index++) {
										if (take_index < j || take_index > i) {
											if (hand_card_idnex.card_index[take_index] == index_num) {
												for (int y = 0; y < hand_card_idnex.card_index[take_index]; y++) {
													tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[take_index][y];
													take_count++;
													if (take_count == three_link_count * 2) {
														break;
													}
												}
											}
											if (take_count == three_link_count * 2) {
												break;
											}
										}
									}
									if (take_count == three_link_count * 2) {
										break;
									}
								}
								if (take_count == three_link_count * 2) {
									for (int x = i; x >= j; x--) {
										for (int y = 0; y < hand_card_idnex.card_index[x]; y++) {
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
										}
									}
									all_tip_count++;
								}

								break;
							} else {
								i = j + 1;
								break;
							}
						}
					}
				}
			} else {
				// 如果牌还够的情况下，先不考虑拆炸弹
				if (hand_card_idnex.card_index[i] >= 3 && i > turn_index) {
					for (int j = i - 1; j >= 0; j--) {
						if (hand_card_idnex.card_index[j] >= 3) {
							if ((i - j) + 1 == three_link_count) {
								for (int take_index = 0; take_index < GameConstants.WSK_MAX_INDEX; take_index++) {
									if (take_index < j || take_index > i) {
										for (int y = 0; y < hand_card_idnex.card_index[take_index]; y++) {
											tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[take_index][y];
										}
									}
								}
								for (int x = i; x >= j; x--) {
									for (int y = 0; y < hand_card_idnex.card_index[x]; y++) {
										tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[x][y];
									}
								}
								all_tip_count++;

								break;
							}
						} else {
							i = j + 1;
							break;
						}
					}
				}
			}
		}
		all_tip_count = search_510k(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
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
			all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
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
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[7]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[7][i]) == color) {
					is_ten = true;
				}
			}
			for (int i = 0; i < hand_card_idnex.card_index[10]; i++) {
				if (this.GetCardColor(hand_card_idnex.card_data[10][i]) == color) {
					is_k = true;
				}
			}

			if (is_five && is_ten && is_k) {
				for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
					if (GetCardColor(hand_card_idnex.card_data[2][j]) == color) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
						break;
					}
				}
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
				all_tip_count++;
			}
		}
		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);

		return all_tip_count;
	}

	public int search_out_card_real_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];

		// 搜索炸弹
		all_tip_count = search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
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
							for (int j = 0; j < hand_card_idnex.card_index[2]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[2][j]) == color_five) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][j];
									break;
								}
							}
							for (int j = 0; j < hand_card_idnex.card_index[7]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[7][j]) == color_ten) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][j];
									break;
								}
							}
							for (int j = 0; j < hand_card_idnex.card_index[10]; j++) {
								if (GetCardColor(hand_card_idnex.card_data[10][j]) == color_k) {
									tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][j];
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

	public int search_boom(int cbCardData[], int cbCardCount, int tip_out_card[][], int tip_out_count[],
			int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 8 - 4; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == 4 + add_boom_count) {
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
					}
					all_tip_count++;
				}
			}
		}
		return all_tip_count;
	}

	public int search_out_card_boom(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);

		int turn_index = this.switch_card_to_idnex(turn_card_data[0]);

		int min_guntong = 2;
		if (turn_card_count == 5) {
			min_guntong = 3;
		} else if (turn_card_count == 6) {
			min_guntong = 4;
		} else if (turn_card_count == 7) {
			min_guntong = 5;
		}
		if (hand_card_idnex.card_index[2] >= min_guntong && hand_card_idnex.card_index[7] >= min_guntong
				&& hand_card_idnex.card_index[10] >= min_guntong) {
			for (int i = 0; i < 2; i++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[2][i];
			}
			for (int i = 0; i < 2; i++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[7][i];
			}
			for (int i = 0; i < 2; i++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[10][i];
			}
			all_tip_count++;
		}
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 8 - turn_card_count; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
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

		return all_tip_count;
	}

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type, int three_link_count) {
		switch (type) {
		case WSKConstants.PXGT_CT_SINGLE_LINK:
		case WSKConstants.PXGT_CT_DOUBLE_LINK: {
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
				this.SortCardList_Out(card_date, card_count, GameConstants.WSK_ST_VALUE);
			}
			return;
		}
		case WSKConstants.PXGT_CT_THREE_TAKE_TWO:
		case WSKConstants.PXGT_CT_FOUR_TAKE_ONE: {
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			for (int i = 0; i < card_count; i++) {
				int index = this.switch_card_to_idnex(card_date[i]);
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
		case WSKConstants.PXGT_CT_PLANE: {
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
				for (int i = 13; i >= 1; i--) {
					int index = this.switch_card_to_idnex(i);
					if (card_data_index.card_index[index] >= 3) {
						for (int j = i - 1; j >= 1; j--) {
							int other_index = this.switch_card_to_idnex(j);
							if (card_data_index.card_index[other_index] >= 3) {
								if ((i - j) + 1 == three_link_count) {
									for (int x = i; x >= j; x--) {
										for (int y = 0; y < 3; y++) {
											int cut_index = this.switch_card_to_idnex(x);
											int card_temp[] = new int[1];
											card_temp[0] = card_data_index.card_data[cut_index][0];
											card_date[count] = card_temp[0];
											this.RemoveCard(card_temp, 1, card_data_index.card_data[cut_index],
													card_data_index.card_index[cut_index]);
											card_data_index.card_index[cut_index]--;
											count++;
										}
									}
									break;
								}
							} else {
								break;
							}
						}
						if (count != 0) {
							break;
						}
					}
				}
			} else {
				for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
					if (card_data_index.card_index[i] >= 3) {
						for (int j = i - 1; j >= 0; j--) {
							if (card_data_index.card_index[j] >= 3) {
								if ((i - j) + 1 == three_link_count) {
									for (int x = i; x >= j; x--) {
										for (int y = 0; y < 3; y++) {
											int card_temp[] = new int[1];
											card_temp[0] = card_data_index.card_data[x][0];
											card_date[count] = card_temp[0];
											this.RemoveCard(card_temp, 1, card_data_index.card_data[x],
													card_data_index.card_index[x]);
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

	public void SortCardList_Out(int cbCardData[], int cbCardCount, int cbSortType) {
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

		return;
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
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}
			// 510K排前面
			int card_510K[] = new int[cbCardCount];
			int num_510K = 0;
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
			for (int j = 0; j < card_count_temp; j++) {
				card_510K[num_510K++] = card_temp[j];
			}

			card_temp = new int[card_index.card_index[10]];
			card_count_temp = 0;
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[10][j];
			}
			for (int j = 0; j < card_count_temp; j++) {
				card_510K[num_510K++] = card_temp[j];
			}
			card_index.card_index[10] = 0;
			card_index.card_index[7] = 0;
			card_index.card_index[2] = 0;
			int sort_num = 0;
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
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = 0; j < card_index.card_index[index[i]]; j++) {
					if (card_index.card_data[index[i]][j] < 0x100) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}

				}
			}
			// 510K牌最后
			for (int i = 0; i < num_510K; i++) {
				cbCardData[sort_num++] = card_510K[i];
			}
			return;
		}
		if (cbSortType == GameConstants.WSK_ST_510K) {
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
			for (int j = 0; j < card_count_temp; j++) {
				card_510K[num_510K++] = card_temp[j];
			}

			card_temp = new int[card_index.card_index[10]];
			card_count_temp = 0;
			for (int j = 0; j < card_index.card_index[10]; j++) {
				card_temp[card_count_temp++] = card_index.card_data[10][j];
			}
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

			// 其他牌
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {

				card_temp = new int[card_index.card_index[i]];
				card_count_temp = 0;
				for (int j = 0; j < card_index.card_index[i]; j++) {
					card_temp[card_count_temp++] = card_index.card_data[i][j];
				}
				for (int j = 0; j < card_count_temp; j++) {
					cbCardData[sort_num++] = card_temp[j];
				}
			}
			// 510K牌最后
			for (int i = 0; i < num_510K; i++) {
				cbCardData[sort_num++] = card_510K[i];
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

		return;
	}

	public int GetCardXianScore(int cbCardData[], int cbCardCount, int card_type) {
		int score = 0;
		int wang_count = Get_Wang_Count(cbCardData, cbCardCount);
		if (wang_count == 4) {
			if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_20)) {
				score = 20;
			} else if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_16)) {
				score = 16;
			}
			return score;
		}
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

	public int GetHandCardXianScore(int cbCardData[], int cbCardCount, int sheng_dang_biaozhi,
			UserCardData.Builder userCardDataBuilder) {
		int score = 0;
		int wang_count = Get_Wang_Count(cbCardData, cbCardCount);
		int max_num_index = -1;
		int max_num = 0;
		boolean si_wang = wang_count == 4;
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (max_num == 0 && card_index.card_index[i] >= 4) {
				max_num_index = i;
				max_num = card_index.card_index[i] + wang_count;
			} else if (card_index.card_index[i] + wang_count > max_num && card_index.card_index[i] >= 4) {
				max_num_index = i;
				max_num = card_index.card_index[i] + wang_count;
			}
		}
		if (si_wang) {
			if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_20)) {
				score += 20;
			} else if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_16)) {
				score += 16;
			}
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < cbCardCount; i++) {
				if (GetCardColor(cbCardData[i]) == 4) {
					cards_card.addItem(cbCardData[i]);
				}
			}
			userCardDataBuilder.addCardsData(cards_card);
			wang_count = 0;
		} else {
			if (max_num_index == -1) {
				return score;
			}
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
		if (!si_wang) {
			for (int i = 0; i < card_index.card_index[13]; i++) {
				card[card_index.card_index[max_num_index] + i] = card_index.card_data[13][i];
			}
			for (int i = 0; i < card_index.card_index[14]; i++) {
				card[card_index.card_index[max_num_index] + card_index.card_index[13]
						+ i] = card_index.card_data[14][i];
			}
		}

		int type = GetCardType(card, card.length, false);
		int sscore = this.GetCardXianScore(card, card_index.card_index[max_num_index] + wang_count, type);
		score += sscore;
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int i = 0; i < card_index.card_index[max_num_index] + wang_count && sscore > 0; i++) {
			cards_card.addItem(card[i]);
		}
		userCardDataBuilder.addCardsData(cards_card);

		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			if (card_index.card_index[i] >= 4 && i != max_num_index) {
				int type1 = GetCardType(card_index.card_data[i], card_index.card_index[i], false);
				sscore = this.GetCardXianScore(card_index.card_data[i], card_index.card_index[i], type1);
				score += sscore;
				cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < card_index.card_index[i] && sscore > 0; j++) {
					cards_card.addItem(card_index.card_data[i][j]);
				}
				userCardDataBuilder.addCardsData(cards_card);
			}
		}
		return score;
	}

	// 删除扑克
	@Override
	public boolean RemoveCard(int cbRemoveCard[], int cbRemoveCount, int cbCardData[], int cbCardCount) {
		// 定义变量
		int cbDeleteCount = 0, cbTempCardData[] = new int[GameConstants.WSK_MAX_COUNT];
		if (cbCardCount > cbTempCardData.length)
			return false;
		for (int i = 0; i < cbCardCount; i++) {
			cbTempCardData[i] = cbCardData[i];
		}

		// 置零扑克
		for (int i = 0; i < cbRemoveCount; i++) {
			for (int j = 0; j < cbCardCount; j++) {
				if (cbRemoveCard[i] == cbTempCardData[j] || cbRemoveCard[i] == cbTempCardData[j] - 0x100) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}
		if (cbDeleteCount != cbRemoveCount)
			return false;

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbTempCardData[i] != 0)
				cbCardData[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}
}
