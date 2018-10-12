package com.cai.game.mj.handler.henanpds;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.ay.MJHandlerOutCardBaoTing;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardBaoTing_HeNanpds extends MJHandlerOutCardBaoTing {
    public int _out_card_player = GameConstants.INVALID_SEAT;
    public int _out_card_data = GameConstants.INVALID_VALUE;
    public int _type;

    public void reset_status(int seat_index, int card, int type) {
        _out_card_player = seat_index;
        _out_card_data = card;
        _type = type;
    }

    @Override
    public void exe(MJTable table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        table.operate_player_action(_out_card_player, true);

        // 设置为报听状态
        table._playerStatus[_out_card_player].set_card_status(GameConstants.CARD_STATUS_BAO_TING);

        // 出牌记录
        table._out_card_count++;
        table._out_card_player = _out_card_player;
        table._out_card_data = _out_card_data;

        // 用户切换
        int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
        table._current_player = next_player;

        // 效果
        table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                new long[] { GameConstants.WIK_BAO_TING }, 1, GameConstants.INVALID_SEAT);

        // 刷新手牌
        int cards[] = new int[GameConstants.MAX_COUNT];

        // 刷新自己手牌
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);

        // 处理王牌
        if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(cards[j])) {
                    cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                }
            }
        }

        table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

        // 出牌
        table.operate_out_card_bao_ting_zhou_kou(_out_card_player, 1,
                new int[] { _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING },
                GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

        // 听的牌，保存，下次不用计算
        int ting_count = table._playerStatus[_out_card_player]._hu_out_card_count;
        for (int i = 0; i < ting_count; i++) {
            int out_card = table._playerStatus[_out_card_player]._hu_out_card_ting[i];
            if (out_card == _out_card_data) {
                int tc = table._playerStatus[_out_card_player]._hu_card_count = table._playerStatus[_out_card_player]._hu_out_card_ting_count[i];
                for (int j = 0; j < tc; j++) {
                    table._playerStatus[_out_card_player]._hu_cards[j] = table._playerStatus[_out_card_player]._hu_out_cards[i][j];
                }
            }
        }

        // 引用权位
        ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];

        chr.bao_ting_index = table.GRR._discard_count[_out_card_player];
        chr.bao_ting_card = _out_card_data;

        boolean bAroseAction = false;
        if (!table._logic.is_magic_card(_out_card_data)) {
            bAroseAction = table.estimate_player_out_card_respond_henan_pds(_out_card_player, _out_card_data);
        }

        if (bAroseAction == false) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
            }

            table.operate_player_action(_out_card_player, true);

            table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
                    GameConstants.DELAY_SEND_CARD_DELAY);

            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
        } else {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                PlayerStatus playerStatus = table._playerStatus[i];
                if (playerStatus.has_action()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
                handler_check_auto_behaviour(table, i, _out_card_data);
            }
        }
    }

    @Override
    public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
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

        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
            }
        }

        // 变量定义 优先级最高操作的玩家和操作--不通炮的算法
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;
            // 优先级别
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    // 获取最大的动作的优先级
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                // 优先级别
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }

        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 修改网络导致吃碰错误 9.26 WalkerGeek
        int target_card = _out_card_data;

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
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

        case GameConstants.WIK_GANG: // 杠牌操作
        {
            // 是否有抢杠胡
            table.exe_gang(target_player, _out_card_player, target_card, target_action,
                    GameConstants.GANG_TYPE_JIE_GANG, false, false);
            return true;
        }
        case GameConstants.WIK_NULL: {
            int real_card = _out_card_data;
            if (table._logic.is_magic_card(_out_card_data)) {// 癞子替换
                real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
            }
            // 加到牌堆 没有人要
            table.exe_add_discard(this._out_card_player, 1, new int[] { real_card }, false, 0);

            // 用户切换
            table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();

            // 发牌
            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == target_player) {
                    table.GRR._chi_hu_rights[i].set_valid(true);
                } else {
                    table.GRR._chi_hu_rights[i].set_valid(false);
                }
            }

            table._cur_banker = target_player;

            table.GRR._chi_hu_card[target_player][0] = target_card;
            table.process_chi_hu_player_operate(target_player, target_card, false);
            table.process_chi_hu_player_score(target_player, _out_card_player, _out_card_data, false);

            // 记录
            table._player_result.jie_pao_count[target_player]++;
            table._player_result.dian_pao_count[_out_card_player]++;

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        default:
            return false;
        }
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
                if (j == table.GRR._chi_hu_rights[i].bao_ting_index && GameDescUtil.has_rule(table.getGameRuleIndexEx(),
                        GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) {
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);
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
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

        if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
            for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
                if (table._logic.is_magic_card(cards[i])) {
                    cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                }

                tableResponse.addCardsData(cards[i]);
            }
        } else {
            for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
                tableResponse.addCardsData(cards[i]);
            }
        }

        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        int real_card = _out_card_data;
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
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

        return true;
    }

    @Override
    public boolean handler_be_set_trustee(MJTable table, int seat_index) {
        handler_check_auto_behaviour(table, seat_index, _out_card_data);
        return false;
    }
}
