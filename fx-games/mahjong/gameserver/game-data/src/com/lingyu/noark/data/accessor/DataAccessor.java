package com.lingyu.noark.data.accessor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.OperateType;
import com.lingyu.noark.data.accessor.mysql.RowMapper;

/**
 * 数据访问策略接口API.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public interface DataAccessor {

	/**
	 * 插入一条数据.
	 * 
	 * @param em 对象实体描述类.
	 * @param entity 对象数据.
	 * @return 返回插入所受影响行数.
	 */
	public <T> int insert(EntityMapping<T> em, T entity);

	/**
	 * 删除一条数据.
	 * 
	 * @param em 对象实体描述类.
	 * @param entity 对象数据.
	 * @return 返回删除所受影响行数.
	 */
	public <T> int delete(EntityMapping<T> em, T entity);

	/**
	 * 修改一条数据.
	 * 
	 * @param em 对象实体描述类.
	 * @param entity 对象数据.
	 * @return 返回修改所受影响行数.
	 */
	public <T> int update(EntityMapping<T> em, T entity);

	/**
	 * 写入角色的修改过的数据.
	 * <p>
	 * 如果是Mysql需要合并分类实现批量操作，Network要实现一次发送.
	 * 
	 * @param roleId 角色Id
	 * @param type 操作类型
	 * @param entitys 修改过的数据
	 */
	public <T> void writeByRoleId(Serializable roleId, OperateType type, List<T> entitys);

	/**
	 * 加载一个指定ID的数据.
	 * 
	 * @param em 对象实体描述类.
	 * @param id 对象Id.
	 * @return 返回对象数据.
	 */
	public <T, K extends Serializable> T load(EntityMapping<T> em, Serializable roleId, K id);

	/**
	 * 加载表里所有的数据.
	 * 
	 * @param em 对象实体描述类.
	 * @return 返回对象数据列表，就算没有数据，也会返回空列表.
	 */
	public <T> List<T> loadAll(EntityMapping<T> em);

	public <T> Page<T> loadAll(EntityMapping<T> em, Pageable pageable);

	/**
	 * 加载指定角色Id对应模块数据.
	 * 
	 * @param roleId 角色Id
	 * @param em 对象实体描述类.
	 * @return 返回这个角色Id的模块数据，就算没有数据，也会返回空列表.
	 */
	public <T> List<T> loadByRoleId(Serializable roleId, EntityMapping<T> em);

	public <T> Page<T> loadByRoleId(Serializable roleId, EntityMapping<T> em, Pageable pageable);

	public <T> List<T> loadByGroup(EntityMapping<T> em, Serializable groupId);

	/**
	 * 检查实体类对应的数据库中的表结构.
	 * <p>
	 * 如果是关系型数据库，没有表则创建表，属性不一样就修改成一样的.
	 * 
	 * @param em 对象实体描述类.
	 */
	public <T> void checkupEntityFieldsWithDatabase(EntityMapping<T> em);

	// SQL系列----------------------------------------------------------------------------
	/**
	 * SQL查询，能直接封装对象.
	 * <p>
	 * 这个区别于queryForList方法，只在于不管查出多少数据，只要第一条.
	 * 
	 * @param em 对象实体描述类.
	 * @param sql SQL语句
	 * @param args 参数，没有可以不写.
	 * @return 实体对象.
	 */
	public <T> T queryForObject(EntityMapping<T> em, String sql, Object... args);

	/**
	 * SQL查询，能直接封装对象列表.
	 * 
	 * @param em 对象实体描述类.
	 * @param sql SQL语句
	 * @param args 参数，没有可以不写.
	 * @return 实体对象列表，就算没有数据，也会返回空列表.
	 */
	public <T> List<T> queryForList(final EntityMapping<T> em, String sql, Object... args);

	/**
	 * SQL查询，基本用于统计方面.
	 * 
	 * @param em 对象实体描述类.
	 * @param sql SQL语句
	 * @param args 参数，没有可以不写.
	 * @return 返回一个数字，没有数据也会返回0.
	 */
	public <T> int queryForInt(EntityMapping<T> em, String sql, Object... args);

	/**
	 * SQL查询，基本用于统计方面.
	 * 
	 * @param em 对象实体描述类.
	 * @param sql SQL语句
	 * @param args 参数，没有可以不写.
	 * @return 返回一个数字，没有数据也会返回0.
	 */
	public <T> long queryForLong(EntityMapping<T> em, String sql, Object... args);

	/**
	 * SQL查询，基本用于统计方面.
	 * 
	 * @param em 对象实体描述类.
	 * @param sql SQL语句
	 * @param args 参数，没有可以不写.
	 * @return 返回一个Map，就算没有数据，也会返回空Map.
	 */
	public <T> Map<String, Object> queryForMap(EntityMapping<T> em, String sql, Object... args);

	/**
	 * SQL查询，能直接封装对象列表.
	 * <p>
	 * 支持回调.
	 * 
	 * @param em 对象实体描述类.
	 * @param sql SQL语句
	 * @param mapper 回调接口.
	 * @param args 参数，没有可以不写.
	 * @return 实体对象列表，就算没有数据，也会返回空列表.
	 */
	public <E> List<E> queryForList(final EntityMapping<E> em, String sql, RowMapper<E> mapper, Object... args);

	/**
	 * 执行一个SQL语句.
	 * 
	 * @param sql SQL语句
	 * @param args 参数，没有可以不写.
	 * @return 执行结果，一般为SQL的返回值.
	 */
	public <E> int execute(String sql, Object... args);
}