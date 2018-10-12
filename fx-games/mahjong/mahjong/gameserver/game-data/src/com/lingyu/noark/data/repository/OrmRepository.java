package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.DefaultRoleId;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.accessor.Page;
import com.lingyu.noark.data.accessor.Pageable;

/**
 * OrmRepository类为所有实体操作类的父类.
 * <p>
 * 向子类提供ORM操作接口.<br>
 * 当此实体有@IsRoleId标识时，此ORM先行判定是否需要网络层存取数据，当没有时，必走存储策略层.
 * 
 * @author 小流氓<176543888@qq.com>
 * 
 * @param <T> 实体类型
 * @param <K> 实体的ID类型
 */
public class OrmRepository<T, K extends Serializable> extends SqlRepository<T> {

	/**
	 * 初始化ORM，检测实体和表结构.
	 */
	protected OrmRepository() {
		super();
		_checkupEntityFieldsWithDatabase();
	}

	/**
	 * 检测实体属性和表结构是否一致，Nosql可以空实现.
	 */
	private void _checkupEntityFieldsWithDatabase() {
		if(this.getDataAccessor(DefaultRoleId.instance) != null){
			this.getDataAccessor(DefaultRoleId.instance).checkupEntityFieldsWithDatabase(entityMapping);
		}
	}

	/**
	 * 获取当前OrmRepository的实体类的描述对象
	 * 
	 * @return 实体类的描述对象
	 */
	public EntityMapping<T> getEntityMapping() {
		return entityMapping;
	}

	/**
	 * 向存储策略接口插入一个实体对象.
	 * 
	 * @param entity 实体类对象.
	 */
	public void insert(T entity) {
		this.getDataAccessor(entityMapping.getRoleIdValue(entity)).insert(entityMapping, entity);
	}

	/**
	 * 向存储策略接口删除一个实体对象.
	 * 
	 * @param entity 实体类对象.
	 */
	public void delete(T entity) {
		this.getDataAccessor(entityMapping.getRoleIdValue(entity)).delete(entityMapping, entity);
	}

	/**
	 * 向存储策略接口修改一个实体对象.
	 * 
	 * @param entity 实体类对象.
	 */
	public void update(T entity) {
		this.getDataAccessor(entityMapping.getRoleIdValue(entity)).update(entityMapping, entity);
	}

	/**
	 * 根据角色ID和实体Id从存储策略层加载数据.
	 * 
	 * @param roleId 角色ID
	 * @param entityId 实体ID.
	 * @return 如果存在此ID的对象，则返回此对象，否则返回 null
	 */
	public T load(Serializable roleId, K entityId) {
		return this.getDataAccessor(roleId).load(entityMapping, roleId, entityId);
	}

	/**
	 * 根据实体Id从存储策略层加载系统模块数据.
	 * 
	 * @param entityId 实体ID.
	 * @return 如果存在此ID的对象，则返回此对象，否则返回 null
	 */
	public T loadBySystem(K entityId) {
		return this.getDataAccessor(DefaultRoleId.instance).load(entityMapping, DefaultRoleId.instance, entityId);
	}

	/**
	 * 从存储策略层加载数据.
	 * <p>
	 * 业内替规则：返回集合时，不要返回null 如果为空也要返回空列表
	 * 
	 * @return 如果存在此类对象，则返回对象列表，否则返回 空列表.
	 */
	public List<T> loadAllBySystem() {
		return this.getDataAccessor(DefaultRoleId.instance).loadAll(entityMapping);
	}

	/**
	 * 根据RoleId从存储策略层加载数据.
	 * <p>
	 * 如果是系统的就直接调用LoadAll
	 * 
	 * @param roleId 角色ID.
	 * @return 如果存在此角色ID的对象，则返回对象列表，否则返回 空列表.
	 */
	public List<T> loadAllByRoleId(Serializable roleId) {;
		if (roleId == DefaultRoleId.instance) {
			return this.loadAllBySystem();
		}
		return this.getDataAccessor(roleId).loadByRoleId(roleId, entityMapping);
	}

	public Page<T> loadAllByRoleId(Serializable roleId, Pageable pageable) {
		return this.getDataAccessor(roleId).loadByRoleId(roleId, entityMapping, pageable);
	}

	public Page<T> loadAllBySystem(Pageable pageable) {
		return this.getDataAccessor(DefaultRoleId.instance).loadAll(entityMapping, pageable);
	}

	public List<T> loadAllByGroup(Serializable roleId) {
		return this.getDataAccessor(DefaultRoleId.instance).loadByGroup(entityMapping, roleId);
	}
}