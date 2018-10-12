package com.lingyu.noark.data;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class AbstractRepositoryTest {

	protected static DataManager manager = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ComboPooledDataSource cpds = new ComboPooledDataSource(true);
		cpds.setDataSourceName("mydatasource");
		cpds.setJdbcUrl("jdbc:mysql://192.168.1.21:3306/wow_zmk?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf-8");
		cpds.setDriverClass("com.mysql.jdbc.Driver");
		cpds.setUser("linyu");
		cpds.setPassword("com.123");
		cpds.setInitialPoolSize(5);
//		manager = new DataManager(cpds, false, 1, 10,false);
		manager = new DataManager(cpds, 1, 10,false);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		manager.shutdown();
	}
}
