package com.cai.game.hh.util;

import java.util.ArrayList;
import java.util.List;

public class WeaveInfo {
	public Triple<Integer, Integer, List<Integer>> triple;
	public List<Integer> cardPositions = new ArrayList<>();

	public WeaveInfo(Triple<Integer, Integer, List<Integer>> triple, int card_position_1, int card_position_2, int card_position_3,
			int card_position_4) {
		this.triple = triple;

		if (card_position_1 >= 0)
			cardPositions.add(card_position_1);
		if (card_position_2 >= 0)
			cardPositions.add(card_position_2);
		if (card_position_3 >= 0)
			cardPositions.add(card_position_3);
		if (card_position_4 >= 0)
			cardPositions.add(card_position_4);
	}

	@Override
	public String toString() {
		return "WeaveInfo [" + triple.toString() + ", CardPositions:" + cardPositions.toString() + "] ";
	}
}
