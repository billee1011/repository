package com.cai.game.mj.hunan.jingdiancs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_JingDian_CS;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.BaseFuture;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerXiaoHu_JingDian_CS extends AbstractMJHandler<Table_JingDian_CS> {
    private static Logger logger = Logger.getLogger(HandlerXiaoHu_JingDian_CS.class);

    private int _current_player = GameConstants.INVALID_SEAT;

    public void reset_status(int seat_index) {
        _current_player = seat_index;
    }

    @Override
    public void exe(Table_JingDian_CS table) {
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            PlayerStatus playerStatus = table._playerStatus[i];
            if (playerStatus._action_count > 0) {
            	table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                table.operate_player_action(i, false);
            }
        }
    }

    @Override
    public boolean handler_operate_card(Table_JingDian_CS table, int seat_index, int operate_code, int operate_card) {

        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_xiao_hu() == false) {
            logger.error("操作失败,玩家" + seat_index + "没有小胡");
            return false;
        }

        playerStatus.operate(operate_code, 0);

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);

            table.GRR._start_hu_right[seat_index].set_empty();
        } else {
            ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

            start_hu_right.set_valid(true);

            show_xiao_hu(table, seat_index);

            table.has_xiao_hu[seat_index] = true;
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            playerStatus = table._playerStatus[i];
            if (playerStatus.has_xiao_hu() && playerStatus.is_respone() == false) {
                return false;
            }
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
        }

        boolean has_xiao_hu = false;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (table.GRR._start_hu_right[i].is_valid()) {
                has_xiao_hu = true;
            }
        }
        // WalkerGeek 调用新小胡Runnable runnable_xiao_hu XiaoHuRunnable
        if (has_xiao_hu == false) {
            table.runnable_xiao_hu(_current_player, true);
        } else {
            GameSchedule.put(new XiaoHuRunnable(table.getRoom_id(), _current_player, true), 4, TimeUnit.SECONDS);
        }

        return true;
    }

    private void show_xiao_hu(Table_JingDian_CS table, int seat_index) {
        ChiHuRight start_hu_right = table.GRR._start_hu_right[seat_index];

        table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, start_hu_right.type_count,
                start_hu_right.type_list, start_hu_right.type_count, GameConstants.INVALID_SEAT);

        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = table.GRR._cards_index[seat_index][i];
        }

        int hand_card_indexs[] = new int[GameConstants.MAX_INDEX];
        int show_card_indexs[] = new int[GameConstants.MAX_INDEX];
        int init_card_count[] = new int[GameConstants.MAX_INDEX];

        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            hand_card_indexs[i] = table.GRR._cards_index[seat_index][i];
            init_card_count[i] = table.GRR._cards_index[seat_index][i];
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

                // table.operated_zt_si_xi[seat_index] = true;
            }
            if ((start_hu_right._index_liul_liu_shun_1 != GameConstants.MAX_INDEX)
                    && (start_hu_right._index_liul_liu_shun_2 != GameConstants.MAX_INDEX)) {
                int count1 = hand_card_indexs[start_hu_right._index_liul_liu_shun_1];
                int count2 = hand_card_indexs[start_hu_right._index_liul_liu_shun_2];
                hand_card_indexs[start_hu_right._index_liul_liu_shun_1] = count1 - 3 >= 0 ? count1 - 3 : 0;
                hand_card_indexs[start_hu_right._index_liul_liu_shun_2] = count2 - 3 >= 0 ? count2 - 3 : 0;

                int count3 = show_card_indexs[start_hu_right._index_liul_liu_shun_1];
                int count4 = show_card_indexs[start_hu_right._index_liul_liu_shun_2];
                show_card_indexs[start_hu_right._index_liul_liu_shun_1] = count3
                        + 3 >= init_card_count[start_hu_right._index_liul_liu_shun_1]
                                ? init_card_count[start_hu_right._index_liul_liu_shun_1]
                                : count3 + 3;
                show_card_indexs[start_hu_right._index_liul_liu_shun_2] = count4
                        + 3 >= init_card_count[start_hu_right._index_liul_liu_shun_2]
                                ? init_card_count[start_hu_right._index_liul_liu_shun_2]
                                : count4 + 3;
            }
            if (start_hu_right._index_jie_jie_gao != GameConstants.MAX_INDEX) {
                int count1 = hand_card_indexs[start_hu_right._index_jie_jie_gao];
                int count2 = hand_card_indexs[start_hu_right._index_jie_jie_gao + 1];
                int count3 = hand_card_indexs[start_hu_right._index_jie_jie_gao + 2];
                hand_card_indexs[start_hu_right._index_jie_jie_gao] = count1 - 2 >= 0 ? count1 - 2 : 0;
                hand_card_indexs[start_hu_right._index_jie_jie_gao + 1] = count2 - 2 >= 0 ? count2 - 2 : 0;
                hand_card_indexs[start_hu_right._index_jie_jie_gao + 2] = count3 - 2 >= 0 ? count3 - 2 : 0;

                int count4 = show_card_indexs[start_hu_right._index_jie_jie_gao];
                int count5 = show_card_indexs[start_hu_right._index_jie_jie_gao + 1];
                int count6 = show_card_indexs[start_hu_right._index_jie_jie_gao + 2];
                show_card_indexs[start_hu_right._index_jie_jie_gao] = count4
                        + 2 >= init_card_count[start_hu_right._index_jie_jie_gao]
                                ? init_card_count[start_hu_right._index_jie_jie_gao]
                                : count4 + 2;
                show_card_indexs[start_hu_right._index_jie_jie_gao
                        + 1] = count5 + 2 >= init_card_count[start_hu_right._index_jie_jie_gao + 1]
                                ? init_card_count[start_hu_right._index_jie_jie_gao + 1]
                                : count5 + 2;
                show_card_indexs[start_hu_right._index_jie_jie_gao
                        + 2] = count6 + 2 >= init_card_count[start_hu_right._index_jie_jie_gao + 2]
                                ? init_card_count[start_hu_right._index_jie_jie_gao + 2]
                                : count6 + 2;
            }
            if (start_hu_right._index_san_tong != GameConstants.MAX_INDEX) {
                int count1 = hand_card_indexs[start_hu_right._index_san_tong];
                int count2 = hand_card_indexs[start_hu_right._index_san_tong + 9];
                int count3 = hand_card_indexs[start_hu_right._index_san_tong + 18];
                hand_card_indexs[start_hu_right._index_san_tong] = count1 - 2 >= 0 ? count1 - 2 : 0;
                hand_card_indexs[start_hu_right._index_san_tong + 9] = count2 - 2 >= 0 ? count2 - 2 : 0;
                hand_card_indexs[start_hu_right._index_san_tong + 18] = count3 - 2 >= 0 ? count3 - 2 : 0;

                int count4 = show_card_indexs[start_hu_right._index_san_tong];
                int count5 = show_card_indexs[start_hu_right._index_san_tong + 9];
                int count6 = show_card_indexs[start_hu_right._index_san_tong + 18];
                show_card_indexs[start_hu_right._index_san_tong] = count4
                        + 2 >= init_card_count[start_hu_right._index_san_tong]
                                ? init_card_count[start_hu_right._index_san_tong]
                                : count4 + 2;
                show_card_indexs[start_hu_right._index_san_tong
                        + 9] = count5 + 2 >= init_card_count[start_hu_right._index_san_tong + 9]
                                ? init_card_count[start_hu_right._index_san_tong + 9]
                                : count5 + 2;
                show_card_indexs[start_hu_right._index_san_tong
                        + 18] = count6 + 2 >= init_card_count[start_hu_right._index_san_tong + 18]
                                ? init_card_count[start_hu_right._index_san_tong + 18]
                                : count6 + 2;
            }

            if (!start_hu_right.opr_and_long(Constants_JingDian_CS.CHR_JIN_TONG_YU_NV).is_empty()) {
                int index1 = table._logic.switch_to_card_index(0x12);
                int index2 = table._logic.switch_to_card_index(0x22);

                int count1 = hand_card_indexs[index1];
                int count2 = hand_card_indexs[index2];
                hand_card_indexs[index1] = count1 - 2 >= 0 ? count1 - 2 : 0;
                hand_card_indexs[index2] = count2 - 2 >= 0 ? count2 - 2 : 0;

                int count3 = show_card_indexs[index1];
                int count4 = show_card_indexs[index2];
                show_card_indexs[index1] = count3 + 2 >= init_card_count[index1] ? init_card_count[index1] : count3 + 2;
                show_card_indexs[index2] = count4 + 2 >= init_card_count[index2] ? init_card_count[index2] : count4 + 2;
            }

            if (!start_hu_right.opr_and_long(Constants_JingDian_CS.CHR_HOU_BA_LUN).is_empty()) {
                int index1 = table._logic.switch_to_card_index(0x28);

                int count1 = hand_card_indexs[index1];
                hand_card_indexs[index1] = count1 - 3 >= 0 ? count1 - 3 : 0;

                int count2 = show_card_indexs[index1];
                show_card_indexs[index1] = count2 + 3 >= init_card_count[index1] ? init_card_count[index1] : count2 + 3;
            }

            if (!start_hu_right.opr_and_long(Constants_JingDian_CS.CHR_MEI_HUA_SAN_NONG).is_empty()) {
                int index1 = table._logic.switch_to_card_index(0x25);

                int count1 = hand_card_indexs[index1];
                hand_card_indexs[index1] = count1 - 3 >= 0 ? count1 - 3 : 0;

                int count2 = show_card_indexs[index1];
                show_card_indexs[index1] = count2 + 3 >= init_card_count[index1] ? init_card_count[index1] : count2 + 3;
            }
        }

        int cards[] = new int[GameConstants.MAX_COUNT];

        int hand_card_count = table._logic.switch_to_cards_data(hand_card_indexs, cards);
        table.operate_player_cards(seat_index, hand_card_count, cards, 0, null);

        hand_card_count = table._logic.switch_to_cards_data(show_card_indexs, cards);
        table.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards,
                GameConstants.INVALID_SEAT);

        // TODO 小胡及时算分
        int wFanShu = table.get_chi_hu_action_rank_cs(table.GRR._start_hu_right[seat_index]);
        int lStartHuScore = wFanShu * GameConstants.CELL_SCORE;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            if (p == seat_index)
                continue;

            int s = lStartHuScore;

            if (table.has_rule(Constants_JingDian_CS.GAME_RULE_ZHUANG_XIAN)) {
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
    }

    @Override
    public boolean handler_player_be_in_room(Table_JingDian_CS table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(_current_player);
        tableResponse.setCellScore(0);

        tableResponse.setActionCard(0);

        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);

            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                int_array.addItem(table.GRR._discard_cards[i][j]);
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

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }

    class XiaoHuRunnable extends BaseFuture {
        private int room_id;
        private int seat_index;
        private boolean is_dispatch;

        public XiaoHuRunnable(int room_id, int seat_index, boolean is_dispatch) {
            super(room_id);
            this.room_id = room_id;
            this.seat_index = seat_index;
            this.is_dispatch = is_dispatch;
        }

        @Override
        public void execute() {
            try {
                AbstractMJTable table = (AbstractMJTable) PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

                if (table == null) {
                    return;
                }

                ReentrantLock roomLock = table.getRoomLock();

                try {
                    roomLock.lock();
                    table.runnable_xiao_hu(seat_index, is_dispatch);
                } finally {
                    roomLock.unlock();
                }
            } catch (Exception e) {
                logger.error("error" + room_id, e);
            }
        }
    }
}
