package com.lingyu.common.id;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.ServerInfo;

/**
 * 生成唯一id的服务 time为服务器第n次重启，seqId 在重启后会重新清零累加 前52位给需持久化的ID使用，52位~53位 给非持久化的使用 AS3
 * number 整形精度可以达到53位 +---------------------------------------------+
 * |-worldId-|--times-|---objectType---|--seqId-|
 * |-16bits--|10bits--|-------4bits----|-22bits-|
 * +---------------------------------------------+
 * 
 * seqId最多可达4194303 ，objectType 可达15 ，times可达1023,
 * serverId可达65535，如果达到MAX_SEQ_ID的话，times人为+1
 * 
 * @author Allen Jiang
 */
public class IdFactory {
	private static final Logger logger = LogManager.getLogger(IdFactory.class);

	private static final long MAX_ID = 0xFFFFFFFFFFFFFl;
	private static final long MAX_SEQ_ID = 0x3FFFFF;
	private static final long MAX_SERVER_ID = 0xFFFFl;
	private static final long MAX_TIMES = 0x3FF;
	private static final long MAX_TYPE = 0xF;
	private Map<Byte, AtomicInteger> map = new ConcurrentHashMap<Byte, AtomicInteger>();
	private static AtomicLong sequence = new AtomicLong(1); // ConcurrentHashMap?

	private int worldId;
	private int times;// 服务器重启次数
	private static long ID_PREFIX;// id前缀
	private static final IdFactory INSTANCE = new IdFactory();
	private final ReentrantLock lock = new ReentrantLock();
	private ServerInfo info;

	public static IdFactory getInstance() {
		return INSTANCE;
	}

	public void init(ServerInfo info) throws ServiceException {
		this.info = info;
		this.init(info.getId(), info.getTimes());
	}

	private void init(int worldId, int times) throws ServiceException {
		logger.info("IdFactory初始化 开始", worldId);
		if (worldId > MAX_SERVER_ID) {
			throw new ServiceException(String.format("serverId 超出最大值: %d", worldId));
		}
		if (times > MAX_TIMES) {
			throw new ServiceException(String.format("times 超出最大值: %d", times));
		}

		this.worldId = worldId;
		this.times = times;
		ID_PREFIX = this.buildPrefix();
		int num = this.register();
		logger.info("IdFactory初始化 完成，共有{}个模块ID", num);
	}

	private int register() {
		int ret = 0;
		for (Field field : ServerObjectType.class.getDeclaredFields()) {
			try {
				byte type = field.getByte(null);
				if (type > MAX_TYPE) {
					throw new ServiceException(String.format("type 超出最大值: %d", type));
				}
				map.put(type, new AtomicInteger(1));
				ret++;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ServiceException(e.getMessage(), e);
			}

		}
		return ret;
	}

	private long buildPrefix() {
		return ((long) worldId << 36) | ((long) times << 26);
	}

	/** 获取某种类型当前的ID值 */
	public int getCurrentSeqId(byte objectType) {
		return map.get(objectType).get();
	}

	/** 根据模块ID,生成ID */
	public long generateId(byte objectType) {
		AtomicInteger sequence = new AtomicInteger(1);
		int seqId = sequence.getAndIncrement();
		long id = ID_PREFIX | ((long) objectType << 22) | seqId;
		// logger.debug("objectType={},seqId={},id={}", objectType, seqId,
		// Long.toHexString(id));
		if (seqId >= MAX_SEQ_ID) {
			lock.lock();
			logger.warn("超过流水ID最大值 objectType={},times={},seqId={}", objectType, times, seqId);
			// 防止其他线程进入时再次修改这个值
			sequence = new AtomicInteger(1);
			seqId = sequence.get();
			id = ID_PREFIX | ((long) objectType << 22) | seqId;
			try {
				if (seqId >= MAX_SEQ_ID) {
					times++;
					ID_PREFIX = this.buildPrefix();
					info.updateTimes(times);
					this.register();
				}
			} finally {
				lock.unlock();
			}
		}
		return id;
	}

	/**
	 * 此ID不会持久化，重启的话就从0开始，用于怪物，陷阱，尸体,开关,副本机关等 可用长度为0x10000000000000 (十进制
	 * 为4503599627370496 服务器从启动到停机一次够用了)[下限 :0xFFFFFFFFFFFFF- 上限
	 * :0x1FFFFFFFFFFFFF]
	 */
	public long generateNonPersistentId() {
		return MAX_ID + sequence.getAndIncrement();
	}

	private static AtomicLong global = new AtomicLong(0x1FFFFFFFFFFFFFl); // ConcurrentHashMap?

	public long generate4GlobalId() {
		return global.getAndDecrement();
		// return Math.max(0xFFFFFFFFFFFFF, MAX_ID -
		// sequence.getAndIncrement());
	}

	/*** 专用于公共场景的ID生成，mapID要小于100W，做这个ID是为了通过mapid和line能反向找到这个ID */
	public long createStageId(int mapId, int line) {
		return ((long) mapId << 32) | line;
	}

	public static void main(String[] args) throws ServiceException {

		long value = ((long) 5 << 36) | ((long) 20 << 26) | ((long) 0 << 22) | 10;

		System.out.println(value);

		// long start = System.nanoTime();
		// for (int i = 1; i < 100; i++) {
		// // byte type=(byte)(RandomUtils.nextInt(4)+1);
		// IdFactory.getInstance().generateId((byte) 2);
		// }
		// System.out.println(IdFactory.getInstance().generate4GlobalId());
		// System.out.println(IdFactory.getInstance().generate4GlobalId());
		// System.out.println(IdFactory.getInstance().generate4GlobalId());
		// System.out.println(IdFactory.getInstance().generate4GlobalId());
		// System.out.println(IdFactory.getInstance().generate4GlobalId());
	}
}
