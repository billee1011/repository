package com.lingyu.noark.data.accessor.redis;

import com.lingyu.noark.data.RedisEntityMapping;
import com.lingyu.noark.data.accessor.redis.cmd.Get;
import com.lingyu.noark.data.accessor.redis.cmd.Hmset;
import com.lingyu.noark.data.accessor.redis.cmd.Hset;
import com.lingyu.noark.data.accessor.redis.cmd.Set;
import com.lingyu.noark.data.accessor.redis.cmd.Zadd;
import com.lingyu.noark.data.accessor.redis.cmd.Zincrby;
import com.lingyu.noark.data.accessor.redis.cmd.Zrem;

public interface IRedisDataAccessor {
	public <T> void zadd(IRediser redis, Zadd set, RedisEntityMapping<T> entityMapping);

	public <T> void zrem(IRediser redis, Zrem set, RedisEntityMapping<T> entityMapping);

	public <T> void zincrby(IRediser redis, Zincrby set, RedisEntityMapping<T> entityMapping);

	public <T> void hset(IRediser redis, Hset hash, RedisEntityMapping<T> entityMapping);

	public <T> void hmset(IRediser redis, Hmset hash, RedisEntityMapping<T> entityMapping);

	public <T> T load(IRediser redis, String key, RedisEntityMapping<T> entityMapping);

	// 字符串------------------------------------------------------------------------
	public <T> T get(IRediser redis, Get string, RedisEntityMapping<T> entityMapping);

	public <T> void set(IRediser redis, Set string, RedisEntityMapping<T> entityMapping);
}