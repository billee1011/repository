package com.cai.game.mj.hunan.new_xiang_tan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.MJConstants_HuNan_XiangTan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_HuNan_XiangTan extends MJHandlerDispatchCard<MJTable_HuNan_XiangTan> {
    boolean ting_send_card = false;

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 清空所有玩家的动作
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 发牌之后，当前玩家又可以胡牌了，如果有漏胡玩法的话，这行代码必须加进去
        table._playerStatus[_seat_index].chi_hu_round_valid();
        table._playerStatus[_seat_index].clear_cards_abandoned_hu();

        // 荒庄结束
        if (table.GRR._left_card_count == 0) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
            }

            table._cur_banker = (_seat_index + (table.getTablePlayerNumber() - 1)) % table.getTablePlayerNumber(); // 最后摸牌的是下一局的庄家

            // 流局
            table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

            return;
        }

        // 重置玩家的部分状态
        PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
        currentPlayerStatus.reset();

        // 从牌堆发一张牌并记录
        ++table._send_card_count;
        _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        --table.GRR._left_card_count;

        if (table.DEBUG_CARDS_MODE) {
            _send_card_data = 0x29;
        }

        // 记录当前玩家，牌的提供者
        table._current_player = _seat_index;
        table._provide_player = _seat_index;

        // 分析是否能自摸胡牌，需要先清空玩家的CHR
        ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
        chr.set_empty();

        int action = GameConstants.WIK_NULL;
        if (!currentPlayerStatus.isAbandoned()) { // 没打王牌弃胡
            // 胡牌检测
            action = table.analyse_chi_hu_card_new(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO, _seat_index, false);
        }

        if (GameConstants.WIK_NULL != action) {
            // 如果发的这张牌能胡牌，当前玩家状态，添加相应的动作
            currentPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
            currentPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
        } else {
            chr.set_empty();
        }

        // 前面只是分析胡牌并在当前玩家状态了添加动作
        // 加到手牌
        ++table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)];

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int count = 0;
        int ting_count = 0;
        int send_card_index = table._logic.switch_to_card_index(_send_card_data);
        ting_send_card = false;

        int card_type_count = GameConstants.MAX_ZI;

        for (int i = 0; i < card_type_count; i++) {
            if (table._logic.is_magic_index(i)) {
                continue;
            }

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
                if (table._logic.is_magic_card(cards[i])) {
                    cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
                } else {
                    for (int j = 0; j < ting_count; j++) {
                        if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
                            cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            break;
                        }
                    }
                }
            }

            table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
        }
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        // 处理王牌
        int real_card = _send_card_data;
        if (table._logic.is_magic_card(_send_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        } else if (ting_send_card) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }

        // 客户端显示玩家抓牌
        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

        // 当前牌桌记录提供的牌
        table._provide_card = _send_card_data;

        // 湖南湘潭麻将判断杠，抓牌之前是听牌状态，杠牌之后也得是听牌状态
        if (table.GRR._left_card_count > 0 && table._playerStatus[_seat_index].get_ting_state() == true) {
            m_gangCardResult.cbCardCount = 0;
            int cbActionMask = table._logic.analyse_gang_exclude_magic_card(table.GRR._cards_index[_seat_index],
                    _send_card_data, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                    m_gangCardResult);
            if (0 != cbActionMask) {
                boolean flag = false;
                for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                    // 删除手牌并放入落地牌之前，保存状态数据信息
                    int tmp_card_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
                    int tmp_card_count = table.GRR._cards_index[_seat_index][tmp_card_index];
                    int tmp_weave_count = table.GRR._weave_count[_seat_index];

                    // 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
                    // 发牌时，杠牌只要碰杠和暗杠这两种
                    table.GRR._cards_index[_seat_index][tmp_card_index] = 0;
                    if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[i]) {
                        table.GRR._weave_items[_seat_index][tmp_weave_count].public_card = 0;
                        table.GRR._weave_items[_seat_index][tmp_weave_count].center_card = m_gangCardResult.cbCardData[i];
                        table.GRR._weave_items[_seat_index][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
                        table.GRR._weave_items[_seat_index][tmp_weave_count].provide_player = _seat_index;
                        ++table.GRR._weave_count[_seat_index];
                    }

                    boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
                            table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

                    // 还原手牌数据和落地牌数据
                    table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
                    table.GRR._weave_count[_seat_index] = tmp_weave_count;

                    // 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
                    if (is_ting_state_after_gang) {
                        currentPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
                                m_gangCardResult.isPublic[i]);
                        flag = true;
                    }
                }
                if (flag) { // 如果能杠，当前用户状态加上杠牌动作
                    currentPlayerStatus.add_action(GameConstants.WIK_GANG);
                }
            }
        }

        // 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
        if (currentPlayerStatus.has_action()) {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();
        }
    }

    // 每一个Handler里面的这个方法，都会根据玩法规则不同和本身处理类型的不同而有所区别，比如发牌之后，玩家能对这张牌进行杠牌和胡牌操作
    // 比如玩家打出一张牌之后，其他玩家能对这张牌进行吃、碰、杠、胡操作。
    // 玩家在客户端点了弹出来得‘吃’、‘碰’、‘杠’、‘胡’、‘过’，之后就会进这里
    // 特别注意seat_index变量和_seat_index变量的差别，发牌时是一个意思，杠牌时和出牌时，可能就不是同一个值了
    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
            int operate_card) {
        // Begin 不同的处理器里面的前面几行判断代码是不一样的
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
        // End 不同的处理器里面的前面几行判断代码是不一样的

        // 如果发完牌之后当前玩家的客户端有杠胡等操作，但是他点了'过'，情况当前玩家的动作和状态，把状态改成出牌
        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            playerStatus.clean_action();
            playerStatus.clean_status();

            table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
            table.operate_player_status();

            return true;
        }
        
        

        // 发牌的时候，执行动作之前是不需要判断优先级的
        // 但是其他的情况有可能需要判断。比如胡牌时，如果有截胡的说法，那么只有一个人能胡，要等所有人在客户端点了操作之后,
        // 才进入下面的操作代号判断代码块。如果可以多人同时操作，比如胡牌，下面的代码会及时执行，但是关键的那个GameSchedule很重要
        // 比如A点了胡，会进入switch的自摸代码段，但是游戏并没有马上结束，要等所有有操作的人操作了之后才胡结束游戏

        switch (operate_code) {
        case GameConstants.WIK_GANG: {
            for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
                if (operate_card == m_gangCardResult.cbCardData[i]) {
                    table.exe_gang(seat_index, seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
                            false);
                    return true; // 即使有多个杠，杠玩第一张牌之后就要返回，然后再次进入发牌处理器
                }
            }
        }
            break;
        case GameConstants.WIK_ZI_MO: {
            // 点了胡牌之后，设置牌局和牌桌的一些状态值，计算分数并且立即结束本局游戏
            table.GRR._chi_hu_rights[seat_index].set_valid(true);
            table._cur_banker = seat_index;
            table.set_niao_card(seat_index);
            table.GRR._chi_hu_card[seat_index][0] = operate_card;
            table._player_result.zi_mo_count[seat_index]++;
            // 客户端弹出来相应的动画效果，并处理手牌和落地的牌
            table.process_chi_hu_player_operate(seat_index, operate_card, true);
            // 计算发牌时的自摸胡分数
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO, true);

            // 发牌时点了胡牌，游戏会立即结束，出牌时有人点了操作，要等所有人操作完之后，游戏才结束
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
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
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
                } else {
                    for (int k = 0; k < out_ting_count; k++) {
                        if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
                            hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
                            break;
                        }
                    }
                }
            }
        } else {
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
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
        if (table._logic.is_magic_card(_send_card_data)) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        } else if (ting_send_card) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
        }

        table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

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
