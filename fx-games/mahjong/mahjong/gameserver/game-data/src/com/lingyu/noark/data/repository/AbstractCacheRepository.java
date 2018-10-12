package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.cache.IDataCache;
import com.lingyu.noark.data.cache.impl.DataCacheFactory;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.write.AsyncWriteService;

public abstract class AbstractCacheRepository<T, K extends Serializable> extends OrmRepository<T, K> implements ICacheRepository<T> {
	// CacheRepository层的数据缓存
	protected final IDataCache<T, K> cache = DataCacheFactory.create(this);

	protected final AsyncWriteService getAsyncWriteService() {
		return DataContext.getAsyncWriteService();
	}

	/**
	 * 保存一个新增对象到缓存.
	 * 
	 * @param entity 新增对象.
	 * @exception DataException 当缓存已存在此对象时会抛出此异常。
	 */
	public void cacheInsert(T entity) {
		cache.insert(entity);
		// 延迟插入，正常走缓存模式
		if (entityMapping.isDelayInsert()) {
			this.getAsyncWriteService().insert(entityMapping, entity);
		}
		// 有些特别的数据，不能等，所以先插入数据库，再加入缓存.
		else {
			try {
				this.insert(entity);
			} catch (Exception e) {
				cache.delete(entity);
				throw new DataException("非延迟实现对象插入失败，缓存数据已作回滚处理.", e);
			}
		}
	}

	/**
	 * 删除缓存一个对象.
	 * 
	 * @param entity 实体对象.
	 */
	public void cacheDelete(T entity) {
		cache.delete(entity);

		this.getAsyncWriteService().delete(entityMapping, entity);
	}

	/**
	 * 删除当前模块全部缓存对象.
	 * <p>
	 * <b>这是删除全部，调用时，别犯2</b>
	 */
	public void cacheDeleteAll() {
		List<T> result = cache.loadAll();
		cache.deleteAll();

		this.getAsyncWriteService().deleteAll(entityMapping, result);
	}

	/**
	 * 修改缓存中的数据.
	 * 
	 * @param entity 实体对象.
	 */
	public void cacheUpdate(T entity) {
		cache.update(entity);

		this.getAsyncWriteService().update(entityMapping, entity);
	}

	@Override
	public void _removeCache(Serializable roleId) {
		cache.removeCache(roleId);
	}
}