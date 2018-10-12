package com.cai.game.mj.hunan.jingdiancs;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.MJHandlerHaiDi;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerHaiDi_JingDian_CS extends MJHandlerHaiDi<Table_JingDian_CS> {
    private static Logger logger = Logger.getLogger(HandlerHaiDi_JingDian_CS.class);

    @Override
    public void exe(Table_JingDian_CS table) {
        PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        curPlayerStatus.reset();

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].chi_hu_round_valid();
        }

        boolean isTing = table.is_cs_ting_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
                table.GRR._weave_count[_seat_index], _seat_index);

        if (isTing) {
            curPlayerStatus.add_action(GameConstants.WIK_YAO_HAI_DI);
            curPlayerStatus.add_yao_hai_di();
            table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
            table.operate_player_action(_seat_index, false);
        } else {
            _seat_index = (_seat_index + 1) % table.getTablePlayerNumber();
            if (_seat_index == _start_index) {
                table._cur_banker = _start_index;

                table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
                return;
            }
            table.exe_hai_di(_start_index, _seat_index);
        }
    }

    @Override
    public boolean handler_operate_card(Table_JingDian_CS table, int seat_index, int operate_code, int operate_card) {
        if (seat_index != _seat_index) {
            logger.error("[海底],操作失败," + seat_index + "不是当前操作玩家");
            return false;
        }
        
        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table._playerStatus[seat_index].clean_action(GameConstants.WIK_YAO_HAI_DI);

            _seat_index = (_seat_index + 1) % table.getTablePlayerNumber();
            if (_seat_index == _start_index) {
                table._cur_banker = _start_index;

                table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

                return true;
            }

            table.exe_hai_di(_start_index, _seat_index);
        } else {
            table.exe_yao_hai_di(_seat_index);
        }

        return true;
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

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }
}
