package com.cai.game.mj.universal;

import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.PlayerStatus;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class GangHandler_Universal extends AbstractHandler_Universal<AbstractMahjongTable_Universal> {
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		currentSeatIndex = seat_index;
		providerSeatIndex = provide_player;
		cardDataHandled = center_card;
		currentAction = action;
		gangType = type;
	}

	@Override
	public void exe(AbstractMahjongTable_Universal table) {
		MahjongUtil_Universal.cleanPlayerAction(table, this);

		table._current_player = currentSeatIndex;

		table._playerStatus[currentSeatIndex].chi_hu_round_valid();

		table.operate_effect_action(currentSeatIndex, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { currentAction }, 1,
				UniversalConstants.INVALID_SEAT);

		if (gangType == UniversalConstants.GANG_TYPE_AN_GANG || gangType == UniversalConstants.GANG_TYPE_JIE_GANG) {
			exe_gang(table);
			return;
		}

		boolean bAroseAction = table.estimate_gang_respond(currentSeatIndex, cardDataHandled);

		if (bAroseAction == false) {
			exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(i, UniversalConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	protected boolean exe_gang(AbstractMahjongTable_Universal table) {
		int cbCardIndex = table._logic.switch_to_card_index(cardDataHandled);

		int cbWeaveIndex = MahjongUtil_Universal.processGangIndex(table, this);

		if (cbWeaveIndex == -1) {
			table.log_player_error(currentSeatIndex, "杠牌出错");
			return false;
		}

		MahjongUtil_Universal.processGangWeave(table, cbCardIndex, cbWeaveIndex, this);

		MahjongUtil_Universal.refreshPlayerCardsNoMagic(table, currentSeatIndex);

		MahjongUtil_Universal.getAndDisplayTingCards(table, currentSeatIndex);

		int basicScore = UniversalConstants.CELL_SCORE;
		if (UniversalConstants.GANG_TYPE_AN_GANG == gangType) {
			basicScore = 1 * basicScore;
		} else if (UniversalConstants.GANG_TYPE_JIE_GANG == gangType) {
			basicScore = 1 * basicScore;
		} else if (UniversalConstants.GANG_TYPE_ADD_GANG == gangType) {
			basicScore = 1 * basicScore;
		}

		MahjongUtil_Universal.processGangScore(table, basicScore, this);

		table.exe_dispatch_card(currentSeatIndex, UniversalConstants.DISPATCH_CARD_TYPE_GANG, 0);

		return false;
	}

	@Override
	public boolean handleOperateCard(AbstractMahjongTable_Universal table, int seat_index, int operate_code, int operate_card) {
		MahjongUtil_Universal.actionCheckBeforeOperate(table, seat_index, operate_code, this);

		table._playerStatus[seat_index].operate(operate_code, operate_card);

		if (operate_code == UniversalConstants.WIK_NULL) {
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { UniversalConstants.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_chi_hu()) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}
		}

		int target_player = MahjongUtil_Universal.getHpSeatNoTp(table, seat_index, operate_code, currentSeatIndex);
		int target_action = table._playerStatus[target_player].get_perform();
		operate_card = cardDataHandled;

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		MahjongUtil_Universal.cleanHandlerStateAction(table, this);

		switch (target_action) {
		case UniversalConstants.WIK_NULL: {
			exe_gang(table);

			return true;
		}
		case UniversalConstants.WIK_CHI_HU: {
			MahjongUtil_Universal.processJiePaoNoTp(table, target_player, operate_card, currentSeatIndex);

			return true;
		}
		}

		return true;
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
