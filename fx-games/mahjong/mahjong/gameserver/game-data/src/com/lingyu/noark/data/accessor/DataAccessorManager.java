package com.lingyu.noark.data.accessor;

import java.io.Serializable;

import javax.sql.DataSource;

import com.lingyu.noark.data.DefaultRoleId;
import com.lingyu.noark.data.accessor.mysql.Jdbcs;
import com.lingyu.noark.data.accessor.mysql.MysqlDataAccessor;
import com.lingyu.noark.data.accessor.network.NetworkDataAccessor;
import com.lingyu.noark.data.accessor.redis.IRedisDataAccessor;
import com.lingyu.noark.data.accessor.redis.RedisDataAccessorImpl;
import com.lingyu.noark.data.exception.UnrealizedException;

/**
 * 数据存储策略管理器.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DataAccessorManager {
	private final DataAccessor dataAccess;
	private NetworkDataAccessor networkDataAccess;
	private IRedisDataAccessor redisDataAccessor = new RedisDataAccessorImpl();

	public NetworkDataAccessor getNetworkDataAccess() {
		return networkDataAccess;
	}

	/**
	 * 无参构造数据存储管理器，就是要走网络路线.
	 */
	public DataAccessorManager(boolean openNetwork) {
		if(openNetwork){
			this.networkDataAccess = new NetworkDataAccessor();
			this.dataAccess = networkDataAccess;
		}else{
			dataAccess=null;
		}
		
	}

	/**
	 * 以数据源的形式构造数据存储管理器，就是要走SQL路线.
	 * 
	 * @param dataSource 数据源
	 * @param openNetwork 是否开启网络存储策略
	 */
	public DataAccessorManager(DataSource dataSource, boolean openNetwork) {
		if (dataSource == null) {
			throw new NullPointerException("初始化noark-data不传入DataSource吗？");
		}

		// 如果开启网络存储策略，直接New一个好啦~~~
		if (openNetwork) {
			networkDataAccess = new NetworkDataAccessor();
		}

		// 数据源需要判定一下类型，再建构
		AccessType accessType = Jdbcs.judgeAccessType(dataSource);
		switch (accessType) {
		case Mysql:
			dataAccess = new MysqlDataAccessor(dataSource);
			break;

		default:
			throw new UnrealizedException("没有实现的数据存储接口.");
		}
	}

	/**
	 * 获取数据存储策略接口.
	 * <p>
	 * 取系统数据或没开网络存储策略，直接返回数据存储策略<br>
	 * 如果此角色在网络存储策略中注册了的话就使用网络存储策略<br>
	 * 其他情况全走数据存储策略
	 * 
	 * @param roleId 角色Id.
	 * @return 返回数据存储策略接口的具体实现.
	 */
	public DataAccessor getDataAccess(Serializable roleId) {
		// 取系统数据或没开网络存储策略，直接返回数据存储策略
		if (DefaultRoleId.instance.equals(roleId) || networkDataAccess == null) {
			return dataAccess;
		}
		// 如果此角色在网络存储策略中注册了的话就使用网络存储策略
		if (networkDataAccess.isRegister(roleId)) {
			return networkDataAccess;
		}
		// 其他情况全走数据存储策略
		return dataAccess;
	}

	/**
	 * 获取数据存储策略接口.
	 * <p>
	 * 取系统数据或没开网络存储策略，直接返回数据存储策略<br>
	 * 如果此角色在网络存储策略中注册了的话就使用网络存储策略<br>
	 * 其他情况全走数据存储策略
	 * 
	 * @param roleId 角色Id.
	 * @return 返回数据存储策略接口的具体实现.
	 */
	public IRedisDataAccessor getRedisDataAccess(Serializable roleId) {
		// 取系统数据或没开网络存储策略，直接返回数据存储策略
		if (DefaultRoleId.instance.equals(roleId) || networkDataAccess == null) {
			return redisDataAccessor;
		}
		// 如果此角色在网络存储策略中注册了的话就使用网络存储策略
		if (networkDataAccess.isRegister(roleId)) {
			return networkDataAccess;
		}
		// 其他情况全走数据存储策略
		return redisDataAccessor;
	}

	public IRedisDataAccessor getRedisDataAccess() {
		return redisDataAccessor;
	}
}