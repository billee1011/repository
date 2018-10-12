package com.lingyu.noark.data.accessor.mysql;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.OperateType;
import com.lingyu.noark.data.accessor.Page;
import com.lingyu.noark.data.accessor.Pageable;
import com.lingyu.noark.data.exception.DataException;

public class MysqlDataAccessor extends SqlDataAccessor {

	public MysqlDataAccessor(DataSource dataSource) {
		super(new MysqlSqlExpert(), dataSource);
	}

	@Override
	public <T> int insert(final EntityMapping<T> em, final T entity) {
		class InsterPreparedStatementCallback implements PreparedStatementCallback<Integer> {
			private int index = 1;

			@Override
			public Integer doInPreparedStatement(PreparedStatement pstmt) throws SQLException, IllegalArgumentException, IllegalAccessException {
				for (FieldMapping fm : em.getFieldMapping()) {
					fm.getAdaptor().set(pstmt, fm, entity, index++);
				}
				return pstmt.executeUpdate();
			}
		}
		return execute(new InsterPreparedStatementCallback(), ((SqlGener) em).getInsterSql(expert));
	}

	@Override
	public <T> int delete(final EntityMapping<T> em, final T entity) {
		return delete(em, em.getPrimaryIdValue(entity));
	}

	// 这个接口不要暴露出来
	private <K extends Serializable> int delete(final EntityMapping<?> em, final K id) {
		class DeletePreparedStatementCallback implements PreparedStatementCallback<Integer> {
			@Override
			public Integer doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				pstmt.setObject(1, id);
				return pstmt.executeUpdate();
			}
		}
		return execute(new DeletePreparedStatementCallback(), ((SqlGener) em).getDeleteSql(expert));
	}

	@Override
	public <T> int update(final EntityMapping<T> em, final T entity) {
		class UpdatePreparedStatementCallback implements PreparedStatementCallback<Integer> {
			private int index = 1;

			@Override
			public Integer doInPreparedStatement(PreparedStatement pstmt) throws SQLException, IllegalArgumentException, IllegalAccessException {
				for (FieldMapping fm : em.getFieldMapping()) {
					if (fm.isPrimaryId()) {
						continue;
					}
					fm.getAdaptor().set(pstmt, fm, entity, index++);
				}
				em.getPrimaryId().getAdaptor().set(pstmt, em.getPrimaryId(), entity, index);
				return pstmt.executeUpdate();
			}
		}
		return execute(new UpdatePreparedStatementCallback(), ((SqlGener) em).getUpdateSql(expert));
	}

	@Override
	public <T, K extends Serializable> T load(final EntityMapping<T> em, Serializable roleId, final K id) {
		class LoadPreparedStatementCallback implements PreparedStatementCallback<T> {
			@Override
			public T doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				pstmt.setObject(1, id);
				try (ResultSet rs = pstmt.executeQuery()) {
					return rs.next() ? em.newEntity(rs) : null;
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new LoadPreparedStatementCallback(), ((SqlGener) em).getSeleteSql(expert));
	}

	@Override
	public <T> List<T> loadAll(final EntityMapping<T> em) {
		class LoadAllPreparedStatementCallback implements PreparedStatementCallback<List<T>> {
			@Override
			public List<T> doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				try (ResultSet rs = pstmt.executeQuery()) {
					return em.newEntityList(rs);
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new LoadAllPreparedStatementCallback(), ((SqlGener) em).getSeleteAllSql(expert));
	}

	@Override
	public <T> List<T> loadByRoleId(final Serializable roleId, final EntityMapping<T> em) {
		class LoadByRoleIdPreparedStatementCallback implements PreparedStatementCallback<List<T>> {
			@Override
			public List<T> doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				pstmt.setObject(1, roleId);

				try (ResultSet rs = pstmt.executeQuery()) {
					return em.newEntityList(rs);
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new LoadByRoleIdPreparedStatementCallback(), ((SqlGener) em).getSeleteByRoleId(expert));
	}

	@Override
	public <T> void writeByRoleId(Serializable roleId, OperateType type, List<T> entitys) {

	}

	// --------SQL查询系列---------------------------------
	@Override
	public <T> List<T> queryForList(final EntityMapping<T> em, String sql, final Object... args) {
		class QueryForListCallback implements PreparedStatementCallback<List<T>> {
			@Override
			public List<T> doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					return em.newEntityList(rs);
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new QueryForListCallback(), sql);
	}

	@Override
	public <T> long queryForLong(final EntityMapping<T> em, String sql, final Object... args) {
		class QueryForLongCallback implements PreparedStatementCallback<Long> {
			@Override
			public Long doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					return rs.next() ? rs.getLong(1) : 0L;
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new QueryForLongCallback(), sql);
	}

	@Override
	public <T> T queryForObject(final EntityMapping<T> em, String sql, final Object... args) {
		class QueryForObjectCallback implements PreparedStatementCallback<T> {
			@Override
			public T doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					return rs.next() ? (T) em.newEntity(rs) : null;
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new QueryForObjectCallback(), sql);
	}

	@Override
	public <T> int queryForInt(final EntityMapping<T> em, String sql, final Object... args) {
		class QueryForIntCallback implements PreparedStatementCallback<Integer> {
			@Override
			public Integer doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					return rs.next() ? rs.getInt(1) : 0;
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new QueryForIntCallback(), sql);
	}

	@Override
	public <T> Map<String, Object> queryForMap(final EntityMapping<T> em, String sql, final Object... args) {
		class QueryForMapCallback implements PreparedStatementCallback<Map<String, Object>> {
			@Override
			public Map<String, Object> doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						Map<String, Object> result = new HashMap<>();
						ResultSetMetaData rsmd = rs.getMetaData();
						int count = rsmd.getColumnCount();
						for (int i = 1; i <= count; i++) {
							String key = rsmd.getColumnName(i);
							result.put(key, rs.getObject(i));
						}
						return result;
					} else {
						return Collections.emptyMap();
					}
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new QueryForMapCallback(), sql);
	}

	@Override
	public <E> List<E> queryForList(final EntityMapping<E> em, String sql, final RowMapper<E> mapper, final Object... args) {
		class QueryForListCallback implements PreparedStatementCallback<List<E>> {
			@Override
			public List<E> doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					List<E> es = new ArrayList<E>();
					while (rs.next()) {
						es.add(mapper.mapRow(rs));
					}
					return es;
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new QueryForListCallback(), sql);
	}

	@Override
	public <T> List<T> loadByGroup(final EntityMapping<T> em, final Serializable groupId) {
		class LoadByRoleIdPreparedStatementCallback implements PreparedStatementCallback<List<T>> {
			@Override
			public List<T> doInPreparedStatement(PreparedStatement pstmt) throws SQLException {
				pstmt.setObject(1, groupId);

				try (ResultSet rs = pstmt.executeQuery()) {
					return em.newEntityList(rs);
				} catch (Exception e) {
					throw new DataException("加载数据时异常，请查看实体类[" + em.getEntityClass().getName() + "]配置", e);
				}
			}
		}
		return execute(new LoadByRoleIdPreparedStatementCallback(), ((SqlGener) em).getSeleteByGroupBy(expert));
	}

	// limit是mysql的语法
	// select * from table limit m,n
	// 其中m是指记录开始的index，从0开始，表示第一条记录
	// n是指从第m+1条开始，取n条。
	// select * from tablename limit 2,4
	// 即取出第3条至第6条，4条记录
	@Override
	public <T> Page<T> loadByRoleId(final Serializable roleId, final EntityMapping<T> em, final Pageable pageable) {
		// 查出目标总记录数.
		final int totalSize = this.queryForInt(em, ((SqlGener) em).getSeleteByRoleIdAndCount(expert), roleId);
		// 计算查询数据所在的区间.
		final int m = (pageable.getPage() - 1) * pageable.getSize();
		final int n = pageable.getSize();

		List<T> result = this.queryForList(em, ((SqlGener) em).getSeleteByRoleIdAndPage(expert, pageable.getSort()), roleId, m, n);

		Page<T> page = new Page<>();
		page.setResult(result);
		page.setTotalSize(totalSize);
		page.setTotalPage(totalSize / n);
		if (totalSize % n != 0) {
			page.setTotalPage(page.getTotalPage() + 1);
		}
		page.setPage(pageable.getPage());
		page.setSize(pageable.getSize());
		return page;
	}

	@Override
	public <T> Page<T> loadAll(EntityMapping<T> em, Pageable pageable) {
		// 查出目标总记录数.
		final int totalSize = this.queryForInt(em, ((SqlGener) em).getSeleteByCount(expert));
		// 计算查询数据所在的区间.
		final int m = (pageable.getPage() - 1) * pageable.getSize();
		final int n = pageable.getSize();

		List<T> result = this.queryForList(em, ((SqlGener) em).getSeleteByPage(expert, pageable.getSort()), m, n);

		Page<T> page = new Page<>();
		page.setResult(result);
		page.setTotalSize(totalSize);
		page.setTotalPage(totalSize / n);
		if (totalSize % n != 0) {
			page.setTotalPage(page.getTotalPage() + 1);
		}
		page.setPage(pageable.getPage());
		page.setSize(pageable.getSize());
		return page;
	}

	@Override
	public <E> int execute(String sql, final Object... args) {
		class ExecutePreparedStatementCallback implements PreparedStatementCallback<Integer> {
			@Override
			public Integer doInPreparedStatement(PreparedStatement pstmt) throws SQLException, IllegalArgumentException, IllegalAccessException {
				int argsLength = args == null ? 0 : args.length;
				for (int i = 0; i < argsLength; i++) {
					pstmt.setObject(i + 1, args[i]);
				}
				return pstmt.executeUpdate();
			}
		}
		return execute(new ExecutePreparedStatementCallback(), sql);
	}
}