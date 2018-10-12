package com.cai.common.domain;


//杠牌结果
public class GangCardResult {
	public int cbCardCount;//扑克数目
	public int cbCardData[] ;//扑克数据
	public int isPublic[] ;//
	public int type[] ;//
	public GangCardResult(){
		cbCardData =  new int[4];
		isPublic = new int[4];
		type = new int[4];
	}
}
