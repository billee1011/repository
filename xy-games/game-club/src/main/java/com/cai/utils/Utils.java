package com.cai.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.define.EPhoneIdentifyCodeType;
import com.cai.common.define.ERedHeartCategory;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.StatusModule;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.StatusUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.EClubOperateCategory;
import com.cai.constant.ERuleSettingStatus;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubService;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.ClubMsgProto.ClubActivityEventNotify;
import protobuf.clazz.ClubMsgProto.ClubBulletinEventNotify;
import protobuf.clazz.ClubMsgProto.ClubCommonIIsProto;
import protobuf.clazz.ClubMsgProto.ClubEventMsgRsp;
import protobuf.clazz.ClubMsgProto.ClubExclusiveGoldProto;
import protobuf.clazz.ClubMsgProto.ClubIdentityUpdateProto;
import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import protobuf.clazz.ClubMsgProto.ClubJoinResultRsp;
import protobuf.clazz.ClubMsgProto.ClubMatchEventNotify;
import protobuf.clazz.ClubMsgProto.ClubMemberJoinProto;
import protobuf.clazz.ClubMsgProto.ClubOperateEventRsp;
import protobuf.clazz.Common.CommonII;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ClubResponse;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;
import protobuf.clazz.s2s.ClubServerProto.ClubToClientRsp;

import static protobuf.clazz.ClubMsgProto.ClubIdentityUpdateBatchProto;
import static protobuf.clazz.ClubMsgProto.MemberIdentity;

public class Utils {
	public static void sendClientClubResponse(ClubResponse clubRep, C2SSession session, long account) {
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.CLUB);
		responseBuilder.setExtension(Protocol.clubResponse, clubRep);

		session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, getClubServerResponse(account).setRsp(responseBuilder)));
	}

	private static ClubToClientRsp.Builder getClubServerResponse(long account_id) {
		ClubToClientRsp.Builder b = ClubToClientRsp.newBuilder();
		b.setClientSessionId(account_id);
		return b;
	}

	public static void sendClientClubResponse(ClubResponse clubRep, long account, EServerType type, int index) {
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.CLUB);
		responseBuilder.setExtension(Protocol.clubResponse, clubRep);
		SessionService.getInstance()
				.sendMsg(type, index, PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, getClubServerResponse(account).setRsp(responseBuilder)));

	}

	public static void sendMsg(C2SSession session, long account, String msg) {
		session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, getMsgToCLubResponse(account, msg)));
	}

	/**
	 * 普通提示信息
	 */
	public static ClubToClientRsp.Builder getMsgToCLubResponse(long account_id, String msg) {
		return getClubServerResponse(account_id).setRsp(getMsgAllResponse(msg, ESysMsgType.NONE));
	}

	public static ClubToClientRsp.Builder getMsgToCLubResponse(long account_id, String msg, ESysMsgType type) {
		return getClubServerResponse(account_id).setRsp(getMsgAllResponse(msg, type));
	}

	private static Response.Builder getMsgAllResponse(String msg, ESysMsgType type) {
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(type.getId());
		msgBuilder.setMsg(msg);
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		return responseBuilder;
	}

	public static MatchClientResponse.Builder getMatchResponse(int cmd, GeneratedMessage.Builder<?> builder) {
		MatchClientResponse.Builder b = MatchClientResponse.newBuilder();
		b.setCmd(cmd);
		b.setData(builder.build().toByteString());
		return b;
	}

	/**
	 * 俱乐部事件消息通知
	 */
	public static void sendClubEventMsg(List<Long> accountIds, int clubId, ClubJoinQuitMsgProto msg) {
		ClubEventMsgRsp.Builder builder = ClubEventMsgRsp.newBuilder().setClubId(clubId).addEventMsgs(msg);
		Utils.sendClient(accountIds, S2CCmd.CLUB_EVENT_MSG_NOTIFY, builder);
		Utils.notifyRedHeart(ClubService.getInstance().getClub(clubId), ERedHeartCategory.CLUB_EVENT_NOTIFY);
	}

	public static void sendClubEventMsgBatch(List<Long> accountIds, int clubId, List<ClubJoinQuitMsgProto> msgList) {
		ClubEventMsgRsp.Builder builder = ClubEventMsgRsp.newBuilder().setClubId(clubId).addAllEventMsgs(msgList);
		Utils.sendClient(accountIds, S2CCmd.CLUB_EVENT_MSG_NOTIFY, builder);
		Utils.notifyRedHeart(ClubService.getInstance().getClub(clubId), ERedHeartCategory.CLUB_EVENT_NOTIFY);
	}

	/**
	 * 俱乐部事件消息
	 */
	public static ClubJoinQuitMsgProto.Builder newEventMsg(int clubId, ClubJoinQuitMsgProto.MsgType type, ClubMemberModel operator,
			ClubMemberModel targetAccount) {

		ClubJoinQuitMsgProto.Builder builder = ClubJoinQuitMsgProto.newBuilder().setClubId(clubId).setMsgType(type)
				.setOperateTime((int) (System.currentTimeMillis() / 1000));

		if (null != operator) {
			builder.setOperatorId(operator.getAccount_id()).setOperatorName(operator.getNickname());
		} else {
			builder.setOperatorId(0L).setOperatorName("");
		}

		if (null != targetAccount) {
			builder.setAccountId(targetAccount.getAccount_id());
			builder.setUserName(targetAccount.getNickname());
		} else {
			builder.setAccountId(0L);
			builder.setUserName("");
		}

		if (type == ClubJoinQuitMsgProto.MsgType.QUIT && targetAccount != null) {
			int tireValue = 0;
			ClubMemberRecordModel recordModel = targetAccount.getMemberRecordMap().get(ClubRecordDayType.TODAY);
			if (recordModel != null) {
				if (ClubCfg.get().isUseOldTireWay()) {
					tireValue = recordModel.getTireValue();
				} else {
					tireValue = recordModel.getAccuTireValue();
				}
			}
			builder.setTireValue(tireValue);
			Club club = ClubService.getInstance().clubs.get(clubId);
			if (club != null && club.clubWelfareWrap.isOpenClubWelfare()) {
				builder.setPlayerClubWelfare(targetAccount.getClubWelfare());
			}
		}

		return builder;
	}

	/**
	 * settingStatus 装换成直观的键值对
	 */
	public static List<CommonII> toClubStatusBuilder(StatusModule statusModule) {
		return StatusUtil.toStatusBuilder(statusModule, EClubSettingStatus.values());
	}

	/**
	 * settingStatus 装换成直观的键值对
	 */
	public static List<CommonII> toRuleStatusBuilder(StatusModule statusModule) {
		return StatusUtil.toStatusBuilder(statusModule, ERuleSettingStatus.values());
	}

	/**
	 * 默认只发给在场景内的玩家
	 */
	public static void sendClubAllMembers(final GeneratedMessage.Builder<?> builder, int cmd, final Club club) {
		sendClubAllMembers(builder, cmd, club, true);
	}

	/**
	 * @param inScene 是否在场景内才广播
	 */
	public static void sendClubAllMembers(final GeneratedMessage.Builder<?> builder, int cmd, final Club club, boolean inScene) {
		sendClubAllMembers(builder, cmd, club, inScene, null);
	}

	/**
	 * @param except 排出
	 */
	public static void sendClubAllMembers(final GeneratedMessage.Builder<?> builder, int cmd, final Club club, boolean inScene, Set<Long> except) {

		final Map<Long, ClubMemberModel> members = club.members;
		List<Long> accountIds = Lists.newArrayList();
		for (Map.Entry<Long, ClubMemberModel> entry : members.entrySet()) {

			final Long accountId = entry.getKey();

			if (null != except && except.contains(accountId)) {
				continue;
			}
			if (inScene && !PlayerService.getInstance().inClubScene(accountId)) {
				continue;
			}
			accountIds.add(accountId);
		}

		if (accountIds.isEmpty()) {
			return;
		}

		// 1,from gate
		// Utils.sendClient(accountIds, cmd, builder);

		// 2,from club
		accountIds.forEach((accountId) -> {
			SessionService.getInstance().sendClient(accountId, cmd, builder);
		});
	}

	/**
	 * 发消息给客户端
	 *
	 * @param accountId
	 * @param cmd
	 * @param builder
	 * @see S2CCmd
	 */
	public static void sendClient(final long accountId, int cmd, final GeneratedMessage.Builder<?> builder) {
		if (accountId <= 0)
			return;

		final SessionService sender = SessionService.getInstance();
		// 通过网关服转发
		// sender.sendGate(PBUtil.toS_S2CRequet(accountId, cmd,
		// builder).build());
		// 直接转发
		sender.sendClient(accountId, cmd, builder);
	}

	/**
	 * 发消息给客户端
	 *
	 * @param accountIds
	 * @param cmd
	 * @param builder
	 * @see S2CCmd
	 */
	public static void sendClient(final Collection<Long> accountIds, int cmd, final GeneratedMessage.Builder<?> builder) {
		if (accountIds.size() <= 0)
			return;

		final SessionService sender = SessionService.getInstance();

		// sender.sendGate(PBUtil.toS_S2CRequet(accountIds, cmd,
		// builder).build());

		accountIds.forEach((accountId) -> {
			sender.sendClient(accountId, cmd, builder);
		});
	}

	/**
	 * 通知给俱乐部成员
	 */
	public static void notifyUpdateRule(final int clubId, final int ruleId, final Club club, EClubOperateCategory category) {
		ClubOperateEventRsp.Builder updateBuilder = ClubOperateEventRsp.newBuilder().setClubId(clubId).setRuleId(ruleId)
				.setCategory(category.category());

		Utils.sendClubAllMembers(updateBuilder, S2CCmd.CLUB_RULE_UPDATE, club);

		// 牌桌内的玩家也要通知到
		Set<Long> inTablePlayers = Sets.newHashSet();
		club.ruleTables.forEach((id, ruleTable) -> {
			ruleTable.getTables().forEach(table -> {
				if (!table.isGameStart()) {
					inTablePlayers.addAll(table.getPlayers().keySet());
				}
			});
		});

		// 俱乐部操作事件需要推送给自建赛的参赛成员
		club.matchs.forEach((id, matchWrap) -> {
			inTablePlayers.addAll(matchWrap.enrollAccountIds());
		});

		Utils.sendClient(inTablePlayers, S2CCmd.CLUB_RULE_UPDATE, updateBuilder);
	}

	/**
	 * 红点
	 */
	public static void notifyRedHeart(final Club club, int type) {
		club.sendHaveNewMsg(type);
	}

	/**
	 * 加入俱乐部反馈
	 */
	public static void notifyJoinResult(long accountId, long operatorId, final Club club, int result) {
		ClubJoinResultRsp.Builder b = ClubJoinResultRsp.newBuilder().setClubId(club.getClubId()).setResult(result);
		b.setClubName(club.getClubName());
		b.setTargetAccountId(accountId);
		b.setOperatorAccountId(operatorId);
		// 管理员
		club.getManagerIds().forEach(managerId -> {
			sendClient(managerId, S2CCmd.CLUB_JOIN_RESULT, b);
		});
		// 目标玩家
		sendClient(accountId, S2CCmd.CLUB_JOIN_RESULT, b);
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
		SessionService.getInstance().sendClient(accountId, S2CCmd.CLUB_EXCLUSIVE_GOLD_INFO, builder);
	}

	/**
	 * 推送给指定玩家[一般为管理员，管理员收到这个消息，可以从请求列表移出这个玩家的加入请求]，玩家加入的信息，
	 */
	public static void sendJoinMemberInfo(Collection<Long> accountIds, int clubId, ClubMemberModel newMember) {
		boolean isOnline = PlayerService.getInstance().isPlayerOnline(newMember.getAccount_id());
		ClubMemberJoinProto.Builder b = ClubMemberJoinProto.newBuilder().setClubId(clubId).setMember(newMember.encode(isOnline));
		sendClient(accountIds, S2CCmd.CLUB_MEMBER_JOIN, b);
	}

	public static void notityIdentityUpdate(Collection<Long> accountIds, long targetId, int clubId, int newIdentity) {
		ClubIdentityUpdateProto.Builder b = ClubIdentityUpdateProto.newBuilder().setAccountId(targetId).setClubId(clubId).setIdentity(newIdentity);
		sendClient(accountIds, S2CCmd.CLUB_IDENTITY_UPDATE, b);
	}

	public static void notityIdentityUpdateBatch(Collection<Long> accountIds, int clubId, Map<Long, Integer> targetMap) {
		ClubIdentityUpdateBatchProto.Builder b = ClubIdentityUpdateBatchProto.newBuilder();
		b.setClubId(clubId);
		for (Map.Entry<Long, Integer> entry : targetMap.entrySet()) {
			MemberIdentity.Builder tmpBuilder = MemberIdentity.newBuilder();
			tmpBuilder.setAccountId(entry.getKey());
			tmpBuilder.setIdentity(entry.getValue());
			b.addMemberIdentity(tmpBuilder);
		}
		sendClient(accountIds, S2CCmd.CLUB_IDENTITY_UPDATE_BATCH, b);
	}

	/**
	 * 通知设置
	 */
	public static void notityClubSetsUpdate(final Club club) {
		ClubCommonIIsProto.Builder b = ClubCommonIIsProto.newBuilder().setClubId(club.getClubId())
				.addAllCommon(Utils.toClubStatusBuilder(club.setsModel));

		sendClubAllMembers(b, S2CCmd.CLUB_SETTINGS, club);
	}

	/**
	 * 俱乐部事件推送
	 */
	public static void notifyActivityEvent(long operatorId, final Club club, long activityId, int code) {
		ClubActivityEventNotify.Builder b = ClubActivityEventNotify.newBuilder().setOperatorId(operatorId).setClubId(club.getClubId())
				.setEventCode(code).setActivityId(activityId);
		sendClubAllMembers(b, S2CCmd.CLUB_ACTIVITY_EVENT, club);
	}

	public static void sendTip(long accountId, String msg, ESysMsgType msgType, C2SSession proxy) {
		proxy.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(accountId, msg, msgType)));
	}

	public static void sendTip(long accountId, String msg, ESysMsgType msgType) {
		C2SSession session = SessionService.getInstance()
				.getSession(EServerType.PROXY, SessionService.getInstance().getProxyByServerIndex(accountId));
		if (session != null) {
			Utils.sendTip(accountId, msg, msgType, session);
		}
	}

	/**
	 * 验证码
	 */
	public static boolean identifyCodeVaild(String mobile, String identifyCode, EPhoneIdentifyCodeType codeType) {
		return Objects.equals(identifyCode, SpringService.getBean(RedisService.class).get(codeType.exe().apply(mobile)));
	}

	/**
	 * 俱乐部事件推送
	 */
	public static void bulletinEvent(long operatorId, final Club club, long id, int code) {
		ClubBulletinEventNotify.Builder b = ClubBulletinEventNotify.newBuilder().setOperatorId(operatorId).setClubId(club.getClubId())
				.setEventCode(code).setBulletinId(id);
		sendClubAllMembers(b, S2CCmd.CLUB_BULLETIN_EVENT, club);
	}

	/**
	 * 亲友圈自建赛事件通知
	 */
	public static void notifyClubMatchEvent(long operatorId, Club club, long matchId, int opCode) {
		ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(operatorId).setClubId(club.getClubId()).setEventCode(opCode)
				.setMatchId(matchId);
		sendClubAllMembers(b, S2CCmd.CLUB_MATCH_EVENT, club, false);
	}

	/**
	 * 亲友圈自建赛事件推送
	 */
	public static void sendClubMatchEvent(long operatorId, Club club, long matchId, int opCode) {
		ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(operatorId).setClubId(club.getClubId()).setEventCode(opCode)
				.setMatchId(matchId);
		Utils.sendClient(operatorId, S2CCmd.CLUB_MATCH_EVENT, b);
	}
}
