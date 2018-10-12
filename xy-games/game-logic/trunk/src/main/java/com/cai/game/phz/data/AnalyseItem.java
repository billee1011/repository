package com.cai.game.phz.data;

//分析子项
public class AnalyseItem {
	public int cbCardEye;//// 牌眼扑克
	public int cbMenEye[]=new int[2];//// 牌眼扑克
	public boolean bMagicEye;// 牌眼是否是王霸
	public int cbWeaveKind[] = new int[7];// 组合类型
	public int cbCenterCard[] = new int[7];// 中心扑克
	public int cbCardData[][] = new int[7][4]; // 实际扑克
	public int hu_xi[]       =  new int[7];// 计算胡息

	public int cbPoint;// 组合牌的最佳点数;

	public boolean curCardEye;// 当前摸的牌是否是牌眼
	public boolean isShuangDui;// 牌眼 true双对--判断碰碰胡
	public int eyeKind;// 牌眼 组合类型
	public int eyeCenterCard;// 牌眼 中心扑克
	public int cbHuXiCount;	//胡息
}