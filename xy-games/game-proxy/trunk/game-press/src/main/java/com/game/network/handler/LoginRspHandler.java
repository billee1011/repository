/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.handler;

import com.game.Player;
import com.game.common.Constant;
import com.game.common.IClientHandler;
import com.game.manager.PlayerMananger;
import com.game.network.tasks.AddGoldTask;
import com.game.network.tasks.ReEnterRoomTask;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.AccountResponse;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 
 *
 * @author wu_hc date: 2017年10月12日 上午10:15:33 <br/>
 */
@ICmd(code = ResponseType.LOING_VALUE, exName = "loginResponse")
public final class LoginRspHandler extends IClientHandler<LoginResponse> {

	@Override
	protected void execute(LoginResponse rsp, Response response, S2SSession session) throws Exception {

		Player player = session.attr(Constant.player_key).get();
		if (null == player) {
			logger.error("连接:{} 没有玩家对象！！！", session.channel());
			return;
		}

		AccountResponse accountResponse = rsp.getFastLogingItemResponse().getLoginItemResponse().getAccountResponse();
		player.setAccountId(accountResponse.getAccountId());
		player.setAccountName(accountResponse.getAccountName());

		PlayerMananger.getInstance().addPlayer(player);

		if (accountResponse.getGold() > 500) {
			Runnable task = new ReEnterRoomTask(player);
			task.run();
		} else {
			Runnable task = new AddGoldTask(player);
			task.run();
		}
	}
}
