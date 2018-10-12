package com.cai.game.shengji.data;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.game.shengji.SJConstants;

public class tagAnalyseIndexResult_Xpsj {
	public int card_index[][] = new int[5][SJConstants.XP_SJ_MAX_INDEX];
	public int card_data[][][] = new int[5][SJConstants.XP_SJ_MAX_INDEX][GameConstants.XFGD_MAX_COUT];

	public tagAnalyseIndexResult_Xpsj() {
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < SJConstants.XP_SJ_MAX_INDEX; j++) {
				card_index[i][j] = 0;
				Arrays.fill(card_data[i][j], 0);
			}
		}
	}

	public void Reset() {
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < SJConstants.XP_SJ_MAX_INDEX; j++) {
				card_index[i][j] = 0;
				Arrays.fill(card_data[i][j], 0);
			}
		}
	}
}