package com.cai.game.mj.sichuan;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.util.Pair;

public class WeaveInfo {
	public Pair<Integer, List<Integer>> pair;
	public List<Integer> cardPositions = new ArrayList<>();

	public WeaveInfo(Pair<Integer, List<Integer>> pair, int card_position_1, int card_position_2, int card_position_3) {
		this.pair = pair;

		if (card_position_1 >= 0)
			cardPositions.add(card_position_1);
		if (card_position_2 >= 0)
			cardPositions.add(card_position_2);
		if (card_position_3 >= 0)
			cardPositions.add(card_position_3);
	}
}
