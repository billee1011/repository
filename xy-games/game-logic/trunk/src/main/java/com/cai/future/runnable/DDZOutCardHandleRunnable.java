package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.PBUtil;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.ddz.DDZTable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.CallBankerResult;

public class DDZOutCardHandleRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(DDZOutCardHandleRunnable.class);

	private int _room_id;
	private int _seat_index;
	private int _qiang_player;
	private int _call_player;
	private int _qiang_acction;
	private int _call_acction;
	private boolean _bout;
	private DDZTable _table;

	public DDZOutCardHandleRunnable(int room_id, int seat_index, DDZTable table, int qiang_player, int call_player,
			int qiang_acction, int call_acction, boolean bout, boolean bBanker) {
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_table = table;
		_qiang_player = qiang_player;
		_call_player = call_player;
		_qiang_acction = qiang_acction;
		_call_acction = call_acction;
		_bout = bout;
		table._call_banker_status = 0;
		if (_seat_index == GameConstants.INVALID_SEAT) {
			return;
		}

		// 叫地主
		if (bBanker) {
			for (int i = 0; i < table._di_pai_card_count; i++) {
				table.GRR._cards_data[seat_index][i + table.GRR._card_count[seat_index]] = table._di_pai_card_data[i];
			}
			table.GRR._card_count[seat_index] += table._di_pai_card_count;
			table._logic.sort_card_date_list(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index]);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					table._init_hand_card[i][j] = table.GRR._cards_data[i][j];
				}
				table._init_hand_card_count[i] = table.GRR._card_count[i];
			}
			// 底牌类型
			int cards_type = table._logic.GetDipaiType(table._di_pai_card_data, table._di_pai_card_count);
			int times = 1;
			if (table.has_rule(GameConstants.GAME_RULE_DIPAI_DOUBLE)) {
				if (cards_type == GameConstants.DDZ_CT_DI_PAI_TONGHUA_SUNN_ZI) {
					times *= 3;
				} else if (cards_type == GameConstants.DDZ_CT_DI_PAI_TONGHUA) {
					times *= 2;
				} else if (cards_type == GameConstants.DDZ_CT_DI_PAI_BAOZI) {
					times *= 3;
				} else if (cards_type == GameConstants.DDZ_CT_DI_PAI_DUI_WANG) {
					times *= 4;
				} else if (cards_type == GameConstants.DDZ_CT_DI_PAI_DAN_WANG) {
					times *= 2;
				}
			} else {
				cards_type = GameConstants.DDZ_CT_DI_PAI_ERROR;
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._user_times[i] *= times;
			}
			PlayerServiceImpl.getInstance().updateRoomInfo(table.getRoom_id());
			table._di_pai_type = cards_type;
			table._current_player = seat_index;
			for (int index = 0; index < table.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER_RESULT);
				CallBankerResult.Builder call_banker_ddz_result = CallBankerResult.newBuilder();
				call_banker_ddz_result.setQiangAction(qiang_acction);
				call_banker_ddz_result.setQiangPlayer(qiang_player);
				call_banker_ddz_result.setCallAction(call_acction);
				call_banker_ddz_result.setBankerPlayer(seat_index);
				call_banker_ddz_result.setCallPlayer(call_player);
				for (int i = 0; i < table._di_pai_card_count; i++) {
					call_banker_ddz_result.addCardsData(table._di_pai_card_data[i]);
				}
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					call_banker_ddz_result.addUserCardCount(table.GRR._card_count[i]);
				}

				call_banker_ddz_result.setCardsType(cards_type);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					call_banker_ddz_result.addDifenBombDes(table.get_boom_difen_des(i));
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					call_banker_ddz_result.addUserCardsData(i, cards_card);
				}
				Int32ArrayResponse.Builder user_cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < table.GRR._card_count[index]; j++) {
					user_cards_card.addItem(table.GRR._cards_data[index][j]);
				}
				call_banker_ddz_result.setUserCardsData(index, user_cards_card);
				roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz_result));
				table.send_response_to_player(index, roomResponse);
			}

			// 回放
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER_RESULT);
			CallBankerResult.Builder call_banker_ddz_result = CallBankerResult.newBuilder();
			call_banker_ddz_result.setQiangAction(qiang_acction);
			call_banker_ddz_result.setQiangPlayer(qiang_player);
			call_banker_ddz_result.setCallAction(call_acction);
			call_banker_ddz_result.setBankerPlayer(seat_index);
			call_banker_ddz_result.setCallPlayer(call_player);
			for (int i = 0; i < table._di_pai_card_count; i++) {
				call_banker_ddz_result.addCardsData(table._di_pai_card_data[i]);
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				call_banker_ddz_result.addUserCardCount(table.GRR._card_count[i]);
			}

			call_banker_ddz_result.setCardsType(cards_type);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				call_banker_ddz_result.addDifenBombDes(table.get_boom_difen_des(i));
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards_card.addItem(table.GRR._cards_data[i][j]);
				}
				call_banker_ddz_result.addUserCardsData(i, cards_card);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz_result));
			table.GRR.add_room_response(roomResponse);
		}

	}

	@Override
	public void execute() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (table == null) {
				logger.info("调度发牌失败,房间[" + _room_id + "]不存在");
				return;
			}
			// logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try {
				roomLock.lock();
				_table.exe_call_banker_finish(_seat_index, _bout);
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
