package com.cai.game.mj.sichuan;

public enum ScoreRowType {
	JIE_PAO(1, "接炮"), //
	ZI_MO(2, "自摸"), //
	BA_GANG_RUAN_GANG(3, "巴杠(软杠)"), //
	BA_GANG_WU_JI(4, "巴杠(无鸡)"), //
	MING_GANG_RUAN_GANG(5, "明杠(软杠)"), //
	MING_GANG_WU_JI(6, "明杠(无鸡)"), //
	AN_GANG_RUAN_GANG(7, "暗杠(软杠)"), //
	AN_GANG_WU_JI(8, "暗杠(无鸡)"), //
	SI_JI_FA_CAI(9, "四鸡发财"), //
	CHA_JIAO(10, "查叫"), //
	//
	JIE_PAO_PING_HU(11, "接炮(平胡)"), //
	JIE_PAO_PENG_PENG_HU(12, "接炮(碰碰胡)"), //
	JIE_PAO_QING_YI_SE(13, "接炮(清一色)"), //
	JIE_PAO_PENG_PENG_DIAO(14, "接炮(碰碰钓)"), //
	JIE_PAO_DAI_YAO_JIU(15, "接炮(带幺九)"), //
	JIE_PAO_QI_DUI(16, "接炮(七对)"), //
	JIE_PAO_QING_PENG_PENG_HU(17, "接炮(清碰碰胡)"), //
	JIE_PAO_QING_PENG_PENG_DIAO(18, "接炮(清碰碰钓)"), //
	JIE_PAO_QING_QI_DUI(19, "接炮(清七对)"), //
	JIE_PAO_DI_HU(20, "接炮(地胡)"), //
	//
	ZI_MO_PING_HU(21, "自摸(平胡)"), //
	ZI_MO_PENG_PENG_HU(22, "自摸(碰碰胡)"), //
	ZI_MO_QING_YI_SE(23, "自摸(清一色)"), //
	ZI_MO_PENG_PENG_DIAO(24, "自摸(碰碰钓)"), //
	ZI_MO_DAI_YAO_JIU(25, "自摸(带幺九)"), //
	ZI_MO_QI_DUI(26, "自摸(七对)"), //
	ZI_MO_QING_PENG_PENG_HU(27, "自摸(清碰碰胡)"), //
	ZI_MO_QING_PENG_PENG_DIAO(28, "自摸(清碰碰钓)"), //
	ZI_MO_QING_QI_DUI(29, "自摸(清七对)"), //
	ZI_MO_TIAN_HU(30, "自摸(天胡)"), //
	//
	CHA_JIAO_PING_HU(31, "查叫(平胡)"), //
	CHA_JIAO_PENG_PENG_HU(32, "查叫(碰碰胡)"), //
	CHA_JIAO_QING_YI_SE(33, "查叫(清一色)"), //
	CHA_JIAO_PENG_PENG_DIAO(34, "查叫(碰碰钓)"), //
	CHA_JIAO_DAI_YAO_JIU(35, "查叫(带幺九)"), //
	CHA_JIAO_QI_DUI(36, "查叫(七对)"), //
	CHA_JIAO_QING_PENG_PENG_HU(37, "查叫(清碰碰胡)"), //
	CHA_JIAO_QING_PENG_PENG_DIAO(38, "查叫(清碰碰钓)"), //
	CHA_JIAO_QING_QI_DUI(39, "查叫(清七对)"), //
	//
	CHA_HUA_ZHU(40, "查花猪"), //
	//
	AN_GANG_XIA_YU(41, "暗杠(下雨)"), //
	WAN_GANG_GUA_FENG(42, "弯杠(刮风)"), //
	ZHI_GANG_GUA_FENG(43, "直杠(刮风)"), //
	;

	int type;
	String desc;

	ScoreRowType(int type, String desc) {
		this.type = type;
		this.desc = desc;
	}

	public int getType() {
		return type;
	}

	public String getDesc() {
		return desc;
	}
}
