package com.lingyu.noark.data;

import com.lingyu.noark.data.accessor.DataAccessorManager;
import com.lingyu.noark.data.write.AsyncWriteService;

public final class DataContext {
	private final static DataContext context = new DataContext();
	private DataManager dataManager;
	private long offlineInterval = 3600;
	private long saveInterval = 300;
	private boolean isInitCache = false;
	private boolean debug;

	private DataContext() {
	}

	public static void setDataManager(DataManager dataManager) {
		context.dataManager = dataManager;
	}

	public static DataAccessorManager getDataAccessorManager() {
		Assert.notNull(context.dataManager);
		return context.dataManager.getDataAccessorManager();
	}

	public static long getOfflineInterval() {
		return context.offlineInterval;
	}

	public static void setOfflineInterval(long offlineInterval) {
		context.offlineInterval = offlineInterval;
	}

	public static long getSaveInterval() {
		return context.saveInterval;
	}

	public static void setSaveInterval(long saveInterval) {
		context.saveInterval = saveInterval;
	}

	public static AsyncWriteService getAsyncWriteService() {
		Assert.notNull(context.dataManager);
		return context.dataManager.getAsyncWriteService();
	}

	public static boolean isInitCache() {
		return context.isInitCache;
	}

	public static void setInitCache(boolean isInitCache) {
		context.isInitCache = isInitCache;
	}

	public static boolean isDebug() {
		return context.debug;
	}

	public static void setDebug(boolean debug) {
		context.debug = debug;
	}

	public static boolean isCross() {
		return context.dataManager.isCross();
	}

	/**
	 * 构造一个内部小工具，简化上面的代码调用.
	 */
	private static class Assert {
		private static void notNull(DataManager dataManager) {
			if (dataManager == null) {
				throw new NullPointerException("使用Data存储功能，就不先初始化DataManager吗？");
			}
		}
	}
}