package com.cai.game.mj.hunan.new_xiang_tan;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_HuNan_XiangTan extends MJHandlerGang<MJTable_HuNan_XiangTan> {

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 清空所有玩家的动作
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 吃碰杠之后，玩家又可以胡牌了
        table._playerStatus[_seat_index].chi_hu_round_valid();
        table._playerStatus[_seat_index].clear_cards_abandoned_hu();

        // 客户端弹出杠牌的动画效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        // 湘潭麻将杠牌之后是选美，暂时先直接执行杠，后面再加入选美的exe和handler
        this.exe_gang(table);
    }

    @Override
    protected boolean exe_gang(MJTable_HuNan_XiangTan table) {
        // 获取杠的那张牌的索引
        int gang_card_index = table._logic.switch_to_card_index(_center_card);

        int weave_index = -1;

        if (GameConstants.GANG_TYPE_AN_GANG == _type) { // 暗杠
            weave_index = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) { // 明杠
            weave_index = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;

            // 明杠需要删除废弃牌堆里别人打出来的那张牌，客户端操作
            table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) { // 碰杠
            // 对已经落地的牌进行轮询
            for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
                int weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
                int center_card = table.GRR._weave_items[_seat_index][i].center_card;
                if (center_card == this._center_card && weave_kind == GameConstants.WIK_PENG) {
                    weave_index = i;
                    break;
                }
            }
        }

        if (-1 == weave_index) { // 杠牌出错
            table.log_player_error(_seat_index, "杠牌出错");
            return false;
        }

        // 处理杠牌的落地牌显示和存储
        table.GRR._weave_items[_seat_index][weave_index].public_card = _p ? 1 : 0;
        table.GRR._weave_items[_seat_index][weave_index].center_card = _center_card;
        table.GRR._weave_items[_seat_index][weave_index].weave_kind = _action;
        table.GRR._weave_items[_seat_index][weave_index].provide_player = _provide_player;

        // 设置当前用户，下一轮handler或者其他操作的时候用得到
        table._current_player = _seat_index;

        // 删掉手里杠了那张牌，不管是明杠还是暗杠
        table.GRR._cards_index[_seat_index][gang_card_index] = 0;

        table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

        // 客户端进行手牌和落地牌刷新
        int[] cards = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }
        table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
                table.GRR._weave_items[_seat_index]);

        // TODO 显示听牌数据
        table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(
                table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
        int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
        int ting_count = table._playerStatus[_seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
        } else {
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
        }

        // 计算杠分
        // 选美玩法，杠不算分
        /**
         * int gangCount = table.GRR._gang_score[_seat_index].gang_count++; if
         * (GameConstants.GANG_TYPE_AN_GANG == _type) { // 暗杠，每人2分 for (int i = 0; i <
         * table.getTablePlayerNumber(); i++) { if (i == _seat_index) continue;
         * table.GRR._gang_score[_seat_index].scores[gangCount][i] -= 2 *
         * GameConstants.CELL_SCORE;
         * table.GRR._gang_score[_seat_index].scores[gangCount][_seat_index] += 2 *
         * GameConstants.CELL_SCORE; }
         * 
         * table._player_result.an_gang_count[_seat_index]++; } else if
         * (GameConstants.GANG_TYPE_JIE_GANG == _type) { // 明杠，放杠者单独给3分
         * table.GRR._gang_score[_seat_index].scores[gangCount][_seat_index] += 3 *
         * GameConstants.CELL_SCORE;
         * table.GRR._gang_score[_seat_index].scores[gangCount][_provide_player] -= 3 *
         * GameConstants.CELL_SCORE;
         * 
         * table._player_result.ming_gang_count[_seat_index]++; } else if
         * (GameConstants.GANG_TYPE_ADD_GANG == _type) { // 碰杠，每人1分 for (int i = 0; i <
         * table.getTablePlayerNumber(); i++) { if (i == _seat_index) continue;
         * table.GRR._gang_score[_seat_index].scores[gangCount][i] -=
         * GameConstants.CELL_SCORE;
         * table.GRR._gang_score[_seat_index].scores[gangCount][_seat_index] +=
         * GameConstants.CELL_SCORE; }
         * 
         * table._player_result.ming_gang_count[_seat_index]++; }
         **/

        // 湖南湘潭麻将杠牌之后要选美，暂时直接发牌，后面再添加选美的Handler
        // table.exe_dispatch_card(_seat_index, _action, 0);

        table.exe_gang_xuan_mei(_seat_index, table.get_xuan_mei_count());

        return true;
    }

    // 因为湘潭麻将没人会胡开杠的那张牌，直接返回true
    // 每一个Handler里面的这个方法，都会根据玩法规则不同和本身处理类型的不同而有所区别，比如发牌之后，玩家能对这张牌进行杠牌和胡牌操作
    // 比如玩家打出一张牌之后，其他玩家能对这张牌进行吃、碰、杠、胡操作。
    // 玩家在客户端点了弹出来得‘吃’、‘碰’、‘杠’、‘胡’、‘过’，之后就会进这里
    // 特别注意seat_index变量和_seat_index变量的差别，发牌时是一个意思，杠牌时和出牌时，可能就不是同一个值了
    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
            int operate_card) {
        return true;
    }

    // 和吃碰handler的断线重连一样
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
