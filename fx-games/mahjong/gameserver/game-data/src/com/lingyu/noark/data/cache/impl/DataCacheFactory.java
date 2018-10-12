package com.lingyu.noark.data.cache.impl;

import java.io.Serializable;

import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.cache.IDataCache;
import com.lingyu.noark.data.repository.AbstractCacheRepository;
import com.lingyu.noark.data.repository.RedisHashRepository;

public final class DataCacheFactory {

	/**
	 * 根据传入的目标Repository分析创建对应的缓存容器.
	 * <p>
	 * 实现缓存初始化的抓取策略.
	 * 
	 * @param repository 目标Repository.
	 * @return 缓存容器的实现.
	 */
	public static <T, K extends Serializable> IDataCache<T, K> create(AbstractCacheRepository<T, K> repository) {
		IDataCache<T, K> dataCache = analysis(repository);
		if (DataContext.isInitCache() && repository.getEntityMapping().getFeatchType() == FeatchType.START) {
			dataCache.initCacheData();
		}
		return dataCache;
	}

	public static <T> IDataCache<T, String> create(RedisHashRepository<T> repository) {
		return new RedisDataCacheImpl<>(repository);
	}

	public static <T, K extends Serializable> IDataCache<T, K> analysis(AbstractCacheRepository<T, K> repository) {

		if (repository.getEntityMapping().getGroupBy() != null) {
			return new MultipleDataCacheImpl<>(repository);
		}

		// 1.没有{@link IsRoleId}注解的实体类
		if (repository.getEntityMapping().getRoleId() == null) {
			return new UniqueDataCacheImpl<>(repository);
		}

		// 2.有{@link IsRoleId}注解并且和{@link Id}注解同一个属性的实体类
		if (repository.getEntityMapping().getRoleId().getId() != null) {
			return new UniqueDataCacheImpl<>(repository);
		}

		return new MultipleDataCacheImpl<>(repository);
	}
}
