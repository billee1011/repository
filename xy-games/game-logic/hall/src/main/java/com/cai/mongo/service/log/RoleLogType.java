package com.cai.mongo.service.log;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.mongo.service.imp.RoleLogService;
import com.cai.mongo.service.log.bean.RoleLogBase;

public class RoleLogType {

	private static final Logger log = LoggerFactory.getLogger(RoleLogType.class);

	/**
	 * 玩家日志类型枚举
	 */
	private LogType logType;
	/**
	 * 日志队列
	 */
	private LinkedBlockingQueue<RoleLogBase> queue = new LinkedBlockingQueue<RoleLogBase>();

	/**
	 * 用于保存处理入库时日志队列中的所有元素
	 */
	private ArrayList<RoleLogBase> executeQueue = new ArrayList<RoleLogBase>(LOG_SIZE);

	private final RoleLogService roleLogService;

	private final PerformanceTimer perforTime;

	private static final int LOG_SIZE = 10000;

	public RoleLogType(LogType logType) {
		this.logType = logType;
		perforTime = new PerformanceTimer();
		this.roleLogService = SpringService.getBean(RoleLogService.class);
	}

	/**
	 * 添加日志对象
	 * 
	 * @param log
	 */
	public void add(RoleLogBase log) {
		queue.add(log);
	}

	/**
	 * 检查是否需要保存日志，需要的话则保存
	 */
	public void checkIsNeedSaveLog(boolean isCloseServer) {
		if (queue.isEmpty()) {
			return;
		}
		executeQueue.clear();
		queue.drainTo(executeQueue);
		saveLogToDB();
	}

	/**
	 * 保存日志到数据库
	 */
	private void saveLogToDB() {
		int currentSize = executeQueue.size();
		boolean isNeedLog = currentSize >= LOG_SIZE;
		if (isNeedLog) {
			perforTime.reset();
		}

		roleLogService.insertAll(executeQueue);

		if (isNeedLog) {
			log.warn("saveLogToDB size == {},costTime = {},type={}", currentSize, perforTime.duration(), getName());
		}

	}

	public String getName() {
		return logType.getName();
	}

}
