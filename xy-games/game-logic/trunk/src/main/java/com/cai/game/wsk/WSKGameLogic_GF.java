package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.gfWsk.gfWskRsp.UserCardData;

public class WSKGameLogic_GF extends WSKGameLogic {

	public WSKGameLogic_GF() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount, boolean isLast) {
		if (cbCardCount == 0) {
			return GameConstants.WSK_GF_CT_ERROR;
		}
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
			return GameConstants.WSK_GF_CT_ERROR;
		}

		if (cbCardCount == 3) {
			// 三张
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 3) {
				return GameConstants.WSK_GF_CT_THREE;
			}
			// 510K
			if (card_index.card_index[2] > 0 && card_index.card_index[7] > 0 && card_index.card_index[10] > 0) {
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
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[this.switch_card_to_idnex(cbCardData[i])] == 3) {
					return GameConstants.WSK_GF_CT_THREE;
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
		if (cbCardCount >= 5) {
			if (this.is_link(card_index, 1, cbCardCount)) {
				return GameConstants.WSK_GF_CT_SINGLE_LINK;
			}
		}

		if (cbCardCount % 2 == 0) {
			// 连对
			if (this.is_link(card_index, 2, cbCardCount / 2) && wang_count == 0 && er_count == 0) {
				return GameConstants.WSK_GF_CT_DOUBLE_LINK;
			}
		}

		// 飞机
		int nPlane = is_plane(card_index, cbCardData, cbCardCount);
		if (nPlane == 0) {
			return GameConstants.WSK_GF_CT_PLANE_LOST;
		} else if (nPlane == 1) {
			return GameConstants.WSK_GF_CT_PLANE;
		}
		// 三连
		if (cbCardCount % 3 == 0) {
			if (this.is_link(card_index, 3, cbCardCount / 3)) {
				return GameConstants.WSK_GF_CT_PLANE_LOST;
			}
		}
		return GameConstants.WSK_GF_CT_ERROR;
	}

	public int get_three_link_count(int cbCardData[], int cbCardCount, int type) {
		tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_data_index);
		if (type == GameConstants.WSK_GF_CT_PLANE_LOST) {
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
							if (link_num > 1 && link_num * 5 > cbCardCount) {
								return link_num;
							}
						}
						if (j == 0) {
							if (link_num > 1 && link_num * 5 > cbCardCount) {
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

	// 对比扑克
	@Override
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,
			boolean pre_is_Last, boolean next_is_Last, int three_link_count) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount, next_is_Last);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount, pre_is_Last);

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
		tagAnalyseIndexResult_WSK next_card_index = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK first_card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbNextCard, cbNextCount, next_card_index);
		AnalysebCardDataToIndex(cbFirstCard, cbFirstCount, first_card_index);
		switch (cbFirstType) {
		case GameConstants.WSK_GF_CT_SINGLE:
		case GameConstants.WSK_GF_CT_DOUBLE: {
			if (cbFirstType != cbNextType) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case GameConstants.WSK_GF_CT_SINGLE_LINK:
		case GameConstants.WSK_GF_CT_DOUBLE_LINK: {
			if (cbFirstType != cbNextType || cbFirstCount != cbNextCount) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case GameConstants.WSK_GF_CT_PLANE:
		case GameConstants.WSK_GF_CT_PLANE_LOST: {
			if (cbNextType != GameConstants.WSK_GF_CT_PLANE && cbNextType != GameConstants.WSK_GF_CT_PLANE_LOST) {
				return false;
			}
			if (three_link_count * 5 < cbNextCount) {
				return false;
			}
			;
			int first_Type_index1 = get_plane_max_index(first_card_index, cbFirstCard, cbFirstCount, three_link_count);
			int cbNextType_index1 = get_plane_max_index(next_card_index, cbNextCard, cbNextCount, three_link_count);
			return cbNextType_index1 > first_Type_index1;
		}
		case GameConstants.WSK_GF_CT_THREE_TAKE_TWO:
		case GameConstants.WSK_GF_CT_THREE: {
			if (cbNextType != GameConstants.WSK_GF_CT_THREE_TAKE_TWO && cbNextType != GameConstants.WSK_GF_CT_THREE) {
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
		case GameConstants.WSK_GF_CT_BOMB_13:
		case GameConstants.WSK_GF_CT_BOMB_12:
		case GameConstants.WSK_GF_CT_BOMB_11: {
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_BOMB_10:
		case GameConstants.WSK_GF_CT_BOMB_9:
		case GameConstants.WSK_GF_CT_BOMB_8:
		case GameConstants.WSK_GF_CT_BOMB_7:
		case GameConstants.WSK_GF_CT_BOMB_6:
		case GameConstants.WSK_GF_CT_BOMB_5:
		case GameConstants.WSK_GF_CT_BOMB_4: {
			tip_type_count += search_out_card_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_510K_SC: {
			tip_type_count += search_out_card_real_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_510K_DC: {
			tip_type_count += search_out_card_false_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_PLANE:
		case GameConstants.WSK_GF_CT_PLANE_LOST: {
			tip_type_count += search_out_card_plane(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					three_link_count, tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_DOUBLE_LINK: {
			tip_type_count += search_out_card_double_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_SINGLE_LINK: {
			tip_type_count += search_out_card_single_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_THREE_TAKE_TWO:
		case GameConstants.WSK_GF_CT_THREE: {
			tip_type_count += search_out_card_three(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_DOUBLE:
		case GameConstants.WSK_GF_CT_SINGLE: {
			tip_type_count += search_out_single_double(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case GameConstants.WSK_GF_CT_ERROR: {
			tip_type_count += search_out_error(cbCardData, cbCardCount, turn_card_data, turn_card_count, tip_out_card,
					tip_out_count, tip_type_count);
			return tip_type_count;
		}
		}

		return tip_type_count;
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

		// 没有510K的情况
		if (hand_card_idnex.card_index[2] <= 0 || hand_card_idnex.card_index[7] <= 0
				|| hand_card_idnex.card_index[10] <= 0) {
			all_tip_count += search_boom(cbCardData, cbCardCount, tip_out_card, tip_out_count, all_tip_count);
			return all_tip_count;
		}
		// 有510K的情况下
		int out_color = this.GetCardColor(turn_card_data[0]);
		// 纯510K
		for (int color = out_color + 1; color < 4; color++) {
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
		// 王炸
		if (hand_card_idnex.card_index[14] + hand_card_idnex.card_index[13] == 4) {
			for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
			}
			for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
				tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
			}
			all_tip_count++;
		}
		// 带王的炸弹
		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 12 - 4; add_boom_count++) {
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					if (hand_card_idnex.card_index[i] >= 4) {
						if (hand_card_idnex.card_index[i] + count == 4 + add_boom_count) {
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

		if (turn_card_count <= 9) {
			// 王炸
			if (hand_card_idnex.card_index[14] + hand_card_idnex.card_index[13] == 4) {
				for (int j = 0; j < hand_card_idnex.card_index[13]; j++) {
					tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[13][j];
				}
				for (int j = 0; j < hand_card_idnex.card_index[14]; j++) {
					tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[14][j];
				}
				all_tip_count++;
			}
		}

		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 12 - turn_card_count; add_boom_count++) {
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					if (hand_card_idnex.card_index[i] >= 4) {
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

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type, int three_link_count) {
		switch (type) {
		case GameConstants.WSK_GF_CT_THREE_TAKE_TWO: {
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
		case GameConstants.WSK_GF_CT_PLANE:
		case GameConstants.WSK_GF_CT_PLANE_LOST: {
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			int count = 0;
			for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
				if (card_data_index.card_index[i] >= 3) {
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							if ((i - prv_index) + 1 == three_link_count) {
								for (int x = j; x <= i; x++) {
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

			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				for (int j = 0; j < card_data_index.card_index[i]; j++) {
					card_date[count++] = card_data_index.card_data[i][j];
				}
			}
			return;
		}
		}
	}

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		if (cbSortType == GameConstants.WSK_ST_VALUE) {
			// 转换数值
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = GetCardValue(cbCardData[i]);
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

		for (int i = cbCardCount - 1; i >= 0; i--) {
			if (cbCardData[i] > 0x100) {

				for (int j = i; j < cbCardCount - 1; j++) {
					if ((cbCardData[j + 1] / 0x100) < (cbCardData[j] / 0x100)) {
						int temp = cbCardData[j];
						cbCardData[j] = cbCardData[j + 1];
						cbCardData[j + 1] = temp;
					} else {
						break;
					}
				}
			}
		}
		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
			int index[] = new int[GameConstants.WSK_MAX_INDEX];
			boolean has_boom = false;
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
				if (card_index.card_index[index[i]] >= 4) {
					has_boom = true;
				}
			}
			if (has_boom) {
				int sort_num = 0;
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
				for (int i = GameConstants.WSK_MAX_INDEX - 3; i >= 0; i--) {
					for (int j = 0; j < card_index.card_index[index[i]]; j++) {
						if (card_index.card_data[index[i]][j] < 0x100) {
							cbCardData[sort_num++] = card_index.card_data[index[i]][j];
						}

					}
				}
				for (int i = GameConstants.WSK_MAX_INDEX - 1; i > GameConstants.WSK_MAX_INDEX - 3; i--) {
					for (int j = 0; j < card_index.card_index[index[i]]; j++) {
						if (card_index.card_data[index[i]][j] < 0x100) {
							cbCardData[sort_num++] = card_index.card_data[index[i]][j];
						}

					}
				}
			} else {
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

		for (int i = cbCardCount - 1; i >= 0; i--) {
			if (cbCardData[i] > 0x100) {

				for (int j = i; j < cbCardCount - 1; j++) {
					if ((cbCardData[j + 1] / 0x100) < (cbCardData[j] / 0x100)) {
						int temp = cbCardData[j];
						cbCardData[j] = cbCardData[j + 1];
						cbCardData[j + 1] = temp;
					} else {
						break;
					}
				}
			}
		}
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
			UserCardData.Builder userCardDataBuilder, int userdatascore[]) {
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
				userdatascore[userCardDataBuilder.getCardsDataCount()] = 20;
			} else if (has_rule(GameConstants.GAME_RULE_WSK_GF_SCORE_LIMIT_16)) {
				score += 16;
				userdatascore[userCardDataBuilder.getCardsDataCount()] = 16;
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
		userdatascore[userCardDataBuilder.getCardsDataCount()] = sscore;
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
				userdatascore[userCardDataBuilder.getCardsDataCount()] = sscore;
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
