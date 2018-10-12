package com.cai.mongo.service.log;

import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.cai.mongo.service.log.bean.RoleLogBase;
import com.cai.service.AbstractService;

public class LogService extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(LogService.class);

	private static LogService instance = new LogService();

	private final RoleLogType[] types;

	private volatile boolean isRun = true;

	public static LogService getInstance() {
		return instance;
	}

	private LogService() {
		types = new RoleLogType[LogType.values().length];
		for (LogType logType : LogType.values()) {
			types[logType.getId()] = new RoleLogType(logType);
		}

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (isRun) {
					try {
						executeLog(false);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							log.error(e.getMessage(), e);
						}
					} catch (Throwable e) {
						log.error("玩家日志线程执行出现异常", e);
					}

				}
				executeLog(true);
				log.warn("已停止玩家日志线程!");

			}
		}, "日志处理线程");
		thread.start();

	}

	/**
	 * @param isCloseServer
	 */
	private void executeLog(boolean isCloseServer) {

		for (int i = 0; i < types.length; i++) {
			RoleLogType logType = types[i];
			try {
				logType.checkIsNeedSaveLog(isCloseServer);
			} catch (Exception e) {
				log.error("日志线程 {}出现异常", e, logType.getName());
			}
		}

	}

	/**
	 * 分担多线程写的压力
	 * 
	 * @param index
	 * @param log
	 */
	public void addLog(int index, RoleLogBase log) {
		types[index].add(log);
	}

	/**
	 * 服务器关闭调用这个方法 剩余日志入库
	 */
	public void shutdown() {
		this.isRun = false;
	}

	@Override
	protected void startService() {

	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {

	}

}
