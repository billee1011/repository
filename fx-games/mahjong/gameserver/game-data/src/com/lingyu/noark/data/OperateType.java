package com.lingyu.noark.data;

/**
 * 对数据的操作类型.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public enum OperateType {
	// MYSQL
	INSTER, DELETE, UPDATE, SELECT,

	// ----------------------------REDIS---------------------------------------------
	ZADD, ZREM, ZINCRBY,
	// Hash
	HSET, HMSET;
}
