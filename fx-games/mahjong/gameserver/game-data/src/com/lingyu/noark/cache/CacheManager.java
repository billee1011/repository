package com.lingyu.noark.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.lingyu.noark.cache.exception.GuavaCacheException;

/**
 * 临时缓存管理器.
 * <p>
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class CacheManager {
	private static final Logger logger = LogManager.getLogger(CacheManager.class);
	// 缓存Key:模块---->Value:Map
	private final LoadingCache<String, Cache<Long, Object>> caches = CacheBuilder.newBuilder().build(new CacheLoader<String, Cache<Long, Object>>() {
		@Override
		public Cache<Long, Object> load(String module) throws Exception {
			logger.debug("永久类型的临时缓存命中失败，创建缓存容器. CacheModule={}", module);
			return CacheBuilder.newBuilder().build();
		}
	});

	/**
	 * 构造一个临时缓存管理器.
	 * <p>
	 * 可以重写此类，调用{@link #register(String, long, TimeUnit)}实现定制缓存时间的功能.
	 */
	public CacheManager() {
	}

	/**
	 * 注册指定模块的缓存过期时间.
	 * <p>
	 * <b>必需在使用此模块之前注册，不然没有用</b>
	 * 
	 * @param module
	 *            模块名称
	 * @param duration
	 *            缓存过期时间
	 * @param unit
	 *            缓存过期时间的单位
	 */
	public final void register(final String module, final long duration, final TimeUnit unit) {
		try {
			this.caches.get(module, new Callable<Cache<Long, Object>>() {
				@Override
				public Cache<Long, Object> call() throws Exception {
					logger.debug("注册一个有过期时间的临时缓存，创建缓存容器. CacheModule={}, duration={}, unit={}", module, duration, unit);
					return CacheBuilder.newBuilder().expireAfterAccess(duration, unit).removalListener(new RemovalListener<Long, Object>() {
						@Override
						public void onRemoval(RemovalNotification<Long, Object> notification) {
							logger.debug("临时缓存容器移除对象. CacheModule={}, key={}", module, notification.getKey());
						}
					}).build();
				}
			});
		} catch (ExecutionException e) {
			throw new GuavaCacheException("使用自定义缓存时，注册缓存移除策略时发生异常。", e);
		}
	}

	/**
	 * 缓存一个对象到缓存管理器中.
	 * <p>
	 * 如果此模块缓存中映射以前包含一个该键的映射关系，则用指定值替换旧值。
	 * 
	 * @param module
	 *            模块名称
	 * @param key
	 *            与缓存对象关联的键
	 * @param value
	 *            与缓存对象关联的值
	 */
	public final <V> void putData(String module, long key, V value) {
		logger.debug("临时缓存容器添加对象. CacheModule={}, key={}", module, key);
		caches.getUnchecked(module).put(key, value);
	}

	/**
	 * 获取指定键所映射的缓存对象.
	 * <p>
	 * 如果此映射不包含该键的映射关系，则返回 null。 更确切地讲，如果此映射包含满足 (key==null ? k==null :
	 * key.equals(k)) 的键 k 到值 v 的映射关系，则此方法返回 v；否则返回 null。
	 * 
	 * @param module
	 *            模块名称
	 * @param key
	 *            与缓存对象关联的键
	 * @return 返回此模块中以前与 key关联的值，如果没有针对 key的映射关系，则返回 null。
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getData(String module, long key) {
		return (T) caches.getUnchecked(module).getIfPresent(key);
	}

	/**
	 * 移除一个指定键所映射的缓存对象.
	 * 
	 * @param module
	 *            模块名称
	 * @param key
	 *            与缓存对象关联的键
	 */
	public final void removeData(String module, long key) {
		caches.getUnchecked(module).invalidate(key);
	}

	/**
	 * 获取指定模块中的全部数据.
	 * 
	 * @param module
	 *            模块名称
	 * @return 指定模块中的全部数据.
	 */
	@SuppressWarnings("unchecked")
	public final <T> ConcurrentMap<Long, T> getAllData(String module) {
		return (ConcurrentMap<Long, T>) caches.getUnchecked(module).asMap();
	}

	/**
	 * 移除一个系统模块的所有缓存.
	 * 
	 * @param module
	 *            模块名称
	 */
	public final void removeAll(String module) {
		caches.invalidate(module);
	}
}