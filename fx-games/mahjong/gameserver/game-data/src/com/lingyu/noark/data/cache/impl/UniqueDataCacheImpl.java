package com.lingyu.noark.data.cache.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.repository.AbstractCacheRepository;
import com.lingyu.noark.data.repository.QueryFilter;

/**
 * 这类的，要么没有角色Id，要么就是角色Id就是主键。
 * 
 * @param <T> 实体类
 * @param <K> 实体类Id
 * @author 小流氓<176543888@qq.com>
 */
class UniqueDataCacheImpl<T, K extends Serializable> extends AbstractDataCache<T, K> {

	// 实体Id <==> 一个数据包装器
	private final LoadingCache<K, DataWrapper<T>> caches;

	public UniqueDataCacheImpl(final AbstractCacheRepository<T, K> repository) {
		super(repository);
		// 构建缓存容器,没有角色Id的永久缓存，有角色Id的具有超时时间.
		if (entityMapping.getRoleId() == null) {
			// 构建一个数据加载器
			CacheLoader<K, DataWrapper<T>> loader = new CacheLoader<K, DataWrapper<T>>() {
				@Override
				public DataWrapper<T> load(K entityId) throws Exception {
					// 没有缓存时，从数据访问策略中加载
					return new DataWrapper<>(repository.loadBySystem(entityId));
				}
			};
			caches = CacheBuilder.newBuilder().build(loader);
		} else {
			// 有没有@IsRoleId加载方式不一样的
			CacheLoader<K, DataWrapper<T>> loader = new CacheLoader<K, DataWrapper<T>>() {
				@Override
				public DataWrapper<T> load(K entityId) throws Exception {
					// 没有缓存时，从数据访问策略中加载
					return new DataWrapper<>(repository.load(entityId, entityId));
				}
			};

			// 启动服务器时加载的缓存，需要永久缓存
			if (entityMapping.getFeatchType() == FeatchType.START && !DataContext.isCross()) {
				caches = CacheBuilder.newBuilder().build(loader);
			} else {
				caches = CacheBuilder.newBuilder().expireAfterAccess(DataContext.getOfflineInterval(), TimeUnit.SECONDS).build(loader);
			}
		}
	}

	/**
	 * 数据包装器.
	 * <p>
	 * 这个类里有一个实体对象，可能为空，主要用来初始化缓存数据
	 * 
	 * @param <E> 实体对象
	 * @author 小流氓<176543888@qq.com>
	 */
	private class DataWrapper<E> {
		private E entity;

		private DataWrapper(E entity) {
			this.entity = entity;
		}

		E getEntity() {
			return entity;
		}

		void setEntity(E entity) {
			this.entity = entity;
		}
	}

	@Override
	public void insert(T entity) {
		K entityId = this.getPrimaryIdValue(entity);
		DataWrapper<T> wrapper = caches.getUnchecked(entityId);
		if (wrapper.getEntity() == null) {
			wrapper.setEntity(entity);
		} else {
			throw new DataException("插入了重复Key:" + entityMapping.getPrimaryKey(entity) + ",info=" + ToStringBuilder.reflectionToString(entity));
		}
	}

	@Override
	public void delete(T entity) {
		K entityId = this.getPrimaryIdValue(entity);
		DataWrapper<T> wrapper = caches.getUnchecked(entityId);
		if (wrapper.getEntity() == null) {
			throw new DataException("删除了一个不存在的Key:" + entityMapping.getPrimaryKey(entity) + ",info=" + ToStringBuilder.reflectionToString(entity));
		} else {
			wrapper.setEntity(null);
		}
	}

	@Override
	public void deleteAll() {
		caches.invalidateAll();
	}

	@Override
	public void update(T entity) {
		K entityId = this.getPrimaryIdValue(entity);
		DataWrapper<T> wrapper = caches.getUnchecked(entityId);
		if (wrapper.getEntity() == null) {
			throw new DataException("修改了一个不存在的Key:" + entityMapping.getPrimaryKey(entity) + ",info=" + ToStringBuilder.reflectionToString(entity));
		} else {
			wrapper.setEntity(entity);
		}
	}

	@Override
	public T load(K entityId) {
		this.checkValid();
		return caches.getUnchecked(entityId).getEntity();
	}

	@Override
	public List<T> loadAll() {
		this.checkValid();
		return loadAllByQueryFilter(null);
	}

	@Override
	public List<T> loadAll(QueryFilter<T> filter) {
		this.checkValid();
		return loadAllByQueryFilter(filter);
	}

	private List<T> loadAllByQueryFilter(QueryFilter<T> filter) {
		this.checkValid();
		ConcurrentMap<K, DataWrapper<T>> map = caches.asMap();
		if (map.isEmpty()) {
			return Collections.emptyList();
		}
		ArrayList<T> result = new ArrayList<>(map.size());
		for (Entry<K, DataWrapper<T>> e : map.entrySet()) {
			T entity = e.getValue().getEntity();
			if (entity == null) {
				continue;
			}
			if (filter != null) {
				if (filter.stopped()) {
					break;
				}
				if (!filter.check(entity)) {
					continue;
				}
			}
			result.add(entity);
		}
		result.trimToSize();
		return result;
	}

	@Override
	public void initCacheData() {
		logger.debug("实体类[{}]抓取策略为启动服务器就加载缓存.", entityMapping.getEntityClass());
		List<T> result = repository.loadAllBySystem();
		if (!result.isEmpty()) {
			for (T entity : result) {
				caches.put(this.getPrimaryIdValue(entity), new DataWrapper<>(entity));
			}
		}
		logger.debug("实体类[{}]初始化缓存完成,一共 {} 条数据.", entityMapping.getEntityClass(), result.size());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initCacheDataByRoleId(Serializable roleId) {
		if (entityMapping.getRoleId() != null) {
			caches.getUnchecked((K) roleId);
		}
	}

	@Override
	public void removeCache(Serializable roleId) {
		if (entityMapping.hasRoleId()) {
			caches.invalidate(roleId);
		}
	}
}
