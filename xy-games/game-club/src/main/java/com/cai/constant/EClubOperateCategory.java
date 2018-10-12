/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

/**
 * 
 *
 * @author wu_hc date: 2017年11月2日 下午2:34:57 <br/>
 */
public enum EClubOperateCategory {

	NONE((byte) -1), RULE_ADD((byte) 1), RULE_DEL((byte) 2), RULE_UPDATE((byte) 3), CLUB_DEL((byte) 4), CLUB_FREEZE((byte) 5);

	private final byte category;

	EClubOperateCategory(byte category) {
		this.category = category;
	}

	public final byte category() {
		return category;
	}

	public static EClubOperateCategory of(int category) {
		for (EClubOperateCategory type : EClubOperateCategory.values()) {
			if (category == type.category) {
				return type;
			}
		}
		return NONE;
	}
}
