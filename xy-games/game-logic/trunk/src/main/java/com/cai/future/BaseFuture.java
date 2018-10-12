/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future;

import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;

/**
 * 
 * @author wu_hc date: 2018年1月23日 上午11:39:46 <br/>
 */
public abstract class BaseFuture implements Runnable {

	protected final int roomId;

	public BaseFuture(int room_id) {
		this.roomId = room_id;
	}

	@Override
	public void run() {
		AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
		if (null != room) {
			room.runInRoomLoop(() -> {
				execute();
			});
		}
	}

	public abstract void execute();
}
