/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Player;
import com.cai.common.handler.IServerHandler;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.s2s.S2SProto.RoomWealthUpdateProto;

/**
 * 来自服务器端
 *
 * @author wu_hc date: 2018年09月17日 下午6:20:07 <br/>
 */
@IServerCmd(code = S2SCmd.ACCOUNT_WEALTH_UPATE, desc = "玩家财富更新")
public final class AccountWealthUpdateReqHandler extends IServerHandler<RoomWealthUpdateProto> {

	@Override
	public void execute(RoomWealthUpdateProto req, S2SSession session) throws Exception {

		//玩家缓存修复
		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(req.getAccountId());
		if (null != player) {
			assign(player, req);
		}

		//房间修复
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(req.getRoomId());
		if (room == null) {
			return;
		}
		player = room.get_player(req.getAccountId());
		if (player == null) {
			return;
		}

		assign(player, req);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		room.load_player_info_data(roomResponse);
		room.send_response_to_room(roomResponse);
	}

	private void assign(Player player, RoomWealthUpdateProto proto) {
		if (proto.hasGold()) {
			player.setGold(proto.getGold());
		}
		if (proto.hasMoney()) {
			player.setMoney(proto.getMoney());
		}
	}
}
