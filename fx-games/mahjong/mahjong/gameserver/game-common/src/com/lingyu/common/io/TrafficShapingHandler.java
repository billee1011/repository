package com.lingyu.common.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 亲.写一下这货是来干什么的...
 *
 * @author Kevin Lee xffforever@gmail.com
 * @version 1.0
 * @since 2014/6/27
 */
@Sharable
public class TrafficShapingHandler extends ChannelDuplexHandler {
	/**
	 * Default delay between two checks: 1s
	 */
	public static final long DEFAULT_CHECK_INTERVAL = 1000;

	/**
	 * Default minimal time to wait
	 */
	private static final long MINIMAL_WAIT = 10;

	/**
	 * Traffic Counter
	 */
	protected final TrafficCounter trafficCounter;

	/**
	 * Delay between two performance snapshots
	 */
	protected long checkInterval = DEFAULT_CHECK_INTERVAL; // default 1 s

	/**
	 * Create a new instance
	 *
	 * @param executor the {@link ScheduledExecutorService} to use for the {@link TrafficCounter}
	 */
	public TrafficShapingHandler(EventExecutor executor, String name) {
		if (executor == null) {
			throw new NullPointerException("executor");
		}
		trafficCounter = new TrafficCounter(this, executor, name, checkInterval);
		trafficCounter.start();
	}

	/**
	 * Release all internal resources of this instance
	 */
	public final void release() {
		trafficCounter.stop();
	}

	/**
	 * Change the check interval.
	 *
	 * @param newCheckInterval The new check interval (in milliseconds)
	 */
	public void configure(long newCheckInterval) {
		checkInterval = newCheckInterval;
		trafficCounter.configure(checkInterval);
	}

	/**
	 * Called each time the accounting is computed from the TrafficCounters.
	 * This method could be used for instance to implement almost real time accounting.
	 *
	 * @param counter the TrafficCounter that computes its performance
	 */
	@SuppressWarnings("unused")
	protected void doAccounting(TrafficCounter counter) {
		// NOOP by default
	}

	/**
	 * @return the time that should be necessary to wait to respect limit. Can be negative time
	 */
	private static long getTimeToWait(long limit, long bytes, long lastTime, long curtime) {
		long interval = curtime - lastTime;
		if (interval <= 0) {
			// Time is too short, so just lets continue
			return 0;
		}
		return (bytes * 1000 / limit - interval) / 10 * 10;
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		long size = calculateSize(msg);

		if (size > -1) {
			trafficCounter.bytesRecvFlowControl(size);
		}
		ctx.fireChannelRead(msg);
	}

	@Override
	public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
		long size = calculateSize(msg);

		if (size > -1) {
			trafficCounter.bytesWriteFlowControl(size);
		}
		ctx.write(msg, promise);
	}

	/**
	 * @return the current TrafficCounter (if
	 * channel is still connected)
	 */
	public TrafficCounter trafficCounter() {
		return trafficCounter;
	}

	@Override
	public String toString() {
		return "TrafficShaping Counter: " + trafficCounter.toString();
	}

	/**
	 * Calculate the size of the given {@link Object}.
	 * <p/>
	 * This implementation supports {@link io.netty.buffer.ByteBuf} and {@link io.netty.buffer.ByteBufHolder}. Sub-classes may override this.
	 *
	 * @param msg the msg for which the size should be calculated
	 * @return size     the size of the msg or {@code -1} if unknown.
	 */
	protected long calculateSize(Object msg) {
		if (msg instanceof ByteBuf) {
			return ((ByteBuf) msg).readableBytes();
		}
		if (msg instanceof ByteBufHolder) {
			return ((ByteBufHolder) msg).content().readableBytes();
		}
		return -1;
	}

}
