package com.cai.game.mj.handler.hunanhengyang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate_HY extends MJHandlerOutCardOperate<MJTable_HY> {
    @Override
    public void exe(MJTable_HY table) {
        PlayerStatus playerStatus = table._playerStatus[_out_card_player];

        // 重置玩家状态
        // playerStatus.clean_status();
        table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
        playerStatus.clean_action();

        //
        // 出牌记录
        table._out_card_count++;
        table._out_card_player = _out_card_player;
        table._out_card_data = _out_card_data;

        if (table._logic.is_magic_card(_out_card_data)) { // 打红中癞子，本局弃胡
            playerStatus.setAbandoned(true);
        }
        // 用户切换
        int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
        table._current_player = next_player;

        // 刷新手牌
        int cards[] = new int[GameConstants.MAX_COUNT];

        // 刷新自己手牌
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
        // 特殊牌
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_wang_ba_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }
        table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

        // 显示出牌
        table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
                GameConstants.INVALID_SEAT);
        table._provide_player = _out_card_player;
        table._provide_card = _out_card_data;

        // 玩家出牌 响应判断,是否有吃碰杠补胡
        boolean bAroseAction = table.estimate_player_out_card_respond_hy(_out_card_player, _out_card_data);// ,
                                                                                                           // EstimatKind.EstimatKind_OutCard

        // 如果没有需要操作的玩家，派发扑克
        if (bAroseAction == false) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
                // table._playerStatus[i].clean_status();
            }

            table.operate_player_action(_out_card_player, true);

            table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
                    GameConstants.DELAY_SEND_CARD_DELAY);

            // 发牌
            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
        } else {
            // 等待别人操作这张牌
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (table._playerStatus[i].has_action()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    /***
     * //用户操作
     * 
     * @param seat_index
     * @param operate_code
     * @param operate_card
     * @return
     */
    @Override
    public boolean handler_operate_card(MJTable_HY table, int seat_index, int operate_code, int operate_card) {
        // 效验状态

        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 是否已经响应
        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return true;
        }

        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
        {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return true;
        }

        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            // 效果
            table.process_chi_hu_player_operate_hy(seat_index, new int[] { operate_card }, 1, false);

        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
            table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
        }

        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                return false;
        }

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;

        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action);
                }

                // 优先级别
                int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                }
            }
        }
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 修改网络导致吃碰错误 9.26 WalkerGeek
        int target_card = _out_card_data;
        // error int target_card = table._playerStatus[target_player]._operate_card;

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }

        // 删除扑克
        switch (target_action) {
        case GameConstants.WIK_LEFT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_RIGHT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_CENTER: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_PENG: // 碰牌操作
        {

            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }

            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }

        case GameConstants.WIK_YAO_YI_SE: // 补张牌操作
        case GameConstants.WIK_GANG: // 杠牌操作
        {
            // 是否有抢杠胡
            table.exe_gang(target_player, _out_card_player, target_card, target_action,
                    GameConstants.GANG_TYPE_JIE_GANG, false, false);
            return true;
        }
        case GameConstants.WIK_NULL: {

            // 删掉出来的那张牌
            // table.operate_out_card(this._out_card_player, 0,
            // null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);

            // 加到牌堆 没有人要
            table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, false, 0);

            // 用户切换
            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();

            // 发牌
            table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            int jie_pao_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                jie_pao_count++;
            }

            if (jie_pao_count > 0) {
                int player_seat = 0;
                if (jie_pao_count > 1) {
                    // 结束
                    table._cur_banker = _out_card_player;
                    player_seat = _out_card_player;
                } else if (jie_pao_count == 1) {
                    table._cur_banker = target_player;
                    // 结束后设置鸟牌
                    player_seat = target_player;
                }

                // 设置鸟
                if (table.has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)) {
                    table.set_niao_card_hy(player_seat, GameConstants.INVALID_VALUE, true, 0, false, operate_card);// 结束后设置鸟牌
                } else {
                    table.set_niao_card_hy(player_seat, GameConstants.INVALID_VALUE, true, 0, false,
                            GameConstants.INVALID_VALUE);// 结束后设置鸟牌
                }

                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                        continue;
                    }
                    table.process_chi_hu_player_score(i, _out_card_player, GameConstants.INVALID_CARD, false);
                    table.GRR._chi_hu_card[i][0] = target_card;
                    // 记录
                    table._player_result.jie_pao_count[i]++;
                    table._player_result.dian_pao_count[_out_card_player]++;

                }

                GameSchedule.put(
                        new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            }
            return true;
        }
        default:
            return false;
        }
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HY table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        // 游戏变量
        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(_out_card_player);
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
                if (table._logic.is_wang_ba_card(table.GRR._discard_cards[i][j])) {
                    // 特殊牌
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
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
                if (table._logic.is_wang_ba_card(table.GRR._weave_items[i][j].center_card)) {
                    weaveItem_item.setCenterCard(
                            table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                }

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
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_wang_ba_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }
        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }
        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        int real_card = _out_card_data;
        if (table._logic.is_wang_ba_card(_out_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        }
        // 出牌
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                seat_index);

        // table.operate_player_get_card(_seat_index, 1, new
        // int[]{_send_card_data});
        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        return true;
    }
}
