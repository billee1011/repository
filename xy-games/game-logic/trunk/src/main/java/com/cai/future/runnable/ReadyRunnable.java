package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.service.PlayerServiceImpl;

public class ReadyRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(ReadyRunnable.class);

	private int _room_id;

	public ReadyRunnable(int room_id) {
		super(room_id);
		_room_id = room_id;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("踢人操作,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.ready_timer();
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
