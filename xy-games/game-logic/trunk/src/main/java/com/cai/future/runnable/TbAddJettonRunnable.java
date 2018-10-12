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
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.handler.tb.TBTable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

/**
 * 推饼托管后自动准备
 * 
 * @author hexinqi
 *
 */
public class TbAddJettonRunnable extends BaseFuture {
	private static Logger logger = LoggerFactory.getLogger(TbAddJettonRunnable.class);

	private int _room_id;
	private int seatIndex;
	private int jettonInfo;

	public TbAddJettonRunnable(int room_id, int seatIndex, int jettonInfo) {
		super(room_id);
		_room_id = room_id;
		this.seatIndex = seatIndex;
		this.jettonInfo = jettonInfo;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		try {
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (room == null) {
				logger.info("房间[" + _room_id + "]不存在");
				return;
			}
			if (room._game_type_index == GameConstants.GAME_TYPE_BTZ_TB || (GameConstants.GAME_TYPE_BTZ_TB_BEGIN <= room._game_type_index
					&& GameConstants.GAME_TYPE_BTZ_TB_END >= room._game_type_index)) {
				TBTable table = (TBTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
				ReentrantLock roomLock = table.getRoomLock();
				try {
					roomLock.lock();
					if (table._handler != null) {
						table._handler.handler_add_jetton(table, seatIndex, jettonInfo);
					}
				} finally {
					roomLock.unlock();
				}
			} else if (room._game_type_index == GameConstants.GAME_TYPE_BTZ_YY) {
				BTZTable table = (BTZTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
				ReentrantLock roomLock = table.getRoomLock();
				try {
					roomLock.lock();
					if (table._handler != null) {
						table._handler.handler_add_jetton(table, seatIndex, jettonInfo);
					}
				} finally {
					roomLock.unlock();
				}
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
