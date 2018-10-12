package com.cai.game.mj.sichuan;

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

public class SiChuanTrusteeRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(SiChuanTrusteeRunnable.class);

	private int room_id;
	private int seat_index;

	public SiChuanTrusteeRunnable(int room_id, int seat_index) {
		super(room_id);
		this.room_id = room_id;
		this.seat_index = seat_index;
	}

	@Override
	public void execute() {
		try {
			AbstractSiChuanMjTable table = (AbstractSiChuanMjTable) PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if (table == null) {
				logger.info("超时托管调度失败，房间【" + room_id + "】不存在！");
				return;
			}

			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.handler_request_trustee(seat_index, true, 0);
			} finally {
				roomLock.unlock();
			}
		} catch (Exception e) {
			logger.error("error:" + room_id, e);

			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if (room != null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e), 0L,
						SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}
	}
}
