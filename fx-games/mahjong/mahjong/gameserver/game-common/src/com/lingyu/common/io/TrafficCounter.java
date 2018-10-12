package com.lingyu.common.io;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 亲.写一下这货是来干什么的...
 *
 * @author Kevin Lee xffforever@gmail.com
 * @version 1.0
 * @since 2014/6/27
 */
public class TrafficCounter {
	/**
	 * Current transaction num
	 */
	private final AtomicLong currentTransactionNum = new AtomicLong();

	/**
	 * Current written bytes
	 */
	private final AtomicLong currentWrittenBytes = new AtomicLong();

	/**
	 * Current read bytes
	 */
	private final AtomicLong currentReadBytes = new AtomicLong();


	/**
	 * Long life transaction num
	 */
	private final AtomicLong cumulativeTransactionNum = new AtomicLong();

	/**
	 * Long life written bytes
	 */
	private final AtomicLong cumulativeWrittenBytes = new AtomicLong();

	/**
	 * Long life read bytes
	 */
	private final AtomicLong cumulativeReadBytes = new AtomicLong();


	/**
	 * Last Time where cumulative bytes where reset to zero
	 */
	private long lastCumulativeTime;

	/**
	 * Last transaction bandwidth
	 */
	private long lastTransactionThroughput;
	private long lastCumulativeTransactionThroughput;

	/**
	 * Last writing bandwidth
	 */
	private long lastWriteThroughput;
	private long lastCumulativeReadThroughput;
	private long lastCumulativeWriteThroughput;

	/**
	 * Last reading bandwidth
	 */
	private long lastReadThroughput;

	/**
	 * Last Time Check taken
	 */
	private final AtomicLong lastTime = new AtomicLong();

	/**
	 * Last transact num number during last check interval
	 */
	private long lastTransactionNum;
	private long lastCumulativeTransactionNum;

	/**
	 * Last written bytes number during last check interval
	 */
	private long lastWrittenBytes;
	private long lastCumulativeReadBytes;
	private long lastCumulativeWrittenBytes;

	/**
	 * Last read bytes number during last check interval
	 */
	private long lastReadBytes;

	/**
	 * Delay between two captures
	 */
	final AtomicLong checkInterval = new AtomicLong(
			TrafficShapingHandler.DEFAULT_CHECK_INTERVAL);

	// default 1 s

	/**
	 * Name of this Monitor
	 */
	final String name;

	/**
	 * The associated TrafficShapingHandler
	 */
	private final TrafficShapingHandler trafficShapingHandler;

	/**
	 * Executor that will run the monitor
	 */
	private final    ScheduledExecutorService executor;
	/**
	 * Monitor created once in start()
	 */
	private          Runnable                 monitor;
	/**
	 * used in stop() to cancel the timer
	 */
	private volatile ScheduledFuture<?>       scheduledFuture;

	/**
	 * Is Monitor active
	 */
	final AtomicBoolean monitorActive = new AtomicBoolean();

	/**
	 * Class to implement monitoring at fix delay
	 */
	private static class TrafficMonitoringTask implements Runnable {
		/**
		 * The associated TrafficShapingHandler
		 */
		private final TrafficShapingHandler trafficShapingHandler1;

		/**
		 * The associated TrafficCounter
		 */
		private final TrafficCounter counter;

		/**
		 * @param trafficShapingHandler The parent handler to which this task needs to callback to for accounting
		 * @param counter               The parent TrafficCounter that we need to reset the statistics for
		 */
		protected TrafficMonitoringTask(
				TrafficShapingHandler trafficShapingHandler,
				TrafficCounter counter) {
			trafficShapingHandler1 = trafficShapingHandler;
			this.counter = counter;
		}

		@Override
		public void run() {
			if (!counter.monitorActive.get()) {
				return;
			}
			long endTime = System.currentTimeMillis();
			counter.resetAccounting(endTime);
			if (trafficShapingHandler1 != null) {
				trafficShapingHandler1.doAccounting(counter);
			}
			counter.scheduledFuture = counter.executor.schedule(this, counter.checkInterval.get(),
					TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Start the monitoring process
	 */
	public synchronized void start() {
			if (monitorActive.get()) {
				return;
			}
			lastTime.set(System.currentTimeMillis());
			if (checkInterval.get() > 0) {
				monitorActive.set(true);
				monitor = new TrafficMonitoringTask(trafficShapingHandler, this);
				scheduledFuture =
						executor.schedule(monitor, checkInterval.get(), TimeUnit.MILLISECONDS);
			}
		
	}

	/**
	 * Stop the monitoring process
	 */
	public synchronized void stop() {
			if (!monitorActive.get()) {
				return;
			}
			monitorActive.set(false);
			resetAccounting(System.currentTimeMillis());
			if (trafficShapingHandler != null) {
				trafficShapingHandler.doAccounting(this);
			}
			if (scheduledFuture != null) {
				scheduledFuture.cancel(true);
			}
		}
	

	/**
	 * Reset the accounting on Read and Write
	 *
	 * @param newLastTime the millisecond unix timestamp that we should be considered up-to-date for
	 */
	synchronized void resetAccounting(long newLastTime) {
			long interval = newLastTime - lastTime.getAndSet(newLastTime);
			long cumulativeInterval = newLastTime - lastCumulativeTime();
			if (interval == 0) {
				// nothing to do
				return;
			}
			lastTransactionNum = currentTransactionNum.getAndSet(0);
			lastCumulativeTransactionNum = cumulativeTransactionNum();
			lastReadBytes = currentReadBytes.getAndSet(0);
			lastWrittenBytes = currentWrittenBytes.getAndSet(0);
			lastCumulativeReadBytes = cumulativeReadBytes();
			lastCumulativeWrittenBytes = cumulativeWrittenBytes();

			lastTransactionThroughput = lastTransactionNum * 1000 / interval;
			lastCumulativeTransactionThroughput = lastCumulativeTransactionNum * 1000 / cumulativeInterval;
			lastReadThroughput = lastReadBytes * 1000 / interval;
			// nb byte / checkInterval in ms * 1000 (1s)
			lastWriteThroughput = lastWrittenBytes * 1000 / interval;
			// nb byte / checkInterval in ms * 1000 (1s)
			lastCumulativeReadThroughput = lastCumulativeReadBytes * 1000 / cumulativeInterval;
			// nb byte / checkInterval in ms * 1000 (1s)
			lastCumulativeWriteThroughput = lastCumulativeWrittenBytes * 1000 / cumulativeInterval;
			// nb byte / checkInterval in ms * 1000 (1s)
		
	}

	/**
	 * Constructor with the {@link TrafficShapingHandler} that hosts it, the Timer to use, its
	 * name, the checkInterval between two computations in millisecond
	 *
	 * @param trafficShapingHandler the associated AbstractTrafficShapingHandler
	 * @param executor              the underlying executor service for scheduling checks
	 * @param name                  the name given to this monitor
	 * @param checkInterval         the checkInterval in millisecond between two computations
	 */
	public TrafficCounter(TrafficShapingHandler trafficShapingHandler, ScheduledExecutorService executor, String name, long checkInterval) {
		this.trafficShapingHandler = trafficShapingHandler;
		this.executor = executor;
		this.name = name;
		lastCumulativeTime = System.currentTimeMillis();
		configure(checkInterval);
	}

	/**
	 * Change checkInterval between two computations in millisecond
	 *
	 * @param newcheckInterval The new check interval (in milliseconds)
	 */
	public void configure(long newcheckInterval) {
		long newInterval = newcheckInterval / 10 * 10;
		if (checkInterval.get() != newInterval) {
			checkInterval.set(newInterval);
			if (newInterval <= 0) {
				stop();
				// No more active monitoring
				lastTime.set(System.currentTimeMillis());
			} else {
				// Start if necessary
				start();
			}
		}
	}

	/**
	 * Computes counters for Read.
	 *
	 * @param recv the size in bytes to read
	 */
	void bytesRecvFlowControl(long recv) {
		currentReadBytes.addAndGet(recv);
		cumulativeReadBytes.addAndGet(recv);
		currentTransactionNum.incrementAndGet();
		cumulativeTransactionNum.incrementAndGet();
	}

	/**
	 * Computes counters for Write.
	 *
	 * @param write the size in bytes to write
	 */
	void bytesWriteFlowControl(long write) {
		currentWrittenBytes.addAndGet(write);
		cumulativeWrittenBytes.addAndGet(write);
	}

	/**
	 * @return the current checkInterval between two computations of traffic counter
	 * in millisecond
	 */
	public long checkInterval() {
		return checkInterval.get();
	}

	/**
	 * @return the Read Throughput in bytes/s computes in the last check interval
	 */
	public long lastReadThroughput() {
		return lastReadThroughput;
	}

	/**
	 * @return the Write Throughput in bytes/s computes in the last check interval
	 */
	public long lastWriteThroughput() {
		return lastWriteThroughput;
	}

	/**
	 * @return the number of bytes read during the last check Interval
	 */
	public long lastReadBytes() {
		return lastReadBytes;
	}

	/**
	 * @return the number of bytes written during the last check Interval
	 */
	public long lastWrittenBytes() {
		return lastWrittenBytes;
	}

	/**
	 * @return the current number of bytes read since the last checkInterval
	 */
	public long currentReadBytes() {
		return currentReadBytes.get();
	}

	/**
	 * @return the current number of bytes written since the last check Interval
	 */
	public long currentWrittenBytes() {
		return currentWrittenBytes.get();
	}

	/**
	 * @return the Time in millisecond of the last check as of System.currentTimeMillis()
	 */
	public long lastTime() {
		return lastTime.get();
	}

	/**
	 * @return the cumulativeWrittenBytes
	 */
	public long cumulativeWrittenBytes() {
		return cumulativeWrittenBytes.get();
	}

	/**
	 * @return the cumulativeReadBytes
	 */
	public long cumulativeReadBytes() {
		return cumulativeReadBytes.get();
	}

	/**
	 * @return the transactionNum
	 */
	public long cumulativeTransactionNum() {
		return cumulativeTransactionNum.get();
	}

	/**
	 * @return the lastCumulativeTime in millisecond as of System.currentTimeMillis()
	 * when the cumulative counters were reset to 0.
	 */
	public long lastCumulativeTime() {
		return lastCumulativeTime;
	}

	/**
	 * Reset both read and written cumulative bytes counters and the associated time.
	 */
	public void resetCumulativeTime() {
		lastCumulativeTime = System.currentTimeMillis();
		cumulativeReadBytes.set(0);
		cumulativeWrittenBytes.set(0);
		cumulativeTransactionNum.set(0);
	}

	/**
	 * @return the name
	 */
	public String name() {
		return name;
	}

	/**
	 * String information
	 */
	@Override
	public String toString() {
		return "Monitor " + name +
				" Total T: " + (lastCumulativeTransactionNum) + "" +
				" Average TPS: " + (lastCumulativeTransactionThroughput) + "" +
				" Current TPS: " + (lastTransactionThroughput) + "" +
				" Average Speed Read: " + lastCumulativeReadThroughput + " byte/s, Write: " + lastCumulativeWriteThroughput + " byte/s" +
				" Total Read: " + (lastCumulativeReadBytes >> 10) + " KB, Write: " + (lastCumulativeWrittenBytes >> 10) + " KB" +
				" Current Speed Read: " + lastReadThroughput  + " byte/s, Write: " + lastWriteThroughput+ " byte/s" +
				" Current Read: " + currentReadBytes.get() + " byte, Write: " + currentWrittenBytes.get() + " byte"
				;
	}
}
