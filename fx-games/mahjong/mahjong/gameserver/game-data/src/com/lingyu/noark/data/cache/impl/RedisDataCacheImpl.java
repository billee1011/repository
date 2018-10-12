package com.lingyu.noark.data.cache.impl;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.cache.IDataCache;
import com.lingyu.noark.data.exception.UnrealizedException;
import com.lingyu.noark.data.repository.QueryFilter;
import com.lingyu.noark.data.repository.RedisHashRepository;

/**
 * 这类的，只用于Redis
 * 
 * @param <T> 实体类
 * @author 小流氓<176543888@qq.com>
 */
class RedisDataCacheImpl<T> implements IDataCache<T, String> {

	public RedisDataCacheImpl(RedisHashRepository<T> repository) {
	}

	@Override
	public T load(String key) {
		return null;
	}

	@Override
	public void removeCache(Serializable key) {
	}

	@Override
	public void insert(T entity) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public void delete(T entity) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public void deleteAll() {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public void deleteAll(Serializable roleId) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public void update(T entity) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public T load(Serializable roleId, String entityId) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public List<T> loadAll() {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public List<T> loadAll(QueryFilter<T> filter) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public List<T> loadAll(Serializable roleId) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public List<T> loadAll(Serializable roleId, QueryFilter<T> filter) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public void initCacheData() {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public void initCacheDataByRoleId(Serializable roleId) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public T load(Serializable roleId, QueryFilter<T> filter) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public int count(Serializable roleId, QueryFilter<T> filter) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}
}