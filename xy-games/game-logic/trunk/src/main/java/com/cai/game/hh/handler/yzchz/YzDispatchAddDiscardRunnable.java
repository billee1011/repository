package com.cai.game.hh.handler.yzchz;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class YzDispatchAddDiscardRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(YzDispatchAddDiscardRunnable.class);

	private int room_id;
	private int seat_index;
	private int next_index;
	private int card;

	public YzDispatchAddDiscardRunnable(int room_id, int seat_index, int next_index, int card) {
		super(room_id);
		this.room_id = room_id;
		this.seat_index = seat_index;
		this.next_index = next_index;
		this.card = card;
	}

	@Override
	public void execute() {
		try {
			Table_YongZhou table = (Table_YongZhou) PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if (table == null) {
				logger.info("清牌和加入废牌堆调度失败，房间【" + room_id + "】不存在！");
				return;
			}

			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();

				table.operate_player_get_card(seat_index, 0, null, GameConstants.INVALID_SEAT, false);
				table.exe_add_discard(seat_index, 1, new int[] { card }, true, 0);
				table.exe_dispatch_card(next_index, GameConstants.WIK_NULL, 0);
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
