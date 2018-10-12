package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.EntityHolder;
import com.lingyu.noark.data.annotation.Group;

/**
 * 封装了一套缓存机制的ORM数据访问层.
 * <p>
 * 应用于一种情况：<br>
 * 1.有{@link Group}注解标识的实体类. <br>
 * 可以理解为，这个实体类需要组，但他不属性角色数据的实体类.<br>
 * 
 * @param <T> 实体类
 * @param <K> 实体类的主键
 * @author 小流氓<176543888@qq.com>
 */
public class GroupByCacheRepository<T, K extends Serializable> extends AbstractCacheRepository<T, K> {
	protected GroupByCacheRepository() {
		EntityHolder.register(this);
	}

	/**
	 * 从缓存中删除指定Id的对象.
	 * <p>
	 * 
	 * @param entityId 缓存对象的Id
	 */
	public void cacheDelete(Serializable groupId, K entityId) {
		T result = cache.load(groupId, entityId);
		cache.delete(result);

		this.getAsyncWriteService().delete(entityMapping, result);
	}

	/**
	 * 删除角色模块缓存里的所有数据.
	 * 
	 * @param groupId 角色Id
	 */
	public void cacheDeleteAll(Serializable groupId) {
		List<T> result = cache.loadAll(groupId);
		cache.deleteAll(groupId);

		this.getAsyncWriteService().deleteAll(entityMapping, result);
	}

	/**
	 * 从角色缓存中根据实体Id获取对象.
	 * 
	 * @param groupId 角色Id
	 * @param entityId 实体Id
	 * @return 实体对象.
	 */
	public T cacheLoad(Serializable groupId, K entityId) {
		return cache.load(groupId, entityId);
	}

	/**
	 * 从角色缓存中获取一个模块所有缓存数据.
	 * 
	 * @param groupId 角色Id
	 * @return 一个模块所有缓存数据.
	 */
	public List<T> cacheLoadAll(Serializable groupId) {
		return cache.loadAll(groupId);
	}

	/**
	 * 从缓存中获取符合过虑器的需求的对象.
	 * 
	 * @param groupId 角色Id
	 * @param filter 过虑器
	 * @return 符合过虑器的需求的对象列表.
	 */
	public List<T> cacheLoadAll(Serializable groupId, QueryFilter<T> filter) {
		return cache.loadAll(groupId, filter);
	}

	@Override
	public List<T> _loadDataByRoleIdForNewwork(Serializable roleId) {
		throw new UnsupportedOperationException("分组模块目前不支持此操作.");
	}
}
