package com.lingyu.noark.data.repository;

import com.lingyu.noark.data.accessor.mysql.MysqlSqlExpert;
import com.lingyu.noark.data.accessor.mysql.SqlExpert;

/**
 * LogRepository类为了实现文本形式的日志而生的.
 * <p>
 * 他的功能中增，删，改功能是不走DB的只是生成一条对应的SQL，但有查询功能
 * 
 * @author 小流氓<176543888@qq.com>
 * 
 * @param <T> 实体类型
 */
public class LogRepository<T> extends SqlRepository<T> {
	private static final SqlExpert expert = new MysqlSqlExpert();

	/**
	 * 初始化ORM，检测实体和表结构.
	 */
	protected LogRepository() {
		super();
	}
	public String logNInsert(T entity) {
		return expert.genNInsertSql(entityMapping, entity);
	}
	
	public String logInsert(T entity) {
		return expert.genInsertSql(entityMapping, entity);
	}

	public String logUpdate(T entity) {
		return expert.genUpdateSql(entityMapping, entity);
	}

	public String logDelete(T entity) {
		return expert.genDeleteSql(entityMapping, entity);
	}
}