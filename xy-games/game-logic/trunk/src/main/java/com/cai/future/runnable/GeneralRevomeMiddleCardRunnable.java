package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

/**
 * 移除中心牌的通用Runnable
 * 
 *
 * @author WalkerGeek 
 * date: 2018年6月14日 上午11:16:22 <br/>
 */
public class GeneralRevomeMiddleCardRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(GeneralRevomeMiddleCardRunnable.class);

	private int _room_id;
	private int _seat_index;

	public GeneralRevomeMiddleCardRunnable(int room_id, int seat_index) {
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
	}

	@Override
	public void execute() {
		try {
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (room == null) {
				logger.info("调度中心牌结束失败,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = room.getRoomLock();
			try {
				roomLock.lock();
				// 移除中间番的牌
				room.runnable_remove_middle_cards_general(_seat_index);
			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + _room_id, e);
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(room!=null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e),
						0L, SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}

	}

}
