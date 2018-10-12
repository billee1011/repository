package com.cai.game.mj.handler.lxcs;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.XiaoHuRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerXiaoHu_CSLX extends MJHandler {
    private static Logger logger = Logger.getLogger(MJHandlerXiaoHu_CSLX.class);

    private int _current_player = GameConstants.INVALID_SEAT;

    public void reset_status(int seat_index) {
        _current_player = seat_index;
    }

    @Override
    public void exe(MJTable table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            PlayerStatus playerStatus = table._playerStatus[i];
            if (playerStatus._action_count > 0) {
                table.operate_player_action(i, false);
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
    public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {

        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_xiao_hu() == false) {
            logger.error("操作失败,玩家" + seat_index + "没有小胡");
            return false;
        }

        // 玩家操作
        playerStatus.operate(operate_code, 0);

        table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
                1);

        if (operate_code == GameConstants.WIK_NULL) {
            table.GRR._start_hu_right[seat_index].set_empty();
        } else {
            ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

            // 小胡生效
            start_hu_right.set_valid(true);

            // int lStartHuScore = 0;
            //
            // int wFanShu =
            // table._logic.get_chi_hu_action_rank_cs(table.GRR._start_hu_right[seat_index]);
            //
            // lStartHuScore = wFanShu * MJGameConstants.CELL_SCORE;

            // table.GRR._start_hu_score[seat_index] = lStartHuScore * 3;//赢3个人的分数

            // 算分
            // for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
            // if (i == seat_index)
            // continue;
            // table.GRR._lost_fan_shu[i][seat_index] = wFanShu;//
            // int s=lStartHuScore;
            // //庄闲
            // if(table.is_zhuang_xian()){
            // if((table.GRR._banker_player == seat_index)||(table.GRR._banker_player ==
            // i)){
            // s+=1;
            // }
            // }
            // table.GRR._start_hu_score[i] -= s;//输的番薯
            // table.GRR._start_hu_score[seat_index]+=s;
            // }

            show_xiao_hu(table, seat_index);

        }

        // 还有没有没操作的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            playerStatus = table._playerStatus[i];
            if (playerStatus.has_xiao_hu() && playerStatus.is_respone() == false) {
                return false;
            }
        }

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            // table._playerStatus[i].clean_status();
        }

        boolean has_xiao_hu = false;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            // 清除动作
            if (table.GRR._start_hu_right[i].is_valid()) {
                has_xiao_hu = true;
            }
        }
        if (has_xiao_hu == false) {
            table.runnable_xiao_hu(_current_player, false);
        } else {
            // 延迟调度小胡结束
            GameSchedule.put(new XiaoHuRunnable(table.getRoom_id(), _current_player, false),
                    GameConstants.XIAO_HU_DELAY, TimeUnit.SECONDS);

        }

        return true;
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
        tableResponse.setCurrentPlayer(_current_player);
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
                weaveItem_item.setProvidePlayer(
                        table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
                weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
                weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
                weaveItem_array.addWeaveItem(weaveItem_item);
            }
            tableResponse.addWeaveItemArray(weaveItem_array);

            //
            tableResponse.addWinnerOrder(0);

            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

        }

        // 数据
        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        // TODO: 出任意一张牌时，能胡哪些牌 -- Begin
        int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

        if ((out_ting_count > 0) && (seat_index == _current_player)) {
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
        // TODO: 出任意一张牌时，能胡哪些牌 -- End

        /*
         * for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
         * tableResponse.addCardsData(hand_cards[i]); }
         * 
         * roomResponse.setTable(tableResponse);
         * 
         * table.send_response_to_player(seat_index, roomResponse);
         */

        // 效果
        // for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
        // if(){
        //
        //
        // }
        // }

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }

    private void show_shai_zi(MJTable table, int seat_index) {
        int num1 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
        int num2 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

        table._player_result.shaizi[seat_index][0] = num1;
        table._player_result.shaizi[seat_index][1] = num2;
        // 临湘长沙麻将 要加骰子效果
        table.operate_shai_zi_effect(num1, num2, 1450, 500);
    }

    private void show_xiao_hu(MJTable table, int seat_index) {
        ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

        // 效果
        table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, start_hu_right.type_count,
                start_hu_right.type_list, start_hu_right.type_count, GameConstants.INVALID_SEAT);
        show_shai_zi(table, seat_index);

        // 构造扑克
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = table.GRR._cards_index[seat_index][i];
        }

        int hand_card_indexs[] = new int[GameConstants.MAX_INDEX];
        int show_card_indexs[] = new int[GameConstants.MAX_INDEX];

        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            hand_card_indexs[i] = table.GRR._cards_index[seat_index][i];
        }

        if (start_hu_right._show_all) {
            for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
                show_card_indexs[i] = table.GRR._cards_index[seat_index][i];
                hand_card_indexs[i] = 0;
            }
        } else {
            if (start_hu_right._index_da_si_xi != GameConstants.MAX_INDEX) {
                hand_card_indexs[start_hu_right._index_da_si_xi] = 0;
                show_card_indexs[start_hu_right._index_da_si_xi] = 4;
            }
            if ((start_hu_right._index_liul_liu_shun_1 != GameConstants.MAX_INDEX)
                    && (start_hu_right._index_liul_liu_shun_2 != GameConstants.MAX_INDEX)) {
                if (start_hu_right._index_liul_liu_shun_1 != start_hu_right._index_da_si_xi) {
                    hand_card_indexs[start_hu_right._index_liul_liu_shun_1] = hand_card_indexs[start_hu_right._index_liul_liu_shun_1]
                            - 3;
                    show_card_indexs[start_hu_right._index_liul_liu_shun_1] = 3;
                }
                if (start_hu_right._index_liul_liu_shun_2 != start_hu_right._index_da_si_xi) {
                    hand_card_indexs[start_hu_right._index_liul_liu_shun_2] = hand_card_indexs[start_hu_right._index_liul_liu_shun_2]
                            - 3;
                    show_card_indexs[start_hu_right._index_liul_liu_shun_2] = 3;
                }
            }
        }

        int cards[] = new int[GameConstants.MAX_COUNT];

        // 刷新自己手牌
        int hand_card_count = table._logic.switch_to_cards_data(hand_card_indexs, cards);
        table.operate_player_cards(seat_index, hand_card_count, cards, 0, null);

        // 显示 小胡牌
        hand_card_count = table._logic.switch_to_cards_data(show_card_indexs, cards);
        table.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards,
                GameConstants.INVALID_SEAT);
    }

}
