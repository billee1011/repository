package com.lingyu.common.db;

import java.beans.PropertyVetoException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.util.PropertyUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class GameRepository {
	private static final Logger logger = LogManager.getLogger(GameRepository.class);
	private JdbcTemplate jdbcTemplate;
	private DataSource dataSource;

	public void init(ServerConfig config) throws ServiceException {
		ComboPooledDataSource dataSource = this.initDataSource(config);
		jdbcTemplate = new JdbcTemplate(dataSource);
		logger.info("初始化GameRepository成功:{}", config.getDbConfig().get("jdbcUrl"));
	}

	private ComboPooledDataSource initDataSource(ServerConfig config) {
		ComboPooledDataSource ret = new ComboPooledDataSource();
		try {
			ret.setDriverClass(PropertyUtil.getString(config.getDbConfig(), "jdbcDriver"));
		} catch (PropertyVetoException e) {
			throw new ServiceException(e);
		}
		ret.setJdbcUrl(PropertyUtil.getString(config.getDbConfig(), "jdbcUrl"));
		ret.setUser(PropertyUtil.getString(config.getDbConfig(), "jdbcUser"));
		ret.setPassword(PropertyUtil.getString(config.getDbConfig(), "jdbcPassword"));
		ret.setInitialPoolSize(PropertyUtil.getInt(config.getDbConfig(), "initialPoolSize"));
		ret.setMinPoolSize(PropertyUtil.getInt(config.getDbConfig(), "minPoolSize"));
		ret.setAcquireIncrement(PropertyUtil.getInt(config.getDbConfig(), "acquireIncrement"));
		ret.setMaxPoolSize(PropertyUtil.getInt(config.getDbConfig(), "maxPoolSize"));
		ret.setMaxIdleTime(PropertyUtil.getInt(config.getDbConfig(), "maxIdleTime"));
		ret.setMaxStatements(PropertyUtil.getInt(config.getDbConfig(), "maxStatements"));
		ret.setMaxStatementsPerConnection(PropertyUtil.getInt(config.getDbConfig(), "maxStatementsPerConnection"));
		ret.setPreferredTestQuery(PropertyUtil.getString(config.getDbConfig(), "preferredTestQuery"));
		// ret.setIdleConnectionTestPeriod(PropertyUtil.getInt(
		// config.getDbConfig(), "idleConnectionTestPeriod"));
		this.dataSource = ret;
		return ret;
	}

	/** 获取变量的值 */
	public String getValue(String variableName) {
		String ret = "";
		String sql = StringUtils.replace("show variables where variable_name='{}'", "{}", variableName);
		List<Map<String, Object>> list = this.selectQuery(sql);
		for (Map<String, Object> e : list) {
			if (e.containsKey("Value")) {
				ret = String.valueOf(e.get("Value"));
				return ret;
			}
		}
		return ret;
	}

	// public boolean isExist(String sql){
	// List<Map<String, Object>> list = this.selectQuery(sql);
	// return true;
	// }

	/** 数据库合法性检测 */
	public boolean checkDBValid() {
		boolean ret = true;
		// 存储过程是否存在检测,取消存储过程检测
		//this.selectQuery("show create procedure stat_remain_player");
		// 参数检查
		String value = this.getValue("explicit_defaults_for_timestamp");
		if (StringUtils.equals(value, "ON")) {
			ret = false;
			throw new ServiceException(
					"DB 配置不满足需求，你需要设置 MySql explicit_defaults_for_timestamp=false,确认[show variables where Variable_name='explicit_defaults_for_timestamp']");
		}
//		// 编码检查
//		if (StringUtils.equals("utf8", this.getValue("character_set_database")) && StringUtils.equals("utf8", this.getValue("character_set_results"))
//				&& StringUtils.equals("utf8", this.getValue("character_set_system")) && StringUtils.equals("utf8", this.getValue("character_set_server"))) {
//			logger.info("数据库编码检查正确 character={}", "utf8");
//		} else {
//			ret = false;
//			throw new ServiceException("DB 编码不满足需求，你需要设置 MySql character_set_database,character_set_results,character_set_system,character_set_server 为 utf8");
//		}
		return ret;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void execute(String sql) {
		jdbcTemplate.execute(sql);
	}
	public int update(String sql,Object... args){
		return jdbcTemplate.update(sql, args);
	}
	public int update(String sql){
		return jdbcTemplate.update(sql);
	}

	/**
	 * 后台执行sql查询
	 * 
	 * @return
	 */
	public List<Map<String, Object>> selectQuery(String sql) {
		// 这里要抓捕异常，以免mysql没有授权
		try {
			List<Map<String, Object>> ret = jdbcTemplate.query(sql, new RowMapper<Map<String, Object>>() {
				@Override
				public Map<String, Object> mapRow(ResultSet resultset, int j) throws SQLException {
					ResultSetMetaData rsmd = resultset.getMetaData();
					int columnCount = rsmd.getColumnCount();
					Map<String, Object> mapOfColValues = new LinkedHashMap<String, Object>();
					for (int i = 1; i <= columnCount; i++) {
						String key = lookupColumnName(rsmd, i);
						Object obj = resultset.getObject(i);
						mapOfColValues.put(key, obj);
					}
					return mapOfColValues;
				}
			});
			return ret;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
		
	}

	private String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (name == null || name.length() < 1) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}
	
	/** 获取最大的编号 */
	public long getMaxId(String tableName) {
		String sql ="select max(id) from " + tableName;
		Long maxId = jdbcTemplate.queryForObject(sql, Long.class);
		return maxId == null ? 0 : maxId;
	}
	
	/** 获取所有注册角色 */
	public int getAllRegistRoleNum() {
		return jdbcTemplate.queryForObject("select count(id) from role", Integer.class);
	}
	
	public Map<String, Long> getAllNameMap() {
		final Map<String, Long> ret = new HashMap<>();
		jdbcTemplate.query("select id,name from role", new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				String name = rs.getString("name");
				Long id = rs.getLong("id");
				ret.put(name, id);

			}
		});
		return ret;
	}
}
