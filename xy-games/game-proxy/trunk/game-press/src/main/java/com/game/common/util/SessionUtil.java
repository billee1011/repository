/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.common.util;

import com.game.Player;
import com.game.common.Constant;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

/**
 * 
 *
 * @author wu_hc date: 2017年10月13日 下午5:26:01 <br/>
 */
public final class SessionUtil {

	public static Player getPlayer(S2SSession session) {
		return session.attr(Constant.player_key).get();
	}

	public static WorkerLoop getWorker(S2SSession session) {
		return session.attr(Constant.worker_key).get();
	}
}
