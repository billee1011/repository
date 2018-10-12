/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.cai.common.ClubTableKickOutType;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EPlayerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.ClubExitType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.service.ClubService;
import com.cai.service.GroupClubMemberService;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.cai.utils.Utils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import org.apache.commons.lang.StringUtils;
import protobuf.clazz.ClubMsgProto.ClubChatHistory;
import protobuf.clazz.ClubMsgProto.ClubCommonLIIProto;
import protobuf.clazz.ClubMsgProto.ClubEventMsgRsp;
import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import protobuf.clazz.ClubMsgProto.ClubMemberListProto;
import protobuf.clazz.ClubMsgProto.ClubMemberRemarkProto;
import protobuf.clazz.ClubMsgProto.ClubOnlineMemeberRsp;
import protobuf.clazz.ClubMsgProto.ClubProto;
import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.ClubMsgProto.ClubRequest.ClubRequestType;
import protobuf.clazz.ClubMsgProto.ClubRuleOnSitRsp;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.ClubMsgProto.ClubRuleRemarkProto;
import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;
import protobuf.clazz.ClubMsgProto.ClubTodayCostProto;
import protobuf.clazz.ClubMsgProto.ClubUpdateProto;
import protobuf.clazz.ClubMsgProto.GroupMembers;
import protobuf.clazz.ClubMsgProto.OperateRsp;
import protobuf.clazz.ClubMsgProto.WxGroups;
import protobuf.clazz.Common.CommonIII;
import protobuf.clazz.Protocol.ClubResponse;
import protobuf.clazz.Protocol.ClubResponse.ClubResponseType;
import protobuf.clazz.s2s.ClubServerProto.ProxyClubRq;

/**
 *
 */
@ICmd(code = S2SCmd.CLUB_REQ, desc = "亲友圈请求")
public final class ClubProxyRequestHandler extends IClientHandler<ProxyClubRq> {

	final static HashMap<ClubRequestType, ClubRqType> RQ_MAPS = new HashMap<>(ClubRqType.values().length);

	static {
		ClubRqType[] temp = ClubRqType.values();
		for (int i = 0; i < temp.length; i++) {
			RQ_MAPS.put(temp[i].value, temp[i]);
		}
	}

	@Override
	public void execute(ProxyClubRq req, C2SSession session) throws Exception {
		ClubRequest clubRequest = req.getClubRq();
		ClubRqType type = RQ_MAPS.get(clubRequest.getType());
		if (type != null) {

			if (type != ClubRqType.CLUB_REQ_CHAT) {
				// 加强正确性，如果有请求，设置在线
				Pair<EServerType, Integer> serverInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
				if (null != serverInfo) {
					long reqAccountId = req.getClientSessionId();
					PlayerService.getInstance().enter(reqAccountId);
					SessionService.getInstance().statusUpate(reqAccountId, EPlayerStatus.ONLINE, serverInfo.getSecond());
				}
			}

			Club club = ClubService.getInstance().getClub(clubRequest.getClubId());
			if (null != club) {
				if (ClubCfg.get().isUseReserveWorker()) {
					club.runInClubLoop(new ClubReqInvoker(clubRequest, req, session, type));
				} else {
					club.runInReqLoop(new ClubReqInvoker(clubRequest, req, session, type));
				}

			} else {
				type.exe(clubRequest, req, session);
			}
		} else {
			throw new NullPointerException("亲友圈协议找不到," + clubRequest.getType());
		}
	}

	// 任务执行器
	final static class ClubReqInvoker implements Runnable {
		final ClubRequest clubRequest;
		final ProxyClubRq req;
		final C2SSession session;
		final ClubRqType type;

		/**
		 * @param clubRequest
		 * @param req
		 * @param session
		 * @param type
		 */
		public ClubReqInvoker(ClubRequest clubRequest, ProxyClubRq req, C2SSession session, ClubRqType type) {
			this.clubRequest = clubRequest;
			this.req = req;
			this.session = session;
			this.type = type;
		}

		@Override
		public void run() {
			this.type.exe(clubRequest, req, session);
		}

		@Override
		public String toString() {
			return clubRequest.getType().toString();
		}
	}

	enum ClubRqType {
		CLUB_REQ_LIST(ClubRequestType.CLUB_REQ_LIST_VALUE) {// 列表

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {

				long accountId = topRequest.getClientSessionId();

				List<ClubProto> clubs = ClubService.getInstance().getMyClub(accountId, false);

				ClubResponse.Builder clubRep = ClubResponse.newBuilder();
				clubRep.setType(ClubResponseType.CLUB_RSP_LIST);
				clubRep.addAllClubs(clubs);

				Utils.sendClientClubResponse(clubRep.build(), session, accountId);
			}
		}, CLUB_REQ_ENTER(ClubRequestType.CLUB_REQ_ENTER_VALUE) {// 请求加入

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();

				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				PlayerViewVO account = centerRMIServer.getPlayerViewVo(topRequest.getClientSessionId());

				ClubRoomModel status = ClubService.getInstance()
						.requestClub(clubId, topRequest.getClientSessionId(), account.getHead(), request.getJoinContent(), account.getNickName(),
								request.getPartnerId());

				if (StringUtils.isNotEmpty(status.getDesc())) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(account.getAccountId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				}

				if (status.getStatus() == Club.SUCCESS) { // 回应，让客户端关闭房号窗口
					ClubResponse.Builder b = ClubResponse.newBuilder();
					b.setType(ClubResponseType.CLUB_RSP_ENTER);
					b.setClubId(clubId);
					Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
				}
			}
		}, CLUB_REQ_MEMBER_OUT(ClubRequestType.CLUB_REQ_MEMBER_OUT_VALUE) {// 退出

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				PlayerViewVO account = centerRMIServer.getPlayerViewVo(topRequest.getClientSessionId());
				ClubRoomModel status = ClubService.getInstance()
						.requestQuitClub(clubId, topRequest.getClientSessionId(), account.getHead(), request.getJoinContent(), account.getNickName());
				// ClubRoomModel status =
				// ClubService.getInstance().outClub(clubId,
				// topRequest.getClientSessionId());
				if (StringUtils.isNotEmpty(status.getDesc())) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc())));
				}

				switch (status.getStatus()) {
				case Club.SUCCESS:
					ClubResponse.Builder b = ClubResponse.newBuilder();
					b.setType(ClubResponseType.CLUB_RSP_MEMBER_OUT);
					b.setClubId(clubId);

					Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
					break;
				}
			}
		}, CLUB_REQ_CREATE_ROOM(ClubRequestType.CLUB_REQ_CREATE_ROOM_VALUE) { // 创建房间_

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {

				int clubId = request.getClubId();

				Club club = ClubService.getInstance().getClub(clubId);
				if (null == club) {
					return;
				}

				ClubRoomModel roomModel = ClubService.getInstance().createClubRoom(request, topRequest, session);
				if (roomModel == null) {
					return;
				}

				if (roomModel.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), roomModel.getDesc())));
				}
			}
		}, CLUB_REQ_AGREE(ClubRequestType.CLUB_REQ_AGREE_VALUE) {// 同意加入

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = null;
				int clubId = request.getClubId();
				long accountId = request.getAccountId();
				if (request.getIsBatch()) {
					status = ClubService.getInstance().agreeJoinClubBatch(topRequest.getClientSessionId(), clubId);
				} else {
					status = ClubService.getInstance().agreeJoinClub(topRequest.getClientSessionId(), clubId, accountId);
				}

				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));

				switch (status.getStatus()) {
				case Club.SUCCESS:
					ClubResponse.Builder b = ClubResponse.newBuilder();
					b.setType(ClubResponseType.CLUB_RSP_CLUB_AGREE);
					b.setTargetAccountId(accountId);
					b.setClubId(clubId);
					b.setIsBatch(request.getIsBatch());
					Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
					break;
				}
			}
		}, CLUB_REQ_REJECT(ClubRequestType.CLUB_REQ_REJECT_VALUE) {// 拒绝加入

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				long accountId = request.getAccountId();
				int clubId = request.getClubId();
				int status = ClubService.getInstance().rejectClub(clubId, topRequest.getClientSessionId(), accountId);

				switch (status) {
				case Club.CLUB_NOT_FIND:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "找不到该亲友圈")));
					break;
				case Club.PERM_DENIED:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "权限不足！")));
					break;
				case Club.HAS_JOIN:
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "该玩家已经加入亲友圈")));
					break;
				case Club.NO_REQUEST:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "没有申请记录")));
					break;
				case 1:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "拒绝审核成功")));
					// 后期移出
					ClubResponse.Builder b = ClubResponse.newBuilder();
					b.setType(ClubResponseType.CLUB_RSP_CLUB_REJECT);
					b.setTargetAccountId(accountId);
					b.setClubId(clubId);
					Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
					break;
				}
			}
		}, CLUB_REQ_KICK(ClubRequestType.CLUB_REQ_KICK_VALUE) {// 踢人出亲友圈

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				long accountId = request.getAccountId();
				int clubId = request.getClubId();
				int exitType = request.getExitType();
				// 先踢微信群成员
				ClubService.getInstance().kickGroup(clubId, topRequest.getClientSessionId(), accountId);
				int status = ClubService.getInstance().kickClub(clubId, topRequest.getClientSessionId(), accountId, exitType);

				switch (status) {
				case Club.CLUB_NOT_FIND:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "找不到该亲友圈")));
					break;
				case Club.PERM_DENIED:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "权限不够")));
					break;
				case Club.HAS_JOIN:
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "该玩家已经加入亲友圈")));
					break;
				case Club.NO_REQUEST:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "没有申请记录")));
					break;
				case Club.IS_PARTNER:
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "该玩家是亲友圈合伙人，请先解除合伙关系")));
					break;
				case Club.SUCCESS:
					String msg = "";
					if (exitType == ClubExitType.KICK) {
						msg = "踢出成功";
					} else if (exitType == ClubExitType.AGREE_QUIT) {
						msg = "同意该玩家的退出申请";
					}
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), msg)));
					ClubResponse.Builder b = ClubResponse.newBuilder();
					b.setType(ClubResponseType.CLUB_RSP_KICK);
					b.setTargetAccountId(accountId);
					b.setClubId(clubId);
					Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
					break;
				}
			}
		}, CLUB_REQ_CREATE_CLUB(ClubRequestType.CLUB_REQ_CREATE_CLUB_VALUE) {// 创建亲友圈

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubProto clubProto = request.getCreatClub();

				int status = ClubService.getInstance().createClub(topRequest.getClientSessionId(), clubProto);
				if (status > 0) {
					ClubProto createClub = ClubService.getInstance().getClubEncodeDetail(status, topRequest.getClientSessionId());
					if (createClub != null) {
						session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
								Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "创建亲友圈成功")));
						ClubResponse.Builder b = ClubResponse.newBuilder();
						b.setType(ClubResponseType.CLUB_RSP_CLUB_CREATE);
						b.setClubDetail(createClub);
						Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
						return;
					}
				}

				switch (status) {
				case -1:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "创建亲友圈失败")));
					break;
				case -2:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "创建亲友圈失败")));
					break;
				case Club.CLUB_NAME_ERROR:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "亲友圈名字非法")));
					break;
				case Club.CLUB_DESC_ERROR:
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "亲友圈描述非法")));
					break;
				case Club.CLUB_CREATE_MAX:
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "亲友圈创建数量达到上限")));
					break;
				}
			}
		}, CLUB_REQ_UPDATE_CLUB(ClubRequestType.CLUB_REQ_UPDATE_CLUB_VALUE) {// 修改亲友圈[修改，添加包间，删除包间等]

			@SuppressWarnings("unchecked")
			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubUpdateProto clubProto = request.getClubUpdate();

				ClubRoomModel status = ClubService.getInstance().updateClub(topRequest.getClientSessionId(), clubProto);
				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));

				if (status.getStatus() != Club.SUCCESS) {
					return;
				}
				Club club = ClubService.getInstance().getClub(clubProto.getClubId());
				if (null != club) {

					List<Integer> ruleIds = null;
					if (clubProto.getType() == 3) {
						ruleIds = (List<Integer>) (status.getAttament());
					} else {
						ruleIds = clubProto.getClubRuleList().stream().map(ClubRuleProto::getId).collect(Collectors.toList());
					}
					ClubProto createClub = club.encode(ruleIds, true).build();
					if (createClub != null) {
						ClubResponse.Builder b = ClubResponse.newBuilder();
						b.setType(ClubResponseType.CLUB_RSP_UPDATE_CLUB);
						b.setClubUpdate(clubProto.toBuilder().clearClubRule().addAllClubRule(createClub.getClubRuleList()).clearSetStatus()
								.addAllSetStatus(Utils.toClubStatusBuilder(club.setsModel)));

						if (clubProto.getType() == 5 || clubProto.getType() == 6) {
							club.getManagerIds().forEach(accountId -> {
								Utils.sendClientClubResponse(b.build(), session, accountId);
							});
						} else if (clubProto.getType() != 2 && clubProto.getType() != 7) { // 在修改里已经发了通知
							Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
						}
						return;
					}
				}
			}
		},

		CLUB_REQ_DELETE(ClubRequestType.CLUB_REQ_DELETE_VALUE) {// 解散亲友圈

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();

				ClubRoomModel status = ClubService.getInstance().deleteClub(clubId, topRequest.getClientSessionId(), request.getField(), false);

				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc())));

				if (status.getStatus() == Club.SUCCESS) {
					ClubResponse.Builder b = ClubResponse.newBuilder();
					b.setType(ClubResponseType.CLUB_RSP_DELETE);
					b.setClubId(clubId);

					Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
				}
			}
		}, CLUB_REQ_RULE_TABLES(ClubRequestType.CLUB_REQ_RULE_TABLES_VALUE) { // 玩法对应牌桌列表

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();
				Club club = ClubService.getInstance().getClub(clubId);
				if (null == club) {
					return;
				}
				if (!club.isMember(topRequest.getClientSessionId())) {
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "不是亲友圈成员！")));
					return;
				}
				int ruleId = request.getClubRuleId();
				final ClubRuleTable ruleTables = club.ruleTables.get(ruleId);
				if (null == ruleTables) {
					Utils.sendTip(topRequest.getClientSessionId(), "包间不存在，可能被管理员删除或者下架了,请退出游戏重进！", ESysMsgType.NONE, session);
					return;
				}
				// send to client
				// ClubRuleTableGroupProto.Builder builder =
				// ruleTables.toTablesBuilder(clubId);
				// session.send(PBUtil.toS_S2CRequet(request.getAccountId(),
				// S2CCmd.CLUB_RULE_TABLES, builder));

				ClubRoomModel status = ClubService.getInstance().clubReqRuleTables(request, topRequest);
				if (Club.SUCCESS != status.getStatus()) {
					Utils.sendTip(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.NONE, session);
				} else {
					ClubRuleTableGroupProto.Builder builder = (ClubRuleTableGroupProto.Builder) status.getAttament();
					session.send(PBUtil.toS_S2CRequet(request.getAccountId(), S2CCmd.CLUB_RULE_TABLES, builder));
				}
			}

		}, CLUB_REQ_RULE_TABLE_PLAYER_SIZE(ClubRequestType.CLUB_REQ_RULE_TABLE_PLAYER_SIZE_VALUE) { // 规则下对应的在桌玩家数量

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();
				Club club = ClubService.getInstance().getClub(clubId);
				if (null == club) {
					return;
				}
				if (!club.members.containsKey(topRequest.getClientSessionId())) {
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "不是亲友圈成员！")));
					return;
				}
				// send to client
				ClubRuleOnSitRsp.Builder builder = ClubRuleOnSitRsp.newBuilder();
				builder.setClubId(clubId);
				club.ruleTables.forEach((ruleId, ruleTable) -> {
					CommonIII.Builder kv = CommonIII.newBuilder();
					kv.setK(ruleId);
					kv.setV1(ruleTable.getPlayerCount());
					kv.setV2(ruleTable.getPlayingTableCount());
					builder.addRuleIdAndSize(kv);
				});
				session.send(PBUtil.toS_S2CRequet(request.getAccountId(), S2CCmd.CLUB_RULE_TABLE_ON_SIT, builder));
			}

		}, CLUB_REQ_EVENT_MSG(ClubRequestType.CLUB_REQ_EVENT_MSG_VALUE) { // 亲友圈事件消息

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();
				Club club = ClubService.getInstance().getClub(clubId);
				if (null == club) {
					return;
				}
				if (!club.members.containsKey(topRequest.getClientSessionId())) {
					session.send(
							PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), "不是亲友圈成员！")));
					return;
				}
				// send to client
				ClubEventMsgRsp.Builder builder = ClubEventMsgRsp.newBuilder();
				builder.setClubId(clubId);
				ClubJoinQuitMsgProto[] msgs = new ClubJoinQuitMsgProto[club.joinQuitMsgQueueProto.size()];
				club.joinQuitMsgQueueProto.toArray(msgs);
				for (ClubJoinQuitMsgProto msg : msgs) {
					builder.addEventMsgs(msg);
				}
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_EVENT_MSG_RSP, builder));
			}
		}, CLUB_REQ_DISBAND_TABLE(ClubRequestType.CLUB_REQ_DISBAND_TABLE_VALUE) { // 房主解散桌子

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {

				ClubRoomModel status = ClubService.getInstance()
						.disbandTable(request.getClubId(), request.getClubRuleId(), request.getJoinId(), topRequest.getClientSessionId());

				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc())));

			}
		}, CLUB_REQ_RULE_DETAIL(ClubRequestType.CLUB_REQ_RULE_DETAIL_VALUE) { // 玩法详情

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {

				Club club = ClubService.getInstance().getClub(request.getClubId());
				if (null == club) {
					return;
				}
				ClubRoomModel status = ClubService.getInstance().clubRuleDetail(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc())));
					return;
				}

				ClubProto.Builder clubProtoBuilder = (ClubProto.Builder) status.getAttament();
				ClubResponse.Builder b = ClubResponse.newBuilder();
				b.setType(ClubResponseType.CLUB_RSP_UPDATE_CLUB); // 客户端要求走更新，处理方便
				// b.setClubDetail(clubProtoBuilder);
				ClubUpdateProto.Builder updateBuilder = ClubUpdateProto.newBuilder().addAllClubRule(clubProtoBuilder.getClubRuleList())
						.setClubId(request.getClubId());
				updateBuilder.setType(2);
				if (request.hasField()) {
					b.setField(request.getField());
				}
				b.setClubUpdate(updateBuilder);
				b.setClubId(request.getClubId());

				Utils.sendClientClubResponse(b.build(), session, topRequest.getClientSessionId());
			}
		}, CLUB_REQ_CHAT(ClubRequestType.CLUB_REQ_CHAT_VALUE) { // 聊天

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubChat(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				}
			}
		}, CLUB_ONLINE_MEMBERS(ClubRequestType.CLUB_ONLINE_MEMBERS_VALUE) { // 在线成员

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubMemberStatus(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					ClubOnlineMemeberRsp.Builder builder = (ClubOnlineMemeberRsp.Builder) status.getAttament();
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_ONLINE_MEMBER, builder));
				}
			}
		}, CLUB_MEMBER_MARKER(ClubRequestType.CLUB_MEMBER_MARKER_VALUE) {// 成员备注

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubMemberRemark(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					Club club = ClubService.getInstance().getClub(request.getClubId());
					if (null == club) {
						return;
					}
					ClubMemberRemarkProto.Builder builder = (ClubMemberRemarkProto.Builder) status.getAttament();
					club.getManagerIds().forEach(managerId -> {
						SessionService.getInstance().sendClient(managerId, S2CCmd.CLUB_MEMBER_REMARK, builder);
					});
				}
			}

		}, CLUB_NOTICE(ClubRequestType.CLUB_NOTICE_VALUE) {// 公告设置

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance()
						.clubNoticeSets(request.getClubId(), topRequest.getClientSessionId(), request.getNotice());
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				}
			}

		}, CLUB_CHAT_HISTORY(ClubRequestType.CLUB_CHAT_HISTORY_VALUE) { // 聊天记录

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubChatHistory(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					ClubChatHistory.Builder builder = (ClubChatHistory.Builder) status.getAttament();
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_CHAT_HISTORY, builder));
				}
			}
		}, CLUB_TO_GROUP(ClubRequestType.CLUB_TO_GROUP_VALUE) {// 亲友圈成员同步到微信群

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				String groupId = request.getGroupId();
				int clubId = request.getClubId();
				OperateRsp.Builder builder = OperateRsp.newBuilder();
				if (clubId == 0 || StringUtils.isBlank(groupId)) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_TO_GROUP, builder));
					return;
				}
				Club club = ClubService.getInstance().getClub(clubId);
				if (club == null) {
					builder.setMsg("操作失败,亲友圈不存在");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_TO_GROUP, builder));
					return;
				}
				if (club.getOwnerId() != topRequest.getClientSessionId()) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_TO_GROUP, builder));
					return;
				}
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("groupId", groupId);
				map.put("clubId", clubId + "");
				Integer res = centerRMIServer.rmiInvoke(RMICmd.CLUB_TO＿GROUP, map);
				if (res != null && res == 0) {
					// res =
					// GroupClubMemberService.getInstance().GroupToClub(clubId,
					// groupId);
					// if(res == 0){
					builder.setMsg("操作成功");
					builder.setResult(1);
					// }else{
					// builder.setMsg("中心服操作成功，亲友圈服操作失败");
					// builder.setResult(0);
					// }
				} else {
					builder.setMsg("操作失败，请稍后再试试");
					builder.setResult(0);
				}
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_TO_GROUP, builder));
			}
		}, GROUP_TO_CLUB(ClubRequestType.GROUP_TO_CLUB_VALUE) {// 微信成语同步到亲友圈

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				String groupId = request.getGroupId();
				int clubId = request.getClubId();
				OperateRsp.Builder builder = OperateRsp.newBuilder();
				if (clubId == 0 || StringUtils.isBlank(groupId)) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(1);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_TO_CLUB, builder));
					return;
				}
				Club club = ClubService.getInstance().getClub(clubId);
				if (club == null) {
					builder.setMsg("操作失败,亲友圈不存在");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_TO_CLUB, builder));
					return;
				}
				if (club.getOwnerId() != topRequest.getClientSessionId()) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_TO_CLUB, builder));
					return;
				}
				// ICenterRMIServer centerRMIServer =
				// SpringService.getBean(ICenterRMIServer.class);
				// HashMap<String,String> map = new HashMap<String,String>();
				// map.put("groupId", groupId);
				// map.put("clubId", clubId+"");
				// Integer res = centerRMIServer.rmiInvoke(RMICmd.CLUB_TO＿GROUP,
				// map);
				// if(res!=null&&res==0){
				int res = GroupClubMemberService.getInstance().GroupToClub(clubId, groupId);
				if (res == 0) {
					builder.setMsg("操作成功");
					builder.setResult(1);
				} else {
					builder.setMsg("操作失败，请稍后再试试");
					builder.setResult(0);
				}
				// }else{
				// builder.setMsg("操作失败，请稍后再试试");
				// builder.setResult(1);
				// }
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_TO_CLUB, builder));
			}
		}, GROUP_REQ_LIST(ClubRequestType.GROUP_REQ_LIST_VALUE) {// 微信群列表

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				long accountId = request.getAccountId();
				ClubProto.Builder builder = ClubProto.newBuilder();
				builder.setHasAssistant(0);
				if (accountId == 0) {
					builder.addAllGroupMembersList(new ArrayList<GroupMembers>());
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_REQ_LIST, builder));
					return;
				}
				StringBuffer code = new StringBuffer();
				List<WxGroups> builderList = GroupClubMemberService.getInstance().getWxGroupsList(accountId, code, null);
				if (StringUtils.isNotBlank(code.toString())) {
					builder.setHasAssistant(1);
				}
				builder.addAllWxGroupsList(builderList);
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_REQ_LIST, builder));
			}
		}, CLUB_GROUP_INFO(ClubRequestType.CLUB_GROUP_INFO_VALUE) {// 亲友圈绑定的微信群

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubProto.Builder builder = ClubProto.newBuilder();
				int clubId = request.getClubId();
				Club club = ClubService.getInstance().getClub(clubId);
				// builder.setClubGroup(WxGroups.newBuilder().build());
				List<WxGroups> builderList = new ArrayList<>();
				if (club == null || club.groupSet.size() == 0) {
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_GROUP, builder));
					return;
				}
				StringBuffer code = new StringBuffer();
				String groupId = "";
				for (String str : club.groupSet) {
					groupId = str;
					break;
				}
				builderList = GroupClubMemberService.getInstance().getWxGroupsList(topRequest.getClientSessionId(), code, groupId);
				builder.addAllWxGroupsList(builderList);
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_GROUP, builder));
			}
		}, GROUP_MEMBER(ClubRequestType.GROUP_MEMBER_VALUE) {
			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				String groupId = request.getGroupId();
				int clubId = request.getClubId();
				ClubProto.Builder builder = ClubProto.newBuilder();
				if (clubId == 0 || StringUtils.isBlank(groupId)) {
					// builder.setMsg("操作失败，参数有误");
					// builder.setResult(1);
					builder.addAllGroupMembersList(new ArrayList<GroupMembers>());
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_MEMBER_LIST, builder));
					return;
				}
				List<GroupMembers> builderList = GroupClubMemberService.getInstance().getGroupMembersList(clubId, groupId);
				builder.addAllGroupMembersList(builderList);
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_MEMBER_LIST, builder));
			}
		}, BIND_GROUP(ClubRequestType.BIND_GROUP_VALUE) {
			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				String groupId = request.getGroupId();
				int clubId = request.getClubId();
				OperateRsp.Builder builder = OperateRsp.newBuilder();
				if (clubId == 0 || StringUtils.isBlank(groupId)) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.BIND_GROUP, builder));
					return;
				}
				int result = GroupClubMemberService.getInstance().bindGroup(groupId, clubId, topRequest.getClientSessionId());
				if (result == 1) {
					builder.setMsg("操作成功");
					builder.setResult(1);
				} else {
					if (result == Club.GROUP_ISBIND) {
						builder.setMsg("微信群已经绑定了亲友圈" + ClubService.getInstance().groupClubMaps.get(groupId));
					} else {
						builder.setMsg("绑定微信群失败");
					}
					builder.setResult(0);
				}
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.BIND_GROUP, builder));
			}
		}, UNBIND_GROUP(ClubRequestType.UNBIND_GROUP_VALUE) {
			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				String groupId = request.getGroupId();
				int clubId = request.getClubId();
				OperateRsp.Builder builder = OperateRsp.newBuilder();
				if (clubId == 0 || StringUtils.isBlank(groupId)) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.UNBIND_GROUP, builder));
					return;
				}
				int result = GroupClubMemberService.getInstance().unbindGroup(groupId, clubId, topRequest.getClientSessionId());
				if (result == 1) {
					builder.setMsg("操作成功");
					builder.setResult(1);
				} else {
					builder.setMsg("解绑微信群失败");
					builder.setResult(0);
				}
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.UNBIND_GROUP, builder));
			}
		}, GROUP_MEMBER_JOIN_CLUB(ClubRequestType.GROUP_MEMBER_JOIN_CLUB_VALUE) {
			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				String groupId = request.getGroupId();
				int clubId = request.getClubId();
				long accountId = request.getAccountId();
				OperateRsp.Builder builder = OperateRsp.newBuilder();
				if (clubId == 0 || StringUtils.isBlank(groupId) || accountId == 0) {
					builder.setMsg("操作失败，参数有误");
					builder.setResult(0);
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_MEMBER_JOIN_CLUB, builder));
					return;
				}
				int result = GroupClubMemberService.getInstance().GroupMemberToClub(clubId, groupId, accountId);
				if (result == 0) {
					builder.setMsg(accountId + "");
					builder.setResult(1);
				} else {
					builder.setMsg("添加用户失败");
					builder.setResult(0);
				}
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.GROUP_MEMBER_JOIN_CLUB, builder));
			}
		}, CLUB_FAST_JOIN(ClubRequestType.CLUB_FAST_JOIN_VALUE) {
			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {

				Club club = ClubService.getInstance().getClub(request.getClubId());
				if (null == club) {
					return;
				}
				ClubRoomModel roomModel = ClubService.getInstance().clubFastJoin(request, topRequest, session);
				if (null == roomModel) {
					return;
				}
				if (roomModel.getStatus() != Club.SUCCESS) {
					final String msg = Strings.isNullOrEmpty(roomModel.getDesc()) ? "创建房间失败!" : roomModel.getDesc();
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), msg, ESysMsgType.INCLUDE_ERROR)));
				}
			}
		}, CLUB_TODAY_RECORD(ClubRequestType.CLUB_TODAY_RECORD_VALUE) {// 列表

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				int clubId = request.getClubId();
				Club club = ClubService.getInstance().getClub(clubId);
				if (club == null) {
					return;
				}
				ClubTodayCostProto.Builder b = ClubTodayCostProto.newBuilder();
				b.setClubId(clubId);
				b.setDailyCount(club.gameCount);
				b.setDailyGold(club.costGold);
				session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_TODAY_RECORD, b));
			}
		}, CLUB_RULE_MARKER(ClubRequestType.CLUB_RULE_MARKER_VALUE) {// 包间备注

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubRuleRemark(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					Club club = ClubService.getInstance().getClub(request.getClubId());
					if (null == club) {
						return;
					}
					ClubRuleRemarkProto.Builder builder = (ClubRuleRemarkProto.Builder) status.getAttament();
					Utils.sendClient(club.getManagerIds(), S2CCmd.CLUB_RULE_REMARK, builder);
				}
			}

		},

		CLUB_ADD_MEMBER(ClubRequestType.CLUB_ADD_MEMBER_VALUE) {// 添加成员

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubAddMember(request, topRequest);
				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
			}

		}, CLUB_SET_MANAGER(ClubRequestType.CLUB_SET_MANAGER_VALUE) {// 设置管理员

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubSetManager(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					ClubCommonLIIProto.Builder builder = (ClubCommonLIIProto.Builder) status.getAttament();
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_DEFRIEND, builder));
				}
			}

		}, CLUB_DEFRIEND(ClubRequestType.CLUB_DEFRIEND_VALUE) {// 拉黑

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubDeFriend(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					ClubCommonLIIProto.Builder builder = (ClubCommonLIIProto.Builder) status.getAttament();
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_DEFRIEND, builder));
				}
			}

		}, CLUB_MEMBER_LIST(ClubRequestType.CLUB_MEMBER_LIST_VALUE) {// 成员列表

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubMemberList(request, topRequest);
				if (status.getStatus() != Club.SUCCESS) {
					session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
							Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
				} else {
					ClubMemberListProto.Builder builder = (ClubMemberListProto.Builder) status.getAttament();
					session.send(PBUtil.toS_S2CRequet(topRequest.getClientSessionId(), S2CCmd.CLUB_MEMBER_LIST, builder));
				}
			}

		}, CLUB_KICK_PLAYER(ClubRequestType.CLUB_KICK_PLAYER_VALUE) {// 踢玩家

			@Override
			protected void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
				ClubRoomModel status = ClubService.getInstance().clubKickPlayer(request, topRequest, ClubTableKickOutType.ACTIVE_KICK);
				session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP,
						Utils.getMsgToCLubResponse(topRequest.getClientSessionId(), status.getDesc(), ESysMsgType.INCLUDE_ERROR)));
			}
		},
		;

		private ClubRequestType value;

		ClubRqType(int value) {
			this.value = ClubRequestType.valueOf(name());
			Preconditions.checkNotNull(this.value, "亲友圈子协议找不到  %s", value);
		}

		protected abstract void exe(ClubRequest request, ProxyClubRq topRequest, C2SSession session);
	}

}
