package com.cai.game.wsk.gzhbzp;

public class Constants_GZHBZP {
	/**
	 * 牌值为2的牌的索引，逻辑值为 2+13，索引为 牌值 - 3
	 */
	public static final int INDEX_CARD_TWO = 12;
	/**
	 * 三
	 */
	public static final int CARD_THREE = 3;
	/**
	 * 五
	 */
	public static final int CARD_FIVE = 5;
	/**
	 * 十
	 */
	public static final int CARD_TEN = 10;
	/**
	 * K
	 */
	public static final int CARD_THIRTEEN = 13;
	/**
	 * 牌值转换的时候，特殊处理的牌值
	 */
	public static final int SPECIAL_CARD_TYPE = 0x100;
	/**
	 * 通城，最大索引为16，排序的时候，只排0-12的牌
	 */
	public static final int INDEX_FENCE = 4;
	/**
	 * 黑桃五。黑 Space，红 Heart，梅 Club，方 Diamond
	 */
	public static final int SPACE_CARD_FIVE = 0x35;
	/**
	 * 小王
	 */
	public static final int CARD_SMALL_MAGIC = 0x4E;
	/**
	 * 大王
	 */
	public static final int CARD_BIG_MAGIC = 0x4F;
	/**
	 * 牌值的Mask
	 */
	public static final int CARD_MASK = 0x10;
	/**
	 * 黑桃的夜色值：3
	 */
	public static final int COLOR_SPACE = 3;
	/**
	 * 五十K的牌值和，28=5+10+13
	 */
	public static final int SUM_WSK = 28;
	/**
	 * 花牌
	 */
	public static final int FLOWER_CARD = 0x5F;

	// 五十K 2幅扑克 一共108张
	public static final int CARD_DATA_WSK[] = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块A-K
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花A-K
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃A-K
			0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃A-K
			0x4E, 0x4F, // 大王 小王
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块A-K
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花A-K
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃A-K
			0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃A-K
			0x4E, 0x4F, // 大王 小王
	};

	// 五十K 2幅扑克+1张花牌 一共109张
	public static final int CARD_DATA_WSK_ONE_MAGIC[] = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块A-K
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花A-K
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃A-K
			0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃A-K
			0x4E, 0x4F, // 大王 小王
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块A-K
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花A-K
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃A-K
			0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃A-K
			0x4E, 0x4F, // 大王 小王
			0x5F, // 花牌
	};

	// 五十K 2幅扑克+2张花牌 一共110张
	public static final int CARD_DATA_WSK_TWO_MAGIC[] = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块A-K
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花A-K
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃A-K
			0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃A-K
			0x4E, 0x4F, // 大王 小王
			0x5F, // 花牌
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块A-K
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花A-K
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃A-K
			0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃A-K
			0x4E, 0x4F, // 大王 小王
			0x5F, // 花牌
	};
}
