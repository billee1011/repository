package com.cai.game.dtz.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.game.dtz.Table_DTZ;
import com.cai.service.PlayerServiceImpl;

public class AutoOutCardRunnable implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(AutoOutCardRunnable.class);

	private int _room_id;
	private int _seat_index;
	private Table_DTZ _table;

	public AutoOutCardRunnable(int room_id, int seat_index, Table_DTZ table) {
		_room_id = room_id;
		_seat_index = seat_index;
		_table = table;
	}

	@Override
	public void run() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				return;
			}
			// logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				_table.auto_out_card(_seat_index);
			} finally {
				roomLock.unlock();
			}
		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
