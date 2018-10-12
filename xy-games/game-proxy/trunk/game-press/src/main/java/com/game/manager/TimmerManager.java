/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.manager;

import java.util.Timer;
import java.util.TimerTask;

import com.game.Player;

/**
 * 
 *
 * @author wu_hc date: 2017年10月13日 下午2:54:52 <br/>
 */
public final class TimmerManager {
	/**
	 * 
	 */
	private static final Timer timer = new Timer("TimmerManager-Timer");;

	public static void init() {

		//
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				for (final Player p : PlayerMananger.getInstance().players()) {
					p.sendHeart();
				}
			}
		}, 1000L, 5000L);
	}

}
