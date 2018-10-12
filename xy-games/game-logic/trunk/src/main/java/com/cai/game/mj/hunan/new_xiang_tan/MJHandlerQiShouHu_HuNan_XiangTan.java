package com.cai.game.mj.hunan.new_xiang_tan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.MJConstants_HuNan_XiangTan;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerQiShouHu_HuNan_XiangTan extends AbstractMJHandler<MJTable_HuNan_XiangTan> {

    protected int _banker = GameConstants.INVALID_SEAT;

    public void reset_status(int banker) {
        _banker = banker;
    }

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 清空所有玩家的动作
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
            table._playerStatus[i].chi_hu_round_valid();

            // 发完牌并处理王牌之后，进入起手混handler，在第一次给玩家发牌之前，需要判断玩家是否已经听牌
            boolean is_ting_state = table.is_ting_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
                    table.GRR._weave_count[i], i);
            if (is_ting_state) {
                table._playerStatus[i].set_ting_state(true);
            } else {
                table._playerStatus[i].set_ting_state(false);
            }
        }

        boolean bAroseAction = false;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            for (int j = 0; j < table._logic.get_magic_card_count(); j++) {
                // 起手4个一样的混
                if (table.GRR._cards_index[i][table._logic.get_magic_card_index(j)] == 4) {
                    bAroseAction = true;
                    table._playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
                    table._playerStatus[i]
                            .add_zi_mo(table._logic.switch_to_card_data(table._logic.get_magic_card_index(j)), i);
                }
            }
        }

        if (bAroseAction == false) { // 没起手胡，直接发牌
            table.exe_dispatch_card(_banker, GameConstants.WIK_NULL, 0);
        } else {
            // 等待别人操作
            PlayerStatus playerStatus = null;
            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                playerStatus = table._playerStatus[i];
                if (playerStatus.has_action()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
            int operate_card) {
        // Begin 不同的处理器里面的前面几行判断代码是不一样的
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
        playerStatus.clean_status();
        // End 不同的处理器里面的前面几行判断代码是不一样的

        if (GameConstants.WIK_NULL == operate_code) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        // 起手胡，优先级判断，只能有一个人胡
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
            int i = (_banker + p) % GameConstants.GAME_PLAYER;
            if (i == target_player) {
                target_p = GameConstants.GAME_PLAYER - p;
            }
        }
        for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
            int i = (_banker + p) % GameConstants.GAME_PLAYER;

            int cbUserActionRank = 0;
            int cbTargetActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + GameConstants.GAME_PLAYER - p;
                } else {
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + GameConstants.GAME_PLAYER - p;
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
                    target_p = GameConstants.GAME_PLAYER - p;
                }
            }
        }

        // 如果优先级最高的人还没有操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 清空所有玩家的动作和状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
            table.operate_player_action(i, true);
        }

        // 只有胡或者过
        switch (target_action) {
        case GameConstants.WIK_NULL: { // 点过直接给庄家发牌
            table.exe_dispatch_card(_banker, GameConstants.WIK_NULL, 0);
            return true;
        }
        case GameConstants.WIK_ZI_MO: { // 起手胡算自摸
            // 点了胡牌之后，设置牌局和牌桌的一些状态值，计算分数并且立即结束本局游戏
            table.GRR._chi_hu_rights[seat_index].set_valid(true);
            table._cur_banker = seat_index;
            table.set_niao_card(seat_index);
            table.GRR._chi_hu_card[seat_index][0] = operate_card;
            table._player_result.zi_mo_count[seat_index]++;

            table.GRR._chi_hu_rights[seat_index].opr_or_xt(MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU, false);

            // 客户端弹出来相应的动画效果，并处理手牌和落地的牌
            table.process_chi_hu_player_operate(seat_index, operate_card, true);
            // 计算起手胡时的自摸胡分数
            table.process_chi_hu_player_score(seat_index, seat_index, operate_card,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QI_SHOU_HU, true);

            // 起手胡时点了胡牌，游戏会立即结束，出牌时有人点了操作，要等所有人操作完之后，游戏才结束
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        }
        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_HuNan_XiangTan table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();
        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(table.GRR._banker_player);
        tableResponse.setCellScore(0);
        tableResponse.setActionCard(0);
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);

            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
                } else {
                    int_array.addItem(table.GRR._discard_cards[i][j]);
                }

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
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        // TODO 显示听牌数据
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
