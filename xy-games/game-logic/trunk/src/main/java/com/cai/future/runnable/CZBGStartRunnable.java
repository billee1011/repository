package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.future.BaseFuture;
import com.cai.game.czbg.CZBGTable;
import com.cai.service.PlayerServiceImpl;

/**
 * 推饼托管后自动准备
 * 
 * @author hexinqi
 *
 */
public class CZBGStartRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(CZBGStartRunnable.class);

	private int _room_id;

	public CZBGStartRunnable(int room_id) {
		super(room_id);
		_room_id = room_id;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			CZBGTable table = (CZBGTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("先牛后怪,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.game_start_after_ox();
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}
	}

}
