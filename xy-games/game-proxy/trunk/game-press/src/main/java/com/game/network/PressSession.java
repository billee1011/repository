/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network;

import com.game.Player;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.transport.Session;
import com.xianyi.framework.core.transport.netty.session.AbstractNettySession;

import io.netty.channel.Channel;

/**
 * 
 *
 * @author wu_hc date: 2017年10月13日 下午5:00:06 <br/>
 */
public final class PressSession extends AbstractNettySession implements Session {

	private Player player;
	private WorkerLoop worker;

	/**
	 * @param channel
	 */
	public PressSession(Channel channel) {
		super(channel);
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public WorkerLoop getWorker() {
		return worker;
	}

	public void setWorker(WorkerLoop worker) {
		this.worker = worker;
	}

}
