package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Room;
import com.cai.future.BaseFuture;
import com.cai.service.PlayerServiceImpl;

public class HuPaiRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(HuPaiRunnable.class);

	private int _room_id;
	private int _operate_code;
	private int _seat_index;
	private int _wik_kind;

	public HuPaiRunnable(int room_id, int seat_index, int wik_kind, int operate_code) {
		super(room_id);
		_room_id = room_id;
		_operate_code = operate_code;
		_seat_index = seat_index;
		_wik_kind = wik_kind;
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
				table.hu_pai_timer(_seat_index, _operate_code, _wik_kind);
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
