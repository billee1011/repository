package com.cai.dao;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.ibatis.sqlmap.client.SqlMapClient;

//@Repository
public class PublicDataDAO extends CoreDao{

	
	@Autowired
	@Qualifier("dataSourcePublicData")
	public void setDataSource2(DataSource dataSource) {
		super.setDataSource(dataSource);
	}
	
	@Autowired
	@Qualifier("sqlMapClientPlblicData")
	public void setSqlMapClient2(SqlMapClient sqlMapClient){
		super.setSqlMapClient(sqlMapClient);;
	}
	
	

	
	
	
}
