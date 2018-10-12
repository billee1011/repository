package com.cai.game.mj.hunan.new_xiang_tan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.MJConstants_HuNan_XiangTan;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.XiangTanXuanMeiRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGangXuanMei_HuNan_XiangTan extends AbstractMJHandler<MJTable_HuNan_XiangTan> {

    protected int _seat_index;
    protected CardsData _gang_card_data;
    protected int _xuan_mei_count;

    public MJHandlerGangXuanMei_HuNan_XiangTan() {
        this._gang_card_data = new CardsData(MJConstants_HuNan_XiangTan.COUNT_OF_MEI_4);
    }

    public void reset_status(int seat_index, int xuan_mei_count) {
        this._seat_index = seat_index;
        this._xuan_mei_count = xuan_mei_count;
        this._gang_card_data.clean_cards();
    }

    // 可以参考出牌的处理器
    // 将选美翻出来的2、3或4张牌，放到牌桌的正中央，注意鬼牌的处理
    // 一个玩家可以同时胡多张牌，胡牌分数直接叠加
    // 只能有一个人能胡，需要获取优先级，选美的牌可以‘胡’、‘杠’、‘碰’、‘吃’
    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 清空所有玩家的动作
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 记录一下当前牌局的状态信息
        table._provide_player = _seat_index;

        // 选美，摸出来的牌，自己胡算杠上开花，别人胡算抢杠胡

        // 这里基本不会走，杠牌时，牌堆里必然还有1张以上的牌，但是杠完选美之后，如果牌堆没牌，会直接流局
        if (_xuan_mei_count == 0)
            return;

        table._send_card_count += _xuan_mei_count;

        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        int tmp_xuan_mei_cards[] = new int[_xuan_mei_count];

        // 从剩余牌堆里顺序取出选美的牌
        table._logic.switch_to_cards_index(table._repertory_card, table._all_card_len - table.GRR._left_card_count,
                _xuan_mei_count, cbCardIndexTemp);
        // 往_gang_card_data进行存储，断线重连时能用得到，下面的判断也可以使用
        table._logic.switch_to_cards_data(cbCardIndexTemp, tmp_xuan_mei_cards);

        if (table.DEBUG_CARDS_MODE) {
            if (_xuan_mei_count == 2) {
                tmp_xuan_mei_cards = new int[] { 0x22, 0x16 };
            } else if (_xuan_mei_count == 3) {
                tmp_xuan_mei_cards = new int[] { 0x14, 0x16, 0x18 };
            } else if (_xuan_mei_count == 4) {
                tmp_xuan_mei_cards = new int[] { 0x24, 0x24, 0x24, 0x24 };
            }
        }

        // 处理鬼牌和存储杠牌数据
        // 在牌桌正中央显示牌时用_gang_card_data，分析吃碰胡时用tmp_xuan_mei_cards
        for (int i = 0; i < tmp_xuan_mei_cards.length; i++) {
            if (table._logic.is_magic_card(tmp_xuan_mei_cards[i])) {
                this._gang_card_data.add_card(tmp_xuan_mei_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
            } else {
                this._gang_card_data.add_card(tmp_xuan_mei_cards[i]);
            }
        }
        
        table._gang_card_data = this._gang_card_data;

        table.GRR._left_card_count -= _xuan_mei_count;

        // 想选美的牌显示在牌桌正中央，分析完之后，重牌桌正中央删除并放到杠牌玩家的面前，吃碰杠了的牌不用显示
        table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, _xuan_mei_count,
                this._gang_card_data.get_cards(), GameConstants.INVALID_SEAT);

        // 分析选美翻出来的牌时，所有玩家都要判断
        boolean bAroseAction = table.estimate_xuan_mei_respond(_seat_index, tmp_xuan_mei_cards);

        if (bAroseAction == false) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
                table.operate_player_action(i, true);
            }

            GameSchedule.put(new XiangTanXuanMeiRunnable(table.getRoom_id(), _seat_index), 2, TimeUnit.SECONDS);
        } else {
            // 等待别人操作这些牌
            PlayerStatus playerStatus = null;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (playerStatus.has_action()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    // 杠选美之后，有可能玩家会多次进入这个方法
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
        // 这里要注意了，当operate_card为0时，别更新玩家的操作牌和操作码
        // 否则在自己点了操作，但是优先级不够，然后别人点了过之后，后面的代码处理会出错
        // if (table._logic.is_valid_card(operate_card)) {
        // playerStatus.operate(operate_code, operate_card);
        // }
        if (playerStatus.is_respone() == false) { // 如果已经相应了，不能重复设置操作状态
            playerStatus.operate(operate_code, operate_card);
        }
        playerStatus.clean_status();
        // End 不同的处理器里面的前面几行判断代码是不一样的

        if (GameConstants.WIK_NULL == operate_code) {
            // 注意了，玩家在客户端点了过之后，玩家就已经相应了，需要记录一下，不然又是个坑
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        // 判断优先级，杠牌的那个人胡牌时优先级最高，点胡的时候，直接一次把所有能胡的牌都胡了
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
            int i = (_seat_index + p) % GameConstants.GAME_PLAYER;
            if (i == target_player) {
                target_p = GameConstants.GAME_PLAYER - p;
            }
        }
        for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
            int i = (_seat_index + p) % GameConstants.GAME_PLAYER;

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
        // 注意一些不同的地方，一些小细节的东西，整个handler里的代码，对逻辑数据的处理是相当混乱的，一不小心就会踩bug
        // 玩家状态的operate方法会更新是否已相应，那么在本方法的前面几行判断代码就要相应的做调整
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 清空所有玩家的动作和状态
        // 下面这几行代码也要注意使用场景，别轻易用，容易造成bug，注意是clean方法别乱用，加一层过滤就可以了
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (i == target_player)
                continue;
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
            table.operate_player_action(i, true);
        }

        // 注意下面这几行代码，有个大坑等你踩，如果你不注意的话
        // 方法体本身传入的operate_card和玩家状态的_operate_card会根据玩家在客户端点击的先后顺序，为0或者为实际操作的牌
        int tmp_operate_card = table._playerStatus[target_player]._operate_card;
        // if (operate_card == 0 && tmp_operate_card == 0) {
        // table.log_player_error(seat_index, "出牌，操作牌时出错");
        // return true;
        // }
        if (operate_card == 0 && tmp_operate_card != 0)
            operate_card = tmp_operate_card;

        // 记录哪些牌没人操作的，往废弃牌堆放
        int[] tmp_remove_cards = new int[_xuan_mei_count];
        int[] tmp_xuan_mei_cards = _gang_card_data.get_cards();
        int tmp_count = 0;
        boolean removed = false;
        for (int x = 0; x < _xuan_mei_count; x++) {
            if (table.get_real_card(tmp_xuan_mei_cards[x]) == operate_card && removed == false) {
                removed = true;
            } else {
                tmp_remove_cards[tmp_count++] = tmp_xuan_mei_cards[x];
            }
        }

        // 注意下面几个不同之处，‘吃’牌或者‘碰’牌的时候，在exe方法之前处理手牌；‘杠’牌的时候，进入handler的exe_gang方法才会处理手牌
        // 这个不同点，对有些处理器的一小部分代码会有影响，处理时要注意
        switch (target_action) {
        case GameConstants.WIK_LEFT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { operate_card + 1, operate_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            // 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
            table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            // 将选美翻出来的牌里面没人操作的牌往费牌堆放
            table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, GameConstants.DELAY_SEND_CARD_DELAY);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_GANG);
        }
            break;
        case GameConstants.WIK_RIGHT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { operate_card - 1, operate_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            // 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
            table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            // 将选美翻出来的牌里面没人操作的牌往费牌堆放
            table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, GameConstants.DELAY_SEND_CARD_DELAY);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_GANG);
        }
            break;
        case GameConstants.WIK_CENTER: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { operate_card - 1, operate_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            // 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
            table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            // 将选美翻出来的牌里面没人操作的牌往费牌堆放
            table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, GameConstants.DELAY_SEND_CARD_DELAY);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_GANG);
        }
            break;
        case GameConstants.WIK_PENG: // 碰牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { operate_card, operate_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            // 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
            table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            // 将选美翻出来的牌里面没人操作的牌往费牌堆放
            table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, GameConstants.DELAY_SEND_CARD_DELAY);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_GANG);
        }
            break;
        case GameConstants.WIK_GANG: // 杠牌操作
        {
            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            // 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
            table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            // 将选美翻出来的牌里面没人操作的牌往费牌堆放
            table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, GameConstants.DELAY_SEND_CARD_DELAY);

            // 是否有抢杠胡
            // 杠之后选美，如果是自己再次杠，就是碰杠或者暗杠，如果是其他人杠，就是明杠
            if (target_player == _seat_index) {
                // 如果手牌里有和operate_card相等的牌，杠就是暗杠，如果没有，就是碰杠
                int[] tmp_hand_cards = new int[GameConstants.MAX_COUNT];
                int tmp_hand_cards_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player],
                        tmp_hand_cards);
                boolean is_an_gang = false;
                for (int i = 0; i < tmp_hand_cards_count; i++) {
                    if (tmp_hand_cards[i] == operate_card) {
                        is_an_gang = true;
                        break;
                    }
                }
                if (is_an_gang) {
                    table.exe_gang(target_player, _seat_index, operate_card, target_action,
                            GameConstants.GANG_TYPE_AN_GANG, false, false);
                } else {
                    table.exe_gang(target_player, _seat_index, operate_card, target_action,
                            GameConstants.GANG_TYPE_ADD_GANG, false, false);
                }
            } else {
                table.exe_gang(target_player, _seat_index, operate_card, target_action,
                        GameConstants.GANG_TYPE_JIE_GANG, false, false);
            }
            return true;
        }
        case GameConstants.WIK_NULL: {
            // 用户切换
            table._current_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            // 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
            table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            // 将选美翻出来的牌里面没人操作的牌往费牌堆放
            table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, GameConstants.DELAY_SEND_CARD_DELAY);

            // 发牌
            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            // 玩家出牌之后，有人胡牌，记录牌桌、出牌人以及胡牌人的状态信息
            table.GRR._chi_hu_rights[seat_index].set_valid(true);

            table._cur_banker = seat_index;

            table.set_niao_card(seat_index);

            table._player_result.jie_pao_count[seat_index]++;
            table._player_result.dian_pao_count[_seat_index]++;

            table.GRR._chi_hu_rights[_seat_index].opr_or_xt(MJConstants_HuNan_XiangTan.CHR_FANG_PAO, false);

            int[] tmp_hu_cards = table._playerStatus[seat_index].get_hu_cards_of_xuan_mei();

            for (int h = 0; h < tmp_hu_cards.length; h++) {
                table.GRR._chi_hu_card[seat_index][h] = tmp_hu_cards[h];
            }

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            table.process_chi_hu_player_operate_xt_xuan_mei(seat_index, false);
            table.process_chi_hu_player_score(seat_index, _seat_index, operate_card,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU, false);

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        case GameConstants.WIK_ZI_MO: {
            // 点了胡牌之后，设置牌局和牌桌的一些状态值，计算分数并且立即结束本局游戏
            table.GRR._chi_hu_rights[_seat_index].set_valid(true);
            table._cur_banker = _seat_index;
            table.set_niao_card(_seat_index);
            table.GRR._chi_hu_card[_seat_index][0] = operate_card;
            table._player_result.zi_mo_count[_seat_index]++;

            // 将翻出来的牌从牌桌的正中央移除
            table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(),
                    GameConstants.INVALID_SEAT);

            int[] tmp_hu_cards = table._playerStatus[_seat_index].get_hu_cards_of_xuan_mei();

            for (int h = 0; h < tmp_hu_cards.length; h++) {
                table.GRR._chi_hu_card[_seat_index][h] = tmp_hu_cards[h];
            }

            // 客户端弹出来相应的动画效果，并处理手牌和落地的牌
            table.process_chi_hu_player_operate_xt_xuan_mei(seat_index, false);
            // 计算发牌时的自摸胡分数
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI, true);

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

        // 比吃碰断线重连多了一个在客户端牌桌正中央显示选美牌的效果
        table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, _xuan_mei_count,
                this._gang_card_data.get_cards(), GameConstants.INVALID_SEAT);

        return true;
    }
}
