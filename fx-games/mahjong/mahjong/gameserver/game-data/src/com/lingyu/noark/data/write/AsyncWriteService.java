package com.lingyu.noark.data.write;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.EntityMapping;

/**
 * 异步回写服务接口.
 * <p>
 * 这个搞成接口是为了先规划API和后期实现其他不同策略的回写服务.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public interface AsyncWriteService {

	public <T> void insert(final EntityMapping<T> em, final T entity);

	public <T> void delete(final EntityMapping<T> em, final T entity);

	public <T> void deleteAll(final EntityMapping<T> em, List<T> result);

	public <T> void update(final EntityMapping<T> em, final T entity);

	/**
	 * 同步式的清理指定角色Id的异步回写数据.
	 * <p>
	 * 下线操作或退出跨服
	 * 
	 * @param roleId 角色Id
	 */
	public void syncFlushByRoleId(Serializable roleId);

	/**
	 * 同步式的清理全部回写数据.
	 * <p>
	 * 一般用于停机维护.
	 */
	public void syncFlushAll();

	public void shutdown();

	/**
	 * 异步存储，由存储系统保存.
	 */
	public void asyncFlushByRoleId(Serializable roleId);
}
