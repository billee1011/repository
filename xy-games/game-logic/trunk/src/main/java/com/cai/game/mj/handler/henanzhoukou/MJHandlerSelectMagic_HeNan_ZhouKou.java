package com.cai.game.mj.handler.henanzhoukou;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerSelectMagic_HeNan_ZhouKou extends AbstractMJHandler<MJTable> {
    protected int _da_dian_card;
    protected int _banker;

    public void reset_status(int banker) {
        _banker = banker;
    }

    @Override
    public void exe(MJTable table) {
        if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
            table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
            
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                int[] hand_cards = new int[GameConstants.MAX_COUNT];
                int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
                table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
            }
        } else if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
            // 从牌堆拿出一张牌，并显示在牌桌的正中央
            table._send_card_count++;
            _da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
            table.GRR._left_card_count--;
            
            if (table.DEBUG_CARDS_MODE)
                _da_dian_card = 0x16;
            
            // 将翻出来的牌显示在牌桌的正中央
            table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
                    GameConstants.INVALID_SEAT);
            
            int card_next = 0;
            
            int cur_data = table._logic.get_card_value(_da_dian_card);
            int cur_color = table._logic.get_card_color(_da_dian_card);
            
            if (cur_color == 3) {
                if (cur_data == 7) {
                    card_next = _da_dian_card - 6;
                } else {
                    card_next = _da_dian_card + 1;
                }
            } else {
                if (cur_data == 9) {
                    card_next = _da_dian_card - 8;
                } else {
                    card_next = _da_dian_card + 1;
                }
            }
            
            // 添加鬼
            table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
            table.GRR._especial_card_count = 1;
            table.GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_HUN;
            
            // 处理每个玩家手上的牌，如果有王牌，处理一下
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                int[] hand_cards = new int[GameConstants.MAX_COUNT];
                int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
                for (int j = 0; j < hand_card_count; j++) {
                    if (table._logic.is_magic_card(hand_cards[j])) {
                        hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                    }
                }
                // 玩家客户端刷新一下手牌
                table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
            }
            
            GameSchedule.put(new Runnable() {
                @Override
                public void run() {
                    // 将翻出来的牌从牌桌的正中央移除
                    table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
                }
            }, 2000, TimeUnit.MILLISECONDS);
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
        tableResponse.setCurrentPlayer(_banker);
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
                if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
                    if (i != seat_index) {
                        int_array.addItem(GameConstants.BLACK_CARD);

                    } else {
                        int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);
                    }
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
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
            for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
                if (table._logic.is_magic_card(hand_cards[i])) {
                    hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                }
                
                tableResponse.addCardsData(hand_cards[i]);
            }
        } else {
            for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
                tableResponse.addCardsData(hand_cards[i]);
            }
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);
        
        if(_banker == seat_index){
            if (table._playerStatus[_banker].has_action_by_code(GameConstants.WIK_BAO_TING)) {
                //如果可以报听,刷新自己的手牌
                int ting_count = table._playerStatus[_banker]._hu_out_card_count  ;
                if(ting_count>0){
                    
                    //刷新手牌
                    //int cards[] = new int[MJGameConstants.MAX_COUNT];
                    //刷新自己手牌
                    //hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
                    
                    for(int i = 0; i < hand_card_count; i++){
                        for(int j = 0; j<ting_count;j++){
                            if(hand_cards[i] == table._playerStatus[_banker]._hu_out_card_ting[j]){
                                hand_cards[i] +=GameConstants.CARD_ESPECIAL_TYPE_TING;
                            }
                        }
                    }
                    
                    table.operate_player_cards_with_ting(_banker, hand_card_count, hand_cards, 0, null);
                }
            }
        }else{
            int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
            int ting_count = table._playerStatus[seat_index]._hu_card_count;
            
            if(ting_count>0){
                if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
                    for (int x = 0; x < ting_count; x++) {
                        if (table._logic.is_magic_card(ting_cards[x])) {
                            ting_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
                        }
                    }
                }
                table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
            }
        }

        // 安阳麻将必须还章报听

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

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
