package com.cai.game.wsk.handler.pcdz.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.game.wsk.handler.pcdz.WSKTable_PCDZ;
import com.cai.service.PlayerServiceImpl;

public class AutoPassRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(AutoPassRunnable.class);

	private int _room_id;
	private int _seat_index;

	public AutoPassRunnable(int room_id, int seat_index) {
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
	}

	@Override
	public void execute() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				return;
			}
			// logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.handler_operate_out_card_mul(_seat_index, null, 0, 0, "");
			} finally {
				roomLock.unlock();
			}
		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
