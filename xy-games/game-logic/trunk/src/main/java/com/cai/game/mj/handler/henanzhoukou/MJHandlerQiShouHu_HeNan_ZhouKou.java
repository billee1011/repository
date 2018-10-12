package com.cai.game.mj.handler.henanzhoukou;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerQiShouHu_HeNan_ZhouKou extends AbstractMJHandler<MJTable> {
    private int _seat_index;

    public void reset_status(int seat_index) {
        _seat_index = seat_index;
    }

    @Override
    public void exe(MJTable table) {
        table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
        table.operate_player_action(_seat_index, false);
    }

    @Override
    public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_error("没有这个操作");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("不是当前玩家操作");
            return false;
        }

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table._playerStatus[_seat_index].clean_action();
            table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);

            table.exe_dispatch_card(table.GRR._banker_player, GameConstants.WIK_NULL, 0);

            return true;
        }

        table.GRR._chi_hu_rights[_seat_index].set_valid(true);
        table.set_niao_card(_seat_index, GameConstants.INVALID_VALUE, true, 0);

        table.GRR._chi_hu_card[_seat_index][0] = operate_card;

        table._cur_banker = _seat_index;

        table.process_chi_hu_player_operate(_seat_index, operate_card, true);
        table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

        table._player_result.zi_mo_count[_seat_index]++;

        GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable table, int seat_index) {
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
                if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
                    if (i != seat_index) {
                        int_array.addItem(GameConstants.BLACK_CARD);
                    } else {
                        int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);
                    }
                } else {
                    int real_card = table.GRR._discard_cards[i][j];
                    if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
                        if (table._logic.is_magic_card(real_card)) {
                            real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                        }
                    }
                    int_array.addItem(real_card);
                }
            }
            tableResponse.addDiscardCards(int_array);

            // 组合扑克
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

            //
            tableResponse.addWinnerOrder(0);

            // 牌
            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
        }

        // 数据
        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
            for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
                if (table._logic.is_magic_card(hand_cards[i])) {
                    hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                }

                tableResponse.addCardsData(hand_cards[i]);
            }
        } else {
            for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
                tableResponse.addCardsData(hand_cards[i]);
            }
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        if (_seat_index == seat_index) {
            if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
                // 如果可以报听,刷新自己的手牌
                int ting_count = table._playerStatus[_seat_index]._hu_out_card_count;
                if (ting_count > 0) {
                    for (int i = 0; i < hand_card_count; i++) {
                        for (int j = 0; j < ting_count; j++) {
                            if (hand_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                                hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            }
                        }
                    }

                    table.operate_player_cards_with_ting(_seat_index, hand_card_count, hand_cards, 0, null);
                }
            }
        } else {
            int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
            int ting_count = table._playerStatus[seat_index]._hu_card_count;

            if (ting_count > 0) {
                if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
                    for (int x = 0; x < ting_count; x++) {
                        if (table._logic.is_magic_card(ting_cards[x])) {
                            ting_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                        }
                    }
                }
                table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
            }
        }

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
                for (int x = 0; x < ting_count; x++) {
                    if (table._logic.is_magic_card(ting_cards[x])) {
                        ting_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                    }
                }
            }
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
