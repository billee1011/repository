package com.lingyu.noark.data.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;

import com.lingyu.noark.data.AbsRedisRepository;
import com.lingyu.noark.data.accessor.redis.cmd.Hmset;
import com.lingyu.noark.data.accessor.redis.cmd.Hset;
import com.lingyu.noark.data.cache.IDataCache;
import com.lingyu.noark.data.cache.impl.DataCacheFactory;
import com.lingyu.noark.data.exception.DataException;

/**
 * RedisHashRepository类为所有Redis操作Hash数据结构接口.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public abstract class RedisHashRepository<T> extends AbsRedisRepository<T> {
	private static final Logger logger = LogManager.getLogger(RedisHashRepository.class);
	// CacheRepository层的数据缓存
	protected final IDataCache<T, String> cache = DataCacheFactory.create(this);

	/**
	 * 这个不走缓存，直接Redis取数据
	 */
	public T load(String key) {
		return this.getRedisDataAccessor().load(this.getRedis(), key, entityMapping);
	}

	/**
	 * 这个有一层缓存，缓存拉不到就去Redis里取
	 */
	public T cacheLoad(String key) {
		return load(key);
	}

	public void cacheRemove(String key) {
		cache.removeCache(key);
	}
	/**
	 * 根据前缀和要拼接的值从redis中取对象列表
	 * @param keyPrefix redis中存的key的前缀
	 * @param collection 前缀要拼接的值的集合
	 * @return
	 */
	public <E extends Serializable> List<T> loadShardedList(String keyPrefix, Collection<E> collection) {
		List<T> ret = new ArrayList<T>();
		List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String, String>>>();
		ShardedJedisPool pool = this.getRedis().getShardedPool();
		ShardedJedis j = pool.getResource();
		try {
			ShardedJedisPipeline p = j.pipelined();
			for (E id : collection) {
				Response<Map<String, String>> response = p.hgetAll(keyPrefix + id);
				if (response != null) {
					responses.add(response);
				}
			}
			p.sync();
			pool.returnResource(j);
		} catch (Exception e) {
			pool.returnBrokenResource(j);
		}
		for (Response<Map<String, String>> response : responses) {
			Map<String, String> map = response.get();
			if (map != null && !map.isEmpty()) {
				try {
					T entity = entityMapping.newEntity(map);
					ret.add(entity);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return ret;
	}
	
	/**
	 * 根据前缀和要拼接的值从redis中取对象列表
	 * @param keyPrefix redis中存的key的前缀
	 * @param collection 前缀要拼接的值的集合
	 * @return
	 */
	public <E extends Serializable> List<T> loadList(String keyPrefix, Collection<E> collection) {
		List<T> ret = new ArrayList<T>();
		List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String, String>>>();
		JedisPool pool = this.getRedis().getPool();
		Jedis j = pool.getResource();
		try {
			Pipeline p = j.pipelined();
			for (E id : collection) {
				Response<Map<String, String>> response = p.hgetAll(keyPrefix + id);
				if (response != null) {
					responses.add(response);
				}
			}
			p.sync();
			pool.returnResource(j);
		} catch (Exception e) {
			pool.returnBrokenResource(j);
		}
		for (Response<Map<String, String>> response : responses) {
			Map<String, String> map = response.get();
			if (map != null && !map.isEmpty()) {
				try {
					T entity = entityMapping.newEntity(map);
					ret.add(entity);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return ret;
	}

	/**
	 * 批量更新数据.
	 * <p>
	 * 更新完会删除缓存里的数据
	 */
	public void cacheUpdate(Serializable roleId, String key, Map<String, String> data) {
		this.hmset(new Hmset(roleId, key, data));
	}

	/**
	 * 更新一条数据.
	 * <p>
	 * 更新完会删除缓存里的数据
	 */
	public void cacheUpdate(long roleId, String key, String field, String value) {
		this.hset(new Hset(roleId, key, field, value));
	}

	public void cacheUpdate(T entity) {
		Serializable roleId = entityMapping.getRoleIdValue(entity);
		String key = entityMapping.makeKey(entity);
		try {
			this.cacheUpdate(roleId, key, entityMapping.toValue(entity));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataException("保存实现对象到Redis中时，获取数据异常.", e);
		}
	}

	/**
	 * 只保存指定属性到Redis中.
	 */
	public void cacheUpdate(T entity, String... fields) {
		Serializable roleId = entityMapping.getRoleIdValue(entity);
		String key = entityMapping.makeKey(entity);
		try {
			this.cacheUpdate(roleId, key, entityMapping.toValue(entity, fields));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataException("保存实现对象到Redis中时，获取数据异常.", e);
		}
	}

	// 操作区，可以跨服的功能...
	@Override
	public <O extends Hset> void hset(O hash) {
		this.getRedisDataAccessor(hash.getRoleId()).hset(this.getRedis(), hash, entityMapping);
	}

	@Override
	public <O extends Hmset> void hmset(O hash) {
		this.getRedisDataAccessor(hash.getRoleId()).hmset(this.getRedis(), hash, entityMapping);
	}
}