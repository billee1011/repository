package com.lingyu.game.service.job;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.http.MethodWrapper;
import com.lingyu.common.job.Schedule;
import com.lingyu.common.job.Scheduled;
import com.lingyu.common.message.BalanceBusinessExecutor;
import com.lingyu.common.message.IRunnable;
import com.lingyu.game.GameServerContext;

/**
 * @author Allen Jiang
 * @since 2014-01-23
 * @version 1.0
 **/
@Service
public class AsyncManager {
	private static final Logger logger = LogManager.getLogger(AsyncManager.class);
	private Map<Byte, MethodWrapper> cachedMethod = new HashMap<>();
	private ThreadPoolTaskScheduler scheduler;// 用于所有场景的异步事件调用
	@Autowired
	@Qualifier("publicExecutor")
	private BalanceBusinessExecutor publicExecutor;
	@Autowired
	@Qualifier("busExecutor")
	private BalanceBusinessExecutor busExecutor;
	/** 调度类型列表 */
	private List<Byte> typeList = new ArrayList<>();
	/** 所有Schedule，key 为type:id */
	// GameServerContext.getBean("gameScheduler");
	private ConcurrentMap<String, Schedule> taskMap = new ConcurrentHashMap<>();

	public void init() {
		logger.info("调度事件初始化开始");
		this.parseScheduleType();
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("async-event-");
		scheduler.initialize();
		ApplicationContext context = GameServerContext.getAppContext();
		String[] list = context.getBeanDefinitionNames();
		for (String name : list) {
			Object instance = context.getBean(name);
			// 获取MethodAccess 对象
			MethodAccess access = MethodAccess.get(instance.getClass());
			Method[] methods = instance.getClass().getDeclaredMethods();
			for (Method method : methods) {
				Scheduled schedule = method.getAnnotation(Scheduled.class);
				if (schedule != null) {
					int methodIndex = access.getIndex(method.getName(), method.getParameterTypes());
					byte type = schedule.type();
					MethodWrapper wrapper = cachedMethod.get(type);
					if (wrapper != null) {
						logger.error("调度事件处理方法被多次定义 type={},method1={},method2={},methodIndex={}", type, wrapper.getMethod().getName(),
								method.getName(), methodIndex);
						throw new ServiceException("调度事件处理方法被多次定义 type=" + type);
					} else {
						cachedMethod.put(type, new MethodWrapper(method, instance, access, methodIndex));
					}
				}
			}
		}
		logger.info("调度事件初始化完毕");
	}

	public void parseScheduleType() {
		Field[] fields = ScheduleType.class.getFields();
		for (Field field : fields) {
			try {
				Byte type = (Byte) field.get(ScheduleType.class);
				typeList.add(type);
			} catch (Exception e) {
				logger.error(e.getMessage() + ":" + field.getName(), e);
			}
		}
	}

	/**
	 * 固定频率的重复性调度器|给bus用
	 * 
	 * @param stageId
	 *            场景ID
	 * @param scheduleType
	 *            调度类型
	 * @param objectId
	 *            对象ID
	 * @param startTime
	 *            调度的有效开始时间
	 * @param interval
	 *            毫秒级 调度间隔
	 * @param args
	 *            参数值
	 */
	public void schedule(byte scheduleType, long objectId, Date startTime, int interval) {
		this.schedule(scheduleType, objectId, startTime, interval, objectId);
	}

	/**
	 * 固定频率的重复性调度器|给bus用
	 * 
	 * @param stageId
	 *            场景ID
	 * @param scheduleType
	 *            调度类型
	 * @param objectId
	 *            对象ID
	 * @param startTime
	 *            调度的有效开始时间
	 * @param interval
	 *            毫秒级 调度间隔
	 * @param args
	 *            参数值
	 */
	public void schedule(byte scheduleType, long objectId, Date startTime, int interval, Object... args) {
		this.removeSchedule(scheduleType, objectId);
		logger.info("增加计划任务 scheduleType={},objectId={},args={},startTime={},interval={} ms", scheduleType, objectId, args, startTime, interval);
		RouterTask task = new RouterTask(SystemConstant.GROUP_BUS_CACHE, objectId, scheduleType, args);
		ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(task, startTime, interval);
		this.addSchedule(scheduleType, objectId, future);
	}

	/**
	 * 用于bus组调度执行
	 * 
	 * @param scheduleType
	 *            调度类型
	 * @param objectId
	 *            对象ID
	 * @param delay
	 *            延迟多少时间执行一次 单位 毫秒级
	 * @param args
	 *            变量
	 */
	public void scheduleOnce(byte scheduleType, long objectId, long delay, Object... args) {
		this.removeSchedule(scheduleType, objectId);
		// logger.debug("增加计划任务
		// stageId={},scheduleType={},objectId={},args={},delay={} ms",
		// stageId, scheduleType, objectId, args, delay);
		Date startTime = new Date(System.currentTimeMillis() + delay);
		RouterTask task = new RouterTask(SystemConstant.GROUP_BUS_CACHE, objectId, scheduleType, args);
		ScheduledFuture<?> future = scheduler.schedule(task, startTime);
		// 小于1000 ms的话，没有取消的必要了
		if (delay > 1000) {
			this.addSchedule(scheduleType, objectId, future);
		}
	}

	/**
	 * 用于public组调度执行
	 * 
	 * @param scheduleType
	 *            调度类型
	 * @param moduleName
	 *            模块名
	 * @param objectId
	 *            对象ID
	 * @param delay
	 *            延迟多少时间执行一次 单位 毫秒级
	 * @param args
	 *            变量
	 */
	public void scheduleOnce(byte scheduleType, String moduleName, long objectId, long delay, Object... args) {
		this.removeSchedule(scheduleType, objectId);
		// logger.debug("增加计划任务
		// stageId={},scheduleType={},objectId={},args={},delay={} ms",
		// stageId, scheduleType, objectId, args, delay);
		Date startTime = new Date(System.currentTimeMillis() + delay);
		RouterTask task = new RouterTask(SystemConstant.GROUP_PUBLIC, moduleName, scheduleType, args);
		ScheduledFuture<?> future = scheduler.schedule(task, startTime);
		// 小于1000 ms的话，没有取消的必要了
		if (delay > 1000) {
			this.addSchedule(scheduleType, objectId, future);
		}
	}

	/** 正在处理的事件将被继续处理完成 */
	public boolean cancel(Schedule schedule) {
		boolean ret = false;
		ScheduledFuture<?> future = schedule.getFuture();
		if (!future.isCancelled()) {
			ret = future.cancel(false);
		}
		return ret;
	}

	private void addSchedule(int scheduleType, long objectId, ScheduledFuture<?> future) {
		if (scheduleType != 0 && objectId != 0) {
			Schedule schedule = new Schedule(scheduleType, objectId, future);
			taskMap.put(scheduleType + ":" + objectId, schedule);
			// LOCK.lock();
			// try {
			// List<Schedule> list = scheduleMap.get(objectId);
			// if (null == list) {
			// list = new ArrayList<>();
			// scheduleMap.put(objectId, list);
			// }
			// list.add(schedule);
			// } finally {
			// LOCK.unlock();
			// }

		}
	}

	/** 删除挂在该对象的某种调度事件 */
	public void removeSchedule(int scheduleType, long objectId) {
		Schedule schedule = taskMap.remove(scheduleType + ":" + objectId);
		if (schedule != null) {
			// logger.warn("删除任务调度 scheduleType={},objectId={}", scheduleType,
			// objectId);
			this.cancel(schedule);
			// List<Schedule> list = scheduleMap.get(objectId);
			// if (list != null) {
			// for (Schedule e : list) {
			// if (e.getType() == scheduleType) {
			// list.remove(e);
			// break;
			// }
			// }
			// // if (list.isEmpty()) {
			// // scheduleMap.remove(objectId);
			// // }
			// }
		}
	}

	/** 删除挂在该对象的所有调度事件 */
	public void removeScheduleList(long objectId) {
		for (Byte e : typeList) {
			// taskMap.remove(e+":"+objectId);
			Schedule schedule = taskMap.remove(e + ":" + objectId);
			if (schedule != null) {
				this.cancel(schedule);
			}
		}
		// List<Schedule> list = scheduleMap.remove(objectId);
		// if (list != null) {
		// // logger.debug("删除任务调度 objectId={}", objectId);
		// for (Schedule e : list) {
		// Schedule schedule = taskMap.remove(e.getKey());
		// this.cancel(schedule);
		// }
		// }
	}

	/** 定期清理完成的Schedule */
	public void clearSchedule4Done() {
		Collection<Schedule> list = taskMap.values();
		Set<Integer> set = new HashSet<>();
		int num = 0;
		for (Schedule e : list) {
			if (e.getFuture().isDone()) {
				taskMap.remove(e.getKey());
				// List<Schedule> scheduleList = scheduleMap.get(e.getId());
				// if (scheduleList != null) {
				// scheduleList.remove(e);
				// if (scheduleList.isEmpty()) {
				// scheduleMap.remove(e.getId());
				//
				// }
				//
				// }
				num++;
				set.add(e.getType());
			}
		}
		logger.warn("处理未有及时清理的调度对象type={},num={}", set, num);
	}

	/** 具体的调度任务 */
	private class ScheduleTask implements IRunnable {
		private byte scheduleType;
		private Object[] args;

		public ScheduleTask(byte scheduleType, Object... args) {
			this.scheduleType = scheduleType;
			this.args = args;
		}

		@Override
		public void run() {
			call(scheduleType, args);
		}

		@Override
		public int getCommand() {
			return scheduleType;
		}

		@Override
		public long getRoleId() {
			return 0;
		}
	}

	private Object call(byte type, Object... args) throws ServiceException {
		Object ret = null;
		MethodWrapper method = cachedMethod.get(type);
		if (method != null) {
			try {
				if (args == null) {
					method.invoke();
				} else {
					method.invoke(args);
				}
			} catch (Exception e) {
				logger.error(e.getMessage() + " type=" + type, e);
			}
		} else {
			logger.error("调度类型找不到相应的方法 type={}", type);
		}
		return ret;
	}

	/** 路由任务 */
	private class RouterTask implements Runnable {
		private ScheduleTask task;
		private long objectId;
		private byte groupName;
		private String moduleName;

		public RouterTask(byte groupName, String moduleName, byte scheduleType, Object... args) {
			this.moduleName = moduleName;
			this.groupName = groupName;
			task = new ScheduleTask(scheduleType, args);
		}

		public RouterTask(byte groupName, long objectId, byte scheduleType, Object... args) {
			this.groupName = groupName;
			this.objectId = objectId;
			task = new ScheduleTask(scheduleType, args);
		}

		@Override
		public void run() {
			switch (groupName) {
			// case SystemConstant.GROUP_STAGE: {
			// stageExecutor.getExecutorService(groupName,
			// objectId).execute(task);
			// break;
			// }
			case SystemConstant.GROUP_PUBLIC: {
				publicExecutor.getExecutorService(groupName, moduleName).execute(task);
				break;
			}
			default: {
				busExecutor.getExecutorService(groupName, objectId).execute(task);
			}
			}
		}
	}
}
