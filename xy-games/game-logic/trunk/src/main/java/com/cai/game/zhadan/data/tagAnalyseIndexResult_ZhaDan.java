package com.cai.game.zhadan.data;

import java.util.Arrays;

import com.cai.game.zhadan.ZhaDanConstants;

public class tagAnalyseIndexResult_ZhaDan {
	public int card_index[] = new int[ZhaDanConstants.ZHADAN_MAX_INDEX];
	public int card_data[][] = new int[ZhaDanConstants.ZHADAN_MAX_INDEX][ZhaDanConstants.ZHADAN_MAX_COUNT];

	public tagAnalyseIndexResult_ZhaDan() {
		for (int i = 0; i < ZhaDanConstants.ZHADAN_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void Reset() {
		for (int i = 0; i < ZhaDanConstants.ZHADAN_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
}