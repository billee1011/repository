package com.lingyu.noark.data.accessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.RedisEntityMapping;
import com.lingyu.noark.data.accessor.mysql.Jdbcs;
import com.lingyu.noark.data.annotation.Blob;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Enumerated;
import com.lingyu.noark.data.annotation.Group;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;
import com.lingyu.noark.data.exception.NoEntityException;
import com.lingyu.noark.data.kit.ReflectKit;
import com.lingyu.noark.data.kit.StringKit;

/**
 * 实体对象解析生成器.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class AnnotationEntityMaker {

	private static final List<Class<?>> annotations = new ArrayList<>();
	static {
		annotations.add(Column.class);
		annotations.add(Id.class);
		annotations.add(IsRoleId.class);
		annotations.add(Enumerated.class);
		annotations.add(Temporal.class);
		annotations.add(Blob.class);
		annotations.add(Group.class);
	}

	public <T> EntityMapping<T> make(Class<T> klass) {
		// 没有Entity注解，就认为他不是一个实体对象.
		if (!klass.isAnnotationPresent(Entity.class)) {
			throw new NoEntityException(klass.getName(), "没有@Entity注解标识 ≡ (^(OO)^) ≡");
		}
		return _makeEntity(klass);
	}

	public <T> RedisEntityMapping<T> makeRedisEntityMapping(Class<T> klass) {
		// 没有Entity注解，就认为他不是一个实体对象.
		if (!klass.isAnnotationPresent(Entity.class)) {
			throw new NoEntityException(klass.getName(), "没有@Entity注解标识 ≡ (^(OO)^) ≡");
		}
		return _makeRedisEntity(klass);
	}

	private <T> RedisEntityMapping<T> _makeRedisEntity(Class<T> klass) {
		// 解析属性
		Field[] fields = ReflectKit.scanAllField(klass, annotations);
		RedisEntityMapping<T> em = new RedisEntityMapping<>(klass);
		// 如果没有写TableName 默认为类的简单名称由驼峰式命名变成分割符分隔单词
		Table table = klass.getAnnotation(Table.class);
		String tableName = (table == null || StringKit.isEmpty(table.name())) ? StringKit.lowerWord(klass.getSimpleName(), '_') : table.name();
		em.setTableName(tableName);

		String[] tns = tableName.split("[{|}]");
		em.setPrefix(tns[0]);
		String key = null;
		if (tns.length > 1) {
			key = tns[1];
			if (tns.length > 2) {
				em.setPrefix(tns[2]);
			}
		}
		ArrayList<FieldMapping> fieldInfo = new ArrayList<>(fields.length);
		for (Field field : fields) {
			FieldMapping fm = _makeFieldMapping(field, em.getMethodAccess());
			fieldInfo.add(fm);

			if (fm.isRoleId()) {
				em.setRoleId(fm);
			}
			if (field.getName().equals(key)) {
				em.setKeyField(fm);
			}
		}
		em.setFieldInfo(fieldInfo);
		return em;
	}

	private <T> EntityMapping<T> _makeEntity(Class<T> klass) {
		AccessorEntityMapping<T> em = new AccessorEntityMapping<>(klass);

		// 如果没有写TableName 默认为类的简单名称由驼峰式命名变成分割符分隔单词
		Table table = klass.getAnnotation(Table.class);
		String tableName = (table == null || StringKit.isEmpty(table.name())) ? StringKit.lowerWord(klass.getSimpleName(), '_') : table.name();
		// 分析表名，如果是动态表名，需解析
		String[] tns = tableName.split("[{|}]");
		em.setTableName(tns[0]);
		String key = null;
		if (tns.length > 1) {
			String[] fs = tns[1].split("#");
			key = fs[0];
			if (fs.length > 1) {
				em.setDynamicTableNameFormat(fs[1]);
			}
		}

		if (table != null) {
			em.setTableComment(table.comment());
		}

		// 解析属性
		Field[] fields = ReflectKit.scanAllField(klass, annotations);

		if (fields.length <= 0) {
			// 一个表没有属性，还ORM个蛋蛋~~
			throw new NoEntityException(klass.getName(), "没有可映射的属性 ≡ (^(OO)^) ≡");
		}

		ArrayList<FieldMapping> fieldInfo = new ArrayList<>(fields.length);
		for (Field field : fields) {
			FieldMapping fm = _makeFieldMapping(field, em.getMethodAccess());
			if (fm.isPrimaryId()) {
				em.setPrimaryId(fm);
			}
			if (fm.isRoleId()) {
				em.setRoleId(fm);
			}
			if (fm.hasGroupBy()) {
				em.setGroupBy(fm);
			}
			fieldInfo.add(fm);

			if (key != null && field.getName().equals(key)) {
				em.setDynamicTableNameField(fm);
			}
		}
		Collections.sort(fieldInfo);
		em.setFieldInfo(fieldInfo);
		return em;
	}

	private FieldMapping _makeFieldMapping(Field field, MethodAccess methodAccess) {
		FieldMapping fm = new FieldMapping(field, methodAccess);
		// 需要解析的解析，有些不要用动的还放注解里面
		if (fm.getColumn() == null || StringKit.isEmpty(fm.getColumn().name())) {
			fm.setColumnName(StringKit.lowerWord(field.getName(), '_'));
		} else {
			fm.setColumnName(fm.getColumn().name());
		}

		Jdbcs.guessEntityFieldColumnType(fm);
		return fm;
	}
}
