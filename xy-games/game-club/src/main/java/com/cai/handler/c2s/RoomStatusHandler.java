/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.EServerType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.StatusModule;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.ClubPlayer;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubTable;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.cai.utils.Utils;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.ClubCommonLIIProto;
import protobuf.clazz.ClubMsgProto.ClubGameOverSnapshotProto;
import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;
import protobuf.clazz.ClubMsgProto.ClubTablePlayerProto;
import protobuf.clazz.ClubMsgProto.ClubTableProto;
import protobuf.clazz.ClubMsgProto.ClubTableStatusUpdateRsp;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;

/**
 * @author wu_hc date: 2017年10月27日 下午4:32:13 <br/>
 */
@ICmd(code = S2SCmd.L_2_CLUB_ROOM_STATUS, desc = "游戏房间状态同步")
public class RoomStatusHandler extends IClientHandler<ClubRoomStatusProto> {

	@Override
	protected void execute(ClubRoomStatusProto req, C2SSession session) throws Exception {
		ERoomStatus status = ERoomStatus.of(req.getType());
		if (ERoomStatus.NONE == status) {
			return;
		}

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			logger.error("1俱乐部[{}]规则id[{}] 不存在,type:{},roomId:{}!", req.getClubId(), req.getRuleId(), status.status(), req.getRoomId());
			return;
		}

		if (ClubCfg.get().isUseOwnThreadSyncRoomStatus()) {
			club.runInClubLoop(new ClubRoomUpateInvoke(req, session, status, club, this));
		} else {
			club.runInReqLoop(new ClubRoomUpateInvoke(req, session, status, club, this));
		}

	}

	// 执行器
	final static class ClubRoomUpateInvoke implements Runnable {
		private final ClubRoomStatusProto req;
		private final C2SSession session;
		private final ERoomStatus status;
		private final Club club;
		private final RoomStatusHandler handler;

		public ClubRoomUpateInvoke(ClubRoomStatusProto req, C2SSession session, ERoomStatus status, Club club, RoomStatusHandler handler) {
			this.req = req;
			this.session = session;
			this.status = status;
			this.club = club;
			this.handler = handler;
		}

		@Override
		public void run() {

			if (req.getClubMatchId() > 0) {
				if (status != ERoomStatus.END && status != ERoomStatus.ROUND_SNAPSHOT && status != ERoomStatus.START) {
					return;
				}
			} else {
				if (status != ERoomStatus.SNAPSHOT) {
					ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
					if (null == ruleTables) {
						handler.logger
								.error("2俱乐部[{}]规则id[{}] 不存在,type:{},roomId:{}!", req.getClubId(), req.getRuleId(), status.status(), req.getRoomId());
						return;
					}
					ClubTable table = ruleTables.getTable(req.getTableIndex());
					if (table != null && req.getBrandId() > 0) {
						table.setBrandId(req.getBrandId());
					}
				}
			}

			switch (status) {
			case CREATE:
				handler.roomCreateAndEnter(req, club, session);
				break;
			case PLAYER_ENTER:
				handler.playerEnter(req, club);
				break;
			case PLAYER_EXIT:
				handler.playerExit(req, club);
				break;
			case START:
				handler.gameStart(req, club);
				break;
			case END:
				handler.gameEnd(req, club);
				break;
			case NEXT:
				handler.gameNext(req, club);
				break;
			case PLAYER_READY:
				handler.playerReady(req, club);
				break;
			case L_2_CLUB_ROOM_SYNC:
				handler.clubRoomStatusSync(req, club, session);
				break;
			case SNAPSHOT:
				handler.gameOverSnapshot(req, club);
				break;
			case TABLE_REFRESH:
				handler.tableRefresh(req, club, session);
				break;
			case TABLE_MIN_PLAYER_COUNT:
				handler.tableMinPlayerCount(req, club);
				break;
			case ROUND_SNAPSHOT:
				handler.roundSnapshot(req, club);
			default:
				break;
			}
		}

	}

	private void roundSnapshot(ClubRoomStatusProto req, Club club) {
		ClubMatchWrap matchWrap = club.matchs.get(req.getClubMatchId());
		if (null != matchWrap) {
			req.getPlayersList().forEach(pb -> {
				matchWrap.updateScore(pb.getAccountId(), (int) (pb.getScore()), false, true);
			});
		}
	}

	private void tableMinPlayerCount(ClubRoomStatusProto req, Club club) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		if (ClubCfg.get().isCheckRoomId() && table.getRoomId() != req.getRoomId()) {
			return;
		}

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());
		builder.setMinPlayerCount(req.getMinPlayerCount());
		// 之需要告知牌桌内玩家
		table.playerIds().forEach((accountId) -> {
			SessionService.getInstance().sendClient(accountId, S2CCmd.CLUB_GAME_START_NOTIFY, builder);
		});
	}

	private void playerReady(ClubRoomStatusProto req, Club club) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		if (ClubCfg.get().isCheckRoomId() && table.getRoomId() != req.getRoomId()) {
			return;
		}

		ClubPlayer player = table.playerReady(req.getPlayer().getAccountId(), req.getPlayer().getReady());
		if (null == player) {
			return;
		}

		ClubTablePlayerProto playerPb = req.getPlayer();

		player.setReady(req.getPlayer().getReady());
		player.setSeatIndex(playerPb.getSeatIndex());

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());
		builder.setPlayer(playerPb);

		if (club.hasSetting(EClubSettingStatus.CONCEAL_TABLE) && table.isGameStart()) {
			notifyClubStatus(builder, club, club.getManagerIds());
		} else {
			notifyClubStatus(builder, club);
		}

	}

	private void gameNext(ClubRoomStatusProto req, Club club) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		if (ClubCfg.get().isCheckRoomId() && table.getRoomId() != req.getRoomId()) {
			return;
		}
		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());

		if (club.hasSetting(EClubSettingStatus.CONCEAL_TABLE)) {
			notifyClubStatus(builder, club, club.getManagerIds());
		} else {
			notifyClubStatus(builder, club);
		}
	}

	private void gameEnd(ClubRoomStatusProto req, Club club) {
		if (req.getClubMatchId() > 0) {
			try {
				ClubMatchWrap matchWrap = club.matchs.get(req.getClubMatchId());
				if (matchWrap != null) {
					List<ClubTablePlayerProto> list = req.getPlayersList();
					for (ClubTablePlayerProto proto : list) {
						ClubMemberModel member = club.members.get(proto.getAccountId());
						if (member != null) {
							//	boolean isComplete = req.getRoomCurRound() >= matchWrap.getRuleBuilder().getGameRound();
							// 目前自建赛房间是不能解散的，一定会打满所有小局,不存在解散的情况了
							matchWrap.updateScore(member.getAccount_id(), (int) proto.getScore(), true, false);
							matchWrap.setGameEnd(member.getAccount_id());
						}
					}
					matchWrap.updateClubMatchTableEnd(req.getRoomId());
				}
				club.checkMatchEnd(req);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		if (table.getRoomId() != req.getRoomId()) {
			return;
		}
		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());

		if (club.hasSetting(EClubSettingStatus.CONCEAL_TABLE) && table.isGameStart()) {
			notifyClubStatus(builder, club, club.getManagerIds());
		} else {
			notifyClubStatus(builder, club);
		}

		table.resetTable();
		table.delTableCache();
	}

	private void gameStart(ClubRoomStatusProto req, Club club) {
		if (req.getClubMatchId() > 0) {
			try {
				ClubMatchWrap matchWrap = club.matchs.get(req.getClubMatchId());
				if (matchWrap != null) {
					matchWrap.updateClubMatchTableRound(req.getRoomId(), req.getRoomCurRound());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		if (ClubCfg.get().isCheckRoomId() && table.getRoomId() != req.getRoomId()) {
			return;
		}

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());
		builder.setCurRound(req.getRoomCurRound());
		table.setCurRound(req.getRoomCurRound());
		table.nextRound();
		notifyClubStatus(builder, club);

		// 告知牌桌内人员可以开局了
		ClubCommonLIIProto.Builder sBuilder = ClubCommonLIIProto.newBuilder().setClubId(club.getClubId()).setRuleId(req.getRuleId());
		table.playerIds().forEach((accountId) -> {
			SessionService.getInstance().sendClient(accountId, S2CCmd.CLUB_GAME_START_NOTIFY, sBuilder);
		});

		// GAME-TODO 如果有隐藏桌子设置，这里要推送一个空桌给客户端[管理员看到的是全部桌子，不需要推送]
		if (club.hasSetting(EClubSettingStatus.CONCEAL_TABLE) && table.getCurRound() == 1) {
			Optional<ClubTable> emptyTable = ruleTables.emptyAndNotInShowTable();
			ruleTables.willShow(table, false);
			emptyTable.ifPresent(etyTable -> {
				ruleTables.willShow(etyTable, true);
				ClubRuleTableGroupProto.Builder rBuilder = ClubRuleTableGroupProto.newBuilder();
				rBuilder.setClubId(req.getClubId());
				rBuilder.setRuleId(req.getRuleId());
				rBuilder.addClubTables(etyTable.toClubTableBuilder());
				Utils.sendClubAllMembers(rBuilder, S2CCmd.CLUB_EMPTY_TABLE, club, true, new HashSet<Long>(club.getManagerIds()));
			});
		}
	}

	private void playerExit(ClubRoomStatusProto req, Club club) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		if (ClubCfg.get().isCheckRoomId() && table.getRoomId() != req.getRoomId()) {
			return;
		}
		table.playerExit(req.getPlayer().getAccountId());

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());
		builder.setPlayer(req.getPlayer());

		notifyClubStatus(builder, club);
	}

	private void playerEnter(ClubRoomStatusProto req, Club club) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}

		if (!req.hasPlayer()) {
			return;
		}
		ClubTable table = ruleTables.getTable(req.getTableIndex());

		if (ClubCfg.get().isCheckRoomId() && table.getRoomId() != req.getRoomId()) {
			return;
		}

		table.playerEnter(req.getPlayer());

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());
		builder.setPlayer(req.getPlayer());

		if (req.getPlayer().getReady()) {
			table.setDefaultReady(true);
		}

		notifyClubStatus(builder, club);
	}

	private void roomCreateAndEnter(final ClubRoomStatusProto req, Club club, C2SSession session) {
		final int ruleId = req.getRuleId();

		ClubRuleTable ruleTables = club.ruleTables.get(ruleId);
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}
		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		Pair<EServerType, Integer> sessionInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
		if (sessionInfo.getFirst() != EServerType.LOGIC || sessionInfo.getSecond().intValue() > 0) {
			table.setLogicIndex(sessionInfo.getSecond().intValue());
		}

		table.setRoomId(req.getRoomId());
		table.setSetsModel(StatusModule.newWithStatus(req.getRoomSetStatus()));
		table.setMinPlayerCount(req.getMinPlayerCount());
		if (req.hasPlayer()) {
			ClubTablePlayerProto playerPb = req.getPlayer();
			table.playerEnter(playerPb);
			if (playerPb.getReady()) {
				table.setDefaultReady(true);
			}
		}

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());

		if (req.hasPlayer()) {
			builder.setPlayer(req.getPlayer());
		}
		notifyClubStatus(builder, club);
	}

	private void clubRoomStatusSync(final ClubRoomStatusProto req, Club club, C2SSession session) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}
		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}

		//		logger.info("俱乐部[{}]，玩法[{}]，桌子[{}],房间id：{},  同步！", club.getClubId(), req.getRuleId(), req.getTableIndex(), req.getRoomId());
		table.resetTable();

		Pair<EServerType, Integer> sessionInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
		if (sessionInfo.getFirst() != EServerType.LOGIC || sessionInfo.getSecond().intValue() > 0) {
			table.setLogicIndex(sessionInfo.getSecond().intValue());
		}
		table.setRoomId(req.getRoomId());
		table.setSetsModel(StatusModule.newWithStatus(req.getRoomSetStatus()));
		table.setCurRound(req.getRoomCurRound());
		table.setMinPlayerCount(req.getMinPlayerCount());
		List<ClubTablePlayerProto> players = req.getPlayersList();
		if (null != players && !players.isEmpty()) {
			players.forEach((playerPB) -> {
				table.playerEnter(playerPB);
			});
		}
	}

	/**
	 * 结算快照
	 */
	private void gameOverSnapshot(ClubRoomStatusProto req, Club club) {
		if (req.getClubMatchId() > 0) { // 自建赛数据不算到亲友圈数据中
			return;
		}

		ClubGameOverSnapshotProto.Builder builder = ClubGameOverSnapshotProto.newBuilder();
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		ClubTableProto.Builder tableBuilder = ClubTableProto.newBuilder();
		tableBuilder.setIndex(req.getTableIndex());
		tableBuilder.setRoomId(req.getRoomId());
		tableBuilder.addAllPlayers(req.getPlayersList());
		builder.setClubTable(tableBuilder);
		builder.setBrandId(String.valueOf(req.getBrandId()));
		Utils.sendClubAllMembers(builder, S2CCmd.CLUB_GAME_OVER_SNAPSHOT, club, false);

		// 更新玩家打过的局数 (后续会在逻辑服都更新后走ClubService roomPlayerLog)
		//		if (req.getRoomCurRound() >= 1) {
		//			ClubRuleTable ruleTable = club.ruleTables.get(req.getRuleId());
		//			if (ruleTable == null) {
		//				return;
		//			}
		//			List<ClubTablePlayerProto> list = req.getPlayersList();
		//			if (req.getRoomCurRound() == 1) {// 只打了一局的情况下,如果所有玩家积分都为0,就不算局数
		//				boolean isNotComplete = true;
		//				for (ClubTablePlayerProto proto : list) {
		//					if (proto.getScore() != 0) {// 有一人分数不为0就认为这一局正常打完了
		//						isNotComplete = false;
		//						break;
		//					}
		//				}
		//				if (isNotComplete) {
		//					return;
		//				}
		//			}
		//
		//			// 判断是否跨天的牌局(这里取不到扣豆时间，暂时通过查战绩来获取
		//			Calendar now = Calendar.getInstance();
		//			int hour = now.get(Calendar.HOUR_OF_DAY);
		//			int min = now.get(Calendar.MINUTE);
		//			if (hour == 0 && min <= 30) {
		//				BrandLogModel brand = MongoDBServiceImpl.getInstance().getClubParentBrand(req.getBrandId());
		//				if (brand != null) {
		//					if (!DateUtils.isSameDay(new Date(), brand.getCreate_time())) { // 昨天开局的
		//						return;
		//					}
		//				}
		//			}
		//			List<ClubMemberModel> memList = new ArrayList<>();
		//			for (ClubTablePlayerProto proto : list) {
		//				ClubMemberModel member = club.members.get(proto.getAccountId());
		//				if (member != null) {
		//					member.updatePlayRound(req.getRuleId());
		//					memList.add(member);
		//				}
		//			}
		//			club.runInDBLoop(new ClubRecordDBTask(memList));
		//		}
	}

	/**
	 * TABLE_REFRESH((byte) 15, "整个牌桌刷新"),; 牌桌刷新，整个桌子刷新，代价比较大
	 */
	private void tableRefresh(ClubRoomStatusProto req, Club club, C2SSession session) {
		ClubRuleTable ruleTables = club.ruleTables.get(req.getRuleId());
		if (null == ruleTables) {
			logger.error("俱乐部[{}]规则[{}] 不存在!", req.getClubId(), req.getRuleId());
			return;
		}
		ClubTable table = ruleTables.getTable(req.getTableIndex());
		if (null == table) {
			return;
		}
		logger.info("俱乐部[{}]，玩法[{}]，桌子[{}],房间id：{},  刷新！", club.getClubId(), req.getRuleId(), req.getTableIndex(), req.getRoomId());
		table.resetTable();
		Pair<EServerType, Integer> sessionInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
		if (sessionInfo.getFirst() != EServerType.LOGIC || sessionInfo.getSecond().intValue() > 0) {
			table.setLogicIndex(sessionInfo.getSecond().intValue());
		}
		table.setRoomId(req.getRoomId());
		table.setSetsModel(StatusModule.newWithStatus(req.getRoomSetStatus()));
		table.setCurRound(req.getRoomCurRound());
		table.setMinPlayerCount(req.getMinPlayerCount());

		ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setRuleId(req.getRuleId());
		builder.setIndex(req.getTableIndex());
		builder.setRoomId(req.getRoomId());
		builder.setRoomStatus(req.getRoomStatus());
		builder.addAllPlayers(req.getPlayersList());
		notifyClubStatus(builder, club);
		req.getPlayersList().forEach(p -> {
			table.replacePlayerInfo(p);
		});
	}

	private static void notifyClubStatus(final GeneratedMessage.Builder<?> builder, final Club club) {
		Utils.sendClubAllMembers(builder, S2CCmd.CLUB_TABLE_UPDATE, club, true, null);
	}

	private static void notifyClubStatus(final GeneratedMessage.Builder<?> builder, final Club club, Collection<Long> targetIds) {
		Utils.sendClient(targetIds, S2CCmd.CLUB_TABLE_UPDATE, builder);
	}

	/**
	 * 房间id对不上
	 */
	protected void warn(int clubId, int ruleId, int index, int cacheRoomId, int reqRoomId) {
		logger.warn("[clubId:{},ruleId:{},index:{}] cacheRoomId:{},reqRoomId:{}", clubId, ruleId, index, cacheRoomId, reqRoomId);
	}
}
