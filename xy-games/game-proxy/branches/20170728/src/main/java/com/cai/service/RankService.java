/**
 * 湘ICP备15020076 copyright@2015-2016湖南旗胜网络科技有限公司
 */
package com.cai.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cai.common.define.ERankType;
import com.google.common.collect.Maps;

import protobuf.clazz.Protocol.RankInfoProto;

/**
 * 排行榜，排行榜数据会从center server当前服务器
 *
 * @author wu_hc
 */
public final class RankService {

	/**
	 * 
	 */
	private static final RankService INSTANCE = new RankService();

	/**
	 * 排行榜缓存
	 */
	private final Map<ERankType, List<RankInfoProto>> rankMap = Maps.newConcurrentMap();

	private RankService() {
	}

	/**
	 * 
	 * @return
	 */
	public static RankService getInstance() {
		return RankService.INSTANCE;
	}

	/**
	 * 
	 * @param type
	 * @param ranks
	 */
	public void addOrUpdate(ERankType type, List<RankInfoProto> ranks) {
		rankMap.put(type, ranks);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public List<RankInfoProto> getRankByType(ERankType type) {
		List<RankInfoProto> ranks = rankMap.get(type);
		return null != ranks ? Collections.unmodifiableList(ranks) : Collections.emptyList();
	}
}
