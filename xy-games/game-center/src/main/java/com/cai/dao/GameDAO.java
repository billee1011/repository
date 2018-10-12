package com.cai.dao;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.ibatis.sqlmap.client.SqlMapClient;

//@Repository
public class GameDAO extends CoreDao{

	
	@Autowired
	@Qualifier("dataSourceGame")
	public void setDataSource2(DataSource dataSource) {
		super.setDataSource(dataSource);
	}
	
	@Autowired
	@Qualifier("sqlMapClientGame")
	public void setSqlMapClient2(SqlMapClient sqlMapClient){
		super.setSqlMapClient(sqlMapClient);;
	}
	
	
	
	
	
	
	
	
}
