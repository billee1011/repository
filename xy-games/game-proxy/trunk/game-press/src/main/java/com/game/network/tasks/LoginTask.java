/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.tasks;

import com.game.Player;

/**
 * 登陆
 *
 * @author wu_hc date: 2017年10月13日 下午3:16:03 <br/>
 */
public final class LoginTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public LoginTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (null != player) {
			player.connect();
		}
	}
}
