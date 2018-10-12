package com.lingyu.common.cache;

import com.lingyu.noark.cache.CacheManager;

/**
 * 自定义缓存时间的临时缓存管理器.
 * <p>
 * 有需要定制缓存时间的需要在此注册
 * 
 * @author 小流氓<zhoumingkai@lingyuwangluo.com>
 */
public final class CustomCacheManager extends CacheManager {

	public CustomCacheManager() {
//		this.register(CacheConstant.SIGNCACHE, 10, TimeUnit.MINUTES);
	}
}