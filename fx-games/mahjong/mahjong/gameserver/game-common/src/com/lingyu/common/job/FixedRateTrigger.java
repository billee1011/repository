package com.lingyu.common.job;

import java.util.Date;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * @author Allen Jiang
 * @since 2014-01-23
 * @version 1.0
 **/
public class FixedRateTrigger implements Trigger {
	private final Date startTime;
	private final Date endTime;
	// false fixed-delay 下一次执行时间相对于 上一次 实际执行完成的时间点 ，因此执行时间会不断延后
	// true fixed-rate= 下一次执行时间相对于上一次开始的 时间点 ，因此执行时间不会延后，存在并发性
	private volatile boolean fixedRate = false;

	private final long period;

	public FixedRateTrigger(Date startTime, Date endTime, long period) {
		this(0,startTime,endTime,period);
	}
	/**
	 * <p>
	 * Create a trigger with the given period, start and end time that define a
	 * time window that a task will be scheduled within.
	 * </p>
	 * 
	 * @param period
	 *            单位毫秒
	 */
	public FixedRateTrigger(long delay, Date startTime, Date endTime, long period) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.period = period;
	}

	/**
	 * <p>
	 * Returns the time after which a task should run again.
	 * </p>
	 */
	public Date nextExecutionTime(TriggerContext triggerContext) {

		long now = System.currentTimeMillis();

		// if we are before the cut off
		if (now < endTime.getTime()) {
			// and after the start time
			if (now >= startTime.getTime()) {
				if (!fixedRate) {
					Date lastCompletionTime = triggerContext.lastCompletionTime();
					if (lastCompletionTime == null) {
						lastCompletionTime = new Date();
					}
					Date result = new Date(lastCompletionTime.getTime() + period);
					if (result.getTime() <= endTime.getTime()) {
						return result;
					} else {
						return null;
					}
				} else {
					Date lastScheduledExecutionTime = triggerContext.lastScheduledExecutionTime();
					if (lastScheduledExecutionTime == null) {
						lastScheduledExecutionTime = new Date();
					}
					Date result = new Date(lastScheduledExecutionTime.getTime() + period);
					if (result.getTime() <= endTime.getTime()) {
						return result;
					} else {
						return null;
					}

				}
			} else {
				return startTime;
			}
		}

		return null;
	}

	/**
	 * <p>
	 * Specify whether the periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times. The
	 * latter, "fixed delay" behavior, is the default.
	 * </p>
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FixedRateTrigger)) {
			return false;
		}
		FixedRateTrigger other = (FixedRateTrigger) obj;
		return this.period == other.period && this.startTime.equals(other.startTime) && this.endTime.equals(other.endTime);
	}

	@Override
	public int hashCode() {
		return (int) (this.period * 29) + (int) (37 * this.startTime.getTime()) + (int) (19 * this.endTime.getTime());
	}
}
