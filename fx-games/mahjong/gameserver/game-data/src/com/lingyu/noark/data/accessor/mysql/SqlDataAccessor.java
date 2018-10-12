package com.lingyu.noark.data.accessor.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.data.DataManager;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.accessor.AbstractDataAccessor;
import com.lingyu.noark.data.exception.DataAccessException;
import com.lingyu.noark.data.exception.DataException;

/**
 * SQL存储策略入口.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public abstract class SqlDataAccessor extends AbstractDataAccessor {

	private static final Logger logger = LogManager.getLogger(DataManager.class);
	protected final SqlExpert expert;
	protected final DataSource dataSource;

	public SqlDataAccessor(SqlExpert expert, DataSource dataSource) {
		this.expert = expert;
		this.dataSource = dataSource;
	}

	protected <T> T execute(ConnectionCallback<T> action) {
		try (Connection con = dataSource.getConnection()) {
			return action.doInConnection(con);
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
	}

	protected <T> T execute(StatementCallback<T> action) {
		try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement()) {
			return action.doInStatement(stmt);
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
	}

	protected <T> T execute(PreparedStatementCallback<T> action, String sql) {
		try (Connection con = dataSource.getConnection(); PreparedStatement pstmt = con.prepareStatement(sql)) {
			return action.doInPreparedStatement(pstmt);
		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * 判定一个表是否存在.
	 */
	protected boolean exists(final String tableName) {
		return this.execute(new StatementCallback<Boolean>() {
			@Override
			public Boolean doInStatement(Statement stmt) throws SQLException {
				String sql = "SELECT COUNT(1) FROM " + tableName + " where 1!=1";
				try (ResultSet rs = stmt.executeQuery(sql)) {
					return rs.next();
				} catch (Exception e) {
					// 有异常就是表不存在嘛~~~~
					return Boolean.FALSE;
				}
			}
		});
	}

	/**
	 * 检查一下表结构是不是跟这个实体一样一样的.
	 */
	@Override
	public synchronized <T> void checkupEntityFieldsWithDatabase(EntityMapping<T> em) {
		// 先判定一下，存不存在
		if (this.exists(em.getTableName())) {
			this.checkEntityTable(em);
		} else {
			// 不存在，直接创建
			this.createEntityTable(em);
		}
	}

	private synchronized <T> void checkEntityTable(final EntityMapping<T> em) {
		final String sql = "SELECT * FROM " + em.getTableName() + " LIMIT 1";
		this.execute(new StatementCallback<Void>() {
			@Override
			public Void doInStatement(Statement stmt) throws SQLException {
				try (ResultSet rs = stmt.executeQuery(sql)) {
					ResultSetMetaData rsmd = rs.getMetaData();
					int columnCount = rsmd.getColumnCount();

					// 当表字段比属性多时...
					if (columnCount > em.getFieldMapping().size()) {
						for (int i = 1; i <= columnCount; i++) {
							String columnName = rsmd.getColumnName(i);
							boolean exit = false;
							for (FieldMapping fm : em.getFieldMapping()) {
								if (fm.getColumnName().equals(columnName)) {
									exit = true;
									break;
								} else if (fm.getColumnName().equalsIgnoreCase(columnName)) {
									exit = true;
									String entity = em.getEntityClass().getName();
									String field = fm.getField().getName();
									logger.warn("字段名大小写不匹配,建议修正! table={},column={},entity={},field={}", em.getTableName(), columnName, entity, field);
									break;
								}
							}
							if (!exit) {
								throw new DataException("表结构字段比实体类属性多. 表[" + em.getTableName() + "]中的属性：" + columnName);
							}
						}
					}

					// 循环字段检查，如果属性比字段多，就自动补上...
					for (FieldMapping fm : em.getFieldMapping()) {
						boolean exit = false;
						for (int i = 1; i <= columnCount; i++) {
							String columnName = rsmd.getColumnName(i);
							if (fm.getColumnName().equals(columnName)) {
								exit = true;
								break;
							} else if (fm.getColumnName().equalsIgnoreCase(columnName)) {
								exit = true;
								String entity = em.getEntityClass().getName();
								String field = fm.getField().getName();
								logger.warn("字段名不匹配,建议修正! table={},column={},entity={},field={}", em.getTableName(), columnName, entity, field);
								break;
							}
						}
						if (!exit) {
							// 修补属性
							aotuUpdateTable(em, fm);
							tryRepairTextDefaultValue(em, fm);
						}
					}
				}
				return null;
			}
		});
	}

	// 如果是Text智能修补一下默认值.
	private <T> void tryRepairTextDefaultValue(final EntityMapping<T> em, final FieldMapping fm) {
		// 修正Text字段的默认值.
		if (fm.getWidth() >= 65535 && fm.hasDefaultValue()) {
			final String sql = expert.genUpdateDefaultValueSql(em, fm);
			logger.info("实体类[{}]中的字段[{}]不支持默认值，准备智能修补默认值，SQL如下:\n{}", em.getEntityClass(), fm.getColumnName(), sql);
			this.execute(new StatementCallback<Void>() {
				@Override
				public Void doInStatement(Statement stmt) throws SQLException {
					stmt.executeUpdate(sql);
					return null;
				}
			});
		}
	}

	// 自动修补表结构...
	private <T> void aotuUpdateTable(final EntityMapping<T> em, final FieldMapping fm) {
		final String sql = expert.genAddTableColumnSql(em, fm);
		logger.info("实体类[{}]对应的数据库表结构不一致，准备自动修补表结构，SQL如下:\n{}", em.getEntityClass(), sql);
		this.execute(new StatementCallback<Void>() {
			@Override
			public Void doInStatement(Statement stmt) throws SQLException {
				stmt.executeUpdate(sql);
				return null;
			}
		});
	}

	/**
	 * 创建实体对应的数据库表结构.
	 */
	private synchronized <T> void createEntityTable(EntityMapping<T> em) {
		final String sql = expert.genCreateTableSql(em);
		logger.warn("实体类[{}]对应的数据库表不存在，准备自动创建表结构，SQL如下:\n{}", em.getEntityClass(), sql);
		this.execute(new StatementCallback<Integer>() {
			@Override
			public Integer doInStatement(Statement stmt) throws SQLException {
				try {
					return stmt.executeUpdate(sql);
				} catch (Exception e) {
					throw new DataAccessException(e);
				}
			}
		});
	}
}
