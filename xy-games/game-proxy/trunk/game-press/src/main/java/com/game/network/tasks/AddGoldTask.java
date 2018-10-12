/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.tasks;

import com.cai.common.constant.C2SCmd;
import com.game.Player;
import com.game.common.Cfg;
import com.game.common.util.PressUtils;

import protobuf.clazz.c2s.C2SProto.__GMReq;

/**
 * 
 *
 * @author wu_hc date: 2017年10月24日 上午11:09:14 <br/>
 */
public final class AddGoldTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public AddGoldTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		__GMReq.Builder builder = __GMReq.newBuilder();
		builder.setType(1);
		builder.setValue(Cfg.ADD_GOLD + "");
		player.send(PressUtils.toC2SRequet(C2SCmd.TEST_TEST, builder.build()));
	}
}
