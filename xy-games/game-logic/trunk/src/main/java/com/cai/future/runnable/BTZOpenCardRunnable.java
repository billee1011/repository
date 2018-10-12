package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.game.btz.BTZTable;
import com.cai.service.PlayerServiceImpl;

/**
 * 推饼托管后自动准备
 * 
 * @author hexinqi
 *
 */
public class BTZOpenCardRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(BTZOpenCardRunnable.class);

	private int _room_id;
	private int seatIndex;

	public BTZOpenCardRunnable(int room_id, int seatIndex) {
		super(room_id);
		_room_id = room_id;
		this.seatIndex = seatIndex;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (room == null) {
				logger.info("房间[" + _room_id + "]不存在");
				return;
			}
			BTZTable table = (BTZTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				if (table._handler != null) {
					table.handler_open_cards(seatIndex, true);
				}
			} finally {
				roomLock.unlock();
			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
