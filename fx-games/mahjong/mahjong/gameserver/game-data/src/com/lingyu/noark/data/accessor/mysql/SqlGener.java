package com.lingyu.noark.data.accessor.mysql;

import com.lingyu.noark.data.accessor.Sort;

//提供SQL所需的接口
public interface SqlGener {

	public String getInsterSql(SqlExpert expert);

	public String getDeleteSql(SqlExpert expert);

	public String getUpdateSql(SqlExpert expert);

	public String getSeleteSql(SqlExpert expert);

	public String getSeleteAllSql(SqlExpert expert);

	public String getSeleteByRoleId(SqlExpert expert);

	public String getSeleteByRoleIdAndPage(SqlExpert expert, Sort sort);

	public String getSeleteByPage(SqlExpert expert, Sort sort);

	public String getSeleteByGroupBy(SqlExpert expert);

	public String getSeleteByRoleIdAndCount(SqlExpert expert);

	public String getSeleteByCount(SqlExpert expert);
}