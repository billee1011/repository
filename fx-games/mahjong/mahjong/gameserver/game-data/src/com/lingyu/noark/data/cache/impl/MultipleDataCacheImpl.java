package com.lingyu.noark.data.cache.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.repository.AbstractCacheRepository;
import com.lingyu.noark.data.repository.QueryFilter;

class MultipleDataCacheImpl<T, K extends Serializable> extends AbstractDataCache<T, K> {

	// 角色Id <==> 一个数据集合
	private final LoadingCache<Serializable, ConcurrentHashMap<K, T>> caches;

	public MultipleDataCacheImpl(final AbstractCacheRepository<T, K> repository) {
		super(repository);
		CacheLoader<Serializable, ConcurrentHashMap<K, T>> loader = new CacheLoader<Serializable, ConcurrentHashMap<K, T>>() {
			@Override
			@SuppressWarnings("unchecked")
			public ConcurrentHashMap<K, T> load(Serializable roleId) throws Exception {
				List<T> result = null;
				if (entityMapping.getGroupBy() == null) {
					result = repository.loadAllByRoleId(roleId);
				} else {
					result = repository.loadAllByGroup(roleId);
				}
				int initSize = result.size() > 32 ? result.size() : 32;
				ConcurrentHashMap<K, T> datas = new ConcurrentHashMap<>(initSize);
				for (T entity : result) {
					datas.put((K) entityMapping.getPrimaryIdValue(entity), entity);
				}
				return datas;
			}
		};
		// 只要有角色Id，必有超时
		caches = CacheBuilder.newBuilder().expireAfterAccess(DataContext.getOfflineInterval(), TimeUnit.SECONDS).build(loader);
	}

	@Override
	public void insert(T entity) {
		Serializable roleId = entityMapping.getGroupIdValue(entity);
		ConcurrentHashMap<K, T> data = caches.getUnchecked(roleId);
		K entityId = this.getPrimaryIdValue(entity);
		if (data.containsKey(entityId)) {
			throw new DataException("插入了重复Key:" + entity.toString() + ",info=" + ToStringBuilder.reflectionToString(entity));
		}
		data.put(entityId, entity);
	}

	@Override
	public void delete(T entity) {
		Serializable roleId = entityMapping.getGroupIdValue(entity);
		K entityId = this.getPrimaryIdValue(entity);
		ConcurrentHashMap<K, T> data = caches.getUnchecked(roleId);
		T result = data.remove(entityId);
		if (result == null) {
			throw new DataException("删除了一个不存在的Key:" + entityId + ",info=" + ToStringBuilder.reflectionToString(entity));
		}
	}

	@Override
	public void deleteAll() {
		// 疯了，有角色区别删全部，希望你是故意的
		caches.invalidateAll();
	}

	@Override
	public void deleteAll(Serializable roleId) {
		caches.getUnchecked(roleId).clear();
	}

	@Override
	public void update(T entity) {
		Serializable roleId = entityMapping.getGroupIdValue(entity);
		ConcurrentHashMap<K, T> data = caches.getUnchecked(roleId);
		K entityId = this.getPrimaryIdValue(entity);
		if (!data.containsKey(entityId)) {
			throw new DataException("修改了一个不存在的Key:" + entityMapping.getPrimaryKey(entity) + ",info=" + ToStringBuilder.reflectionToString(entity));
		}
		data.put(entityId, entity);
	}

	@Override
	public T load(Serializable roleId, K entityId) {
		this.checkValid();
		return caches.getUnchecked(roleId).get(entityId);
	}

	@Override
	public T load(Serializable roleId, QueryFilter<T> filter) {
		this.checkValid();
		T result = null;
		for (Entry<K, T> e : caches.getUnchecked(roleId).entrySet()) {
			if (filter.stopped()) {
				break;
			}
			if (filter.check(e.getValue())) {
				result = e.getValue();
			}
		}
		return result;
	}

	@Override
	public int count(Serializable roleId, QueryFilter<T> filter) {
		this.checkValid();
		int count = 0;
		for (Entry<K, T> e : caches.getUnchecked(roleId).entrySet()) {
			if (filter.stopped()) {
				break;
			}
			if (filter.check(e.getValue())) {
				count++;
			}
		}
		return count;
	}

	@Override
	public List<T> loadAll(Serializable roleId) {
		this.checkValid();
		return new ArrayList<>(caches.getUnchecked(roleId).values());
	}

	@Override
	public List<T> loadAll(Serializable roleId, QueryFilter<T> filter) {
		this.checkValid();
		ConcurrentHashMap<K, T> data = caches.getUnchecked(roleId);
		ArrayList<T> result = new ArrayList<>(data.size());
		for (Entry<K, T> e : data.entrySet()) {
			if (filter.stopped()) {
				break;
			}
			T t = e.getValue();
			if (filter.check(t)) {
				result.add(t);
			}
		}
		return result;
	}

	@Override
	public void initCacheData() {
		logger.debug("实体类[{}]抓取策略为启动服务器就加载缓存.", entityMapping.getEntityClass());
		List<T> result = repository.loadAllBySystem();
		if (!result.isEmpty()) {
			Map<Serializable, ConcurrentHashMap<K, T>> data = new HashMap<>(result.size());
			for (T entity : result) {
				Serializable roleId = entityMapping.getGroupIdValue(entity);
				ConcurrentHashMap<K, T> ds = data.get(roleId);
				if (ds == null) {
					ds = new ConcurrentHashMap<>();
					data.put(roleId, ds);
				}
				ds.put(this.getPrimaryIdValue(entity), entity);
			}
			caches.putAll(data);
		}
		logger.debug("实体类[{}]初始化缓存完成,一共 {} 条数据.", entityMapping.getEntityClass(), result.size());
	}

	@Override
	public void initCacheDataByRoleId(Serializable roleId) {
		caches.getUnchecked(roleId);
	}

	@Override
	public void removeCache(Serializable roleId) {
		caches.invalidate(roleId);
	}
}
