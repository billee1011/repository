package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.sdh.SDHConstants;
import com.cai.game.sdh.SDHTable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class SDHCallBankerRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(SDHCallBankerRunnable.class);

	private int roomId;
	private int seatIndex;
	private int type;

	public SDHCallBankerRunnable(int roomId, int seatIndex, int type) {
		super(roomId);
		this.roomId = roomId;
		this.seatIndex = seatIndex;
		this.type = type;
	}

	@Override
	public void execute() {
		try {
			SDHTable table = (SDHTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if (table == null) {
				logger.info("三打哈叫庄操作,房间[" + roomId + "]不存在");
				return;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				if (type == SDHConstants.Game_CALL_BANKER_WAIT_TIME_OUT) {
					table.beginTime = System.currentTimeMillis();
					table.operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
							new long[] { SDHConstants.Player_Status_CALL_BANKER }, SDHConstants.SDH_OPERATOR_TIME, seatIndex);
					table.showPlayerOperate(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
							new long[] { SDHConstants.Player_Status_CALL_BANKER }, SDHConstants.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);
				}

			} finally {
				roomLock.unlock();

			}

		} catch (Exception e) {
			logger.error("error" + roomId, e);
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if(room!=null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e),
						0L, SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}
	}

}
