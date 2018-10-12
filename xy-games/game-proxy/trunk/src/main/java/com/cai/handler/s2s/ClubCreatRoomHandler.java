/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.domain.Account;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.handler.IServerHandler;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.RedisKeyUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamDict;
import com.cai.module.ClubMsgSender;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.RoomUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.s2s.ClubServerProto.ClubCreateRoom;

/**
 * 
 */
//@IServerCmd(code = S2SCmd.CREATE_CLUB_ROOM_RSP, desc = "俱乐部开房,如果有围观，1现走代开房，2再走成为围观者")
public class ClubCreatRoomHandler extends IServerHandler<ClubCreateRoom> {

	@Override
	public void execute(ClubCreateRoom resp, S2SSession session) throws Exception {
		C2SSession client_ = C2SSessionService.getInstance().getSession(resp.getClientSessionId());
		if (client_ == null) {

			// 如果玩家在创建过程中有离线/断线，替他走完创房流程，保证创建成功
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAccount(resp.getClientSessionId());
			account.setWorkerLoop(C2SSessionService.getInstance().getWorkerGroup().next());
			client_ = new C2SSession(null);
			client_.setAccount(account);

			logger.warn("俱乐部调试日志:玩家[{}]在俱乐部[{},{}]创建了房间[{}]，但过程中离线了!", account, resp.getClubId(), resp.getClubName(), resp.getRoomId());
		}

		final C2SSession client = client_;

		/* client.getAccount().getWorkerLoop() */ClubMsgSender.worker(resp.getClubId()).runInLoop(new Runnable() {
			@Override
			public void run() {
				Account account = client.getAccount();
				if (account == null) {
					return;
				}

				if (RoomUtil.getRoomId(account.getAccount_id()) > 0) {
					logger.error("玩家[{}]尝试进入俱乐部房间失败,msg[{}]!", account, resp);

					// 创房者已经有房间了，创建失败，清理
					RedisService redisService = SpringService.getBean(RedisService.class);
					redisService.hDel(RedisKeyUtil.clubRoomKey(resp.getClubId(), resp.getClubRule().getId()),
							Integer.toString((resp.getJoinId() & 0xFFFF0000) >> 16));
					return;
				}
				SessionUtil.setLogicSvrId(client, resp.getLogicId(), resp.getRoomId());

				boolean isObserverGame = SysParamDict.getInstance().isObserverGameTypeIndex(resp.getClubRule().getGameTypeIndex());
				if (isObserverGame) {
					proxyCreateRoomAction(resp, client, 66);
				} else {
					proxyCreateRoomAction(resp, client, 1);
				}

				// GAME-TODO 临时修改，默认第一个进去直接坐下
				// proxyCreateRoomAction(resp, client, 1);
			}
		});
	}

	/**
	 * 
	 * @param resp
	 * @param session
	 */
	private void proxyCreateRoomAction(ClubCreateRoom resp, C2SSession client, int logicRequestCode) {
		Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, client);
		LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
		logicRoomRequestBuilder.setType(logicRequestCode); // 2 or 51
		RoomRequest.Builder b = RoomRequest.newBuilder();

		b.setRuleId(resp.getClubRule().getId());
		b.setClubId(resp.getClubId());
		b.setClubName(resp.getClubName());
		b.setAppId(resp.getClubRule().getGameTypeIndex());
		b.setGameRound(resp.getClubRule().getGameRound());
		b.setGameTypeIndex(resp.getClubRule().getGameTypeIndex());
		b.setNewRules(resp.getClubRule().getRules());
		b.setClubMemberSize(resp.getClubMemberSize());
		logicRoomRequestBuilder.setRoomRequest(b.build());
		logicRoomRequestBuilder.setAccountId(client.getAccount().getAccount_id());
		logicRoomRequestBuilder.setRoomId(resp.getRoomId());
		LogicRoomAccountItemRequest.Builder accountItemB = MessageResponse.getLogicRoomAccountItemRequest(client);
		accountItemB.setClubOwner(resp.getClubOwnerId());
		accountItemB.setJoinId(resp.getJoinId());
		accountItemB.setProxyIndex(SystemConfig.proxy_index);
		logicRoomRequestBuilder.setLogicRoomAccountItemRequest(accountItemB);

		// 优化，如果俱乐部创始人也在当前服务器中，直接用缓存数据 就可以，不用走RMI获取其数据
		PlayerViewVO clubOwner = null;

		C2SSession owenerSession = C2SSessionService.getInstance().getSession(resp.getClubOwnerId());
		if (null != owenerSession) {
			Account clubOwnerAccount = owenerSession.getAccount();
			if (null != clubOwnerAccount) {
				clubOwner = new PlayerViewVO();
				clubOwner.setAccountId(clubOwnerAccount.getAccount_id());
				clubOwner.setGold(clubOwnerAccount.getAccountModel().getGold());
				clubOwner.setHead(clubOwnerAccount.getIcon());
				clubOwner.setMoney(clubOwnerAccount.getAccountModel().getMoney());
				clubOwner.setNickName(clubOwnerAccount.getNickName());
			}
		}

		if (null == clubOwner) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			clubOwner = centerRMIServer.getPlayerViewVo(resp.getClubOwnerId());
		}

		accountItemB = LogicRoomAccountItemRequest.newBuilder();
		accountItemB.setAccountId(resp.getClubOwnerId());
		accountItemB.setAccountIcon(clubOwner.getHead());
		accountItemB.setNickName(clubOwner.getNickName());
		accountItemB.setGold(clubOwner.getGold());
		accountItemB.setMoney(clubOwner.getMoney());
		accountItemB.setProxySessionId(resp.getClubOwnerId());
		accountItemB.setJoinId(resp.getJoinId());
		accountItemB.setProxyIndex(SystemConfig.proxy_index);
		logicRoomRequestBuilder.setClubOwnerAccount(accountItemB);

		requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());

		ClientServiceImpl.getInstance().sendMsg(resp.getLogicId(), requestBuider.build());
	}
}
