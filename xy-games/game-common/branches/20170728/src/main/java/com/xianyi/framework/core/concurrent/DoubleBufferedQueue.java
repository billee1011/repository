/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.core.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author
 */
@SuppressWarnings("unchecked")
public class DoubleBufferedQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(DoubleBufferedQueue.class.getName());

	/** The queued items */
	private final E[] itemsA;
	private final E[] itemsB;

	private ReentrantLock readLock, writeLock;
	private Condition notEmpty;
	private Condition notFull;
	private Condition awake;

	private E[] writeArray, readArray;
	private volatile int writeCount, readCount;
	private int writeArrayHP, writeArrayTP, readArrayHP, readArrayTP;

	public DoubleBufferedQueue(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Queue initial capacity can't less than 0!");
		}

		itemsA = (E[]) new Object[capacity];
		itemsB = (E[]) new Object[capacity];

		readLock = new ReentrantLock();
		writeLock = new ReentrantLock();

		notEmpty = readLock.newCondition();
		notFull = writeLock.newCondition();
		awake = writeLock.newCondition();

		readArray = itemsA;
		writeArray = itemsB;
	}

	private void insert(E e) {
		writeArray[writeArrayTP] = e;
		++writeArrayTP;
		++writeCount;
	}

	private E extract() {
		E e = readArray[readArrayHP];
		readArray[readArrayHP] = null;
		++readArrayHP;
		--readCount;
		return e;
	}

	/**
	 * switch condition: read queue is empty && write queue is not empty
	 * 
	 * Notice:This function can only be invoked after readLock is grabbed,or may
	 * cause dead lock
	 * 
	 * @param timeout
	 * @param isInfinite:
	 *            whether need to wait forever until some other thread awake it
	 * @return
	 * @throws InterruptedException
	 */
	private long queueSwitch(long timeout, boolean isInfinite) throws InterruptedException {
		writeLock.lock();
		try {
			if (writeCount <= 0) {
				logger.debug("Write Count:" + writeCount + ", Write Queue is empty, do not switch!");
				try {
					logger.debug("Queue is empty, need wait....");
					if (isInfinite && timeout <= 0) {
						awake.await();
						return -1;
					} else {
						return awake.awaitNanos(timeout);
					}
				} catch (InterruptedException ie) {
					awake.signal();
					throw ie;
				}
			} else {
				E[] tmpArray = readArray;
				readArray = writeArray;
				writeArray = tmpArray;

				readCount = writeCount;
				readArrayHP = 0;
				readArrayTP = writeArrayTP;

				writeCount = 0;
				writeArrayHP = readArrayHP;
				writeArrayTP = 0;

				notFull.signal();
				logger.debug("Queue switch successfully!");
				return -1;
			}
		} finally {
			writeLock.unlock();
		}
	}

	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		if (e == null) {
			throw new NullPointerException();
		}

		long nanoTime = unit.toNanos(timeout);
		writeLock.lockInterruptibly();
		try {
			for (;;) {
				if (writeCount < writeArray.length) {
					insert(e);
					if (writeCount == 1) {
						awake.signal();
					}
					return true;
				}

				// Time out
				if (nanoTime <= 0) {
					logger.debug("offer wait time out!");
					return false;
				}
				// keep waiting
				try {
					logger.debug("Queue is full, need wait....");
					nanoTime = notFull.awaitNanos(nanoTime);
				} catch (InterruptedException ie) {
					notFull.signal();
					throw ie;
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanoTime = unit.toNanos(timeout);
		readLock.lockInterruptibly();

		try {
			for (;;) {
				if (readCount > 0) {
					return extract();
				}

				if (nanoTime <= 0) {
					logger.debug("poll time out!");
					return null;
				}
				nanoTime = queueSwitch(nanoTime, false);
			}
		} finally {
			readLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Queue#poll()
	 */
	@Override
	public E poll() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Queue#peek()
	 */
	@Override
	public E peek() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
	 */
	@Override
	public boolean offer(E e) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
	 */
	@Override
	public void put(E e) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.BlockingQueue#take()
	 */
	@Override
	public E take() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.BlockingQueue#remainingCapacity()
	 */
	@Override
	public int remainingCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
	 */
	@Override
	public int drainTo(Collection<? super E> c) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection,
	 * int)
	 */
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.AbstractCollection#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.AbstractCollection#size()
	 */
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
}