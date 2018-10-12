/**
 * 
 */
package com.cai.hp.model;

/**
 * 分析子项
 * 
 * @author xwy
 *
 */
public class AnalyseItemHP {

	public int cbCardEye[]; // 丫口索引
	public int cbWeaveKind[]; // 组合类型
	public int cbCenterCard[]; // 中心扑克
	public int cbPoint; // 组合牌的最佳点数
	public int cbGoldCard; // 选定的主金

	// 调试时用的信息
	public int cbGoldPoint[]; // 保存五种主金计算出来的分数
	public int cbKindPoint[]; // 每一种胡牌组合中每一个组合项的点数

	public AnalyseItemHP() {
		cbCardEye = new int[2];
		cbWeaveKind = new int[8];
		cbCenterCard = new int[8];

		cbGoldPoint = new int[5];
		cbCenterCard = new int[8];
	}
}
