/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.utils;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerType;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.dictionary.SysParamDict;
import com.cai.service.ClubCacheService;
import com.cai.service.SessionService;

import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.s2s.ClubServerProto.ClubCreateRoomNewProto;
import protobuf.clazz.s2s.ClubServerProto.ProxyClubRq;

/**
 * 
 *
 * @author wu_hc date: 2017年12月13日 下午3:06:20 <br/>
 */
public final class LogicMsgSender {

	private static final Logger logger = LoggerFactory.getLogger(LogicMsgSender.class);

	/**
	 * 俱乐部直连创建房间
	 * 
	 * @param proxyClubRq
	 * @param club
	 * @param roomModel
	 * @param proxyId
	 */
	public static void sendCreateRoom(final ProxyClubRq proxyClubRq, Club club, ClubRoomModel roomModel, int proxyId, int joinId) {

		Optional<LogicRoomAccountItemRequest.Builder> clubOwnerPB = ClubCacheService.getInstance().ownerPB(club.getOwnerId());
		if (!clubOwnerPB.isPresent()) {
			logger.error("俱乐部[{},{}] clubOwnerPB is nil value!!!", club.getClubId(), club.getClubName());
			return;
		}

		ClubCreateRoomNewProto.Builder builder = ClubCreateRoomNewProto.newBuilder();

		boolean isObserverGame = SysParamDict.getInstance().isObserverGameTypeIndex(roomModel.getClubRule().getGame_type_index());

		LogicRoomRequest.Builder logicRoomRqBuilder = LogicRoomRequest.newBuilder();
		logicRoomRqBuilder.setType(isObserverGame ? 66 : 1);
		logicRoomRqBuilder.setRoomId(roomModel.getRoomId());

		// 请求者数据
		LogicRoomAccountItemRequest.Builder requestMember = proxyClubRq.getRequestAccountProto().toBuilder().setClubOwner(club.getOwnerId())
				.setJoinId(joinId);
		logicRoomRqBuilder.setLogicRoomAccountItemRequest(requestMember);

		// 俱乐部创始人数据
		logicRoomRqBuilder.setClubOwnerAccount(clubOwnerPB.get());

		// roomrequst
		RoomRequest.Builder roomRqBuilder = RoomRequest.newBuilder();
		roomRqBuilder.setRuleId(roomModel.getClubRule().getId());
		roomRqBuilder.setClubId(club.getClubId());
		roomRqBuilder.setClubName(club.getClubName());
		roomRqBuilder.setAppId(0);
		roomRqBuilder.setGameRound(roomModel.getClubRule().getGame_round());
		roomRqBuilder.setGameTypeIndex(roomModel.getClubRule().getGame_type_index());
		roomRqBuilder.setNewRules(roomModel.getClubRule().getRules());
		roomRqBuilder.setClubMemberSize(club.getMemberCount());

		logicRoomRqBuilder.setRoomRequest(roomRqBuilder.build());

		builder.setProxyServerId(proxyId);
		builder.setLogicRoomRequest(logicRoomRqBuilder);

		final SessionService sender = SessionService.getInstance();
		sender.sendMsg(EServerType.LOGIC, roomModel.getLogicId(), PBUtil.toS2SResponse(S2SCmd.CREATE_CLUB_ROOM_RSP, builder));
	}

	/**
	 * 加入房间
	 * 
	 * @param proxyClubRq
	 * @param club
	 * @param roomModel
	 * @param proxyId
	 */
	public static void sendJoinRoom(final ProxyClubRq proxyClubRq, Club club, ClubRoomModel roomModel, int proxyId, int joinId) {
		ClubCreateRoomNewProto.Builder builder = ClubCreateRoomNewProto.newBuilder();

		boolean isObserverGame = SysParamDict.getInstance().isObserverGameTypeIndex(roomModel.getClubRule().getGame_type_index());

		LogicRoomRequest.Builder logicRoomRqBuilder = LogicRoomRequest.newBuilder();
		logicRoomRqBuilder.setType(isObserverGame ? 56 : 2);
		logicRoomRqBuilder.setRoomId(roomModel.getRoomId());

		// 请求者数据
		logicRoomRqBuilder.setLogicRoomAccountItemRequest(proxyClubRq.getRequestAccountProto().toBuilder().setJoinId(joinId));

		// roomrequst
		RoomRequest.Builder roomRqBuilder = RoomRequest.newBuilder();
		roomRqBuilder.setRuleId(roomModel.getClubRule().getId());
		roomRqBuilder.setClubId(club.getClubId());
		roomRqBuilder.setClubName(club.getClubName());
		roomRqBuilder.setAppId(0);
		roomRqBuilder.setGameRound(roomModel.getClubRule().getGame_round());
		roomRqBuilder.setGameTypeIndex(roomModel.getClubRule().getGame_type_index());
		roomRqBuilder.setNewRules(roomModel.getClubRule().getRules());
		roomRqBuilder.setClubMemberSize(club.getMemberCount());

		logicRoomRqBuilder.setRoomRequest(roomRqBuilder.build());

		builder.setProxyServerId(proxyId);
		builder.setLogicRoomRequest(logicRoomRqBuilder);

		final SessionService sender = SessionService.getInstance();
		sender.sendMsg(EServerType.LOGIC, roomModel.getLogicId(), PBUtil.toS2SResponse(S2SCmd.CREATE_ENTER_ROOM_RSP, builder));
	}

	/**
	 * 俱乐部自建赛创建房间
	 */
	public static boolean sendCreateClubMatchRoom(Club club, ClubRoomModel roomModel, List<Long> playerList, long clubMatchId) {

		Optional<LogicRoomAccountItemRequest.Builder> clubOwnerPB = ClubCacheService.getInstance().ownerPB(club.getOwnerId());
		if (!clubOwnerPB.isPresent()) {
			logger.error("俱乐部[{},{}] clubOwnerPB is nil value!!!", club.getClubId(), club.getClubName());
			return false;
		}

		ClubCreateRoomNewProto.Builder builder = ClubCreateRoomNewProto.newBuilder();

		LogicRoomRequest.Builder logicRoomRqBuilder = LogicRoomRequest.newBuilder();
		logicRoomRqBuilder.setType(77);
		logicRoomRqBuilder.setRoomId(roomModel.getRoomId());
		logicRoomRqBuilder.setClubMatchId(clubMatchId);

		// 请求者数据
		LogicRoomAccountItemRequest.Builder requestMember = LogicRoomAccountItemRequest.newBuilder().setClubOwner(club.getOwnerId());
		logicRoomRqBuilder.setLogicRoomAccountItemRequest(requestMember);

		// 俱乐部创始人数据
		logicRoomRqBuilder.setClubOwnerAccount(clubOwnerPB.get());

		// roomrequst
		RoomRequest.Builder roomRqBuilder = RoomRequest.newBuilder();
		roomRqBuilder.setClubId(club.getClubId());
		roomRqBuilder.setClubName(club.getClubName());
		roomRqBuilder.setAppId(0);
		roomRqBuilder.setGameRound(roomModel.getClubRule().getGame_round());
		roomRqBuilder.setGameTypeIndex(roomModel.getClubRule().getGame_type_index());
		roomRqBuilder.setNewRules(roomModel.getClubRule().getRules());
		roomRqBuilder.setClubMemberSize(club.getMemberCount());

		logicRoomRqBuilder.setRoomRequest(roomRqBuilder.build());
		for (Long accountId : playerList) {
			Optional<LogicRoomAccountItemRequest.Builder> playerPB = ClubCacheService.getInstance().memberPB(accountId);
			if (playerPB.isPresent()) {
				playerPB.get().setProxyIndex(SessionService.getInstance().getProxyByServerIndex(accountId));
				logicRoomRqBuilder.addClubMatchPlayers(playerPB.get());
			}
		}

		builder.setLogicRoomRequest(logicRoomRqBuilder);
		builder.setProxyServerId(SessionService.getInstance().getProxyByServerIndex(playerList.get(0)));

		final SessionService sender = SessionService.getInstance();
		sender.sendMsg(EServerType.LOGIC, roomModel.getLogicId(), PBUtil.toS2SResponse(S2SCmd.CREATE_CLUB_ROOM_RSP, builder));
		return true;
	}

	private LogicMsgSender() {
	}
}
