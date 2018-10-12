package com.cai.game.mj.handler.yyzxz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.MJHandlerChiPeng;
import com.cai.game.mj.handler.moyanggui.MjTableMoYangGui;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_YYZXZ extends MJHandlerChiPeng<MJTable_YYZXZ> {

    private GangCardResult m_gangCardResult = new GangCardResult();

    public MJHandlerChiPeng_YYZXZ() {
        super();
    }

    @Override
    public void exe(MJTable_YYZXZ table) {

        table.isPeng[_seat_index] = true;
        // 组合扑克
        int wIndex = table.GRR._weave_count[_seat_index]++;
        table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
        table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
        table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
        table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

        // 设置用户
        table._current_player = _seat_index;

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        if (_type == GameConstants.CHI_PENG_TYPE_OUT_CARD) {
            // 删掉出来的那张牌
            // table.operate_out_card(this._provider, 0,
            // null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
            table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);
        }

        // 刷新手牌包括组合
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		if(table.has_rule(GameConstants.ZXZ_MAN_TIAN_FEI)){
			for(int j=0; j < hand_card_count; j++){
				if( table._logic.checkWanZi(cards[j])){
					cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		}
        
        table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
                table.GRR._weave_items[_seat_index]);
        
        
     // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
 		int count = 0;
 		int ting_count = 0;

 		int card_type_count = GameConstants.MAX_ZI_FENG;

 		for (int i = 0; i < card_type_count; i++) {
 			count = table.GRR._cards_index[_seat_index][i];

 			if (count > 0) {
 				table.GRR._cards_index[_seat_index][i]--;

 				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
 						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
 						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

 				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
 					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

 					ting_count++;
 				}

 				table.GRR._cards_index[_seat_index][i]++;
 			}
 		}

 		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

 		if (ting_count > 0) {
 			int tmp_cards[] = new int[GameConstants.MAX_COUNT];
 			int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);
 			for (int i = 0; i < tmp_hand_card_count; i++) {
 				if (table._logic.is_magic_card(tmp_cards[i])) {
 					tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
 				else{
 					for (int j = 0; j < ting_count; j++) {
 	 					if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
 	 						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
 	 						break;
 	 					}
 	 				}
 				}
 			}

 			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, 
 					table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
 		} else {
 			// 刷新手牌
 			table.operate_player_cards(_seat_index, hand_card_count, cards,
 					table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
 		}
 		// TODO: 出任意一张牌时，能胡哪些牌 -- End

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

        m_gangCardResult.cbCardCount = 0;
        // 如果牌堆还有牌，判断能不能杠
        if (table.GRR._left_card_count > 1) {

            int cbActionMask = GameConstants.WIK_NULL;
            cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
                    0);

            if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
            	curPlayerStatus.add_action(GameConstants.WIK_XIA_ZI_BU);
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    //curPlayerStatus.add_action(GameConstants.WIK_GANG);// 听牌的时候可以杠
                    //curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
                	//curPlayerStatus.add_action(GameConstants.WIK_XIA_ZI_BU);// 
                    curPlayerStatus.add_XiaZi(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
                }

            }

        }

        if (curPlayerStatus.has_action()) {
            // curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);

        } else {
            // curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();
        }
    }

    /***
     * //用户操作
     * 
     * @param seat_index
     * @param operate_code
     * @param operate_card
     * @return
     */
    @Override
    public boolean handler_operate_card(MJTable_YYZXZ table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 效验操作
        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_error("没有这个操作");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("不是当前玩家操作");
            return false;
        }

        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_GANG: // 杠牌操作
        case GameConstants.WIK_XIA_ZI_BU: // 补操作
        {
            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                if (operate_card == m_gangCardResult.cbCardData[i]) {
                    // 是否有抢杠胡
                    table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
                            false);
                    return true;
                }
            }
            break;
        }
        case GameConstants.WIK_NULL: {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            // table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();

            return true;
        }
        }

        return true;
    }
    
	@Override
	public boolean handler_player_be_in_room(MJTable_YYZXZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		tableResponse.setSendCardData(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			/*for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}*/
			for (int j = 0; j < 55; j++) {
	            if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
	                // 癞子
	                int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
	            } else {
	                int_array.addItem(table.GRR._discard_cards[i][j]);
	            }
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
        
		
		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
                	hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
				else{
					for (int k = 0; k < out_ting_count; k++) {
						if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
							hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

			}
		}
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
            	hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End
		
		
		/*for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(hand_cards[j])) {
            	hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
        }
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);*/

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
