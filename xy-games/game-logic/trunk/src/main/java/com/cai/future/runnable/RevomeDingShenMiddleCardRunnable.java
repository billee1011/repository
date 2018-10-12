package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.future.BaseFuture;
import com.cai.game.mj.henan.kulongdaishen.MJTable_KLDS;
import com.cai.service.PlayerServiceImpl;

public class RevomeDingShenMiddleCardRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(RevomeDingShenMiddleCardRunnable.class);

	private int _room_id;
	private int _seat_index;
	private int _maggic_card; // 神牌值

	public RevomeDingShenMiddleCardRunnable(int room_id, int seat_index, int maggic_card) {
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_maggic_card = maggic_card;

	}

	@Override
	public void execute() {
		try {
			MJTable_KLDS table = (MJTable_KLDS) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度定神结束失败,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				// 移除中间神牌
				table.runnable_remove_ding_shen_middle_cards(_seat_index, _maggic_card);
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}

	}

}
