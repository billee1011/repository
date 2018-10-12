/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler.c2s;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountMatchInfoRedis;
import com.cai.common.domain.AccountMatchRedis;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.type.MatchType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamDict;
import com.cai.module.RoomModule;
import com.cai.redis.service.RedisService;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientRequest;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;
import protobuf.clazz.match.MatchClientRsp.MatchEnterRequest;
import protobuf.clazz.match.MatchClientRsp.MatchEnterResponse;
import protobuf.clazz.match.MatchClientRsp.MatchLeaveRequest;
import protobuf.clazz.match.MatchClientRsp.MatchRankRequest;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerAllocationRequest;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerListRequest;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerListResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerMsg;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerOperationRequest;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerTableRequest;
import protobuf.clazz.match.MatchClientRsp.RequestTopRoundRecord;
import protobuf.clazz.match.MatchRsp.MatchClientRequestWrap;
import protobuf.clazz.match.MatchRsp.MatchS2SCmd;
import protobuf.clazz.match.MatchRsp.MatchS2SRequest;

/**
 * 
 */
@ICmd(code = C2SCmd.MATCH)
public class MatchHandler extends IClientHandler<MatchClientRequest> {
	private static final Logger logger = LoggerFactory.getLogger(MatchHandler.class);

	@Override
	protected void execute(MatchClientRequest request, Request topRequest, C2SSession session) throws Exception {

		Account account = session.getAccount();
		if (account == null)
			return;
		
		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
			msgBuilder.setType(ESysMsgType.NONE.getId());
			msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
			responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
			session.send(responseBuilder.build());
			return;
		}
		
		AccountMatchRedis accountMatchRedis = null;
		AccountMatchInfoRedis matchInfoRedis = null;
		int matchId = 0;
		int id = 0;
		boolean isSuc = false;
		MatchClientResponse.Builder matchResponse = null;
		// 不需要俱乐部服处理的就在这处理
		switch (request.getCmd()) {
		case MatchCmd.C2S_MATCH_APPLY:// 请求比赛相关游戏规则
			if (account.getNextEnterRoomTime() >= System.currentTimeMillis()) {
				session.send(MessageResponse.getMsgAllResponse("操作过于频繁").build());
				return;
			}

			RoomRedisModel roomRedis = RoomModule.getRoomRedisModelIfExsit(account, session);

			if (roomRedis != null) {
				session.send(MessageResponse.getMsgAllResponse("由于您已在其他房间开始牌局,无法报名当前的比赛!").build());
				return;
			}

			account.resetNextEnterRoomTime();
			break;
		case MatchCmd.C2S_MATCH_ENTER_MATCH://转发到逻辑服--请求重连进入房间

			if (!GbCdCtrl.canHandle(session, Opt.C2S_MATCH_ENTER_MATCH))
				return;
			
			MatchEnterRequest enterRequest = MatchEnterRequest.parseFrom(request.getData());
			matchId = enterRequest.getMatchId();

			accountMatchRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.MATCH_ROOM_ACCOUNT,
					account.getAccount_id() + "", AccountMatchRedis.class);
			if (accountMatchRedis == null) {
				 session.send(MessageResponse.getMsgAllResponse("未找到相关数据进入失败").build());
				return;
			}
			
			matchInfoRedis = accountMatchRedis.getMatchInfo(matchId);
			if(matchInfoRedis == null){
				return;
			}
			
			id = matchInfoRedis.getId();
			if(id <= 0){
				removeMatchInfo(accountMatchRedis, account.getAccount_id(), matchId);
				logger.error("enter match fail [id] accountId:{} matchId:{} id:{} !!",account.getAccount_id(),matchId,id);
				sendEnterResp(session, matchId, MatchType.NO_ENTER_REMOVE, "未找到相关比赛,进入失败");
				return;
			}
			
			if (!matchInfoRedis.isStart()) {
				session.send(MessageResponse.getMsgAllResponse("比赛未开始进入失败").build());
				logger.error("enter match fail [isStart] accountId:{} matchId:{} id:{} !!",account.getAccount_id(),matchId,id);
//				removeMatchInfo(accountMatchRedis, account.getAccount_id(), matchId);
//				sendEnterResp(session, matchId, MatchType.NO_ENTER_REMOVE, "比赛未开始,进入失败");
				return;
			}

			ClientServiceImpl.getInstance().sendMsg(matchInfoRedis.getLogicIndex(), getS2SReq(request, account.getAccount_id(), id));
			return;
		case MatchCmd.C2S_MATCH_LEAVE: //转发到逻辑服--把房间清理掉  退赛

			if (!GbCdCtrl.canHandle(session, Opt.C2S_MATCH_LEAVE))
				return;
			
			MatchLeaveRequest leaveRequest = MatchLeaveRequest.parseFrom(request.getData());
			matchId = leaveRequest.getMatchId();
			
			accountMatchRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.MATCH_ROOM_ACCOUNT,
					account.getAccount_id() + "", AccountMatchRedis.class);
			if (accountMatchRedis == null) {
				session.send(MessageResponse.getMsgAllResponse("未找到相关数据退出失败").build());
				return;
			}
			matchInfoRedis = accountMatchRedis.getMatchInfo(matchId);
			if(matchInfoRedis == null){
				return;
			}
			
			id = matchInfoRedis.getId();
			if (id <= 0 || !matchInfoRedis.isStart()) {
				session.send(MessageResponse.getMsgAllResponse("比赛未开始退出失败").build());
				return;
			}
			ClientServiceImpl.getInstance().sendMsg(matchInfoRedis.getLogicIndex(), getS2SReq(request, account.getAccount_id(), id));
			return;
		case MatchCmd.MATCH_GMAE_RANK://转发到逻辑服--比赛服内排名
			if (account.getNextEnterRoomTime() >= System.currentTimeMillis()) {
				session.send(MessageResponse.getMsgAllResponse("操作过于频繁").build());
				return;
			}
			MatchRankRequest rankRequest = MatchRankRequest.parseFrom(request.getData());
			matchId = rankRequest.getMatchId();
			
			sendToLogicServer(request, account, matchId);
			account.resetNextEnterRoomTime();
			return;
		case MatchCmd.MATCH_WINNER_OPERATION://转发到逻辑服--管理员对比赛的相关管理
			MatchWinnerOperationRequest winnerOpRequest = MatchWinnerOperationRequest.parseFrom(request.getData());
			matchId = winnerOpRequest.getMatchId();
			sendToLogicServer(request, account, matchId);
			return;
		case MatchCmd.MATCH_WINNER_ALLOCATION://转发到逻辑服--管理员对比赛进行配桌
			MatchWinnerAllocationRequest winnerAlloRequest = MatchWinnerAllocationRequest.parseFrom(request.getData());
			matchId = winnerAlloRequest.getMatchId();
			sendToLogicServer(request, account, matchId);
			return;
		case MatchCmd.MATCH_WINNER_LIST: //转发到逻辑服--管理员对比赛的相关人员
			MatchWinnerListRequest winnerListRequest = MatchWinnerListRequest.parseFrom(request.getData());
			matchId = winnerListRequest.getMatchId();
			isSuc = sendToLogicServer(request, account, matchId);
			if(!isSuc){
				MatchWinnerListResponse.Builder winerListResp = MatchWinnerListResponse.newBuilder();
				winerListResp.setIsAllocation(false);
				winerListResp.setTableStatus(MatchType.TABLE_OVER);
				winerListResp.setTableNum(0);
				winerListResp.addAllMsgs(new ArrayList<MatchWinnerMsg>());
				
				matchResponse = MatchClientResponse.newBuilder();
				matchResponse.setCmd(MatchCmd.MATCH_WINNER_LIST);
				matchResponse.setData(winerListResp.build().toByteString());

				session.send(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, matchResponse).build());
			}
			return;
		case MatchCmd.MATCH_TOP_ROUND_RECORD://转发到逻辑服--
			RequestTopRoundRecord topRoundRequest = RequestTopRoundRecord.parseFrom(request.getData());
			matchId = topRoundRequest.getMatchId();
			sendToLogicServer(request, account, matchId);
			return;
		case MatchCmd.MATCH_WINNER_TABLE_LIST:
			MatchWinnerTableRequest winnerTableRequest = MatchWinnerTableRequest.parseFrom(request.getData());
			matchId = winnerTableRequest.getMatchId();
			isSuc = sendToLogicServer(request, account, matchId);
			if(isSuc){
				return;
			}
		}

		MatchS2SRequest.Builder b = MatchS2SRequest.newBuilder();
		b.setCmd(MatchS2SCmd.S2S_MATCH_CLIENT);

		MatchClientRequestWrap.Builder wrap = MatchClientRequestWrap.newBuilder();
		wrap.setAccountId(session.getAccountID());
		wrap.setRequest(request);
		wrap.setProxyIndex(SystemConfig.proxy_index);
		b.setClientRequest(wrap);
		//转发到比赛场的---MatchServerHandler
		boolean result = ClientServiceImpl.getInstance().sendMatch(SystemConfig.match_index, PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER, b.build()).build());
		if (!result) {
			session.send(MessageResponse.getMsgAllResponse("比赛场正在例行维护中，请稍后再试！").build());
		}
	}

	private boolean sendToLogicServer(MatchClientRequest request, Account account, int matchId) {
		AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.MATCH_ROOM_ACCOUNT,
				account.getAccount_id() + "", AccountMatchRedis.class);
		if (accountMatchRedis == null) {
			return false;
		}
		AccountMatchInfoRedis matchInfoRedis = accountMatchRedis.getMatchInfo(matchId);
		if(matchInfoRedis == null){
			return false;
		}
		
		int id = matchInfoRedis.getId();
		if (id <= 0 || !matchInfoRedis.isStart()) {
			return false;
		}
		ClientServiceImpl.getInstance().sendMsg(matchInfoRedis.getLogicIndex(), getS2SReq(request, account.getAccount_id(), id));
		return true;
	}
	
	private void removeMatchInfo(AccountMatchRedis accountMatchRedis,long accountId,int matchId){
		accountMatchRedis.removeMatchInfo(matchId);
		SpringService.getBean(RedisService.class).hSet(RedisConstant.MATCH_ROOM_ACCOUNT, accountId + "", accountMatchRedis);
	}
	
	private void sendEnterResp(C2SSession session,int matchId,int status,String msg){
		MatchEnterResponse.Builder response = MatchEnterResponse.newBuilder();
		response.setMatchId(matchId);
		response.setStatus(status);
		response.setMsg(msg);
		
		MatchClientResponse.Builder matchResponse = MatchClientResponse.newBuilder();
		matchResponse.setCmd(MatchCmd.S2C_MATCH_ENTER);
		matchResponse.setData(response.build().toByteString());
		
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, matchResponse).build());
	}

	private Request getS2SReq(MatchClientRequest request, long accountId,int matchId) {
		MatchS2SRequest.Builder b = MatchS2SRequest.newBuilder();
		b.setCmd(MatchS2SCmd.S2S_MATCH_CLIENT);

		MatchClientRequestWrap.Builder wrap = MatchClientRequestWrap.newBuilder();
		wrap.setAccountId(accountId);
		wrap.setRequest(request);
		wrap.setId(matchId);
		wrap.setProxyIndex(SystemConfig.proxy_index);
		b.setClientRequest(wrap);
		return PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER, b.build()).build();
	}

}
