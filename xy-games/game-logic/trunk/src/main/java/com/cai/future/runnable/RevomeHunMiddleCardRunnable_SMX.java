package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.mj.handler.henansmx.MJTable_SMX;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class RevomeHunMiddleCardRunnable_SMX extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(RevomeHunMiddleCardRunnable_SMX.class);

	private int _room_id;
	private int _seat_index;

	public RevomeHunMiddleCardRunnable_SMX(int room_id, int seat_index) {
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			MJTable_SMX table = (MJTable_SMX) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度hun结束失败,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.runnable_remove_hun_middle_cards(_seat_index);
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
