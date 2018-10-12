package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.AbstractRoom;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

/**
 * 托管后自动准备
 * 
 * @author hexinqi
 *
 */
public class LuHeReadyRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(LuHeReadyRunnable.class);

	private int _room_id;

	public LuHeReadyRunnable(int room_id) {
		super(room_id);
		_room_id = room_id;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			AbstractRoom table=  PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("准备操作,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.get_players()[i] != null && table.istrustee[i]) {
						if (table._player_ready[i] == 0) {
							table.handler_player_ready(i, false);
						}
					}
				}
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
