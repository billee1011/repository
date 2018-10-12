package com.cai.game.hh.handler.leiyangphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_LeiYang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class HandlerLiangPai_LeiYang extends HHHandlerDispatchCard<Table_LeiYang> {

    @Override
    public void exe(Table_LeiYang table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

        int cards[] = new int[GameConstants.MAX_HH_COUNT];
        int hand_card_count = 0;

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._current_player = _seat_index;

        int an_long_Index[] = new int[5];
        int an_long_count = 0;
        boolean ti_send_card = false;

        for (int k = 0; k < table.getTablePlayerNumber(); k++) {
            an_long_count = 0;

            for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
                if (table.GRR._cards_index[k][i] == 4) {
                    an_long_Index[an_long_count++] = i;
                    if (i == table._logic.switch_to_card_index(table._send_card_data))
                        ti_send_card = true;

                    if (0 == table._logic.get_card_color(table._logic.switch_to_card_data(i))) { // 小字提龙
                        table.player_ti_count[k][0]++;
                    } else { // 大字提龙
                        table.player_ti_count[k][1]++;
                    }

                    table.process_mid_score();
                    table.operate_game_mid_score();
                }
            }

            if (an_long_count >= 2) {
                table._ti_mul_long[k] = an_long_count - 1;
                table.out_status[k] = 1;
            }

            for (int i = 0; i < an_long_count; i++) {
                int cbWeaveIndex = table.GRR._weave_count[k];
                table.GRR._weave_items[k][cbWeaveIndex].public_card = 1;
                table.GRR._weave_items[k][cbWeaveIndex].center_card = table._logic
                        .switch_to_card_data(an_long_Index[i]);
                table.GRR._weave_items[k][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
                table.GRR._weave_items[k][cbWeaveIndex].provide_player = _seat_index;
                table.GRR._weave_items[k][cbWeaveIndex].hu_xi = table._logic
                        .get_weave_hu_xi(table.GRR._weave_items[k][cbWeaveIndex]);
                table.GRR._weave_count[k]++;
                table._long_count[k]++;

                table.GRR._cards_index[k][an_long_Index[i]] = 0;

                table.GRR._card_count[k] = table._logic.get_card_count_by_index(table.GRR._cards_index[k]);
            }

            if (an_long_count > 0) {
                int _action = GameConstants.WIK_AN_LONG;
                table.operate_effect_action(k, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                        GameConstants.INVALID_SEAT);

                hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[k], cards);
                table.operate_player_cards(k, hand_card_count, cards, table.GRR._weave_count[k],
                        table.GRR._weave_items[k]);
            }
        }

        ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            chr[i] = table.GRR._chi_hu_rights[i];
            chr[i].set_empty();
        }

        int card_type = Constants_LeiYang.HU_CARD_FAN_PAI_ZI_MO;

        int bHupai = 0;
        int action_hu[] = new int[table.getTablePlayerNumber()];

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            // 亮的那张牌，只能庄家胡
            if (_seat_index != i)
                continue;

            int card_data = table._send_card_data;

            if (table._current_player == i)
                card_data = 0;

            if (_seat_index != i && ti_send_card == true)
                continue;

            PlayerStatus tempPlayerStatus = table._playerStatus[i];
            tempPlayerStatus.reset();

            int hu_xi[] = new int[1];

            action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
                    table.GRR._weave_count[i], i, _seat_index, card_data, chr[i], card_type, hu_xi, true);

            if (action_hu[i] != GameConstants.WIK_NULL) {
                tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                tempPlayerStatus.add_zi_mo(table._send_card_data, i);

                if (table.has_rule(Constants_LeiYang.GAME_RULE_QIANG_ZHI_HU)) {
                    GameSchedule.put(new Runnable() {
                        @Override
                        public void run() {
                            table.handler_operate_card(_seat_index, GameConstants.WIK_ZI_MO, table._send_card_data, -1);
                        }
                    }, 1500, TimeUnit.MILLISECONDS);
                    return;
                }

                tempPlayerStatus.add_action(GameConstants.WIK_NULL);
                tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
                table.operate_player_action(i, false);

                bHupai = 1;
            } else {
                chr[i].set_empty();
            }

        }

        if (bHupai == 0) {
            hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
            table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
                    table.GRR._weave_items[_seat_index]);
            table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

            // 如果庄家起手提2龙，判断是否听牌，如果听牌，免打张
//            if (table._ti_mul_long[_seat_index] > 0) {
//                table._ti_mul_long[_seat_index]--;
//                boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index],
//                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
//                table._ti_mul_long[_seat_index]++;
//                
//                if (b_is_ting_state == true) {
//                    table._ti_mul_long[_seat_index]--;
//
//                    int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//
//                    table._playerStatus[_seat_index].clean_action();
//                    table._playerStatus[_seat_index].clean_status();
//                    table._current_player = next_player;
//                    table._last_player = next_player;
//
//                    table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
//                } else {
//                    curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
//                    table.operate_player_status();
//                }
//            } else {
                curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
//            }
        }

        return;
    }

    @Override
    public boolean handler_operate_card(Table_LeiYang table, int seat_index, int operate_code, int operate_card,
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

        table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
                1);

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

                // _send_card_data = 0; // 这行代码会引起重连时出错
                table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

                int cards[] = new int[GameConstants.MAX_HH_COUNT];
                int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
                table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
                        table.GRR._weave_items[_seat_index]);

                PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
                curPlayerStatus.reset();

                curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
                table.operate_player_status();
            }
            return true;
        }
        case GameConstants.WIK_ZI_MO: {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);

            table.GRR._chi_hu_card[seat_index][0] = operate_card;

            table._cur_banker = seat_index;

            table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data },
                    GameConstants.INVALID_SEAT, false);

            table._shang_zhuang_player = seat_index;

            table.GRR._chi_hu_rights[seat_index].opr_or(Constants_LeiYang.CHR_TIAN_HU);

            if (table.has_rule(Constants_LeiYang.GAME_RULE_FAN_XING)) {
                table.set_niao_card(seat_index, GameConstants.INVALID_VALUE, true); // 结束后设置翻醒
            }
            table.hu_action = operate_code;
            table.process_chi_hu_player_operate(seat_index, operate_card, true);
            table.process_chi_hu_player_score_phz(seat_index, _seat_index, operate_card, true);

            table.countChiHuTimes(seat_index, true);

            int delay = GameConstants.GAME_FINISH_DELAY_FLS;
            if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
                delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
            }
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
                    delay, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_LeiYang table, int seat_index) {
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
