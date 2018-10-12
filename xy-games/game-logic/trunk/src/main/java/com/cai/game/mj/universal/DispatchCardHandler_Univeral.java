package com.cai.game.mj.universal;

import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.GangCardResult;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class DispatchCardHandler_Univeral extends AbstractHandler_Universal<AbstractMahjongTable_Universal> {
	public DispatchCardHandler_Univeral() {
		gangCardResult = new GangCardResult();
	}

	public void reset_status(int currentSeatIndex, int dispatchCardType) {
		this.currentSeatIndex = currentSeatIndex;
		this.dispatchCardType = dispatchCardType;
	}

	@Override
	public void exe(AbstractMahjongTable_Universal table) {
		MahjongUtil_Universal.cleanPlayerAction(table, this);

		if (table.GRR._left_card_count <= 0) {
			MahjongUtil_Universal.liuJu(table);
			return;
		}

		MahjongUtil_Universal.dispatchCard(table, this);

		MahjongUtil_Universal.analyseZiMo(table, this);

		table.GRR._cards_index[currentSeatIndex][table._logic.switch_to_card_index(cardDataHandled)]++;

		table.operate_player_get_card(currentSeatIndex, 1, new int[] { cardDataHandled }, UniversalConstants.INVALID_SEAT);

		if (table.GRR._left_card_count > 0) {
			MahjongUtil_Universal.analyseZiMo(table, this);
		}

		MahjongUtil_Universal.operateOrOutCard(table, currentSeatIndex);
	}

	@Override
	public boolean handleOperateCard(AbstractMahjongTable_Universal table, int seat_index, int operate_code, int operate_card) {
		MahjongUtil_Universal.actionCheckBeforeOperate(table, seat_index, operate_code, this);

		table._playerStatus[seat_index].operate(operate_code, operate_card);

		if (operate_code == UniversalConstants.WIK_NULL) {
			MahjongUtil_Universal.recordNullAction(table, seat_index);

			return true;
		}

		switch (operate_code) {
		case UniversalConstants.WIK_GANG: {
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				if (operate_card == gangCardResult.cbCardData[i]) {
					table.exe_gang(seat_index, seat_index, operate_card, operate_code, gangCardResult.type[i], true, false);
					return true;
				}
			}
			break;
		}
		case UniversalConstants.WIK_ZI_MO: {
			MahjongUtil_Universal.processZiMo(table, seat_index, operate_card);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handlePlayerOutCard(AbstractMahjongTable_Universal table, int seat_index, int card) {
		return MahjongUtil_Universal.handlePlayerOutCard(table, seat_index, card, this);
	}

	@Override
	public boolean handlePlayerBeInRoom(AbstractMahjongTable_Universal table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		MahjongUtil_Universal.initialReconnectData(table, roomResponse, tableResponse);

		MahjongUtil_Universal.displayTableCards(table, roomResponse, tableResponse, seat_index, this);

		table.send_response_to_player(seat_index, roomResponse);

		MahjongUtil_Universal.displayTingCards(table, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
