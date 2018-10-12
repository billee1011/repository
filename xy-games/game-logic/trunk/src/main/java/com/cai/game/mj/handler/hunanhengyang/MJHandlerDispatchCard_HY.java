package com.cai.game.mj.handler.hunanhengyang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 摸牌
 *
 * @author WalkerGeek date: 2017年8月1日 下午7:56:01 <br/>
 */
public class MJHandlerDispatchCard_HY extends MJHandlerDispatchCard<MJTable_HY> {

    @Override
    public void exe(MJTable_HY table) {
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }
        table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
        // 清理漏胡
        table._playerStatus[_seat_index].clear_cards_abandoned_hu();

        // 荒庄结束
        if (table.GRR._left_card_count == 0) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
            }

            // 流局
            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
            return;
        }

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        // 海底
        if (table.GRR._left_card_count == table.get_liu_niao()) {
            table.exe_hai_di(_seat_index, _seat_index);
            return;
        }

        table._current_player = _seat_index;// 轮到操作的人是自己

        // 从牌堆拿出一张牌
        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        --table.GRR._left_card_count;

        table._provide_player = _seat_index;
        if (table.DEBUG_CARDS_MODE || table.BACK_DEBUG_CARDS_MODE) {
            // table.GRR._left_card_count=10;
            _send_card_data = 0x08;
        }

        // 发牌处理,判断发给的这个人有没有胡牌或杠牌
        // 胡牌判断
        ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
        chr.set_empty();

        int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                table.GRR._weave_count[_seat_index], _send_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index,
                true);// 自摸

        boolean is_ting = false;

        if (action != GameConstants.WIK_NULL) {
            // 添加动作
            curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
            curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
            is_ting = true;
        } else {
            chr.set_empty();
        }

        // 加到手牌
        table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

        int real_card = _send_card_data;
        // 特殊牌
        if (table._logic.is_wang_ba_card(real_card)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        }

        // 发送数据 只有自己才有数值
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

        // 设置变量
        table._provide_card = _send_card_data;// 提供的牌

        m_gangCardResult.cbCardCount = 0;
        if (table.GRR._left_card_count > table.get_liu_niao()) {

            // 看手上所有的牌,有没有补张或杠
            int cbActionMask = table._logic.analyse_gang_card_hy(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
                    -1, table.GRR._cards_abandoned_gang[_seat_index]);

            if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
                // 添加动作 长沙麻将是补张
                if (table._playerStatus[_seat_index].lock_huan_zhang() == false) {
                    curPlayerStatus.add_action(GameConstants.WIK_GANG);
                }

                is_ting = false;

                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    // 加上补张
                    curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
                    boolean can_gang = false;
                    if (table.GRR._left_card_count > table.get_liu_niao()) {
                        if (is_ting == true) {
                            can_gang = true;

                        } else {
                            // 把可以杠的这张牌去掉。看是不是听牌
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

                            can_gang = table.is_hy_ting_card(table.GRR._cards_index[_seat_index],
                                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                    _seat_index);

                            table.GRR._weave_count[_seat_index] = cbWeaveIndex;

                            // 把牌加回来
                            table.GRR._cards_index[_seat_index][bu_index] = save_count;

                        }

                        if (can_gang) {
                            curPlayerStatus.add_action(GameConstants.WIK_YAO_YI_SE);// 听牌的时候可以杠
                            // 加上杠
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
    public boolean handler_operate_card(MJTable_HY table, int seat_index, int operate_code, int operate_card) {
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
            
            // 放弃明杠时加入过滤组合杠数组
            boolean flag = table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_GANG);
            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                if (flag && m_gangCardResult.type[i] == GameConstants.GANG_TYPE_ADD_GANG) {
                    table.GRR._cards_abandoned_gang[_seat_index][table._logic
                            .switch_to_card_index(m_gangCardResult.cbCardData[i])]++;
                }
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

            return true;
        }
        
        

        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_GANG: // 杠牌操作
        case GameConstants.WIK_YAO_YI_SE: // 杠牌操作
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

            // 设置鸟
            if (table.has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)) {
                table.set_niao_card_hy(_seat_index, GameConstants.INVALID_VALUE, true, 0, false, operate_card);// 结束后设置鸟牌
            } else {
                table.set_niao_card_hy(_seat_index, GameConstants.INVALID_VALUE, true, 0, false,
                        GameConstants.INVALID_VALUE);// 结束后设置鸟牌
            }
            // 下局胡牌的是庄家

            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            table._cur_banker = _seat_index;
            table.process_chi_hu_player_operate_hy(_seat_index, new int[] { operate_card }, 1, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, GameConstants.INVALID_CARD, true);

            // 记录
            table._player_result.zi_mo_count[_seat_index]++;

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HY table, int seat_index) {
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
                if (table._logic.is_wang_ba_card(table.GRR._discard_cards[i][j])) {
                    // 特殊牌
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
                } else {
                    int_array.addItem(table.GRR._discard_cards[i][j]);
                }
            }
            tableResponse.addDiscardCards(int_array);

            // 组合扑克
            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                if (table._logic.is_wang_ba_card(table.GRR._weave_items[i][j].center_card)) {
                    weaveItem_item.setCenterCard(
                            table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
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
            if (table._logic.is_wang_ba_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        // 次牌
        int real_card = _send_card_data;
        /*
         * if(table._logic.is_magic_card(_send_card_data)){
         * real_card+=GameConstants.CARD_ESPECIAL_TYPE_HUN; }
         */

        if (table._logic.is_wang_ba_card(_send_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        }
        // 摸牌
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }
        return true;
    }
}
