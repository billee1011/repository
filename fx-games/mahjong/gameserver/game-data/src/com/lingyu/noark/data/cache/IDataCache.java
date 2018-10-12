package com.lingyu.noark.data.cache;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.repository.QueryFilter;

public interface IDataCache<T, K extends Serializable> {

	void insert(T entity);

	void delete(T entity);

	void deleteAll();

	void deleteAll(Serializable roleId);

	void update(T entity);

	T load(K entityId);

	T load(Serializable roleId, K entityId);

	T load(Serializable roleId, QueryFilter<T> filter);

	int count(Serializable roleId, QueryFilter<T> filter);

	List<T> loadAll();

	List<T> loadAll(QueryFilter<T> filter);

	List<T> loadAll(Serializable roleId);

	List<T> loadAll(Serializable roleId, QueryFilter<T> filter);

	void initCacheData();

	void initCacheDataByRoleId(Serializable roleId);

	void removeCache(Serializable roleId);
}
