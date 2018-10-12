package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.DBUpdateDto;
import com.cai.common.util.ThreadUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 数据库异步入库
 * 
 * @author run
 *
 */
public final class DataThreadPool {
	Logger logger = LoggerFactory.getLogger(DataThreadPool.class);

	private static volatile DataThreadPool task = null;

	private int minPoolSize = 20;

	private int maxPoolSize = 20;

	private LinkedBlockingQueue<Runnable> blockQueue = new LinkedBlockingQueue<Runnable>();
	private ThreadPoolExecutor tpe;

	public static DataThreadPool getInstance() {
		if (task == null) {
			synchronized (DataThreadPool.class) {
				if (task == null) {
					task = new DataThreadPool();
				}
			}
		}

		return task;

	}
	
	
	public ThreadPoolExecutor getTpe() {
		return tpe;
	}

	public DataThreadPool() {

		tpe = new ThreadPoolExecutor(minPoolSize, maxPoolSize, 0, TimeUnit.SECONDS, blockQueue);
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("DataThreadPool-%d");
		tpe.setThreadFactory(tfb.build());

	}

	public void addTask(DbInvoker task) {
		try {
			// System.out.println("当前收到数据库操作:"+tpe.getTaskCount()+",已处理消息数:"+tpe.getCompletedTaskCount()+",当前排队数量:"+blockQueue.size());
			tpe.execute(task);
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.error("task error "+ThreadUtil.getStack(exception));
			logger.error("server threadpool full,threadpool maxsize is:" + ((ThreadPoolExecutor) tpe).getMaximumPoolSize()+"task=="+task.getDbUpdateDto().getSqlStr());
		}
	}

	public void addTask(DBUpdateDto dbUpdateDto) {
		DbInvoker dbInvoker = new DbInvoker(dbUpdateDto);
		this.addTask(dbInvoker);
	}

	public synchronized void shutdown() {
		tpe.shutdown();
	}

	public int getActiveCount() {
		return tpe.getActiveCount();
	}

	public synchronized boolean remove(Runnable task) {
		return tpe.remove(task);
	}

	public LinkedBlockingQueue<Runnable> getBlockQueue() {
		return blockQueue;
	}

	public void setBlockQueue(LinkedBlockingQueue<Runnable> blockQueue) {
		this.blockQueue = blockQueue;
	}

}