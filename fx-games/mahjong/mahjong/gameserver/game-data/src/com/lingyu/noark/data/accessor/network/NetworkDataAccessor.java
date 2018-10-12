package com.lingyu.noark.data.accessor.network;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.OperateType;
import com.lingyu.noark.data.RedisEntityMapping;
import com.lingyu.noark.data.accessor.AbstractDataAccessor;
import com.lingyu.noark.data.accessor.Page;
import com.lingyu.noark.data.accessor.Pageable;
import com.lingyu.noark.data.accessor.redis.IRedisDataAccessor;
import com.lingyu.noark.data.accessor.redis.IRediser;
import com.lingyu.noark.data.accessor.redis.cmd.Get;
import com.lingyu.noark.data.accessor.redis.cmd.Hmset;
import com.lingyu.noark.data.accessor.redis.cmd.Hset;
import com.lingyu.noark.data.accessor.redis.cmd.Set;
import com.lingyu.noark.data.accessor.redis.cmd.Zadd;
import com.lingyu.noark.data.accessor.redis.cmd.Zincrby;
import com.lingyu.noark.data.accessor.redis.cmd.Zrem;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.exception.UnrealizedException;

/**
 * 网络数据存储层.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class NetworkDataAccessor extends AbstractDataAccessor implements IRedisDataAccessor {

	// 网络管理器.
	private NetworkManager networkManager = new NetworkManager();

	public NetworkDataAccessor() {

	}

	public void register(NetworkDataSource dataSource) {
		networkManager.register(dataSource.getRoleId(), dataSource);
	}

	/**
	 * 判定一个角色Id有没有在网络存储层注册过.
	 * 
	 * @param roleId 角色Id
	 * @return 如果这个角色在这里注册过就返回true,否则返回false
	 */
	public boolean isRegister(Serializable roleId) {
		return networkManager.contains(roleId);
	}

	public void remove(Serializable roleId) {
		networkManager.remove(roleId);
	}

	public NetworkDataSource getNetworkDataSource(Serializable roleId) {
		return networkManager.getNetworkDataSource(roleId);
	}

	public NetworkDataSource getNetworkDataSourceByUserId(Serializable userId) {
		return networkManager.getNetworkDataSourceByUserId(userId);
	}

	/**
	 * 唯一缓存的走这里
	 */
	@Override
	public <T, K extends Serializable> T load(EntityMapping<T> em, Serializable roleId, K id) {
		NetworkDataSource source = networkManager.getNetworkDataSource(roleId);
		return source.loadData(em);
	}

	private <T> NetworkDataSource getNetworkDataSource(EntityMapping<T> em, T entity) {
		NetworkDataSource source = networkManager.getNetworkDataSource(em.getRoleIdValue(entity));
		if (source == null) {
			throw new DataException("没有找到这跨服孩子的网络存储接口，咋回写嘛~~~");
		}
		return source;
	}

	@Override
	public <T> int insert(EntityMapping<T> em, T entity) {
		NetworkDataSource source = this.getNetworkDataSource(em, entity);
		return source.writeData(em, OperateType.INSTER, entity);
	}

	@Override
	public <T> int delete(EntityMapping<T> em, T entity) {
		NetworkDataSource source = this.getNetworkDataSource(em, entity);
		return source.writeData(em, OperateType.DELETE, entity);
	}

	@Override
	public <T> int update(EntityMapping<T> em, T entity) {
		NetworkDataSource source = this.getNetworkDataSource(em, entity);
		return source.writeData(em, OperateType.UPDATE, entity);
	}

	@Override
	public <T> List<T> loadAll(EntityMapping<T> em) {
		return null;
	}

	@Override
	public <T> List<T> loadByGroup(EntityMapping<T> em, Serializable groupId) {
		return null;
	}

	@Override
	public <T> List<T> loadByRoleId(Serializable roleId, EntityMapping<T> em) {
		NetworkDataSource source = networkManager.getNetworkDataSource(roleId);
		return source.loadDataList(em);
	}

	@Override
	public <T> void writeByRoleId(Serializable roleId, OperateType type, List<T> entitys) {

	}

	@Override
	public <T> void zadd(IRediser redis, Zadd set, RedisEntityMapping<T> entityMapping) {
		NetworkDataSource source = networkManager.getNetworkDataSource(set.getRoleId());
		source.writeData(OperateType.ZADD, set, entityMapping);
	}

	@Override
	public <T> void hset(IRediser redis, Hset hash, RedisEntityMapping<T> entityMapping) {
		NetworkDataSource source = networkManager.getNetworkDataSource(hash.getRoleId());
		source.writeData(OperateType.HSET, hash, entityMapping);
	}

	@Override
	public <T> void zrem(IRediser redis, Zrem set, RedisEntityMapping<T> entityMapping) {
		NetworkDataSource source = networkManager.getNetworkDataSource(set.getRoleId());
		source.writeData(OperateType.ZREM, set, entityMapping);
	}

	@Override
	public <T> T load(IRediser redis, String key, RedisEntityMapping<T> entityMapping) {
		throw new UnrealizedException("Redis缓存操作，没有此API...");
	}

	@Override
	public <T> void hmset(IRediser redis, Hmset hash, RedisEntityMapping<T> entityMapping) {
		NetworkDataSource source = networkManager.getNetworkDataSource(hash.getRoleId());
		source.writeData(OperateType.HMSET, hash, entityMapping);
	}

	@Override
	public <T> void zincrby(IRediser redis, Zincrby set, RedisEntityMapping<T> entityMapping) {
		NetworkDataSource source = networkManager.getNetworkDataSource(set.getRoleId());
		source.writeData(OperateType.ZINCRBY, set, entityMapping);
	}

	@Override
	public <T> T get(IRediser redis, Get string, RedisEntityMapping<T> entityMapping) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> void set(IRediser redis, Set string, RedisEntityMapping<T> entityMapping) {
		// TODO Auto-generated method stub
	}

	@Override
	public <T> Page<T> loadByRoleId(Serializable roleId, EntityMapping<T> em, Pageable pageable) {
		throw new DataException("网络层不应该调到直接调用分页查DB的API.");
	}

	@Override
	public <T> Page<T> loadAll(EntityMapping<T> em, Pageable pageable) {
		throw new DataException("网络层不应该调到直接调用分页查DB的API.");
	}
}