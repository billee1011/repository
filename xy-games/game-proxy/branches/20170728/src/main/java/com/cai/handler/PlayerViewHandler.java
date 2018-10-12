/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import com.cai.common.domain.PlayerViewVO;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.domain.Session;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientHandler;

import io.netty.util.AttributeKey;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.PlayerViewRequest;
import protobuf.clazz.Protocol.PlayerViewResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.PLAYER_VIEW_VALUE, exName = "playerViewRequest")
public class PlayerViewHandler extends IClientHandler<PlayerViewRequest> {

	/**
	 * 该命令需要查询数据库，限制玩家请求频率
	 */
	private static final AttributeKey<Integer> REQ_TIME_KEY = AttributeKey.valueOf("TEst_REQ_TIME_KEY");

	/**
	 * 10s CD
	 */
	private static final int REQ_CD_SECOND = 1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(PlayerViewRequest request, Request topRequest, Session session) throws Exception {
		// 1,accountId 简单检测
		long accountId = request.getAccountId();
		if (accountId <= 0) {
			return;
		}

		// 2,请求频率检测
		Integer lastReqTime_Second = session.attr(REQ_TIME_KEY).get();
		if (null != lastReqTime_Second
				&& (System.currentTimeMillis() / 1000) - lastReqTime_Second.intValue() < REQ_CD_SECOND) {
			logger.warn("player:{} 查询其他玩家数据过于频繁!", session.getAccount());
			return;
		}

		// 3,设置此次请求时间
		session.attr(REQ_TIME_KEY).set((int) (System.currentTimeMillis() / 1000));

		// 4,通过RMI从中心服获得玩家数据
		PlayerViewVO vo = SpringService.getBean(ICenterRMIServer.class).getPlayerViewVo(accountId);
		if (null == vo) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session,
					MessageResponse.getMsgAllResponse("不存在该玩家!").build());
			return;
		}

		// 5,填充，回复
		PlayerViewResponse.Builder viewRspBuilder = PlayerViewResponse.newBuilder();
		viewRspBuilder.setAccountId(accountId);
		viewRspBuilder.setHead(vo.getHead());
		viewRspBuilder.setNickName(vo.getNickName());
		viewRspBuilder.setGold(vo.getGold());
		viewRspBuilder.setMoney(vo.getMoney());
		viewRspBuilder.setSignature(vo.getSignature());
		viewRspBuilder.setSex(vo.getSex());
		viewRspBuilder.setVipLv(vo.getVipLv());
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.PLAYER_VIEW);
		responseBuilder.setExtension(Protocol.playerViewResponse, viewRspBuilder.build());

		session.send(responseBuilder.build());
	}

}
