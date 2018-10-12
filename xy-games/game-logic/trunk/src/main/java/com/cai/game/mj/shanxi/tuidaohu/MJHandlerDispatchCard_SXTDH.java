package com.cai.game.mj.shanxi.tuidaohu;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_SXJY;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_SXTDH extends MJHandlerDispatchCard<MJTable_SXTDH> {

	   boolean can_bao_ting = false;
	   boolean ting_send_card = false;
    @Override
    public void exe(MJTable_SXTDH table) {
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid(); // 可以胡了
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

        // 荒庄结束
        if (table.GRR._left_card_count == 0) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
            }
         // 荒庄庄家下家坐庄
            //table._cur_banker = table._last_dispatch_player;
            table._cur_banker = (_seat_index + (table.getTablePlayerNumber() - 1)) % table.getTablePlayerNumber(); // 最后摸牌的是下一局的庄家

            // 流局
            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

            return;
        }

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._current_player = _seat_index;// 轮到操作的人是自己

        // 从牌堆拿出一张牌
        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        if (AbstractMJTable.DEBUG_CARDS_MODE) {
        	_send_card_data = 0x09;
		}
        --table.GRR._left_card_count;

        table._provide_player = _seat_index;

        //固定发牌

        // 发牌处理,判断发给的这个人有没有胡牌或杠牌
        // 胡牌判断
        ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
        chr.set_empty();

        int action = GameConstants.WIK_NULL;
        if (!table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING) 
        		|| (table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING) && table._playerStatus[_seat_index].is_bao_ting())) { // 没打王牌弃胡
            // 胡牌检测
            action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                    table.GRR._weave_count[_seat_index], _send_card_data, chr,
                    Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO, _seat_index); // 自摸
        }

		// 添加动作
		if (GameConstants.WIK_NULL != action ) {
			// 如果发的这张牌能胡牌，当前玩家状态，添加相应的动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		}else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}
		
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		if (table._playerStatus[_seat_index].is_bao_ting() == true) {
        } else {int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card=false;
		
		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			if (table._logic.is_magic_index(i))
				continue;

			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

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
			can_bao_ting=true;
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
			//判断能不能添加报听的操作
	        if (!table._playerStatus[_seat_index].is_bao_ting() && table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING)) {
	        	table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
	        }
			
			
		}}
        
		
		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;//如果有报听提示
		}
        
		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);
    	


        // 当前牌桌记录提供的牌
        table._provide_card = _send_card_data;

        //如果是报听状态了
        if(table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING)&&table._playerStatus[_seat_index].is_bao_ting()){
    		//如果选择了“改变听口不能杠”玩法，那么杠完之后 不能改变所听的那几张牌
            if(table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_GAI_TING_KOU_NO_GANG)){
            	//如果杠完之后所听的牌没有发生改变，就把杠的动作加上去
            	if(!table.check_gang_huan_zhang(_seat_index,_send_card_data)){
            		
                	m_gangCardResult.cbCardCount = 0;
                    int cbActionMask = table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index],
                            _send_card_data, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                            m_gangCardResult);
        			if (cbActionMask != GameConstants.WIK_NULL) {
        				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
        						curPlayerStatus.add_action(GameConstants.WIK_GANG);	
        						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
        					
        				}
        			}
            	}
            }else{
            	//在没有报听的情况下，处理普通的杠牌操作
            	m_gangCardResult.cbCardCount = 0;
                int cbActionMask = table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index],
                        _send_card_data, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                        m_gangCardResult);
    			if (cbActionMask != GameConstants.WIK_NULL) {
    				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
    					if(m_gangCardResult.type[i]==GameConstants_SXJY.GANG_TYPE_AN_FENG_GANG){
    						curPlayerStatus.add_action(GameConstants.WIK_FENG_GANG);	
    						curPlayerStatus.add_feng_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
    					}else{
    						curPlayerStatus.add_action(GameConstants.WIK_GANG);	
    						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
    					}			
    					
    				}
    				
    			}			
            }
        }else{
        	//在没有报听的情况下，处理普通的杠牌操作
        	m_gangCardResult.cbCardCount = 0;
            int cbActionMask = table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index],
                    _send_card_data, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                    m_gangCardResult);
			if (cbActionMask != GameConstants.WIK_NULL) {
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if(m_gangCardResult.type[i]==GameConstants_SXJY.GANG_TYPE_AN_FENG_GANG){
						curPlayerStatus.add_action(GameConstants.WIK_FENG_GANG);	
						curPlayerStatus.add_feng_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}else{
						curPlayerStatus.add_action(GameConstants.WIK_GANG);	
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}			
					
				}
				
			}			
        }
       
        
        // 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮      
        if (curPlayerStatus.has_action()) { // 有动作
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            if (table._playerStatus[_seat_index].is_bao_ting()) {          	
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
                table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
            }
        }


        return;
    }

    @Override
    public boolean handler_operate_card(MJTable_SXTDH table, int seat_index, int operate_code,
            int operate_card) {
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
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            if (table._playerStatus[_seat_index].is_bao_ting()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
                table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
            }
            //如果勾选了报听规则，那么在能报听的时候，将角标全去掉，等用户点击报听按钮的时候，再去判断出哪几张牌能胡
            if (table.has_rule(Constants_SXTuiDaoHu.GAME_RULE_BAO_TING)&&can_bao_ting) {
                table.operate_player_cards_flag(_seat_index, 0, null, 0, null);
            }
         
            return true;
        }

        
        
        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_GANG: // 杠牌操作
        {
            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                if (operate_card == m_gangCardResult.cbCardData[i]) {
                    // 是否有抢杠胡
                    table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
                            false);
                    return true;
                }
            }

        }
            break;
        case GameConstants.WIK_ZI_MO: // 自摸
        {
            table.GRR._chi_hu_rights[_seat_index].set_valid(true);

            //如果胡牌的人不是庄家，
			if(table._cur_banker!=seat_index){
				table._cur_banker =  (table._cur_banker + (table.getTablePlayerNumber() +1)) % table.getTablePlayerNumber();	//若闲家胡牌则庄家的下家坐庄
			}else{
				table._cur_banker = seat_index;//庄家胡牌则继续坐庄
			}

            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            //这句干嘛的？？？
            table.GRR._win_order[_seat_index] = 1;
            
            table.process_chi_hu_player_operate(_seat_index, operate_card, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card,
                    Constants_SXTuiDaoHu.HU_CARD_TYPE_ZI_MO, true);

            // 记录
            table._player_result.zi_mo_count[_seat_index]++;

            // 结束
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        case GameConstants.WIK_BAO_TING: // 报听
		{
		     operate_card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			// 效验参数
			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_SXTDH table, int seat_index) {
    	RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		
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

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]); 
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						if(!table._playerStatus[seat_index].is_bao_ting()){
							hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						}					
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
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

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if(table._playerStatus[i].is_bao_ting()){
				table._playerStatus[i].set_card_status(GameConstants.CARD_STATUS_BAO_TING);
			}
		}
		

        
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

        // 癞子
        int real_card = _send_card_data;
        if(!table._playerStatus[seat_index].is_bao_ting()){
            if (ting_send_card) {
                real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
            }
        }
 
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		
		return true;
    }
}
