package com.cai.game.mj.hunan.xiangtan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGangXuanMei_HuNan_XiangTan extends AbstractMJHandler<MJTable_HuNan_XiangTan> {

    protected int _seat_index;

    protected CardsData _gang_card_data;

    public MJHandlerGangXuanMei_HuNan_XiangTan() {
        _gang_card_data = new CardsData(MJConstants_HuNan_XiangTan.COUNT_OF_MEI_4);
    }

    public void reset_status(int seat_index) {
        _seat_index = seat_index;
        _gang_card_data.clean_cards();
    }

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table._playerStatus[i].chi_hu_round_valid(); // 可以胡了
        }

        // 选美，摸出来的牌，自己胡算杠上开花，别人胡算抢杠胡
        int xuan_mei_count = table.get_xuan_mei_count();

        table._send_card_count += xuan_mei_count;

        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        int tmp_xuan_mei_cards[] = new int[table.get_xuan_mei_count()];

        // 从剩余牌堆里顺序取出选美的牌
        table._logic.switch_to_cards_index(table._repertory_card, table._all_card_len - table.GRR._left_card_count,
                xuan_mei_count, cbCardIndexTemp);
        // 往_gang_card_data进行存储，断线重连时能用得到，下面的判断也可以使用
        table._logic.switch_to_cards_data(cbCardIndexTemp, tmp_xuan_mei_cards);

        // 处理鬼牌和存储杠牌数据
        for (int i = 0; i < tmp_xuan_mei_cards.length; i++) {
            if (table._logic.is_magic_card(tmp_xuan_mei_cards[i]))
                tmp_xuan_mei_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
            this._gang_card_data.add_card(tmp_xuan_mei_cards[i]);
        }

        table.GRR._left_card_count -= xuan_mei_count;

        // 显示选美的牌
        table.operate_out_card(_seat_index, table.get_xuan_mei_count(), this._gang_card_data.get_cards(),
                GameConstants.OUT_CARD_TYPE_LEFT, _seat_index);

        boolean can_win_myself = false;
        boolean can_win_others = false;

        for (int i = 0; i < GameConstants.MAX_INDEX; i++) { // 先判断自己能不能胡
            if (cbCardIndexTemp[i] == 0)
                continue;
            ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
            int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
                    table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
                    table._logic.switch_to_card_data(i), chr, MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI,
                    _seat_index); // 选美自摸算杠上开花
            // 结果判断
            if (action != 0) {
                can_win_myself = true;
                table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
                table._playerStatus[_seat_index].add_zi_mo(table._logic.switch_to_card_data(i), _seat_index); // 吃胡的组合
                break;
            }
        }

        if (!can_win_myself) {
            for (int j = 0; j < table.getTablePlayerNumber(); j++) { // 再判断其他人能不能胡
                if (j == _seat_index)
                    continue;

                for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
                    if (cbCardIndexTemp[i] == 0)
                        continue;

                    int card = table._logic.switch_to_card_data(i);

                    // 构造扑克
                    int tmp_card_index[] = new int[GameConstants.MAX_INDEX];
                    for (int x = 0; x < GameConstants.MAX_INDEX; x++) {
                        tmp_card_index[x] = table.GRR._cards_index[j][x];
                    }

                    // 插入扑克
                    if (card != GameConstants.INVALID_VALUE) {
                        tmp_card_index[table._logic.switch_to_card_index(card)]++;
                    }

                    // 分析扑克
                    List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
                    boolean bValue = table._logic.analyse_card_henan_zhou_kou(tmp_card_index, table.GRR._weave_items[j],
                            table.GRR._weave_count[j], analyseItemArray, false);
                    boolean is_peng_peng_hu = false;

                    if (bValue) {
                        for (AnalyseItem analyseItem : analyseItemArray) {
                            if (table._logic.is_pengpeng_hu(analyseItem)) { // 碰碰胡
                                is_peng_peng_hu = true;
                            }
                        }
                    }

                    if (is_peng_peng_hu || table.GRR._weave_count[j] == 0) { // 是碰碰胡或者没吃牌碰牌才能接炮
                        ChiHuRight chr = table.GRR._chi_hu_rights[j];
                        int action = table.analyse_chi_hu_card(table.GRR._cards_index[j], table.GRR._weave_items[j],
                                table.GRR._weave_count[j], table._logic.switch_to_card_data(i), chr,
                                MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU, j); // 选美点炮算抢杠胡
                        // 结果判断
                        if (action != 0) {
                            can_win_others = true;
                            table._playerStatus[j].add_action(GameConstants.WIK_CHI_HU);
                            table._playerStatus[j].add_chi_hu(table._logic.switch_to_card_data(i), j); // 吃胡的组合
                            break;
                        }
                    }

                }
            }
        }

        if (!can_win_myself && !can_win_others) {
            // 从后面发一张牌给下家
            table.exe_dispatch_card(table.get_banker_next_seat(_seat_index), GameConstants.INVALID_VALUE, 0);
        } else {
            // 等待别人操作这这些牌
            PlayerStatus playerStatus = null;

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (playerStatus.has_chi_hu()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    @Override
    public boolean handler_operate_card(MJTable_HuNan_XiangTan table, int seat_index, int operate_code,
            int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 效验操作
        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_error("没有这个操作");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("不是当前玩家操作");
            return false;
        }

        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }

        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            // table.process_chi_hu_player_operate(seat_index, operate_card, true);
        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
            }
        }

        // 吃胡等待，不能一炮多响，只能优先级高的人胡
        /**
         * for (int i = 0; i < GameConstants.GAME_PLAYER; i++) { if
         * ((table._playerStatus[i].is_respone() == false) &&
         * (table._playerStatus[i].has_chi_hu())) return false; }
         **/

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;

        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % GameConstants.GAME_PLAYER;
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

        // 变量定义
        // int target_card = table._playerStatus[target_player]._operate_card;

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }

        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_NULL: {
            // 加到牌堆 没有人要
            table.exe_add_discard(this._seat_index, table.get_xuan_mei_count(), _gang_card_data.get_cards(), false, 0);

            // 用户切换
            table._current_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

            // 发牌
            table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_ZI_MO: // 自摸
        {
            table.GRR._chi_hu_rights[_seat_index].set_valid(true);

            table._cur_banker = _seat_index;

            table.set_niao_card(_seat_index); // 结束后设置鸟牌

            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            table.process_chi_hu_player_operate(_seat_index, operate_card, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI, true);

            // 记录
            table._player_result.zi_mo_count[_seat_index]++;

            // 结束
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);

            table._cur_banker = seat_index;
            // 结束后设置鸟牌
            table.set_niao_card(seat_index);

            table.process_chi_hu_player_operate(seat_index, operate_card, true);
            table.process_chi_hu_player_score(seat_index, _seat_index, operate_card,
                    MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU, false);

            table.GRR._chi_hu_card[seat_index][0] = operate_card;

            // 记录
            table._player_result.jie_pao_count[seat_index]++;
            table._player_result.dian_pao_count[_seat_index]++;

            table.GRR._chi_hu_rights[_seat_index].opr_or(MJConstants_HuNan_XiangTan.CHR_FANG_PAO);

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
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

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                new long[] { GameConstants.WIK_GANG }, 1, seat_index);

        // 出牌
        table.operate_out_card(_seat_index, table.get_xuan_mei_count(), this._gang_card_data.get_cards(),
                GameConstants.OUT_CARD_TYPE_LEFT, seat_index);

        table.send_response_to_player(seat_index, roomResponse);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
