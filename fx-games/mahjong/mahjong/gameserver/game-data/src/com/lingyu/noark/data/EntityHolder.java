package com.lingyu.noark.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.lingyu.noark.data.repository.AbstractCacheRepository;
import com.lingyu.noark.data.repository.ICacheRepository;

/**
 * 封装实体类对应的Repository引用.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class EntityHolder {
	private static final Map<String, ICacheRepository<?>> repositoryMap = new HashMap<>(56);

	public static void register(AbstractCacheRepository<?, ?> repository) {
		repositoryMap.put(repository.getEntityMapping().getEntityClass().getName(), repository);
	}

	@SuppressWarnings("unchecked")
	public static <T> ICacheRepository<T> getCacheRepository(String klassName) {
		return (ICacheRepository<T>) repositoryMap.get(klassName);
	}

	public static void removeCache(Serializable roleId) {
		for (Entry<String, ICacheRepository<?>> repository : repositoryMap.entrySet()) {
			repository.getValue()._removeCache(roleId);
		}
	}

	private static final Map<String, AbsRedisRepository<?>> redisRepositoryMap = new HashMap<>(56);

	static void register(String className, AbsRedisRepository<?> repository) {
		redisRepositoryMap.put(className, repository);
	}

	@SuppressWarnings("unchecked")
	public static <T> AbsRedisRepository<T> getRedisRepository(String klassName) {
		return (AbsRedisRepository<T>) redisRepositoryMap.get(klassName);
	}
}