package com.cai.game.mj.chenchuang.xianning;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.mj.GameConstants_MJ_XIAN_NING;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGang_XianNing extends MJHandlerGang<Table_XianNing> {
    protected int _seat_index;
    protected int _provide_player;
    protected int _center_card;
    protected int _action;
    protected boolean _p;
    protected boolean _self;
    protected boolean _double;
    protected int _type;

    public HandlerGang_XianNing() {
    }

    @Override
    public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self,
            boolean d) {
        _seat_index = seat_index;
        _provide_player = provide_player;
        _center_card = center_card;
        _action = action;
        _type = type;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            _p = false;
        } else {
            _p = true;
        }
        _self = self;
        _double = d;
    }

    @Override
    public void exe(Table_XianNing table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (table._playerStatus[i].has_action()) {
                table.operate_player_action(i, true);
            }
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        // 根据不同的杠类型，播放不同的杠动画和语音
        switch (_type) {
        // 红中杠
        case GameConstants.GANG_TYPE_HONG_ZHONG:{
        	if(table.GRR._left_card_count > table.entrySeabedCount())
		    	table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
		    			new long[] { GameConstants.WIK_HONG_ZHONG_GANG }, 1, GameConstants.INVALID_SEAT);
        	break;
        }
        // 癞子杠
        case GameConstants.GANG_TYPE_LAI_ZI:{
        	if(table.GRR._left_card_count > table.entrySeabedCount())
		    	table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
		    			new long[] { GameConstants.WIK_LAI_ZI_GANG }, 1, GameConstants.INVALID_SEAT);
        	break;
        }
        // 暗杠
        case GameConstants.GANG_TYPE_AN_GANG:
            table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_AN_GANG_HB }, 1, GameConstants.INVALID_SEAT);
            break;
        // 明杠/接杠
        case GameConstants.GANG_TYPE_JIE_GANG:
            table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_JIE_GANG_HB }, 1, GameConstants.INVALID_SEAT);
            break;
        // 碰杠/回头杠
        case GameConstants.GANG_TYPE_ADD_GANG:
            table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_ADD_GANG_HB }, 1, GameConstants.INVALID_SEAT);
            break;
        }
        if(table.GRR._left_card_count > table.entrySeabedCount() && _type != GameConstants.GANG_TYPE_HONG_ZHONG)
	        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants.INVALID_SEAT);
        
        if ((GameConstants.GANG_TYPE_HONG_ZHONG == _type) || (GameConstants.GANG_TYPE_LAI_ZI == _type)) {
            this.exe_gang(table);
            exe_gang_dispatch_card(table);
            return;
        }
        boolean bAroseAction = table.estimate_gang_respond(_seat_index,GameConstants.GANG_TYPE_AN_GANG == _type ? 0 : _center_card);

        if (bAroseAction == false) {
            this.exe_gang(table);
            exe_gang_dispatch_card(table);
        } else {
        	this.exe_gang(table);
            PlayerStatus playerStatus = null;

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (table.has_chi_hu(playerStatus)) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    public boolean exe_gang(Table_XianNing table) {
        int cbCardIndex = table._logic.switch_to_card_index(_center_card);
        int cbWeaveIndex = -1;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
        	//玩家的番数+2
        	table.player_multiple_count[_seat_index] += 2;
        	
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;
            table.GRR._gang_score[_seat_index].an_gang_count++;
            table._player_result.an_gang_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
        	//玩家的番数+1
        	table.player_multiple_count[_seat_index]++;

            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;
            
            table.player_kai_kou[_seat_index]++;
            //记录每个玩家第三次开口的提供者（玩家位置索引+1，为0表示没有），用来处理包赔（胡牌者必须有清一色或者将一色的牌型）
            if(table.player_kai_kou[_seat_index] == 3)
            	table.player_kai_kou3[_seat_index] = _provide_player + 1;

            table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

            table.GRR._gang_score[_seat_index].ming_gang_count++;
            table._player_result.ming_gang_count[_seat_index]++;
            
          //清一色和将一色开2次口提醒
            if(table.player_kai_kou[_seat_index] == 2){
            	boolean is_qing_yi_se = table.is_qing_yi_se(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
            	boolean is_jiang_yi_se = table.is_jiang_yi_se(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
            	if(is_qing_yi_se || is_jiang_yi_se)
            		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
            				new long[] { GameConstants_MJ_XIAN_NING.WIK_CAI_GANG+1 }, 1);
            }
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
        	//玩家的番数+1
        	table.player_multiple_count[_seat_index]++;
        	
            for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
                int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
                int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
                if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
                    cbWeaveIndex = i;
                    break;
                }
            }

            if (cbWeaveIndex == -1) {
                table.log_player_error(_seat_index, "杠牌出错");
                return false;
            }

            table.GRR._gang_score[_seat_index].ming_gang_count++;
            table._player_result.ming_gang_count[_seat_index]++;
        }

        if (GameConstants.GANG_TYPE_AN_GANG == _type || GameConstants.GANG_TYPE_JIE_GANG == _type
                || GameConstants.GANG_TYPE_ADD_GANG == _type) {
            table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
            table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
            table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
            table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
            
            //删除手牌
            table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
        }

        table._current_player = _seat_index;
        //更新手牌数量
        table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
        //复制牌数据
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[_seat_index], cards);
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
        //添加特殊牌标记给客户端显示用
        for (int i = 0; i < hand_card_count; i++) {
            if (table._logic.is_magic_card(cards[i])) {
                cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            } else if (cards[i] == Constants_EZ.HZ_CARD) {
                cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
            }
        }

        if ((GameConstants.GANG_TYPE_HONG_ZHONG == _type) || (GameConstants.GANG_TYPE_LAI_ZI == _type)) {
        	if(GameConstants.GANG_TYPE_HONG_ZHONG == _type){
        		//玩家的番数+1
        		table.player_multiple_count[_seat_index]++;
        	}
        	else{
        		//癞子玩家的番数+2
        		table.player_multiple_count[_seat_index] +=2;
        		table.player_lai_zi_gang_count[_seat_index]++;
        	}
            table.GRR._lai_zi_pi_zi_gang[_seat_index][table.GRR._player_niao_count[_seat_index]++] = _center_card;
        }

        table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

        table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(
                table._playerStatus[_seat_index]._hu_cards, table._playerStatus[_seat_index]._hu_out_cards_fan[0], table.GRR._cards_index[_seat_index],
                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
        int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
        int ting_count = table._playerStatus[_seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
        } else {
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
        }

        // 每次牌桌上有番变动，都重新获取一次听牌数据
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            // 杠牌之后，杠牌人自己的听牌数据也得变动
            table._playerStatus[p]._hu_card_count = table.get_ting_card(table._playerStatus[p]._hu_cards, table._playerStatus[p]._hu_out_cards_fan[0],
                    table.GRR._cards_index[p], table.GRR._weave_items[p], table.GRR._weave_count[p], p);
            int tmp_ting_cards[] = table._playerStatus[p]._hu_cards;
            int tmp_ting_count = table._playerStatus[p]._hu_card_count;

            if (tmp_ting_count > 0) {
                table.operate_chi_hu_cards(p, tmp_ting_count, tmp_ting_cards);
            } else {
                tmp_ting_cards[0] = 0;
                table.operate_chi_hu_cards(p, 1, tmp_ting_cards);
            }
        }
        return true;
    }
    
    public void exe_gang_dispatch_card(Table_XianNing table) {
	    table.operate_player_info();
	    if(table.GRR._left_card_count == table.entrySeabedCount()){
	    	int next_player = (_seat_index + 1) % table.getTablePlayerNumber();
	    	table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
	    }else
	    	table.exe_dispatch_card(_seat_index, _type, 0);
    }

    @Override
    public boolean handler_operate_card(Table_XianNing table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return false;
        }

        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_discard_gang(seat_index);
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);

            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();
            }
        }
        
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            int cbUserActionRank = 0;
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = (table._playerStatus[i].get_perform() == GameConstants_MJ_XIAN_NING.WIK_CAI_GANG ? 40 : table._logic.get_action_rank(table._playerStatus[i].get_perform()))
                            + table.getTablePlayerNumber() - p;
                } else {
                    cbUserActionRank = (table._playerStatus[i].has_action_by_code(GameConstants_MJ_XIAN_NING.WIK_CAI_GANG) ? 40 : table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action)) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    cbTargetActionRank = (table._playerStatus[target_player].get_perform() == GameConstants_MJ_XIAN_NING.WIK_CAI_GANG ? 40 : table._logic.get_action_rank(table._playerStatus[target_player].get_perform()))
                            + target_p;
                } else {
                    cbTargetActionRank = (table._playerStatus[target_player].has_action_by_code(GameConstants_MJ_XIAN_NING.WIK_CAI_GANG) ? 40 : table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action)) + target_p;
                }

                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }

        if (table._playerStatus[target_player].is_respone() == false)
            return true;
        
        if (operate_code == GameConstants.WIK_CHI_HU) {
            int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(seat_index, 1, ting_cards);

            table.GRR._chi_hu_rights[seat_index].set_valid(true);
            //table.process_chi_hu_player_operate(seat_index, operate_card, false);
        }

        int target_card = table._playerStatus[target_player]._operate_card;

        switch (target_action) {
        case GameConstants.WIK_NULL: {
        	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                table.operate_player_action(i, true);
            }
            exe_gang_dispatch_card(table);

            return true;
        }
        case GameConstants_MJ_XIAN_NING.WIK_CAI_GANG: {//猜杠胡
        	/*// TODO:成功
        	int action = table.analyse_chi_hu_card_new(table.GRR._cards_index[target_player], table.GRR._weave_items[target_player], table.GRR._weave_count[target_player], operate_card, table.GRR._chi_hu_rights[target_player],
            		GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG, target_player , _provide_player);
        	if(action != GameConstants.WIK_NULL && table.cai_card[target_player] == _center_card){
        		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (i == target_player) {
                        table.GRR._chi_hu_rights[i].set_valid(true);
                    } else {
                        table.GRR._chi_hu_rights[i].set_valid(false);
                    }
                }
        		
        		table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_MJ_XIAN_NING.CHR_BEI_QIANG_GNAG);

                table._cur_banker = target_player;

                table.GRR._chi_hu_card[target_player][0] = target_card;

                table.process_chi_hu_player_operate(target_player, target_card, false);
                table.process_chi_hu_player_score(target_player, _seat_index, _center_card, false);

                table._player_result.jie_pao_count[target_player]++;
                table._player_result.dian_pao_count[_seat_index]++;

                GameSchedule.put(
                        new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
                return true;
        	}else{
                table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants_MJ_XIAN_NING.WIK_CAI_GANG }, 1, GameConstants.INVALID_SEAT);
        		table.GRR._game_score[target_player] -= 50;
        		table.GRR._game_score[_seat_index] += 50;
        		table.qiang_fail[target_player] = true;
        		exe_gang(table);
        		return true;
        	}*/
        	boolean exe_cai_hu = exe_cai_hu(table, target_player, target_card);
        	if(!exe_cai_hu){//猜胡失败
        		int i = target_player - _seat_index > 0 ? target_player - _seat_index : target_player - _seat_index + table.getTablePlayerNumber();
        		boolean exe_cai_hu2 = false;
        		for(;i != table.getTablePlayerNumber() - 1; ){
        			target_player = (target_player + 1) % table.getTablePlayerNumber();
        			if (table._playerStatus[target_player].has_action()) {
                        if (table._playerStatus[target_player].is_respone()) {
                        	if(table._playerStatus[target_player].get_perform() == GameConstants_MJ_XIAN_NING.WIK_CAI_GANG){
                        		exe_cai_hu2 = exe_cai_hu(table, target_player, table._playerStatus[target_player]._operate_card);
                        		if(exe_cai_hu2)
                        			break;
                        	}
                        } else {
                        	return false;
                        }
        			
        			}
        			i = target_player - _seat_index > 0 ? target_player - _seat_index : target_player - _seat_index + table.getTablePlayerNumber();
        		}
        		if(!exe_cai_hu2){
        			exe_gang_dispatch_card(table);
        			return true;
        		}
        	}
        	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                table.operate_player_action(i, true);
            }
        	return true;
        }
        case GameConstants.WIK_CHI_HU: {
        	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                table.operate_player_action(i, true);
            }
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == target_player) {
                    table.GRR._chi_hu_rights[i].set_valid(true);
                } else {
                    table.GRR._chi_hu_rights[i].set_valid(false);
                }
            }
            
            //把杠的牌变成碰
            /*table.GRR._gang_score[_seat_index].ming_gang_count--;
            table._player_result.ming_gang_count[_seat_index]--;
            int wIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_items[_seat_index][wIndex - 1].public_card = 1;
            table.GRR._weave_items[_seat_index][wIndex - 1].center_card = target_card;
            table.GRR._weave_items[_seat_index][wIndex - 1].weave_kind = GameConstants.WIK_PENG;
            table.GRR._weave_items[_seat_index][wIndex - 1].provide_player = _seat_index;*/
            //end
            
            table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_MJ_XIAN_NING.CHR_BEI_QIANG_GNAG);

            table._cur_banker = target_player;

            table.GRR._chi_hu_card[target_player][0] = target_card;

            table.process_chi_hu_player_operate1(target_player, target_card);
            table.process_chi_hu_player_score(target_player, _seat_index, _center_card, false);

            table._player_result.jie_pao_count[target_player]++;
            table._player_result.dian_pao_count[_seat_index]++;
            table.showAction(target_player);
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    1, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }
    
    public boolean exe_cai_hu(Table_XianNing table, int target_player, int target_card){
    	int action = table.analyse_chi_hu_card_new(table.GRR._cards_index[target_player], table.GRR._weave_items[target_player], table.GRR._weave_count[target_player], target_card, table.GRR._chi_hu_rights[target_player],
        		GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG, target_player , _provide_player);
    	if(action != GameConstants.WIK_NULL && target_card == _center_card){
    		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == target_player) {
                    table.GRR._chi_hu_rights[i].set_valid(true);
                } else {
                    table.GRR._chi_hu_rights[i].set_valid(false);
                }
            }
    		
    		table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_MJ_XIAN_NING.CHR_BEI_QIANG_GNAG);
    		table.GRR._chi_hu_rights[target_player].opr_or(GameConstants_MJ_XIAN_NING.CHR_CAI_GNAG);

            table._cur_banker = target_player;

            table.GRR._chi_hu_card[target_player][0] = target_card;

            table.process_chi_hu_player_operate1(target_player, target_card);
            table.process_chi_hu_player_score(target_player, _seat_index, _center_card, false);

            table._player_result.jie_pao_count[target_player]++;
            table._player_result.dian_pao_count[_seat_index]++;
            table.showAction(target_player);
            //把杠的牌变成碰
            /*table._player_result.an_gang_count[_seat_index]--;
            table.GRR._gang_score[_seat_index].an_gang_count--;
            int wIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_items[_seat_index][wIndex - 1].public_card = 1;
            table.GRR._weave_items[_seat_index][wIndex - 1].center_card = target_card;
            table.GRR._weave_items[_seat_index][wIndex - 1].weave_kind = GameConstants.WIK_PENG;
            table.GRR._weave_items[_seat_index][wIndex - 1].provide_player = _seat_index;*/
            //end
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    1, TimeUnit.SECONDS);
            return true;
    	}else if(!table.qiang_fail[target_player]){
            table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants_MJ_XIAN_NING.WIK_CAI_GANG }, 1, GameConstants.INVALID_SEAT);
    		table.GRR._game_score[target_player] -= 50 * table.getBottomScore();
    		table.GRR._game_score[_seat_index] += 50 * table.getBottomScore();
    		table.qiang_fail[target_player] = true;
    		return false;
    	}
    	return false;
    }

    @Override
    public boolean handler_player_be_in_room(Table_XianNing table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        // roomResponse.setTarget(seat_index);
        // roomResponse.setScoreType(table.get_player_fan_shu(seat_index));
        // table.send_response_to_other(seat_index, roomResponse);
        
        roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                int real_card = table.GRR._discard_cards[i][j];
                if (table._logic.is_magic_card(real_card)) {
                    real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                } else if (real_card == Constants_EZ.HZ_CARD) {
                    real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
                }
                int_array.addItem(real_card);
            }
            tableResponse.addDiscardCards(int_array);

            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
                weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
                weaveItem_item.setProvidePlayer(
                        table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);

                if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG
                        && table.GRR._weave_items[i][j].public_card == 0) {
                    // 暗杠的牌的显示
                    if (seat_index == i) {
                        weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                    } else {
                        weaveItem_item.setCenterCard(0);
                    }
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                }
                weaveItem_array.addWeaveItem(weaveItem_item);
            }
            tableResponse.addWeaveItemArray(weaveItem_array);

            tableResponse.addWinnerOrder(0);

            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
        }

        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[seat_index], hand_cards);

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            if (table._logic.is_magic_card(hand_cards[i])) {
                hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            } else if (hand_cards[i] == Constants_EZ.HZ_CARD) {
                hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
            }
            tableResponse.addCardsData(hand_cards[i]);
        }

        // TODO 添加是否托管
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(table.istrustee[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
            table.process_chi_hu_player_operate_reconnect(seat_index, _center_card, false); // 效果
        } else {
            int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
            int ting_count = table._playerStatus[seat_index]._hu_card_count;

            if (ting_count > 0) {
                table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);

                if (seat_index == _seat_index) {
                    // 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
                    table.operate_auto_win_card(seat_index, false);
                } else {
                    // 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
                    table.operate_auto_win_card(seat_index, true);
                }
            }

            // 根据不同的杠类型，播放不同的杠动画和语音
            switch (_type) {
            // 红中杠
            case GameConstants.GANG_TYPE_HONG_ZHONG:
                table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.WIK_HONG_ZHONG_GANG }, 1, seat_index);
                break;
            // 癞子杠
            case GameConstants.GANG_TYPE_LAI_ZI:
                table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.WIK_LAI_ZI_GANG }, 1, seat_index);
                break;
            // 暗杠
            case GameConstants.GANG_TYPE_AN_GANG:
                table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.WIK_AN_GANG_HB }, 1, seat_index);
                break;
            // 明杠/接杠
            case GameConstants.GANG_TYPE_JIE_GANG:
                table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.WIK_JIE_GANG_HB }, 1, seat_index);
                break;
            // 碰杠/回头杠
            case GameConstants.GANG_TYPE_ADD_GANG:
                table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.WIK_ADD_GANG_HB }, 1, seat_index);
                break;
            }

            if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
                table.operate_player_action(seat_index, false);
            }
        }

        return true;
    }
    
    @Override
    public boolean handler_be_set_trustee(Table_XianNing table, int seat_index) {
        return false;
    }
}
