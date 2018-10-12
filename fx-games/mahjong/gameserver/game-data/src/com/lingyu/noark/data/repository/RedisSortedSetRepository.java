package com.lingyu.noark.data.repository;

import com.lingyu.noark.data.AbsRedisRepository;
import com.lingyu.noark.data.accessor.redis.cmd.Zadd;
import com.lingyu.noark.data.accessor.redis.cmd.Zincrby;
import com.lingyu.noark.data.accessor.redis.cmd.Zrem;

/**
 * RedisRepository类为所有Redis操作接口.
 * <p>
 * 
 * @author 小流氓<176543888@qq.com>
 */
public abstract class RedisSortedSetRepository<T> extends AbsRedisRepository<T> {
	@Override
	public <S extends Zadd> void zadd(S set) {
		this.getRedisDataAccessor(set.getRoleId()).zadd(this.getRedis(), set, entityMapping);
	}

	@Override
	public <S extends Zrem> void zrem(S set) {
		this.getRedisDataAccessor(set.getRoleId()).zrem(this.getRedis(), set, entityMapping);
	}

	@Override
	public <S extends Zincrby> void zincrby(S set) {
		this.getRedisDataAccessor(set.getRoleId()).zincrby(this.getRedis(), set, entityMapping);
	}
}