package com.cai.game.gdy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.gdy.data.tagAnalyseIndexResult_GDY;

public class GDYGameLogic {
	private static Logger logger = Logger.getLogger(GDYGameLogic.class);
	public Map<Integer, Integer> ruleMap = new HashMap<>();

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public void switch_to_card_index(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue(card_data[i]);
			card_index[index - 3]++;
		}
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_GDY AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] == 0) {
				continue;
			}
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	// 是否连
	public boolean is_link(tagAnalyseIndexResult_GDY card_data_index, int link_num, int link_count_num) {
		int pai_count = 0;
		for (int i = 0; i < GameConstants.GDY_MAX_INDEX; i++) {
			pai_count += card_data_index.card_index[i];
		}
		int num = 0;
		for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
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
				if (card_data_index.card_index[i] >= link_num) {
					num++;
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

	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane(tagAnalyseIndexResult_GDY card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		for (int i = GameConstants.GDY_MAX_INDEX - 4; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num * 5 == cbCardCount) {
							return 1;
						} else if (link_num * 5 > cbCardCount) {
							return 0;
						}
					} else {
						i = j + 1;
						break;
					}
				}
			}
		}

		return -1;
	}

	// 飞机 0飞机缺翅膀 1飞机
	public int is_plane_take_double(tagAnalyseIndexResult_GDY card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		int num = 0;
		for (int i = GameConstants.GDY_MAX_INDEX - 4; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num * 5 == cbCardCount) {
							for (int x = i; x >= j; x--) {
								card_data_index.card_index[x] -= 3;
							}
							int double_count = 0;
							for (int x = GameConstants.GDY_MAX_INDEX - 3; x >= 0; x--) {
								if (card_data_index.card_index[x] == 2) {
									double_count++;
								} else if (card_data_index.card_index[x] == 4) {
									double_count += 2;
								}
							}
							if (double_count == link_num) {
								return 1;
							} else {
								i = j + 1;
								break;
							}

						}
					} else {
						i = j + 1;
						break;
					}
				}
			}
		}

		return -1;
	}

	public int get_card_index(int card_data) {
		int index = GetCardLogicValue(card_data);
		return index - 3;
	}

	public int get_index_value(int index) {
		return index + 3;
	}

	public int get_index_card_count(int card_index[]) {
		int card_count = 0;
		for (int i = 0; i < GameConstants.GDY_MAX_INDEX; i++) {
			card_count += card_index[i];
		}
		return card_count;
	}

	public int get_magic_card_count(int card_index[]) {
		return card_index[13] + card_index[14];
	}

	public int get_trustee_card(int card_data[], int cardCount, int hand_card_data[], int hand_card_count,
			int out_card_data[], int out_card_change_data[]) {
		int card_type = this.GetCardType_GDY(card_data, cardCount);
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int hand_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(card_data, cardCount, card_index);
		switch_to_card_index(hand_card_data, hand_card_count, hand_index);
		int magic_count = this.get_magic_card_count(hand_index);
		switch (card_type) {
		case GameConstants.GDY_CT_SINGLE: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			if (this.GetCardValue(card_data[0]) != 2) {

				for (int i = 0; i < hand_card_count; i++) {
					if (get_index_value(index + 1) == this.GetCardLogicValue(hand_card_data[i])) {
						out_card_data[0] = hand_card_data[i];
						out_card_change_data[0] = hand_card_data[i];
						return 1;
					}
					if (get_index_value(GameConstants.GDY_MAX_INDEX - 3) == GetCardLogicValue(hand_card_data[i])) {
						out_card_data[0] = hand_card_data[i];
						out_card_change_data[0] = hand_card_data[i];
						return 1;
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])
											&& hand_index[13] > 0) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])
											&& hand_index[14] > 0) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] == 0 || hand_index[14] == 0) {
					return 0;
				}
				int out_card_count = 0;
				for (int j = 0; j < hand_card_count; j++) {
					if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
						out_card_data[out_card_count] = hand_card_data[j];
						out_card_change_data[out_card_count++] = hand_card_data[j];
						if (out_card_count > 2) {
							return out_card_count;
						}
					}
					if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
						out_card_data[out_card_count] = hand_card_data[j];
						out_card_change_data[out_card_count++] = hand_card_data[j];
						if (out_card_count > 2) {
							return out_card_count;
						}
					}
				}
			}

			return 0;
		}
		case GameConstants.GDY_CT_DOUBLE: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}
			if (this.GetCardValue(card_data[0]) != 2) {
				int out_card_count = 0;
				for (int i = 0; i < hand_card_count; i++) {
					if (get_index_value(index + 1) == GetCardLogicValue(hand_card_data[i])
							&& hand_index[index + 1] + magic_count == 2 && hand_index[index + 1] > 0) {
						if (hand_index[index + 1] == 2) {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							out_card_data[out_card_count] = hand_card_data[i + 1];
							out_card_change_data[out_card_count++] = hand_card_data[i + 1];
							return out_card_count;
						} else {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							for (int j = 0; j < hand_card_count; j++) {
								if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = this.get_index_value(index + 1);
									return out_card_count;
								}
								if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = this.get_index_value(index + 1);
									return out_card_count;
								}
							}
						}

						return 0;
					}
					if (get_index_value(GameConstants.GDY_MAX_INDEX - 3) == GetCardLogicValue(hand_card_data[i])
							&& hand_index[GameConstants.GDY_MAX_INDEX - 3] + magic_count == 2
							&& hand_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
						if (hand_index[GameConstants.GDY_MAX_INDEX - 3] == 2) {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							out_card_data[out_card_count] = hand_card_data[i + 1];
							out_card_change_data[out_card_count++] = hand_card_data[i + 1];
							return out_card_count;
						} else {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							for (int j = 0; j < hand_card_count; j++) {
								if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = hand_card_data[i];
									return out_card_count;
								}
								if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = hand_card_data[i];
									return out_card_count;
								}
							}
						}

						return 0;
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_3: {
			int index = get_card_index(card_data[0]);

			// 获取三炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 3 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_4: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 4 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_5: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 5 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count == 5) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count == 5) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 5) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 5) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 5) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_SINGLE_LINK: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] == 3 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			int magic_count_temp = magic_count;
			magic_count_temp = magic_count;
			if (index < GameConstants.GDY_MAX_INDEX - cardCount - 1) {
				for (int i = index + 1; i > index - cardCount; i--) {
					if (hand_index[i] == 0) {
						if (magic_count_temp == 0) {
							if (index - i >= cardCount) {
								int out_card_count = 0;
								for (int j = index + 1; j < i; j++) {
									if (hand_index[j] == 0) {
										for (int x = 0; x < hand_card_count; x++) {
											if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])
													|| get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
												out_card_data[out_card_count] = hand_card_data[x];
												out_card_change_data[out_card_count++] = this.get_index_value(j);
												break;
											}
										}
									} else {
										for (int x = 0; x < hand_card_count; x++) {
											if (get_index_value(j) == GetCardLogicValue(hand_card_data[x])) {
												out_card_data[out_card_count] = hand_card_data[x];
												out_card_change_data[out_card_count++] = this.get_index_value(j);
												break;
											}
										}
									}
								}
								return out_card_count;
							}
							break;
						} else {
							magic_count_temp--;
						}
					}
					if (i == index - cardCount + 2) {
						int hand_card_data_temp[] = new int[hand_card_count];
						int out_card_count = 0;
						for (int x = 0; x < hand_card_count; x++) {
							hand_card_data_temp[x] = hand_card_data[x];
						}
						for (int j = index + 1; j >= i; j--) {
							if (hand_index[j] == 0) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data_temp[x])
											|| get_index_value(14) == GetCardLogicValue(hand_card_data_temp[x])) {
										out_card_data[out_card_count] = hand_card_data_temp[x];
										out_card_change_data[out_card_count++] = this.get_index_value(j);
										hand_card_data_temp[x] = 0;
										break;
									}
								}
							} else {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(j) == GetCardLogicValue(hand_card_data_temp[x])) {
										out_card_data[out_card_count] = hand_card_data_temp[x];
										out_card_change_data[out_card_count++] = this.get_index_value(j);
										hand_card_data_temp[x] = 0;
										break;
									}
								}
							}
						}
						return out_card_count;
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_DOUBLE_LINK: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] == 3 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			int magic_count_temp = magic_count;

			for (int i = index + 1; i <= GameConstants.GDY_MAX_INDEX - cardCount - 1; i++) {
				if (hand_index[i] == 0) {
					if (magic_count_temp == 0) {
						if (index - i >= cardCount) {
							int out_card_count = 0;
							for (int j = index + 1; j < i; j++) {
								int count = 0;
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(j) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = this.get_index_value(j);
										count++;
										if (count == 2) {
											break;
										}
									}
								}
								if (count < 2) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])
												|| get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = this.get_index_value(j);
											if (count == 2) {
												break;
											}
										}
									}
								}

							}
							return out_card_count;
						}
					} else {
						magic_count_temp--;
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		}
		return 0;
	}

	public boolean search_card_data(int card_data[], int cardCount, int hand_card_data[], int hand_card_count) {
		int card_type = this.GetCardType_GDY(card_data, cardCount);
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int hand_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(card_data, cardCount, card_index);
		switch_to_card_index(hand_card_data, hand_card_count, hand_index);
		int magic_count = this.get_magic_card_count(hand_index);
		if (hand_index[13] > 0 && hand_index[14] > 0) {
			return true;
		}
		switch (card_type) {
		case GameConstants.GDY_CT_SINGLE: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			if (this.GetCardValue(card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] > 0 || hand_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_DOUBLE: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			if (this.GetCardValue(card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] + magic_count > 1
					|| hand_index[GameConstants.GDY_MAX_INDEX - 3] + magic_count > 1) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_3: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
				if (i > index && (hand_index[i] + magic_count > 2)) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_4: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					return true;
				}
				if (i > index && (hand_index[i] + magic_count > 3)) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_5: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 5) {
					return true;
				}
				if (i > index && (hand_index[i] + magic_count > 4)) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_6: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (i > index && (hand_index[i] + magic_count > 5)) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_SINGLE_LINK: {
			int index = get_card_index(card_data[cardCount - 1]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			magic_count_temp = magic_count;
			for (int i = index + 1; i <= GameConstants.GDY_MAX_INDEX - cardCount - 1; i++) {
				if (hand_index[i] == 0) {
					if (magic_count_temp == 0) {
						if (index - i >= cardCount) {
							return true;
						}
						break;
					} else {
						magic_count_temp--;
					}
				}
				if (i - index >= cardCount && i <= GameConstants.GDY_MAX_INDEX - cardCount - 1) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_DOUBLE_LINK: {
			int index = get_card_index(card_data[cardCount - 1]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			for (int i = index + 1; i <= GameConstants.GDY_MAX_INDEX - cardCount / 2 - 2; i++) {
				if (hand_index[i] < 2) {
					if (magic_count_temp + hand_index[i] < 2) {
						if (index - i >= cardCount) {
							return true;
						}
						break;
					} else {
						magic_count_temp = 2 - hand_index[i];
					}

				}
				if (i - index >= cardCount / 2 && i <= GameConstants.GDY_MAX_INDEX - cardCount / 2 - 2) {
					return true;
				}
			}
			return false;
		}
		}
		return false;
	}

	public boolean is_have_card(int cbCardData[], int cbMagicCardData[], int cbCardCount) {
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int magic_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);
		switch_to_card_index(cbMagicCardData, cbCardCount, magic_index);

		int magic_count = this.get_magic_card_count(card_index);
		for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
			if (magic_index[i] - card_index[i] <= magic_count && magic_index[i] - card_index[i] >= 0) {
				magic_count -= magic_index[i] - card_index[i];
			} else {
				return false;
			}
		}
		if (magic_count != 0) {
			return false;
		}

		return true;
	}

	// 获取类型
	public int GetCardType_GDY(int cbCardData[], int cbCardCount) {
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);
		if (isAllMagic(cbCardData, cbCardCount) && cbCardCount == 2) {
			return GameConstants.GDY_CT_KING_BOMB;
		}

		for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
			int cbCardCountTemp = cbCardCount;
			if (card_index[i] > 0) {
				if (cbCardCount == card_index[i]) {
					if (cbCardCount == 1) {
						return GameConstants.GDY_CT_SINGLE;
					} else if (cbCardCount == 2) {
						return GameConstants.GDY_CT_DOUBLE;
					} else if (cbCardCount == 3) {
						return GameConstants.GDY_CT_BOMB_3;
					} else if (cbCardCount == 4) {
						return GameConstants.GDY_CT_BOMB_4;
					} else if (cbCardCount == 5) {
						return GameConstants.GDY_CT_BOMB_5;
					} else if (cbCardCount == 6) {
						return GameConstants.GDY_CT_BOMB_6;
					}
				}
				if (card_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
					return GameConstants.GDY_CT_ERROR;
				}
				cbCardCountTemp -= card_index[i];
				if (card_index[i] >= 3) {
					return GameConstants.GDY_CT_ERROR;
				} else if (card_index[i] == 2) {
					for (int j = i + 1; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (card_index[j] == 2) {
							cbCardCountTemp -= card_index[j];
							if (cbCardCountTemp == 0) {
								if (j - i < 1) {
									return GameConstants.GDY_CT_ERROR;
								} else {
									return GameConstants.GDY_CT_DOUBLE_LINK;
								}
							}
						} else {
							if (cbCardCountTemp == 0) {
								if (j - i < 1) {
									return GameConstants.GDY_CT_ERROR;
								} else {
									return GameConstants.GDY_CT_DOUBLE_LINK;
								}
							} else {
								return GameConstants.GDY_CT_ERROR;
							}
						}
					}
				} else {
					for (int j = i + 1; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (card_index[j] >= 1) {
							cbCardCountTemp -= 1;
							if (cbCardCountTemp == 0) {
								if (j - i < 2) {
									return GameConstants.GDY_CT_ERROR;
								} else {
									return GameConstants.GDY_CT_SINGLE_LINK;
								}
							}
						} else {
							if (cbCardCountTemp == 0) {
								if (j - i < 2) {
									return GameConstants.GDY_CT_ERROR;
								} else {
									return GameConstants.GDY_CT_SINGLE_LINK;
								}
							} else {
								return GameConstants.GDY_CT_ERROR;
							}
						}

					}
				}
			}
		}
		return GameConstants.GDY_CT_ERROR;
	}

	public void make_magic_card(int cbCardData[], int magic_card[], int cbCardCount) {
		int card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int maigc_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, card_index);
		Arrays.fill(maigc_index, 0);
		int maigc_count = this.get_magic_card_count(card_index);
		for (int i = 0; i < cbCardCount; i++) {
			magic_card[i] = cbCardData[i];
		}
		if (maigc_count == 0) {
			return;
		}
		for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
			int cbCardCountTemp = cbCardCount;
			int magic_count_temp = maigc_count;
			if (card_index[i] > 0) {
				if (cbCardCount == card_index[i] + maigc_count) {
					// 先判断炸弹
					for (int j = 0; j < cbCardCount; j++) {
						if (GetCardLogicValue(magic_card[j]) == 16 || GetCardLogicValue(magic_card[j]) == 17) {
							magic_card[j] = get_index_value(i);
						}
					}
					return;
				}
			}
		}
		boolean dan_shun_zi = true;
		for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
			if (card_index[i] > 2) {
				// 炸弹已经过滤
				return;
			}
			if (card_index[i] > 1) {
				dan_shun_zi = false;
			}
		}
		if (dan_shun_zi) {
			// 单顺
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
				int cbCardCountTemp = cbCardCount;
				int magic_count_temp = maigc_count;
				if (card_index[i] > 0) {
					// 往前变牌
					for (int j = i + 1; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (card_index[j] == 0) {

							for (int x = 0; x < cbCardCount; x++) {
								if (GetCardLogicValue(magic_card[x]) == 16 || GetCardLogicValue(magic_card[x]) == 17) {
									magic_card[x] = get_index_value(j);
									break;
								}
							}
							magic_count_temp--;
							if (magic_count_temp == 0) {
								return;
							}
						}
					}
					// 往后变牌
					if (magic_count_temp != 0) {
						for (int j = i - 1; j >= 0; j--) {
							if (j == 0) {
								for (int x = 0; x < magic_count_temp; x++) {
									if (GetCardLogicValue(magic_card[x]) == 16
											|| GetCardLogicValue(magic_card[x]) == 17) {
										magic_card[x] = get_index_value(j);
									}
								}
							} else {
								for (int x = 0; x < cbCardCount; x++) {
									if (GetCardLogicValue(magic_card[x]) == 16
											|| GetCardLogicValue(magic_card[x]) == 17) {
										magic_card[x] = get_index_value(j);
										break;
									}
								}
								magic_count_temp--;
								if (magic_count_temp == 0) {
									return;
								}
							}
						}
					}
					return;
				}
			}
		} else {
			// 双顺
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 3; i++) {
				int cbCardCountTemp = cbCardCount;
				int magic_count_temp = maigc_count;
				if (card_index[i] > 0) {
					// 往前变牌
					for (int j = i; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (card_index[j] < 2) {
							for (int y = 0; y < 2 - card_index[j]; y++) {
								for (int x = 0; x < cbCardCount; x++) {
									if (GetCardLogicValue(magic_card[x]) == 16
											|| GetCardLogicValue(magic_card[x]) == 17) {
										magic_card[x] = get_index_value(j);
										break;
									}
								}
								magic_count_temp--;
								if (magic_count_temp == 0) {
									return;
								}
							}
						}
					}
					// 往后变牌
					if (magic_count_temp != 0) {
						for (int j = i; j >= 0; j--) {
							if (j == 0) {
								for (int x = 0; x < magic_count_temp; x++) {
									if (GetCardLogicValue(magic_card[x]) == 16
											|| GetCardLogicValue(magic_card[x]) == 17) {
										magic_card[x] = get_index_value(j);
									}
								}
							} else {
								if (card_index[j] < 2) {
									for (int y = 0; y < 2 - card_index[j]; y++) {
										for (int x = 0; x < cbCardCount; x++) {
											if (GetCardLogicValue(magic_card[x]) == 16
													|| GetCardLogicValue(magic_card[x]) == 17) {
												magic_card[x] = get_index_value(j);
												break;
											}
										}
										magic_count_temp--;
										if (magic_count_temp == 0) {
											return;
										}
									}
								}
							}
						}
					}
					return;
				}
			}
		}
	}

	public boolean comparecarddata(int first_card[], int first_count, int next_card[], int next_count) {
		int first_card_index[] = new int[GameConstants.GAY_MAX_COUT];
		int next_card_index[] = new int[GameConstants.GAY_MAX_COUT];
		switch_to_card_index(first_card, first_count, first_card_index);
		switch_to_card_index(next_card, next_count, next_card_index);

		int first_card_type = GetCardType_GDY(first_card, first_count);
		int next_card_type = GetCardType_GDY(next_card, next_count);
		if (next_card_type == GameConstants.GDY_CT_KING_BOMB) {
			return false;
		}
		if (first_card_type == GameConstants.GDY_CT_KING_BOMB) {
			return true;
		}
		if (next_card_type >= GameConstants.GDY_CT_BOMB_3 && first_card_type < GameConstants.GDY_CT_BOMB_3) {
			return false;
		}
		if (next_card_type < GameConstants.GDY_CT_BOMB_3 && first_card_type >= GameConstants.GDY_CT_BOMB_3) {
			return true;
		}
		if (next_card_type >= GameConstants.GDY_CT_BOMB_3 && first_card_type >= GameConstants.GDY_CT_BOMB_3) {
			if (next_card_type > first_card_type) {
				return false;
			} else if (next_card_type < first_card_type) {
				return true;
			} else {
				return this.GetCardLogicValue(first_card[0]) > this.GetCardLogicValue(next_card[0]);
			}
		}
		if (next_card_type != first_card_type) {
			return false;
		}
		if (first_count != next_count) {
			return false;
		}
		switch (first_card_type) {
		case GameConstants.GDY_CT_SINGLE:
		case GameConstants.GDY_CT_DOUBLE: {
			int first_value = this.GetCardLogicValue(first_card[0]);
			int next_value = this.GetCardLogicValue(next_card[0]);
			if (first_value == 15 && next_value < 15) {
				return true;
			}
			return first_value - 1 == next_value;
		}
		case GameConstants.GDY_CT_SINGLE_LINK:
		case GameConstants.GDY_CT_DOUBLE_LINK: {
			int first_value = this.GetCardLogicValue(first_card[0]);
			int next_value = this.GetCardLogicValue(next_card[0]);
			return first_value - 1 == next_value;
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
	// 获取类型
	public int GetCardType_GDY_HB(int cbCardData[], int change_out_card_data[], int cbCardCount) {
		int real_card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		int change_card_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(cbCardData, cbCardCount, real_card_index);
		switch_to_card_index(change_out_card_data, cbCardCount, change_card_index);
		if (isAllMagic(cbCardData, cbCardCount) && cbCardCount == 2
				&& this.has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
			return GameConstants.GDY_CT_KING_BOMB_HL;
		}

		for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
			int cbCardCountTemp = cbCardCount;
			if (change_card_index[i] > 0) {
				if (cbCardCount == change_card_index[i]) {
					if (cbCardCount == 1) {
						return GameConstants.GDY_CT_SINGLE_HL;
					} else if (cbCardCount == 2) {
						return GameConstants.GDY_CT_DOUBLE_HL;
					} else if (cbCardCount == 3) {
						if (real_card_index[14] > 0) {
							return GameConstants.GDY_CT_BOMB_3_DAWANG_RUAN_HL;
						} else if (real_card_index[13] > 0) {
							return GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL;
						} else {
							return GameConstants.GDY_CT_BOMB_3_HL;
						}
					} else if (cbCardCount == 4) {
						if (real_card_index[14] > 0) {
							return GameConstants.GDY_CT_BOMB_4_DAWANG_RUAN_HL;
						} else if (real_card_index[13] > 0) {
							return GameConstants.GDY_CT_BOMB_4_XIAOWANG_RUAN_HL;
						} else {
							return GameConstants.GDY_CT_BOMB_4_HL;
						}
					} else if (cbCardCount == 5) {
						if (real_card_index[14] > 0) {
							return GameConstants.GDY_CT_BOMB_5_DAWANG_RUAN_HL;
						} else if (real_card_index[13] > 0) {
							return GameConstants.GDY_CT_BOMB_5_XIAOWANG_RUAN_HL;
						} else {
							return GameConstants.GDY_CT_BOMB_3_HL;
						}
					} else if (cbCardCount == 6) {
						return GameConstants.GDY_CT_BOMB_6_HL;
					}
				}
				if (change_card_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
					return GameConstants.GDY_CT_ERROR_HL;
				}
				cbCardCountTemp -= change_card_index[i];
				if (change_card_index[i] >= 3) {
					return GameConstants.GDY_CT_ERROR_HL;
				} else if (change_card_index[i] == 2) {
					for (int j = i + 1; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (change_card_index[j] == 2) {
							cbCardCountTemp -= change_card_index[j];
							if (cbCardCountTemp == 0) {
								if (j - i < 1) {
									return GameConstants.GDY_CT_ERROR_HL;
								} else {
									return GameConstants.GDY_CT_DOUBLE_LINK;
								}
							}
						} else {
							if (cbCardCountTemp == 0) {
								if (j - i < 1) {
									return GameConstants.GDY_CT_ERROR_HL;
								} else {
									return GameConstants.GDY_CT_DOUBLE_LINK;
								}
							} else {
								return GameConstants.GDY_CT_ERROR_HL;
							}
						}
					}
				} else {
					for (int j = i + 1; j < GameConstants.GDY_MAX_INDEX - 3; j++) {
						if (change_card_index[j] >= 1) {
							cbCardCountTemp -= 1;
							if (cbCardCountTemp == 0) {
								if (j - i < 2) {
									return GameConstants.GDY_CT_ERROR_HL;
								} else {
									return GameConstants.GDY_CT_SINGLE_LINK_HL;
								}
							}
						} else {
							if (cbCardCountTemp == 0) {
								if (j - i < 2) {
									return GameConstants.GDY_CT_ERROR_HL;
								} else {
									return GameConstants.GDY_CT_SINGLE_LINK_HL;
								}
							} else {
								return GameConstants.GDY_CT_ERROR_HL;
							}
						}

					}
				}
			}
		}
		return GameConstants.GDY_CT_ERROR_HL;
	}

	public boolean search_card_data_hb(int card_data[], int change_out_card_data[], int cardCount, int hand_card_data[],
			int hand_card_count) {
		int card_type = this.GetCardType_GDY_HB(card_data, change_out_card_data, cardCount);
		int hand_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(hand_card_data, hand_card_count, hand_index);
		int magic_count = this.get_magic_card_count(hand_index);
		if (hand_card_count == hand_index[13] + hand_index[14]) {
			if (hand_card_count == 1) {
				return false;
			} else {
				if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
					return true;
				} else {
					return false;
				}
			}

		}
		if (hand_index[13] > 0 && hand_index[14] > 0) {
			return true;
		}
		switch (card_type) {
		case GameConstants.GDY_CT_SINGLE_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			if (this.GetCardValue(change_out_card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] > 0 || hand_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_DOUBLE_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			if (this.GetCardValue(change_out_card_data[0]) == 2) {
				return false;
			}
			if (hand_index[index + 1] + magic_count > 1
					|| hand_index[GameConstants.GDY_MAX_INDEX - 3] + magic_count > 1) {
				return true;
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
				if (i >= index && (hand_index[i] + magic_count > 2)) {
					return true;
				}
				if (hand_index[i] >= 3) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_3_DAWANG_RUAN_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
				if (i > index && (hand_index[i] + magic_count > 2)) {
					return true;
				}
				if (hand_index[i] >= 3) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_3_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					return true;
				}
				if (i > index && hand_index[i] > 2) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_4_XIAOWANG_RUAN_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					return true;
				}
				if (i >= index && (hand_index[i] + magic_count > 3)) {
					return true;
				}
				if (hand_index[i] >= 4) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_4_DAWANG_RUAN_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					return true;
				}
				if (i > index && (hand_index[i] + magic_count > 3)) {
					return true;
				}
				if (hand_index[i] >= 4) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_4_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					return true;
				}
				if (i > index && hand_index[i] > 3) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_5_XIAOWANG_RUAN_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 5) {
					return true;
				}
				if (i >= index && (hand_index[i] + magic_count > 4)) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_5_DAWANG_RUAN_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 5) {
					return true;
				}
				if (i > index && (hand_index[i] + magic_count > 4)) {
					return true;
				}
				if (i > index && hand_index[i] > 4) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_BOMB_6_HL: {
			int index = get_card_index(change_out_card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (i > index && (hand_index[i] + magic_count > 5)) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_SINGLE_LINK_HL: {
			int index = get_card_index(change_out_card_data[cardCount - 1]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			magic_count_temp = magic_count;
			for (int i = index + 1; i <= GameConstants.GDY_MAX_INDEX - cardCount - 1; i++) {
				if (hand_index[i] == 0) {
					if (magic_count_temp == 0) {
						if (index - i >= cardCount) {
							return true;
						}
						break;
					} else {
						magic_count_temp--;
					}
				}
				if (i - index >= cardCount && i <= GameConstants.GDY_MAX_INDEX - cardCount - 1) {
					return true;
				}
			}
			return false;
		}
		case GameConstants.GDY_CT_DOUBLE_LINK_HL: {
			int index = get_card_index(change_out_card_data[cardCount - 1]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					return true;
				}
			}
			int magic_count_temp = magic_count;
			for (int i = index + 1; i <= GameConstants.GDY_MAX_INDEX - cardCount / 2 - 2; i++) {
				if (hand_index[i] < 2) {
					if (magic_count_temp + hand_index[i] < 2) {
						if (index - i >= cardCount) {
							return true;
						}
						break;
					} else {
						magic_count_temp = 2 - hand_index[i];
					}

				}
				if (i - index >= cardCount / 2 && i <= GameConstants.GDY_MAX_INDEX - cardCount / 2 - 2) {
					return true;
				}
			}
			return false;
		}
		}
		return false;
	}

	public boolean comparecarddata_hb(int first_card[], int first_change_card[], int first_count, int next_card[],
			int next_change_card[], int next_count) {
		int first_card_index[] = new int[GameConstants.GAY_MAX_COUT];
		int next_card_index[] = new int[GameConstants.GAY_MAX_COUT];
		switch_to_card_index(first_card, first_count, first_card_index);
		switch_to_card_index(next_card, next_count, next_card_index);

		int first_card_type = GetCardType_GDY_HB(first_card, first_change_card, first_count);
		int next_card_type = GetCardType_GDY_HB(next_card, next_change_card, next_count);
		if (next_card_type == GameConstants.GDY_CT_KING_BOMB_HL) {
			return false;
		}
		if (first_card_type == GameConstants.GDY_CT_KING_BOMB_HL) {
			return true;
		}
		if (next_card_type >= GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL
				&& first_card_type < GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL) {
			return false;
		}
		if (next_card_type < GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL
				&& first_card_type >= GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL) {
			return true;
		}

		if (next_card_type >= GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL
				&& first_card_type >= GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL) {
			if (first_count > next_count) {
				return true;
			} else if (first_count < next_count) {
				return false;
			} else {
				int first_magic_count = this.get_magic_card_count(first_card_index);
				int next_magic_count = this.get_magic_card_count(next_card_index);
				if (first_magic_count > 0 && next_magic_count == 0) {
					return false;
				} else if (first_magic_count == 0 && next_magic_count > 0) {
					return true;
				} else {
					if (first_magic_count > 0 && next_magic_count > 0) {
						if (GetCardLogicValue(first_change_card[0]) == this.GetCardLogicValue(next_change_card[0])) {
							if (first_card_index[14] > 0) {
								return true;
							} else {
								return false;
							}
						} else {
							return this.GetCardLogicValue(first_change_card[0]) > this
									.GetCardLogicValue(next_change_card[0]);
						}
					} else {
						return this.GetCardLogicValue(first_change_card[0]) > this
								.GetCardLogicValue(next_change_card[0]);
					}

				}
			}
		}
		if (next_card_type != first_card_type) {
			return false;
		}
		if (first_count != next_count) {
			return false;
		}
		switch (first_card_type) {
		case GameConstants.GDY_CT_SINGLE_HL:
		case GameConstants.GDY_CT_DOUBLE_HL: {
			int first_value = this.GetCardLogicValue(first_change_card[0]);
			int next_value = this.GetCardLogicValue(next_change_card[0]);
			if (first_value == 15 && next_value < 15) {
				return true;
			}
			return first_value - 1 == next_value;
		}
		case GameConstants.GDY_CT_SINGLE_LINK_HL:
		case GameConstants.GDY_CT_DOUBLE_LINK_HL: {
			int first_value = this.GetCardLogicValue(first_change_card[0]);
			int next_value = this.GetCardLogicValue(next_change_card[0]);
			return first_value - 1 == next_value;
		}
		}

		return false;
	}

	public int get_trustee_card_hb(int card_data[], int card_change_data[], int cardCount, int hand_card_data[],
			int hand_card_count, int out_card_data[], int out_card_change_data[]) {
		int card_type = this.GetCardType_GDY_HB(card_data, card_change_data, cardCount);
		int hand_index[] = new int[GameConstants.GDY_MAX_INDEX];
		switch_to_card_index(hand_card_data, hand_card_count, hand_index);
		int magic_count = this.get_magic_card_count(hand_index);
		switch (card_type) {
		case GameConstants.GDY_CT_SINGLE_HL: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}
			if (this.GetCardValue(card_data[0]) != 2) {

				for (int i = 0; i < hand_card_count; i++) {
					if (get_index_value(index + 1) == this.GetCardLogicValue(hand_card_data[i])) {
						out_card_data[0] = hand_card_data[i];
						out_card_change_data[0] = hand_card_data[i];
						return 1;
					}
					if (get_index_value(GameConstants.GDY_MAX_INDEX - 3) == GetCardLogicValue(hand_card_data[i])) {
						out_card_data[0] = hand_card_data[i];
						out_card_change_data[0] = hand_card_data[i];
						return 1;
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}
			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_DOUBLE_HL: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}
			if (this.GetCardValue(card_data[0]) != 2) {
				int out_card_count = 0;
				for (int i = 0; i < hand_card_count; i++) {
					if (get_index_value(index + 1) == GetCardLogicValue(hand_card_data[i])
							&& hand_index[index + 1] + magic_count == 2 && hand_index[index + 1] > 0) {
						if (hand_index[index + 1] == 2) {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							out_card_data[out_card_count] = hand_card_data[i + 1];
							out_card_change_data[out_card_count++] = hand_card_data[i + 1];
							return out_card_count;
						} else {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							for (int j = 0; j < hand_card_count; j++) {
								if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = hand_card_data[i];
									return out_card_count;
								}
								if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = hand_card_data[i];
									return out_card_count;
								}
							}
						}

						return 0;
					}
					if (get_index_value(GameConstants.GDY_MAX_INDEX - 3) == GetCardLogicValue(hand_card_data[i])
							&& hand_index[GameConstants.GDY_MAX_INDEX - 3] + magic_count == 2
							&& hand_index[GameConstants.GDY_MAX_INDEX - 3] > 0) {
						if (hand_index[GameConstants.GDY_MAX_INDEX - 3] == 2) {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							out_card_data[out_card_count] = hand_card_data[i + 1];
							out_card_change_data[out_card_count++] = hand_card_data[i + 1];
							return out_card_count;
						} else {
							out_card_data[out_card_count] = hand_card_data[i];
							out_card_change_data[out_card_count++] = hand_card_data[i];
							for (int j = 0; j < hand_card_count; j++) {
								if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = hand_card_data[i];
									return out_card_count;
								}
								if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
									out_card_data[out_card_count] = hand_card_data[j];
									out_card_change_data[out_card_count++] = hand_card_data[i];
									return out_card_count;
								}
							}
						}

						return 0;
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_3_XIAOWANG_RUAN_HL: {
			int index = get_card_index(card_data[0]);

			// 获取硬炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			// 获取软炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 3 && i >= index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_3_DAWANG_RUAN_HL: {
			int index = get_card_index(card_data[0]);
			// 获取软炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 3 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}
			// 获取硬炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			// 获取3个以上炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_3_HL: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 3) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}
			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_4_XIAOWANG_RUAN_HL: {
			int index = get_card_index(card_data[0]);
			// 获取硬炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] >= 4) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}

			// 获取软炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 4 && i >= index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_4_DAWANG_RUAN_HL: {
			int index = get_card_index(card_data[0]);

			// 获取软炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 4 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}
			// 获取硬炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] >= 4) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			// 获取4个以上软炸弹
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_4_HL: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 4) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 4) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_5_XIAOWANG_RUAN_HL: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 5 && i >= index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count == 5) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 5) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 5) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 5) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_BOMB_5_DAWANG_RUAN_HL: {
			int index = get_card_index(card_data[0]);
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count == 5 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count == 5) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 5) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0 || hand_index[14] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 5) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 5) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_SINGLE_LINK_HL: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] == 3 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			int magic_count_temp = magic_count;
			magic_count_temp = magic_count;
			if (index < GameConstants.GDY_MAX_INDEX - cardCount - 1) {
				for (int i = index + 1; i > index - cardCount; i--) {
					if (hand_index[i] == 0) {
						if (magic_count_temp == 0) {
							if (index - i >= cardCount) {
								int out_card_count = 0;
								for (int j = index + 1; j < i; j++) {
									if (hand_index[j] == 0) {
										for (int x = 0; x < hand_card_count; x++) {
											if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])
													|| get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
												out_card_data[out_card_count] = hand_card_data[x];
												out_card_change_data[out_card_count++] = this.get_index_value(j);
												break;
											}
										}
									} else {
										for (int x = 0; x < hand_card_count; x++) {
											if (get_index_value(j) == GetCardLogicValue(hand_card_data[x])) {
												out_card_data[out_card_count] = hand_card_data[x];
												out_card_change_data[out_card_count++] = this.get_index_value(j);
												break;
											}
										}
									}
								}
								return out_card_count;
							}
							break;
						} else {
							magic_count_temp--;
						}
					}
					if (i == index - cardCount + 2) {
						int hand_card_data_temp[] = new int[hand_card_count];
						int out_card_count = 0;
						for (int x = 0; x < hand_card_count; x++) {
							hand_card_data_temp[x] = hand_card_data[x];
						}
						for (int j = index + 1; j >= i; j--) {
							if (hand_index[j] == 0) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data_temp[x])
											|| get_index_value(14) == GetCardLogicValue(hand_card_data_temp[x])) {
										out_card_data[out_card_count] = hand_card_data_temp[x];
										out_card_change_data[out_card_count++] = this.get_index_value(j);
										hand_card_data_temp[x] = 0;
										break;
									}
								}
							} else {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(j) == GetCardLogicValue(hand_card_data_temp[x])) {
										out_card_data[out_card_count] = hand_card_data_temp[x];
										out_card_change_data[out_card_count++] = this.get_index_value(j);
										hand_card_data_temp[x] = 0;
										break;
									}
								}
							}
						}
						return out_card_count;
					}
				}
			}

			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}

			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		case GameConstants.GDY_CT_DOUBLE_LINK_HL: {
			int index = get_card_index(card_data[0]);
			// 癞子和剩下的牌能一次性全出
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_card_count == hand_index[i] + magic_count && hand_card_count >= 3) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
									if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = hand_card_data[j];
										if (out_card_count > 2) {
											return out_card_count;
										}
									}
								}
								return out_card_count;
							}
						}
					}
				}
			}

			/////
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] == 3 && i > index) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								return out_card_count;
							}
						}
					}
				}
			}
			int magic_count_temp = magic_count;
			magic_count_temp = magic_count;
			for (int i = index + 1; i <= GameConstants.GDY_MAX_INDEX - cardCount - 1; i++) {
				if (hand_index[i] == 0) {
					if (magic_count_temp == 0) {
						if (index - i >= cardCount) {
							int out_card_count = 0;
							for (int j = index + 1; j < i; j++) {
								int count = 0;
								for (int x = 0; x < hand_card_count; x++) {
									if (get_index_value(j) == GetCardLogicValue(hand_card_data[x])) {
										out_card_data[out_card_count] = hand_card_data[x];
										out_card_change_data[out_card_count++] = this.get_index_value(j);
										count++;
										if (count == 2) {
											break;
										}
									}
								}
								if (count < 2) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])
												|| get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = this.get_index_value(j);
											if (count == 2) {
												break;
											}
										}
									}
								}

							}
							return out_card_count;
						}
					} else {
						magic_count_temp--;
					}
				}
			}
			for (int i = 0; i < GameConstants.GDY_MAX_INDEX - 2; i++) {
				if (hand_index[i] + magic_count > 2) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {

						if (get_index_value(i) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count == hand_index[i]) {
								if (hand_index[13] > 0) {
									for (int x = 0; x < hand_card_count; x++) {
										if (get_index_value(13) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
										if (get_index_value(14) == GetCardLogicValue(hand_card_data[x])) {
											out_card_data[out_card_count] = hand_card_data[x];
											out_card_change_data[out_card_count++] = hand_card_data[j];
											if (out_card_count > 2) {
												return out_card_count;
											}
										}
									}

								}
								return out_card_count;
							}
						}
					}
				}
			}
			// 王炸
			if (has_rule(GameConstants.GAME_RULE_GDY_KING_BOMB)) {
				if (hand_index[13] > 0 && hand_index[14] > 0) {
					int out_card_count = 0;
					for (int j = 0; j < hand_card_count; j++) {
						if (get_index_value(13) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
						if (get_index_value(14) == GetCardLogicValue(hand_card_data[j])) {
							out_card_data[out_card_count] = hand_card_data[j];
							out_card_change_data[out_card_count++] = hand_card_data[j];
							if (out_card_count > 2) {
								return out_card_count;
							}
						}
					}
				}
			}
			return 0;
		}
		}
		return 0;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	public int get_plane_max_index(tagAnalyseIndexResult_GDY card_data_index, int cbCardData[], int cbCardCount) {
		for (int i = GameConstants.WSK_MAX_INDEX - 4; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num * 5 == cbCardCount) {
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

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

}
