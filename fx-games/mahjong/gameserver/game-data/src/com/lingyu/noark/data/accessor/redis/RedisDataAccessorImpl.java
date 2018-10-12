package com.lingyu.noark.data.accessor.redis;

import java.util.Map;

import com.lingyu.noark.data.RedisEntityMapping;
import com.lingyu.noark.data.accessor.redis.cmd.Get;
import com.lingyu.noark.data.accessor.redis.cmd.Hmset;
import com.lingyu.noark.data.accessor.redis.cmd.Hset;
import com.lingyu.noark.data.accessor.redis.cmd.Set;
import com.lingyu.noark.data.accessor.redis.cmd.Zadd;
import com.lingyu.noark.data.accessor.redis.cmd.Zincrby;
import com.lingyu.noark.data.accessor.redis.cmd.Zrem;
import com.lingyu.noark.data.exception.DataException;

/**
 * 直接访问Redis.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class RedisDataAccessorImpl implements IRedisDataAccessor {

	@Override
	public <T> void zadd(IRediser redis, Zadd set, RedisEntityMapping<T> entityMapping) {
		redis.zadd(set.getKey(), set.getSorce(), set.getMember());
	}

	@Override
	public <T> void zrem(IRediser redis, Zrem set, RedisEntityMapping<T> entityMapping) {
		redis.zrem(set.getKey(), set.getMember());
	}

	@Override
	public <T> void zincrby(IRediser redis, Zincrby set, RedisEntityMapping<T> entityMapping) {
		redis.zincrby(set.getKey(), set.getSorce(), set.getMember());
	}

	@Override
	public <T> void hset(IRediser redis, Hset hash, RedisEntityMapping<T> entityMapping) {
		redis.hset(hash.getKey(), hash.getField(), hash.getValue());
	}

	@Override
	public <T> T load(IRediser redis, String key, RedisEntityMapping<T> entityMapping) {
		Map<String, String> fields = redis.hgetAll(key);
		try {
			return entityMapping.newEntity(fields);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataException("Redis里拉出来的数据出异常啦...", e);
		}
	}

	@Override
	public <T> void hmset(IRediser redis, Hmset hash, RedisEntityMapping<T> entityMapping) {
		redis.hmset(hash.getKey(), hash.getHash());
	}

	@Override
	public <T> T get(IRediser redis, Get string, RedisEntityMapping<T> entityMapping) {
		return entityMapping.newEntity(redis.get(string.getKey()));
	}

	@Override
	public <T> void set(IRediser redis, Set string, RedisEntityMapping<T> entityMapping) {
		redis.set(string.getKey(), string.getValue());
	}
}