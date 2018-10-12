package com.cai.game.mj.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.NewAbstractMjTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 直接庄家起手14牌
 * 
 * @author BlackSwan
 *
 * @param <T>
 */
public class NewHandlerQiShou<T extends NewAbstractMjTable> extends AbstractMJHandler<T> {
    protected int _seat_index;
    protected int _type;
    protected GangCardResult m_gangCardResult;

    public NewHandlerQiShou() {
        m_gangCardResult = new GangCardResult();
    }

    public void reset_status(int seat_index, int type) {
        _seat_index = seat_index;
        _type = type;
    }

    @Override
    public void exe(T table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
        currentPlayerStatus.reset();

        table._current_player = _seat_index;
        table._provide_player = _seat_index;

        table._last_dispatch_player = _seat_index;

        ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
        chr.set_empty();

        // 胡牌类型默认为GameConstants里的自摸，自己维护HU_CARD的，需要相应的变动一下
        int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
        // 走新的起手胡牌分析
        int action = table.analyse_qi_shou_hu_pai(table.GRR._cards_index[_seat_index],
                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], chr, card_type, _seat_index);

        if (GameConstants.WIK_NULL != action) {
            currentPlayerStatus.add_action(GameConstants.WIK_ZI_MO);

            // TODO 最后一张牌算自摸的牌
            int hu_card = 0;
            int card_type_count = GameConstants.MAX_ZI;
            for (int i = card_type_count - 1; i >= 0; i--) {
                int count = table.GRR._cards_index[_seat_index][i];

                if (count > 0) {
                    hu_card = table._logic.switch_to_card_data(i);
                }
            }
            currentPlayerStatus.add_zi_mo(hu_card, _seat_index);
        } else {
            chr.set_empty();
        }

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int count = 0;
        int ting_count = 0;

        int card_type_count = GameConstants.MAX_ZI;

        for (int i = 0; i < card_type_count; i++) {
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
            int cards[] = new int[GameConstants.MAX_COUNT];
            int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

            for (int i = 0; i < hand_card_count; i++) {
                for (int j = 0; j < ting_count; j++) {
                    if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                        cards[i] = cards[i] + GameConstants.CARD_ESPECIAL_TYPE_TING;
                        break;
                    }
                }
            }

            table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
        }
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        // 起手判断暗杠
        if (table.GRR._left_card_count > 0) {
            m_gangCardResult.cbCardCount = 0;

            int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true);

            if (0 != cbActionMask) {
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    currentPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                            m_gangCardResult.isPublic[i]);
                }
                currentPlayerStatus.add_action(GameConstants.WIK_GANG);
            }
        }

        if (currentPlayerStatus.has_action()) {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();
        }
    }

    @Override
    public boolean handler_operate_card(T table, int seat_index, int operate_code, int operate_card) {
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

            playerStatus.clean_action();
            playerStatus.clean_status();

            table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();

            return true;
        }

        switch (operate_code) {
        case GameConstants.WIK_GANG: {
            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                if (operate_card == m_gangCardResult.cbCardData[i]) {
                    table.exe_gang(seat_index, seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
                            false);
                    return true;
                }
            }
        }
            break;
        case GameConstants.WIK_ZI_MO: {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);

            table._cur_banker = seat_index;

            table.GRR._chi_hu_card[seat_index][0] = operate_card;
            table._player_result.zi_mo_count[seat_index]++;

            table.process_chi_hu_player_operate(seat_index, operate_card, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_out_card(T table, int seat_index, int card) {
        card = table.get_real_card(card);

        if (table._logic.is_valid_card(card) == false) {
            table.log_error("出牌,牌型出错");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("出牌,没到出牌");
            return false;
        }

        if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
            table.log_error("出牌删除出错");
            return false;
        }

        table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(T table, int seat_index) {
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
                int_array.addItem(table.GRR._discard_cards[i][j]);
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

        int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

        if ((out_ting_count > 0) && (seat_index == _seat_index)) {
            for (int j = 0; j < hand_card_count; j++) {
                for (int k = 0; k < out_ting_count; k++) {
                    if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
                        hand_cards[j] = hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_TING;
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

            int tmp_card = table._playerStatus[seat_index]._hu_out_card_ting[i];
            roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_TING);

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
