package com.lingyu.noark.data;

import com.lingyu.noark.data.accessor.redis.cmd.Get;
import com.lingyu.noark.data.accessor.redis.cmd.Hmset;
import com.lingyu.noark.data.accessor.redis.cmd.Hset;
import com.lingyu.noark.data.accessor.redis.cmd.Set;
import com.lingyu.noark.data.accessor.redis.cmd.Zadd;
import com.lingyu.noark.data.accessor.redis.cmd.Zincrby;
import com.lingyu.noark.data.accessor.redis.cmd.Zrem;

public interface IRedisRepository {
	// string ----------------------------------
	public <S extends Set> void set(S string);

	public <S extends Get> void get(S string);

	// Sorted Sets ----------------------------------
	public <S extends Zadd> void zadd(S set);

	public <S extends Zrem> void zrem(S set);

	public <S extends Zincrby> void zincrby(S set);

	// Hash ----------------------------------
	public <H extends Hset> void hset(H hash);

	public <H extends Hmset> void hmset(H hash);
}
