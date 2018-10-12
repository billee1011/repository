package com.lingyu.noark.data.cache.impl;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.cache.IDataCache;
import com.lingyu.noark.data.repository.AbstractCacheRepository;
import com.lingyu.noark.data.repository.QueryFilter;

abstract class AbstractDataCache<T, K extends Serializable> implements IDataCache<T, K> {
	protected static final Logger logger = LogManager.getLogger(AbstractDataCache.class);
	protected final EntityMapping<T> entityMapping;
	protected final AbstractCacheRepository<T, K> repository;

	public AbstractDataCache(AbstractCacheRepository<T, K> repository) {
		this.repository = repository;
		this.entityMapping = repository.getEntityMapping();
		// 如果此实体是启服务器时就加载缓存数据，果断拉数据啊~~
		if (entityMapping.getFeatchType() == FeatchType.START) {
			// this.initCacheData();
		}
	}

	@SuppressWarnings("unchecked")
	protected K getPrimaryIdValue(T entity) {
		return (K) entityMapping.getPrimaryIdValue(entity);
	}

	@Override
	public T load(K entityId) {
		// 当不支持请求的操作时，抛出该异常。
		throw new UnsupportedOperationException();
	}

	@Override
	public T load(Serializable roleId, K entityId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<T> loadAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<T> loadAll(QueryFilter<T> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<T> loadAll(Serializable roleId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<T> loadAll(Serializable roleId, QueryFilter<T> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteAll(Serializable roleId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T load(Serializable roleId, QueryFilter<T> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int count(Serializable roleId, QueryFilter<T> filter) {
		throw new UnsupportedOperationException();
	}
	protected void checkValid() {
		if (DataContext.isDebug()) {
			if (StringUtils.startsWith(Thread.currentThread().getName(), "game-stage")) {
				throw new UnsupportedOperationException("不能在stage 进行数据库操作");
			}
		}
	}
}