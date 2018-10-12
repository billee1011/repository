package com.cai.game.mj.handler.yiyang.szg;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChuPaiBaoTing_YiYang_SZG extends AbstractMJHandler<MJTable_YiYang_SZG> {
    public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
    public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
    public int _type;

    public void reset_status(int seat_index, int card, int type) {
        _out_card_player = seat_index;
        _out_card_data = card;
        _type = type;
    }

    @Override
    public void exe(MJTable_YiYang_SZG table) {
        table._playerStatus[_out_card_player].add_action(GameConstants.WIK_BAO_TING);
        table._playerStatus[_out_card_player].add_bao_ting(GameConstants.INVALID_VALUE, GameConstants.WIK_BAO_TING,
                _out_card_player);

        table.change_player_status(_out_card_player, GameConstants.Player_Status_OPR_CARD);
        table.operate_player_action(_out_card_player, false);
    }

    @Override
    public boolean handler_operate_card(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return false;
        }

        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        if (operate_code == GameConstants.WIK_BAO_TING) {
            // TODO 报听之后，相当于自动托管，摸什么牌打什么牌。直到胡牌
            table.is_bao_ting[seat_index] = true;

            table.operate_player_info();
        }

        // 处理完报听之后，返回到出牌处理器。_type设定为一个特殊值
        table.exe_out_card(_out_card_player, _out_card_data, GameConstants.WIK_BAO_TING);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_YiYang_SZG table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(_out_card_player);
        tableResponse.setCellScore(0);

        tableResponse.setActionCard(0);

        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(table.is_bao_ting[i]);

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
                weaveItem_item.setProvidePlayer(
                        table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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
        @SuppressWarnings("unused")
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

        for (int i = 0; i < hand_card_count; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        return true;
    }
}
