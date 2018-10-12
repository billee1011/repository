package com.lingyu.noark.cache;

import java.util.concurrent.TimeUnit;

/**
 * 一个简单的自定义缓存时间的临时缓存管理器.
 * <p>
 * 算是一个使用样例吧~~~
 * 
 * @author 小流氓<176543888@qq.com>
 */
public final class SimpleCustomCacheManager extends CacheManager {
	// 副本结束后的奖励模块的名称
	public final static String MODULE_INSTANCE_REWARD = "module_instance_reward";

	public SimpleCustomCacheManager() {
		// 注册一个超时时间，就不要担心副本结束后先存着的奖励清不掉了
		this.register(MODULE_INSTANCE_REWARD, 1, TimeUnit.HOURS);
	}
}
