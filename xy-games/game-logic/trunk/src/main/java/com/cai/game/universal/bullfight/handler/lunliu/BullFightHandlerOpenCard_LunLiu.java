package com.cai.game.universal.bullfight.handler.lunliu;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.universal.bullfight.BullFightTable;
import com.cai.game.universal.bullfight.handler.BullFightHandlerOpenCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.TableResponseOX;
import protobuf.clazz.Protocol.Timer_OX;

public class BullFightHandlerOpenCard_LunLiu extends BullFightHandlerOpenCard {

    @Override
    public void exe(BullFightTable table) {
    }

    @Override
    public boolean handler_open_cards(BullFightTable table, int seat_index, boolean open_flag) {
        if (_game_status != GameConstants.GS_OX_OPEN_CARD) {
            table.log_error("游戏状态不对 " + _game_status + "用户开牌 :" + GameConstants.GS_OX_OPEN_CARD);
            return false;
        }

        if (table._open_card[seat_index] == true) {
            table.log_error("你已经开牌操作了 ");
            return false;
        }

        if (table._player_status[seat_index] == false) {
            table.log_error("不能参与 这局游戏" + seat_index);
            return false;
        }

        if (open_flag == true)
            table._open_card[seat_index] = open_flag;
        else {
            table.log_error("open_flag 数据错误");
            return true;
        }

        table._logic.get_ox_card(table.GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT,
                table._game_rule_index, table._game_type_index);
        table._card_type_ox[seat_index] = table._logic.get_card_type(table.GRR._cards_data[seat_index],
                GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);

        if (!table.is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
            table._card_type_ox[seat_index] = table._logic.switch_ox(table._card_type_ox[seat_index]);
        }

        table.open_card_ox(seat_index);

        boolean flag = true;
        for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
            if (table._player_status[i] == true) {
                if (table._open_card[i] == false)
                    flag = false;
            }
        }

        if (flag == true) {
            table.process_ox_calulate_end();
            table.process_chi_calulate_score_ox();

            table.countChiHuTimes(seat_index, true);

            for (int i = 1; i <= GameConstants.GAME_PLAYER_OX; i++) {
                table._next_banker = (table._cur_banker + i) % GameConstants.GAME_PLAYER_OX;

                if (table._next_banker == table._cur_banker)
                    continue;

                if (table._player_status[table._next_banker] == true)
                    break;
            }

            int delay = GameConstants.GAME_FINISH_DELAY_FLS;
            if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
                delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
            }

            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
                    delay, TimeUnit.SECONDS);
        }

        return false;
    }

    @Override
    public boolean handler_player_be_in_room(BullFightTable table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);

        int times = 1;
        tableResponse.setCellScore(1);
        tableResponse.setSceneInfo(table._game_status);
        tableResponse.setPlayerStatus(table._player_status[seat_index]);
        tableResponse.setBankerPlayer(table._cur_banker);

        if (table.istrustee[seat_index]) {
            table.istrustee[seat_index] = false;
            if (table._trustee_schedule[seat_index] != null) {
                table._trustee_schedule[seat_index].cancel(false);
                table._trustee_schedule[seat_index] = null;
            }
            table._trustee_type[seat_index] = 0;
        }

        for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
            tableResponse.addTrustee(table.istrustee[i]);
        }

        tableResponse.setTrusteeType(table._trustee_type[seat_index]);
        SendCard.Builder send_card = SendCard.newBuilder();

        for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
            Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

            if (table._player_status[k] != true) {
                for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                    cards.addItem(GameConstants.INVALID_CARD);
                }
            } else {
                if (k == seat_index) {
                    for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                        cards.addItem(table.GRR._cards_data[k][j]);
                    }
                } else if (table._open_card[k] == true) {
                    for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                        cards.addItem(table.GRR._cards_data[k][j]);
                    }

                    if (table.is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
                        if (GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_FAN_BEN))
                            times = table._logic.get_times_mul(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);

                        if (GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_PING_BEN))
                            times = table._logic.get_times_ping(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);
                    } else {
                        if (table.has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
                            times = table._logic.get_times_two(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);

                        if (table.has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
                            times = table._logic.get_times_one(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);
                    }
                } else {
                    for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                        cards.addItem(GameConstants.BLACK_CARD);
                    }
                }
            }

            tableResponse.addGameTime(times);
            tableResponse.addOpenCard(table._open_card[k]);
            tableResponse.addCardType(table._card_type_ox[k]);
            tableResponse.addCardsData(k, cards);
            tableResponse.addAddJetter(table._add_Jetton[k]);
        }

        roomResponse_ox.setSendCard(send_card);

        int display_time = table._cur_operate_time
                - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
        if (display_time > 0) {
            Timer_OX.Builder timer = Timer_OX.newBuilder();
            timer.setDisplayTime(display_time);
            roomResponse_ox.setDisplayTime(timer);
        }

        roomResponse_ox.setTableResponseOx(tableResponse);
        roomResponse.setRoomResponseOx(roomResponse_ox);

        table.send_response_to_player(seat_index, roomResponse);

        return true;
    }

    @Override
    public boolean handler_observer_be_in_room(BullFightTable table, Player player) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);

        int times = 1;
        tableResponse.setCellScore(1);
        tableResponse.setSceneInfo(table._game_status);
        tableResponse.setBankerPlayer(table._cur_banker);

        SendCard.Builder send_card = SendCard.newBuilder();

        for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
            Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

            if (table._player_status[k] != true) {
                for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                    cards.addItem(GameConstants.INVALID_CARD);
                }
            } else {
                if (table._open_card[k] == true) {
                    for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                        cards.addItem(table.GRR._cards_data[k][j]);
                    }

                    if (table.is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
                        if (GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_FAN_BEN))
                            times = table._logic.get_times_mul(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);

                        if (GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_PING_BEN))
                            times = table._logic.get_times_ping(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);
                    } else {
                        if (table.has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
                            times = table._logic.get_times_two(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);

                        if (table.has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
                            times = table._logic.get_times_one(table.GRR._cards_data[k],
                                    GameConstants.OX_MAX_CARD_COUNT, table._game_rule_index, table._game_type_index);
                    }
                } else {
                    for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
                        cards.addItem(GameConstants.BLACK_CARD);
                    }
                }
            }

            tableResponse.addGameTime(times);
            tableResponse.addOpenCard(table._open_card[k]);
            tableResponse.addCardType(table._card_type_ox[k]);
            tableResponse.addCardsData(k, cards);
            tableResponse.addAddJetter(table._add_Jetton[k]);
        }

        roomResponse_ox.setSendCard(send_card);

        int display_time = table._cur_operate_time
                - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
        if (display_time > 0) {
            Timer_OX.Builder timer = Timer_OX.newBuilder();
            timer.setDisplayTime(display_time);
            roomResponse_ox.setDisplayTime(timer);
        }

        roomResponse_ox.setTableResponseOx(tableResponse);
        roomResponse.setRoomResponseOx(roomResponse_ox);

        table.observers().send(player, roomResponse);

        return true;
    }
}
