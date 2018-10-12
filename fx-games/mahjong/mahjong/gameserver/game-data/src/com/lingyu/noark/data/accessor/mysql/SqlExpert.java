package com.lingyu.noark.data.accessor.mysql;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.accessor.AccessorEntityMapping;
import com.lingyu.noark.data.accessor.Sort;

public interface SqlExpert {
	public <T> String genNInsertSql(EntityMapping<T> em, T entity);
	/**
	 * 获取创建表的SQL语句.
	 */
	<T> String genCreateTableSql(EntityMapping<T> em);

	<T> String genInsertSql(EntityMapping<T> em);

	<T> String genUpdateSql(EntityMapping<T> em);

	<T> String genSeleteByRoleId(EntityMapping<T> sem);

	<T> String genSeleteByPageSql(EntityMapping<T> sem);

	<T> String genSeleteByAndSortPageSql(EntityMapping<T> sem, Sort sort);

	<T> String genSeleteByRoleIdAndPage(EntityMapping<T> sem);

	<T> String genSeleteByRoleIdAndSortAndPageSql(EntityMapping<T> sem, Sort sort);

	<T> String genSeleteByCount(EntityMapping<T> sem);

	<T> String genSeleteByRoleIdAndCount(EntityMapping<T> sem);

	<T> String genDeleteSql(AccessorEntityMapping<T> sem);

	<T> String genSeleteSql(AccessorEntityMapping<T> sem);

	<T> String genSeleteAllSql(AccessorEntityMapping<T> sem);

	<T> String genSeleteByGroup(AccessorEntityMapping<T> sem);

	// 生成带值的一条语句
	<T> String genInsertSql(EntityMapping<T> em, T entity);

	<T> String genUpdateSql(EntityMapping<T> em, T entity);

	<T> String genDeleteSql(EntityMapping<T> em, T entity);

	/**
	 * 生成添加表字段的SQL
	 */
	<T> String genAddTableColumnSql(EntityMapping<T> em, FieldMapping fm);

	<T> String genUpdateDefaultValueSql(EntityMapping<T> em, FieldMapping fm);
}