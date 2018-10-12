package com.cai.game.mj.universal;

import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.GangCardResult;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class ChiPengHandler_Univeral extends AbstractHandler_Universal<AbstractMahjongTable_Universal> {
	public ChiPengHandler_Univeral() {
		chiPengType = 0;
		gangCardResult = new GangCardResult();
	}

	public void reset_status(int currentSeatIndex, int provider, int action, int cardDataHandled, int chiPengType) {
		this.currentSeatIndex = currentSeatIndex;
		this.currentAction = action;
		this.cardDataHandled = cardDataHandled;
		this.providerSeatIndex = provider;
		this.chiPengType = chiPengType;
	}

	@Override
	public void exe(AbstractMahjongTable_Universal table) {
		MahjongUtil_Universal.cleanPlayerAction(table, this);

		MahjongUtil_Universal.addChiPengWeave(table, this);

		MahjongUtil_Universal.chiPengEffectRemoveDiscard(table, this);

		MahjongUtil_Universal.refreshPlayerCardsNoMagic(table, currentSeatIndex);

		MahjongUtil_Universal.operateOrOutCard(table, currentSeatIndex);
	}

	@Override
	public boolean handleOperateCard(AbstractMahjongTable_Universal table, int seat_index, int operate_code, int operate_card) {
		MahjongUtil_Universal.actionCheckBeforeOperate(table, seat_index, operate_code, this);

		if (operate_code == UniversalConstants.WIK_NULL) {
			MahjongUtil_Universal.recordNullAction(table, seat_index);
			return true;
		}

		switch (operate_code) {
		case UniversalConstants.WIK_GANG: {
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				if (operate_card == gangCardResult.cbCardData[i]) {
					table.exe_gang(currentSeatIndex, currentSeatIndex, operate_card, operate_code, gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
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

		table.operate_effect_action(currentSeatIndex, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { currentAction }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
