/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ERoomSettingStatus;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.EServerType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubRoomRedisModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.StatusModule;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RedisKeyUtil;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.ClubMsgProto.ClubTablePlayerProto;
import protobuf.clazz.ClubMsgProto.ClubTableProto;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;

/**
 * 俱乐部牌桌
 *
 * @author wu_hc date: 2017年10月30日 下午5:12:24 <br/>
 */
public final class ClubTable {

	private static final Logger logger = LoggerFactory.getLogger(ClubTable.class);

	/**
	 * 俱乐部id
	 */
	private final int clubId;

	/**
	 * 所属规则id
	 */
	private final int ruleId;

	/**
	 * 桌子索引
	 */
	private final int index;

	/**
	 * 桌子内玩家数据
	 */
	private final ConcurrentMap<Long, ClubPlayer> players = Maps.newConcurrentMap();

	/**
	 * 当前局数
	 */
	private int curRound;

	/**
	 * 房间id
	 */
	private int roomId;

	/**
	 * 牌桌所在逻辑服
	 */
	private int logicIndex;

	private StatusModule setsModel;

	/**
	 * 最小开局人数
	 */
	private int minPlayerCount;

	/**
	 * 默认准备的房间-兼容子游戏特性，如FLS
	 */
	private boolean isDefaultReady;

	/**
	 * 是否在显示中，隐藏桌子设置时字段生效
	 */
	private boolean isShow;

	// 当前场景的位置
	private final ClubSeat sceneTag;

	/**
	 * 牌桌密码
	 */
	private int passport;

	private long brandId;

	public ClubTable(int clubId, int ruleId, int index) {
		this.clubId = clubId;
		this.ruleId = ruleId;
		this.index = index;
		sceneTag = ClubSeat.newSeat(clubId, ruleId, index << 16 & 0xffff0000);
	}

	public ClubTable(int clubId, int ruleId, int index, int passport) {
		this.clubId = clubId;
		this.ruleId = ruleId;
		this.index = index;
		sceneTag = ClubSeat.newSeat(clubId, ruleId, index << 16 & 0xffff0000);
		this.passport = passport;
	}

	public int getIndex() {
		return index;
	}

	public Map<Long, ClubPlayer> getPlayers() {
		return players;
	}

	public ClubPlayer playerEnter(final ClubTablePlayerProto playerProto) {
		ClubPlayer player = ClubPlayer.create(playerProto);
		players.put(playerProto.getAccountId(), player);
		ClubCacheService.getInstance().sit(playerProto.getAccountId(), clubId, ruleId, playerProto.getClubJoinId());

		Club club = ClubService.getInstance().clubs.get(this.clubId);
		if (club != null) {
			ClubMemberModel memberModel = club.members.get(playerProto.getAccountId());
			if (memberModel != null) {
				memberModel.setLastEnterTableTime(System.currentTimeMillis());
			}
		}
		return player;
	}

	public ClubPlayer playerExit(long account_id) {
		ClubPlayer player = players.remove(account_id);
		// if (null != player) {
		// ClubCacheService.getInstance().out(account_id);
		// }
		return player;
	}

	public ClubPlayer playerReady(long account_id, boolean isReady) {
		ClubPlayer player = players.get(account_id);
		if (null != player) {
			player.setReady(isReady);
		}
		return player;
	}

	public ClubPlayer getPlayer(long account) {
		return players.get(account);
	}

	public ClubPlayer getPlayerBySeat(int seat) {
		for (Map.Entry<Long, ClubPlayer> entry : players.entrySet()) {
			if (entry.getValue().getSeatIndex() == seat) {
				return entry.getValue();
			}
		}
		return null;
	}

	public int playerSize() {
		return players.size();
	}

	public Set<Long> playerIds() {
		return players.keySet();
	}

	/**
	 * 牌桌结束
	 */
	public void resetTable() {
		playerIds().forEach(accountId -> {
			playerExit(accountId);
			if (!PlayerService.getInstance().isPlayerOnline(accountId)) {
				ClubCacheService.getInstance().sit(accountId, ClubService.currentSeat);
			}
		});

		players.clear();
		roomId = 0;
		curRound = 0;
		minPlayerCount = 0;
		logicIndex = -1;
	}

	public int getCurRound() {
		return curRound;
	}

	public void setCurRound(int curRound) {
		this.curRound = curRound;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public int getLogicIndex() {
		return logicIndex;
	}

	public void setLogicIndex(int logicIndex) {
		this.logicIndex = logicIndex;
	}

	public StatusModule getSetsModel() {
		return setsModel;
	}

	public void setSetsModel(StatusModule setsModel) {
		this.setsModel = setsModel;
	}

	public boolean hasSets(ERoomSettingStatus status) {
		return null != setsModel && setsModel.isStatusTrue(status);
	}

	public void nextRound() {
		// for debug
		if (isGameStart() && (0 < minPlayerCount && playerSize() < minPlayerCount)) {
			logger.error("俱乐部调试日志:[clubId:{},ruleId:{},index:{}] room:{} curRound:{} players:{},minP:{}", clubId, ruleId, index, roomId, curRound,
					players.keySet(), minPlayerCount);

			// 自动尝试修复,第一局主动刷新
			if (curRound == 2 || curRound == 3) {
				reqLogicSyncRoomStatus();
			}
		}
	}

	public boolean isGameStart() {
		return roomId > 0 && curRound > 0;
	}

	public void release() {
		if (this.roomId > 0) {
			SpringService.getBean(ICenterRMIServer.class).delRoomById(roomId, Club.RELESE_TIP);
		}
		// this.resetTable();//在真正释放房间的时候再清缓存
	}

	public void delTableCache() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hDel(RedisKeyUtil.clubRoomKey(clubId, ruleId), Integer.toString(index));
	}

	public int getPlayerSize() {
		return players.size();
	}

	public void replacePlayerInfo(final ClubTablePlayerProto newPb) {
		players.put(newPb.getAccountId(), ClubPlayer.create(newPb));
	}

	public boolean hasSameIP(final String ip) {
		if (StringUtils.isEmpty(ip)) {
			return false;
		}
		for (Map.Entry<Long, ClubPlayer> entry : players.entrySet()) {
			if (ip.equals(entry.getValue().getIp())) {
				return true;
			}
		}
		return false;
	}

	public boolean isInRoom(final long accountId) {
		return players.containsKey(accountId);
	}

	/**
	 * 已经准备的玩家数量
	 */
	public int readyCount() {
		int count = 0;
		for (Map.Entry<Long, ClubPlayer> entry : players.entrySet()) {
			count += (entry.getValue().isReady() ? 1 : 0);
		}
		return count;
	}

	public int getClubId() {
		return clubId;
	}

	public int getRuleId() {
		return ruleId;
	}

	public int getMinPlayerCount() {
		return minPlayerCount;
	}

	public void setMinPlayerCount(int minPlayerCount) {
		this.minPlayerCount = minPlayerCount;
	}

	public boolean isDefaultReady() {
		return isDefaultReady;
	}

	public void setDefaultReady(boolean isDefaultReady) {
		this.isDefaultReady = isDefaultReady;
	}

	public boolean isShow() {
		return isShow;
	}

	public void setShow(boolean isShow) {
		this.isShow = isShow;
	}

	public ClubTableProto.Builder toClubTableBuilder() {
		return toClubTableBuilder(ClubPlayer.OP_ALL);
	}

	public ClubTableProto.Builder toClubTableBuilder(int attrInterestOps) {
		ClubTableProto.Builder builder = ClubTableProto.newBuilder();
		builder.setIndex(index);
		builder.setCurRound(curRound);
		builder.setRoomId(roomId);
		builder.setMinPlayerCount(minPlayerCount);
		builder.setIsDefaultReady(isDefaultReady);
		if (null != setsModel) {
			builder.setAllowJoinHalfWay(setsModel.isStatusTrue(ERoomSettingStatus.ROOM_FORBID_HALF_WAY_ENTER) ? 1 : 0);
		}
		fillPlayerPB(builder, attrInterestOps);
		builder.setIsShow(isShow);
		builder.setIsHavePassport(passport > 0);

		return builder;
	}

	/**
	 * 填充玩家数据
	 *
	 * @param builder
	 * @param attrInterestOps 感兴趣属性
	 */
	private void fillPlayerPB(ClubTableProto.Builder builder, int attrInterestOps) {
		players.forEach((accountId, player) -> {
			builder.addPlayers(player.toInteresPbBuilder(attrInterestOps));
		});
	}

	public ClubSeat sceneTag() {
		return sceneTag;
	}

	/**
	 * 主动同步请求房间数据
	 */
	public final void reqLogicSyncRoomStatus() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		ClubRoomRedisModel room = redisService
				.hGet(RedisKeyUtil.clubRoomKey(getClubId(), getRuleId()), Integer.toString(index), ClubRoomRedisModel.class);
		if (null != room) {
			RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, room.getRoom_id() + "", RoomRedisModel.class);
			if (roomRedisModel != null) {
				ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
				builder.setType(ERoomStatus.TABLE_REFRESH.status());
				builder.setRoomId(room.getRoom_id());
				builder.setClubId(getClubId());
				SessionService.getInstance()
						.sendMsg(EServerType.LOGIC, roomRedisModel.getLogic_index(), PBUtil.toS2SResponse(S2SCmd.C_2_LOGIC_ROOM_STATUS, builder));
			}
		}

	}

	public int getPassport() {
		return passport;
	}

	public void setPassport(int passport) {
		this.passport = passport;
	}

	public long getBrandId() {
		return brandId;
	}

	public void setBrandId(long brandId) {
		this.brandId = brandId;
	}
}
