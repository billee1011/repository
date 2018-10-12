package com.lingyu.noark.data.accessor.network;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.OperateType;
import com.lingyu.noark.data.RedisEntityMapping;

/**
 * 网络数据源接口.
 * <p>
 * <b>这个类的实现类，请一定要注意网络异常问题，当发生网络异常时，万万不能把异常给我吃了</b>
 * 
 * @author 小流氓<176543888@qq.com>
 */
public interface NetworkDataSource {

	/**
	 * 获取这个人的唯一标识.
	 * <p>
	 * 所有服的Id万万不能有一样的值.
	 * 
	 * @return 角色Id
	 */
	public Serializable getRoleId();

	/**
	 * 获取这个人的唯一标识.
	 * <p>
	 * 所有服的Id万万不能有一样的值.
	 * 
	 * @return 用户Id
	 */
	public Serializable getUserId();

	/**
	 * 向网络那边加载数据.
	 * 
	 * @param em 实体类描述对象.
	 * @return 返回这个角色的这个实体对象列表.就算没有数据也要返回空列表.
	 */
	public <T> T loadData(EntityMapping<T> em);

	/**
	 * 向网络那边加载数据.
	 * 
	 * @param em 实体类描述对象.
	 * @return 返回这个角色的这个实体对象列表.就算没有数据也要返回空列表.
	 */
	public <T> List<T> loadDataList(EntityMapping<T> em);

	/**
	 * 向网络那边回写数据.
	 * <p>
	 * 包含新增，修改，删除
	 * 
	 * @param em 实体类描述对象.
	 * @param type 操作类型.
	 * @param entity 回家数据对象.
	 * @return 返回操作结果所影响的记录数
	 */
	public <T> int writeData(EntityMapping<T> em, OperateType type, T entity);

	/**
	 * 向网络那边加载数据.
	 * <p>
	 * 要指定的数据.
	 * 
	 * @param em 实体类描述对象.
	 * @return 返回这个角色的这个实体对象列表.就算没有数据也要返回空列表.
	 */
	public <T> T loadData(EntityMapping<T> em, Serializable entityId);

	public <T> void writeData(OperateType zadd, T set, RedisEntityMapping<?> entityMapping);
}