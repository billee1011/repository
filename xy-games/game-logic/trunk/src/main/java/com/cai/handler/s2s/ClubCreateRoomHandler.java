/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerType;
import com.cai.common.handler.IServerHandler;
import com.cai.domain.Session;
import com.cai.handler.LogicRoomHandler;
import com.cai.service.SessionServiceImpl;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.s2s.ClubServerProto.ClubCreateRoomNewProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年3月31日 下午11:53:36 <br/>
 */
@IServerCmd(code = S2SCmd.CREATE_CLUB_ROOM_RSP, desc = "club<->logic创建房间")
public class ClubCreateRoomHandler extends IServerHandler<ClubCreateRoomNewProto> {

	@Override
	public void execute(ClubCreateRoomNewProto request, S2SSession session) throws Exception {
		LogicRoomRequest logicReq = request.getLogicRoomRequest();
		Session proxy = SessionServiceImpl.getInstance().getSession(EServerType.PROXY, request.getProxyServerId());
		if (null == proxy) {
			proxy = SessionServiceImpl.getInstance().getRandomSession(EServerType.PROXY);
		}
		if (null == proxy) {
			return;
		}

		if (logicReq.getType() == 1) {
			boolean ret = LogicRoomHandler.handler_player_create_room(logicReq, GameConstants.CREATE_ROOM_NORMAL, logicReq.getRoomId(), proxy);
			if (!ret) {
				logger.error("1 club[id:{} ,roomid:{} ] create room type66 failed!", logicReq.getRoomRequest().getClubId(), logicReq.getRoomId());
			}
		} else if (logicReq.getType() == 66) {
			boolean ret = LogicRoomHandler.handler_player_create_room(logicReq, GameConstants.CREATE_ROOM_PROXY, 0, proxy);
			if (ret) {
				ret = LogicRoomHandler.handler_join_room(logicReq.toBuilder().setType(56).build(), logicReq.getRoomId(), proxy);
				if (!ret) {
					logger.error("2 club[id:{} ,roomid:{} ] create room type66 failed!", logicReq.getRoomRequest().getClubId(), logicReq.getRoomId());
				}
			} else {
				logger.error("3 club[id:{} ,roomid:{} ] create room type66 failed!", logicReq.getRoomRequest().getClubId(), logicReq.getRoomId());
			}

		} else if (logicReq.getType() == 77) {
			boolean ret = LogicRoomHandler.handler_create_club_match_room(logicReq);
			if (!ret) {
				logger.error("4 club[id:{} ,roomid:{}, clubMatchId:{} ] create room type77 failed!", logicReq.getRoomRequest().getClubId(),
						logicReq.getRoomId(), logicReq.getClubMatchId());
			}
		} else {
			logger.error("=========  不识别type:{} ==============", logicReq.getType());
		}
	}
}
