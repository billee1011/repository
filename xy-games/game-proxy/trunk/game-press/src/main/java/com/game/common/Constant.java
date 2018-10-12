/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.common;

import com.game.Player;
import com.xianyi.framework.core.concurrent.WorkerLoop;

import io.netty.util.AttributeKey;

/**
 * 
 *
 * @author wu_hc date: 2017年10月12日 下午2:26:55 <br/>
 */
public interface Constant {

	AttributeKey<Player> player_key = AttributeKey.valueOf("player_key~~~~~");

	AttributeKey<WorkerLoop> worker_key = AttributeKey.valueOf("worker_key~~~~~");
}
