package com.lingyu.noark.data;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Table.TableEngine;

/**
 * 实体映射描述类.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public abstract class EntityMapping<T> {
	protected final Class<T> klass;
	// 抓取策略
	protected final FeatchType featchType;
	protected final boolean delayInsert;
	private final MethodAccess methodAccess;
	private final ConstructorAccess<T> constructorAccess;
	// 表名
	protected String tableName;

	// 动态表名属性和格式
	protected FieldMapping dynamicTableNameField = null;
	protected String dynamicTableNameFormat = null;

	// 注释
	private String tableComment;
	// 表结构默认的存储引擎
	private TableEngine tableEngine = TableEngine.InnoDB;
	// 主键
	protected FieldMapping primaryId;
	// 角色ID
	protected FieldMapping roleId;
	// 分组
	protected FieldMapping groupBy;
	// 全表属性
	protected List<FieldMapping> fieldInfo;

	public EntityMapping(Class<T> klass) {
		this.klass = klass;
		Entity entity = klass.getAnnotation(Entity.class);
		this.featchType = entity.fetch();
		this.delayInsert = entity.delayInsert();
		this.methodAccess = MethodAccess.get(klass);
		this.constructorAccess = ConstructorAccess.get(klass);
	}

	public Class<T> getEntityClass() {
		return klass;
	}

	public FeatchType getFeatchType() {
		return featchType;
	}

	public String getTableName() {
		return tableName;
	}

	public String getTableName(Object entity) {
		if (dynamicTableNameField == null) {
			return tableName;
		}
		StringBuilder sb = new StringBuilder(tableName.length() + dynamicTableNameFormat.length());
		return sb.append(tableName).append(this.formatStringKey(entity)).toString();
	}

	/**
	 * 生成当前时间的格式化字符串.
	 * 
	 * @param pattern 格式字符串
	 * @return 当前时间的格式化字符串.
	 */
	private String formatStringKey(Object entity) {
		Object date = methodAccess.invoke(entity, dynamicTableNameField.getGetMethodIndex());
		return new SimpleDateFormat(dynamicTableNameFormat).format(date);
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setPrimaryId(FieldMapping primaryId) {
		this.primaryId = primaryId;
	}

	public FieldMapping getPrimaryId() {
		return primaryId;
	}

	/**
	 * 判定当前实体描述对象上有没有@IsRoleId.
	 * 
	 * @return 如果有@IsRoleId则返回true，否则返回false
	 */
	public boolean hasRoleId() {
		return roleId != null;
	}

	public FieldMapping getRoleId() {
		return roleId;
	}

	public void setRoleId(FieldMapping roleId) {
		this.roleId = roleId;
	}

	public FieldMapping getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(FieldMapping groupBy) {
		this.groupBy = groupBy;
	}

	public void setFieldInfo(List<FieldMapping> fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public List<FieldMapping> getFieldMapping() {
		return fieldInfo;
	}

	public String getTableComment() {
		return tableComment;
	}

	public void setTableComment(String tableComment) {
		this.tableComment = tableComment;
	}

	public TableEngine getTableEngine() {
		return tableEngine;
	}

	public void setTableEngine(TableEngine tableEngine) {
		this.tableEngine = tableEngine;
	}

	public final boolean isDelayInsert() {
		return delayInsert;
	}

	public FieldMapping getDynamicTableNameField() {
		return dynamicTableNameField;
	}

	public void setDynamicTableNameField(FieldMapping dynamicTableNameField) {
		this.dynamicTableNameField = dynamicTableNameField;
	}

	public String getDynamicTableNameFormat() {
		return dynamicTableNameFormat;
	}

	public void setDynamicTableNameFormat(String dynamicTableNameFormat) {
		this.dynamicTableNameFormat = dynamicTableNameFormat;
	}

	/**
	 * 获取主键的值.
	 */
	public Serializable getPrimaryIdValue(Object entity) {
		// ASM去取值
		return (Serializable) methodAccess.invoke(entity, primaryId.getGetMethodIndex());
	}

	/**
	 * 获取角色ID.
	 */
	public Serializable getRoleIdValue(Object entity) {
		if (roleId == null) {
			return DefaultRoleId.instance;
		}
		// ASM去取值
		return (Serializable) methodAccess.invoke(entity, roleId.getGetMethodIndex());
	}

	/**
	 * 获取分组ID.
	 */
	public Serializable getGroupIdValue(Object entity) {
		if (groupBy != null) {
			return (Serializable) methodAccess.invoke(entity, groupBy.getGetMethodIndex());
		}
		return this.getRoleIdValue(entity);
	}

	/**
	 * 构造一个回写数据的唯一Key.
	 * <p>
	 * 类的全名+主键值
	 */
	public String getPrimaryKey(Object entity) {
		return new StringBuilder(64).append(klass.getName()).append(':').append(this.getPrimaryIdValue(entity)).toString();
	}

	/**
	 * 根据返回结果集，反向生成实体对象列表.
	 */
	public List<T> newEntityList(ResultSet rs) throws SQLException, InstantiationException, IllegalAccessException {
		List<T> result = new ArrayList<>();
		while (rs.next()) {
			result.add(this.newEntity(rs));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public T newEntity(ResultSet rs) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException {
		Object entity = constructorAccess.newInstance();
		for (FieldMapping fm : fieldInfo) {
			fm.getAdaptor().get(rs, fm, entity);
		}
		return (T) entity;
	}

	/**
	 * 深度拷贝功能。
	 * 
	 * @param t 目标对象.
	 * @return 返回一个深度拷贝后的对象。
	 */
	@SuppressWarnings("unchecked")
	public T copy(T t) {
		Object object = constructorAccess.newInstance();
		for (FieldMapping fm : fieldInfo) {
			methodAccess.invoke(object, fm.getSetMethodIndex(), methodAccess.invoke(object, fm.getGetMethodIndex()));
		}
		return (T) object;
	}

	public MethodAccess getMethodAccess() {
		return methodAccess;
	}

	/**
	 * 深度拷贝一个对象.
	 * 
	 * @param t 目标对象.
	 * @return 克隆对象.
	 */
	public T clone(T t) {
		// 创建一个对象
		T object = constructorAccess.newInstance();
		// 只要复制有映射关系的属性
		for (FieldMapping fm : fieldInfo) {
			// 调用get(is)方法
			Object value = methodAccess.invoke(t, fm.getGetMethodIndex());
			if (true) {
				// 如果是对象中的对象，需要再来一次
			}
			// 调用set方法
			methodAccess.invoke(object, fm.getSetMethodIndex(), value);
		}
		return object;
	}
}
