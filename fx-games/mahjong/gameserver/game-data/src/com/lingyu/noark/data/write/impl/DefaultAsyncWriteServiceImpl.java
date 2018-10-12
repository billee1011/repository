package com.lingyu.noark.data.write.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.lingyu.noark.data.DataContext;
import com.lingyu.noark.data.DefaultRoleId;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.OperateType;
import com.lingyu.noark.data.accessor.DataAccessor;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.write.AsyncWriteService;

/**
 * 回写策略的默认实现.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DefaultAsyncWriteServiceImpl implements AsyncWriteService {
	private static final Logger logger = LogManager.getLogger(DefaultAsyncWriteServiceImpl.class);
	// 这个定时任务，有空就处理一下数据保存和缓存清理功能
	private final static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4, new GameThreadFactory("async-write-data"));

	private final RemovalListener<Serializable, AsyncWriteContainer> listener = new RemovalListener<Serializable, AsyncWriteContainer>() {
		@Override
		public void onRemoval(RemovalNotification<Serializable, AsyncWriteContainer> notification) {
			logger.debug("销毁{}秒都没有读写操作的异步回写容器， roleId={}", DataContext.getOfflineInterval(), notification.getKey());
			notification.getValue().syncFlush();
			notification.getValue().close();
		}
	};
	private final CacheLoader<Serializable, AsyncWriteContainer> loader = new CacheLoader<Serializable, AsyncWriteContainer>() {
		@Override
		public AsyncWriteContainer load(Serializable roleId) {
			logger.debug("创建异步回写容器， roleId={}", roleId);
			return new AsyncWriteContainer(roleId);
		}
	};
	// 异步回写容器缓存
	private final LoadingCache<Serializable, AsyncWriteContainer> containers = CacheBuilder.newBuilder()
			.expireAfterAccess(DataContext.getOfflineInterval(), TimeUnit.SECONDS).removalListener(listener).build(loader);

	// 关联实体的映射关系 Key：EntityId <--> Value:RoleId
	public Map<Serializable, Serializable> EntityId2RoleId = new HashMap<>();

	public DefaultAsyncWriteServiceImpl() {
		scheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				containers.cleanUp();
			}
		}, DataContext.getOfflineInterval(), DataContext.getOfflineInterval(), TimeUnit.SECONDS);
	}

	/**
	 * 智能分析这个实体类的角色Id是多少.
	 * <p>
	 * 需要考虑到@Join的注解，用来处理User实体的
	 * 
	 * @param em 实体类的描述对象.
	 * @param entity 实体对象.
	 * @return 如果这个实体对象中有@IsRoleId则返回此属性的值，否则返回默认的（系统）角色ID
	 */
	private <T> Serializable analysisRoleIdByEntity(EntityMapping<T> em, T entity) {
		// 拥有@IsRoleId的必属性角色的数据
		if (em.getRoleId() != null) {
			return em.getRoleIdValue(entity);
		}
		// 当前实体是不是有过关联
		Serializable roleId = EntityId2RoleId.get(em.getPrimaryIdValue(entity));
		if (roleId == null) {
			// 没有关联的返回默认的系统Id
			return DefaultRoleId.instance;
		}
		return roleId;
	}

	/**
	 * 数据操作.
	 * 
	 * @param em 实体类的描述对象
	 * @param entity 实体对象
	 * @param type 操作类型
	 */
	private <T> void operationing(EntityMapping<T> em, T entity, OperateType type) {
		if(DataContext.isDebug()){
			if(StringUtils.startsWith(Thread.currentThread().getName(), "game-stage")){
				throw new UnsupportedOperationException("不能在stage 进行数据库操作");
			}
		}
		
		Serializable roleId = this.analysisRoleIdByEntity(em, entity);
		AsyncWriteContainer container = containers.getUnchecked(roleId);
		switch (type) {
		case INSTER:
			container.insert(em, entity);
			break;
		case DELETE:
			container.delete(em, entity);
			break;
		case UPDATE:
			container.update(em, entity);
			break;
		default:
			logger.warn("这是要干嘛？ type={},entity={}", type, entity);
			break;
		}
	}

	@Override
	public <T> void insert(EntityMapping<T> em, T entity) {
		this.operationing(em, entity, OperateType.INSTER);
	}

	@Override
	public <T> void delete(EntityMapping<T> em, T entity) {
		this.operationing(em, entity, OperateType.DELETE);
	}

	@Override
	public <T> void deleteAll(EntityMapping<T> em, List<T> result) {
		for (T entity : result) {
			this.operationing(em, entity, OperateType.DELETE);
		}
	}

	@Override
	public <T> void update(EntityMapping<T> em, T entity) {
		this.operationing(em, entity, OperateType.UPDATE);
	}

	@Override
	public void syncFlushByRoleId(Serializable roleId) {
		containers.getUnchecked(roleId).syncFlush();
	}

	@Override
	public void syncFlushAll() {
		for (AsyncWriteContainer container : containers.asMap().values()) {
			container.syncFlush();
		}
	}

	@Override
	public void shutdown() {
		logger.info("开始通知数据保存任务线程池关闭.");
		scheduledExecutor.shutdown();
		try {
			// 尝试等待10分钟回写操作，10分钟都没写完就全停掉吧，不写了
			if (!scheduledExecutor.awaitTermination(10, TimeUnit.MINUTES)) {
				scheduledExecutor.shutdownNow();
			}
			logger.info("数据保存任务线程池已全部回写完，关闭成功.");
		} catch (InterruptedException ie) {
			logger.info("数据保存任务线程池停机时发生异常.", ie);
			scheduledExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private static class GameThreadFactory implements ThreadFactory {
		private final String name;
		private final AtomicInteger threadCounter = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable runnable) {
			StringBuilder threadName = new StringBuilder(56);
			threadName.append(name).append("-").append(threadCounter.getAndIncrement());
			Thread thread = new Thread(group, runnable, threadName.toString());
			if (thread.isDaemon())
				thread.setDaemon(false);
			if (thread.getPriority() != Thread.NORM_PRIORITY)
				thread.setPriority(Thread.NORM_PRIORITY);
			return thread;
		}

		final ThreadGroup group;

		public GameThreadFactory(String name) {
			SecurityManager securitymanager = System.getSecurityManager();
			this.group = securitymanager == null ? Thread.currentThread().getThreadGroup() : securitymanager.getThreadGroup();
			this.name = name;
		}
	}

	/**
	 * 异步回写容器.
	 * 
	 * @author 小流氓<176543888@qq.com>
	 */
	private class AsyncWriteContainer implements Runnable {
		private final Serializable roleId;
		// 当前已修改过的数据
		private Map<String, EntityOperate<?>> entityOperates = new HashMap<>();
		// 最终需要保存的数据
		private Map<String, EntityOperate<?>> flushOperates;

		private final ReentrantLock dataUpdateLock = new ReentrantLock();
		private final ReentrantLock dataFlushLock = new ReentrantLock();

		// 记录异步操作的结果，以便有需求时，操纵这个结果
		private final ScheduledFuture<?> future;

		private AsyncWriteContainer(Serializable roleId) {
			this.roleId = roleId;
			this.future = scheduledExecutor.scheduleAtFixedRate(this, DataContext.getSaveInterval(), DataContext.getSaveInterval(), TimeUnit.SECONDS);
		}

		@SuppressWarnings("unchecked")
		private <T> EntityOperate<T> getEntityOperate(EntityMapping<T> em, T entity) {
			String entityId = em.getPrimaryKey(entity);
			EntityOperate<T> entityOperate = (EntityOperate<T>) entityOperates.get(entityId);
			if (null == entityOperate) {
				entityOperate = new EntityOperate<>(entityId, em);
				entityOperates.put(entityId, entityOperate);
			}
			return entityOperate;
		}

		/**
		 * 保存一个新增的数据
		 */
		public <T> void insert(EntityMapping<T> em, T entity) {
			dataUpdateLock.lock();
			try {
				EntityOperate<T> entityOperate = getEntityOperate(em, entity);
				entityOperate.insert(entity);
			} finally {
				dataUpdateLock.unlock();
			}
		}

		/**
		 * 保存一个修改过的数据.
		 */
		public <T> void update(EntityMapping<T> em, T entity) {
			dataUpdateLock.lock();
			try {
				EntityOperate<T> entityOperate = getEntityOperate(em, entity);
				entityOperate.update(entity);
			} finally {
				dataUpdateLock.unlock();
			}
		}

		/**
		 * 删除一个数据.
		 */
		public <T> void delete(EntityMapping<T> em, T entity) {
			dataUpdateLock.lock();
			try {
				EntityOperate<T> entityOperate = getEntityOperate(em, entity);
				boolean deleted = entityOperate.delete(entity);
				if (deleted) {
					entityOperates.remove(entityOperate.getId());
				}
			} finally {
				dataUpdateLock.unlock();
			}
		}

		private Map<String, EntityOperate<?>> getNewUpdateData() {
			if (entityOperates.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<String, EntityOperate<?>> updateData = null;
			// 如果变更数据集不为空，就先把他拿出来.
			dataUpdateLock.lock();
			try {
				updateData = entityOperates;
				this.entityOperates = new HashMap<>();
			} finally {
				dataUpdateLock.unlock();
			}
			return updateData;
		}

		private void mergeFlushData(Map<String, EntityOperate<?>> updateData) {
			if (flushOperates == null) {
				flushOperates = updateData;
			} else {
				for (Entry<String, EntityOperate<?>> e : updateData.entrySet()) {
					String id = e.getKey();
					EntityOperate<?> existOperate = e.getValue();
					flushOperates.put(id, existOperate);
				}
			}
		}

		/**
		 * 同步式回写数据.
		 */
		public <T> void syncFlush() {
			dataFlushLock.lock();
			try {
				// 取出最新有过改动的数据
				Map<String, EntityOperate<?>> updateData = this.getNewUpdateData();
				// 合并到要回写的数据里
				this.mergeFlushData(updateData);

				if (flushOperates != null) {
					try {
						if (!flushOperates.isEmpty()) {
							logger.info("开始保存数据，roleId={}", roleId);
							DataAccessor dataAccessor = DataContext.getDataAccessorManager().getDataAccess(roleId);
							for (EntityOperate<?> opx : flushOperates.values()) {
								try {
									@SuppressWarnings("unchecked")
									EntityOperate<T> op = (EntityOperate<T>) opx;
									if (op.isDelete()) {
										// 那就是删除操作
										dataAccessor.delete(op.getEntityMapping(), op.getEntity());
									} else if (op.isInsert()) {
										// 插入
										dataAccessor.insert(op.getEntityMapping(), op.getEntity());
									} else if (op.isUpdate()) {
										// 修改
										dataAccessor.update(op.getEntityMapping(), op.getEntity());
									} else {
										throw new DataException("未知的操作实现...");
									}
								} catch (Exception ex) {
									logger.error("保存实体时数据异常，roleId=" + roleId, ex);
									logger.error("保存实体时的异常数据 entity={}", ToStringBuilder.reflectionToString(opx.getEntity()));
								}
							}
						}
					} finally {
						this.flushOperates = null;
					}
				}
			} finally {
				dataFlushLock.unlock();
			}
		}

		@Override
		public void run() {
			try {
				this.syncFlush();
			} catch (Exception e) {// 每次保存必需保证定时器不能停了.
				logger.error("保存个人数据时异常，roleId=" + roleId, e);
			}
		}

		public void close() {
			this.future.cancel(true);
		}
	}

	@Override
	public void asyncFlushByRoleId(Serializable roleId) {
		AsyncWriteContainer container = containers.getUnchecked(roleId);
		if (container != null) {
			scheduledExecutor.submit(container);
		}
	}
}