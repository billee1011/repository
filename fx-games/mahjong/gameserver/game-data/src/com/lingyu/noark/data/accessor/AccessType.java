package com.lingyu.noark.data.accessor;

/**
 * 存储策略类型.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public enum AccessType {
	/**
	 * 数据存储来自mysql
	 */
	Mysql,
	/**
	 * 数据存储来自redis
	 */
	Redis,
	/**
	 * 数据存储来自网络，实现跨服功能的必需神器.
	 */
	Network;
}
