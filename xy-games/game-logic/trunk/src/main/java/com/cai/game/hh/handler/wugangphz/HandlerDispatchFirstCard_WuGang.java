package com.cai.game.hh.handler.wugangphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.WuGangDealFirstCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchFirstCard_WuGang extends HHHandlerDispatchCard<Table_WuGang> {

    @Override
    public void exe(Table_WuGang table) {
        GameSchedule.put(new WuGangDealFirstCardRunnable(table.getRoom_id(), _seat_index), 3, TimeUnit.SECONDS);
    }

    @Override
    public boolean handler_operate_card(Table_WuGang table, int seat_index, int operate_code, int operate_card,
            int luo_pai) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_info("没有这个操作:" + operate_code);
            return false;
        }
        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return true;
        }
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }
        if (operate_card != table._send_card_data) {
            table.log_player_error(seat_index, "操作牌，与当前牌不一样");
            return true;
        }

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        playerStatus.operate(operate_code, operate_card);
        playerStatus.clean_status();

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }

        switch (operate_code) {
        case GameConstants.WIK_NULL: {
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            int pai_count = 0;

            for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
                if (table.GRR._cards_index[_seat_index][i] < 3)
                    pai_count += table.GRR._cards_index[_seat_index][i];
            }

            if (pai_count == 0) {
                table._is_xiang_gong[_seat_index] = true;
                table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);

                int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

                table._playerStatus[_seat_index].clean_action();
                table._playerStatus[_seat_index].clean_status();
                table._current_player = next_player;
                table._last_player = next_player;

                table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
                return true;
            }

            PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
            curPlayerStatus.reset();

            curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();

            return true;
        }
        case GameConstants.WIK_CHI_HU: {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);

            table.GRR._chi_hu_card[seat_index][0] = operate_card;

            table._cur_banker = seat_index;

            table.process_chi_hu_player_operate(seat_index, operate_card, true);
            table.process_chi_hu_player_score_phz(seat_index, _seat_index, operate_card, true);

            table.countChiHuTimes(seat_index, true);

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_WuGang table, int seat_index) {
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
                int_array.addItem(table.GRR._discard_cards[i][j]);
            }
            tableResponse.addDiscardCards(int_array);

            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
                weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
                weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
                weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
                weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                weaveItem_array.addWeaveItem(weaveItem_item);
            }
            tableResponse.addWeaveItemArray(weaveItem_array);

            tableResponse.addWinnerOrder(0);
            tableResponse.addHuXi(table._hu_xi[i]);

            if (i == _seat_index) {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
            } else {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            }
        }

        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        for (int x = 0; x < hand_card_count; x++) {
            if (table.is_card_has_wei(hand_cards[x])) { // 如果是偎的牌
                // 判断打出这张牌是否能听牌
                table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]--;
                boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[seat_index],
                        table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
                table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]++;

                if (b_is_ting_state)
                    hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
                else
                    hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
            }
        }

        for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        // table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data },
        // seat_index, true);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        if (table._is_xiang_gong[seat_index] == true)
            table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

        table.istrustee[seat_index] = false;

        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;
        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        return true;
    }
}
