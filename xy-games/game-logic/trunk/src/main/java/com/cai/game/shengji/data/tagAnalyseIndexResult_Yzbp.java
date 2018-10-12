package com.cai.game.shengji.data;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;

public class tagAnalyseIndexResult_Yzbp {
	public int card_index[][] = new int[5][GameConstants.XFGD_MAX_INDEX];
	public int card_data[][][] = new int[5][GameConstants.XFGD_MAX_INDEX][GameConstants.XFGD_MAX_COUT];

	public tagAnalyseIndexResult_Yzbp() {
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < GameConstants.XFGD_MAX_INDEX; j++) {
				card_index[i][j] = 0;
				Arrays.fill(card_data[i][j], 0);
			}
		}
	}

	public void Reset() {
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < GameConstants.XFGD_MAX_INDEX; j++) {
				card_index[i][j] = 0;
				Arrays.fill(card_data[i][j], 0);
			}
		}
	}
};