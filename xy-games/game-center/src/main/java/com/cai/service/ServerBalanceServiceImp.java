/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import com.cai.common.domain.Event;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.util.ServerRandomUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.LogicServerBalanceDict;
import com.cai.dictionary.LogicServerBalanceDict.LogicBalanceDictWrap;
import com.cai.dictionary.ServerDict;
import com.cai.domain.Session;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.primitives.Ints;

/**
 * 
 *
 * @author wu_hc date: 2017年8月15日 下午2:52:59 <br/>
 */
public class ServerBalanceServiceImp extends AbstractService {

	/**
	 * 
	 */
	private static final ServerBalanceServiceImp M = new ServerBalanceServiceImp();

	@Override
	protected void startService() {
	}

	/**
	 * 
	 * @return
	 */
	public static ServerBalanceServiceImp getInstance() {
		return M;
	}

	/**
	 * 分配逻辑服id
	 * 
	 * @param gameId
	 * @return
	 */
	public int allotLogicId(int gameId) {
		try {
			List<LogicGameServerModel> logicList = ServerDict.getInstance().getActiveLogicList();
			if (logicList.isEmpty()) {
				logger.error("#########没有可以用于分配的活跃服务器了，请区确认!!!!!##########");
				return 9;
			}
			LogicBalanceDictWrap dictWrap = LogicServerBalanceDict.getInstance().getDict(gameId);

			if (null == dictWrap || dictWrap.getServerIndexs().isEmpty()) {
				return /* SystemConfig.gameDebug == 1 ? 1 : */ randomLogicId(logicList);
			} else {
				int serverIndex = appointLogicId(logicList, gameId, dictWrap.getServerIndexs());
				if (serverIndex == -1) {// 是否需要走随机，还是直接返回-1??
					if (SystemConfig.gameDebug == 1) {
						return -1;
					}
					return randomLogicId(logicList);
				} else {
					return serverIndex;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("error", e);
		}
		return -1;
	}

	/**
	 * 随机分配，需要过滤掉那些被子游戏独占的服务器
	 * 
	 * @param activeServers
	 * @return
	 */
	public int randomLogicId(final List<LogicGameServerModel> activeServers) {
		Set<Integer> occupyServers = LogicServerBalanceDict.getInstance().getOccupyServers();
		if (null != occupyServers && !occupyServers.isEmpty()) { // 1有被独占的服务器，需要过滤掉被独占的服务器
			Set<Integer> activeIdxSet = Sets.newHashSet();
			activeServers.forEach((s) -> {
				activeIdxSet.add(s.getLogic_game_id());
			});

			return ServerRandomUtil.eliminateAndRandom(activeIdxSet, occupyServers);
		} else { // 2没有被独占的服务器，直接全部随机
			int index = ServerRandomUtil.getRandomNumber(activeServers.size());
			LogicGameServerModel logicGameServerModel = activeServers.get(index);
			return logicGameServerModel.getLogic_game_id();
		}
	}

	/**
	 * 指定分配
	 * 
	 * @param activeServers
	 * @param appointSet
	 * @return
	 */
	public int appointLogicId(final List<LogicGameServerModel> activeServers, int gameId, final Set<Integer> appointSet) {
		Set<Integer> activeSvrIds = Sets.newHashSet();
		activeServers.forEach((e) -> {
			activeSvrIds.add(e.getLogic_game_id());
		});
		SetView<Integer> unionSet = Sets.intersection(activeSvrIds, appointSet);
		if (null == unionSet || unionSet.isEmpty()) {
			logger.error("############[{}] 指定分配逻辑服，但没有找到合适的逻辑服 ,检查是否指定的服务器没有启动##############", gameId);
			logger.error("activeServers:{}, appointSet:{}", activeSvrIds, appointSet);
			return -1; // randomLogicId(activeServers); // GAME-TODO
						// 是否需要走随机，还是直接返回-1??
		} else {
			int[] actives = Ints.toArray(unionSet);
			return actives[ServerRandomUtil.getRandomNumber(actives.length)];
		}
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
	}

	@Override
	public void sessionCreate(Session session) {
	}

	@Override
	public void sessionFree(Session session) {
	}

	@Override
	public void dbUpdate(int _userID) {
	}

	public static void main(String[] args) {
		Set<Integer> xx = Sets.newHashSet();
		xx.add(23);
		xx.add(56);
		xx.add(67);
		xx.add(3);
		int[] yy = Ints.toArray(xx);
		System.out.println(Arrays.toString(yy));
		System.out.println(yy[ServerRandomUtil.getRandomNumber(yy.length)]);

		Set<Integer> s1 = Sets.newHashSet(1, 3, 5, 7);
		Set<Integer> s2 = Sets.newHashSet(2, 3, 4, 7);
		SetView<Integer> s3 = Sets.intersection(s1, s2);
		System.out.println(s3);
	}
}
