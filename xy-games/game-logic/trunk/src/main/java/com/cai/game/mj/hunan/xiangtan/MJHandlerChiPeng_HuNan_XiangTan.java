package com.cai.game.mj.hunan.xiangtan;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_HuNan_XiangTan extends MJHandlerChiPeng<MJTable_HuNan_XiangTan> {
    protected GangCardResult m_gangCardResult;

    public MJHandlerChiPeng_HuNan_XiangTan() {
        m_gangCardResult = new GangCardResult();
    }

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 组合扑克
        int wIndex = table.GRR._weave_count[_seat_index]++;
        table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
        table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
        table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
        table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

        // 删掉出来的那张牌
        table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

        // 设置用户
        table._current_player = _seat_index;

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        // 刷新手牌包括组合
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }

        WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
        int weave_count = table.GRR._weave_count[_seat_index];
        for (int i = 0; i < weave_count; i++) {
            weaves[i] = new WeaveItem();
            weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
            weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
            weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
            weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
            // 癞子
            if (table._logic.is_magic_card(weaves[i].center_card)) {
                weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }
        table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

        // 回放
        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._playerStatus[_seat_index].chi_hu_round_valid(); // 可以胡了

        m_gangCardResult.cbCardCount = 0;
        // 如果牌堆还有牌，判断能不能杠
        if (table.GRR._left_card_count > 0) {
            // 只检测手上的牌能不能杠
            int cbActionMask = table._logic.analyse_gang_by_hand_card_exclude_magic_card(
                    table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                    table.GRR._weave_count[_seat_index], m_gangCardResult);

            if (cbActionMask != 0) {
                boolean is_ting_state = table.is_ting_card(table.GRR._cards_index[_seat_index],
                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
                if (is_ting_state) { // 没杠之前玩家已经听牌
                    boolean flag = false;
                    for (int i = 0; i < m_gangCardResult.cbCardCount; i++) { // 有多个杠
                        int tmp_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]); // 杠的牌的索引

                        int resume_card_count = table.GRR._cards_index[_seat_index][tmp_index]; // 杠的牌的数量

                        table.GRR._cards_index[_seat_index][tmp_index] = 0; // 减掉杠的牌

                        boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

                        table.GRR._cards_index[_seat_index][tmp_index] = resume_card_count; // 把牌加回来

                        if (is_ting_state_after_gang) { // 杠后还能听牌
                            // 加上刚
                            curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                    m_gangCardResult.isPublic[i]);
                            flag = true;
                        }
                    }
                    if (flag) {
                        curPlayerStatus.add_action(GameConstants.WIK_GANG);
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

    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
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
    public boolean handler_player_be_in_room(MJTable_HuNan_XiangTan table, int seat_index) {
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

        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);// 是否托管
            // 剩余牌数
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    // 癞子
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
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
                if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
                    weaveItem_item.setCenterCard(
                            table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_GUI);
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                }

                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
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
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);

        /**
         * // 听牌显示 int ting_cards[] = table._playerStatus[seat_index]._hu_cards; int
         * ting_count = table._playerStatus[seat_index]._hu_card_count;
         * 
         * if (ting_count > 0) { table.operate_chi_hu_cards(seat_index, ting_count,
         * ting_cards); }
         **/

        table.send_response_to_player(seat_index, roomResponse);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
