/**
 * 
 */
package com.cai.hp.model;

/**
 * 胡牌类型
 * 
 * @author xwy
 */
public class ChiChuItem {
	public int cbCenterCard[]; // 8种组合的中心牌值
	public int cbWeaveKind[]; // 8种组合的操作类型
	public int cbYaKou[]; // 一对丫口的值
	public int cbHuScore; // 胡牌的分数
	public int cbGoldCard; // 胡牌的主精牌

	public ChiChuItem() {
		cbCenterCard = new int[8];
		cbWeaveKind = new int[8];
		cbYaKou = new int[2];
	}
}
