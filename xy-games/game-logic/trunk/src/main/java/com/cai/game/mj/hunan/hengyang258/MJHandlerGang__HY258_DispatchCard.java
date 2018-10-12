package com.cai.game.mj.hunan.hengyang258;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.RemoveOutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang__HY258_DispatchCard extends AbstractMJHandler<MJTable_HY258> {

    private int _seat_index;
    private boolean _double;

    private GangCardResult m_gangCardResult;
    private CardsData _gang_card_data;

    public MJHandlerGang__HY258_DispatchCard() {
        _gang_card_data = new CardsData(GameConstants.MAX_COUNT);
        m_gangCardResult = new GangCardResult();
    }

    public void reset_status(int seat_index, boolean d) {
        _seat_index = seat_index;
        _double = d;
    }

    public void reset_status(int seat_index, int provide_player, int center_card, int action, boolean p, boolean self) {
        _seat_index = seat_index;
        // _provide_player = provide_player;
        // _center_card = center_card;
        // _action = action;
        // _p = p;
        // _self = self;
    }

    @Override
    public void exe(MJTable_HY258 table) {
        ChiHuRight chr = null;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            // table._playerStatus[i].clean_status();
            chr = table.GRR._chi_hu_rights[i];
            chr.set_empty();
            table.operate_player_action(i, true);
        }
        table._playerStatus[_seat_index].set_card_status(GameConstants.CARD_STATUS_CS_GANG);
        table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

        this._gang_card_data.clean_cards();

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        // 设置变量
        table._out_card_data = GameConstants.INVALID_VALUE;
        table._out_card_player = GameConstants.INVALID_SEAT;
        table._current_player = _seat_index;// 轮到操作的人是自己

        table._provide_player = _seat_index;

        int bu_card;
        // 出牌响应判断

        // 从牌堆拿出2张牌
        for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
            table._send_card_count++;
            bu_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
            if (AbstractMJTable.DEBUG_CARDS_MODE) {
                // if(i==0)bu_card=0x14;
                // if(i==1)bu_card=0x15;
                if (i == 0)
                    bu_card = 0x15;
                if (i == 1)
                    bu_card =  0x27;
            }
            --table.GRR._left_card_count;
            this._gang_card_data.add_card(bu_card);
        }

        // 显示两张牌
        table.operate_out_card(_seat_index, GameConstants.CS_GANG_DRAW_COUNT, this._gang_card_data.get_cards(),
                GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

        boolean has_action = false;
        m_gangCardResult.cbCardCount = 0;
        // 显示玩家对这两张牌的操作
        for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
            boolean bAroseAction = false;
            bu_card = this._gang_card_data.get_card(i);

            for (int k = 0; k < table.getTablePlayerNumber(); k++) {
                // 自己只有 自摸
                if (k == _seat_index) {
                    chr = table.GRR._chi_hu_rights[k];
                    int action = table.analyse_chi_hu_card(table.GRR._cards_index[k], table.GRR._weave_items[k],
                            table.GRR._weave_count[k], bu_card, chr, GameConstants.HU_CARD_TYPE_ZIMO, k);// 自摸
                    if (action != GameConstants.WIK_NULL) {
                        if (curPlayerStatus.has_zi_mo() == false) {
                            curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                            curPlayerStatus.add_zi_mo(bu_card, k);
                        }

                        bAroseAction = true;
                    }

                    // 如果牌堆还有牌，判断能不能杠
                    if (table.GRR._left_card_count > 2) {
                    	/*int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,
                                true);*/

                    	int cbActionMask = table._logic.analyse_gang_card_cs(table.GRR._cards_index[k], bu_card,
                                table.GRR._weave_items[k], table.GRR._weave_count[k], m_gangCardResult);
                        if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
                        	
                        	 for (int gc = 0; gc < m_gangCardResult.cbCardCount; gc++) {
                                  
                                 boolean can_gang = false;
                                 int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[gc]);
                                 int save_count = table.GRR._cards_index[_seat_index][bu_index];
                                 table.GRR._cards_index[_seat_index][bu_index] = 0;

                                 int cbWeaveIndex = table.GRR._weave_count[_seat_index];

                                 if (m_gangCardResult.type[gc] == GameConstants.GANG_TYPE_AN_GANG) {
                                     table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
                                     table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[gc];
                                     table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                                     table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
                                     table.GRR._weave_count[_seat_index]++;
                                 }

                                 if (curPlayerStatus.lock_huan_zhang()) {
                                     boolean has_huan_zhang = table.check_gang_huan_zhang(_seat_index,
                                             m_gangCardResult.cbCardData[gc]);
                                     can_gang = !has_huan_zhang;
                                 } else {
                                     can_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
                                             table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                             _seat_index);
                                 }
                                 
                                 int count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], new int[GameConstants.MAX_COUNT]);
                                 //剩下的牌不是258就不能杠
                                 if(count == 1 && can_gang){
                                 	// 构造扑克
                                 	int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
                                 	for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                                 		cbCardIndexTemp[j] = table.GRR._cards_index[_seat_index][j];
                                 	}
                                 	for(int z = 0; z < GameConstants.MAX_INDEX; z++){
                                 		if(cbCardIndexTemp[z] > 0){
                                 			int action_can = table.analyse_chi_hu_card(cbCardIndexTemp,table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                 					table._logic.switch_to_card_data(z), new ChiHuRight(), GameConstants.HU_CARD_TYPE_ZIMO, _seat_index);
                                 			if(action_can == 0){
                                 				can_gang = false;
                                 			}
                                 		}
                                 	}
                                 }

                                 table.GRR._weave_count[_seat_index] = cbWeaveIndex;
                                 table.GRR._cards_index[_seat_index][bu_index] = save_count;

                                 if (can_gang) {
                                     curPlayerStatus.add_action(GameConstants.WIK_GANG);
                                     curPlayerStatus.add_gang(m_gangCardResult.cbCardData[gc], _seat_index,
                                             m_gangCardResult.isPublic[gc]);
                                 }
                             }
                        }
                    }

                } else {
                    int chi_seat_index = (_seat_index + 1) % table.getTablePlayerNumber();
                    if (k == chi_seat_index) {
                        bAroseAction = table.estimate_gang_hy_respond(k, _seat_index, bu_card, _double, true);// ,
                                                                                                              // EstimatKind.EstimatKind_OutCard
                    } else {
                        bAroseAction = table.estimate_gang_hy_respond(k, _seat_index, bu_card, _double, false);// ,
                                                                                                               // EstimatKind.EstimatKind_OutCard
                    }

                }
                // 出牌响应判断

                // 如果没有需要操作的玩家，派发扑克
                if (bAroseAction == true) {
                    has_action = true;
                    
                }
            }
        }

        if (has_action == false) {

            table._provide_player = GameConstants.INVALID_SEAT;
            table._out_card_player = _seat_index;

            GameSchedule.put(
                    new RemoveOutCardRunnable(table.getRoom_id(), _seat_index, GameConstants.OUT_CARD_TYPE_LEFT),
                    GameConstants.GANG_CARD_CS_DELAY, TimeUnit.MILLISECONDS);
            GameSchedule.put(
                    new AddDiscardRunnable(table.getRoom_id(), _seat_index, this._gang_card_data.get_card_count(),
                            this._gang_card_data.get_cards(), true, table.getMaxCount()),
                    GameConstants.GANG_CARD_CS_DELAY, TimeUnit.MILLISECONDS);

            // 继续发牌
            table._current_player = (_seat_index + 1) % table.getTablePlayerNumber();

            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.GANG_CARD_CS_DELAY);

        } else {
            table._provide_player = _seat_index;
            // 玩家有操作
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (table._playerStatus[i].has_action()) {
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
    public boolean handler_operate_card(MJTable_HY258 table, int seat_index, int operate_code, int operate_card) {
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

        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        // 别人胡
        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效--结算根据这个标记
            // 效果
            if (table._playerStatus[_seat_index].has_zi_mo() == false
                    || table._playerStatus[_seat_index].is_respone() == true) {
            }

        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
            }
        }

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;

        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action);
                }

                // 优先级别
                int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                }
            }
        }

        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        if (target_action != GameConstants.WIK_ZI_MO) {// 包含了 可以杠上开花的人不胡 + 杆山炮的人胡
            // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                    return false;
            }
        }
        // 变量定义
        int target_card = table._playerStatus[target_player]._operate_card;

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

            this.exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_RIGHT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            this.exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_CENTER: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            this.exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_PENG: // 碰牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }

            this.exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_BU_ZHNAG: // 补张牌操作
        case GameConstants.WIK_GANG: // 杠牌操作
        {

            // 删掉出来的那两张张牌
            table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT,
                    GameConstants.INVALID_SEAT);

            int add_card = -1;
            boolean card_check = true;
            for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
                // 是摸出来的牌
                if (card_check && (target_card == this._gang_card_data.get_card(i))) {
                    card_check = false;
                } else {
                    add_card = this._gang_card_data.get_card(i);
                }
            }
            if (add_card == -1) {
                table.exe_add_discard(this._seat_index, 2, this._gang_card_data.get_cards(), true, 0);
            } else {
                table.exe_add_discard(this._seat_index, 1, new int[] { add_card }, true, 0);
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                table.operate_player_action(i, true);
            }

            if (_seat_index == target_player) {
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    if (target_card == m_gangCardResult.cbCardData[i]) {
                        // 是否有抢杠胡
                        table.exe_gang(_seat_index, _seat_index, target_card, target_action, m_gangCardResult.type[i],
                                true, true);
                        return true;
                    }
                }
            } else {
                table.exe_gang(target_player, _seat_index, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG,
                        false, false);
            }

            return true;
        }
        case GameConstants.WIK_NULL: {

            // 删掉出来的那张牌
            table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT,
                    GameConstants.INVALID_SEAT);

            // 剩下的牌放到牌堆
            table.exe_add_discard(this._seat_index, 2, this._gang_card_data.get_cards(), true, 0);

            // 用户切换
            table._current_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
            // 发牌
            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_ZI_MO: // 自摸
        {

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == _seat_index) {
                    table.GRR._chi_hu_rights[i].set_valid(true);
                } else {
                    table.GRR._chi_hu_rights[i].set_valid(false);
                }
            }

            // 下局胡牌的是庄家
            table.set_niao_card(table.GRR._banker_player, GameConstants.INVALID_VALUE, true, 0, false,
                    GameConstants.HU_CARD_TYPE_ZIMO);// 结束后设置鸟牌MJGameConstants.INVALID_VALUE

            table.GRR._chi_hu_rights[_seat_index].set_valid(true);

            // 有两个自摸
            if (table.GRR._chi_hu_rights[_seat_index].is_mul(GameConstants.CHR_ZI_MO)) {
                // 结束信息
                ChiHuRight chrtemp = new ChiHuRight();
                int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                        table.GRR._weave_count[_seat_index], this._gang_card_data.get_card(0), chrtemp,
                        GameConstants.HU_CARD_TYPE_ZIMO, _seat_index);// 自摸
                ChiHuRight chrtemp2 = new ChiHuRight();
                int action1 = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                        table.GRR._weave_count[_seat_index], this._gang_card_data.get_card(1), chrtemp2,
                        GameConstants.HU_CARD_TYPE_ZIMO, _seat_index);// 自摸
                
                table.GRR._chi_hu_rights[_seat_index].set_empty();
                table.GRR._chi_hu_rights[_seat_index].copy(chrtemp);
    			for (int j = 0; j < chrtemp2.type_count; j++) {
    				table.GRR._chi_hu_rights[_seat_index].opr_or(chrtemp2.type_list[j]);
    			}

                if(action > 0 && action1 > 0){
                     table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI);
                }else{
                    table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
                }
               /* if (chrtemp.single_da_hu == chrtemp2.single_da_hu) {
                   
                    //table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
                } else {
                    
                }*/
                table.GRR._chi_hu_rights[_seat_index].set_valid(true);

                table.GRR._chi_hu_card[_seat_index][0] = this._gang_card_data.get_card(0);
                table.GRR._chi_hu_card[_seat_index][1] = this._gang_card_data.get_card(1);
                table.process_chi_hu_player_operate_hy(_seat_index, this._gang_card_data.get_cards(), 0, false);
                table.process_chi_hu_player_score(_seat_index, _seat_index,operate_card, true);
                //table.process_chi_hu_player_score(_seat_index, _seat_index,operate_card, true);
            } else {
                table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
                table.GRR._chi_hu_card[_seat_index][0] = target_card;
                table.process_chi_hu_player_operate_hy(_seat_index, new int[] { target_card }, 1, false);
                table.process_chi_hu_player_score(_seat_index, _seat_index,operate_card, true);
            }

            // 记录
            table._player_result.da_hu_zi_mo[_seat_index]++;
            table._player_result.zi_mo_count[_seat_index]++;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                table.operate_player_action(i, true);
            }
            table._cur_banker = _seat_index;
            table._shang_zhuang_player = _seat_index;
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            // 放炮
            table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_FANG_PAO);

            int jie_pao_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                jie_pao_count++;
            }
            if (jie_pao_count > 1) {
                table._cur_banker = _seat_index;
                table.set_niao_card(table.GRR._banker_player, GameConstants.INVALID_VALUE, true, 0, true,
                        GameConstants.HU_CARD_TYPE_PAOHU);
            } else {
                table._cur_banker = target_player;
                table.set_niao_card(table.GRR._banker_player, GameConstants.INVALID_VALUE, true, 0, false,
                        GameConstants.HU_CARD_TYPE_PAOHU);
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }


                table.GRR._chi_hu_rights[i].set_valid(true);
                // 有两个胡
                if (table.GRR._chi_hu_rights[i].is_mul(GameConstants.CHR_SHU_FAN)) {

                    ChiHuRight chrtemp = new ChiHuRight();
                    int action  = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
                            table.GRR._weave_count[i], this._gang_card_data.get_card(0), chrtemp,
                            GameConstants.HU_CARD_TYPE_PAOHU, i);// 自摸
                    ChiHuRight chrtemp2 = new ChiHuRight();
                    int action1 = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
                            table.GRR._weave_count[i], this._gang_card_data.get_card(1), chrtemp2,
                            GameConstants.HU_CARD_TYPE_PAOHU, i);// 自摸
                    
                    table.GRR._chi_hu_rights[i].set_empty();
                    table.GRR._chi_hu_rights[i].copy(chrtemp);
        			for (int j = 0; j < chrtemp2.type_count; j++) {
        				table.GRR._chi_hu_rights[i].opr_or(chrtemp2.type_list[j]);
        			}

                    if(action > 0 && action1 > 0){
                         table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO);
                    }else{
                        table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
                    }

                    table.GRR._chi_hu_rights[i].set_valid(true);
                    table.GRR._chi_hu_card[i][0] = this._gang_card_data.get_card(0);
                    table.GRR._chi_hu_card[i][1] = this._gang_card_data.get_card(1);
                    table.process_chi_hu_player_operate_hy(i, this._gang_card_data.get_cards(), 0, false);
                    table.process_chi_hu_player_score(i, _seat_index, operate_card, false);
                } else {
                    table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
                    table.GRR._chi_hu_card[i][0] = table._playerStatus[i]._operate_card;
                    table.process_chi_hu_player_operate_hy(i, new int[] { table._playerStatus[i]._operate_card }, 1,
                            false);
                    table.process_chi_hu_player_score(i, _seat_index, operate_card, false);
                }

                // 记录
                table._player_result.da_hu_jie_pao[i]++;
                table._player_result.da_hu_dian_pao[_seat_index]++;
            }
            table._shang_zhuang_player = target_player;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                table.operate_player_action(i, true);
            }
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
            return true;
        }
        default:
            return false;
        }
        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HY258 table, int seat_index) {
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

        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);// 是否托管
            // 剩余牌数
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                int_array.addItem(table.GRR._discard_cards[i][j]);
            }
            tableResponse.addDiscardCards(int_array);

            // 组合扑克
            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
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
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                new long[] { GameConstants.WIK_GANG }, 1, seat_index);

        // 出牌
        table.operate_out_card(_seat_index, 2, this._gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_LEFT,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }

    private void exe_chi_peng(MJTable_HY258 table, int target_player, int target_action, int target_card) {

        // 组合扑克
        // int wIndex = table.GRR._weave_count[target_player]++;
        // table.GRR._weave_items[target_player][wIndex].public_card = 1;
        // table.GRR._weave_items[target_player][wIndex].center_card = target_card;
        // table.GRR._weave_items[target_player][wIndex].weave_kind = target_action;
        // table.GRR._weave_items[target_player][wIndex].provide_player = _seat_index;

        // 删掉出来的那两张张牌
        table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

        int add_card = 0;
        for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
            add_card = this._gang_card_data.get_card(i);
            if (target_card != add_card) {
                break;
            }
        }

        table.exe_add_discard(this._seat_index, 1, new int[] { add_card }, true, 0);

        // // 设置用户
        // table._current_player = target_player;
        //
        // //效果
        // table.operate_effect_action(target_player,MJGameConstants.EFFECT_ACTION_TYPE_ACTION,
        // 1, new long[]{target_action}, 1,MJGameConstants.INVALID_SEAT);
        //
        // //刷新手牌包括组合
        // int cards[]= new int[MJGameConstants.MAX_COUNT];
        // int hand_card_count =
        // table._logic.switch_to_cards_data(table.GRR._cards_index[target_player],
        // cards);
        // table.operate_player_cards(target_player, hand_card_count, cards,
        // table.GRR._weave_count[target_player],
        // table.GRR._weave_items[target_player]);
        //
        // for (int i = 0; i < MJtable.getTablePlayerNumber(); i++) {
        // table._playerStatus[i].clean_action();
        // table._playerStatus[i].clean_status();
        //
        // table.operate_player_action(i, true);
        // }

        table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_GANG);

    }

}
