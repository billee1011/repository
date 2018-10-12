/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.cai.common.ClubMemWelfareLotteryInfo;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.constant.ClubSeat;
import com.cai.constant.ServiceOrder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;

import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;

/**
 * @author wu_hc date: 2018年1月5日 上午10:19:09 <br/>
 */
@IService(order = ServiceOrder.CLUB_PLAYER_SEAT, desc = "俱乐部部分缓存维护")
public final class ClubCacheService extends AbstractService {

	/**
	 * 玩家坐标
	 */
	private final Map<Long, ClubSeat> seats = Maps.newConcurrentMap();

	/**
	 * 存储俱乐部创建者信息，不要求立即更新
	 */
	private final Map<Long, LogicRoomAccountItemRequest.Builder> clubOwnerProtos = Maps.newConcurrentMap();

	/**
	 * 存储俱乐部玩家信息，不要求立即更新
	 */
	private final Map<Long, LogicRoomAccountItemRequest.Builder> clubMemberProtos = Maps.newConcurrentMap();

	/**
	 * 玩家所在的俱乐部id
	 */
	private final Map<Long, Set<Integer>> memberClubIds = Maps.newConcurrentMap();

	public Map<Long, Map<Long, Integer>> memOngoingMatchs = new ConcurrentHashMap<>();

	public Map<Long, ClubMemWelfareLotteryInfo> lotteryMembers = new ConcurrentHashMap<>();

	/**
	 */
	private final LoadingCache<Long, Boolean> visitCache = CacheBuilder.newBuilder().maximumSize(2000000L).expireAfterWrite(2, TimeUnit.SECONDS)
			.build(new CacheLoader<Long, Boolean>() {
				@Override
				public Boolean load(Long key) throws Exception {
					return null;
				}
			});

	private static final ClubCacheService m = new ClubCacheService();

	public static final ClubCacheService getInstance() {
		return m;
	}

	/**
	 * 入席，如果之前有位置，坐下失败，并返回当前的位置信息
	 *
	 * @param accountId
	 * @param clubId
	 * @param ruleId
	 * @param joinId
	 * @return
	 */
	public Optional<ClubSeat> sit(long accountId, int clubId, int ruleId, int joinId) {
		return Optional.ofNullable(seats.put(accountId, ClubSeat.newSeat(clubId, ruleId, joinId)));
	}

	/**
	 * @param accountId
	 * @param seat
	 * @return
	 */
	public Optional<ClubSeat> sit(long accountId, ClubSeat seat) {
		return Optional.ofNullable(seats.put(accountId, seat));
	}

	/**
	 * 离席
	 *
	 * @param accountId
	 */
	public void out(long accountId) {
		seats.remove(accountId);
	}

	/**
	 * 当前席位
	 *
	 * @param accountId
	 * @return
	 */
	public Optional<ClubSeat> seat(long accountId) {
		return Optional.ofNullable(seats.get(accountId));
	}

	/**
	 * 外部保证调用安全
	 *
	 * @param accountId
	 * @return
	 */
	public Optional<LogicRoomAccountItemRequest.Builder> ownerPB(long accountId) {
		LogicRoomAccountItemRequest.Builder builder = clubOwnerProtos.get(accountId);
		if (null == builder) {
			builder = LogicRoomAccountItemRequest.newBuilder();
			PlayerViewVO vo = SpringService.getBean(ICenterRMIServer.class).getPlayerViewVo(accountId);
			if (null != vo) {
				builder = LogicRoomAccountItemRequest.newBuilder();
				builder.setAccountId(accountId);
				builder.setAccountIcon(vo.getHead());
				builder.setNickName(vo.getNickName());
				builder.setGold(vo.getGold());
				builder.setMoney(vo.getMoney());
				builder.setClubOwner(accountId);
				builder.setSex(vo.getSex());
				clubOwnerProtos.put(accountId, builder);
			}
		}
		return Optional.of(builder);
	}

	/**
	 *
	 */
	public Optional<LogicRoomAccountItemRequest.Builder> memberPB(long accountId) {
		LogicRoomAccountItemRequest.Builder builder = clubMemberProtos.get(accountId);
		if (null == builder) {
			builder = LogicRoomAccountItemRequest.newBuilder();
			PlayerViewVO vo = SpringService.getBean(ICenterRMIServer.class).getPlayerViewVo(accountId);
			if (null != vo) {
				builder = LogicRoomAccountItemRequest.newBuilder();
				builder.setAccountId(accountId);
				builder.setAccountIcon(vo.getHead());
				builder.setNickName(vo.getNickName());
				builder.setGold(vo.getGold());
				builder.setMoney(vo.getMoney());
				builder.setClubOwner(accountId);
				builder.setProxySessionId(accountId);
				builder.setSex(vo.getSex());
				clubMemberProtos.put(accountId, builder);
			}
		}
		return Optional.of(builder);
	}

	/**
	 * 玩家所在的俱乐部id
	 *
	 * @param accountId
	 * @return
	 */
	public Optional<Set<Integer>> optMemberClubs(long accountId) {
		return Optional.ofNullable(memberClubIds.get(accountId));
	}

	public void addMemberClubId(long accountId, int clubId) {
		Optional<Set<Integer>> opt = optMemberClubs(accountId);
		if (opt.isPresent()) {
			opt.get().add(clubId);
		} else {
			memberClubIds.put(accountId, Sets.newHashSet(clubId));
		}
	}

	public void rmMemberClubId(long accountId, int clubId) {
		Optional<Set<Integer>> opt = optMemberClubs(accountId);
		if (opt.isPresent()) {
			opt.get().remove(clubId);
		}
	}

	/**
	 * @param accountId
	 * @return
	 */
	public boolean isBlockVisit(long accountId) {
		Boolean value = visitCache.getIfPresent(accountId);
		if (null == value) {
			visitCache.put(accountId, Boolean.TRUE);
			return false;
		}
		return true;
	}

	@Override
	public void start() throws Exception {
	}

	@Override
	public void stop() throws Exception {

	}

	public void updateMemberOngoingClubMatch(long targetId, long matchId, int roomId, int opType) {
		Map<Long, Integer> map = memOngoingMatchs.get(targetId);
		if (opType == 1) { // add
			if (map == null) {
				map = new HashMap<>();
				memOngoingMatchs.put(targetId, map);
			}
			map.put(matchId, roomId);
		} else if (opType == 0) { // remove
			if (map != null) {
				map.remove(matchId);
			}
		}
	}

	public void addMemWelfareLotteryInfo(long account_id, int lotteryCost, int endTime, int clubId, int gameTypeIndex) {
		ClubMemWelfareLotteryInfo lotteryInfo = new ClubMemWelfareLotteryInfo();
		lotteryInfo.setAccountId(account_id);
		lotteryInfo.setCost(lotteryCost);
		lotteryInfo.setEndTime(endTime);
		lotteryInfo.setClubId(clubId);
		lotteryInfo.setGameTypeIndex(gameTypeIndex);
		lotteryMembers.put(account_id, lotteryInfo);
	}

	public void removeWelfareLotteryMember(long account_id) {
		lotteryMembers.remove(account_id);
	}

	public static void main(String[] args) {
		System.out.println(ClubCacheService.getInstance().isBlockVisit(111L));
		System.out.println(ClubCacheService.getInstance().isBlockVisit(111L));
	}

}
