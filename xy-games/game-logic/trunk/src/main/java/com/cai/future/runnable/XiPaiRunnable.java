/**
 * 
 */
package com.cai.future.runnable;

/**
 * @author xwy
 *
 */
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.future.BaseFuture;
import com.cai.game.fls.FLSTable;
import com.cai.service.PlayerServiceImpl;

public class XiPaiRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(XiPaiRunnable.class);

	private int _room_id;

	public XiPaiRunnable(int room_id) {
		super(room_id);
		_room_id = room_id;
	}

	@Override
	public void execute() {
		try {
			FLSTable table = (FLSTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.error("调度洗牌,房间[" + _room_id + "]不存在");
				return;
			}

			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.game_start_real();
			} finally {
				roomLock.unlock();

			}
		} catch (Exception e) {
			logger.error("error" + _room_id, e);
		}

	}

}
