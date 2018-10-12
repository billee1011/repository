package com.lingyu.noark.data.accessor.sql;

import org.junit.Test;

import com.lingyu.noark.data.accessor.AnnotationEntityMaker;
import com.lingyu.noark.data.accessor.mysql.MysqlSqlExpert;
import com.lingyu.noark.data.accessor.mysql.SqlExpert;
import com.lingyu.noark.data.entity.Item;

public class SqlExpertTest {

	@Test
	public void testGetCreateTableSql() {
		SqlExpert expert = new MysqlSqlExpert();
		AnnotationEntityMaker maker = new AnnotationEntityMaker();

		System.out.println(expert.genCreateTableSql(maker.make(Item.class)));
	}

	@Test
	public void testGetInsterSql() {
		SqlExpert expert = new MysqlSqlExpert();
		AnnotationEntityMaker maker = new AnnotationEntityMaker();
		System.out.println(expert.genInsertSql(maker.make(Item.class)));
	}

	@Test
	public void testGetUpdateSql() {
		SqlExpert expert = new MysqlSqlExpert();
		AnnotationEntityMaker maker = new AnnotationEntityMaker();
		System.out.println(expert.genUpdateSql(maker.make(Item.class)));
	}

}
