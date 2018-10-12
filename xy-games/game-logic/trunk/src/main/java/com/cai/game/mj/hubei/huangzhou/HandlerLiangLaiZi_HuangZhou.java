package com.cai.game.mj.hubei.huangzhou;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerLiangLaiZi_HuangZhou extends AbstractMJHandler<Table_HuangZhou> {
    protected int _seat_index;
    protected int _send_card_data;
    protected int _type;

    protected GangCardResult m_gangCardResult;

    public HandlerLiangLaiZi_HuangZhou() {
        m_gangCardResult = new GangCardResult();
    }

    public void reset_status(int seat_index, int send_card_data, int type) {
        _seat_index = seat_index;
        _send_card_data = send_card_data;
        _type = type;
    }

    @Override
    public void exe(Table_HuangZhou table) {
        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        curPlayerStatus.add_action(GameConstants.WIK_LIANG_LAI_ZI);
        curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

        table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
        table.operate_player_action(_seat_index, false);
    }

    @Override
    public boolean handler_operate_card(Table_HuangZhou table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_error("没有这个操作");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("不是当前玩家操作");
            return false;
        }

        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }

        playerStatus.operate(operate_code, operate_card);
        playerStatus.clean_status();

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);

            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();
        }

        switch (operate_code) {
        case GameConstants.WIK_LIANG_LAI_ZI: {
            table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_LIANG_LAI_ZI }, 1, GameConstants.INVALID_SEAT);

            // TODO: 和商丘麻将类似，用PlayerResult的nao字段，存储是否亮了癞子牌，0表示没有，1表示有
            table._player_result.nao[_seat_index] = 1;

            table.operate_player_info();

            // TODO 玩家点了亮牌之后，需要重新获取一次听牌数据
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
        }
        }

        table.exe_dispatch_card(_seat_index, Constants_HuangZhou.LIANG_LAI_ZI, 0);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_HuangZhou table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

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
            tableResponse.addTrustee(false);
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                int real_card = table.GRR._discard_cards[i][j];
                if (table._logic.is_magic_card(real_card)) {
                    real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
                int_array.addItem(real_card);
            }
            tableResponse.addDiscardCards(int_array);

            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                weaveItem_item.setProvidePlayer(
                        table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
                weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
                weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
                weaveItem_array.addWeaveItem(weaveItem_item);
            }
            tableResponse.addWeaveItemArray(weaveItem_array);

            tableResponse.addWinnerOrder(0);

            // TODO 因为抓的那张癞子，并没有往手牌里加，所有不能删
            // if (i == _seat_index) {
            // tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])
            // - 1);
            // } else {
            // tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            // }

            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
        }

        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        // TODO 因为抓的那张癞子，并没有往手牌里加，所有不能删
        // if (seat_index == _seat_index) {
        // table._logic.remove_card_by_data(hand_cards, _send_card_data);
        // }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            if (table._logic.is_magic_card(hand_cards[i]))
                hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            tableResponse.addCardsData(hand_cards[i]);
        }

        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        int real_card = _send_card_data;
        if (table._logic.is_magic_card(real_card)) {
            table.total_lai_zi[_seat_index]++;
            real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        }

        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
