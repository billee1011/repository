package com.cai.game.mj.hunan.xiangtan;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_HuNan_XiangTan extends MJHandlerGang<MJTable_HuNan_XiangTan> {

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 用户状态
        for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
            if (table._playerStatus[i].has_action()) {
                table.operate_player_action(i, true);
            }

            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        table._playerStatus[_seat_index].chi_hu_round_valid(); // 可以胡了

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        this.exe_gang(table);
    }

    @Override
    protected boolean exe_gang(MJTable_HuNan_XiangTan table) {
        int cbCardIndex = table._logic.switch_to_card_index(_center_card);
        int cbWeaveIndex = -1;

        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            // 暗杠
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;

        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            // 别人打的牌
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;

            // 删掉出来的那张牌
            table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
            // 看看是不是有碰的牌，明杠
            // 寻找组合
            for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
                int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
                int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
                if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
                    cbWeaveIndex = i; // 第几个组合可以碰
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
        table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

        // 设置用户
        table._current_player = _seat_index;

        // 删除手上的牌
        table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
        table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
        // 刷新手牌包括组合
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }
        WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
        int weave_count = table.GRR._weave_count[_seat_index];
        for (int i = 0; i < weave_count; i++) {
            weaves[i] = new WeaveItem();
            weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
            weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
            weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
            weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
            // 癞子
            if (table._logic.is_magic_card(weaves[i].center_card)) {
                weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }
        table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

        // 算分
        int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                if (i == _seat_index)
                    continue;

                // 暗杠每人2分
                table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -2 * GameConstants.CELL_SCORE;// 暗杠，其他玩家扣分
                table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 2 * GameConstants.CELL_SCORE;// 一共加分
            }

            table._player_result.an_gang_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            // 明杠放杠者给三分
            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = 3 * GameConstants.CELL_SCORE;// m_pGameServiceOption->lCellScore*2;//配置参数乘
            table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -3 * GameConstants.CELL_SCORE;// -m_pGameServiceOption->lCellScore*2;//配置参数乘

            table._player_result.ming_gang_count[_seat_index]++;
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                if (i == _seat_index)
                    continue;

                // 碰杠每人1分
                table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -GameConstants.CELL_SCORE;// 暗杠，其他玩家扣分
                table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += GameConstants.CELL_SCORE;// 一共加分
            }

            table._player_result.ming_gang_count[_seat_index]++;
        }

        /**
         * // 选美，摸出来的牌，自己胡算杠上开花，别人胡算抢杠胡 int xuan_mei_count = table.get_xuan_mei_count();
         * 
         * table._send_card_count += xuan_mei_count;
         * 
         * int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
         * 
         * // 从剩余牌堆里顺序取出选美的牌 table._logic.switch_to_cards_index(table._repertory_card,
         * table._all_card_len - table.GRR._left_card_count, xuan_mei_count,
         * cbCardIndexTemp);
         * 
         * table.GRR._left_card_count -= xuan_mei_count;
         * 
         * boolean can_win_myself = false; boolean can_win_others = false;
         * 
         * for (int i = 0; i < GameConstants.MAX_INDEX; i++) { // 先判断自己能不能胡 if
         * (cbCardIndexTemp[i] == 0) continue; ChiHuRight chr =
         * table.GRR._chi_hu_rights[_seat_index]; int action =
         * table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
         * table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
         * table._logic.switch_to_card_data(i), chr,
         * MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI, _seat_index); // 选美自摸算杠上开花
         * // 结果判断 if (action != 0) { can_win_myself = true;
         * table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
         * table._playerStatus[_seat_index].add_zi_mo(table._logic.switch_to_card_data(i),
         * _seat_index); // 吃胡的组合 break; } }
         * 
         * if (!can_win_myself) { for (int j = 0; j < table.getTablePlayerNumber(); j++)
         * { // 再判断其他人能不能胡 if (j == _seat_index) continue; for (int i = 0; i <
         * GameConstants.MAX_INDEX; i++) { if (cbCardIndexTemp[i] == 0) continue;
         * ChiHuRight chr = table.GRR._chi_hu_rights[j]; int action =
         * table.analyse_chi_hu_card(table.GRR._cards_index[j],
         * table.GRR._weave_items[j], table.GRR._weave_count[j],
         * table._logic.switch_to_card_data(i), chr,
         * MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU, j); // 选美点炮算抢杠胡 //
         * 结果判断 if (action != 0) { can_win_others = true;
         * table._playerStatus[j].add_action(GameConstants.WIK_CHI_HU);
         * table._playerStatus[j].add_chi_hu(table._logic.switch_to_card_data(i), j); //
         * 吃胡的组合 break; } } } }
         * 
         * if (!can_win_myself && !can_win_others) { // 从后面发一张牌给下家
         * table.exe_dispatch_card(table.get_banker_next_seat(_seat_index), _type, 0); }
         * else { // 等待别人操作这这些牌 PlayerStatus playerStatus = null;
         * 
         * for (int i = 0; i < table.getTablePlayerNumber(); i++) { playerStatus =
         * table._playerStatus[i]; if (playerStatus.has_chi_hu()) {
         * table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
         * table.operate_player_action(i, false); } } }
         **/

        table.exe_gang_xuan_mei(_seat_index);

        return true;
    }

    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
            int operate_card) {

        // this.exe_gang(table);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HuNan_XiangTan table, int seat_index) {
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
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    // 癞子
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
                } else {
                    int_array.addItem(table.GRR._discard_cards[i][j]);
                }
            }
            tableResponse.addDiscardCards(int_array);

            // 组合扑克
            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
                    weaveItem_item.setCenterCard(
                            table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_GUI);
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                }
                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
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
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);

        /**
         * // 听牌显示 int ting_cards[] = table._playerStatus[seat_index]._hu_cards; int
         * ting_count = table._playerStatus[seat_index]._hu_card_count;
         * 
         * if (ting_count > 0) { table.operate_chi_hu_cards(seat_index, ting_count,
         * ting_cards); }
         **/

        table.send_response_to_player(seat_index, roomResponse);

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
