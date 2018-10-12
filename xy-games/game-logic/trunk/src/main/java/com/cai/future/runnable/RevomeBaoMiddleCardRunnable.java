package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.future.BaseFuture;
import com.cai.game.mj.jiangxi.ruijin.MJTable_RUIJIN;
import com.cai.service.PlayerServiceImpl;

public class RevomeBaoMiddleCardRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(RevomeBaoMiddleCardRunnable.class);

	private int _room_id;
	private int _seat_index;

	public RevomeBaoMiddleCardRunnable(int room_id, int seat_index) {
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
	}

	@Override
	public void execute() {
		try {
			MJTable_RUIJIN table = (MJTable_RUIJIN) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度宝牌结束失败,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				// 移除中间万能牌
				table.runnable_remove_bao_middle_cards(_seat_index);
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}

	}

}
