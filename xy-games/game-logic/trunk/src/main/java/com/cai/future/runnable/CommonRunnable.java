/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.BaseFuture;
import com.cai.game.AbstractRoom;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

/**
 * 
 *
 * @author wu_hc date: 2018年4月25日 下午6:00:07 <br/>
 */
public class CommonRunnable extends BaseFuture {

	private final SheduleArgs args;

	/**
	 * @param room_id
	 */
	public CommonRunnable(int room_id, SheduleArgs args) {
		super(room_id);
		this.args = args;
	}

	@Override
	public void execute() {
		try {
			AbstractRoom table = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if (table == null) {
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();

				table.runInRoomLoop(() -> {
					table.executeSchedule(args);
				});

			} finally {
				roomLock.unlock();
			}

		} catch (Exception e) {
			e.printStackTrace();
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if(room!=null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e),
						0L, SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}
	}

}
