package com.cai.game.hh.handler.czphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_ChenZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class HandlerDispatchFirstCard_ChenZhou extends HHHandlerDispatchCard<Table_ChenZhou> {

    @SuppressWarnings("unused")
    @Override
    public void exe(Table_ChenZhou table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        table._current_player = _seat_index;
        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        table.GRR._left_card_count--;

        if (table.DEBUG_CARDS_MODE) {
            _send_card_data = 0x17;
        }

        table._send_card_data = _send_card_data;
        table._provide_player = _seat_index;

        boolean is_hu = false;
        int send_index = table._logic.switch_to_card_index(table._send_card_data);
        boolean is_fa_pai = false;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (table._current_player + p) % table.getTablePlayerNumber();
            int ti_count = 0;
            int sao_count = 0;
            int hong_pai_count = 0;
            int hei_pai_count = 0;
            int all_cards_count = 0;

            for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
                if ((i == table._current_player) && (j == send_index))
                    table.GRR._cards_index[i][j]++;

                if (table._logic.color_hei(table._logic.switch_to_card_data(j)) == true) {
                    hei_pai_count += table.GRR._cards_index[i][j];
                } else {
                    hong_pai_count += table.GRR._cards_index[i][j];
                }

                if (table.GRR._cards_index[i][j] == 4) {
                    ti_count++;
                    if ((i == table._current_player) && (j == send_index))
                        is_fa_pai = true;
                }
                if (table.GRR._cards_index[i][j] == 3) {
                    sao_count++;
                    if ((i == table._current_player) && (j == send_index))
                        is_fa_pai = true;
                }

                if ((i == table._current_player) && (j == send_index))
                    table.GRR._cards_index[i][j]--;
            }

            if ((ti_count >= 3) || (sao_count >= 5)) {
                ChiHuRight chr = table.GRR._chi_hu_rights[i];
                chr.set_empty();

                int all_hu_xi = 0;

                for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
                    if ((i == table._current_player) && (j == send_index))
                        table.GRR._cards_index[i][j]++;
                    if (table.GRR._cards_index[i][j] == 4) {
                        if (j < 10)
                            all_hu_xi += 12;
                        else
                            all_hu_xi += 9;
                    }
                    if (table.GRR._cards_index[i][j] == 3) {
                        if (j < 10)
                            all_hu_xi += 6;
                        else
                            all_hu_xi += 3;
                    }
                    if ((i == table._current_player) && (j == send_index))
                        table.GRR._cards_index[i][j]--;
                }

                boolean b_hu_xi = false;

                if (all_hu_xi >= table.get_basic_hu_xi()) {
                    b_hu_xi = true;
                }

                if (b_hu_xi == true) {
                    int weave_count = 0;

                    for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
                        if ((i == table._current_player) && (j == send_index))
                            table.GRR._cards_index[i][j]++;

                        if (table.GRR._cards_index[i][j] == 4 && (ti_count >= 3)) {
                            table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
                            table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_AN_LONG;
                            table._hu_weave_items[i][weave_count].hu_xi = table._logic
                                    .get_weave_hu_xi(table._hu_weave_items[i][weave_count]);

                            weave_count++;
                        }

                        if (table.GRR._cards_index[i][j] == 3 && (sao_count >= 5)) {
                            table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
                            table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_KAN;
                            table._hu_weave_items[i][weave_count].hu_xi = table._logic
                                    .get_weave_hu_xi(table._hu_weave_items[i][weave_count]);

                            weave_count++;
                        }
                        if ((i == table._current_player) && (j == send_index))
                            table.GRR._cards_index[i][j]--;
                    }

                    int hu_card = table._hu_weave_items[i][weave_count - 1].center_card;
                    table._hu_weave_count[i] = weave_count;
                    all_cards_count = hong_pai_count + hei_pai_count;

                    if (table.has_rule(Constants_ChenZhou.GAME_RULE_HONG_HEI_DIAN)) {
                        if (hong_pai_count == 0) {
                            chr.opr_or(Constants_ChenZhou.CHR_ALL_HEI);
                        } else if (hong_pai_count == 1) {
                            chr.opr_or(Constants_ChenZhou.CHR_ONE_HONG);
                        } else if (hong_pai_count >= 10) {
                            chr.opr_or(Constants_ChenZhou.CHR_TEN_HONG_PAI);
                        }
                    }

                    chr.opr_or(Constants_ChenZhou.CHR_SPECAIL_TIAN_HU); // 特殊天胡

                    PlayerStatus curPlayerStatus = table._playerStatus[i];
                    curPlayerStatus.reset();

                    if ((is_fa_pai == true)) {
                        table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data },
                                GameConstants.INVALID_SEAT, false);
                    }

                    curPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
                    curPlayerStatus.add_chi_hu(hu_card, i);

                    table.handler_operate_card(i, GameConstants.WIK_CHI_HU, table._send_card_data, -1);

                    return;
                } else {
                    chr.set_empty();
                }
            }
        }

        if (is_hu == false) {
            table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data },
                    GameConstants.INVALID_SEAT, false);

            table._provide_card = table._send_card_data;

            table.exe_chuli_first_card(_seat_index, GameConstants.WIK_NULL, 2500);

            // _send_card_data = 0; // 这行代码会引起断线重连的bug
        }

        return;
    }

    @SuppressWarnings("unused")
    @Override
    public boolean handler_operate_card(Table_ChenZhou table, int seat_index, int operate_code, int operate_card,
            int luoCode) {
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

        // 变量定义 优先级最高操作的玩家和操作--不通炮的算法
        int target_player = seat_index;
        int target_action = operate_code;
        int target_lou_code = luoCode;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        int cbActionRank[] = new int[table.getTablePlayerNumber()];
        int cbMaxActionRand = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;
            // 优先级别
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                    cbActionRank[i] = cbUserActionRank;
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                    cbActionRank[i] = cbUserActionRank;
                } else {
                    // 获取最大的动作的优先级
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                // 优先级别
                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                    target_lou_code = table._playerStatus[i].get_lou_kind();
                    target_p = table.getTablePlayerNumber() - p;
                    cbMaxActionRand = cbUserActionRank;
                }
            }
        }

        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false) {
            table.log_info("优先级最高的人还没操作");
            return true;
        }

        // 变量定义
        int target_card = table._playerStatus[target_player]._operate_card;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
            table.operate_player_action(i, true);
        }

        switch (target_action) {
        case GameConstants.WIK_NULL: {
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
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

                table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

                PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
                curPlayerStatus.reset();
                curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
            }

            return true;
        }
        case GameConstants.WIK_CHI_HU: {
            table.GRR._chi_hu_rights[target_player].set_valid(true);

            table.GRR._chi_hu_card[target_player][0] = operate_card;

            table._cur_banker = target_player;

            table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data },
                    GameConstants.INVALID_SEAT, false);

            table._shang_zhuang_player = target_player;

            table.process_chi_hu_player_operate(target_player, operate_card, true);

            table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

            table.countChiHuTimes(target_player, true);

            int delay = GameConstants.GAME_FINISH_DELAY_FLS;
            if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
                delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
            }
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
                    delay, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_ChenZhou table, int seat_index) {
        super.handler_player_be_in_room(table, seat_index);
        table.istrustee[seat_index] = false;
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;
        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }
        return true;
    }

}
