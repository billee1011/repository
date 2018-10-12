package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;

public class WSKGameLogic_YXZD extends WSKGameLogic {

	public WSKGameLogic_YXZD() {

	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 0) {
			return WSKConstants.WSK_YXZD_CT_ERROR;
		}
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		int wang_count = Get_Wang_Count(card_index);
		int er_count = card_index.card_index[12];

		// 王加4炸
		if (wang_count > 0) {
			int count = card_index.card_index[this.switch_card_to_idnex(cbCardData[cbCardCount - 1])];
			if (count + wang_count == cbCardCount && cbCardCount >= 4) {
				return WSKConstants.WSK_YXZD_CT_BOMB;
			} else if (wang_count == cbCardCount && cbCardCount >= 4) {
				return WSKConstants.WSK_YXZD_CT_BOMB;
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

			return WSKConstants.WSK_YXZD_CT_BOMB;
		}

		if (cbCardCount == 3) {
			// 三张
			if (card_index.card_index[this.switch_card_to_idnex(cbCardData[0])] == 3) {
				return WSKConstants.WSK_YXZD_CT_THREE;
			}
			// 510K
			if (card_index.card_index[2] > 0 && card_index.card_index[7] > 0 && card_index.card_index[10] > 0) {
				return WSKConstants.WSK_YXZD_CT_510K;
			}
			if (card_index.card_index[2] <= 1 && card_index.card_index[7] <= 1 && card_index.card_index[10] <= 1
					&& card_index.card_index[2] + card_index.card_index[7] + card_index.card_index[10]
							+ wang_count == cbCardCount
					&& card_index.card_index[2] + card_index.card_index[7] + card_index.card_index[10] > 0) {
				return WSKConstants.WSK_YXZD_CT_510K;
			}
			return WSKConstants.WSK_YXZD_CT_ERROR;
		}

		// 顺子
		if (cbCardCount >= 5) {
			if (this.is_link_other(cbCardData, cbCardCount, 1, cbCardCount)) {
				return WSKConstants.WSK_YXZD_CT_SINGLE_LINK;
			}
		}

		if (cbCardCount % 2 == 0) {
			// 连对
			if (this.is_link_other(cbCardData, cbCardCount, 2, cbCardCount / 2)) {
				return WSKConstants.WSK_YXZD_CT_DOUBLE_LINK;
			}
		}
		// 三连
		if (cbCardCount % 3 == 0) {
			if (this.is_link_other(cbCardData, cbCardCount, 3, cbCardCount / 3)) {
				return WSKConstants.WSK_YXZD_CT_PLANE;
			}
		}
		return WSKConstants.WSK_YXZD_CT_ERROR;
	}

	// 是否连 123456
	/*
	 * card_data_index 牌数据 link_num 几连
	 */
	public boolean is_link_other(int cbCardData[], int cbCardCount, int link_num, int link_count_num) {
		tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_data_index);
		int pai_count = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			pai_count += card_data_index.card_index[i];
		}
		if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
			if (card_data_index.card_index[10] > 0) {
				return false;
			}
			int num = 0;
			for (int i = 1; i < 13; i++) {
				int index = this.switch_card_to_idnex(i);
				if (card_data_index.card_index[index] == 0) {
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

				if (card_data_index.card_index[index] == link_num) {
					num++;
				} else {
					return false;
				}
			}
			if (num >= link_count_num) {
				return true;
			} else {
				return false;
			}
		} else {
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
				} else {
					return false;
				}
			}
			if (num >= link_count_num) {
				return true;
			} else {
				return false;
			}
		}

	}

	// 是否连
	/*
	 * card_data_index 牌数据 link_num 几连
	 */
	public boolean is_link(tagAnalyseIndexResult_WSK card_data_index, int link_num, int link_count_num) {
		int pai_count = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			pai_count += card_data_index.card_index[i];
		}
		int num = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
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
			} else {
				return false;
			}
		}
		if (num >= link_count_num) {
			return true;
		} else {
			return false;
		}
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
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙
		if (cbNextType >= WSKConstants.WSK_YXZD_CT_510K && cbFirstType < WSKConstants.WSK_YXZD_CT_510K)
			return true;
		if (cbNextType < WSKConstants.WSK_YXZD_CT_510K && cbFirstType >= WSKConstants.WSK_YXZD_CT_510K) {
			return false;
		}
		if (cbNextType >= WSKConstants.WSK_YXZD_CT_510K && cbFirstType >= WSKConstants.WSK_YXZD_CT_510K) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == WSKConstants.WSK_YXZD_CT_510K) {
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

		switch (cbFirstType) {
		case WSKConstants.WSK_YXZD_CT_SINGLE:
		case WSKConstants.WSK_YXZD_CT_DOUBLE:
		case WSKConstants.WSK_YXZD_CT_THREE: {
			if (cbFirstType != cbNextType) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		case WSKConstants.WSK_YXZD_CT_SINGLE_LINK:
		case WSKConstants.WSK_YXZD_CT_DOUBLE_LINK:
		case WSKConstants.WSK_YXZD_CT_PLANE: {
			if (cbFirstType != cbNextType || cbFirstCount != cbNextCount) {
				return false;
			}
			return GetCardLogicValue(cbNextCard[0]) > GetCardLogicValue(cbFirstCard[0]);
		}
		}
		return false;
	}

	public int search_out_card(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int tip_type_count) {
		int count = 0;
		int turn_card_type = this.GetCardType(turn_card_data, turn_card_count);

		switch (turn_card_type) {
		case WSKConstants.WSK_YXZD_CT_BOMB: {
			tip_type_count += search_out_card_boom(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.WSK_YXZD_CT_510K: {
			tip_type_count += search_out_card_510K(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}

		case WSKConstants.WSK_YXZD_CT_PLANE: {
			tip_type_count += search_out_card_plane(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.WSK_YXZD_CT_DOUBLE_LINK: {
			tip_type_count += search_out_card_double_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.WSK_YXZD_CT_SINGLE_LINK: {
			tip_type_count += search_out_card_single_link(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.WSK_YXZD_CT_THREE:
		case WSKConstants.WSK_YXZD_CT_DOUBLE:
		case WSKConstants.WSK_YXZD_CT_SINGLE: {
			tip_type_count += search_out_single_double(cbCardData, cbCardCount, turn_card_data, turn_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			return tip_type_count;
		}
		case WSKConstants.WSK_YXZD_CT_ERROR: {
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
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(turn_card_data, turn_card_count, card_index);
		AnalysebCardDataToIndex(cbCardData, cbCardCount, hand_card_idnex);
		int wang_count = hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];

		int three_link_count = turn_card_count / 3;
		// 找出飞机的最大值
		int turn_index = -1;
		for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {

			if (card_index.card_index[i] == 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] == 3) {
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

	public int search_out_card_510K(int cbCardData[], int cbCardCount, int turn_card_data[], int turn_card_count,
			int tip_out_card[][], int tip_out_count[], int all_tip_count) {
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
		for (int add_boom_count = 0; add_boom_count <= 24 - 4; add_boom_count++) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
				if (hand_card_idnex.card_index[i] == 4 + add_boom_count) {
					for (int j = 0; j < hand_card_idnex.card_index[i]; j++) {
						tip_out_card[all_tip_count][tip_out_count[all_tip_count]++] = hand_card_idnex.card_data[i][j];
					}
					all_tip_count++;
				}
			}
		}
		// 带王的炸弹
		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 36 - 4; add_boom_count++) {
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
		int turn_index = this.switch_card_to_idnex(turn_card_data[turn_card_count - 1]);
		// 不带王的炸弹
		for (int add_boom_count = 0; add_boom_count <= 24 - turn_card_count; add_boom_count++) {
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

		for (int count = 1; count <= wang_count; count++) {
			for (int add_boom_count = 0; add_boom_count <= 36 - turn_card_count; add_boom_count++) {
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

	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount, int cbSortType) {
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);
		// 510K排前面

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
			for (int j = 0; j < card_index.card_index[index[i]]; j++) {
				cbCardData[sort_num++] = card_index.card_data[index[i]][j];
			}
		}
		return;
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

	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		switch (type) {
		case WSKConstants.WSK_YXZD_CT_SINGLE_LINK:
		case WSKConstants.WSK_YXZD_CT_DOUBLE_LINK:
		case WSKConstants.WSK_YXZD_CT_PLANE: {
			tagAnalyseIndexResult_WSK card_data_index = new tagAnalyseIndexResult_WSK();
			AnalysebCardDataToIndex(card_date, card_count, card_data_index);
			if (card_data_index.card_index[GameConstants.WSK_MAX_INDEX - 3] > 0) {
				this.SortCardList_Out(card_date, card_count, GameConstants.WSK_ST_VALUE);
			}
			return;
		}

		}
	}

}
