/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ERoomStatus;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.Room;
import com.cai.common.util.FilterUtil;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RuntimeOpt;
import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.concurrent.WorkerLoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.ClubMsgProto.ClubExclusiveGoldProto;
import protobuf.clazz.ClubMsgProto.ClubOperateEventRsp;
import protobuf.clazz.ClubMsgProto.ClubTablePlayerProto;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月30日 下午3:05:08 <br/>
 */
public final class ClubMsgSender {

	/**
	 * 日志
	 */
	protected static final Logger logger = LoggerFactory.getLogger(ClubMsgSender.class);

	/**
	 * 俱乐部同步消息线程
	 */
	private static final DefaultWorkerLoopGroup clubWorker = DefaultWorkerLoopGroup.newGroup("club-work-thread",
			RuntimeOpt.availableProcessors() << 1);

	public static void roomStatusUpdate(ERoomStatus status, final AbstractRoom room) {
		if (room.club_id <= 0 || room.clubInfo.clubDel || room.clubInfo.updateRule) {
			return;
		}

		if (room.getMinPlayerCount() <= 0) {
			logger.warn("游戏[gameId:{},gameTypeIndex:{}] 上报最小开局人数不合理,roomId:{},value:{}", room.getGame_id(), room.getGameTypeIndex(),
					room.getRoom_id(), room.getMinPlayerCount());
		}

		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(status.status());
		builder.setClubId(room.clubInfo.clubId);
		builder.setRuleId(room.clubInfo.ruleId);
		builder.setRoomId(room.getRoom_id());
		builder.setRoomStatus(room._game_status);
		builder.setRoomCurRound(room._cur_round);
		builder.setTableIndex(room.clubInfo.index);
		builder.setRoomSetStatus(room.setsModel.getStatus());
		builder.setMinPlayerCount(room.getMinPlayerCount());
		builder.setClubMatchId(room.clubInfo.matchId);
		builder.setBrandId(room.get_record_id());


		if (status == ERoomStatus.END) {
			fillGameOverScore(room, builder);
		}

		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.L_2_CLUB_ROOM_STATUS, builder).build());
	}

	public static void playerStatusUpdate(ERoomStatus status, final AbstractRoom room, Player player) {
		if (null == player) {
			return;
		}
		if (room.club_id <= 0 || room.clubInfo.clubDel || room.clubInfo.updateRule) {
			return;
		}

		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(status.status());
		builder.setClubId(room.clubInfo.clubId);
		builder.setRuleId(room.clubInfo.ruleId);
		builder.setRoomId(room.getRoom_id());
		builder.setRoomStatus(room._game_status);
		builder.setTableIndex(room.clubInfo.index);
		builder.setClubMatchId(room.clubInfo.matchId);
		builder.setPlayer(ClubMsgSender.newClubTablePlayerBuilder(room, player));
		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.L_2_CLUB_ROOM_STATUS, builder).build());
	}

	/**
	 * 结算快照
	 */
	public static void gameOverSnapshotNotify(final AbstractRoom room) {
		if (room.club_id <= 0 || room.clubInfo.clubDel) {
			return;
		}

		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(ERoomStatus.SNAPSHOT.status());
		builder.setClubId(room.clubInfo.clubId);
		builder.setRuleId(room.clubInfo.ruleId);
		builder.setRoomId(room.getRoom_id());
		builder.setRoomStatus(room._game_status);
		builder.setRoomCurRound(room._cur_round);
		builder.setTableIndex(room.clubInfo.index);
		builder.setRoomSetStatus(room.setsModel.getStatus());
		builder.setBrandId(room.get_record_id());
		builder.setClubMatchId(room.clubInfo.matchId);
		fillGameOverScore(room, builder);
		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.L_2_CLUB_ROOM_STATUS, builder).build());
	}

	/**
	 * 小局快照
	 */
	public static void gameRoundSnapshotNotify(final AbstractRoom room) {
		if (room.club_id <= 0 || room.clubInfo.clubDel) {
			return;
		}

		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(ERoomStatus.ROUND_SNAPSHOT.status());
		builder.setClubId(room.clubInfo.clubId);
		builder.setRoomId(room.getRoom_id());
		builder.setRoomCurRound(room._cur_round);
		builder.setClubMatchId(room.clubInfo.matchId);
		fillGameOverScore(room, builder);
		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.L_2_CLUB_ROOM_STATUS, builder).build());
	}

	/**
	 * 推送房间信息，包含房间的所有玩家[会把状态同步发给客户端]
	 */
	public static void roomPlayerStatusUpdate(ERoomStatus status, final AbstractRoom room) {
		if (room.club_id <= 0 || room.clubInfo.clubDel || room.clubInfo.updateRule) {
			return;
		}

		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(status.status());
		builder.setClubId(room.clubInfo.clubId);
		builder.setRuleId(room.clubInfo.ruleId);
		builder.setRoomId(room.getRoom_id());
		builder.setRoomStatus(room._game_status);
		builder.setRoomCurRound(room._cur_round);
		builder.setTableIndex(room.clubInfo.index);
		builder.setRoomSetStatus(room.setsModel.getStatus());
		builder.setMinPlayerCount(room.getMinPlayerCount());
		fillPlayerInfo(room, builder);
		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.L_2_CLUB_ROOM_STATUS, builder).build());
	}

	private static ClubTablePlayerProto.Builder newClubTablePlayerBuilder(final Room room, final Player player) {
		ClubTablePlayerProto.Builder builder = ClubTablePlayerProto.newBuilder();
		builder.setAccountId(player.getAccount_id());
		builder.setUserName(player.getNick_name());
		builder.setGold(player.getGold());
		builder.setHeadImgUrl(player.getAccount_icon());
		builder.setMoney(player.getMoney());
		builder.setSeatIndex(player.get_seat_index());
		builder.setClubJoinId(player.getClubJoinId());
		if (player.get_seat_index() == -1 || player.get_seat_index() >= room._player_ready.length) {
			builder.setReady(false);
			logger.error("clubMsgSender, player[{}] seat index[{}] out of range!", player.getAccount_id(), player.get_seat_index());
		} else {
			builder.setReady(room._player_ready[player.get_seat_index()] == 1);

			// 座位加强判断--for debug
			Player p = room.get_players()[player.get_seat_index()];
			if (null != p && p.getAccount_id() != player.getAccount_id()) {

				logger.error("clubMsgSender,player[p1:{},p2:{}] seat index error[{},{}]!", p.getAccount_id(), player.getAccount_id(),
						p.get_seat_index(), player.get_seat_index());

				int playerIndex = room.getPlayerIndex(player.getAccount_id());
				if (-1 != playerIndex) {
					builder.setSeatIndex(playerIndex);
				} else {
					logger.error("clubMsgSender,can't find player[{}] in room[{}]", player.getAccount_id(), room.getRoom_id());
				}
			}
		}
		builder.setIp(player.getAccount_ip());
		return builder;
	}

	/**
	 * 比较特殊，只有俱乐部解散需要
	 * @param category see EClubOperateCategory
	 */
	public static void notifyUpdateRule(final Player player, final int clubId, final int ruleId, int category) {
		ClubOperateEventRsp.Builder updateBuilder = ClubOperateEventRsp.newBuilder().setClubId(clubId).setRuleId(ruleId).setCategory(category);
		PlayerServiceImpl.getInstance().sendExMsg(player, S2CCmd.CLUB_RULE_UPDATE, updateBuilder);
	}

	/**
	 * 把当前的房间状态同步到俱乐部服
	 */
	public static void syncClubRoomStatusTOClubServer() {
		Map<Integer, AbstractRoom> rooms = PlayerServiceImpl.getInstance().getRoomMap();

		// 满足需要同步的条件
		Predicate<AbstractRoom> predicate = (room) -> {
			return !(room.club_id <= 0 || room.clubInfo.clubDel || room.clubInfo.updateRule);
		};

		Collection<AbstractRoom> allClubRooms = FilterUtil.filter(rooms.values(), predicate);
		if (allClubRooms.isEmpty()) {
			return;
		}

		final AtomicInteger timer = new AtomicInteger();
		allClubRooms.forEach((room) -> {
			try {
				GlobalExecutor.schedule(() -> {
					syncAppointClubRoomStatusTOClubServer(room.getRoom_id(), room.club_id);
				}, timer.addAndGet(20));

			} catch (Exception e) { // 保护
				e.printStackTrace();
			}
		});
	}

	/**
	 * 推送指定房间数据到俱乐部，不会把状态实时同步
	 */
	public static void syncAppointClubRoomStatusTOClubServer(int roomId, int exceptClubId) {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
		if (null == room || room.club_id == 0 || room.clubInfo.clubDel || room.clubInfo.updateRule || room.clubInfo.matchId > 0) {
			return;
		}
		if (exceptClubId != room.club_id) {
			logger.error("clubMsgSender,syncAppointClubRoomStatusTOClubServer exceptClubId[{}] != roomClubId[{}]", exceptClubId, room.club_id);
			return;
		}
		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(ERoomStatus.L_2_CLUB_ROOM_SYNC.status());
		builder.setClubId(room.clubInfo.clubId);
		builder.setRuleId(room.clubInfo.ruleId);
		builder.setRoomId(room.getRoom_id());
		builder.setRoomStatus(room._game_status);
		builder.setRoomCurRound(room._cur_round);
		builder.setTableIndex(room.clubInfo.index);
		builder.setRoomSetStatus(room.setsModel.getStatus());
		builder.setMinPlayerCount(room.getMinPlayerCount());
		builder.setBrandId(room.get_record_id());
		fillPlayerInfo(room, builder);
		SessionServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.L_2_CLUB_ROOM_STATUS, builder).build());
	}

	/**
	 * 通知客户端刷新专属豆
	 */
	public static void sendExclusiveGoldUpdate(long accountId, List<CommonILI> exclusiveGolds) {
		if (accountId <= 0 || null == exclusiveGolds || exclusiveGolds.isEmpty()) {
			return;
		}

		ClubExclusiveGoldProto.Builder builder = ClubExclusiveGoldProto.newBuilder();
		builder.setAccountId(accountId);
		builder.addAllExclusive(exclusiveGolds);
		SessionServiceImpl.getInstance().sendGate(PBUtil.toS_S2CRequet(accountId, S2CCmd.CLUB_EXCLUSIVE_GOLD_INFO, builder).build());
	}

	/**
	 * 填充结算分数
	 */
	private static void fillGameOverScore(final Room room, ClubRoomStatusProto.Builder builder) {
		final PlayerResult playerResult = room._player_result;
		if (null != playerResult) {
			for (int i = 0; i < room.getTablePlayerNumber(); i++) {
				Player player = room.get_players()[i];
				if (null == player) {
					continue;
				}
				ClubTablePlayerProto.Builder playerBuilder = newClubTablePlayerBuilder(room, player);
				playerBuilder.setScore(playerResult.game_score[i]);
				builder.addPlayers(playerBuilder);
			}
		}
	}

	/**
	 * 填充玩家数据
	 */
	private static void fillPlayerInfo(final Room room, ClubRoomStatusProto.Builder builder) {
		for (int i = 0; i < room.getTablePlayerNumber(); i++) {
			Player player = room.get_players()[i];
			if (null == player) {
				continue;
			}
			ClubTablePlayerProto.Builder playerBuilder = newClubTablePlayerBuilder(room, player);
			builder.addPlayers(playerBuilder);
		}
	}

	public static WorkerLoop worker(int clubId) {
		return clubWorker.next(clubId);
	}

	private ClubMsgSender() {
	}
}
