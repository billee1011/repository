/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.util.Pair;
import com.cai.service.MongoDBServiceImpl;
import com.xianyi.framework.core.transport.statistics.SocketIOUtil;

/**
 * 流量统计上报定时器
 *
 * @author wu_hc date: 2017年7月27日 下午8:43:03 <br/>
 */
public class SocketIOTimer extends TimerTask {

	private final SocketIOUtil socketIO;

	// 上一次的记录
	private long lastInCount;

	private long lastOutCount;

	private long lastInBytes;

	private long lastOutBytes;

	/**
	 * @param socketIO
	 */
	public SocketIOTimer(SocketIOUtil socketIO) {
		this.socketIO = socketIO;
	}

	@Override
	public void run() {
		Pair<Long, Long> in = socketIO.getInStatistics();
		Pair<Long, Long> out = socketIO.getOutStatistics();

		// 当前的
		long curInCount = in.getFirst();
		long curInBytes = in.getSecond();
		long curOutCount = out.getFirst();
		long curOutBytes = out.getSecond();

		// 本次变化的
		long changeInCount = curInCount - lastInCount;
		long changeInBytes = curInBytes - lastInBytes;
		long changeOutCount = curOutCount - lastOutCount;
		long changeOutBytes = curOutBytes - lastOutBytes;

		// 记录
		lastInCount = curInCount;
		lastInBytes = curInBytes;

		lastOutCount = curOutCount;
		lastOutBytes = curOutBytes;

		// 入库
		MongoDBServiceImpl.getInstance().systemLog(ELogType.socketStatePack, null, changeInCount, changeOutCount, ESysLogLevelType.NONE);
		MongoDBServiceImpl.getInstance().systemLog(ELogType.socketStateBytes, null, changeInBytes, changeOutBytes, ESysLogLevelType.NONE);
	}
}
