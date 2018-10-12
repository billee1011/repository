package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.List;

public interface ICacheRepository<T> {

	public void cacheInsert(T entity);

	public void cacheDelete(T entity);

	public void cacheUpdate(T entity);

	public List<T> _loadDataByRoleIdForNewwork(Serializable roleId);

	public void _removeCache(Serializable roleId);
}