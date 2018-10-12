/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.util;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * 房间与逻辑服影射关系
 * 
 *
 * @author wu_hc date: 2017年12月12日 下午12:09:43 <br/>
 */
public class RoomLogicUtil {

	/**
	 * 操作三个小时可以理解为失效
	 */
	private static final int CACHE_ALIVE = 3 * 60 * 60 * 1000;

	private static final Map<Integer, Node> cache = Maps.newConcurrentMap();

	public static void append(int roomId, int logicIndex) {
		cache.putIfAbsent(roomId, new Node(logicIndex, System.currentTimeMillis()));
	}

	/**
	 * 
	 * @param roomId
	 * @return
	 */
	public static int getLogicIndexIfExsit(final int roomId) {
		final Node node = cache.get(roomId);
		if (null == node) {
			return -1;
		}
		if ((System.currentTimeMillis() - node.cacheTime) > CACHE_ALIVE) {
			cache.remove(roomId);
			return -1;
		}
		return node.logicIndex;
	}

	static class Node {
		public int logicIndex;
		public long cacheTime;

		public Node(int logicIndex, long cacheTime) {
			this.logicIndex = logicIndex;
			this.cacheTime = cacheTime;
		}
	}
}
