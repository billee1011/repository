package com.cai.game.mj.hunan.new_xiang_tan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.MJConstants_HuNan_XiangTan;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.XiangTanSelectMagicCardRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerSelectMagicCard_HuNan_XiangTan extends AbstractMJHandler<MJTable_HuNan_XiangTan> {

    protected int _da_dian_card;
    protected int _banker;

    public void reset_status(int banker) {
        _banker = banker;
    }

    @Override
    public void exe(MJTable_HuNan_XiangTan table) {
        // 从牌堆拿出一张牌，并显示在牌桌的正中央
        table._send_card_count++;
        _da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        table.GRR._left_card_count--;

        if (table.DEBUG_CARDS_MODE)
            _da_dian_card = 0x19;

        // 将翻出来的牌显示在牌桌的正中央
        table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
                GameConstants.INVALID_SEAT);

        int card_pre = 0;
        int card_next = 0;

        int cur_data = table._logic.get_card_value(_da_dian_card);

        if (cur_data == 1) {
            card_next = _da_dian_card + 1;
            card_pre = _da_dian_card + 8;
        } else if (cur_data == 9) {
            card_next = _da_dian_card - 8;
            card_pre = _da_dian_card - 1;
        } else {
            card_next = _da_dian_card + 1;
            card_pre = _da_dian_card - 1;
        }

        // 添加鬼
        if (GameDescUtil.has_rule(table._game_rule_index, MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_SHANG_WANG)) { // 上鬼
            table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_pre));
            table.GRR._especial_card_count = 1;
            table.GRR._especial_show_cards[0] = card_pre + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        } else if (GameDescUtil.has_rule(table._game_rule_index, MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XIA_WANG)) { // 下鬼
            table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
            table.GRR._especial_card_count = 1;
            table.GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        } else { // 上下鬼
            table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_pre));
            table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
            table.GRR._especial_card_count = 2;
            table.GRR._especial_show_cards[0] = card_pre + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
            table.GRR._especial_show_cards[1] = card_next + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
        }

        // 处理每个玩家手上的牌，如果有王牌，处理一下
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            int[] hand_cards = new int[GameConstants.MAX_COUNT];
            int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
                }
            }
            // 玩家客户端刷新一下手牌
            table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
        }

        GameSchedule.put(new XiangTanSelectMagicCardRunnable(table.getRoom_id(), _banker), 2, TimeUnit.SECONDS);
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
        tableResponse.setCurrentPlayer(_banker);
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

        // 比吃碰的断线重连多了一个客户端显示
        table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, seat_index);

        return true;
    }
}
