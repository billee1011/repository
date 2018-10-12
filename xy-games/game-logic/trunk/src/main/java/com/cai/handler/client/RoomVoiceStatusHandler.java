/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.c2s.C2SProto.RequestChangeGVoiceStatus;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月19日 上午11:48:33 <br/>
 */
@ICmd(code = C2SCmd.VOICE_STATUS, desc = "")
public final class RoomVoiceStatusHandler extends IClientExHandler<RequestChangeGVoiceStatus> {

	@Override
	protected void execute(RequestChangeGVoiceStatus req, TransmitProto topReq, Session session) throws Exception {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(req.getRoomId());
		if (null == room) {
			return;
		}
		Player player = room.get_player(topReq.getAccountId());
		if (null != player) {
			player.setGvoiceStatus(req.getGvoiceStatus());
		}
		// 刷新玩家
		RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
		refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		room.load_player_info_data(refreshroomResponse);
		room.send_response_to_room(refreshroomResponse);
	}
}
