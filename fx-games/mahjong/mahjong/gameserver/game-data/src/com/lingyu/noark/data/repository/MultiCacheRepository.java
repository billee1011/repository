package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.EntityHolder;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.IsRoleId;

/**
 * 封装了一套缓存机制的ORM数据访问层.
 * <p>
 * 应用于一种情况：<br>
 * 1.有{@link IsRoleId}注解并且和{@link Id}注解标识不同属性的实体类. <br>
 * 可以理解为，一个角色可以有多条记录数据的类.<br>
 * 
 * @param <T> 实体类
 * @param <K> 实体类的主键
 * @author 小流氓<176543888@qq.com>
 */
public class MultiCacheRepository<T, K extends Serializable> extends AbstractCacheRepository<T, K> {
	protected MultiCacheRepository() {
		EntityHolder.register(this);
	}

	/**
	 * 从缓存中删除指定Id的对象.
	 * <p>
	 * 
	 * @param entityId 缓存对象的Id
	 */
	public void cacheDelete(Serializable roleId, K entityId) {
		T result = cache.load(roleId, entityId);
		cache.delete(result);

		this.getAsyncWriteService().delete(entityMapping, result);
	}

	/**
	 * 删除角色模块缓存里的所有数据.
	 * 
	 * @param roleId 角色Id
	 */
	public void cacheDeleteAll(Serializable roleId) {
		List<T> result = cache.loadAll(roleId);
		cache.deleteAll(roleId);

		this.getAsyncWriteService().deleteAll(entityMapping, result);
	}

	/**
	 * 从角色缓存中根据实体Id获取对象.
	 * 
	 * @param roleId 角色Id
	 * @param entityId 实体Id
	 * @return 实体对象.
	 */
	public T cacheLoad(Serializable roleId, K entityId) {
		return cache.load(roleId, entityId);
	}

	/**
	 * 从角色缓存中根据过滤器获取对象.
	 * 
	 * @param roleId 角色Id
	 * @param filter 条件过滤器
	 * @return 实体对象.
	 */
	public T cacheLoad(Serializable roleId, QueryFilter<T> filter) {
		return cache.load(roleId, filter);
	}

	/**
	 * 统计角色缓存中符合过滤条件的对象总数。
	 * 
	 * @param roleId 角色Id
	 * @param filter 条件过滤器
	 * @return 符合过滤条件的对象总数。
	 */
	public int cacheCount(Serializable roleId, QueryFilter<T> filter) {
		return cache.count(roleId, filter);
	}

	/**
	 * 从角色缓存中获取一个模块所有缓存数据.
	 * 
	 * @param roleId 角色Id
	 * @return 一个模块所有缓存数据.
	 */
	public List<T> cacheLoadAll(Serializable roleId) {
		return cache.loadAll(roleId);
	}

	/**
	 * 从缓存中获取符合过虑器的需求的对象.
	 * 
	 * @param roleId 角色Id
	 * @param filter 过虑器
	 * @return 符合过虑器的需求的对象列表.
	 */
	public List<T> cacheLoadAll(Serializable roleId, QueryFilter<T> filter) {
		return cache.loadAll(roleId, filter);
	}

	@Override
	public List<T> _loadDataByRoleIdForNewwork(Serializable roleId) {
		return this.cacheLoadAll(roleId);
	}
}
