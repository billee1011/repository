package com.lingyu.noark.data;

import java.io.Serializable;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.data.accessor.DataAccessorManager;
import com.lingyu.noark.data.accessor.network.NetworkDataAccessor;
import com.lingyu.noark.data.accessor.network.NetworkDataSource;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.write.AsyncWriteService;
import com.lingyu.noark.data.write.impl.DefaultAsyncWriteServiceImpl;

/**
 * 数据存储管理器.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DataManager {
	private static final Logger logger = LogManager.getLogger(DataManager.class);
	private DataAccessorManager dataAccessorManager;
	protected final AsyncWriteService asyncWriteService;
	private final boolean cross;

	/**
	 * 构建一个数据存储中心管理器.
	 * 
	 * @param dataSource 数据源.
	 * @param openNetwork 是否打开网络存储策略(用于跨服功能).
	 * @param saveInterval 定时保存的间隔（单位：秒）
	 * @param offlineInterval 缓存数据过期时间（距离最后访问的时间，单位：秒）
	 */
	public DataManager(DataSource dataSource, long saveInterval, long offlineInterval,boolean debug) {
//		logger.info("初始化数据存储中心，{}网络存储策略.", openNetwork ? "开启了" : "未开启");
		logger.info("定时存档的时间间隔为 {}秒, 离线玩家在内存中的存活时间为 {}秒", saveInterval, offlineInterval);
		this.cross = false;
		DataContext.setSaveInterval(saveInterval);
		DataContext.setOfflineInterval(offlineInterval);
		DataContext.setInitCache(true);
		DataContext.setDebug(debug);
		this.asyncWriteService = new DefaultAsyncWriteServiceImpl();

		this.dataAccessorManager = new DataAccessorManager(dataSource, false);
		DataContext.setDataManager(this);
	}

	/**
	 * 构建一个数据存储中心管理器.
	 * <p>
	 * 没有数据源，就是跨服功能啦~~
	 * 
	 * @param saveInterval 定时保存的间隔（单位：秒）
	 * @param offlineInterval 缓存数据过期时间（距离最后访问的时间，单位：秒）
	 */
	public DataManager(boolean openNetwork,long saveInterval, long offlineInterval,boolean debug) {
		logger.info("初始化数据存储中心，{}网络存储策略.", openNetwork ? "开启了" : "未开启");
		logger.info("定时存档的时间间隔为 {}秒, 离线玩家在内存中的存活时间为 {}秒", saveInterval, offlineInterval);
		this.cross = true;
		DataContext.setSaveInterval(saveInterval);
		DataContext.setOfflineInterval(offlineInterval);
		DataContext.setDebug(debug);
		this.asyncWriteService = new DefaultAsyncWriteServiceImpl();

		this.dataAccessorManager = new DataAccessorManager(openNetwork);
		DataContext.setDataManager(this);
	}

	public DataAccessorManager getDataAccessorManager() {
		return dataAccessorManager;
	}

	public AsyncWriteService getAsyncWriteService() {
		return asyncWriteService;
	}

	/**
	 * 把一个角色的数据Flush回去.
	 * <p>
	 * 如果有其他数据正在操作，这个逻辑应该消息分发去控制（保存调用此方法时，不在有对此人的写操作）<br>
	 * 当一个角色下线或退出跨服时需要调用此方法
	 * 
	 * @param roleId 角色Id
	 */
	public void flushDataContainer(Serializable roleId) {
		asyncWriteService.syncFlushByRoleId(roleId);
	}

	/**
	 * 将所有角色数据都Flush回去.
	 */
	public void flushAll() {
		asyncWriteService.syncFlushAll();
	}

	/**
	 * 异步存储，由存储系统保存.
	 */
	public void asyncFlushDataContainer(Serializable roleId) {
		asyncWriteService.asyncFlushByRoleId(roleId);
	}

	/**
	 * 关闭数据管理器中心。
	 * <p>
	 * 保存全部玩家数据，然后关闭回写管理器.
	 */
	public void shutdown() {
		logger.info("数据管理中心开始关闭.");
		this.flushAll();
		asyncWriteService.shutdown();
	}

	/**
	 * 如果是跨服过来的角色，需要向数据中心注册一份他的网络数据源.
	 * <p>
	 * 注册就是登录需求，需要加载那些有{@link IsRoleId}的<br>
	 * 并且有抓取策略为{@link FeatchType#LOGIN}和 {@link FeatchType#START}
	 * 
	 * @param dataSource 网络存储接口.
	 */
	public void register(NetworkDataSource dataSource) {
		NetworkDataAccessor networkAccessor = dataAccessorManager.getNetworkDataAccess();
		Assert.notNull(networkAccessor);
		networkAccessor.register(dataSource);
	}

	public NetworkDataSource getNetworkDataSource(Serializable roleId) {
		NetworkDataAccessor networkAccessor = dataAccessorManager.getNetworkDataAccess();
		if (networkAccessor == null) {
			return null;
		}
		return networkAccessor.getNetworkDataSource(roleId);
	}

	public NetworkDataSource getNetworkDataSourceByUserId(Serializable userId) {
		NetworkDataAccessor networkAccessor = dataAccessorManager.getNetworkDataAccess();
		if (networkAccessor == null) {
			return null;
		}
		return networkAccessor.getNetworkDataSourceByUserId(userId);
	}

	/**
	 * 删除数据访问策略中指定角色Id的网络数据源.
	 * <p>
	 * 删除时也就是退出，需要清空这个人的缓存和Flush他的变动数据回原来的服务器.
	 * 
	 * @param roleId 角色Id
	 */
	public void remove(Serializable roleId) {
		NetworkDataAccessor networkAccessor = dataAccessorManager.getNetworkDataAccess();
		Assert.notNull(networkAccessor);
		// 1、Flush个人数据回原来的服务器
		//TODO 将会被删除
		this.flushDataContainer(roleId);
		// 2、清空此人的缓存数据
		EntityHolder.removeCache(roleId);
		// 3、删除网络数据源
		networkAccessor.remove(roleId);
	}

	public boolean isCross() {
		return cross;
	}

	/**
	 * 构造一个内部小工具，简化上面的代码调用.
	 */
	private static class Assert {
		private static void notNull(NetworkDataAccessor networkAccessor) {
			if (networkAccessor == null) {
				throw new NullPointerException("当前服务器没有开启跨服功能");
			}
		}
	}
}
