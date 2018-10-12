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

public class YYBSFanZhuRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(YYBSFanZhuRunnable.class);

	private int _room_id;
	private int seat_index;

	public YYBSFanZhuRunnable(int roomId,int seat_index) {
		super(roomId);
		this._room_id = roomId;
		this.seat_index = seat_index;
	}

	@Override
	public void execute() {
		try {
			SDHTable_YYBS table = (SDHTable_YYBS) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度定时器失败,房间[" + _room_id + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.GiveUpFanZhu(seat_index);
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
