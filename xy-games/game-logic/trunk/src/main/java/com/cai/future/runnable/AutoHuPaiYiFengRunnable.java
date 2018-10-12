package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.mj.handler.yifeng.Table_YiFeng;
import com.cai.game.tdz.TDZTable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class AutoHuPaiYiFengRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(AutoHuPaiYiFengRunnable.class);

	private int _room_id;
	private int _seat_index;

	
	private int _hu_count;
	private int _hu_player[];
	
	private int _hu_card;


	public AutoHuPaiYiFengRunnable(int room_id, int seat_index, int hu_count, int hu_player[], int hu_card, int maxCount) {
		super(room_id);
		_hu_player = new int[maxCount];

		_room_id = room_id;
		_seat_index = seat_index;
		_hu_count = hu_count;

		for (int i = 0; i < _hu_count; i++) {
			_hu_player[i] = hu_player[i];
		}
		_hu_card = hu_card;
	}

	@Override
	public void execute() {
		try {
			Table_YiFeng table = (Table_YiFeng) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度发牌失败,房间[" + _room_id + "]不存在");
				return;
			}
			// logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				table.runnable_auto_hu_pai(_seat_index, _hu_count, _hu_player, _hu_card);
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
