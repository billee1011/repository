package com.cai.game.czbg;

import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.game.czbg.CZBGConstants;

public class CZBGCardGroup {

	public CZBGTable table;

	public int point = -1; // 最大点数
	public int[] maxCard; //
	public int[] isKing; // 0: 不是癞子, 1: 小王癞子, 2: 大王癞子
	public int[] cards;
	public int[] cardTypes;
	public int[] comScore; // 比分结果
	public int[] baseScore; // 牌型得分结果

	public CZBGCardGroup() {
		this.maxCard = new int[3];
		this.cards = new int[CZBGConstants.MAX_CARD_COUNT];
		this.cardTypes = new int[3];
		this.isKing = new int[3];
		this.comScore = new int[3];
		this.baseScore = new int[3];
	}

	public void setTable(CZBGTable table) {
		this.table = table;
	}

	public void reset(int[] cards, int cardCount) {
		this.cards = Arrays.copyOf(cards, cardCount);
		table._logic.ox_sort_card_list(this.cards, cardCount);
		table._logic.get_ox_card(cards, cardCount, table._game_rule_index, this);
	}

	public void clean() {
		this.point = -1;
		Arrays.fill(maxCard, -1);
		Arrays.fill(isKing, 0);
		Arrays.fill(cardTypes, 0);
		Arrays.fill(comScore, 0);
		Arrays.fill(baseScore, 0);
	}

	/**
	 * 初始化八怪卡牌
	 */
	public void initEightCard(List<Integer> cards, int cardCount) {
		if (this.cards.length < CZBGConstants.MAX_CARD_COUNT) {
			this.cards = new int[CZBGConstants.MAX_CARD_COUNT];
		}
		if (cardCount == CZBGConstants.MAX_CARD_COUNT && cardCount == cards.size()) {
			for (int i = 0; i < cardCount; i++) {
				this.cards[i] = cards.get(i);
			}
		}
	}

	public int calScore(CZBGCardGroup other) {
		int winType = compareToOX(other);
		// 输的话拿的是负分
		int temp = winType == CZBGConstants.CZBG_WIN ? 1 : -1;
		int rate = winType == CZBGConstants.CZBG_WIN ? getRate() : other.getRate();
		return 1 * temp * rate;
	}

	public int calScoreEight(CZBGCardGroup other) {
		int value = 0;
		for (int i = 0; i < 3; i++) {
			int winType = compareToEight(other, i);
			if (winType < 0) {
				this.comScore[i] += winType;
				other.comScore[i] -= winType;
				this.comScore[i] -= other.baseScore[i];
				other.comScore[i] += other.baseScore[i];
			} else if (winType > 0) {
				this.comScore[i] += winType;
				other.comScore[i] -= winType;
				this.comScore[i] += this.baseScore[i];
				other.comScore[i] -= this.baseScore[i];
			}
			// 输的话拿的是负分
			value += this.comScore[i];
		}

		return value;
	}

	public int getRate() {
		if (point == 0 || point == 10) {
			return 3;
		} else if (point > 6) {
			return 2;
		}
		return 1;
	}

	public int compareToOX(CZBGCardGroup o) {
		if (this.point == 0) {
			this.point = 10;
		}
		if (o.point == 0) {
			o.point = 10;
		}
		if (this.point != o.point) {
			return this.point > o.point ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
		} else {
			if (table._logic.get_card_value(this.maxCard[0]) == table._logic.get_card_value(o.maxCard[0])) { // 最大牌值相同
				if (this.maxCard[0] == o.maxCard[0]) { // 花色也相同
					if (this.isKing[0] > 0 && o.isKing[0] > 0) { // 最大牌都是癞子牌则比较癞子
						return this.isKing[0] > o.isKing[0] ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
					} else if (this.isKing[0] > 0 || o.isKing[0] > 0) { // 有一个是癞子则非癞子牌赢
						return o.isKing[0] > 0 ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
					}
					return CZBGConstants.CZBG_ERROR; // 如果没有癞子牌 则不可能出现同牌
				}
				return this.maxCard[0] > o.maxCard[0] ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
			}
			return table._logic.get_card_value(this.maxCard[0]) > table._logic.get_card_value(o.maxCard[0]) ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
		}
	}

	public int compareToEight(CZBGCardGroup o, int index) {
		if (this.cardTypes[index] >= 6 && o.cardTypes[index] >= 6) {
			if (this.table.has_rule(CZBGConstants.CZBG_RULE_SAME_TYPE_BANKER_WIN)) {
				return CZBGConstants.CZBG_LOSE;
			}
			return CZBGConstants.CZBG_SAME;
		}
		if (getSortType(this.cardTypes[index]) == getSortType(o.cardTypes[index])) {
			if (table._logic.get_card_value(this.maxCard[index]) == table._logic.get_card_value(o.maxCard[index])) { // 最大牌值相同
				if (this.table.has_rule(CZBGConstants.CZBG_RULE_SAME_TYPE_COMP_COLOR)) {
					if (table._logic.get_card_color(this.maxCard[index]) == table._logic.get_card_color(o.maxCard[index])) { // 花色也相同
						if (this.isKing[index] > 0 && o.isKing[index] > 0) { // 最大牌都是癞子牌则比较癞子
							return this.isKing[index] > o.isKing[index] ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
						} else if (this.isKing[index] > 0 || o.isKing[index] > 0) { // 有一个是癞子则非癞子牌赢
							return o.isKing[index] > 0 ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
						}
						return CZBGConstants.CZBG_ERROR; // 如果没有癞子牌 则不可能出现同牌
					}
					return table._logic.get_card_color(this.maxCard[index]) > table._logic.get_card_color(o.maxCard[index]) ? CZBGConstants.CZBG_WIN
							: CZBGConstants.CZBG_LOSE; // 花色值大的 牌小
				} else if (this.table.has_rule(CZBGConstants.CZBG_RULE_SAME_TYPE_BANKER_WIN)) {
					return CZBGConstants.CZBG_LOSE;
				} else if (this.table.has_rule(CZBGConstants.CZBG_RULE_SAME_TYPE_DRAW)) {
					return CZBGConstants.CZBG_SAME;
				}
			}
			return table._logic.get_card_value(this.maxCard[index]) > table._logic.get_card_value(o.maxCard[index]) ? CZBGConstants.CZBG_WIN
					: CZBGConstants.CZBG_LOSE;
		}
		return getSortType(this.cardTypes[index]) > getSortType(o.cardTypes[index]) ? CZBGConstants.CZBG_WIN : CZBGConstants.CZBG_LOSE;
	}

	/**
	 * 获取八怪牌型的真实大小
	 * 
	 * @param type
	 * @return
	 */
	public int getSortType(int type) {
		if (type >= CZBGConstants.CZBG_CARD_TYPE_THREE_ORDER && type <= CZBGConstants.CZBG_CARD_TYPE_EIGHT) { // 八怪牌型
			return CZBGConstants.CZBG_CARD_TYPE_EIGHT;
		}
		return type;
	}

	public int getBaseCardTypeScore() {
		int value = 0;
		// for (int i = 0; i < 3; i++) {
		// value += this.baseScore[i];
		// }
		return value;
	}

}
