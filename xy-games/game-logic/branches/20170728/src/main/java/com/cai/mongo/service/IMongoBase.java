package com.cai.mongo.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.mongo.service.log.bean.RoleLogBase;

public interface IMongoBase<T> {

	public List<T> findListByParams(Map<String, Object> params);

	public List<RoleLogBase> findListByQuery(Query query);

	public T findOne(Map<String, Object> params);

	public T findOne(Query query);

	// insert添加
	public void insert(T object);

	// save添加
	public void save(T object);

	// 批量添加
	public void insertAll(List<T> object);

}
