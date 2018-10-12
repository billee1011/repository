package com.lingyu.common.message;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.message.BalanceBusinessExecutor.ExecutorPoolGroup.Executor;

/**
 * 带负载均衡的执行池
 * 
 * @author hehj
 * @author Kevin Lee xffforever@gmail.com
 * @author Allen Jiang jiangguilong@mokylin.com
 * @since 2012-3-28 下午4:39:19
 */
public class BalanceBusinessExecutor {
	private static final Logger logger = LogManager.getLogger(BalanceBusinessExecutor.class);

	private final int clean_gap;
	private final Map<Byte, ExecutorPoolGroup> groups = new HashMap<>();

	/**
	 * @param cleanGap 路由信息清理时间间隔(单位：分钟)
	 * @param name 执行器名称
	 * @param groupConfigMap 线程分配 分组配置
	 */
	public BalanceBusinessExecutor(int cleanGap, Map<Byte, Integer> groupConfigMap) {
		this.clean_gap = Math.max(cleanGap, 10) * 60 * 1000;

		if (null != groupConfigMap) {
			for (Byte group : groupConfigMap.keySet()) {
				int groupSize = groupConfigMap.get(group);
				addExecutorGroup(group, groupSize);
			}
		}
	}

	public void addExecutorGroup(byte group, int size) {
		if (!groups.containsKey(group)) {
			groups.put(group, new ExecutorPoolGroup(group, size));
		}
	}

	public void execute(IRunnable runnable, byte group, String info) {
		// logger.trace("group={},info={}", group, info);
		groups.get(group).execute(info, runnable);
	}

	public Map<Byte, Map<String, Integer>> getExecutorInfos() {

		Map<Byte, Map<String, Integer>> infos = new HashMap<>();
		for (ExecutorPoolGroup group : groups.values()) {
			infos.put(group.getGroup(), group.getGroupInfo());
		}
		return infos;
	}

	/**
	 * 执行器分组
	 * 
	 * @author hehj 2010-9-6 下午03:20:37
	 * @author Kevin Lee xffforever@gmail.com
	 */
	class ExecutorPoolGroup {

		private final byte group;

		private final RouteLoadingCache loader;
		private final LoadingCache<String, Executor> routeCacheMap;

		public ExecutorPoolGroup(byte group, int size) {
			this.group = group;
			Executor[] executors = new Executor[size];
			for (int i = 0; i < size; i++) {
				executors[i] = new Executor("game-" + SystemConstant.getGroupName(group) + "-" + i);
			}
			this.loader = new RouteLoadingCache(group, executors);
			this.routeCacheMap = CacheBuilder.newBuilder().expireAfterAccess(clean_gap, TimeUnit.MILLISECONDS)
					.expireAfterWrite(clean_gap, TimeUnit.MILLISECONDS).build(loader);
		}

		public byte getGroup() {
			return this.group;
		}

		public Map<String, Integer> getGroupInfo() {
			Map<String, Integer> info = new HashMap<>();
			for (Executor executor : this.loader.getExecutors()) {
				info.put(executor.getName(), executor.getLoad());
			}
			return info;
		}

		public Executor getExecutor(String info) {
			return this.routeCacheMap.getUnchecked(info);
		}

		public void execute(String info, IRunnable runnable) {
			getExecutor(info).execute(runnable);
		}

		/**
		 * 执行器 内部使用一个核心为一的线程池 后期使用阻塞队列加单线程实现
		 * 
		 * @author Kevin Lee xffforever@gmail.com
		 */
		public class Executor {
			private final String name;
			private final BlockingQueue<IRunnable> queue = new LinkedBlockingQueue<>();
			private final Thread loop;

			private Executor(final String name) {
				this.name = name;
				loop = new Thread(name) {
					@Override
					public void run() {
						while (true) {
							try {
								IRunnable task = queue.take();
								long start = System.nanoTime();
								task.run();
								float interval = (System.nanoTime() - start) / 1000000f;
								// 只对大于20毫秒的协议进行监控
								if (task.getCommand() != 0 && interval > 20) {
									logger.debug("message interval={} ms,type={},roleId={}", interval, task.getCommand(), task.getRoleId());
								}

							} catch (Throwable e) {
								logger.error("execute[" + name + "] error", e);
							}
						}
					}
				};
				loop.start();
			}

			public int getLoad() {
				return this.queue.size();
			}

			public String getName() {
				return name;
			}

			public void execute(IRunnable command) {
				this.queue.add(command);
			}
		}

		/**
		 * 缓存key和执行器索引映射
		 * 
		 * @author Kevin Lee xffforever@gmail.com
		 */
		class RouteLoadingCache extends CacheLoader<String, Executor> {
			private final byte group;
			private final Executor[] executors;

			private RouteLoadingCache(byte group, Executor[] executors) {
				this.group = group;
				this.executors = executors;
			}

			@Override
			public Executor load(String key) throws Exception {
				// 得到一个hash值然后散列下最后取模
				int h = key.hashCode();
				// This function ensures that hashCodes that differ only by
				// constant multiples at each bit position have a bounded
				// number of collisions (approximately 8 at default load
				// factor).
				h ^= (h >>> 20) ^ (h >>> 12);
				h = h ^ (h >>> 7) ^ (h >>> 4);
				int index = h & (executors.length - 1);
				// if(group==2){
				// logger.info("group={}, index={}",group,index);
				// }
				return executors[index];
				// // 等待队列最小的线程池下标
				// int min = 0;
				// if (executors.length == 1) {
				// return executors[min];
				// }
				// for (int i = 1; i < executors.length; i++) {
				// if(group==2){
				// logger.info("group={}, load{}={},minLoad={}",group,i,executors[i].getLoad(),executors[min].getLoad());
				// }
				//
				// if (executors[i].getLoad() < executors[min].getLoad()) {
				// min = i;
				// }
				// }
				// if(group==2){
				// logger.info("最终获得 group={},minLoad={}",group,executors[min].getLoad());
				// }
				//
				// return executors[min];
			}

			public byte getGroup() {
				return group;
			}

			public Executor[] getExecutors() {
				return executors;
			}
		}
	}

	/** TODO 暂时仅供场景和个人业务模块使用 allen */
	public Executor getExecutorService(byte groupName, long objectId) {
		return groups.get(groupName).getExecutor(Long.toString(objectId));
	}

	/** 供public 模块使用 */
	public Executor getExecutorService(byte groupName, String moduleName) {
		return groups.get(groupName).getExecutor(moduleName);
	}
}
