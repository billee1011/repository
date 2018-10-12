package com.cai.game.mj.hunan.hengyang258;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 长沙麻将摸牌
 * 
 * @author Administrator
 *
 */
public class MJHandlerDispatchCard_HY258 extends MJHandlerDispatchCard<MJTable_HY258> {

    boolean ting_send_card = false;
    private boolean can_bao_ting;

    @Override
    public void exe(MJTable_HY258 table) {
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

            if (table.has_rule(GameConstants.GAME_TYPE_HZ) == false) {

            }
            // 流局
            table._shang_zhuang_player = GameConstants.INVALID_SEAT;
            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

            return;
        }

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        if (AbstractMJTable.DEBUG_CARDS_MODE) {
            //table.GRR._left_card_count = 50;
        }

        // 海底
        if (table.GRR._left_card_count == 1 && _type != GameConstants.WIK_BU_ZHNAG) {
            table.exe_hai_di(_seat_index, _seat_index);
            return;
        }

        table._current_player = _seat_index;// 轮到操作的人是自己

        // 从牌堆拿出一张牌
        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        --table.GRR._left_card_count;

        if (AbstractMJTable.DEBUG_CARDS_MODE) {
            _send_card_data = 0x07;
        }
        table._provide_player = _seat_index;

        // 发牌处理,判断发给的这个人有没有胡牌或杠牌
        // 胡牌判断
        ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
        chr.set_empty();

        int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                table.GRR._weave_count[_seat_index], _send_card_data, chr,  GameConstants.HU_CARD_TYPE_ZIMO,_seat_index);// 自摸

        boolean is_ting = false;
        // 加到手牌
        table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

        if (SysParamServerUtil.is_new_algorithm(3000, 3000, 2)) {
            // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
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
                            table._playerStatus[_seat_index]._hu_out_cards[ting_count],
                            table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                            table.GRR._weave_count[_seat_index], _seat_index);

                    if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
                        table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic
                                .switch_to_card_data(i);

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
                    for (int j = 0; j < ting_count; j++) {
                        if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                            cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            break;
                        }
                    }
                }

                table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
            }
            // TODO: 出任意一张牌时，能胡哪些牌 -- End
        }

        // 发送数据
        int real_card = _send_card_data;
        if (ting_send_card) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }

        // 发送数据 只有自己才有数值
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
        if (table._game_status == GameConstants.GS_MJ_XIAOHU) {
            table.exe_xiao_hu(_seat_index);
            return;
        }

        if (action != GameConstants.WIK_NULL) {
        	if(_type == GameConstants.WIK_BU_ZHNAG){
        		chr.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
        	}
            // 添加动作
            curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
            curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
            is_ting = true;

        } else {
            chr.set_empty();
        }

        // 设置变量
        table._provide_card = _send_card_data;// 提供的牌

        m_gangCardResult.cbCardCount = 0;
        if (table.GRR._left_card_count >= 1) {
            int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,
                    true);

            if (cbActionMask != GameConstants.WIK_NULL) {
                if (curPlayerStatus.lock_huan_zhang() == false) {
                    curPlayerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                }

                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _seat_index,
                            m_gangCardResult.isPublic[i]);
                    boolean can_gang = false;
                    if (table.GRR._left_card_count >= 2) {
                        int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
                        int save_count = table.GRR._cards_index[_seat_index][bu_index];
                        table.GRR._cards_index[_seat_index][bu_index] = 0;

                        int cbWeaveIndex = table.GRR._weave_count[_seat_index];

                        if (m_gangCardResult.type[i] == GameConstants.GANG_TYPE_AN_GANG) {
                            table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
                            table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[i];
                            table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                            table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
                            table.GRR._weave_count[_seat_index]++;
                        }

                        if (curPlayerStatus.lock_huan_zhang()) {
                            boolean has_huan_zhang = table.check_gang_huan_zhang(_seat_index,
                                    m_gangCardResult.cbCardData[i]);
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
                        	for(int k = 0; k < GameConstants.MAX_INDEX; k++){
                        		if(cbCardIndexTemp[k] > 0){
                        			int action_can = table.analyse_chi_hu_card(cbCardIndexTemp,table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                        					table._logic.switch_to_card_data(k), new ChiHuRight(), GameConstants.HU_CARD_TYPE_ZIMO, _seat_index);
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
                            curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                    m_gangCardResult.isPublic[i]);
                        }
                    }
                }
            }
        }

        if (curPlayerStatus.has_action()) {// 有动作
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
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
    public boolean handler_operate_card(MJTable_HY258 table, int seat_index, int operate_code, int operate_card) {
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
        	
        	if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();
            }
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
                table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
            }

            // 可以报听,选择了不报听
            if (can_bao_ting) {
                table.operate_player_cards_flag(_seat_index, 0, null, 0, null);
            }
            return true;
        }
        
        

        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_GANG: // 杠牌操作
        case GameConstants.WIK_BU_ZHNAG: // 杠牌操作
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

            // 下局胡牌的是庄家
            table.set_niao_card(table.GRR._banker_player, GameConstants.INVALID_VALUE, true, 0, false,
                    GameConstants.HU_CARD_TYPE_ZIMO);// 结束后设置鸟牌

            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            table._cur_banker = _seat_index;
            table._shang_zhuang_player = _seat_index;
            table.process_chi_hu_player_operate_hy(_seat_index, new int[] { operate_card }, 1, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index,operate_card, true);

            // 记录
            table._player_result.zi_mo_count[_seat_index]++;
            if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
                table._player_result.da_hu_zi_mo[_seat_index]++;
            } else {
                table._player_result.xiao_hu_zi_mo[_seat_index]++;
            }

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HY258 table, int seat_index) {
        // super.handler_player_be_in_room(table, seat_index);
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        roomResponse.setIsGoldRoom(table.is_sys());

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

            // 牌

            if (i == _seat_index) {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
            } else {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            }

        }

        // 数据
        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        // 如果断线重连的人是自己
        if (seat_index == _seat_index) {
            table._logic.remove_card_by_data(hand_cards, _send_card_data);
        }

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

        if ((out_ting_count > 0) && seat_index == _seat_index) {
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
            tableResponse.addCardsData(hand_cards[i]);
        }

        roomResponse.setTable(tableResponse);

        roomResponse.setOutCardCount(out_ting_count);

        for (int i = 0; i < out_ting_count; i++) {
            int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
            roomResponse.addOutCardTingCount(ting_card_cout);
            roomResponse.addOutCardTing(
                    table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < ting_card_cout; j++) {
                int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
            }
            roomResponse.addOutCardTingCards(int_array);
        }

        table.send_response_to_player(seat_index, roomResponse);

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }
        
        int real_card = _send_card_data;
        if (ting_send_card) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }

        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        table.be_in_room_trustee(seat_index);
        return true;
    }
}
