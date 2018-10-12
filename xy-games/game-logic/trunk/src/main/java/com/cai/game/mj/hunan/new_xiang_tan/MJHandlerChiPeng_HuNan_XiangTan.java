package com.cai.game.mj.hunan.new_xiang_tan;

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

public class MJHandlerChiPeng_HuNan_XiangTan extends MJHandlerChiPeng<MJTable_HuNan_XiangTan> {

    protected GangCardResult m_gangCardResult;

    public MJHandlerChiPeng_HuNan_XiangTan() {
        m_gangCardResult = new GangCardResult();
    }

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 清空所有玩家的动作
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 吃碰牌之后，当前玩家又可以接别人的炮了
        table._playerStatus[_seat_index].chi_hu_round_valid();
        table._playerStatus[_seat_index].clear_cards_abandoned_hu();

        // 将吃或者碰的牌，加入到落地牌组合
        int weave_index = table.GRR._weave_count[_seat_index]++;
        table.GRR._weave_items[_seat_index][weave_index].public_card = 1;
        table.GRR._weave_items[_seat_index][weave_index].center_card = _card;
        table.GRR._weave_items[_seat_index][weave_index].weave_kind = _action;
        table.GRR._weave_items[_seat_index][weave_index].provide_player = _provider;

        // 牌桌上当前的玩家，就是进行了吃或者碰的玩家
        table._current_player = _seat_index;

        // 玩家点了吃或者碰之后，客户端界面会弹出来响应的‘吃’或者‘碰’的动画
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        // 杠选美的牌，不用再移除一次
        if (_type != GameConstants.CHI_PENG_TYPE_GANG) {
            // 从牌桌上删除出牌人出的那张牌
            table.operate_remove_discard(_provider, table.GRR._discard_count[_provider]);
        }

        // 刷新进行吃或者碰的玩家手里的牌，把吃牌的牌显示在落地牌里，更新cards_index
        int[] hand_cards = new int[GameConstants.MAX_COUNT];
        // 将手上牌的索引转变成实际的牌值，返回值是吃碰之前的手牌数目
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], hand_cards);
        // 处理王牌
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(hand_cards[j])) {
                hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int count = 0;
        int ting_count = 0;

        int card_type_count = GameConstants.MAX_ZI;

        for (int i = 0; i < card_type_count; i++) {
            if (table._logic.is_magic_index(i)) {
                continue;
            }

            count = table.GRR._cards_index[_seat_index][i];

            if (count > 0) {
                table.GRR._cards_index[_seat_index][i]--;

                table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
                        table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

                if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
                    table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic
                            .switch_to_card_data(i);

                    ting_count++;
                }

                table.GRR._cards_index[_seat_index][i]++;
            }
        }

        table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

        if (ting_count > 0) {
            int tmp_cards[] = new int[GameConstants.MAX_COUNT];
            int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);

            for (int i = 0; i < tmp_hand_card_count; i++) {
                if (table._logic.is_magic_card(tmp_cards[i])) {
                    tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
                } else {
                    for (int j = 0; j < ting_count; j++) {
                        if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                            tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            break;
                        }
                    }
                }
            }

            table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards,
                    table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
        } else {
            // 刷新手牌
            table.operate_player_cards(_seat_index, hand_card_count, hand_cards, table.GRR._weave_count[_seat_index],
                    table.GRR._weave_items[_seat_index]);
        }
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        // 获取当前玩家的状态，并且重置一部分的玩家状态，像弃胡这些状态是不能再reset方法里重置的
        PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
        currentPlayerStatus.reset();

        // 每次吃碰牌之后，判断玩家手里有没有暗杠，湘潭麻将杠之前必须是听牌状态，也就是吃碰过后是听牌状态
        m_gangCardResult.cbCardCount = 0;

        // 吃碰之后不进行杠牌判断

        // 判断玩家有没有杠牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
        if (currentPlayerStatus.has_action()) {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();
        }
    }

    // 每一个Handler里面的这个方法，都会根据玩法规则不同和本身处理类型的不同而有所区别，比如发牌之后，玩家能对这张牌进行杠牌和胡牌操作
    // 比如玩家打出一张牌之后，其他玩家能对这张牌进行吃、碰、杠、胡操作。
    // 玩家在客户端点了弹出来得‘吃’、‘碰’、‘杠’、‘胡’、‘过’，之后就会进这里
    // 特别注意seat_index变量和_seat_index变量的差别，发牌时是一个意思，杠牌时和出牌时，可能就不是同一个值了
    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
            int operate_card) {
        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HuNan_XiangTan table, int seat_index) {
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
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

        if ((out_ting_count > 0) && (seat_index == _seat_index)) {
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
                } else {
                    for (int k = 0; k < out_ting_count; k++) {
                        if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
                            hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            break;
                        }
                    }
                }
            }
        } else {
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
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
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        // TODO 显示听牌数据
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
