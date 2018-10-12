/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.util;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 临时配特殊牌用
 *
 * @author wu_hc date: 2018/9/13 15:50 <br/>
 */
public final class TestCardUti {

	final static Map<Integer, DebugEntry> entryMap = Maps.newConcurrentMap();

	public static void testCard(int gameTypeIndex, int[] arrays) {
		if (null == arrays) {
			entryMap.remove(gameTypeIndex);
			return;
		}

		entryMap.put(gameTypeIndex, DebugEntry.newEntry(arrays));
	}

	public static DebugEntry cardArray(int gameTypeIndex) {
		return entryMap.get(gameTypeIndex);
	}

	public static class DebugEntry {
		int[] arrays; //debug card
		int head;

		public static DebugEntry newEntry(int[] arrays) {
			DebugEntry entry = new DebugEntry();
			entry.arrays = arrays;
			return entry;
		}

		public int[] getArrays() {
			return arrays;
		}
	}

	private TestCardUti() {
	}
}
