package com.lingyu.noark.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * Redis实体映射描述类.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class RedisEntityMapping<T> {
	protected final Class<T> klass;
	private final MethodAccess methodAccess;
	private final ConstructorAccess<T> constructorAccess;
	// 全部属性
	protected List<FieldMapping> fieldInfo;
	// 角色ID
	protected FieldMapping roleId;

	private String tableName;
	private String prefix;
	protected FieldMapping keyField;
	private String suffix;

	public RedisEntityMapping(Class<T> klass) {
		this.klass = klass;
		this.methodAccess = MethodAccess.get(klass);
		this.constructorAccess = ConstructorAccess.get(klass);
	}

	public void setFieldInfo(List<FieldMapping> fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public List<FieldMapping> getFieldMapping() {
		return fieldInfo;
	}

	public final MethodAccess getMethodAccess() {
		return methodAccess;
	}

	public final String getTableName() {
		return tableName;
	}

	public final void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public T newEntity(Map<String, String> data) throws IllegalArgumentException, IllegalAccessException {
		if (data.isEmpty()) {
			return null;
		}
		T instance = constructorAccess.newInstance();
		for (FieldMapping fm : fieldInfo) {
			String value = data.get(fm.getColumnName());
			if (value != null && !"".equals(value)) {
				fm.getAdaptor().get(value, fm, instance);
			}
		}
		return instance;
	}

	public Class<T> getEntityClass() {
		return klass;
	}

	public T newEntity(String json) {
		return JSON.parseObject(json, getEntityClass());
	}

	public final FieldMapping getRoleId() {
		return roleId;
	}

	public final void setRoleId(FieldMapping roleId) {
		this.roleId = roleId;
	}

	public final String getPrefix() {
		return prefix;
	}

	public final void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public final FieldMapping getKeyField() {
		return keyField;
	}

	public final void setKeyField(FieldMapping keyField) {
		this.keyField = keyField;
	}

	public final String getSuffix() {
		return suffix;
	}

	public final void setSuffix(String suffix) {
		this.suffix = suffix;
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

	public String makeKey(Object entity) {
		if (keyField == null) {
			return tableName;
		}
		StringBuilder sb = new StringBuilder(64);
		if (prefix != null) {
			sb.append(prefix);
		}
		String keyValue = methodAccess.invoke(entity, keyField.getGetMethodIndex()).toString();
		sb.append(keyValue);
		if (suffix != null) {
			sb.append(suffix);
		}
		return sb.toString();
	}

	public Map<String, String> toValue(Object entity, String... fields) throws IllegalArgumentException, IllegalAccessException {
		Map<String, String> result = new HashMap<>();
		if (fields == null || fields.length == 0) {
			for (FieldMapping fm : fieldInfo) {
				String value = fm.getAdaptor().getString(fm, entity);
				if (value != null) {
					result.put(fm.getColumnName(), value);
				}
			}
		} else {
			for (FieldMapping fm : fieldInfo) {
				boolean exist = false;
				for (String field : fields) {
					if (fm.getColumnName().equals(field)) {
						exist = true;
						break;
					}
				}
				if (exist) {
					String value = fm.getAdaptor().getString(fm, entity);
					if (value != null) {
						result.put(fm.getColumnName(), value);
					}
				}
			}
		}
		return result;
	}
}