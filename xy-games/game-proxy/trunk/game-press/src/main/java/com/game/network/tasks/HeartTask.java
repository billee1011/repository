/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.tasks;

import com.game.Player;

/**
 * 
 *
 * @author wu_hc date: 2017年10月13日 下午3:17:16 <br/>
 */
public final class HeartTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public HeartTask(Player player) {
		super();
		this.player = player;
	}

	@Override
	public void run() {
		if (null != player) {
			player.sendHeart();
		}
	}

}
