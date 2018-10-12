/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s.coin;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.Account;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.type.CoinType;
import com.cai.common.util.PBUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.SysParamDict;
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
import protobuf.clazz.coin.CoinProtocol.GameMatchRequest;
import protobuf.clazz.coin.CoinProtocol.GameMatchResponse;
import protobuf.clazz.coin.CoinProtocol.MessageTip;
import protobuf.clazz.coin.CoinServerProtocol.S2SGameMatchRequest;


@ICmd(code = C2SCmd.COIN_GAME_MATCH, desc = "金币场请求匹配")
public final class GameMatchHandler extends IClientHandler<GameMatchRequest> {
	
	@Override
	protected void execute(GameMatchRequest req, Request topRequest, C2SSession session) throws Exception {
		
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
		
		long accountId = account.getAccount_id();
		int gameId = req.getGameId();
		int detailId = req.getDetailId();
		int opType = req.getOpType();
		
		int status = -1; //1成功  2白名单
		switch (opType) {
		case CoinType.OP_START:
		case CoinType.OP_CONTINUE:
			CoinGameDetail detail = CoinDict.getInstance().getGameDetail(gameId, detailId);
			if(detail != null){
				status = 1;
				if(!detail.isWhiteAccountId(accountId)){
					status = 2;
				}
			}
			break;
		case CoinType.OP_QUICK_START:
			List<CoinGameDetail> details = CoinDict.getInstance().getGameDetails(gameId);
			if(details != null && details.size() > 0){
				status = 1;
			}
			break;
		}
		
		GameMatchResponse.Builder resposne = null;
		if(status == -1){
			resposne = GameMatchResponse.newBuilder();
			MessageTip.Builder tip = MessageTip.newBuilder();
			tip.setValue(CoinType.FAIL);
			tip.setTip("请求金币场匹配失败!!");
			resposne.setTip(tip.build());
			session.send(PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_MATCH, resposne).build());
			return;
		}else if(status == 2){
			resposne = GameMatchResponse.newBuilder();
			MessageTip.Builder tip = MessageTip.newBuilder();
			tip.setValue(CoinType.FAIL);
			tip.setTip("非白名单用户,请求金币场匹配失败!!");
			resposne.setTip(tip.build());
			session.send(PBUtil.toS2CCommonRsp(C2SCmd.COIN_GAME_MATCH, resposne).build());
			return;
		}
		
		S2SGameMatchRequest.Builder request = S2SGameMatchRequest.newBuilder();
		request.setAccountId(session.getAccountID());
		request.setMatchReq(req);
		
		boolean result = ClientServiceImpl.getInstance().sendToCoin(SystemConfig.connectCoin, 
				PBUtil.toS2SRequet(C2SCmd.COIN_GAME_MATCH, request).build());
		if (!result) {
			session.send(MessageResponse.getMsgAllResponse("金币场维护中，请稍后再试！").build());
		}
	}
	
}
