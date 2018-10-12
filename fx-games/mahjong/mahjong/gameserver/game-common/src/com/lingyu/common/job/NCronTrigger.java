package com.lingyu.common.job;

import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

/**
 * @author Allen Jiang
 * @since 2014-01-23
 * @version 1.0
 **/
public class NCronTrigger implements Trigger {
	private static final Logger logger = LogManager.getLogger(NCronTrigger.class);
	private final CronSequenceGenerator sequenceGenerator;
	private final Date startTime;
	private final Date endTime;

	/**
	 * Build a {@link CronTrigger} from the pattern provided in the default time
	 * zone.
	 * 
	 * @param cronExpression
	 *            a space-separated list of time fields, following cron
	 *            expression conventions
	 */
	public NCronTrigger(Date startTime, Date endTime, String cronExpression) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.sequenceGenerator = new CronSequenceGenerator(cronExpression);
	}

	/**
	 * Build a {@link CronTrigger} from the pattern provided.
	 * 
	 * @param cronExpression
	 *            a space-separated list of time fields, following cron
	 *            expression conventions
	 * @param timeZone
	 *            a time zone in which the trigger times will be generated
	 */
	public NCronTrigger(Date startTime, Date endTime, String cronExpression, TimeZone timeZone) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.sequenceGenerator = new CronSequenceGenerator(cronExpression, timeZone);
	}

	/**
	 * Determine the next execution time according to the given trigger context.
	 * <p>
	 * Next execution times are calculated based on the
	 * {@linkplain TriggerContext#lastCompletionTime completion time} of the
	 * previous execution; therefore, overlapping executions won't occur.
	 */
	public Date nextExecutionTime(TriggerContext triggerContext) {
		long now = System.currentTimeMillis();
		// if we are before the cut off
		if (endTime == null || now < endTime.getTime()) {
			// and after the start time
			if (now >= startTime.getTime()) {
				Date date = triggerContext.lastCompletionTime();
				if (date != null) {
					Date scheduled = triggerContext.lastScheduledExecutionTime();
					if (scheduled != null && date.before(scheduled)) {
						// Previous task apparently executed too early...
						// Let's simply use the last calculated execution time
						// then,
						// in order to prevent accidental re-fires in the same
						// second.
						date = scheduled;
					}
				} else {
					date = new Date();
				}
				// logger.info("result={},date={}",result,date);
				Date result = this.sequenceGenerator.next(date);
				if (endTime == null || result.getTime() <= endTime.getTime()) {
					return result;
				} else {
					return null;
				}

			} else {
				Date result = this.sequenceGenerator.next(startTime);
				if (endTime == null || result.getTime() <= endTime.getTime()) {
					return result;
				} else {
					return null;
				}
			}
		}
		return null;

	}

	public String getExpression() {
		String array = this.sequenceGenerator.toString();
		return StringUtils.split(array, ":")[1].trim();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NCronTrigger)) {
			return false;
		}
		NCronTrigger other = (NCronTrigger) obj;
		return this.sequenceGenerator == other.sequenceGenerator && this.startTime.equals(other.startTime) && this.endTime.equals(other.endTime);
	}

	@Override
	public int hashCode() {
		return this.sequenceGenerator.hashCode();
	}

	@Override
	public String toString() {
		return this.sequenceGenerator.toString();
	}

}
