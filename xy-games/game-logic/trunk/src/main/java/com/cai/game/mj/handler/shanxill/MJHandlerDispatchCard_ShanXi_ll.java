package com.cai.game.mj.handler.shanxill;

import java.util.concurrent.TimeUnit;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_ShanXi_ll extends MJHandlerDispatchCard<MJTable_LVlIANG> {

    boolean ting_send_card = false;
    private boolean can_bao_ting;
    @Override
    public void exe(MJTable_LVlIANG table) {
        // 用户状态--清除状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        
		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].chi_peng_round_valid(); // 可以碰了
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();// 清理漏碰
        
		
        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();
        table._current_player = _seat_index;// 轮到操作的人是自己
        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        --table.GRR._left_card_count;
        table._provide_player = _seat_index;

        if (table.DEBUG_CARDS_MODE) {
        	_send_card_data = 0x17;
            //table.GRR._left_card_count = 12;
        }

        can_bao_ting = false;
        // 发牌处理,判断发给的这个人有没有胡牌或杠牌
        // 胡牌判断
        ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
        chr.set_empty();
        
        int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
        if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
            card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
        } else if (_type == GameConstants.HU_CARD_TYPE_AN_GANG_KAI) {
            card_type = GameConstants.HU_CARD_TYPE_AN_GANG_KAI;
        } else if (_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI) {
            card_type = GameConstants.HU_CARD_TYPE_BU_GANG_KAI;
        }
        
        if (table._playerStatus[_seat_index]._hu_card_count > 0
        		&& table._playerStatus[_seat_index].is_bao_ting() == true) {
            int action = table.analyse_chi_hu_card_shanxi_ll(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
                    card_type);
            
            //单吊耗子，不管摸到什么牌都可以自摸       或者报听了摸到癞子只能自动胡牌
            /*if(table._logic.is_magic_card(_send_card_data)){
            	int operate_card = _send_card_data;
                table.GRR._chi_hu_rights[_seat_index].set_valid(true);
                table.GRR._chi_hu_card[_seat_index][0] = operate_card;
                if(_seat_index == table._cur_banker){
                    table._cur_banker = _seat_index;
                }
                else{
                	table._cur_banker = (table._cur_banker + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
                }
                table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
                table.process_chi_hu_player_operate(_seat_index, operate_card, true);
                table.process_chi_hu_player_score_ll(_seat_index, _seat_index, operate_card, true);
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (i == _seat_index) {
                        continue;
                    }
                    table._biaoyan_count[i] = 0;
                }
                // 记录
                table._player_result.zi_mo_count[_seat_index]++;
                GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
                return ;
            }*/
            
            //单吊耗子
            if(table._playerStatus[_seat_index]._hu_card_count >= 25){
            	action = GameConstants.WIK_CHI_HU;
            }
            
            if (action != GameConstants.WIK_NULL) {// 自摸
                curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
                
                //10s后自动胡牌
            	table.autozimoplyer = _seat_index;
            	table.autocard = _send_card_data;
            	table.set_timer(table.ID_AUTO_ZIMO_HU,table.AUTO_HUPAI_TIMER);
            	table.bAutoZiMoHuPai = true;
                
            } else {
                chr.set_empty();
            }
        }
        // 刷新手牌包括组合
        int temcards[] = new int[GameConstants.MAX_COUNT];
        int temhand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], temcards);
        if (table.has_rule(GameConstants.GAME_RULE_DAN_HAO) || table.has_rule(GameConstants.GAME_RULE_SHUANG_HAO)) {
            for (int j = 0; j < temhand_card_count; j++) {
                if (table._logic.is_magic_card(temcards[j])) {
                	temcards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
        }
        
        WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
        int weave_count = table.GRR._weave_count[_seat_index];
        for (int i = 0; i < weave_count; i++) {
            weaves[i] = new WeaveItem();
            weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
            weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
            weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player+ GameConstants.WEAVE_SHOW_DIRECT;
            if(_type != GameConstants.HU_CARD_TYPE_AN_GANG_KAI && table.GRR._weave_items[_seat_index][i].public_card == 2){
            	table.GRR._weave_items[_seat_index][i].public_card = 0;
            }
            weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
            // 癞子
            if (table._logic.is_magic_card(weaves[i].center_card)) {
                weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
        }
        table.operate_player_cards(_seat_index, temhand_card_count, temcards, weave_count, weaves);
        
        
        // 加到手牌
        table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
        
        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        if(!table._playerStatus[_seat_index].is_bao_ting()){
        	int count = 0;
            int ting_count = 0;
            int send_card_index = table._logic.switch_to_card_index(_send_card_data);
            ting_send_card = false;

            int card_type_count = GameConstants.MAX_ZI_FENG;

            for (int i = 0; i < card_type_count; i++) {
                count = table.GRR._cards_index[_seat_index][i];

                if (count > 0) {
                    table.GRR._cards_index[_seat_index][i]--;

                    table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
                            table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

                    if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
                    	int iCardValue = 0;
                    	int iCardColor = 0;
                    	boolean bcanbaoting = false;
                    	for(int n = 0;n < table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count];n++){
                    		int temp = table._playerStatus[_seat_index]._hu_out_cards[ting_count][n];
                    		if(temp >= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
                    			continue;
                    		iCardColor = table._logic.get_card_color(temp);
                    		iCardValue = table._logic.get_card_value(temp);
                    		iCardValue = iCardColor > 2 ? 10 : iCardValue;
                    		if(iCardValue >= 6){
                    			bcanbaoting = true;
                    			break;
                    		}
                    	}
                    	
                    	if(bcanbaoting){
                            table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);
                            ting_count++;
                            if (send_card_index == i) {
                                ting_send_card = true;
                            }
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
                    } else {
                        for (int j = 0; j < ting_count; j++) {
                            if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                                cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            }
                        }
                    }
                }
                
                can_bao_ting = true;
                
                table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, weave_count, weaves);
                
                table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
            }
            // TODO: 出任意一张牌时，能胡哪些牌 -- End
        }
        
        // 癞子
        int real_card = _send_card_data;

    	if(table._logic.is_magic_card(_send_card_data)){
    		real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        }else{
        	if(ting_send_card){
        		real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        	}
        }

        // 只有自己才有数值
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

        // 设置变量
        table._provide_card = _send_card_data;// 提供的牌
        m_gangCardResult.cbCardCount = 0;

        int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true);

        if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
            if (table._playerStatus[_seat_index].is_bao_ting()) {
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    // 构造扑克
                    int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                        cbCardIndexTemp[j] = table.GRR._cards_index[_seat_index][j];
                    }
                    cbCardIndexTemp[table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])] = 0;
                    int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
                    int iting_count = 0;
                    if (table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]) == 1) {
                    	iting_count = table.get_ting_card(ting_cards, cbCardIndexTemp,
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index] + 1);
                    } else {
                    	iting_count = table.get_ting_card(ting_cards, cbCardIndexTemp,
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
                    }

                    if (iting_count > 0) {
                        // 加上刚
                        curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                m_gangCardResult.isPublic[i]);
                        curPlayerStatus.add_action(GameConstants.WIK_GANG);
                    }
                }
            } else {
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    // 加上刚
                    curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                            m_gangCardResult.isPublic[i]);
                    curPlayerStatus.add_action(GameConstants.WIK_GANG);
                }
            }

        }
            
        if(!curPlayerStatus.has_action_by_code(GameConstants.WIK_ZI_MO) && !curPlayerStatus.has_action_by_code(GameConstants.WIK_GANG)){
        	int llcard = 0;
            int gang_total_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                gang_total_count += table.GRR._gang_score[i].gang_count;
            }
            llcard = 12 + gang_total_count * 2;

            // 荒庄结束
            if (table.GRR._left_card_count <= llcard) {
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
                }
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                	for(int j = 0; j < table.GRR._gang_score[i].gang_count;j++){
                        table.GRR._gang_score[i].scores[j][i] = 0;
                        table.GRR._gang_score[i].scores[j][i] = 0;
                	}
                }
                table._cur_banker = table.GRR._banker_player;
                //table.handler_game_finish(table.GRR._banker_player, GameConstants.Game_End_DRAW);
                GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
                return;
            }
        }

        if (curPlayerStatus.has_action()) {// 有动作
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            // 不能换章,自动出牌
            if (table._playerStatus[_seat_index].is_bao_ting()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD+240, TimeUnit.MILLISECONDS);
            } else {
                table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
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
    public boolean handler_operate_card(MJTable_LVlIANG table, int seat_index, int operate_code, int operate_card) {
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
        table.bAutoZiMoHuPai = false;
        //有杠或者胡没有操作，而且是最后一张牌则荒庄
        if(operate_code != GameConstants.WIK_ZI_MO && operate_code != GameConstants.WIK_GANG){
        	int llcard = 0;
            int gang_total_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                gang_total_count += table.GRR._gang_score[i].gang_count;
            }
            llcard = 12 + gang_total_count * 2;

            // 荒庄结束
            if (table.GRR._left_card_count <= llcard) {
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
                }
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                	for(int j = 0; j < table.GRR._gang_score[i].gang_count;j++){
                        table.GRR._gang_score[i].scores[j][i] = 0;
                        table.GRR._gang_score[i].scores[j][i] = 0;
                	}
                }
                table._cur_banker = table.GRR._banker_player;
                //table.handler_game_finish(table.GRR._banker_player, GameConstants.Game_End_DRAW);
                GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
                return true;
            }
        }
        
        
        // 记录玩家的操作
        playerStatus.operate(operate_code, operate_card);
        table.change_player_status(seat_index, GameConstants.INVALID_VALUE);

        // 放弃操作
        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
        	//能胡不胡，要过圈
            if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
                table._playerStatus[seat_index].chi_hu_round_invalid();
            }
            
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
            if (table._playerStatus[_seat_index].is_bao_ting()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD+240, TimeUnit.MILLISECONDS);
            } else {
                table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
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
            table.GRR._chi_hu_card[_seat_index][0] = operate_card;
            if(_seat_index == table._cur_banker){
                table._cur_banker = _seat_index;
            }
            else{
            	table._cur_banker = (table._cur_banker + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
            }
            table.process_chi_hu_player_operate(_seat_index, operate_card, true);
            
            //算分
            if(!table.has_rule_pochan){
            	//胡分
                table.process_chi_hu_player_score_ll(_seat_index, _seat_index, operate_card, true);
            }else{
            	//先算杠分
            	table.process_not_bao_gang_score_pochan();
            	//胡分
            	table.process_chi_hu_player_score_pochan(_seat_index, _seat_index, operate_card, true);
            }

            
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == _seat_index) {
                    continue;
                }
                table._biaoyan_count[i] = 0;
            }
            // 记录
            table._player_result.zi_mo_count[_seat_index]++;
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
            return true;
        }
        case GameConstants.WIK_BAO_TING: //
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
    public boolean handler_player_be_in_room(MJTable_LVlIANG table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        // 游戏变量
        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(_seat_index);
        tableResponse.setCellScore(0);

        // 状态变量
        tableResponse.setActionCard(0);
        // tableResponse.setActionMask((_response[seat_index] == false) ?
        // _player_action[seat_index] : MJGameConstants.WIK_NULL);
        
        
        //色子
		if(table._cur_round == 1){
			roomResponse.setEffectCount(4);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[2]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[3]);
		}
		else{
			roomResponse.setEffectCount(2);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
		}

		
        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);// 是否托管
            // 剩余牌数
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
            	int iCardIndex = table.GRR._discard_cards[i][j];
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    // 癞子
                    //int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
                	iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                	
                } else {
                    //int_array.addItem(table.GRR._discard_cards[i][j]);
                }
                if(j == table.GRR._chi_hu_rights[i].bao_ting_index){
                	if(iCardIndex > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
                		iCardIndex -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                	iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
                }
                int_array.addItem(iCardIndex);
            }
            tableResponse.addDiscardCards(int_array);

            // 组合扑克
            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
                    weaveItem_item.setCenterCard(
                            table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                }
                weaveItem_item.setProvidePlayer(
                        table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
                weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
                weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
                weaveItem_array.addWeaveItem(weaveItem_item);
            }
            tableResponse.addWeaveItemArray(weaveItem_array);

            //
            tableResponse.addWinnerOrder(0);

            // 牌

            if (i == _seat_index) {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
            } else {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            }

        }

        // 数据
        tableResponse.setSendCardData(0);
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

        // 如果断线重连的人是自己
        if (seat_index == _seat_index) {
            table._logic.remove_card_by_data(cards, _send_card_data);
        }
        // 癞子
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
        }
        
        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);


        boolean bSendCardBaoTing = false;
        if (_seat_index == seat_index) {
            //报听
        	int send_card_index = table._logic.switch_to_card_index(_send_card_data);
        	table.GRR._cards_index[_seat_index][send_card_index]--;
        	
            int baotingcards[] = new int[GameConstants.MAX_COUNT];
            int baotingcount = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], baotingcards);
            
            table.GRR._cards_index[_seat_index][send_card_index]++;
            
            if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
                // 如果可以报听,刷新自己的手牌
                int ting_count = table._playerStatus[_seat_index]._hu_out_card_count;
                if (ting_count > 0) {
                    for (int i = 0; i < baotingcount; i++) {
                        for (int j = 0; j < ting_count; j++) {
                        	if(table._logic.is_magic_card(baotingcards[i]))
                        		baotingcards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                        	 if (baotingcards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) 
                        		 baotingcards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                        	 if(_send_card_data == table._playerStatus[_seat_index]._hu_out_card_ting[j])
                        		 bSendCardBaoTing = true;
                        }
                    }
                    table.operate_player_cards_with_ting(_seat_index, baotingcount, baotingcards, 0, null);
                }  
            }
        }
        
  
        // 癞子
        int real_card = _send_card_data;
        if (table._logic.is_magic_card(_send_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        }else{
        	if(bSendCardBaoTing){
        		real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        	}
        }
       
        // 摸牌
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0 && table._playerStatus[seat_index].is_bao_ting() == true) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }
        return true;
    }
}
