package com.lingyu.common.db;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import com.google.common.base.Throwables;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.util.PropertyUtil;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.msg.http.FieldDTO;
import com.lingyu.msg.http.QueryDTO;
import com.lingyu.msg.http.Query_S2C_Msg;
import com.mchange.v2.c3p0.C3P0ProxyStatement;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class GameRepository {
	private static final Logger logger = LogManager.getLogger(GameRepository.class);
	private JdbcTemplate jdbcTemplate;
	private DataSource dataSource;
	private static final String SHOW_CREATE_TABLE = "show create table ";
	private static final String SHOW_TABLES = "show tables";
	private static final String SQL_GET_COUNT = "select count(1) from {0}";
	private static final String SQL_TRUNCATE = "truncate table {0}";
	private static final String LOAD_DATA_SQL = "load data local infile '''' into table {0} character set utf8 fields terminated by ''|'' lines terminated by ''\n''";
	private static final String INFILE_MUTATOR_METHOD = "setLocalInfileInputStream";
	private String host;
	private String port;
	private String database;
	private String userName;
	private String password;
	private static Method method;

	static {
		try {
			method = com.mysql.jdbc.Statement.class.getMethod(INFILE_MUTATOR_METHOD, new Class[] { InputStream.class });
		} catch (NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public static void main(String[] args) {
		// GameRepository repository = new GameRepository();
		// repository.init("192.168.1.67", 3306, "wow_jgl", "linyu", "com.123");
		// Collection<String> list = repository.showTables().values();
		// for (String e : list) {
		// boolean ret = repository.isExisted(e, "role_id");
		// if (ret) {
		// logger.info("table={}", e);
		// }
		// }

		// boolean ret = repository.isExisted("role", "role_id");
		// logger.info("table={},ret={}", "role", ret);
		// int num = repository.getCount("user");
		// Map<String, String> map = repository.showTables();

		// for (String table : MergeManager.BLACK_TABLE) {
		// map.remove(table);
		// }
		// Collection<String> list = map.values();
		// for (String table : list) {
		// if (table.indexOf(MergeManager.TEMPLATE) < 0) {
		// repository.getCount(table);
		// }
		// }
	}

	public void init(String ip, int port, String dbName, String userName, String password) throws ServiceException {
		ComboPooledDataSource dataSource = this.initDataSource(ip, port, dbName, userName, password);
		jdbcTemplate = new JdbcTemplate(dataSource);
		logger.info("初始化GameRepository成功:ip={},port={},dbName={},userName={},password={}", ip, port, dbName, userName,
				password);
	}

	private ComboPooledDataSource initDataSource(String ip, int port, String dbName, String userName, String password) {
		String jdbcUrl = "jdbc:mysql://{0}:{1}/{2}?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF-8";
		jdbcUrl = MessageFormat.format(jdbcUrl, ip, String.valueOf(port), dbName);
		return this.initDataSource(jdbcUrl, userName, password);
	}

	private ComboPooledDataSource initDataSource(String jdbcUrl, String userName, String password) {
		ComboPooledDataSource ret = new ComboPooledDataSource();
		try {
			ret.setDriverClass("com.mysql.jdbc.Driver");
		} catch (PropertyVetoException e) {
			throw new ServiceException(e);
		}
		ret.setJdbcUrl(jdbcUrl);
		ret.setUser(userName);
		ret.setPassword(password);
		ret.setInitialPoolSize(1);
		ret.setMinPoolSize(2);
		ret.setAcquireIncrement(3);
		ret.setMaxPoolSize(50);
		ret.setMaxIdleTime(3600);
		ret.setMaxStatements(0);
		ret.setMaxStatementsPerConnection(100);
		ret.setPreferredTestQuery("select 1");
		// ret.setIdleConnectionTestPeriod(PropertyUtil.getInt(
		// config.getDbConfig(), "idleConnectionTestPeriod"));
		this.dataSource = ret;
		return ret;
	}

	public void init(Properties dbConfig) throws ServiceException {
		ComboPooledDataSource dataSource = this.initDataSource(dbConfig);
		jdbcTemplate = new JdbcTemplate(dataSource);
		logger.info("初始化GameRepository成功:ip={},port={},dbName={},userName={},password={}", host, port, database,
				userName, password);
	}

	public ComboPooledDataSource initDataSource(Properties dbConfig) {
		ComboPooledDataSource ret = new ComboPooledDataSource();
		try {
			ret.setDriverClass(PropertyUtil.getString(dbConfig, "jdbcDriver"));
		} catch (PropertyVetoException e) {
			throw new ServiceException(e);
		}
		String jdbcUrl = PropertyUtil.getString(dbConfig, "jdbcUrl");
		String url = StringUtils.substringBetween(jdbcUrl, "mysql://", "?");
		host = StringUtils.substringBefore(url, ":");
		port = StringUtils.substringBetween(url, ":", "/");
		database = StringUtils.substringAfter(url, "/");
		// jdbc:mysql://192.168.1.21:3306/wow_tencent_admin?
		ret.setJdbcUrl(jdbcUrl);
		userName = PropertyUtil.getString(dbConfig, "jdbcUser");
		ret.setUser(userName);
		password = PropertyUtil.getString(dbConfig, "jdbcPassword");
		ret.setPassword(password);
		ret.setInitialPoolSize(PropertyUtil.getInt(dbConfig, "initialPoolSize"));
		ret.setMinPoolSize(PropertyUtil.getInt(dbConfig, "minPoolSize"));
		ret.setAcquireIncrement(PropertyUtil.getInt(dbConfig, "acquireIncrement"));
		ret.setMaxPoolSize(PropertyUtil.getInt(dbConfig, "maxPoolSize"));
		ret.setMaxIdleTime(PropertyUtil.getInt(dbConfig, "maxIdleTime"));
		ret.setMaxStatements(PropertyUtil.getInt(dbConfig, "maxStatements"));
		ret.setMaxStatementsPerConnection(PropertyUtil.getInt(dbConfig, "maxStatementsPerConnection"));
		ret.setPreferredTestQuery(PropertyUtil.getString(dbConfig, "preferredTestQuery"));
		// ret.setIdleConnectionTestPeriod(PropertyUtil.getInt(
		// config.getDbConfig(), "idleConnectionTestPeriod"));
		this.dataSource = ret;
		return ret;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void execute(String sql) {
		jdbcTemplate.execute(sql);
	}

	public boolean isExisted(String tableName, String field) {
		boolean ret = false;
		List<Map<String, Object>> list = this.selectQuery("desc " + tableName + " " + field);
		for (Map<String, Object> e : list) {
			ret = e.containsKey("Field");
			return ret;
		}
		return ret;
	}

	/**
	 * 后台执行sql查询
	 * 
	 * @return
	 */
	public List<Map<String, Object>> selectQuery(String sql) {
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
	}

	private String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (name == null || name.length() < 1) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}

	public Query_S2C_Msg handleSelect(String sql) {
		Query_S2C_Msg ret = new Query_S2C_Msg();
		logger.info("开始查询 sql={}", sql);
		long begin = System.currentTimeMillis();
		int retCode = ErrorCode.EC_OK;
		try {
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			if (sql.startsWith("kill")) {

				this.execute(sql);
			} else {
				list = this.selectQuery(sql);
			}
			logger.info("查询花费  interval={} ms", System.currentTimeMillis() - begin);
			if (CollectionUtils.isNotEmpty(list)) {
				for (Map<String, Object> e : list) {
					QueryDTO queryDTO = new QueryDTO();
					Set<Entry<String, Object>> set = e.entrySet();
					for (Entry<String, Object> entry : set) {
						Object value = entry.getValue();
						String name = entry.getKey();
						FieldDTO dto = new FieldDTO();
						dto.setName(name);
						if (value == null) {
							dto.setValue("");
						} else {
							dto.setValue(value.toString());
						}
						queryDTO.add(dto);
					}
					ret.add(queryDTO);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			retCode = ErrorCode.EC_FAILED;
		}
		long interval = System.currentTimeMillis() - begin;
		logger.info("总花费  interval={} ms", interval);
		ret.setInterval(interval);
		ret.setRetCode(retCode);
		return ret;
	}

	/** 获取创表语句 */
	public String getCreateTableSql(String tableName, Date date, String format) {
		String ret = "";
		List<Map<String, Object>> list = this.selectQuery(SHOW_CREATE_TABLE + tableName);
		for (Map<String, Object> e : list) {
			if (e.containsKey("Create Table")) {
				ret = String.valueOf(e.get("Create Table"));
				ret = StringUtils.replace(ret, "CREATE TABLE", "CREATE TABLE IF NOT EXISTS");
				ret = StringUtils.replace(ret, tableName, tableName + "_" + TimeUtil.format(date, format));
				return ret;
			}

		}
		return ret;
	}

	/** 获取创表语句 */
	public Map<String, String> showTables() {
		Map<String, String> ret = new HashMap<>();
		List<Map<String, Object>> list = this.selectQuery(SHOW_TABLES);
		for (Map<String, Object> e : list) {
			Collection<Object> values = e.values();
			for (Object value : values) {
				String table = String.valueOf(value);
				ret.put(table, table);
			}
		}
		return ret;
	}

	public int update(String sql, Object... args) {
		return jdbcTemplate.update(sql, args);
	}

	public int update(String sql) {
		return jdbcTemplate.update(sql);
	}

	/** 删除满足条件的角色 */
	public List<Long> getRemoveRoleIdList(int startLevel, int endLevel, int day) {
		Date date = DateUtils.addDays(new Date(), -day);
		List<Long> ret = jdbcTemplate.queryForList(
				"select id from role where status=0 and level>=? and level<? and last_login_time<?", Long.class,
				startLevel, endLevel, date);
		return ret;
	}

	/** 删除满足条件的角色 */
	public List<Long> getGuildLeaderIdList() {
		List<Long> ret = jdbcTemplate.queryForList("select deacon_role_id from guild", Long.class);
		return ret;
	}

	public int getCount(String table) {
		// try{
		// int num =
		// jdbcTemplate.queryForObject(MessageFormat.format("select count(*)
		// from {0} where role_id=0",
		// table), Integer.class);
		// if (num > 0) {
		// logger.info("table={}", table);
		// }
		//
		// }catch(Exception e){
		//
		// }

		return jdbcTemplate.queryForObject(MessageFormat.format(SQL_GET_COUNT, table), Integer.class);
	}

	public void truncate(String table) {
		jdbcTemplate.update(MessageFormat.format(SQL_TRUNCATE, table));
	}

	/**
	 * load data infile '/tmp/file.txt' into table create_role_log fields
	 * terminated by '|' lines terminated by '\n'; load bulk data from
	 * InputStream to MySQL
	 * "load data infile '$filename' into table test fields terminated by '|'"
	 */
	public boolean bulkCommit(String tableName, String value) {
		boolean ret = false;
		if (tableName == null || StringUtils.isEmpty(value)) {
			logger.warn("tableName or value is illegal");
			return ret;
		}
		String script = MessageFormat.format(LOAD_DATA_SQL, tableName);
		byte[] bytes = value.getBytes();
		InputStream inputStream = new ByteArrayInputStream(bytes);
		Connection conn = null;
		Statement statment = null;
		try {
			conn = DataSourceUtils.getConnection(dataSource);
			conn.setAutoCommit(false);
			Statement statement = conn.createStatement();
			this.setLocalInfileInputStream(statement, inputStream);
			statement.execute(script);
			conn.commit();
			ret = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			JdbcUtils.closeStatement(statment);
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
		return ret;
	}

	private void setLocalInfileInputStream(Statement statement, InputStream inputStream) throws SQLException {
		try {
			C3P0ProxyStatement proxyStatement = (C3P0ProxyStatement) statement;
			proxyStatement.rawStatementOperation(method, C3P0ProxyStatement.RAW_STATEMENT,
					new Object[] { inputStream });
		} catch (IllegalAccessException e) {
			throw Throwables.propagate(e);
		} catch (InvocationTargetException e) {
			throw Throwables.propagate(e);
		}
	}

}
