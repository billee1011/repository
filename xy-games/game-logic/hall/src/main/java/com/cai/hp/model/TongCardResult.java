package com.cai.hp.model;

/**
 * 花牌统
 * 
 * @author xwy
 *
 */
public class TongCardResult {
	public int cbCardCount; // 藏牌的次数
	public int cbCardData[]; // 每次所对应的中心牌的数据

	public TongCardResult() {
		cbCardData = new int[6];
	}
}
