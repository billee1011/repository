/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.game;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 *
 * @author wu_hc date: 2018年4月17日 下午3:50:10 <br/>
 */
public abstract class AbstractGameTypeTable {

	private final Map<Integer, Set<Class<? extends AbstractRoom>>> map = Maps.newHashMap();

	public abstract void doMaping();

	public Map<Integer, Set<Class<? extends AbstractRoom>>> getMaping() {
		return map;
	}

	protected final void maping(Integer gameTypeIndex, Class<? extends AbstractRoom> clz) {
		Set<Class<? extends AbstractRoom>> clzzs = map.get(gameTypeIndex);
		if (null == clzzs) {
			clzzs = Sets.newHashSet();
			map.put(gameTypeIndex, clzzs);
		}
		clzzs.add(clz);
	}
}
