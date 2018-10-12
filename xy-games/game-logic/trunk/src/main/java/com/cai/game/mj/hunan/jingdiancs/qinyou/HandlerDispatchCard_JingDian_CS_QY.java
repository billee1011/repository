package com.cai.game.mj.hunan.jingdiancs.qinyou;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_JingDian_CS_QY;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_JingDian_CS_QY extends MJHandlerDispatchCard<Table_JingDian_CS_QY> {
    boolean ting_send_card = false;

    @Override
    public void exe(Table_JingDian_CS_QY table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();

        if (table.GRR._left_card_count == 0) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
            }

            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

            return;
        }

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        if (table.GRR._left_card_count == 1
                && !((GameConstants.GANG_TYPE_AN_GANG == _type || GameConstants.GANG_TYPE_JIE_GANG == _type
                        || GameConstants.GANG_TYPE_ADD_GANG == _type || GameConstants.WIK_BU_ZHNAG == _type))) {
            table.exe_hai_di(_seat_index, _seat_index);
            return;
        }

        table._send_card_count++;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        --table.GRR._left_card_count;
        table._send_card_data = _send_card_data;
        if (AbstractMJTable.DEBUG_CARDS_MODE) {
            // _send_card_data = 0x22;
        }

        table._current_player = _seat_index;
        table._provide_player = _seat_index;

        int tmp_cards[] = new int[GameConstants.MAX_COUNT];
        int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);
        table.operate_player_cards(_seat_index, tmp_hand_card_count, tmp_cards, table.GRR._weave_count[_seat_index],
                table.GRR._weave_items[_seat_index]);

        // 中途四喜和中途六六顺，在检查胡牌之前先检查
        boolean check_bu_gang = true;

        if (table.has_rule(Constants_JingDian_CS_QY.GAME_RULE_ZHONG_TU_SI_XI)) {
            boolean has_si_xi = table.check_zt_da_si_xi(table.GRR._cards_index[_seat_index],
                    table.GRR._start_hu_right[_seat_index], _seat_index, _send_card_data);
            if (has_si_xi) {
                curPlayerStatus.add_action(GameConstants.WIK_ZT_SI_XI);
                curPlayerStatus.add_zt_si_xi(_send_card_data, _seat_index);
                check_bu_gang = false;
            }
        }

        if (table.has_rule(Constants_JingDian_CS_QY.GAME_RULE_ZHONG_TU_LLS)) {
            boolean has_si_xi = table.check_zt_liu_liu_shun(table.GRR._cards_index[_seat_index],
                    table.GRR._start_hu_right[_seat_index], _send_card_data, _seat_index);
            if (has_si_xi) {
                curPlayerStatus.add_action(GameConstants.WIK_ZT_LLS);
                curPlayerStatus.add_zt_lls(_send_card_data, _seat_index);
                check_bu_gang = false;
            }
        }

        if (check_bu_gang) {
            ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
            chr.set_empty();

            int card_type = Constants_JingDian_CS_QY.HU_CARD_TYPE_ZI_MO;

            int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
                    card_type, _seat_index);

            if (action != GameConstants.WIK_NULL) {
                curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
            } else {
                chr.set_empty();
            }
        }

        table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int count = 0;
        int ting_count = 0;
        int send_card_index = table._logic.switch_to_card_index(_send_card_data);
        ting_send_card = false;

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

                    if (send_card_index == i) {
                        ting_send_card = true;
                    }
                }

                table.GRR._cards_index[_seat_index][i]++;
            }
        }

        table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

        if (ting_count > 0) {
            table.GRR._cards_index[_seat_index][send_card_index]--;

            int cards[] = new int[GameConstants.MAX_COUNT];
            int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

            table.GRR._cards_index[_seat_index][send_card_index]++;

            for (int i = 0; i < hand_card_count; i++) {
                for (int j = 0; j < ting_count; j++) {
                    if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                        cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                        break;
                    }
                }
            }

            table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
        }
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        int real_card = _send_card_data;
        if (ting_send_card) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }

        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

        table._provide_card = _send_card_data;

        if (check_bu_gang) {
            m_gangCardResult.cbCardCount = 0;
            if (table.GRR._left_card_count > 1) {
                int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,
                        true);

                if (cbActionMask != GameConstants.WIK_NULL) {
                    if (curPlayerStatus.lock_huan_zhang() == false) {
                        curPlayerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                    }

                    for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                        curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _seat_index,
                                m_gangCardResult.isPublic[i]);
                        boolean can_gang = false;
                        if (table.GRR._left_card_count > 2) {
                            int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
                            int save_count = table.GRR._cards_index[_seat_index][bu_index];
                            table.GRR._cards_index[_seat_index][bu_index] = 0;

                            int cbWeaveIndex = table.GRR._weave_count[_seat_index];

                            if (m_gangCardResult.type[i] == GameConstants.GANG_TYPE_AN_GANG) {
                                table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
                                table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[i];
                                table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                                table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
                                table.GRR._weave_count[_seat_index]++;
                            }

                            if (curPlayerStatus.lock_huan_zhang()) {
                                boolean has_huan_zhang = table.check_gang_huan_zhang(_seat_index,
                                        m_gangCardResult.cbCardData[i]);
                                can_gang = !has_huan_zhang;
                            } else {
                                can_gang = table.is_cs_ting_card(table.GRR._cards_index[_seat_index],
                                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                        _seat_index);
                            }

                            table.GRR._weave_count[_seat_index] = cbWeaveIndex;

                            table.GRR._cards_index[_seat_index][bu_index] = save_count;

                            if (can_gang) {
                                curPlayerStatus.add_action(GameConstants.WIK_GANG);
                                curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                        m_gangCardResult.isPublic[i]);
                            }
                        }
                    }
                }
            }
        }

        if (curPlayerStatus.has_action()) {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);

            if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
            } else {
                table.operate_player_status();
            }
        }

        return;
    }

    @Override
    public boolean handler_operate_card(Table_JingDian_CS_QY table, int seat_index, int operate_code, int operate_card) {
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

            if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_ZT_SI_XI)
                    || table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_ZT_LLS)) {
                table.operate_show_card(_seat_index, GameConstants.Show_Card_XiaoHU, 0, null,
                        GameConstants.INVALID_SEAT);

                PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
                curPlayerStatus.clean_action();

                ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
                chr.set_empty();

                int card_type = Constants_JingDian_CS_QY.HU_CARD_TYPE_ZI_MO;

                table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;

                int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
                        card_type, _seat_index);

                table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

                if (action != GameConstants.WIK_NULL) {
                    curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                    curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
                } else {
                    chr.set_empty();
                }

                m_gangCardResult.cbCardCount = 0;
                if (table.GRR._left_card_count > 0) {
                    int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,
                            true);

                    if (cbActionMask != GameConstants.WIK_NULL) {
                        if (table._playerStatus[_seat_index].lock_huan_zhang() == false) {
                            curPlayerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                        }

                        for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                            curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _seat_index,
                                    m_gangCardResult.isPublic[i]);
                            boolean can_gang = false;
                            if (table.GRR._left_card_count > 2) {
                                int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
                                int save_count = table.GRR._cards_index[_seat_index][bu_index];
                                table.GRR._cards_index[_seat_index][bu_index] = 0;

                                int cbWeaveIndex = table.GRR._weave_count[_seat_index];

                                if (m_gangCardResult.type[i] == GameConstants.GANG_TYPE_AN_GANG) {
                                    table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
                                    table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[i];
                                    table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                                    table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
                                    table.GRR._weave_count[_seat_index]++;
                                }

                                can_gang = table.is_cs_ting_card(table.GRR._cards_index[_seat_index],
                                        table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                        _seat_index);

                                table.GRR._weave_count[_seat_index] = cbWeaveIndex;

                                table.GRR._cards_index[_seat_index][bu_index] = save_count;

                                if (can_gang) {
                                    curPlayerStatus.add_action(GameConstants.WIK_GANG);
                                    curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                            m_gangCardResult.isPublic[i]);
                                }
                            }
                        }
                    }
                }

                if (curPlayerStatus.has_action()) {
                    curPlayerStatus._response = false;
                    table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(_seat_index, false);
                } else {
                    table._playerStatus[_seat_index].clean_action();
                    table._playerStatus[_seat_index].clean_status();

                    if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                        GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                                SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
                    } else {
                        table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                        table.operate_player_status();
                    }
                }

                return true;
            } else {
                table._playerStatus[_seat_index].clean_action();
                table._playerStatus[_seat_index].clean_status();

                if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                    GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                            SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
                } else {
                    table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
                    table.operate_player_status();
                }

                return true;
            }
        }
        
        

        switch (operate_code) {
        case GameConstants.WIK_GANG:
        case GameConstants.WIK_BU_ZHNAG: {
            if (operate_code == GameConstants.WIK_GANG) {
                table.gang_or_bu = table.GANG_STATE;
            } else {
                table.gang_or_bu = table.BU_STATE;
            }

            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                if (operate_card == m_gangCardResult.cbCardData[i]) {
                    table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
                            false);
                    return true;
                }
            }

        }
            break;
        case GameConstants.WIK_ZT_SI_XI: {
            ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

            start_hu_right.set_valid(true);

            show_zt_si_xi(table, seat_index);

            return true;
        }
        case GameConstants.WIK_ZT_LLS: {
            ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

            start_hu_right.set_valid(true);

            show_zt_liu_liu_shun(table, seat_index);

            return true;
        }
        case GameConstants.WIK_ZI_MO: {
            table.GRR._chi_hu_rights[_seat_index].set_valid(true);

            table.set_niao_card(_seat_index, true, false);

            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            table._cur_banker = _seat_index;
            table.process_chi_hu_player_operate(_seat_index, new int[] { operate_card }, 1, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);
            
            table._player_result.zi_mo_count[_seat_index]++;

            if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
                table._player_result.da_hu_zi_mo[_seat_index]++;
            } else {
                table._player_result.xiao_hu_zi_mo[_seat_index]++;
            }

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    private void show_zt_si_xi(Table_JingDian_CS_QY table, int seat_index) {
        table.effective_zt_dsx_count[seat_index]++;

        ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

        int show_card_indexs[] = new int[GameConstants.MAX_INDEX];

        if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_ZT_SI_XI)) {
            // TODO 中途四喜判断，不能把chr的值加入放到这里，因为如果点了过，并不能直接set_empty();
            // 因为其他的小胡并不是中途的，如果set_empty()会把之前的小胡类型也清空掉了，所以放在发牌处理的show_zt_si_xi方法里
            start_hu_right.opr_or_jd_cs(Constants_JingDian_CS_QY.CHR_ZT_DA_SI_XI);

            table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1,
                    new long[] { Constants_JingDian_CS_QY.CHR_ZT_DA_SI_XI }, 1, GameConstants.INVALID_SEAT);

            if (start_hu_right._index_da_si_xi != GameConstants.MAX_INDEX) {
                show_card_indexs[start_hu_right._index_da_si_xi] = 4;
            }
        }

        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(show_card_indexs, cards);

        table.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards,
                GameConstants.INVALID_SEAT);

        // TODO 小胡及时算分
        int lStartHuScore = 2 * GameConstants.CELL_SCORE;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            if (p == seat_index)
                continue;

            int s = lStartHuScore;

            if (table.has_rule(Constants_JingDian_CS_QY.GAME_RULE_ZHUANG_XIAN)) {
                if ((table.GRR._banker_player == p) || (table.GRR._banker_player == seat_index)) {
                    s += s / 2;
                }
            }

            table.GRR._start_hu_score[p] -= s;
            table.GRR._start_hu_score[seat_index] += s;

            table._player_result.game_score[p] -= s;
            table._player_result.game_score[seat_index] += s;
        }

        table.operate_player_info();

        GameSchedule.put(new Runnable() {
            @Override
            public void run() {
                table.operate_show_card(_seat_index, GameConstants.Show_Card_XiaoHU, 0, null,
                        GameConstants.INVALID_SEAT);

                PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];

                curPlayerStatus.clean_action(GameConstants.WIK_ZT_SI_XI);

                if (!curPlayerStatus.has_action_by_code(GameConstants.WIK_ZT_LLS)) {
                    ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
                    chr.set_empty();

                    int card_type = Constants_JingDian_CS_QY.HU_CARD_TYPE_ZI_MO;

                    table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;

                    int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data,
                            chr, card_type, _seat_index);

                    table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

                    if (action != GameConstants.WIK_NULL) {
                        curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                        curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
                    } else {
                        chr.set_empty();
                    }

                    m_gangCardResult.cbCardCount = 0;
                    if (table.GRR._left_card_count > 0) {
                        int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                m_gangCardResult, true);

                        if (cbActionMask != GameConstants.WIK_NULL) {
                            if (table._playerStatus[_seat_index].lock_huan_zhang() == false) {
                                curPlayerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                            }

                            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                                curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _seat_index,
                                        m_gangCardResult.isPublic[i]);
                                boolean can_gang = false;
                                if (table.GRR._left_card_count > 2) {
                                    int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
                                    int save_count = table.GRR._cards_index[_seat_index][bu_index];
                                    table.GRR._cards_index[_seat_index][bu_index] = 0;

                                    int cbWeaveIndex = table.GRR._weave_count[_seat_index];

                                    if (m_gangCardResult.type[i] == GameConstants.GANG_TYPE_AN_GANG) {
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[i];
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
                                        table.GRR._weave_count[_seat_index]++;
                                    }

                                    can_gang = table.is_cs_ting_card(table.GRR._cards_index[_seat_index],
                                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                            _seat_index);

                                    table.GRR._weave_count[_seat_index] = cbWeaveIndex;

                                    table.GRR._cards_index[_seat_index][bu_index] = save_count;

                                    if (can_gang) {
                                        curPlayerStatus.add_action(GameConstants.WIK_GANG);
                                        curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                                m_gangCardResult.isPublic[i]);
                                    }
                                }
                            }
                        }
                    }
                }

                if (curPlayerStatus.has_action()) {
                    curPlayerStatus._response = false;
                    table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(_seat_index, false);
                } else {
                    table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);

                    if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                        GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                                SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
                    } else {
                        table.operate_player_status();
                    }
                }
            }
        }, 4, TimeUnit.SECONDS);

    }

    private void show_zt_liu_liu_shun(Table_JingDian_CS_QY table, int seat_index) {
        table.effective_zt_lls_count[seat_index]++;

        ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

        int show_card_indexs[] = new int[GameConstants.MAX_INDEX];

        if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_ZT_LLS)) {
            // TODO 中途四喜判断，不能把chr的值加入放到这里，因为如果点了过，并不能直接set_empty();
            // 因为其他的小胡并不是中途的，如果set_empty()会把之前的小胡类型也清空掉了，所以放在发牌处理的show_zt_si_xi方法里
            start_hu_right.opr_or_jd_cs(Constants_JingDian_CS_QY.CHR_ZT_LLS);

            table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1,
                    new long[] { Constants_JingDian_CS_QY.CHR_ZT_LLS }, 1, GameConstants.INVALID_SEAT);

            if (start_hu_right._index_zt_lls_1 != GameConstants.MAX_INDEX
                    && start_hu_right._index_zt_lls_2 != GameConstants.MAX_INDEX) {
                show_card_indexs[start_hu_right._index_zt_lls_1] = 3;
                show_card_indexs[start_hu_right._index_zt_lls_2] = 3;
            }
        }

        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(show_card_indexs, cards);

        table.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards,
                GameConstants.INVALID_SEAT);

        // TODO 小胡及时算分
        int lStartHuScore = 2 * GameConstants.CELL_SCORE;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            if (p == seat_index)
                continue;

            int s = lStartHuScore;

            if (table.has_rule(Constants_JingDian_CS_QY.GAME_RULE_ZHUANG_XIAN)) {
                if ((table.GRR._banker_player == p) || (table.GRR._banker_player == seat_index)) {
                    s += s / 2;
                }
            }

            table.GRR._start_hu_score[p] -= s;
            table.GRR._start_hu_score[seat_index] += s;

            table._player_result.game_score[p] -= s;
            table._player_result.game_score[seat_index] += s;
        }

        table.operate_player_info();

        GameSchedule.put(new Runnable() {
            @Override
            public void run() {
                table.operate_show_card(_seat_index, GameConstants.Show_Card_XiaoHU, 0, null,
                        GameConstants.INVALID_SEAT);

                PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];

                curPlayerStatus.clean_action(GameConstants.WIK_ZT_LLS);

                if (!curPlayerStatus.has_action_by_code(GameConstants.WIK_ZT_SI_XI)) {
                    ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
                    chr.set_empty();

                    int card_type = Constants_JingDian_CS_QY.HU_CARD_TYPE_ZI_MO;

                    table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;

                    int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data,
                            chr, card_type, _seat_index);

                    table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

                    if (action != GameConstants.WIK_NULL) {
                        curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                        curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
                    } else {
                        chr.set_empty();
                    }

                    m_gangCardResult.cbCardCount = 0;
                    if (table.GRR._left_card_count > 0) {
                        int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
                                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                m_gangCardResult, true);

                        if (cbActionMask != GameConstants.WIK_NULL) {
                            if (table._playerStatus[_seat_index].lock_huan_zhang() == false) {
                                curPlayerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
                            }

                            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                                curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _seat_index,
                                        m_gangCardResult.isPublic[i]);
                                boolean can_gang = false;
                                if (table.GRR._left_card_count > 2) {
                                    int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
                                    int save_count = table.GRR._cards_index[_seat_index][bu_index];
                                    table.GRR._cards_index[_seat_index][bu_index] = 0;

                                    int cbWeaveIndex = table.GRR._weave_count[_seat_index];

                                    if (m_gangCardResult.type[i] == GameConstants.GANG_TYPE_AN_GANG) {
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[i];
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                                        table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
                                        table.GRR._weave_count[_seat_index]++;
                                    }

                                    can_gang = table.is_cs_ting_card(table.GRR._cards_index[_seat_index],
                                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                                            _seat_index);

                                    table.GRR._weave_count[_seat_index] = cbWeaveIndex;

                                    table.GRR._cards_index[_seat_index][bu_index] = save_count;

                                    if (can_gang) {
                                        curPlayerStatus.add_action(GameConstants.WIK_GANG);
                                        curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                                m_gangCardResult.isPublic[i]);
                                    }
                                }
                            }
                        }
                    }
                }

                if (curPlayerStatus.has_action()) {
                    curPlayerStatus._response = false;
                    table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(_seat_index, false);
                } else {
                    table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);

                    if (table._playerStatus[_seat_index].lock_huan_zhang()) {
                        GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                                SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
                    } else {
                        table.operate_player_status();
                    }
                }
            }
        }, 4, TimeUnit.SECONDS);

    }

    @Override
    public boolean handler_player_be_in_room(Table_JingDian_CS_QY table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        roomResponse.setIsGoldRoom(table.is_sys());

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
        // tableResponse.setActionMask((_response[seat_index] == false) ?
        // _player_action[seat_index] : MJGameConstants.WIK_NULL);

        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);// 是否托管
            // 剩余牌数
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                int_array.addItem(table.GRR._discard_cards[i][j]);
            }
            tableResponse.addDiscardCards(int_array);

            // 组合扑克
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

            //
            tableResponse.addWinnerOrder(0);

            // 牌

            if (i == _seat_index) {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
            } else {
                tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            }

        }

        // 数据
        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        // 如果断线重连的人是自己
        if (seat_index == _seat_index) {
            table._logic.remove_card_by_data(hand_cards, _send_card_data);
        }

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

        if ((out_ting_count > 0) && (seat_index == _seat_index)) {
            for (int j = 0; j < hand_card_count; j++) {
                for (int k = 0; k < out_ting_count; k++) {
                    if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
                        hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
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
            roomResponse.addOutCardTing(
                    table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < ting_card_cout; j++) {
                int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
            }
            roomResponse.addOutCardTingCards(int_array);
        }

        table.send_response_to_player(seat_index, roomResponse);

        int real_card = _send_card_data;

        if (ting_send_card) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }

        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        // 听牌显示
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
