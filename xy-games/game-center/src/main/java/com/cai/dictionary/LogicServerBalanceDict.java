/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.Symbol;
import com.cai.common.domain.LogicServerBalanceModule;
import com.cai.common.util.SpringService;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author wu date: 2017年8月31日 上午9:27:23 <br/>
 */
public final class LogicServerBalanceDict {

	private Logger logger = LoggerFactory.getLogger(LogicServerBalanceDict.class);

	private static final LogicServerBalanceDict M = new LogicServerBalanceDict();

	/**
	 * key:gameid
	 */
	private volatile Map<Integer, LogicBalanceDictWrap> balanceMap = Maps.newConcurrentMap();

	/**
	 * 被特指子游戏专享的服务器，不能被随机出来
	 */
	private volatile Set<Integer> occupyServers = Sets.newConcurrentHashSet();

	public static LogicServerBalanceDict getInstance() {
		return M;
	}

	public void load() {
		List<LogicServerBalanceModule> appBalanceModuleList = SpringService.getBean(PublicService.class).getPublicDAO().getServerBalanceModelList();
		if (null == appBalanceModuleList || appBalanceModuleList.isEmpty()) {
			logger.warn("子游戏指定服务器负载配置为空，请确认!!");
			return;
		}

		Map<Integer, LogicBalanceDictWrap> balanceMap_ = Maps.newConcurrentMap();
		Set<Integer> occupyServers_ = Sets.newConcurrentHashSet();

		appBalanceModuleList.forEach((module) -> {
			if (module.getOpen() == 1) {
				try {
					balanceMap_.put(module.getGameId(), new LogicBalanceDictWrap(module));
				} catch (Exception e) {
					logger.error("serverIndex parse error!!{}", module.getServerIndexs(), e);
					e.printStackTrace();
				}
			}
		});

		// 筛选出已经被独占的服务器
		balanceMap_.forEach((K, V) -> {
			if (V.isOccupy()) {
				occupyServers_.addAll(V.getServerIndexs());
			}
		});

		balanceMap = balanceMap_;
		occupyServers = occupyServers_;
		logger.info("加载字典ServerBalanceDict成功！");
		logger.info("balanceMap:{}", balanceMap);
		logger.info("occupyServers:{}", occupyServers);
	}

	public LogicBalanceDictWrap getDict(int gameId) {
		return balanceMap.get(gameId);
	}

	/**
	 * 获取被独占的服务器列表
	 *
	 * @return
	 */
	public final Set<Integer> getOccupyServers() {
		return Collections.unmodifiableSet(occupyServers);
	}

	/**
	 *
	 */
	public static class LogicBalanceDictWrap {

		private final LogicServerBalanceModule module;

		private final Set<Integer> serverIds;

		public LogicBalanceDictWrap(LogicServerBalanceModule module) {
			this.module = module;
			Set<Integer> serverIdxs = com.cai.common.util.StringUtil.toIntSet(module.getServerIndexs(), Symbol.COMMA);
			if (null == serverIdxs) {
				serverIds = Sets.newHashSet();
			} else {
				serverIds = serverIdxs;
			}
		}

		public boolean isOpen() {
			return module.getOpen() == 1;
		}

		public boolean isOccupy() {
			return module.getOccupy() == 1;
		}

		public Set<Integer> getServerIndexs() {
			return Collections.unmodifiableSet(serverIds);
		}

		public LogicServerBalanceModule getModule() {
			return this.module;
		}

		@Override
		public String toString() {
			return "LogicBalanceDictWrap [module=" + module + ", serverIds=" + serverIds + "]";
		}

	}
}
