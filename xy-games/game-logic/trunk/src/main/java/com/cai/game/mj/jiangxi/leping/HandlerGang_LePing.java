package com.cai.game.mj.jiangxi.leping;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGang_LePing extends MJHandlerGang<Table_LePing> {
    protected int _seat_index;
    protected int _provide_player;
    protected int _center_card;
    protected int _action;
    protected boolean _p;
    protected boolean _self;
    protected boolean _double;
    protected int _type;

    public HandlerGang_LePing() {
    }

    @Override
    public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self,
            boolean d) {
        _seat_index = seat_index;
        _provide_player = provide_player;
        _center_card = center_card;
        _action = action;
        _type = type;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            _p = false;
        } else {
            _p = true;
        }
        _self = self;
        _double = d;
    }

    @Override
    public void exe(Table_LePing table) {
        table._card_can_not_out_after_chi[_seat_index] = 0;
        table._playerStatus[_seat_index].clear_cards_abandoned_hu();
        table._playerStatus[_seat_index].clear_cards_abandoned_peng();

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (table._playerStatus[i].has_action()) {
                table.operate_player_action(i, true);
            }

            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
            this.exe_gang(table);
            return;
        }

        boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

        if (bAroseAction == false) {
            this.exe_gang(table);
        } else {
            PlayerStatus playerStatus = null;

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (playerStatus.has_chi_hu()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    public boolean exe_gang(Table_LePing table) {
        int cbCardIndex = table._logic.switch_to_card_index(_center_card);
        int cbWeaveIndex = -1;

        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;

            table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
            for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
                int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
                int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
                if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
                    cbWeaveIndex = i;
                    break;
                }
            }

            if (cbWeaveIndex == -1) {
                table.log_player_error(_seat_index, "杠牌出错");
                return false;
            }
        }

        table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;

        if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
            table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
        }

        table._current_player = _seat_index;

        table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
        table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

        WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
        int weave_count = table.GRR._weave_count[_seat_index];
        for (int i = 0; i < weave_count; i++) {
            weaves[i] = new WeaveItem();
            weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
            weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
            weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
            weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player
                    + GameConstants.WEAVE_SHOW_DIRECT;
        }
        table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

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

        int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == _seat_index)
                    continue;

                table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] -= GameConstants.CELL_SCORE;
                table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += GameConstants.CELL_SCORE;
            }

            table._player_result.an_gang_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += GameConstants.CELL_SCORE;
            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] -= GameConstants.CELL_SCORE;

            table._player_result.ming_gang_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
            int provide_index = table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player;

            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += GameConstants.CELL_SCORE;
            table.GRR._gang_score[_seat_index].scores[cbGangIndex][provide_index] -= GameConstants.CELL_SCORE;

            table._player_result.ming_gang_count[_seat_index]++;
        }

        table.exe_dispatch_card(_seat_index, _type, 0);

        return true;
    }

    @Override
    public boolean handler_operate_card(Table_LePing table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return false;
        }

        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_CHI_HU) {
            int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(seat_index, 1, ting_cards);

            table.GRR._chi_hu_rights[seat_index].set_valid(true);
            table.process_chi_hu_player_operate(seat_index, operate_card, false);
        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_discard_gang(seat_index);
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();
            }
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                return false;
        }

        int target_player = seat_index;
        @SuppressWarnings("unused")
        int target_action = operate_code;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            int cbUserActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
                } else {
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action);
                }

                int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;
                    target_action = table._playerStatus[i].get_perform();
                }
            }
        }
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }
        
        operate_card = _center_card;

        int jie_pao_count = 0;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                continue;
            }
            jie_pao_count++;
        }

        if (jie_pao_count > 0) {
            table.GRR._cards_index[_provide_player][table._logic.switch_to_card_index(operate_card)]--;

            if (jie_pao_count != 3) {
                if (jie_pao_count == 1) {
                    table._cur_banker = target_player;
                } else if (jie_pao_count == 2) {
                    int player_count = table.getTablePlayerNumber();
                    int[] quan_zhong = new int[player_count];
                    for (int i = 0; i < player_count; i++) {
                        quan_zhong[i] = player_count - (_seat_index + i) % player_count;
                    }
                    int hu_seat_1 = -1;
                    int hu_seat_2 = -1;
                    for (int i = 0; i < player_count; i++) {
                        if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false))
                            continue;
                        if (hu_seat_1 == -1) {
                            hu_seat_1 = i;
                        } else if (hu_seat_2 == -1) {
                            hu_seat_2 = i;
                        }
                    }
                    if (quan_zhong[hu_seat_1] > quan_zhong[hu_seat_2]) {
                        table._cur_banker = hu_seat_1;
                    } else {
                        table._cur_banker = hu_seat_2;
                    }
                }
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }

                table.GRR._chi_hu_card[i][0] = operate_card;

                table.process_chi_hu_player_score(i, _seat_index, operate_card, false);

                table._player_result.jie_pao_count[i]++;
                table._player_result.dian_pao_count[_seat_index]++;
            }

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
        } else {
            this.exe_gang(table);
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_LePing table, int seat_index) {
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
        table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
            table.process_chi_hu_player_operate_reconnect(seat_index, _center_card, false); // 效果
        } else {
            int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
            int ting_count = table._playerStatus[seat_index]._hu_card_count;

            if (ting_count > 0) {
                table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
            }

            table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
                    1, seat_index);

            if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
                table.operate_player_action(seat_index, false);
            }
        }

        return true;
    }
}
