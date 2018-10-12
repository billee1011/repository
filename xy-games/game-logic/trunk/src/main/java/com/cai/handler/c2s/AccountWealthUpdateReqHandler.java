/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Player;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.s2s.S2SProto.RoomWealthUpdateProto;

/**
 * 
 *
 * @author wu_hc date: 2018年1月30日 下午6:20:07 <br/>
 */
@IServerCmd(code = S2SCmd.ACCOUNT_WEALTH_UPATE, desc = "玩家财富更新")
public final class AccountWealthUpdateReqHandler extends IClientHandler<RoomWealthUpdateProto> {

	@Override
	protected void execute(RoomWealthUpdateProto req, Session session) throws Exception {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(req.getRoomId());
		if(room == null){
			return;
		}
		Player player = room.get_player(req.getAccountId());
		if (player == null) {
			return;
		}
		
		if (req.hasGold()) {
			player.setGold(req.getGold());
		}
		if (req.hasMoney()) {
			player.setMoney(req.getMoney());
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		room.load_player_info_data(roomResponse);
		room.send_response_to_room(roomResponse);
	}
}
