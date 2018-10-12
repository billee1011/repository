package com.cai.game.mj.handler.shanxill;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate_ShanXi_ll extends MJHandlerOutCardOperate<MJTable_LVlIANG> {

    @Override
    public void exe(MJTable_LVlIANG table) {
    	
        // TODO Auto-generated method stub
        PlayerStatus playerStatus = table._playerStatus[_out_card_player];

        // 重置玩家状态
        table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
        playerStatus.clean_action();

        // 出牌记录
        table._out_card_count++;
        table._out_card_player = _out_card_player;
        table._out_card_data = _out_card_data;

        // 用户切换
        int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
        table._current_player = next_player;

        // 刷新手牌
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
        if (table.has_rule(GameConstants.GAME_RULE_DAN_HAO) || table.has_rule(GameConstants.GAME_RULE_SHUANG_HAO)) {
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(cards[j])) {
                    cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
        }

        table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

        // 癞子
        int real_card = _out_card_data;
         if (table._logic.is_magic_card(_out_card_data)) {
        	 real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
         }
         
         if(table.bBaoTingCard == true){
        	 if(real_card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
        	 {
        		 real_card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        	 }
    		 real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
    		 table.bBaoTingCard = false;
         }
        // 显示出牌
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                GameConstants.INVALID_SEAT);

        table._provide_player = _out_card_player;
        table._provide_card = _out_card_data;
        
        table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);

        // 玩家出牌 响应判断,是否有吃碰杠补胡
        boolean bAroseAction = table.estimate_player_out_card_respond_shanxi_ll(_out_card_player, _out_card_data);// ,
                                                                                                                 // EstimatKind.EstimatKind_OutCard

        // 如果没有需要操作的玩家，派发扑克
        if (bAroseAction == false) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
            }

            table.operate_player_action(_out_card_player, true);

            // 发牌
            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

        } else {
            // 等待别人操作这张牌
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (table._playerStatus[i].has_action()) {
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
        // 效验状态

        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 是否已经响应
        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return true;
        }

        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
        {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return true;
        }
        
        table.bAutoHuPai[seat_index] = false;
        
        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
        } 
        else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }
        if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
            table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
        } 
        
		// 漏碰规则
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_PENG)
				&& operate_code != GameConstants.WIK_PENG) {
			table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
		}

        // 变量定义 优先级最高操作的玩家和操作--不通炮的算法
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
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
        if (table._playerStatus[target_player].is_respone() == false && target_player != _out_card_player)
            return true;

        // 修改网络导致吃碰错误 9.26 WalkerGeek
        int target_card = _out_card_data;
        if (!table.has_rule(GameConstants.GAME_RULE_PUYANG_HUANGHUNAG)) {
            if (target_action == GameConstants.WIK_CHI_HU) {
                table.GRR._chi_hu_rights[target_player].set_valid(true);// 胡牌生效
                // 效果
                table.process_chi_hu_player_operate(target_player, target_card, false);
            }
        }


        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 删除扑克
        switch (target_action) {
        case GameConstants.WIK_LEFT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.remove_discard_after_operate(_out_card_player, _out_card_data);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_RIGHT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.remove_discard_after_operate(_out_card_player, _out_card_data);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_CENTER: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.remove_discard_after_operate(_out_card_player, _out_card_data);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_PENG: // 碰牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }
            table.remove_discard_after_operate(_out_card_player, _out_card_data);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }

        case GameConstants.WIK_GANG: // 杠牌操作
        {
            // 是否有抢杠胡
        	table.remove_discard_after_operate(_out_card_player, _out_card_data);
            table.exe_gang(target_player, _out_card_player, target_card, target_action,
                    GameConstants.GANG_TYPE_JIE_GANG, false, false);
            return true;
        }
        case GameConstants.WIK_NULL: {

            // 删掉出来的那张牌
            int real_card = _out_card_data;
            if (table._logic.is_magic_card(_out_card_data)) {// 癞子替换
                real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }

            // 用户切换
            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();

            // 发牌
            table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
        	
            int jie_pao_count = 0;
            int hupai_index = seat_index;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                hupai_index = i;
                jie_pao_count++;
            }
            if (jie_pao_count > 0) {

            	if(jie_pao_count > 1){
            		int ihuTemp = hupai_index;
            		ihuTemp = table.GetNextHuPaiPlayer(_out_card_player);
            		hupai_index = ihuTemp == _out_card_player ? hupai_index : ihuTemp;
            	}
            	
                if(hupai_index == table._cur_banker){
                    table._cur_banker = hupai_index;
                }
                else{
                	table._cur_banker = (table._cur_banker + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
                }
                
                //table._cur_banker = hupai_index;

                table.GRR._chi_hu_card[hupai_index][0] = _out_card_data;

                table.process_chi_hu_player_operate(hupai_index, new int[] { _out_card_data }, 1, false);
                
                //非破产模式
                if(!table.has_rule_pochan){
                    //没报听把杠去掉,并且，包杠分
                    if(!table._playerStatus[_out_card_player].is_bao_ting() && table.has_rule(GameConstants.GAME_RULE_DA_BAO_HU)){
                    	table.process_bao_gang_score_ll(_out_card_player);
                    }
                    table.process_chi_hu_player_score_ll(hupai_index, _out_card_player, _out_card_data, false);
                }else{//破产模式
                	 if(!table._playerStatus[_out_card_player].is_bao_ting() && table.has_rule(GameConstants.GAME_RULE_DA_BAO_HU)){
                     	table.process_bao_gang_score_pochan(_out_card_player);
                     }else{
                    	 table.process_not_bao_gang_score_pochan();
                     }
                     table.process_chi_hu_player_score_pochan(hupai_index, _out_card_player, _out_card_data, false);
                }

                // 记录
                table._player_result.jie_pao_count[hupai_index]++;
                table._player_result.dian_pao_count[_out_card_player]++;

                GameSchedule.put(
                        new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            }
            for(int i = 0; i < table.getTablePlayerNumber();i++){
            	if(i == hupai_index) continue;
            	table.GRR._chi_hu_rights[i].set_valid(false);
            }
        }
        default:
            return false;
        }
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
        tableResponse.setCurrentPlayer(_out_card_player);
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

        int real_card = _out_card_data;
        if (table._logic.is_magic_card(_out_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        }
        // 出牌
        //table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
        //        seat_index);

        // table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
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
