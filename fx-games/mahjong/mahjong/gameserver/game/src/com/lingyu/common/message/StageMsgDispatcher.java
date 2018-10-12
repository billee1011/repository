package com.lingyu.common.message;

/**
 * @description 客户端接入消息分发器
 * @author hehj 2011-11-4 下午3:04:30
 */
public class StageMsgDispatcher {

	private ThreadLocal<ExecutorRunnablePool> runnableLocal = new ThreadLocal<ExecutorRunnablePool>();

	private BalanceBusinessExecutor businessExexutor;

	private MessageMediator defaultManager;

	public StageMsgDispatcher(BalanceBusinessExecutor businessExexutor, MessageMediator defaultManager) {
		this.businessExexutor = businessExexutor;
		this.defaultManager = defaultManager;
	}

	private ExecutorRunnablePool getRunnablePool() {
		ExecutorRunnablePool ret = runnableLocal.get();
		if (null == ret) {
			ret = new ExecutorRunnablePool(defaultManager);
			runnableLocal.set(ret);
		}
		return ret;
	}
}
