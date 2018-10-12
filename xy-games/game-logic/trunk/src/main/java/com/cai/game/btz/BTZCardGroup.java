package com.cai.game.btz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.BTZConstants;

import protobuf.clazz.btz.BTZRsp.OpenCard_BTZ;
import protobuf.clazz.btz.BTZRsp.OpenCard_BTZ.Builder;

public class BTZCardGroup {

	public final int seat_index;
	public final BTZTable table;

	private int[] cards;
	public BTZCardType type;
	public float point;// 最大点数
	public int zhu;

	public BTZCardGroup(BTZTable table, int seat_index) {
		this.table = table;
		this.seat_index = seat_index;
		cards = new int[0];
	}

	public void reset(int[] cards, int zhu) {
		this.cards = cards;
		this.zhu = zhu;
		type = BTZCardType.getType(cards, table._game_rule_index);
		point = (BTZUtils.get_card_value(cards[0]) + BTZUtils.get_card_value(cards[1])) % 10;
	}

	/**
	 * 九点半2倍,对子3倍,幺鸡对4倍
	 * 
	 * @param zhuang
	 * @return
	 */
	public int calScore(BTZCardGroup zhuang) {
		if (table._game_type_index == GameConstants.GAME_TYPE_BTZ_YY) { // 扳砣子免检算庄家赢
			if (this.point == 0 && this.cards[0] != 0x25 && zhuang.cards[0] != 0x25 && zhuang.point == 0) {
				if (table.has_rule(BTZConstants.BTZ_RULE_DA_HE) && (this.cards[0] == zhuang.cards[0] || this.cards[0] == zhuang.cards[1])) { // 免检、两个人有一张牌相同则另一张牌也一定相同
					return 0;
				}
				return -1 * zhu;
			}
		}
		int winType = compareTo(zhuang);
		if (winType == BTZConstants.SAME && table.has_rule(BTZConstants.BTZ_RULE_DA_HE)) {
			// 打和
			return 0;
		}
		// 输的话拿的是负分
		int temp = winType == BTZConstants.WIN ? 1 : -1;
		int rate = winType == BTZConstants.WIN ? getRate() : zhuang.getRate();
		return 1 * zhu * temp * rate;
	}

	/**
	 * 八点2倍,九点3倍,对子4倍
	 * 
	 * @param zhuang
	 * @return
	 */
	public int calScoreThree(BTZCardGroup zhuang) {
		if (table._game_type_index == GameConstants.GAME_TYPE_BTZ_YY) { // 扳砣子免检算庄家赢
			if (this.point == 0 && this.cards[0] != 0x25 && zhuang.cards[0] != 0x25 && zhuang.point == 0) {
				if (table.has_rule(BTZConstants.BTZ_RULE_DA_HE) && (this.cards[0] == zhuang.cards[0] || this.cards[0] == zhuang.cards[1])) { // 免检、两个人有一张牌相同则另一张牌也一定相同
					return 0;
				}
				return -1 * zhu;
			}
		}
		int winType = compareTo(zhuang);
		if (winType == BTZConstants.SAME && table.has_rule(BTZConstants.BTZ_RULE_DA_HE)) {
			// 打和
			return 0;
		}
		// 输的话拿的是负分
		int temp = winType == BTZConstants.WIN ? 1 : -1;
		int rate = winType == BTZConstants.WIN ? getRate(this.cards) : zhuang.getRate(zhuang.cards);
		return 1 * zhu * temp * rate;
	}

	public int getRate(int cards[]) {
		if (cards[0] == cards[1]) { // 对子4倍
			return 4;
		} else if (BTZUtils.get_card_value(cards[0]) == 0.5f) {
			if (BTZUtils.get_card_value(cards[1]) == 8) {
				return 2;
			} else if (BTZUtils.get_card_value(cards[1]) == 9) {
				return 3;
			}
		} else if (BTZUtils.get_card_value(cards[1]) == 0.5f) {
			if (BTZUtils.get_card_value(cards[0]) == 8) {
				return 2;
			} else if (BTZUtils.get_card_value(cards[0]) == 9) {
				return 3;
			}
		} else {
			if (BTZUtils.get_card_value(cards[0]) + BTZUtils.get_card_value(cards[1]) == 8) {
				return 2;
			} else if (BTZUtils.get_card_value(cards[0]) + BTZUtils.get_card_value(cards[1]) == 9) {
				return 3;
			}
		}
		return 1;
	}

	public int compareTo(BTZCardGroup o) {
		if (o.type != this.type) {
			return this.type.get() > o.type.get() ? BTZConstants.WIN : BTZConstants.LOSE;
		}

		if (o.type == BTZCardType.DOUBLE) {
			if (this.cards[0] == o.cards[0]) {
				return BTZConstants.SAME;
			}
			return this.cards[0] > o.cards[0] ? BTZConstants.WIN : BTZConstants.LOSE;
		}

		if (this.point != o.point) {
			return this.point > o.point ? BTZConstants.WIN : BTZConstants.LOSE;
		} else {
			// 点数一样找出最大牌值
			for (int card : cards) {
				if (card == 0x35 || card == 0x11)
					continue;
				if (card > o.cards[0] && card > o.cards[1]) {
					return BTZConstants.WIN;
				}
			}

			for (int card : o.cards) {
				if (card == 0x35 || card == 0x11)
					continue;
				if (card > cards[0] && card > cards[1]) {
					return BTZConstants.LOSE;
				}
			}
		}

		return BTZConstants.SAME;
	}

	public int getRate() {
		return type.getRate(table, cards);
	}

	public Builder encode() {
		OpenCard_BTZ.Builder b = OpenCard_BTZ.newBuilder();
		b.setOpen(true);
		b.setSeatIndex(seat_index);
		for (int i = 0; i < cards.length; i++) {
			b.addCards(cards[i]);
		}
		b.setTime(getRate());
		b.setCardType(type.get());
		b.setPoint(point);
		// TODO Auto-generated method stub
		return b;
	}
}
