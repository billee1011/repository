package com.lingyu.game.service.job;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.lingyu.common.job.FixedRateTrigger;
import com.lingyu.common.job.NCronTrigger;
import com.lingyu.common.job.Schedule;
import com.lingyu.common.job.ScheduledTask;

/**
 * @author Allen Jiang
 * @since 2014-01-23
 * @version 1.0
 **/
@Service
public class ScheduleManager {
	private static final Logger logger = LogManager.getLogger(ScheduleManager.class);
	@Autowired
	private ThreadPoolTaskScheduler gameScheduler;// = (ThreadPoolTaskScheduler)
													// GameServerContext.getBean("gameScheduler");
	private ConcurrentMap<Integer, Schedule> taskMap = new ConcurrentHashMap<>();

	/**
	 * 只执行一次,多久后执行，
	 * 
	 * @param interval 多久后执行，单位秒
	 */
	public ScheduledFuture<?> scheduleOnce(Runnable runnable, int interval) {
		return gameScheduler.schedule(runnable, DateUtils.addMilliseconds(new Date(), interval));
	}

	/** 只执行一次 */
	public ScheduledFuture<?> scheduleOnce(Runnable runnable, Date date) {
		ScheduledFuture<?> future = gameScheduler.schedule(runnable, date);
		return future;
	}

	/** 可以中途取消 */
	public void scheduleOnce(int scheduleType, Runnable runnable, Date date) {
		this.removeSchedule(scheduleType);
		ScheduledFuture<?> future = this.scheduleOnce(runnable, date);
		this.addSchedule(scheduleType, future);
	}

	private void addSchedule(int scheduleType, ScheduledFuture<?> future) {
		if (scheduleType != 0) {
			Schedule schedule = new Schedule(scheduleType, 0, future);
			taskMap.put(scheduleType, schedule);
		}
	}

	/** 删除挂在该对象的某种调度事件 */
	public void removeSchedule(int scheduleType) {
		Schedule schedule = taskMap.remove(scheduleType);
		if (schedule != null) {
			this.cancel(schedule);

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

	/** @param delay 毫秒级 间隔 */
	public void scheduleWithFixedDelay(Object targetObject, String method, long delay) {

		ScheduledTask task = new ScheduledTask();
		task.setTargetObject(targetObject);
		task.setTargetMethod(method);
		task.setArguments(null);
		try {
			task.prepare();
			// ScheduledFuture future = taskScheduler.schedule(task, trigger);
			// 要取消的话要用ScheduledFuture
			gameScheduler.scheduleWithFixedDelay(task, delay);

		} catch (ClassNotFoundException | NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @param interval 毫秒级 间隔
	 */
	public void schedule(Object targetObject, String method, Date startTime, Date endTime, long interval) {
		this.schedule(targetObject, method, null, startTime, endTime, interval);
	}

	public void schedule(Object targetObject, String method, Object[] args, Date startTime, Date endTime, long interval) {
		logger.info("增加计划任务 targetObject={},method={},args={},startTime={},endTime={},interval={}", targetObject, method, args, startTime, endTime, interval);
		Trigger trigger = new FixedRateTrigger(startTime, endTime, interval);
		ScheduledTask task = new ScheduledTask();
		task.setTargetObject(targetObject);
		task.setTargetMethod(method);
		task.setArguments(args);
		try {
			task.prepare();
			// ScheduledFuture future = taskScheduler.schedule(task, trigger);
			// 要取消的话要用ScheduledFuture
			gameScheduler.schedule(task, trigger);

		} catch (ClassNotFoundException | NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void schedule(Object targetObject, String method, String cronExpression) {
		this.schedule(targetObject, method, null, cronExpression);
	}

	/**
	 * 指定cronExpression表达式执行指定对象的指定方法
	 * 
	 * @param executeTarget 指定执行对象
	 * @param method 指定执行方法
	 * @param args 方法参数
	 * @param cronExpression 0 15 10 * * ? 秒 分 时 日 月 星期 cron表达式 字段 允许值 允许的特殊字符 <br>
	 *            秒 0-59 , - * / <br>
	 *            分 0-59 , - * / <br>
	 *            小时 0-23 , - * / <br>
	 *            日期 1-31 , - * ? / L W C <br>
	 *            月份 1-12 或者 JAN-DEC , - * / <br>
	 *            星期 1-7 或者 SUN-SAT , - * ? / L C # <br>
	 *            年（可选） 留空, 1970-2099 , - * / <br>
	 *            sample: "*\\/5 * * * * ?" 每隔5秒 <br>
	 *            <property name="cronExpression" value="0 *\\/1 * * * ?" />
	 *            每隔一分钟 <br>
	 *            <property name="cronExpression" value="0 0 23 * * ?" /> 每天23点 <br>
	 *            <property name="cronExpression" value="0 0 1 1 * ?" /> 每月1号1点 <br>
	 *            <property name="cronExpression" value="0 0 23 L * ?" />
	 *            每月最后一天23点 <br>
	 *            <property name="cronExpression" value="0 0 1 ? * L" /> 每周星期天1点 <br>
	 *            <property name="cronExpression" value="0 26,29,30 * * * ?" />
	 *            "0 10,44 14 ? 3 WED" 每年三月的星期三的下午2:10和2:44触发
	 *            "0 15 10 15 * ?" 每月15日上午10:15触发  分别在26,29.30分执行<br>
	 * 
	 * @return 定时任务唯一标识
	 */
	public ScheduledFuture<?> schedule(Object targetObject, String method, Object[] args, String cronExpression) {
		logger.debug("增加计划任务 targetObject={},method={},args={},,cronExpression=[{}]", targetObject, method, args, cronExpression);
		if (cronExpression == null) {
			logger.error("当前时间的表达式为一个非法的表达式,表达式为:{}", cronExpression);
		}
		Trigger trigger = new CronTrigger(cronExpression);
		ScheduledTask task = new ScheduledTask();
		task.setTargetObject(targetObject);
		task.setTargetMethod(method);
		task.setArguments(args);
		try {
			task.prepare();
			return gameScheduler.schedule(task, trigger);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * 指定cronExpression表达式执行指定对象的指定方法
	 * 
	 * @param executeTarget 指定执行对象
	 * @param method 指定执行方法
	 * @param args 方法参数
	 * @param cronTrigger cron表达式实例
	 * 
	 * @return 定时任务唯一标识
	 */
	public void schedule(Object targetObject, String method, Object[] args, Date startTime, Date endTime, String cronExpression) {

		logger.info("增加计划任务 targetObject={},method={},args={},startTime={},endTime={},cronExpression=[{}]", targetObject.getClass().getSimpleName(), method,
				args, startTime, endTime, cronExpression);
		if (cronExpression == null) {
			logger.error("当前时间的表达式为一个非法的表达式,表达式为:{}", cronExpression);
		}
		Trigger trigger = new NCronTrigger(startTime, endTime, cronExpression);
		ScheduledTask task = new ScheduledTask();
		task.setTargetObject(targetObject);
		task.setTargetMethod(method);
		task.setArguments(args);
		try {
			task.prepare();
			// ScheduledFuture future = taskScheduler.schedule(task, trigger);
			gameScheduler.schedule(task, trigger);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			logger.error(e.getMessage(), e);
		}

	}

	public void schedule(Object targetObject, String method, Date startTime, Date endTime, String cronExpression) {
		this.schedule(targetObject, method, null, startTime, endTime, cronExpression);
	}
}
