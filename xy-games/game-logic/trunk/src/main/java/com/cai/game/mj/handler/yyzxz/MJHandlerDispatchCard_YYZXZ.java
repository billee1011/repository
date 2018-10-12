package com.cai.game.mj.handler.yyzxz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.handler.moyanggui.MjTableMoYangGui;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class MJHandlerDispatchCard_YYZXZ extends MJHandlerDispatchCard<MJTable_YYZXZ> {

	boolean ting_send_card = false; //听发的牌
    private final XiaZiCardResult xiaZiResult;

    public MJHandlerDispatchCard_YYZXZ() {
        super();
        xiaZiResult = new XiaZiCardResult();
    }
    
    @Override
    public void exe(MJTable_YYZXZ table) {
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

        // 荒庄结束
        if (table.GRR._left_card_count == 0) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
            }
            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
            table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
            return;
        }

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._current_player = _seat_index;// 轮到操作的人是自己

        xiaZiResult.cbCardCount = 0;

	    table._send_card_count++;
	    int card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
	    --table.GRR._left_card_count;
	    // card = 0x01;
	    _send_card_data = card;
	    
	    
        if (table.DEBUG_CARDS_MODE) {
            //_send_card_data = 0x09;
        }

        cardCount = 1;
        table.preDisPatchCardPlayer = _seat_index;
        table._provide_player = _seat_index;

        // 有虾牌就不能胡
        // 发牌处理,判断发给的这个人有没有胡牌或杠牌
        // 胡牌判断
        boolean havamatianfei = false;
        if(table.has_rule(GameConstants.ZXZ_MAN_TIAN_FEI)){
        	havamatianfei = true;
        }
        if( havamatianfei || (table._logic.get_card_color(_send_card_data) != 0 && !havamatianfei)){
            ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
            chr.set_empty();
            int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
                    GameConstants.HU_CARD_TYPE_ZIMO, _seat_index);// 自摸
            if (action != GameConstants.WIK_NULL) {
                // 添加动作
                curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

            } else {
                chr.set_empty();
            }
        }

        // 加到手牌
        table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

        
     // 出任意一张牌时，能胡哪些牌 -- Begin
 		int count = 0;
 		int ting_count = 0;
 		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
 		ting_send_card = false;

 		int card_type_count = GameConstants.MAX_ZI_FENG;

 		for (int i = 0; i < card_type_count; i++) {
 			count = table.GRR._cards_index[_seat_index][i];

 			if (count > 0) {
 				table.GRR._cards_index[_seat_index][i]--;
 				//打出哪些牌可以听牌，同时得到听牌的数量，把可以胡牌的数据data型放入table._playerStatus[_seat_index]._hu_out_cards[ting_count]
 				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
 						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
 						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

 				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
 					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

 					ting_count++;

 					if (send_card_index == i) {
 						ting_send_card = true;
 					}
 				}

 				table.GRR._cards_index[_seat_index][i]++;
 			}
 		}

 		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

 		if (ting_count > 0) {
 			table.GRR._cards_index[_seat_index][send_card_index]--;

 			int cards[] = new int[GameConstants.MAX_COUNT];
 			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

 			table.GRR._cards_index[_seat_index][send_card_index]++;

 			for (int i = 0; i < hand_card_count; i++) {
 				if (table._logic.is_magic_card(cards[i])) {
                    cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
 				else{
 					for (int j = 0; j < ting_count; j++) {
 	 					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
 	 						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
 	 						break;
 	 					}
 	 				}
 				}
 			}

 			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
 		}
 		// 出任意一张牌时，能胡哪些牌 -- End
        
        //虾子
        int re_card = _send_card_data;
        if (table._logic.checkWanZi(_send_card_data) && havamatianfei) {
        	re_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        }else if(ting_send_card){
        	re_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }
        // 发送数据
        // 只有自己才有数值
        table.operate_player_get_card(_seat_index, 1, new int[] {re_card}, GameConstants.INVALID_SEAT);

        // 设置变量
        table._provide_card = _send_card_data;// 提供的牌

        m_gangCardResult.cbCardCount = 0;

        if (table.GRR._left_card_count >= 1) {
            // 看手上所有的牌,有没有杠
            int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
                    0);

            if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
                curPlayerStatus.add_action(GameConstants.WIK_XIA_ZI_BU);
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    // 加上刚
                    //curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
                	curPlayerStatus.add_XiaZi(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
                }
            }
        }

        if (curPlayerStatus.has_action()) {// 有动作
            // 操作状态
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {

            // 出牌状态
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            // 不能换章,自动出牌
            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
                table.operate_player_status();
            }
        }
        return;
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
        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }
        // 记录玩家的操作
        playerStatus.operate(operate_code, operate_card);
        playerStatus.clean_status();

        // 放弃操作
        if (operate_code == GameConstants.WIK_NULL) {
        	//能胡不胡，要过圈
            if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
                table._playerStatus[seat_index].chi_hu_round_invalid();
            }
            
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
                // table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
                table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
            }

            return true;
        }
        
        

        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_GANG: // 杠牌操作
        case GameConstants.WIK_XIA_ZI_BU: // 杠牌操作
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

        case GameConstants.WIK_ZI_MO: // 自摸
        {
            table.GRR._chi_hu_rights[_seat_index].set_valid(true);

            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            table._cur_banker = _seat_index;

            if (isGang) {
                table.GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
            }
            

            table.process_chi_hu_player_operate(_seat_index, new int[] { operate_card }, 1, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

            // 记录
            ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
            if (!(chr.opr_and(GameConstants.CHR_DA_HU)).is_empty()) {
                table._player_result.da_hu_zi_mo[_seat_index]++;
            } else {
                table._player_result.xiao_hu_zi_mo[_seat_index]++;
            }
       
            table._player_result.zi_mo_count[_seat_index]++;
            
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

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

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}
		
		boolean bSendCardCanOutTing = false;
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
		
		/*if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (hand_cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
                if (table._logic.is_magic_card(hand_cards[i])) {
                	hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
			tableResponse.addCardsData(hand_cards[i]);
		}*/

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
		

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		
		int isendcard = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			isendcard += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}else {
            if (ting_send_card) {
            	isendcard += GameConstants.CARD_ESPECIAL_TYPE_TING;
            }
        }
		table.operate_player_get_card(_seat_index, 1, new int[] { isendcard }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

    public class XiaZiCardResult {
        public int cbCardCount;// 扑克数目
        public int cbCardData[];// 扑克数据
        public int isPublic[];//
        public int type[];// ;类型

        public XiaZiCardResult() {
            cbCardData = new int[9];
            isPublic = new int[9];
            type = new int[9];
        }
    }
}
