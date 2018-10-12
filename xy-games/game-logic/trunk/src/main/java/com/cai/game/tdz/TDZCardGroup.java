package com.cai.game.tdz;

import com.cai.common.constant.game.TDZConstants;

import protobuf.clazz.tdz.TDZRsp.OpenCard_TDZ;
import protobuf.clazz.tdz.TDZRsp.OpenCard_TDZ.Builder;

public class TDZCardGroup {

	public final int seat_index;
	public final TDZTable table;

	private int[] cards;
	public TDZCardType type;
	public float point;// 最大点数
	public int zhu;

	public TDZCardGroup(TDZTable table, int seat_index) {
		this.table = table;
		this.seat_index = seat_index;
		cards = new int[0];
	}

	public void reset(int[] cards, int zhu) {
		this.cards = cards;
		this.zhu = zhu;
		type = TDZCardType.getType(cards, table._game_rule_index);
		point = (TDZUtils.get_card_value(cards[0]) + TDZUtils.get_card_value(cards[1])) % 10;
	}

	public int calScore(TDZCardGroup group) {
		int winType = compareTo(group);
		if (winType == TDZConstants.SAME && table.has_rule(TDZConstants.TDZ_RULE_DA_HE)) { // 打和
			return 0;
		}
		// 输的话拿的是负分
		int temp = winType == TDZConstants.WIN ? 1 : -1;
		int rate = winType == TDZConstants.WIN ? getRate() : 1; // 庄家赢不翻倍
		return 1 * zhu * temp * rate;
	}

	public int compareTo(TDZCardGroup o) {
		if (o.type != this.type) {
			return this.type.get() > o.type.get() ? TDZConstants.WIN : TDZConstants.LOSE;
		}

		if (o.type == TDZCardType.DOUBLE) {
			if (this.cards[0] == o.cards[0]) {
				return TDZConstants.SAME;
			}
			return this.cards[0] > o.cards[0] ? TDZConstants.WIN : TDZConstants.LOSE;
		}

		if (this.point != o.point) {
			return this.point > o.point ? TDZConstants.WIN : TDZConstants.LOSE;
		}

		return TDZConstants.SAME;
	}

	public int getRate() {
		return type.getRate(table, cards);
	}

	public Builder encode() {
		OpenCard_TDZ.Builder b = OpenCard_TDZ.newBuilder();
		b.setOpen(true);
		b.setSeatIndex(seat_index);
		for (int i = 0; i < cards.length; i++) {
			b.addCards(cards[i]);
		}
		b.setTime(getRate());
		b.setCardType(type.get());
		b.setPoint(point);
		return b;
	}
}
