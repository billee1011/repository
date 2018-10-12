/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

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
@IServerCmd(code = S2SCmd.CREATE_ENTER_ROOM_RSP, desc = "club<->logic加入房间")
public class ClubJoinRoomHandler extends IServerHandler<ClubCreateRoomNewProto> {

	@Override
	public void execute(ClubCreateRoomNewProto request, S2SSession session) throws Exception {

		LogicRoomRequest logicReq = request.getLogicRoomRequest();
		Session proxy = SessionServiceImpl.getInstance().getSession(EServerType.PROXY, request.getProxyServerId());
		if (null == proxy) {
			logger.warn("club[id:{} ,roomid:{} ] join  type:{} failed,proxy[{}] session is nil!!", logicReq.getRoomRequest().getClubId(),
					logicReq.getRoomId(), logicReq.getType(), request.getProxyServerId());
			return;
		}

		if (logicReq.getType() == 2 || logicReq.getType() == 56) {
			boolean ret = LogicRoomHandler.handler_join_room(logicReq, logicReq.getRoomId(), proxy);
			if (!ret) {
				logger.warn("club[id:{} ,roomid:{} ] join  type:{} failed!", logicReq.getRoomRequest().getClubId(), logicReq.getRoomId(),
						logicReq.getType());
			}
		} else {
			logger.warn("=========  不识别type:{} ==============", logicReq.getType());
		}
	}
}
