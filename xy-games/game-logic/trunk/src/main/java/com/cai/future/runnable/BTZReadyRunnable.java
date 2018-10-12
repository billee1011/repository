package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.future.BaseFuture;
import com.cai.game.btz.BTZTable;
import com.cai.service.PlayerServiceImpl;

/**
 * 推饼托管后自动准备
 * 
 * @author hexinqi
 *
 */
public class BTZReadyRunnable extends BaseFuture{
	private static Logger logger = LoggerFactory.getLogger(BTZReadyRunnable.class);

	private int _room_id;

	public BTZReadyRunnable(int room_id) {
		super(room_id);
		_room_id = room_id;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			BTZTable table = (BTZTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("踢人操作,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.get_players()[i] != null) {
						table.handler_player_ready(i, false);
					}
				}
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
