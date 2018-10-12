package com.cai.game.pdk.handler.jdpdk_NP;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EGameType;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.SheduleArgs;
import com.cai.game.pdk.handler.PDKHandlerMingPai;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pdk_np.Pdk_npRsp.MingPaiResponse_NP;
import protobuf.clazz.pdk_np.Pdk_npRsp.TableResponse_PDK_NP;

public class PDKHandlerMingPaiNP extends PDKHandlerMingPai<PDK_JD_Table_NP> {
	@Override
	public void exe(PDK_JD_Table_NP table) {
		table._game_status = GameConstants.GS_NPPDK_MING;
		table.ming_pai_begin(GameConstants.INVALID_SEAT);
	}

	@Override
	public boolean handler_ming_pai(PDK_JD_Table_NP table, int seat_index, int ming_action) {
		if (table._game_status != GameConstants.GS_NPPDK_MING) {
			return true;
		}
		if (table._ming_pai[seat_index] != -1) {
			return true;
		}
		table._ming_pai[seat_index] = ming_action;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		MingPaiResponse_NP.Builder mign_pai_response = MingPaiResponse_NP.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_MING_PAI_OPREATE);// 201
		mign_pai_response.setOpreateSeat(seat_index);
		mign_pai_response.setAction(ming_action);
		roomResponse.setCommResponse(PBUtil.toByteString(mign_pai_response));
		table.send_response_to_room(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._ming_pai[i] == -1) {
				return true;
			}
		}
		if (table.has_rule(GameConstants.GAME_RULE_MING_PAI_NEXT)) {
			table.ming_refresh_card();
			if (table.has_rule(GameConstants.GAME_RULE_QIANG_GUAN)) {
				table.schedule(table.ID_TIMER_QIANG_GUAN, SheduleArgs.newArgs(), 1000);
			} else {
				table._game_status = GameConstants.GS_NPPDK_PLAY;
				table.schedule(table.ID_TIMER_OUT_CARD, SheduleArgs.newArgs(), 1000);
			}
		} else {
			table.game_start_pkd();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(PDK_JD_Table_NP table, int seat_index) {

		if (table.has_rule(GameConstants.GAME_RULE_MING_PAI_NEXT)) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PDK_RECONNECT_DATA);

			TableResponse_PDK_NP.Builder tableResponse_pdk = TableResponse_PDK_NP.newBuilder();
			table.load_player_info_data_reconnect(tableResponse_pdk);
			tableResponse_pdk.setRoomInfo(table.getRoomInfo());
			if (table.GRR != null) {
				tableResponse_pdk.setBankerPlayer(table.GRR._banker_player);
				tableResponse_pdk.setCurrentPlayer(table._current_player);
				tableResponse_pdk.setPrevPlayer(table._prev_palyer);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					tableResponse_pdk.addOutCardsCount(table.GRR._cur_round_count[i]);
					tableResponse_pdk.addPlayerPass(table.GRR._cur_round_pass[i]);
					Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
					Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
						if (table.GRR._cur_round_count[i] > 0) {
							out_cards.addItem(table.GRR._cur_round_data[i][j]);
							out_change_cards.addItem(table.GRR._cur_round_data[i][j]);
						}
					}
					if (table.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)) {
						tableResponse_pdk.addCardCount(table.GRR._card_count[i]);
					} else {
						if (i == seat_index) {
							tableResponse_pdk.addCardCount(table.GRR._card_count[i]);
						} else {
							tableResponse_pdk.addCardCount(-1);
						}
					}
					tableResponse_pdk.addCardType(table.GRR._cur_card_type[i]);
					tableResponse_pdk.addOutCardsData(i, out_cards);
					tableResponse_pdk.addChangeCardsData(out_change_cards);
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
					tableResponse_pdk.addCardsData(i, cards_card);
					if (table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_ONE)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_TWO)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_THREE)) {
						tableResponse_pdk.addSeatPiaoScore(table._piao_fen[i]);
					} else {
						tableResponse_pdk.addSeatPiaoScore(-2);
					}

					if (table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_ONE)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_TWO)
							|| table.has_rule(GameConstants.GAME_RULE_PIAO_SCORE_THREE)) {
						tableResponse_pdk.addOpreatePiaoScore(-2);
					}

				}

				if (table._current_player == seat_index) {
					int can_out_card_data[] = new int[table.get_hand_card_count_max()];
					int can_out_card_count = table._logic.Player_Can_out_card(
							table.GRR._cards_data[table._current_player], table.GRR._card_count[table._current_player],
							table._turn_out_card_data, table._turn_out_card_count, can_out_card_data);
					for (int i = 0; i < can_out_card_count; i++) {
						tableResponse_pdk.addUserCanOutData(can_out_card_data[i]);
					}
					tableResponse_pdk.setUserCanOutCount(can_out_card_count);
				}

				// 手牌--将自己的手牌数据发给自己
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
					cards_card.addItem(table.GRR._cards_data[seat_index][j]);
				}
				tableResponse_pdk.setCardsData(seat_index, cards_card);
				for (int i = 0; i < table._turn_out_card_count; i++) {
					if (table._turn_out_card_count > 0) {
						tableResponse_pdk.addPrCardsData(table._turn_out_card_data[i]);
						tableResponse_pdk.addPrChangeCardsData(table._turn_out_card_data[i]);
					}
				}
				tableResponse_pdk.setPrCardsCount(table._turn_out_card_count);
				tableResponse_pdk.setPrOutCardType(table._turn_out_card_type);
				tableResponse_pdk.setPrOutCardPlayer(table._turn_out__player);
				if (table._turn_out_card_count == 0) {
					tableResponse_pdk.setIsFirstOut(1);
				} else {
					tableResponse_pdk.setIsFirstOut(0);
				}
				if (table.matchId == 0) {
					tableResponse_pdk.setDisplayTime(10);
				} else {
					tableResponse_pdk.setDisplayTime(SysParamServerDict.getInstance()
							.getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(8).getVal1() / 1000);
				}
				tableResponse_pdk.setMagicCard(GameConstants.INVALID_CARD);
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					tableResponse_pdk.addSeatPiaoScore(table._piao_fen_select[i]);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_pdk));
			table.send_response_to_player(seat_index, roomResponse);
		}

		if (table._game_status == GameConstants.GS_NPPDK_MING && table._ming_pai[seat_index] == -1) {
			table.ming_pai_begin(seat_index);
		}
		return true;
	}

}
