/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import com.cai.common.util.Bits;

/**
 * 俱乐部身份
 *
 * @author wu_hc date: 2017年12月25日 下午8:22:17 <br/>
 */
public enum EClubIdentity {

	/**
	 *
	 */
	OBSERVER(Bits.byte_negative_2),

	/**
	 *
	 */
	DEFRIEND(Bits.byte_negative_1),

	/**
	 *
	 */
	COMMONER(Bits.byte_0),

	/**
	 *
	 */
	MANAGER(Bits.byte_1),

	/**
	 *
	 */
	CREATOR(Bits.byte_2);

	private final byte identity;

	EClubIdentity(byte identity) {
		this.identity = identity;
	}

	/**
	 * @param identity
	 * @return
	 */
	public static EClubIdentity of(int identity) {
		for (EClubIdentity i : EClubIdentity.values()) {
			if (identity == i.identity) {
				return i;
			}
		}
		return null;
	}

	public byte identify() {
		return this.identity;
	}

	/**
	 * 是否创建者角色
	 *
	 * @param identify
	 * @return
	 */
	public static boolean isCreator(byte identify) {
		return EClubIdentity.CREATOR.identify() == identify;
	}

	/**
	 * 是否管理员
	 *
	 * @param identify
	 * @return
	 */
	public static boolean isManager(byte identify) {
		return EClubIdentity.CREATOR.identify() == identify || EClubIdentity.MANAGER.identify() == identify || EClubIdentity.OBSERVER.identify() == identify;
		// return identify >= EClubIdentity.MANAGER.identify();
	}

	/**
	 * 黑名单
	 *
	 * @param identify
	 * @return
	 */
	public static boolean isDefriend(byte identify) {
		return identify <= EClubIdentity.DEFRIEND.identity;
	}
}
