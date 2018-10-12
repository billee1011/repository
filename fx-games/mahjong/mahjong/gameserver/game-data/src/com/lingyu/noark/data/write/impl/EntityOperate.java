package com.lingyu.noark.data.write.impl;

import com.lingyu.noark.data.EntityMapping;

public class EntityOperate<T> {
	private String id;

	private EntityMapping<T> em;
	private T entity;

	private boolean insert = false;
	private boolean update = false;
	private boolean delete = false;

	public EntityOperate(String entityId, EntityMapping<T> em) {
		this.em = em;
		this.id = entityId;
	}

	private void updateEntity(T entity) {
		this.entity = entity;
	}

	public void insert(T insertEntity) {
		if (delete) {
			throw new RuntimeException("illeagle [insert] after [delete]," + insertEntity.getClass().getName());
		}

		this.insert = true;
		this.updateEntity(insertEntity);
	}

	public void update(T entity) {
		this.update = true;
		this.updateEntity(entity);
	}

	public boolean delete(T deleteEntity) {
		// 如果是刚插入的状态，直接返回true，由调用层删除
		if (insert) {
			return true;
		}
		this.delete = true;
		this.updateEntity(deleteEntity);
		return false;
	}

	public String getId() {
		return id;
	}

	public EntityMapping<T> getEntityMapping() {
		return em;
	}

	public boolean isDelete() {
		return delete;
	}

	public boolean isInsert() {
		if (delete)
			return false;
		return insert;
	}

	public boolean isUpdate() {
		if (delete || insert)
			return false;
		return update;
	}

	public T getEntity() {
		// FIXME 这里再好弄成深拷贝
		return entity;
	}
}
