/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import com.cai.common.define.ITypeStatus;
import com.cai.common.util.Bits;

/**
 * 
 * 俱乐部某个规则的一些设置
 *
 * @author wu_hc date: 2017年11月2日 下午2:34:57 <br/>
 */
public enum ERuleSettingStatus implements ITypeStatus {

	NONE((byte) -1),

	/** 疲劳值开关 0关闭 1打开 */
	TIRE_VALUE_SWITCH(Bits.byte_0),
	
	/** 局数限制开关 0关闭 1打开 */
	GAME_ROUND_LIMIT_SWITCH(Bits.byte_1),

	/** 亲友圈福卡限制开关 0关闭 1打开 */
	CLUB_WELFARE_SWITCH(Bits.byte_2);

	// [0,32)
	private final byte position;

	private ERuleSettingStatus(byte position) {
		this.position = position;
	}

	@Override
	public byte position() {
		return position;
	}

	/**
	 * 
	 * @param position
	 * @return
	 */
	public static ERuleSettingStatus of(int position) {
		for (ERuleSettingStatus type : ERuleSettingStatus.values()) {
			if (position == type.position()) {
				return type;
			}
		}
		return NONE;
	}
}
