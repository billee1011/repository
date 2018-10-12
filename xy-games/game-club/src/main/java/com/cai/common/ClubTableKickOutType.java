package com.cai.common;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/18 17:31
 */
public interface ClubTableKickOutType {
	/**
	 * 主动踢出
	 */
	int ACTIVE_KICK = 1;
	/**
	 * 切换包间模式时踢出
	 */
	int RULE_TABLE_MODE_SWIFT = 2;
	/**
	 * 多包间模式下未准备自动踢出
	 */
	int AUTO_KICK = 3;
}
