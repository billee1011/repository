/**
 * 
 */
package com.cai.hp.model;

/**
 * 胡牌结果
 * 
 * @author xwy
 */
public class HuCardResult {
	public boolean IsHu; // 是否可以胡牌
	public int HuScore; // 胡牌的分数
	public int bHuPoint; // 胡点
	public int bRealGold; // 主金
	AnalyseItemHP analyseItem; // 胡牌时最佳组合
}
