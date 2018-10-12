package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.sdh.handler.yybs.SDHTable_YYBS;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class YYBSGameStartRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(YYBSGameStartRunnable.class);

	private int _room_id;

	public YYBSGameStartRunnable(int roomId) {
		super(roomId);
		this._room_id = roomId;
	}

	@Override
	public void execute() {
		try {
			SDHTable_YYBS table = (SDHTable_YYBS) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度发牌失败,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.clearAction(i);
				}
				// if (table.GRR != null) {
				table.on_handler_game_start();
				// }
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
