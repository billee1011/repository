/**
 * 
 */
package com.cai.game.czbg;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.util.RandomUtil;

public class CZBGGameLogic {

	/**
	 * 获取数值
	 * 
	 * @param card
	 * @return
	 */
	public int get_card_value_ox(int card) {
		int card_value = card & GameConstants.LOGIC_MASK_VALUE;
		return card_value > 10 ? 10 : card_value;
	}

	public int get_card_value(int card) {
		int card_value = card & GameConstants.LOGIC_MASK_VALUE;
		return card_value == 1 ? 14 : card_value;
	}

	/**
	 * 获取数值
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	/**
	 * 获取花色
	 * 
	 * @param card
	 * @return
	 */
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public int get_real_card(int card) {
		return card & 0x0FF;
	}

	public int get_king_value(int card) {
		return (card & 0x0F);
	}

	/**
	 * 洗牌
	 * 
	 * @param return_cards
	 * @param mj_cards
	 */
	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);
	}

	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);

	}

	/***
	 * 获取牛牛倍数
	 * 
	 * @param cardData
	 * @param card_count
	 * @return
	 */
	public int get_times_one(int cardData[], int card_count, int game_rule_index, int game_type_index) {
		int times = 0;
		return times;
	}

	/***
	 * 获取牛值
	 * 
	 * @param cardData
	 * @param card_count
	 * @return
	 */
	public void get_ox_card(int cardData[], int card_count, int game_rule_index, CZBGCardGroup group) {
		int tempData[] = Arrays.copyOf(cardData, card_count);

		ox_sort_card_list(tempData, card_count);
		getMaxOXValue(tempData, card_count, group);
	}

	public boolean get_eight_card(CZBGCardGroup group) {
		// 特殊牌型判断
		// if (judgeEight(group, CZBGConstants.MAX_CARD_COUNT)) {
		// return true;
		// }

		getCardType(Arrays.copyOfRange(group.cards, 0, 2), 0, group);
		getCardType(Arrays.copyOfRange(group.cards, 2, 5), 1, group);
		getCardType(Arrays.copyOfRange(group.cards, 5, 8), 2, group);

		// 非特殊牌型 则必须满足 尾道>=中道>=头道
		if (!compare(group, 0, 1) || !compare(group, 1, 2)) {
			Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_ERROR);
			return false;
		}
		return true;
	}

	public boolean compare(CZBGCardGroup group, int first, int next) {
		if (group.cardTypes[next] > group.cardTypes[first]) {
			return true;
		}
		if (group.cardTypes[next] == group.cardTypes[first] && compare(group.maxCard[first], group.maxCard[next])) {
			return true;
		}
		return false;
	}

	public boolean compare(int cardFirst, int cardNext) {
		int valueF = get_card_value(cardFirst);
		int valueN = get_card_value(cardNext);
		if (valueF == valueN) { // 值相同则比较花色
			return get_card_color(cardNext) > get_card_color(cardFirst);
		}
		return valueN > valueF;
	}

	public boolean compareOx(int cardFirst, int cardNext) {
		int valueF = get_card_value_ox(cardFirst);
		int valueN = get_card_value_ox(cardNext);
		if (valueF == valueN) { // 值相同则比较花色
			return get_card_color(cardNext) > get_card_color(cardFirst);
		}
		return valueN > valueF;
	}

	private void getCardType(int[] card, int index, CZBGCardGroup group) {
		int maxCardCount = card.length;
		int cardValue[] = new int[15];
		int cardColor[] = new int[5];
		int kingNumber = 0;
		for (int i = 0; i < maxCardCount; i++) {
			int isKing = this.get_king_value(card[i]);
			if (isKing >= 14) {
				isKing -= 13;
				kingNumber++;
			} else {
				isKing = 0;
			}
			int realCard = this.get_real_card(card[i]);

			if (realCard > group.maxCard[index]) {
				group.maxCard[index] = realCard;
				group.isKing[index] = isKing;
			}

			cardColor[this.get_card_color(realCard)]++;
			if (0 == isKing) { // 大小王不需要计算这个
				int value = this.get_real_card_value(realCard);
				cardValue[value]++;
				if (value == 1) { // QKA辅助判断
					cardValue[14]++;
				}
			}
		}

		this.ox_sort_card_list(card, card.length);
		switch (maxCardCount) {
		case 2: // 头道只有乌龙或对子
			if (kingNumber > 1) {
				group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_DOUBLE;
				group.baseScore[index] = this.get_card_value(0x01);
				group.maxCard[index] = 0x01;
				group.isKing[index] = this.get_king_value(0x4F);
			} else if (kingNumber > 0) {
				group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_DOUBLE;
				group.baseScore[index] = this.get_card_value(card[1]);
				if (this.get_card_color(card[1]) == 0) {
					group.maxCard[index] = card[1];
				} else {
					group.maxCard[index] = 0x00 + group.baseScore[index];
					group.isKing[index] = kingNumber;
				}
				group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_DOUBLE;
				return;
			}
			if (this.get_real_card_value(card[0]) == this.get_real_card_value(card[1])) {
				group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_DOUBLE;
				group.baseScore[index] = get_card_value(card[0]);
				group.maxCard[index] = getMaxCard(card, 2);
				return;
			}
			group.maxCard[index] = getMaxCard(card, 2);
			group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_SINGLE;
			return;
		case 3: // 中道尾道有乌龙、对子、顺子、三条、同花顺
			for (int i = 0; i < 14; i++) {
				if (kingNumber > 0 && cardValue[i] == 2) {
					group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_THREE;
					if (index == 2) { // 中道
						group.baseScore[index] = CZBGConstants.CZBG_CARD_SCORE_THREE;
					} else if (index == 1) {
						group.baseScore[index] = CZBGConstants.CZBG_CARD_SCORE_THREE * 2;
					}
					if (this.get_card_color(card[2]) == 0) {
						group.maxCard[index] = card[2];
					} else {
						group.maxCard[index] = 0x00 + (this.get_card_value(card[2]) > 13 ? 1 : this.get_card_value(card[2]));
						group.isKing[index] = kingNumber;
					}
					return;
				} else if (cardValue[i] == 2) { // 对子
					group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_DOUBLE;
					if (this.get_card_value(card[0]) == this.get_card_value(card[1])) {
						group.maxCard[index] = card[0] > card[1] ? card[0] : card[1];
					} else {
						group.maxCard[index] = card[1] > card[2] ? card[1] : card[2];
					}
					return;
				} else if (cardValue[i] == 3) { // 三条
					group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_THREE;
					if (index == 2) { // 中道
						group.baseScore[index] = CZBGConstants.CZBG_CARD_SCORE_THREE;
					} else if (index == 1) {
						group.baseScore[index] = CZBGConstants.CZBG_CARD_SCORE_THREE * 2;
					}
					group.maxCard[index] = getMaxCard(card, 3);
					return;
				}
			}
			int max = this.getMaxTogetherNumber(cardValue);
			int count = maxCardCount;
			if (kingNumber > 0) {
				max = getMaxTogetherNumber(cardValue, kingNumber, card.length);
			}
			if (max == count) { // 顺子
				for (int i = 0; i < 4; i++) {
					if (cardColor[i] + kingNumber == max) {
						group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_SAME_COLOR_ORDER;
						if (index == 2) { // 中道
							group.baseScore[index] = CZBGConstants.CZBG_CARD_SCORE_FLUSH;
						} else if (index == 1) {
							group.baseScore[index] = CZBGConstants.CZBG_CARD_SCORE_FLUSH * 2;
						}
					}
				}
				switch (this.getMaxTogetherNumber(cardValue)) {
				case 1: // 有两个癞子
					group.maxCard[index] = (this.get_card_value(card[1]) > this.get_card_value(card[2]) ? card[1] : card[2]);
					break;
				case 2: // 一个癞子
					switch (this.get_card_value(card[1])) {
					case 13: // QKA
						group.maxCard[index] = card[1];
						break;
					case 2: // A23的时候是最小的顺子 最大牌是3
						group.maxCard[index] = 16 * (group.baseScore[index] > 0 ? this.get_card_value(card[1]) : 0) + 3;
						group.isKing[index] = kingNumber;
						break;
					default:
						group.maxCard[index] = card[0];
					}
					break;
				case 3:
					switch (this.get_card_value(card[0])) {
					case 13: // QKA
						group.maxCard[index] = card[2];
						break;
					default:
						group.maxCard[index] = card[0];
					}
					break;
				}
				if (group.cardTypes[index] == 0) {
					group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_ORDER;
				}
				return;
			}
			if (kingNumber > 0) {
				group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_DOUBLE;
				if (this.get_card_value(card[1]) > this.get_card_value(card[2])) {
					if (this.get_card_color(card[1]) == 0) {
						group.maxCard[index] = card[1];
					} else {
						group.maxCard[index] = 0x00 + (this.get_card_value(card[1]) > 13 ? 1 : this.get_card_value(card[1]));
						group.isKing[index] = kingNumber;
					}
				} else {
					if (this.get_card_color(card[2]) == 0) {
						group.maxCard[index] = card[2];
					} else {
						group.maxCard[index] = 0x00 + (this.get_card_value(card[2]) > 13 ? 1 : this.get_card_value(card[2]));
						group.isKing[index] = kingNumber;
					}
				}
			} else {
				group.cardTypes[index] = CZBGConstants.CZBG_CARD_TYPE_SINGLE;
				group.maxCard[index] = getMaxCard(card, 3);
			}

			break;
		}
	}

	public int getMaxCard(int[] cards, int count) {
		if (count < 1) {
			return 0;
		}
		int index = 0;
		for (int i = 1; i < count; i++) {
			if (compare(cards[index], cards[i])) {
				index = i;
			}
		}
		return cards[index];
	}

	/**
	 * 八怪牌型判断
	 * 
	 * @param cards
	 * @param maxCardCount
	 * @return
	 */
	public boolean judgeEight(CZBGCardGroup group, int maxCardCount) {
		group.clean();

		int countLaiZi = 0;
		int cardValue[] = new int[15];
		int cardColor[] = new int[5];
		for (int i = 0; i < maxCardCount; i++) {
			if (this.get_card_color(group.cards[i]) == 4) {
				countLaiZi++;
			} else {
				cardValue[this.get_real_card_value(this.get_real_card(group.cards[i]))]++;
			}
		}
		if (countLaiZi > 0) {
			int v[] = new int[5];
			for (int i = 0; i < 14; i++) {
				if (cardValue[i] == 1) {
					v[1]++;
				} else if (cardValue[i] == 2) {
					v[2]++;
				} else if (cardValue[i] == 3) {
					v[3]++;
				} else if (cardValue[i] == 4) {
					v[4]++;
				}
			}
			if (v[4] > 0 || v[3] > 0 || (v[2] > 0 && countLaiZi > 1)) { // 炸弹
				Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_BOOM);
				Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
				return true;
			}
			if (v[2] > 2 || (v[2] > 1 && countLaiZi > 1)) { // 四对
				Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_FOUR_DOUBLE);
				Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
				return true;
			}
			int cv[] = new int[15];
			countLaiZi = 0;
			for (int i = 0; i < 2; i++) {
				if (this.get_card_color(group.cards[i]) == 4) {
					countLaiZi++;
				} else {
					cv[this.get_real_card_value(this.get_real_card(group.cards[i]))]++;
				}
			}
			if (v[2] > 0) { // 之后的八怪判断不能有对子 否则不是八怪牌型
				return false;
			}
			int max = getMaxTogetherNumber(cardValue);
			if (max <= 2) { // 最大连牌小于等于2
				Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_EIGHT);
				Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
				return true;
			}
			return false;
		}

		Arrays.fill(cardValue, 0);
		for (int i = 0; i < maxCardCount; i++) {
			int isKing = this.get_king_value(group.cards[i]);
			int realCard = this.get_real_card(group.cards[i]);

			if (realCard > group.maxCard[0]) {
				Arrays.fill(group.maxCard, realCard);
				Arrays.fill(group.isKing, isKing);
			}

			cardColor[this.get_card_color(realCard)]++;
			if (isKing < 14) { // 大小王不需要计算这个
				int value = this.get_real_card_value(realCard);
				cardValue[value]++;
				if (value == 1) { // QKA辅助判断
					cardValue[14]++;
				}
			}
		}

		int cv[] = new int[15];
		for (int i = 0; i < 2; i++) {
			cv[this.get_real_card_value(this.get_real_card(group.cards[i]))]++;
			cv[14] = cv[1];
		}

		int sub = 0, boom = 0;
		for (int i = 0; i < 14; i++) {
			if (cardValue[i] == 2) {
				sub++;
			} else if (cardValue[i] == 4) {
				boom++;
			}
		}

		if (sub == 4) { // 四对
			Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_FOUR_DOUBLE);
			Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
			return true;
		}

		if (boom > 0) { // 炸弹
			Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_BOOM);
			Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
			return true;
		}
		// 八怪判断 不能有三张以上同花 不能有对子 最大连牌不能超过2
		for (int i = 0; i < 15; i++) {
			if (cardValue[i] > 1) {
				return false;
			}
		}
		int max = getMaxTogetherNumber(cardValue);
		if (max < 3) {
			Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_EIGHT);
			Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
			return true;
		}

		if (judgeQuanShun(group.cards, maxCardCount)) {
			Arrays.fill(group.cardTypes, CZBGConstants.CZBG_CARD_TYPE_THREE_ORDER);
			Arrays.fill(group.baseScore, CZBGConstants.CZBG_CARD_BAO_DAO);
			return true;
		}

		return false;
	}

	/**
	 * 全顺判断
	 * 
	 * @param cards
	 * @param maxCardCount
	 * @return
	 */
	public boolean judgeQuanShun(int[] cards, int maxCardCount) {
		if (maxCardCount < 1) {
			return false;
		}
		int tempCards[] = Arrays.copyOf(cards, maxCardCount);
		int kingNumber = 0;
		int cardValue[] = new int[15];

		if (maxCardCount == 2) {
			kingNumber = this.getKingNumber(cards, cardValue, maxCardCount);
			return this.getMaxTogetherNumber(cardValue, kingNumber, maxCardCount) == maxCardCount;
		} else {
			for (int i = 0; i < maxCardCount; i++) {
				for (int j = i + 1; j < maxCardCount; j++) {
					for (int k = j + 1; k < maxCardCount; k++) {
						Arrays.fill(cardValue, 0);
						int continuityCards[] = { tempCards[i], tempCards[j], tempCards[k] };
						kingNumber = this.getKingNumber(continuityCards, cardValue, 3);
						if (this.getMaxTogetherNumber(cardValue, kingNumber, 3) == 3) {
							int c[] = new int[maxCardCount - 3];
							int count = 0;
							for (int z = 0; z < maxCardCount; z++) {
								if (z == i || z == j || z == k) {
									continue;
								}
								c[count++] = cards[z];
							}
							if (judgeQuanShun(c, count)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public int getKingNumber(int cards[], int cardValue[], int count) {
		int kingNumber = 0;
		for (int i = 0; i < count; i++) {
			int isKing = this.get_card_color(cards[i]);
			int realCard = this.get_real_card(cards[i]);

			if (isKing < 4) { // 大小王不需要计算这个
				int value = this.get_real_card_value(realCard);
				cardValue[value]++;
				if (value == 1) { // QKA辅助判断
					cardValue[14]++;
				}
			} else {
				kingNumber++;
			}
		}
		return kingNumber;
	}

	/**
	 * 获取最大连牌
	 * 
	 * @return
	 */
	public int getMaxTogetherNumber(int[] cardValue) {
		int max = 1;
		for (int i = 0; i < 15; i++) {
			int airplanceLen = 1;
			int value = cardValue[i];
			if (value == 0) {
				continue;
			}
			for (int j = i + 1; j < 15; j++) {
				if (cardValue[j] == 0) { // 不连续
					break;
				}
				airplanceLen++;
			}
			if (airplanceLen > max) {
				max = airplanceLen;
			}
			i += airplanceLen - 1;
		}
		return max;
	}

	public int getMaxTogetherNumber(int[] cardValue, int countLaiZi, int count) {
		if ((countLaiZi == 1 && count == 2) || (countLaiZi == 2 && count == 2)) {
			return 2;
		} else if (countLaiZi == 2 && count == 3) {
			return 3;
		}

		int max = 1;
		for (int i = 0; i < 15; i++) {
			int airplanceLen = 1;
			int value = cardValue[i];
			if (value == 0) {
				continue;
			}
			for (int j = i + 1; j < 15; j++) {
				if (cardValue[j] == 0) { // 不连续
					break;
				}
				airplanceLen++;
			}
			if (airplanceLen > max) {
				max = airplanceLen;
			}
			i += airplanceLen - 1;
		}
		if (3 == count && countLaiZi == 1) {
			if (2 == max) {
				return 3;
			}
			int first = 0, last = 0;
			for (int i = 0; i < 14; i++) {
				if (cardValue[i] > 0) {
					if (first == 0) {
						first = i;
					} else {
						last = i;
					}
				}
			}
			if (first == 1) {
				if (last == 3 || last == 12) { // 123/QKA
					return 3;
				}
			} else if (last - first == 2) {
				return 3;
			}
		}
		return max;
	}

	/**
	 * 获取最大牛值
	 * 
	 * @param card
	 * @param cardCount
	 * @return
	 */
	private int getMaxOXValue(int[] card, int cardCount, CZBGCardGroup group) {
		int cardsTotal = 0, cow = -1;
		group.point = -1;

		if (cardCount == 5) {
			group.maxCard[0] = 0;
			int count = 0;
			for (int i = 0; i < cardCount; i++) {
				if (4 == this.get_card_color(card[i])) {
					count++;
				} else {
					group.maxCard[0] = this.get_real_card_value(group.maxCard[0]) < this.get_real_card_value(card[i]) ? card[i] : group.maxCard[0];
				}
				group.cards[i] = card[i];
			}
			if (count == 2) { // 两个癞子 找出最大牌
				group.point = 10;
				this.ox_sort_card_list(card, cardCount);
				for (int i = 2; i < cardCount; i++) {
					for (int j = i + 1; j < cardCount; j++) {
						int value = get_card_value_ox(card[i]) + get_card_value_ox(card[j]);
						if (value % 10 != 0) {
							for (int z = 2; z < cardCount; z++) {
								if (z == i || z == j) {
									continue;
								}
								int vz = get_card_value_ox(card[z]);
								if (vz % 10 == 0) {
									if (group.maxCard[0] != 0x3d) {
										group.maxCard[0] = 0x3d;
										group.isKing[0] = 15;
									}
									break;
								} else {
									if (this.get_real_card_value(group.maxCard[0]) < this.get_real_card_value(10 - vz + 0x30)) {
										group.maxCard[0] = this.get_real_card_value(10 - vz + 0x30);
										group.isKing[0] = 15;
									}
								}
							}
							if (this.get_real_card_value(group.maxCard[0]) < this.get_real_card_value(10 - value + 0x30)) {
								group.maxCard[0] = this.get_real_card_value(10 - value + 0x30);
								group.isKing[0] = 15;
							}
						} else {
							if (group.maxCard[0] != 0x3d) {
								group.maxCard[0] = 0x3d;
								group.isKing[0] = 15;
							}
							break;
						}
					}
				}
				return 10;
			}
		}

		for (int i = 0; i < card.length; i++) {
			if (get_card_color(card[i]) < 4) {
				cardsTotal += get_card_value_ox(card[i]);
			}
		}

		for (int i = 0; i < cardCount; i++) {
			int firstCard = card[i];
			if (this.get_real_card_value(group.maxCard[0]) < this.get_real_card_value(firstCard) && get_card_color(firstCard) < 4) {
				group.maxCard[0] = firstCard;
				group.isKing[0] = 0;
			}
			if (get_card_color(firstCard) == 4) { // 大小王
				if (group.table.has_rule(CZBGConstants.CZBG_RULE_TRANSFORM_WITH_KING)) {
					for (int k = 0; k < 13; k++) {
						int tempCard = 0x31 + k;
						cardsTotal += get_card_value_ox(tempCard);
						for (int j = i + 1; j < cardCount; j++) {
							if ((cardsTotal - get_card_value_ox(tempCard) - get_card_value_ox(card[j])) % 10 == 0) {
								cow = (get_card_value_ox(tempCard) + get_card_value_ox(card[j])) % 10;
								if (cow == 0) {
									cow = 10;
								}
								if (cow >= group.point) {
									if (compareOx(group.maxCard[0], tempCard)) {
										group.maxCard[0] = tempCard;
										group.isKing[0] = get_real_card_value(tempCard);
									}
									group.point = cow;
								}

								group.cards[0] = card[i];
								group.cards[1] = card[j];
								int number = 2;
								for (int z = 0; z < cardCount; z++) {
									if (z == i || z == j) {
										continue;
									}
									group.cards[number++] = card[z];
								}
							}
						}
						for (int j = i + 1; j < cardCount; j++) {
							for (int z = j + 1; z < cardCount; z++) {
								if ((cardsTotal - get_card_value_ox(card[j]) - get_card_value_ox(card[z])) % 10 == 0) {
									cow = (get_card_value_ox(card[j]) + get_card_value_ox(card[z])) % 10;
									if (cow == 0) {
										cow = 10;
									}
									if (cow >= group.point) {
										if (compareOx(group.maxCard[0], tempCard)) {
											group.maxCard[0] = tempCard;
											group.isKing[0] = get_card_value_ox(card[i]) > 13 ? get_card_value_ox(card[i]) : 0;
										}
										group.point = cow;

										group.cards[0] = card[z];
										group.cards[1] = card[j];
										int number = 2;
										for (int b = 0; b < cardCount; b++) {
											if (b == z || b == j) {
												continue;
											}
											group.cards[number++] = card[b];
										}
									}
								}
							}
						}
						cardsTotal -= get_card_value_ox(tempCard);
					}
				} else {
					return CZBGConstants.CZBG_ERROR;
				}
			} else {
				for (int j = i + 1; j < cardCount; j++) {
					if ((cardsTotal - get_card_value_ox(firstCard) - get_card_value_ox(card[j])) % 10 == 0) {
						cow = (get_card_value_ox(firstCard) + get_card_value_ox(card[j])) % 10;
						if (cow == 0) {
							cow = 10;
						}
						group.cards[0] = card[i];
						group.cards[1] = card[j];
						int number = 2;
						for (int z = 0; z < cardCount; z++) {
							if (z == i || z == j) {
								continue;
							}
							group.cards[number++] = card[z];
						}
						if (cow >= group.point) {
							group.point = cow;
							group.maxCard[0] = this.getMaxCard(group.cards, cardCount);
						}
						break;
					}
				}
			}
		}

		return group.point;
	}

	/***
	 * //排列扑克
	 * 
	 * @param cardData
	 * @param card_count
	 * @return
	 */
	public void ox_sort_card_list(int cardData[], int card_count) {
		int logic_value[] = new int[CZBGConstants.COLOR_COUNT];
		for (int i = 0; i < card_count; i++) {
			logic_value[i] = get_real_card_value(cardData[i]);
		}
		boolean sorted = true;
		int temp_date, last = card_count - 1;
		do {
			sorted = true;
			for (int i = 0; i < last; i++) {
				if ((logic_value[i] < logic_value[i + 1]) || ((logic_value[i] == logic_value[i + 1]) && (cardData[i] < cardData[i + 1]))) {
					// 交换位置
					temp_date = cardData[i];
					cardData[i] = cardData[i + 1];
					cardData[i + 1] = temp_date;
					temp_date = logic_value[i];
					logic_value[i] = logic_value[i + 1];
					logic_value[i + 1] = temp_date;
					sorted = false;
				}
			}
			last--;
		} while (sorted == false);
		return;
	}

	public static void main(String[] args) {
		CZBGGameLogic logic = new CZBGGameLogic();
		// int cards[] = { 0x11, 0x41, 0x22, 0x23, 0x34 };
		int cards[] = { 0x21, 0x25, 0x4E, 0x11, 0x02, 0x03, 0x4F, 0x26, };

		CZBGCardGroup group = new CZBGCardGroup();
		// System.out.println(logic.getMaxOXValue(cards, cards.length, group));
		for (int i = 0; i < 8; i++) {
			group.cards[i] = cards[i];
		}
		// group.reset(cards, cards.length);
		logic.get_eight_card(group);

		for (int i = 0; i < 3; i++) {
			System.out.println(group.cardTypes[i] + " " + group.maxCard[i]);
		}
		System.out.println(logic.judgeQuanShun(cards, cards.length));

		int c[] = { 0x4f, 0x4e, 0x01, 0x32 };
		logic.ox_sort_card_list(c, c.length);
		for (int i = 0; i < c.length; i++) {
			System.out.print(c[i] + " ");
		}
	}

}
