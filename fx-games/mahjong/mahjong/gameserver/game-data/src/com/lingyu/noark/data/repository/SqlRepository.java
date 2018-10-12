package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.DefaultRoleId;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.accessor.AnnotationEntityMaker;
import com.lingyu.noark.data.accessor.DataAccessor;
import com.lingyu.noark.data.accessor.mysql.RowMapper;

public class SqlRepository<T> {
	private final static AnnotationEntityMaker maker = new AnnotationEntityMaker();
	protected EntityMapping<T> entityMapping;

	@SuppressWarnings("unchecked")
	protected SqlRepository() {
		Type mySuperClass = this.getClass().getGenericSuperclass();
		Type type = ((ParameterizedType) mySuperClass).getActualTypeArguments()[0];
		this.entityMapping = maker.make((Class<T>) type);
	}

	/**
	 * 向数据存储策略管理器要一个数据存储策略.
	 * <p>
	 * 条件为当前实体有没有@IsRoleId,如果有：则有可能返回的是网络层存储策略.<br>
	 * 使用数据源接口，采用Get方式，以便后面维护Mysql时不用停服.
	 * 
	 * @param roleId 角色Id
	 * @return 数据存储策略
	 */
	protected DataAccessor getDataAccessor(Serializable roleId) {
		return DataContext.getDataAccessorManager().getDataAccess(roleId);
	}

	// 直接操作SQL系列
	public T queryForObject(String sql, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).queryForObject(entityMapping, sql, args);
	}

	public List<T> queryForList(String sql, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).queryForList(entityMapping, sql, args);
	}

	public int queryForInt(String sql, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).queryForInt(entityMapping, sql, args);
	}

	public long queryForLong(String sql, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).queryForLong(entityMapping, sql, args);
	}

	public Map<String, Object> queryForMap(String sql, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).queryForMap(entityMapping, sql, args);
	}

	public List<T> queryForList(String sql, RowMapper<T> mapper, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).queryForList(entityMapping, sql, mapper, args);
	}

	/**
	 * 执行一条SQL.
	 * 
	 * @param sql SQL语句.
	 * @param args 参数，可以为空
	 * @return 返回SQL执行的结果.
	 */
	public int execute(String sql, Object... args) {
		return this.getDataAccessor(DefaultRoleId.instance).execute(sql, args);
	}
}