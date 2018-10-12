package com.lingyu.common.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 线程安全
 * 
 * 
 */
public abstract class AbstractService {
	private static final Logger logger = LogManager.getLogger(AbstractService.class);

	enum State {
		STATE_NEW, STATE_STARTING, STATE_RUNNING, STATE_PAUSING, STATE_PAUSED, STATE_RESUMING, STATE_STOPPING, STATE_TERMINATED
	}

	static class ShutdownThread extends Thread {
		private final AbstractService service;

		public ShutdownThread(AbstractService service) {
			this.service = service;
		}

		@Override
		public void run() {
			if (service != null && !service.isStoppingOrTerminated()) {
				service.stop();
			}
		}
	}
	public abstract String getServiceName();
	private volatile State state = State.STATE_NEW;
	protected final String[] args;

	public AbstractService(String[] args) {
		this.args = args;

		// 设置shutdown hook
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
	}

	protected abstract void onStart() throws ServiceException;

	protected abstract void onRun() throws ServiceException;

	protected abstract void onStop() throws ServiceException;

	protected abstract void onPause() throws ServiceException;

	protected abstract void onResume() throws ServiceException;

	public State getState() {
		return state;
	}

	public boolean isRunning() {
		return state == State.STATE_RUNNING;
	}

	public boolean isStoppingOrTerminated() {
		return state == State.STATE_STOPPING || state == State.STATE_TERMINATED;
	}

	public void start() {
		logger.info("starting {} service",this.getServiceName());
		long startTime = System.currentTimeMillis();

		if (state != State.STATE_NEW) {
			logger.error("invalid state: {}", state);
			return;
		}

		state = State.STATE_STARTING;
		try {
			onStart();
			state = State.STATE_RUNNING;
			logger.info("{} is running, delta={} ms", getServiceName(), (System.currentTimeMillis() - startTime));
			System.out.println(getServiceName()+" is running,delta="+(System.currentTimeMillis() - startTime)+" ms");
			onRun();
		} catch (Exception e) {
			logger.error("failed to starting service: " + getServiceName(), e);
			System.exit(1);
			return;
		}
	}

	public void stop() {
		logger.info("stopping service: {}", getServiceName());
		if (state == State.STATE_NEW || state == State.STATE_STOPPING || state == State.STATE_TERMINATED) {
			logger.error("invalid state: {}", state);
			return;
		}

		state = State.STATE_STOPPING;
		try {
			onStop();
			state = State.STATE_TERMINATED;
			logger.info("goodbye {}", getServiceName());
			System.out.println("goodbye "+getServiceName());
		} catch (Exception e) {
			logger.error("failed to stopping service: " + getServiceName(), e);
			return;
		}

	}

	public void pause() {
		logger.info("pausing service: {}", getServiceName());
		if (state != State.STATE_RUNNING) {
			logger.error("invalid state: {}", state);
			return;
		}

		state = State.STATE_PAUSING;
		try {
			onPause();
			state = State.STATE_PAUSED;
		} catch (Exception e) {
			logger.error("failed to pausing service: " + getServiceName(), e);
			return;
		}

	}

	public void resume() {
		logger.info("resuming service: {}", getServiceName());
		if (state != State.STATE_PAUSED) {
			logger.error("invalid state: {}", state);
			return;
		}

		state = State.STATE_RESUMING;
		try {
			onResume();
			state = State.STATE_RUNNING;
		} catch (Exception e) {
			logger.error("failed to resuming service: " + getServiceName(), e);
			return;
		}

	}

}
