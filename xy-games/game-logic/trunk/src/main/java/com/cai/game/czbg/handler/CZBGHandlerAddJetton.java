package com.cai.game.czbg.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.PBUtil;
import com.cai.game.czbg.CZBGTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.czbg.CZBGRsp.AddJetton_CZBG;
import protobuf.clazz.czbg.CZBGRsp.TableResponse_CZBG;

public class CZBGHandlerAddJetton extends CZBGHandler {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	protected GangCardResult m_gangCardResult;

	public CZBGHandlerAddJetton() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;

	}

	@Override
	public void exe(CZBGTable table) {

	}

	@Override
	public boolean handler_add_jetton(CZBGTable table, int seat_index, int jetton) {
		if (table._game_status != CZBGConstants.GS_CZBG_GAME_XIA_ZHU) {
			table.log_error("游戏状态不对 " + _game_status);
			return false;
		}
		if (table._add_Jetton[seat_index] != 0) {
			table.log_error("已经下注操作了 ");
			return false;
		}
//		if (jetton < 1 || jetton > table.baseScore) {
//			table.log_error("您下注已经越界了");
//			return false;
//		}
		if (seat_index == table._cur_banker) {
			table.log_error("庄家不用下注");
			return false;
		}

		table._add_Jetton[seat_index] = jetton;
		table.add_jetton_ox(seat_index);

		boolean flag = true;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (table._player_status[i] == true) {
				if (i == table._cur_banker) {
					continue;
				}
				if (table._add_Jetton[i] == 0) {
					flag = false;
				}
			}
		}
		if (flag == true) {
			table.sendCard();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(CZBGTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(CZBGConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_CZBG.Builder tableResponse = TableResponse_CZBG.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setCurrentPlayer(_seat_index);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table.isTrutess(i));// 是否托管
			tableResponse.addAddJetter(table._add_Jetton[i]);
		}
		tableResponse.setRoomInfo(table.getRoomInfo());
		
		if (table.has_rule(CZBGConstants.CZBG_RULE_BANKER_WITH_RATE) && table.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				tableResponse.addTrustee(false);// 是否托管
				tableResponse.addOpenCard(table._open_card[i]);

				if (i == seat_index) {
					for (int j = 0; j < 4; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}
				tableResponse.addPoint(table.cardGroup[i].point);
				tableResponse.addCardsData(cards);

				tableResponse.addEndScore(table.gameScore[i]);
				tableResponse.addScore(table.roundScore[i]);
			}
		}

		if (table._add_Jetton[seat_index] == 0) {
			AddJetton_CZBG.Builder addJetton = AddJetton_CZBG.newBuilder();
			addJetton.setMinValue(1);
			addJetton.setMaxValue(table.baseScore);
			addJetton.setCurBanker(table._cur_banker);
			
			tableResponse.setAddJettonInfo(addJetton);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addBeishu(table._call_banker[i]);
			tableResponse.addFenshu(table._add_Jetton[i]);
		}
		
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		
		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
