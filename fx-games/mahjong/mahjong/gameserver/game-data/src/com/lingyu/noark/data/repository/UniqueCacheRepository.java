package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lingyu.noark.data.EntityHolder;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.IsRoleId;

/**
 * 封装了一套缓存机制的ORM数据访问层.
 * <p>
 * 应用于两种情况：<br>
 * 1.没有{@link IsRoleId}注解的实体类.<br>
 * 2.有{@link IsRoleId}注解并且和{@link Id}注解同一个属性的实体类. <br>
 * 可以理解为，一个角色只有一条记录或不属于任何角色的数据的类.<br>
 * 
 * @param <T> 实体类
 * @param <K> 实体类的主键
 * @author 小流氓<176543888@qq.com>
 */
public class UniqueCacheRepository<T, K extends Serializable> extends AbstractCacheRepository<T, K> {

	protected UniqueCacheRepository() {
		EntityHolder.register(this);
	}

	/**
	 * 从缓存中删除指定Id的对象.
	 * <p>
	 * 
	 * @param entityId 缓存对象的Id
	 */
	public void cacheDelete(K entityId) {
		T result = cache.load(entityId);
		cache.delete(result);

		this.getAsyncWriteService().delete(entityMapping, result);
	}

	public T cacheLoad(K entityId) {
		return cache.load(entityId);
	}

	/** 只会从缓存检索 */
	public List<T> cacheLoadAll() {
		return cache.loadAll();
	}

	/** 只会从缓存检索 */
	public List<T> cacheLoadAll(QueryFilter<T> filter) {
		return cache.loadAll(filter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> _loadDataByRoleIdForNewwork(Serializable roleId) {
		T result = this.cacheLoad((K) roleId);
		if (result == null) {
			return Collections.emptyList();
		}
		List<T> resultList = new ArrayList<>(1);
		resultList.add(result);
		return resultList;
	}
}
