/**
 * 
 */
package com.cai.hp.model;

/**
 * 类型子项
 * 
 * @author xwy
 *
 */
public class KindItem {
	public int cbWeaveKind;// 组合类型
	public int cbCenterCard;// 中心扑克
	public int cbCardIndex[] = new int[5];// 扑克索引
	public int cbValidIndex[] = new int[5];// 实际扑克索引
	
	public int cbCardCount;
}
