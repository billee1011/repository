package com.cai.game.hh.handler.wugangphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_WuGang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_WuGang extends HHHandlerDispatchCard<Table_WuGang> {

    @SuppressWarnings("static-access")
    @Override
    public void exe(Table_WuGang table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        if (table.GRR._left_card_count == 0) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                int cards[] = new int[GameConstants.MAX_HH_COUNT];
                int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

                table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards,
                        table.GRR._weave_items[i], table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
            }

            table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();
            table._shang_zhuang_player = GameConstants.INVALID_SEAT;

            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

            return;
        }

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        if (table.has_rule(Constants_WuGang.GAME_RULE_PLAYER_3)) {
            table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
                    table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, _seat_index);

            int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
            int ting_count = table._playerStatus[_seat_index]._hu_card_count;

            if (ting_count > 0) {
                table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
            } else {
                ting_cards[0] = 0;
                table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
            }
        } else {
            table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card_sixteen(
                    table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, _seat_index);

            int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
            int ting_count = table._playerStatus[_seat_index]._hu_card_count;

            if (ting_count > 0) {
                table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
            } else {
                ting_cards[0] = 0;
                table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
            }
        }

        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        table.GRR._left_card_count--;

        if (table.DEBUG_CARDS_MODE) {
            _send_card_data = 0x1a;
        }

        table._send_card_data = _send_card_data;
        table._current_player = _seat_index;
        table._provide_player = _seat_index;
        table._last_card = _send_card_data;

        table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data },
                GameConstants.INVALID_SEAT, false);

        int ti_sao = table.estimate_player_ti_wei_respond_phz_chd(_seat_index, table._send_card_data);
        if (ti_sao != GameConstants.WIK_NULL) { // 有提偎，就直接执行提偎并返回
            return;
        }

        ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            chr[i] = table.GRR._chi_hu_rights[i];
            chr[i].set_empty();
        }

        int bHupai = 0;
        int action_hu[] = new int[table.getTablePlayerNumber()];
        int action_pao[] = new int[table.getTablePlayerNumber()];
        int pao_type[][] = new int[table.getTablePlayerNumber()][1];

        int card_type = GameConstants.HU_CARD_TYPE_FAN_PAI;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            int hu_xi_chi[] = new int[1];
            hu_xi_chi[0] = 0;

            PlayerStatus tempPlayerStatus = table._playerStatus[i];
            tempPlayerStatus.reset();

            if (table.has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
                action_hu[i] = table.analyse_chi_hu_card_sixteen(table.GRR._cards_index[i], table.GRR._weave_items[i],
                        table.GRR._weave_count[i], i, _seat_index, table._send_card_data, chr[i], card_type, hu_xi_chi,
                        true);
            } else {
                action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
                        table.GRR._weave_count[i], i, _seat_index, table._send_card_data, chr[i], card_type, hu_xi_chi,
                        true);
            }

            action_pao[i] = table.estimate_player_respond_phz_chd(i, _seat_index, table._send_card_data, pao_type[i],
                    true);

            if (table._is_xiang_gong[i] == true)
                action_hu[i] = GameConstants.WIK_NULL;

            if (action_hu[i] != GameConstants.WIK_NULL) {
                tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
                tempPlayerStatus.add_chi_hu(table._send_card_data, i);

                if (action_pao[i] != GameConstants.WIK_PAO) {
                    tempPlayerStatus.add_action(GameConstants.WIK_NULL);
                    tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
                } else { // 如果发牌时，有玩家同时能跑牌或胡牌
                    tempPlayerStatus.add_action(GameConstants.WIK_PAO);
                    tempPlayerStatus.add_pao(table._send_card_data, _seat_index);
                }

                ti_sao = GameConstants.WIK_ZI_MO;

                bHupai = 1;
            } else {
                chr[i].set_empty();
            }
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((action_pao[i] != GameConstants.WIK_NULL) && (bHupai == 0)) { // 如果没人胡牌，但是有人有跑，直接执行跑牌动作
                ti_sao = GameConstants.WIK_PAO;
                table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false,
                        1000);
                return;
            } else if (action_pao[i] != GameConstants.WIK_NULL) { // 如果有人胡并且有人跑，不能先直接执行跑牌
                ti_sao = GameConstants.WIK_PAO;
            }
        }

        boolean bAroseAction = false;

        boolean can_eat = true;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            for (int c = 0; c < table.GRR._weave_count[p]; c++) {
                if (table.GRR._weave_items[p][c].weave_kind == GameConstants.WIK_PENG
                        || table.GRR._weave_items[p][c].weave_kind == GameConstants.WIK_WEI) {
                    can_eat = false;
                    break;
                }
            }
        }

        if (can_eat || ti_sao != GameConstants.WIK_PAO) { // 如果没人跑牌，判断有没有吃碰
            if (table.GRR._left_card_count > 0) { // 不是最后一张牌才判断吃碰
                bAroseAction = table.estimate_player_out_card_respond_chen_zhou(_seat_index, table._send_card_data,
                        true);
            }
        }

        if ((bAroseAction == false) && (ti_sao == GameConstants.WIK_NULL)) { // 如果没人跑牌，并且没人碰牌
            table.operate_player_action(_seat_index, true);
        } else {
            // 等待别人操作这张牌
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (table._playerStatus[i].has_action()) {
                    table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }

        if (curPlayerStatus.has_action()) { // 如果当前玩家有动作？？？要是胡牌动作呢？？？为什么这个if else需要判断当前玩家是否有动作？？？
            if (table.isTrutess(_seat_index)) { // 有动作也直接出牌？？？
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
                return;
            }
            curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            if (table.isTrutess(_seat_index)) { // 如果当前玩家没动作，并且是托管状态。但是为什么是直接出牌呢，别人也可以有操作啊？
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
                return;
            }

            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else if (ti_sao == GameConstants.WIK_NULL) { // 如果没人跑牌
                if (bAroseAction == false) { // 并且没人吃碰牌
                    table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

                    int discard_time = 2000;
                    int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
                    SysParamModel sysParamModel1104 = SysParamDict.getInstance()
                            .getSysParamModelDictionaryByGameId(gameId).get(1104);
                    if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0
                            && sysParamModel1104.getVal4() < 10000) {
                        discard_time = sysParamModel1104.getVal4();
                    }

                    if (table._last_card != 0)
                        table.exe_add_discard(_seat_index, 1, new int[] { table._last_card }, true, discard_time);

                    int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
                    table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = table._send_card_data;
                    table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = table._send_card_data;

                    table._current_player = next_player;
                    _seat_index = next_player;

                    int dispatch_time = 3000;
                    if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0
                            && sysParamModel1104.getVal5() < 10000) {
                        dispatch_time = sysParamModel1104.getVal5();
                    }
                    table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);

                    table._last_card = table._send_card_data;
                    table._last_player = table._current_player;
                }
            }
        }

        return;
    }

    @Override
    public boolean handler_operate_card(Table_WuGang table, int seat_index, int operate_code, int operate_card,
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

        if (operate_code == GameConstants.WIK_NULL) {
            boolean flag = false;
            for (int i = 0; i < playerStatus._action_count; i++) {
                switch (playerStatus._action[i]) {
                case GameConstants.WIK_LEFT:
                case GameConstants.WIK_CENTER:
                case GameConstants.WIK_RIGHT:
                case GameConstants.WIK_XXD:
                case GameConstants.WIK_DDX:
                case GameConstants.WIK_EQS: {
                    if (flag == false) {
                        table._cannot_chi[seat_index][table._cannot_chi_count[seat_index]++] = operate_card;
                        playerStatus.set_exe_pass(true);
                        flag = true;
                    }
                }
                    break;
                case GameConstants.WIK_PENG: {
                    table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = operate_card;
                    playerStatus.set_exe_pass(true);
                }
                    break;
                }
            }
        }

        switch (operate_code) {
        case GameConstants.WIK_LEFT:
        case GameConstants.WIK_CENTER:
        case GameConstants.WIK_RIGHT:
        case GameConstants.WIK_XXD:
        case GameConstants.WIK_DDX:
        case GameConstants.WIK_EQS:
            if (luoCode != -1)
                playerStatus.set_lou_pai_kind(luoCode);
        }

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
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {

            int i = (_seat_index + p) % table.getTablePlayerNumber();
            int cbUserActionRank = 0;
            int cbTargetActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                    cbActionRank[i] = cbUserActionRank;
                } else {
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                    cbActionRank[i] = cbUserActionRank;
                } else {
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;
                    target_action = table._playerStatus[i].get_perform();
                    target_lou_code = table._playerStatus[i].get_lou_kind();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }

        if (table._playerStatus[target_player].is_respone() == false) {
            table.log_info("优先级最高的人还没操作");
            return true;
        }

        int target_card = table._playerStatus[target_player]._operate_card;

        int last_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

        boolean flag = false;
        int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
                | GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS;

        for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {
            switch (table._playerStatus[last_player]._action[j]) {
            case GameConstants.WIK_LEFT:
            case GameConstants.WIK_CENTER:
            case GameConstants.WIK_RIGHT:
            case GameConstants.WIK_XXD:
            case GameConstants.WIK_DDX:
            case GameConstants.WIK_EQS:
                if (target_action == GameConstants.WIK_NULL)
                    continue;
                if (flag == false)
                    if (table._playerStatus[last_player].get_exe_pass() == true) {
                        table._cannot_chi[last_player][table._cannot_chi_count[last_player]--] = 0;
                        flag = true;
                        table._playerStatus[last_player].set_exe_pass(false);
                    }
                break;
            }
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            boolean flag_temp = false;

            if (table._playerStatus[i].has_action()) {
                for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
                    switch (table._playerStatus[i]._action[j]) {
                    case GameConstants.WIK_LEFT:
                    case GameConstants.WIK_CENTER:
                    case GameConstants.WIK_RIGHT:
                    case GameConstants.WIK_XXD:
                    case GameConstants.WIK_DDX:
                    case GameConstants.WIK_EQS:
                        if (!((target_action == GameConstants.WIK_PENG) || (target_action == GameConstants.WIK_ZI_MO)))
                            continue;
                        if (flag_temp == false)
                            if (table._playerStatus[i].get_exe_pass() == true) {
                                table._cannot_chi[i][table._cannot_chi_count[i]--] = 0;
                                flag_temp = true;
                            }
                        break;
                    case GameConstants.WIK_PENG:
                        if (!((target_action == GameConstants.WIK_NULL)
                                || (target_action & eat_type) != GameConstants.WIK_NULL))
                            continue;
                        if (table._playerStatus[i].get_exe_pass() == false) {
                            table._cannot_peng[i][table._cannot_peng_count[i]++] = operate_card;
                        }
                        break;
                    }
                }
            }

            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }

        int cards_cur[] = new int[GameConstants.MAX_HH_COUNT];
        int hand_card_count_cur = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards_cur);
        table.operate_player_cards(_seat_index, hand_card_count_cur, cards_cur, table.GRR._weave_count[_seat_index],
                table.GRR._weave_items[_seat_index]);

        switch (target_action) {
        case GameConstants.WIK_NULL: {
            table._playerStatus[_seat_index].clean_action();
            table._playerStatus[_seat_index].clean_status();

            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    int cards[] = new int[GameConstants.MAX_HH_COUNT];
                    int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);
                    table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards,
                            table.GRR._weave_items[i], table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
                }

                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
            } else {
                for (int i = 0; i < table.getTablePlayerNumber(); i++) { // 点过之后，还有再次判断是否有跑？？
                    int pao_type[] = new int[1];
                    int action = table.estimate_player_respond_phz_chd(i, _seat_index, table._send_card_data, pao_type,
                            true);
                    if (action != GameConstants.WIK_NULL) {
                        table.exe_gang(i, _seat_index, table._send_card_data, action, pao_type[0], true, true, false,
                                1000);
                        return true;
                    }
                }

                table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

                int pai_count = 0;
                for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
                    if (table.GRR._cards_index[_seat_index][i] < 3)
                        pai_count += table.GRR._cards_index[_seat_index][i];
                }

                if (pai_count == 0) { // 胡牌时，能胡不糊，结果没牌能出了，设置成相公
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

                int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
                int ting_count = table._playerStatus[_seat_index]._hu_card_count;

                if (ting_count > 0) {
                    table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
                } else {
                    ting_cards[0] = 0;
                    table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
                }

                table.exe_add_discard(_seat_index, 1, new int[] { table._send_card_data }, true, 0);

                int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

                table._current_player = next_player;
                _seat_index = next_player;
                table._last_player = next_player;

                table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 0);
                table._last_card = table._send_card_data;
            }
            return true;
        }
        case GameConstants.WIK_LEFT: {
            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }

            table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index,
                    table._lou_weave_item[target_player][0]);

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_RIGHT: {
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }

            table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index,
                    table._lou_weave_item[target_player][2]);

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_CENTER: {
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index,
                    table._lou_weave_item[target_player][1]);

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_XXD: {
            int target_card_color = table._logic.get_card_color(target_card);

            int cbRemoveCard[] = new int[2];
            if (target_card_color == 0) {
                cbRemoveCard[0] = target_card;
                cbRemoveCard[1] = target_card + 16;
            } else {
                cbRemoveCard[0] = target_card - 16;
                cbRemoveCard[1] = target_card - 16;
            }
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index,
                    table._lou_weave_item[target_player][4]);

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_DDX: {
            int target_card_color = table._logic.get_card_color(target_card);

            int cbRemoveCard[] = new int[2];
            if (target_card_color == 0) {
                cbRemoveCard[0] = target_card + 16;
                cbRemoveCard[1] = target_card + 16;
            } else {
                cbRemoveCard[0] = target_card - 16;
                cbRemoveCard[1] = target_card;
            }
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index,
                    table._lou_weave_item[target_player][5]);

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_EQS: {
            int cbRemoveCard[] = new int[] { target_card, target_card };
            int target_card_value = table._logic.get_card_value(target_card);
            switch (target_card_value) {
            case 2:
                cbRemoveCard[0] = target_card + 5;
                cbRemoveCard[1] = target_card + 8;
                break;
            case 7:
                cbRemoveCard[0] = target_card - 5;
                cbRemoveCard[1] = target_card + 3;
                break;
            case 10:
                cbRemoveCard[0] = target_card - 8;
                cbRemoveCard[1] = target_card - 3;
                break;

            default:
                break;
            }
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index,
                    table._lou_weave_item[target_player][3]);

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_PENG: {
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "碰牌删除出错");
                return false;
            }

            table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
            return true;
        }
        case GameConstants.WIK_PAO: {
            int pao_type[] = new int[1];
            int action = table.estimate_player_respond_phz_chd(target_player, _seat_index, table._send_card_data,
                    pao_type, true);

            if (action != GameConstants.WIK_NULL) {
                table.exe_gang(target_player, _seat_index, table._send_card_data, action, pao_type[0], true, true,
                        false, 1000);
            }

            return true;
        }
        case GameConstants.WIK_CHI_HU: {
            table.GRR._chi_hu_rights[target_player].set_valid(true);

            table.GRR._chi_hu_card[target_player][0] = operate_card;

            table._cur_banker = target_player;

            table.process_chi_hu_player_operate(target_player, operate_card, true);
            table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

            table.countChiHuTimes(target_player, true);

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
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

        table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

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
