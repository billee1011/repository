package com.cai.game.mj.hunan.yuanjiang;

public enum PaiXingEnum {
	CHR_GANG_SHANG_HUA(0x00000008), // 杠爆
	CHR_QIANG_GANG_HU(0x00000010), // 抢杠胡
	CHR_PING_HU(0x00000020), // 平胡
	CHR_JIANG_JIANG_HU(0x00000040), // 将将胡
	CHR_QI_XIAO_DUI(0x00000080), // 七小对
	CHR_PENG_PENG_HU(0x00000100), // 碰碰胡
	CHR_YI_TIAO_LONG(0x00000200), // 一条龙
	CHR_QING_YI_SE(0x00000400), // 清一色
	CHR_YI_ZI_QIAO(0x00000800), // 一字翘
	CHR_ONE_HH_QI_XIAO_DUI(0x00001000), // 豪华七小对
	CHR_TWO_HH_QI_XIAO_DUI(0x00002000), // 双豪华七小对
	CHR_THREE_HH_QI_XIAO_DUI(0x00004000), // 三豪华七小对
	CHR_MEN_QING(0x00008000), // 门清
	CHR_HAI_DI(0x00010000), // 海底
	CHR_BAO_TING(0x00020000), // 报听
	CHR_TIAN_HU(0x00040000), // 天胡
	;

	private int chr;

	public int getChr() {
		return chr;
	}

	PaiXingEnum(int chr) {
		this.chr = chr;
	}
}
