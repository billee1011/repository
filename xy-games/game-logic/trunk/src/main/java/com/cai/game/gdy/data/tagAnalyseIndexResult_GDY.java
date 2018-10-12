package com.cai.game.gdy.data;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;

public class tagAnalyseIndexResult_GDY {
	public int card_index[] = new int[GameConstants.WSK_MAX_INDEX];
	public int card_data[][] = new int[GameConstants.WSK_MAX_INDEX][GameConstants.MAX_PDK_COUNT_EQ];

	public tagAnalyseIndexResult_GDY() {
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void Reset() {
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
}