package com.lingyu.noark.data;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.lingyu.noark.data.accessor.AnnotationEntityMaker;
import com.lingyu.noark.data.accessor.redis.IRedisDataAccessor;
import com.lingyu.noark.data.accessor.redis.IRediser;
import com.lingyu.noark.data.accessor.redis.cmd.Get;
import com.lingyu.noark.data.accessor.redis.cmd.Hmset;
import com.lingyu.noark.data.accessor.redis.cmd.Hset;
import com.lingyu.noark.data.accessor.redis.cmd.Set;
import com.lingyu.noark.data.accessor.redis.cmd.Zadd;
import com.lingyu.noark.data.accessor.redis.cmd.Zincrby;
import com.lingyu.noark.data.accessor.redis.cmd.Zrem;

public abstract class AbsRedisRepository<T> implements IRedisRepository {
	private static final AnnotationEntityMaker maker = new AnnotationEntityMaker();
	protected final RedisEntityMapping<T> entityMapping;

	@SuppressWarnings("unchecked")
	public AbsRedisRepository() {
		Type mySuperClass = this.getClass().getGenericSuperclass();
		Type type = ((ParameterizedType) mySuperClass).getActualTypeArguments()[0];
		this.entityMapping = maker.makeRedisEntityMapping((Class<T>) type);
		EntityHolder.register(entityMapping.getEntityClass().getName(), this);
	}

	protected abstract IRediser getRedis();

	protected IRedisDataAccessor getRedisDataAccessor() {
		return DataContext.getDataAccessorManager().getRedisDataAccess();
	}

	protected IRedisDataAccessor getRedisDataAccessor(Serializable roleId) {
		return DataContext.getDataAccessorManager().getRedisDataAccess(roleId);
	}

	@Override
	public <S extends Set> void set(S string) {

	}

	@Override
	public <S extends Get> void get(S string) {

	}

	@Override
	public <S extends Zadd> void zadd(S set) {

	}

	@Override
	public <S extends Zrem> void zrem(S set) {

	}

	@Override
	public <S extends Zincrby> void zincrby(S set) {

	}

	@Override
	public <O extends Hset> void hset(O hash) {

	}

	@Override
	public <O extends Hmset> void hmset(O hash) {

	}
}