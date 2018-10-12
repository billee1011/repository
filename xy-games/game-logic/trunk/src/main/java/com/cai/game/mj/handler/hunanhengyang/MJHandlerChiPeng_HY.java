package com.cai.game.mj.handler.hunanhengyang;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_HY extends MJHandlerChiPeng<MJTable_HY> {

    private GangCardResult m_gangCardResult;

    public MJHandlerChiPeng_HY() {
        m_gangCardResult = new GangCardResult();
    }

    @Override
    public void exe(MJTable_HY table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 组合扑克
        int wIndex = table.GRR._weave_count[_seat_index]++;
        table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
        table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
        table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
        table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

        if (_action == GameConstants.WIK_PENG
                && table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)] == 1) { // 碰牌之后，已经删掉碰的牌了
                                                                                                         // -_-
            table.GRR._cards_abandoned_gang[_seat_index][table._logic.switch_to_card_index(_card)]++;
        }

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
        // 特殊牌
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_wang_ba_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }
        table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
                table.GRR._weave_items[_seat_index]);

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

        m_gangCardResult.cbCardCount = 0;
        // 如果牌堆还有牌，判断能不能杠
        if (table.GRR._left_card_count > 1) {

            int cbActionMask = GameConstants.WIK_NULL;

            cbActionMask = table._logic.analyse_gang_card_hy(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
                    -1, table.GRR._cards_abandoned_gang[_seat_index]);

            if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
                // 添加动作 补张
                curPlayerStatus.add_action(GameConstants.WIK_GANG);

                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    // 加上补张
                    curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);

                    if (table.GRR._left_card_count > 2) {

                        boolean can_gang = false;
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
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

                        table.GRR._weave_count[_seat_index] = cbWeaveIndex;
                        // 把牌加回来
                        table.GRR._cards_index[_seat_index][bu_index] = save_count;

                        if (can_gang == true) {
                            // 加上杠
                            curPlayerStatus.add_action(GameConstants.WIK_YAO_YI_SE);// 听牌的时候可以杠
                            curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                    m_gangCardResult.isPublic[i]);
                        }
                    }

                }
            }

        }

        if (curPlayerStatus.has_action()) {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);

        } else {
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

        // 放弃操作
        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();

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
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HY table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();
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
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
                } else {
                    int_array.addItem(table.GRR._discard_cards[i][j]);
                }

            }
            tableResponse.addDiscardCards(int_array);
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

        tableResponse.setSendCardData(0);
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }

}
