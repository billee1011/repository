package com.lingyu.noark.data.repository;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;
import com.lingyu.noark.data.AbsRedisRepository;
import com.lingyu.noark.data.accessor.redis.cmd.Get;
import com.lingyu.noark.data.accessor.redis.cmd.Set;

/**
 * RedisStringRepository类为所有Redis操作String数据结构接口.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public abstract class RedisStringRepository<T> extends AbsRedisRepository<T> {

	public T cacheLoad(Serializable roleId, String key) {
		return this.getRedisDataAccessor(roleId).get(this.getRedis(), new Get(roleId, key), entityMapping);
	}

	public void cacheUpdate(Serializable roleId, String key, T entity) {
		this.getRedisDataAccessor(roleId).set(this.getRedis(), new Set(roleId, key, JSON.toJSONString(entity)), entityMapping);
	}
}