package com.cai.game.mj.handler.shanxill;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_ShanXi_ll extends MJHandlerGang<MJTable_LVlIANG> {
    @Override
    public void exe(MJTable_LVlIANG table) {
        // TODO Auto-generated method stub
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (table._playerStatus[i].has_action()) {
                table.operate_player_action(i, true);
            }
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        //table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
            this.exe_gang(table);
            return;
        }

        // 检查对这个杠有没有 胡
        boolean bAroseAction = false;
        bAroseAction = table.estimate_gang_respond_ll(_seat_index, _center_card);

        if (bAroseAction == false) {
            this.exe_gang(table);
        } else {
            PlayerStatus playerStatus = null;

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (playerStatus.has_chi_hu()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
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
    public boolean handler_operate_card(MJTable_LVlIANG table, int seat_index, int operate_code, int operate_card) {
        // 抢杠胡

        // 效验状态
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 是否已经响应
        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return false;
        }

        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
        {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
            table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
            return false;
        }
        
        table.bAutoQiangGangHu[seat_index] = false;

        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
            table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
        } else if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
        } else {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        table.operate_player_action(seat_index, true);

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;
            // 优先级别
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    // 获取最大的动作的优先级
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }
                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 选择了不胡
        if (target_action == GameConstants.WIK_NULL) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_rights[i].set_valid(false);
            }
            // 发牌给杠的玩家
            this.exe_gang(table);
            return true;
        }
        
        operate_card = _center_card;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (i == target_player) {
                table.GRR._chi_hu_rights[i].set_valid(true);
            } else {
                table.GRR._chi_hu_rights[i].set_valid(false);
            }
        }
        table.process_chi_hu_player_operate(target_player, _center_card, false);

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }
        int jie_pao_count = 0;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                continue;
            }
            jie_pao_count++;
        }

        if (jie_pao_count > 0) {
            if(target_player == table._cur_banker){
                table._cur_banker = target_player;
            }
            else{
            	table._cur_banker = (table._cur_banker + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
            }

            table.GRR._chi_hu_card[target_player][0] = _center_card;
            
            //把被抢杠的牌移走
            table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_center_card)]--;
            
            //没报听把杠去掉
            //if(!table._playerStatus[_seat_index].is_bao_ting()){
            //	table.process_gang_score_ll(_seat_index);
            //}
            //table.process_chi_hu_player_score_ll(target_player, _seat_index, _center_card, false);
            
            //非破产模式
            if(!table.has_rule_pochan){
                //没报听把杠去掉
                if(!table._playerStatus[_seat_index].is_bao_ting()){
                	table.process_bao_gang_score_ll(_seat_index);
                }
                table.process_chi_hu_player_score_ll(target_player, _seat_index, _center_card, false);
            }else{//破产模式
            	 if(!table._playerStatus[_seat_index].is_bao_ting()){
                 	table.process_bao_gang_score_pochan(_seat_index);
                 }else{
                	 table.process_not_bao_gang_score_pochan();
                 }
                 table.process_chi_hu_player_score_pochan(target_player, _seat_index, _center_card, false);
            }
           
            // 记录
            table._player_result.jie_pao_count[target_player]++;
            table._player_result.dian_pao_count[_seat_index]++;

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

        }

        return true;
    }

    /**
     * 执行杠
     * 
     * 
     ***/
    @Override
    protected boolean exe_gang(MJTable_LVlIANG table) {
        int cbCardIndex = table._logic.switch_to_card_index(_center_card);
        int cbWeaveIndex = -1;

        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;

        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;
            // 删掉出来的那张牌
            table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
            // 看看是不是有碰的牌，明杠
            for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
                int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
                int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
                if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
                    cbWeaveIndex = i;// 第几个组合可以碰
                    _provide_player = _seat_index;// 找到放碰到人
                    break;
                }
            }
            if (cbWeaveIndex == -1) {
                table.log_player_error(_seat_index, "杠牌出错");
                return false;
            }

        }

        table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 2;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type;

        // 设置用户
        table._current_player = _seat_index;

        // 删除手上的牌
        table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
        table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
        // 刷新手牌包括组合
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
        }

        WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
        int weave_count = table.GRR._weave_count[_seat_index];
        for (int i = 0; i < weave_count; i++) {
            weaves[i] = new WeaveItem();
            weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
            weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
            weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
            weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player
                    + GameConstants.WEAVE_SHOW_DIRECT;
        }

        table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

        // 河南麻将,检查听牌
        table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(
                table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

        int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
        int ting_count = table._playerStatus[_seat_index]._hu_card_count;
        if (table._playerStatus[_seat_index].is_bao_ting()) {
            if (ting_count > 0) {
                table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
            } else {
                ting_cards[0] = 0;
                table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
            }
        }
        int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
        
        //算杠分
        int iCardValue = 0;
        int iCardColor = 0;
    	iCardColor = table._logic.get_card_color(_center_card);
    	iCardValue = table._logic.get_card_value(_center_card);
    	iCardValue = iCardColor > 2 ? 10 : iCardValue;
    	boolean bmacCard = false;
    	if(table._logic.is_magic_card(_center_card)) 
    		bmacCard = true;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {//暗杆
        	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == _seat_index)
                    continue;
                int score = table._difen * iCardValue * 2;
                if(bmacCard){
                	table.an_gang_laizi_score[i] -= 100;
                	table.an_gang_laizi_score[_seat_index] += 100;
                }
                else{
                    table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;
                    table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
                }

            }
            table._player_result.an_gang_count[_seat_index]++;
            
        }else if(GameConstants.GANG_TYPE_JIE_GANG == _type){//点杠
        	//癞子杠
			if(bmacCard){
				table.ming_gang_laizi_score[_provide_player] -= 300;
             	table.ming_gang_laizi_score[_seat_index] += 300;
			}else{
				if(table._playerStatus[_provide_player].is_bao_ting()){//包杠
					table.GRR._weave_items[_seat_index][cbWeaveIndex].is_lao_gang = true;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			            if (i == _seat_index)
			                continue;
			            int score = table._difen * iCardValue;
			            table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= score;
			            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
					}
				}
				else{
					table.GRR._weave_items[_seat_index][cbWeaveIndex].is_lao_gang = false;
					int score = table._difen*iCardValue;
					if(table.has_rule(GameConstants.GAME_RULE_BAO_GANG)){
						score *= 3;
			            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] -= score;
			            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
					}
					else{
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			                if (i == _seat_index)
			                    continue;
			                table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= score;
			                table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
						}
					}
				}
			}
            table._player_result.ming_gang_count[_seat_index]++;
            
        }else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {//补杠
        	int score = table._difen*iCardValue;
        	//癞子杠
			if(bmacCard){
				table.ming_gang_laizi_score[_provide_player] -= 300;
             	table.ming_gang_laizi_score[_seat_index] += 300;
			}else{
	    		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	                if (i == _seat_index)
	                    continue;
	                table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= score;
	                table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;
	            }
			}

            table._player_result.ming_gang_count[_seat_index]++;
        }

        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_AN_GANG_KAI, 0);
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type || GameConstants.GANG_TYPE_JIE_GANG == _type) {
            table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_BU_GANG_KAI, 0);
        } else {
            table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);
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


            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

        }

        // 数据
        tableResponse.setSendCardData(0);
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

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

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }

        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0 && table._playerStatus[seat_index].is_bao_ting() == true) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        return true;
    }
}
