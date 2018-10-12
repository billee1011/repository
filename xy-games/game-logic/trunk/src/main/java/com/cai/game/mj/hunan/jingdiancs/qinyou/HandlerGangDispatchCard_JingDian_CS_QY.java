package com.cai.game.mj.hunan.jingdiancs.qinyou;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_JingDian_CS_QY;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.RemoveOutCardRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGangDispatchCard_JingDian_CS_QY extends AbstractMJHandler<Table_JingDian_CS_QY> {
    private int _seat_index;

    private GangCardResult m_gangCardResult;
    private CardsData _gang_card_data;

    public HandlerGangDispatchCard_JingDian_CS_QY() {
        _gang_card_data = new CardsData(GameConstants.MAX_COUNT);
        m_gangCardResult = new GangCardResult();
    }

    public void reset_status(int seat_index) {
        _seat_index = seat_index;
    }

    @Override
    public void exe(Table_JingDian_CS_QY table) {
        table.zi_mo_count = 0;
        table.jie_pao_count = new int[table.getTablePlayerNumber()];

        ChiHuRight chr = null;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            chr = table.GRR._chi_hu_rights[i];
            chr.set_empty();
            operate_player_action_gang(table, i, true);
        }
        table._playerStatus[_seat_index].set_card_status(GameConstants.CARD_STATUS_CS_GANG);
        table._playerStatus[_seat_index].chi_hu_round_valid();

        this._gang_card_data.clean_cards();

        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        table._out_card_data = GameConstants.INVALID_VALUE;
        table._out_card_player = GameConstants.INVALID_SEAT;
        table._current_player = _seat_index;

        table._provide_player = _seat_index;

        int bu_card;

        for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
            table._send_card_count++;
            bu_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
            if (table.DEBUG_CARDS_MODE) {
                if (i == 0)
                    bu_card = 0x12;
                if (i == 1)
                    bu_card = 0x15;
            }
            --table.GRR._left_card_count;
            this._gang_card_data.add_card(bu_card);
        }

        table.operate_out_card(_seat_index, GameConstants.CS_GANG_DRAW_COUNT, this._gang_card_data.get_cards(),
                GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

        // TODO 杠牌而非补牌之后，需要重新获取听牌数据
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

        boolean has_action = false;
        m_gangCardResult.cbCardCount = 0;
        for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
            boolean bAroseAction = false;
            bu_card = this._gang_card_data.get_card(i);

            for (int k = 0; k < table.getTablePlayerNumber(); k++) {
                if (k == _seat_index) {
                    chr = table.GRR._chi_hu_rights[k];
                    int action = table.analyse_chi_hu_card(table.GRR._cards_index[k], table.GRR._weave_items[k],
                            table.GRR._weave_count[k], bu_card, chr, Constants_JingDian_CS_QY.HU_CARD_TYPE_GANG_KAI, k);

                    if (action != GameConstants.WIK_NULL) {
                        table.zi_mo_count++;

                        if (curPlayerStatus.has_zi_mo() == false) {
                            curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
                            curPlayerStatus.add_zi_mo(bu_card, k);
                        }

                        bAroseAction = true;
                    }

                    if (table.GRR._left_card_count > 2) {
                        int cbActionMask = table._logic.analyse_gang_card_cs(table.GRR._cards_index[k], bu_card,
                                table.GRR._weave_items[k], table.GRR._weave_count[k], m_gangCardResult);
                        boolean can_gang = false;
                        if (cbActionMask != GameConstants.WIK_NULL) {
                            for (int gc = 0; gc < m_gangCardResult.cbCardCount; gc++) {

                                int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[gc]);
                                int save_count = table.GRR._cards_index[k][bu_index];
                                table.GRR._cards_index[k][bu_index] = 0;
                                int cbWeaveIndex = table.GRR._weave_count[k];

                                if (m_gangCardResult.type[gc] == GameConstants.GANG_TYPE_AN_GANG) {
                                    table.GRR._weave_items[k][cbWeaveIndex].public_card = 0;
                                    table.GRR._weave_items[k][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[gc];
                                    table.GRR._weave_items[k][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
                                    table.GRR._weave_items[k][cbWeaveIndex].provide_player = k;
                                    table.GRR._weave_count[k]++;
                                }

                                if (curPlayerStatus.lock_huan_zhang()) {
                                    boolean has_huan_zhang = table.check_gang_huan_zhang(k,
                                            m_gangCardResult.cbCardData[gc]);
                                    can_gang = !has_huan_zhang;
                                } else {
                                    can_gang = table.is_cs_ting_card(table.GRR._cards_index[k],
                                            table.GRR._weave_items[k], table.GRR._weave_count[k], k);
                                }

                                table.GRR._weave_count[k] = cbWeaveIndex;
                                table.GRR._cards_index[_seat_index][bu_index] = save_count;

                                if (can_gang) {
                                    curPlayerStatus.add_action(GameConstants.WIK_GANG);
                                    curPlayerStatus.add_gang(m_gangCardResult.cbCardData[gc], k,
                                            m_gangCardResult.isPublic[gc]);
                                    bAroseAction = true;
                                }
                            }
                        }
                    }

                } else {
                    bAroseAction = table.estimate_player_fan_pai_response(k, _seat_index, bu_card);
                }

                if (bAroseAction == true) {
                    has_action = true;
                }
            }
        }

        if (has_action == false) {
            table._provide_player = GameConstants.INVALID_SEAT;
            table._out_card_player = _seat_index;

            GameSchedule.put(
                    new RemoveOutCardRunnable(table.getRoom_id(), _seat_index, GameConstants.OUT_CARD_TYPE_LEFT),
                    GameConstants.GANG_CARD_CS_DELAY, TimeUnit.MILLISECONDS);
            GameSchedule.put(
                    new AddDiscardRunnable(table.getRoom_id(), _seat_index, this._gang_card_data.get_card_count(),
                            this._gang_card_data.get_cards(), true, table.getMaxCount()),
                    GameConstants.GANG_CARD_CS_DELAY, TimeUnit.MILLISECONDS);

            table._current_player = (_seat_index + 1) % table.getTablePlayerNumber();

            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.GANG_CARD_CS_DELAY);
        } else {
            table._provide_player = _seat_index;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (table._playerStatus[i].has_action()) {
                    operate_player_action_gang(table, i, false);
                }
            }
        }
    }

    @Override
    public boolean handler_operate_card(Table_JingDian_CS_QY table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return true;
        }

        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return true;
        }

        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);
        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table.GRR._chi_hu_rights[seat_index].set_valid(false);
            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();
            }
        }

        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();

            int cbUserActionRank = 0;
            int cbTargetActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                } else {
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }

        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        if (target_action != GameConstants.WIK_ZI_MO) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                    return false;
            }
        }

        int target_card = table._playerStatus[target_player]._operate_card;

        switch (target_action) {
        case GameConstants.WIK_LEFT: {
            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }

            exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_RIGHT: {
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_CENTER: {
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_PENG: {
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }

            exe_chi_peng(table, target_player, target_action, target_card);
        }
            break;
        case GameConstants.WIK_BU_ZHNAG:
        case GameConstants.WIK_GANG: {
            table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT,
                    GameConstants.INVALID_SEAT);

            int add_card = -1;
            boolean card_check = true;
            for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
                if (card_check && (target_card == this._gang_card_data.get_card(i))) {
                    card_check = false;
                } else {
                    add_card = this._gang_card_data.get_card(i);
                }
            }
            if (add_card == -1) {
                table.exe_add_discard(this._seat_index, 2, this._gang_card_data.get_cards(), true, 0);
            } else {
                table.exe_add_discard(this._seat_index, 1, new int[] { add_card }, true, 0);
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                operate_player_action_gang(table, i, true);
            }

            if (_seat_index == target_player) {
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    if (target_card == m_gangCardResult.cbCardData[i]) {
                        table.exe_gang(_seat_index, _seat_index, target_card, target_action, m_gangCardResult.type[i],
                                true, true);
                        return true;
                    }
                }
            } else {
                table.exe_gang(target_player, _seat_index, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG,
                        false, false);
            }

            return true;
        }
        case GameConstants.WIK_NULL: {
            table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT,
                    GameConstants.INVALID_SEAT);

            table.exe_add_discard(this._seat_index, 2, this._gang_card_data.get_cards(), true, 0);

            table._current_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_ZI_MO: {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == _seat_index) {
                    table.GRR._chi_hu_rights[i].set_valid(true);
                } else {
                    table.GRR._chi_hu_rights[i].set_valid(false);
                }
            }

            table.set_niao_card(_seat_index, true, false);

            table.GRR._chi_hu_rights[_seat_index].set_valid(true);

            if (table.GRR._chi_hu_rights[_seat_index].is_mul_long(Constants_JingDian_CS_QY.CHR_ZI_MO)) {
                table.GRR._chi_hu_card[_seat_index][0] = _gang_card_data.get_card(0);
                table.GRR._chi_hu_card[_seat_index][1] = _gang_card_data.get_card(1);
                table.process_chi_hu_player_operate(_seat_index, _gang_card_data.get_cards(), 0, false);
                table.process_chi_hu_player_score(_seat_index, _seat_index, _gang_card_data.get_card(0), true);
            } else {
                table.GRR._chi_hu_card[_seat_index][0] = target_card;
                table.process_chi_hu_player_operate(_seat_index, new int[] { target_card }, 1, false);
                table.process_chi_hu_player_score(_seat_index, _seat_index, target_card, true);
            }
            
            table._player_result.zi_mo_count[_seat_index]++;

            table._player_result.da_hu_zi_mo[_seat_index]++;

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                operate_player_action_gang(table, i, true);
            }
            table._cur_banker = _seat_index;
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        case GameConstants.WIK_CHI_HU: {
            table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_JingDian_CS_QY.CHR_FANG_PAO);

            int jie_pao_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                jie_pao_count++;
            }
            if (jie_pao_count > 1) {
                table._cur_banker = _seat_index;
                table.set_niao_card(_seat_index, true, false);
            } else {
                table._cur_banker = target_player;
                table.set_niao_card(target_player, true, false);
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }

                table.GRR._chi_hu_rights[i].set_valid(true);

                if (table.GRR._chi_hu_rights[i].is_mul_long(Constants_JingDian_CS_QY.CHR_JIE_PAO)) {
                    table.GRR._chi_hu_card[i][0] = _gang_card_data.get_card(0);
                    table.GRR._chi_hu_card[i][1] = _gang_card_data.get_card(1);
                    table.process_chi_hu_player_operate(i, _gang_card_data.get_cards(), 0, false);
                    table.process_chi_hu_player_score(i, _seat_index, _gang_card_data.get_card(0), false);
                } else {
                    table.GRR._chi_hu_card[i][0] = table._playerStatus[i]._operate_card;
                    table.process_chi_hu_player_operate(i, new int[] { table._playerStatus[i]._operate_card }, 1,
                            false);
                    table.process_chi_hu_player_score(i, _seat_index, table._playerStatus[i]._operate_card, false);
                }

                table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_seat_index]++;
                
                table._player_result.da_hu_jie_pao[i]++;
                table._player_result.da_hu_dian_pao[_seat_index]++;
            }
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();

                operate_player_action_gang(table, i, true);
            }
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
            return true;
        }
        default:
            return false;
        }
        return true;
    }

    private void exe_chi_peng(Table_JingDian_CS_QY table, int target_player, int target_action, int target_card) {
        table.operate_out_card(this._seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

        int add_card = 0;
        for (int i = 0; i < GameConstants.CS_GANG_DRAW_COUNT; i++) {
            add_card = this._gang_card_data.get_card(i);
            if (target_card != add_card) {
                break;
            }
        }

        table.exe_add_discard(this._seat_index, 1, new int[] { add_card }, true, 0);

        table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_GANG);
    }

    @Override
    public boolean handler_player_be_in_room(Table_JingDian_CS_QY table, int seat_index) {
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

            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

        }

        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
        }

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                new long[] { GameConstants.WIK_GANG }, 1, seat_index);

        table.operate_out_card(_seat_index, 2, this._gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_LEFT,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            operate_player_action_gang(table, seat_index, false);
        }

        return true;
    }
    
    public boolean operate_player_action_gang(Table_JingDian_CS_QY table, int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);
		roomResponse.addDouliuzi(_gang_card_data.get_cards()[0]);
		roomResponse.addDouliuzi(_gang_card_data.get_cards()[1]);
		table.load_common_status(roomResponse);

		if (close == true) {
			table.GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			table.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		table.GRR.add_room_response(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}
}
