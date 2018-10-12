/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.module.RoomModule;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.RoomInfoReq;
import protobuf.clazz.c2s.C2SProto.RoomInfoRsp;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@ICmd(code = C2SCmd.ROOM_INFO, desc = "请求房间信息")
public final class RoomInfoHandler extends IClientHandler<RoomInfoReq> {

	@Override
	protected void execute(RoomInfoReq req, Request topRequest, C2SSession session) throws Exception {

		RoomRedisModel roomModel = RoomModule.getRoomRedisModelIfExsit(req.getRoomId());

		// 组装房间信息
		RoomInfoRsp.Builder builder = RoomInfoRsp.newBuilder();
		builder.setRoomId(req.getRoomId());
		if (null == roomModel) {
//			session.send(MessageResponse.getMsgAllResponse("房间不存在!").build());
			builder.setStatus(0);
		} else {
			builder.setGameTypeIndex(roomModel.getGame_type_index());
			int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(roomModel.getGame_type_index());
			builder.setAppId(gameId);
			builder.setRoomRule(SysParamDict.getInstance().isObserverGameTypeIndex(roomModel.getGame_type_index()) ? 1 : 0);
			builder.setStatus(1);
			builder.setClubId(roomModel.getClub_id());
			builder.setClubName(roomModel.getClubName() == null?"":roomModel.getClubName());
		}
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.ROOM_INFO, builder));
	}

}
