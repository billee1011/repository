/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.handler;

import com.cai.common.constant.S2CCmd;
import com.game.Player;
import com.game.common.IClientHandler;
import com.game.common.util.SessionUtil;
import com.game.network.tasks.ReEnterRoomTask;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.Response;
import protobuf.clazz.c2s.C2SProto.MessageReceiveRsp;

/**
 * 
 * 
 * @author wu_hc date: 2017年10月13日 下午3:03:12 <br/>
 */
@ICmd(code = S2CCmd.TEST_TEST)
public final class GMRspHandler extends IClientHandler<MessageReceiveRsp> {

	@Override
	protected void execute(MessageReceiveRsp rsp, Response response, S2SSession session) throws Exception {

		final Player player = SessionUtil.getPlayer(session);
		if (rsp.getType() == 1) { // 添加金币成功
			Runnable task = new ReEnterRoomTask(player);
			task.run();
		}
	}
}
