package com.cai.game.shengji.data;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;

//分析子项
public class tagAnalyseCardType {
	public int card_data[][]=new int[GameConstants.XFGD_MAX_COUT][GameConstants.XFGD_MAX_COUT];
	public int type[] = new int[GameConstants.XFGD_MAX_COUT];
	public int count[] = new int[GameConstants.XFGD_MAX_COUT];
	public int type_count=0;
	public tagAnalyseCardType(){
		for(int i=0;i<GameConstants.XFGD_MAX_COUT;i++){
			type[i]=0;
			count[i]=0;
			Arrays.fill(card_data[i],0);
		}
		type_count=0;
	}
	public void Reset(){
		for(int i=0;i<GameConstants.XFGD_MAX_COUT;i++){
			type[i]=0;
			count[i]=0;
			Arrays.fill(card_data[i],0);
		}
		type_count=0;
	}
}