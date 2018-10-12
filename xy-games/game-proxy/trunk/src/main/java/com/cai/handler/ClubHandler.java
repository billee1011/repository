/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.ClubHonourRecordRunnable;
import com.cai.future.ClubReqRecordRunnable;
import com.cai.module.RoomModule;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.base.Preconditions;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.ClubMsgProto.ClubRequest.ClubRequestType;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.ClubMsgProto.ClubUpdateProto;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.s2s.ClubServerProto.ProxyClubRq;
import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

/**
 */
@ICmd(code = RequestType.CLUB_VALUE, exName = "clubRequest")
public class ClubHandler extends IClientHandler<ClubRequest> {
	final static HashMap<ClubRequestType, ClubRqType> RQ_MAPS = new HashMap<>(ClubRqType.values().length, 1);

	protected static final Logger logger = LoggerFactory.getLogger(ClubHandler.class);

	static {
		ClubRqType[] temp = ClubRqType.values();
		for (int i = 0; i < temp.length; i++) {
			RQ_MAPS.put(temp[i].value, temp[i]);
		}
	}

	@Override
	protected void execute(ClubRequest request, Request topRequest, C2SSession session) throws Exception {

		final Account account = session.getAccount();
		if (account == null)
			return;

		ClubRqType type = RQ_MAPS.get(request.getType());
		if (type != null) {
			if (type.exe(request, topRequest, session)) {
				return;
			}
		}

		ProxyClubRq.Builder rq = ProxyClubRq.newBuilder();
		rq.setClubRq(request);
		if (request.getType() == ClubRequestType.CLUB_REQ_CREATE_ROOM || request.getType() == ClubRequestType.CLUB_FAST_JOIN) {
			AccountModel model = account.getAccountModel();
			if (null != model) {
				rq.setAccountIp(model.getClient_ip2());
			}

			// 创建/加入 房间时带上请求者个人信息
			LogicRoomAccountItemRequest.Builder accountItemPB = MessageResponse.getLogicRoomAccountItemRequest(session);
			accountItemPB.setJoinId(request.getJoinId());
			rq.setRequestAccountProto(accountItemPB);
		}
		rq.setClientSessionId(account.getAccount_id());
		boolean result = ClientServiceImpl.getInstance().sendClub(1, PBUtil.toS2SRequet(S2SCmd.CLUB_REQ, rq.build()).build());
		if (SystemConfig.gameDebug == 1 && !result) {
			session.send(MessageResponse.getMsgAllResponse("亲友圈功能正在例行维护中，请稍后再试！").build());
		}
	}

	enum ClubRqType {
		CLUB_REQ_CREATE_ROOM(4) {
			@Override
			protected boolean exe(ClubRequest request, Request topRequest, C2SSession session) {
				Account account = session.getAccount();

				if (account.getNextEnterRoomTime() >= System.currentTimeMillis()) {
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.MSG);
					MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
					msgBuilder.setType(ESysMsgType.NONE.getId());
					msgBuilder.setMsg("操作太频繁");
					responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
					session.send(responseBuilder.build());
					return true;
				}

				account.resetNextEnterRoomTime();

				SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
				if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.MSG);
					MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
					msgBuilder.setType(ESysMsgType.NONE.getId());
					msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
					responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
					session.send(responseBuilder.build());
					return true;
				}
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

				RsSystemStopReadyStatusResponse rsSystemStopReadyStatusResponse = centerRMIServer.systemStopReadyStatus();
				if (rsSystemStopReadyStatusResponse.getSystemStopReady()) {
					session.send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能进入房间,请等待服务器停机维护完成再登录!").build());
					return true;
				}

				RoomRedisModel roomRedisModel = RoomModule.getRoomRedisModelIfExsit(account, session);
				// int source_room_id =
				// RoomUtil.getRoomId(account.getAccount_id());
				if (roomRedisModel != null) {
					int source_room_id = roomRedisModel.getRoom_id();
					// session.send(MessageResponse.getMsgAllResponse("你有其他房间，无法组局").build());
					logger.error("玩家[{}]你有其他房间，无法组局{}", account.getAccount_id(), source_room_id);
					int loginc_index = roomRedisModel.getLogic_index();
					if (loginc_index <= 0) {
						logger.error("玩家[{}]请求加入房间 ，但房间对应的处理逻辑服不存在,{}", loginc_index);
						return false;
					}
					SessionUtil.setLogicSvrId(session, loginc_index, source_room_id);
					Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
					LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
					logicRoomRequestBuilder.setType(3);
					logicRoomRequestBuilder.setRoomRequest(RoomRequest.newBuilder().setRoomId(source_room_id));
					logicRoomRequestBuilder.setRoomId(source_room_id);
					logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
					requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
					boolean flag = ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
					if (!flag) {
						session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
					}
					return true;
				}

				return false;

			}
		},
		CLUB_REQ_RECORD(5) {
			@Override
			protected boolean exe(ClubRequest request, Request topRequest, C2SSession session) {
				if (!GbCdCtrl.canHandleMust(session, Opt.CLUB_REQ_RECORD))
					return true;

				Global.getGameDispatchService().execute(new ClubReqRecordRunnable(request, session.getAccountID()));
				return true;
			}
		},

		CLUB_REQ_CREATE_CLUB(6) {
			@Override
			protected boolean exe(ClubRequest request, Request topRequest, C2SSession session) {

				for (ClubRuleProto ruleProto : request.getCreatClub().getClubRuleList()) {
					if (GameGroupRuleDict.getInstance().getBySubId(ruleProto.getGameTypeIndex()) == null) {
						session.send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能进入房间,请等待服务器停机维护完成再登录!").build());
						return true;
					}
				}

				return false;
			}
		},
		CLUB_REQ_UPDATE_CLUB(7) {// 修改俱乐部
			@Override
			protected boolean exe(ClubRequest request, Request topRequest, C2SSession session) {
				ClubUpdateProto clubProto = request.getClubUpdate();
				if (clubProto.getType() == 2 || clubProto.getType() == 3) {
					for (ClubRuleProto ruleProto : clubProto.getClubRuleList()) {
						if (GameGroupRuleDict.getInstance().getBySubId(ruleProto.getGameTypeIndex()) == null) {
							session.send(MessageResponse.getMsgAllResponse("该玩法暂未开放，敬请期待").build());
							return true;
						}
					}
				}
				return false;
			}

		},
		CLUB_HONOUR_RECORD(ClubRequestType.CLUB_HONOUR_RECORD_VALUE) { // 荣耀

			@Override
			protected boolean exe(ClubRequest request, Request topRequest, C2SSession session) {

				if (!GbCdCtrl.canHandleMust(session, Opt.CLUB_HONOUR_RECORD))
					return true;

				Global.getGameDispatchService().execute(new ClubHonourRecordRunnable(request, session.getAccountID()));

				return true;
			}
		};

		private ClubRequestType value;

		ClubRqType(int value) {
			this.value = ClubRequestType.valueOf(value);
			Preconditions.checkNotNull(this.value, "俱乐部子协议找不到  %s", value);
		}

		protected abstract boolean exe(ClubRequest request, Request topRequest, C2SSession session);
	}

}
