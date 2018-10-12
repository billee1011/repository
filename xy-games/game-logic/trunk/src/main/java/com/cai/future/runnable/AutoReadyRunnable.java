/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.service.PlayerServiceImpl;

/**
 * 
 *
 * @author wu_hc date: 2018年7月18日 下午6:31:44 <br/>
 */
public class AutoReadyRunnable extends BaseFuture {

	/**
	 * @param room_id
	 */
	public AutoReadyRunnable(int room_id) {
		super(room_id);
	}

	@Override
	public void execute() {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
		if (table == null) {
			return;
		}
		ReentrantLock roomLock = table.getRoomLock();
		try {
			roomLock.lock();

			List<Player> players = table.getAllPlayers();
			if (null != players && !players.isEmpty()) {
				players.forEach(player -> {
					if (player.get_seat_index() < 0) {
						return;
					}
					if (table._player_ready[player.get_seat_index()] != 1) {
						table.handler_player_ready(player.get_seat_index(), false);
					}
				});
			}
		} finally {
			roomLock.unlock();
		}
	}
}
