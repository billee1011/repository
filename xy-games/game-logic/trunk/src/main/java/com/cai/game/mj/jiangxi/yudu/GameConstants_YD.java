package com.cai.game.mj.jiangxi.yudu;


import com.cai.common.constant.GameConstants;

public class GameConstants_YD extends GameConstants {

	public static final int GAME_RULE_PLAYER_4 = 1; // 四人场
	public static final int GAME_RULE_PLAYER_3 = 2; // 三人场
	public static final int GAME_RULE_BASE1 = 3; // 1分底
	public static final int GAME_RULE_BASE2 = 4; //2分底
	public static final int GAME_RULE_BASE5 = 5; // 5分底

	
	public static final int CHR_PING_HU = 0x00000001; // 平胡
	public static final int CHR_WUBAO_HU = 0x00000002; // 无宝
	public static final int CHR_QI_DUI = 0x00000004; // 七对
	public static final int CHR_DA_QI_DUI = 0x00000008; // 大七对
	public static final int CHR_QUAN_QIU_REN = 0x00000010; // 全求人
	public static final int CHR_SHI_SAN_LAN = 0x00000020; //十三烂
	public static final int CHR_QI_XING_SHI_SAN_LAN = 0x00000040; // 七星十三烂
	public static final int CHR_TIAN_HU = 0x00000080; // 天胡
	public static final int CHR_DI_HU = 0x00000100; // 地胡
	
	public static final int CHR_ZI_MO = 0x01000000; // 自摸
	public static final int CHR_JIE_PAO = 0x00000400;// 接炮
	public static final int CHR_GANG_SHANG_KAI_HUA = 0x00000800;//杠上开花
	public static final int CHR_GANG_SHANG_PAO = 0x00001000;//杠上炮
	public static final int CHR_FANG_PAO=0x00002000;// 放炮
	public static final int CHR_QIANG_GANG = 0x00004000;// 抢杠
	
    public static final int HU_CARD_TYPE_ZI_MO = 7; // 自摸
    public static final int HU_CARD_TYPE_JIE_PAO = 8; // 接炮
    public static final int HU_CARD_TYPE_QIANG_GANG = 9; // 抢杠
    public static final int HU_CARD_TYPE_GANG_SHANG_KAI_HUA = 10;//杠上开花
    public static final int HU_CARD_TYPE_GAME_GANG_SHANG_PAO = 11;//杠上炮
	
	// 全部的麻将数据
	public static final int CARD_DATA_MAX[] = new int[] { 
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子

			0x31, 0x31, 0x31, 0x31, // 东风
			0x32, 0x32, 0x32, 0x32, // 西风
			0x33, 0x33, 0x33, 0x33, // 南风
			0x34, 0x34, 0x34, 0x34, // 北风
			0x35, 0x35, 0x35, 0x35, // 红中
			0x36, 0x36, 0x36, 0x36, // 绿发
			0x37, 0x37, 0x37, 0x37, // 白板
	};
}
