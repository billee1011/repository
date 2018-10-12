package com.cai.game.mj.handler.jsjh;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.LengTuoZiRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate_JiangSu_JH extends MJHandlerOutCardOperate<MJTable_JiangSu_JH> {

    @Override
    public void exe(MJTable_JiangSu_JH table) {

        int outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._out_card_index[_out_card_player][i] > 0) {
                outcard_count += table._out_card_index[_out_card_player][i];
            }
        }
        int no_da_outcard_count = 0;
        int da_outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._out_card_index[_out_card_player][i] > 0 && !table._logic.is_magic_index(i)) {
                no_da_outcard_count += table._out_card_index[_out_card_player][i];
            }
            if (table._out_card_index[_out_card_player][i] > 0 && table._logic.is_magic_index(i)) {
                da_outcard_count += table._out_card_index[_out_card_player][i];
            }
        }
        if (table._logic.is_magic_card(_out_card_data)) {
            table._playerStatus[_out_card_player].chi_hu_round_valid();// 可以胡了
            table._is_da_shouda[_out_card_player] = true;
            if (no_da_outcard_count == 0) {
                table._xian_chu_count[_out_card_player]--;
                // 效果
                table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.EFFECT_TOUDA }, 1, GameConstants.INVALID_SEAT);
            } else {
                table._xian_chu_count[_out_card_player] = 0;
                if (_type == GameConstants.WIK_PENG) {
                    table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                            new long[] { GameConstants.EFFECT_BEIDONGDA }, 1, GameConstants.INVALID_SEAT);
                } else {
                    if (da_outcard_count == 0) {
                        table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                                new long[] { GameConstants.EFFECT_ERDA }, 1, GameConstants.INVALID_SEAT);
                    } else {
                        table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                                new long[] { _out_card_data }, 1, GameConstants.INVALID_SEAT);
                    }
                }
            }
        } else {
            table._xian_chu_count[_out_card_player] = 0;
        }

        // TODO Auto-generated method stub
        PlayerStatus playerStatus = table._playerStatus[_out_card_player];

        // 重置玩家状态
        // playerStatus.clean_status();
        table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
        playerStatus.clean_action();

        // 出牌记录
        table._out_card_count++;
        table._out_card_player = _out_card_player;
        table._out_card_data = _out_card_data;

        // 用户切换
        int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
        table._current_player = next_player;
        table._out_card_index[_out_card_player][table._logic.switch_to_card_index(_out_card_data)]++;
        // 打牌记录,自大暗杠
        if (!table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
            if (table._out_card_index[_out_card_player][table._logic.switch_to_card_index(_out_card_data)] == 4) {
                int cell_score = 2;
                if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
                    cell_score *= 2;
                }
                table._player_result.game_score[_out_card_player] -= cell_score * 3;
                table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.EFFECT_ZIDAANGANG }, 1, GameConstants.INVALID_SEAT);
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (i == _out_card_player) {
                        continue;
                    }
                    table._player_result.game_score[i] += cell_score;
                    table._end_score[i] += cell_score;
                }
                table._zi_da_an_gang[_out_card_player] += cell_score;
                table._end_score[_out_card_player] -= cell_score * 3;
                RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
                roomResponse2.setGameStatus(table._game_status);
                roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
                table.load_player_info_data(roomResponse2);
                table.send_response_to_room(roomResponse2);
            }
        } else {
            if (table._out_card_index[_out_card_player][table._logic.switch_to_card_index(_out_card_data)] == 4) {
                table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                        new long[] { GameConstants.EFFECT_ZIDAANGANG }, 1, GameConstants.INVALID_SEAT);
            }
        }

        // 跟张
        if (!table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
            if (table._gen_player != GameConstants.INVALID_SEAT) {
                if (table._gen_out_card != _out_card_data) {
                    table._gen_out_card = _out_card_data;
                    table._gen_player = _out_card_player;
                } else if (table._gen_out_card == _out_card_data) {
                    table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                            new long[] { GameConstants.EFFECT_GENZHANG }, 1, GameConstants.INVALID_SEAT);

                    if (_out_card_data == table.GRR._especial_show_cards[0]) {
                        // 正搭牌只需要连着3个人打出即算跟张
                        if ((next_player + 1) % table.getTablePlayerNumber() == table._gen_player) {
                            int score = 1;

                            if (table._logic.is_magic_card(_out_card_data)) {
                                score = 2;
                            }
                            if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
                                score *= 2;
                            }
                            table._gen_zhang[table._gen_player] += score;
                            table._player_result.game_score[table._gen_player] -= score * 3;
                            table._end_score[table._gen_player] -= score * 3;
                            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                                if (i == table._gen_player) {
                                    continue;
                                }
                                table._player_result.game_score[i] += score;
                                table._end_score[i] += score;
                            }
                            RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
                            roomResponse2.setGameStatus(table._game_status);
                            roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
                            table.load_player_info_data(roomResponse2);
                            table.send_response_to_room(roomResponse2);
                        }
                    } else {
                        if (table._gen_player == next_player) {
                            int score = 1;

                            if (table._logic.is_magic_card(_out_card_data)) {
                                score = 2;
                            }
                            if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
                                score *= 2;
                            }
                            table._gen_zhang[table._gen_player] += score;
                            table._player_result.game_score[table._gen_player] -= score * 3;
                            table._end_score[table._gen_player] -= score * 3;
                            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                                if (i == table._gen_player) {
                                    continue;
                                }
                                table._player_result.game_score[i] += score;
                                table._end_score[i] += score;
                            }
                            RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
                            roomResponse2.setGameStatus(table._game_status);
                            roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
                            table.load_player_info_data(roomResponse2);
                            table.send_response_to_room(roomResponse2);
                        }
                    }

                }
            } else {
                table._gen_out_card = _out_card_data;
                table._gen_player = _out_card_player;
            }
        } else {
            if (table._gen_player != GameConstants.INVALID_SEAT) {
                if (table._gen_out_card != _out_card_data) {
                    table._gen_out_card = _out_card_data;
                    table._gen_player = _out_card_player;
                } else if (table._gen_out_card == _out_card_data) {
                    table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                            new long[] { GameConstants.EFFECT_GENZHANG }, 1, GameConstants.INVALID_SEAT);
                }
            } else {
                table._gen_out_card = _out_card_data;
                table._gen_player = _out_card_player;
            }
        }

        // 刷新手牌
        int cards[] = new int[GameConstants.MAX_COUNT];
        // 刷新自己手牌
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);

        outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._out_card_index[_out_card_player][i] > 0) {
                outcard_count += table._out_card_index[_out_card_player][i];
            }
        }
        // 癞子
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j]) && outcard_count == 0) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
            } else if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DA;
            }
        }
        table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

        // 癞子
        int real_card = _out_card_data;

        if (table._logic.is_magic_card(real_card)) {

            if (no_da_outcard_count == 0) {
                real_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
            } else if (_type == GameConstants.WIK_PENG) {
                real_card += GameConstants.CARD_ESPECIAL_TYPE_BEIDONG_DA;
            } else {
                real_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
            }
        }
        int dacard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._logic.is_magic_index(i)) {
                dacard_count += table.GRR._cards_index[_out_card_player][i];
            }
        }
        dacard_count -= table._xian_chu_count[_out_card_player];
        long effect_indexs[] = new long[dacard_count];
        table.operate_effect_action(_out_card_player, GameConstants.Effect_Action_Other, dacard_count, effect_indexs, 1,
                GameConstants.INVALID_SEAT);
        // 显示出牌
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                GameConstants.INVALID_SEAT, table.GRR._discard_count[_out_card_player]);

        // 河南麻将,检查听牌
        table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(
                table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
                table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player]);

        int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
        int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

        if (ting_count > 0) {
            if (ting_cards[0] == -1) {
                table._user_pao_da[_out_card_player] = true;
            }
            table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
        } else {
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
        }

        table._provide_player = _out_card_player;
        table._provide_card = _out_card_data;

        // 玩家出牌 响应判断,是否有吃碰杠补胡
        boolean bAroseAction = table.estimate_player_out_card_respond_jszz(_out_card_player, _out_card_data);// ,
                                                                                                             // EstimatKind.EstimatKind_OutCard

        if (table._logic.is_magic_card(_out_card_data)) {

            if (no_da_outcard_count != 0 && _type != GameConstants.WIK_PENG) {
                boolean has_hu = false;
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (table._playerStatus[i].has_chi_hu()) {
                        has_hu = true;
                    }
                }
                if (has_hu) {
                    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                        if (!table._playerStatus[i].has_chi_hu()) {
                            table._playerStatus[i].clean_action();
                        } else {
                            table._playerStatus[i].clean_action(GameConstants.WIK_GANG);
                            table._playerStatus[i].clean_action(GameConstants.WIK_PENG);
                        }
                    }
                } else {
                    bAroseAction = false;
                }

            }
        }
        // 如果没有需要操作的玩家，派发扑克
        if (bAroseAction == false) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
                // table._playerStatus[i].clean_status();
            }

            table.operate_player_action(_out_card_player, true);

            // 加入牌队
            // GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _out_card_player,
            // 1, new int[]{_out_card_data}),
            // MJGameConstants.DELAY_ADD_CARD_DELAY, TimeUnit.MILLISECONDS);
            table.exe_add_discard(_out_card_player, 1, new int[] { real_card }, false,
                    GameConstants.DELAY_SEND_CARD_DELAY);
            // 发牌
            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
            if (table._logic.is_magic_card(_out_card_data)) {
                if (no_da_outcard_count == 0) {
                    table._da_tou_da[_out_card_player] += 6;
                    table._xian_chu_count[_out_card_player] = 0;
                } else {
                    if (_type == GameConstants.WIK_PENG) {
                        table._da_peng_da[_out_card_player] += 2;
                    } else {
                        table._da_er_da[_out_card_player] += 4;
                    }
                }
            }
        } else {
            // 等待别人操作这张牌
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (table._playerStatus[i].has_action()) {
                    // table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//操作状态
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
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
    @Override
    public boolean handler_operate_card(MJTable_JiangSu_JH table, int seat_index, int operate_code, int operate_card) {
        // 效验状态

        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 是否已经响应
        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return true;
        }

        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }

        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
        {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return true;
        }
        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_CHI_HU) {

        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            table._can_not_open_index[seat_index][table._logic.switch_to_card_index(_out_card_data)]++;
        }

        if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
            table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
        }

        // 变量定义 优先级最高操作的玩家和操作--不通炮的算法
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;
            // 优先级别
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    // 获取最大的动作的优先级
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }
                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }
        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)
                || table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
            if (operate_code == GameConstants.WIK_CHI_HU) {
                table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
                // 效果
                table.process_chi_hu_player_operate(seat_index, new int[] { operate_card }, 1, false);
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu())) {
                        return false;
                    }
                }
            } else if (operate_code == GameConstants.WIK_NULL) {
                // 优先级最高的人还没操作
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu())) {
                        return false;
                    }
                }
            } else {
                // 优先级最高的人还没操作
                if (table._playerStatus[target_player].is_respone() == false)
                    return true;
            }
        } else {
            // 优先级最高的人还没操作
            if (table._playerStatus[target_player].is_respone() == false)
                return true;
        }

        // 修改网络导致吃碰错误 9.26 WalkerGeek
        int target_card = _out_card_data;
        // error int target_card = table._playerStatus[target_player]._operate_card;

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            // table._playerStatus[i].clean_status();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 删除扑克
        switch (target_action) {
        case GameConstants.WIK_LEFT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_RIGHT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_CENTER: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }
        case GameConstants.WIK_PENG: // 碰牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }
            table.GRR._chi_hu_rights[target_player].set_valid(false);// 胡牌无效
            if (table._playerStatus[target_player].has_chi_hu()) {
                table._playerStatus[target_player].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
            }
            if (table._logic.is_magic_card(target_card)) {
                int da_outcard_count = 0;
                for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
                    if (table._out_card_index[_out_card_player][i] > 0 && !table._logic.is_magic_index(i)) {
                        da_outcard_count += table._out_card_index[_out_card_player][i];
                    }
                }
                if (da_outcard_count == 0) {
                    table._da_tou_da[target_player] += 6 * 3;
                    table._xian_chu_count[target_player] -= 2;
                } else {
                    table._da_er_da[target_player] += 4 * 3;
                    table._xian_chu_count[target_player] = 0;
                }
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);

            return true;
        }

        case GameConstants.WIK_GANG: // 杠牌操作
        {
            // 是否有抢杠胡
            table.exe_gang(target_player, _out_card_player, target_card, target_action,
                    GameConstants.GANG_TYPE_JIE_GANG, false, false);

            return true;
        }
        case GameConstants.WIK_NULL: {

            // 删掉出来的那张牌
            // table.operate_out_card(this._out_card_player, 0,
            // null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
            int real_card = _out_card_data;
            int outcard_count = 0;
            for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
                if (table._out_card_index[_out_card_player][i] > 0 && !table._logic.is_magic_index(i)) {
                    outcard_count += table._out_card_index[_out_card_player][i];
                }
            }
            if (table._logic.is_magic_card(real_card)) {
                if (outcard_count == 0) {
                    real_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
                } else {
                    real_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
                }
            }
            // 加到牌堆 没有人要
            table.exe_add_discard(this._out_card_player, 1, new int[] { real_card }, false, 0);

            // 用户切换
            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();

            // 发牌
            table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

            if (table._logic.is_magic_card(_out_card_data)) {
                if (outcard_count == 0) {
                    table._da_tou_da[_out_card_player] += 6;
                } else {
                    if (_type == GameConstants.WIK_PENG) {
                        table._da_peng_da[_out_card_player] += 2;
                    } else {
                        table._da_er_da[_out_card_player] += 4;
                    }
                }
            }
            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {

            if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (i == target_player) {
                        table.GRR._chi_hu_rights[i].set_valid(true);
                    } else {
                        table.GRR._chi_hu_rights[i].set_valid(false);
                    }
                }
                table.GRR._chi_hu_card[target_player][0] = target_card;
                table.process_chi_hu_player_operate(target_player, target_card, false);
                table.process_chi_hu_player_score_jszz(target_player, _out_card_player, _out_card_data, false, false,
                        false, false);
            } else {
                int hu_count = 0;
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                        continue;
                    }
                    hu_count++;
                }

                // 打了三个碰的包赔玩家
                Arrays.fill(table.bao_pei_palyer, GameConstants.INVALID_SEAT);
                for (int hu_player = 0; hu_player < table.getTablePlayerNumber(); hu_player++) {

                    if (hu_count == 3) {
                        break;
                    }
                    if (!table.GRR._chi_hu_rights[hu_player].is_valid()) {
                        continue;
                    }
                    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                        if (table._peng_palyer_count[hu_player][i] >= 3) {
                            table._is_bao_pei = true;
                            table.bao_pei_palyer[hu_player] = i;
                            break;
                        }
                    }
                }

                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                        continue;
                    }
                    table.GRR._chi_hu_card[i][0] = target_card;
                    if (hu_count == 3) {
                        table.process_chi_hu_player_score_jszz(i, _out_card_player, _out_card_data, false, true, false,
                                false);
                    } else {
                        if (table.bao_pei_palyer[i] == GameConstants.INVALID_SEAT) {
                            table.process_chi_hu_player_score_jszz(i, _out_card_player, _out_card_data, false, false,
                                    false, false);
                        } else {
                            table.process_chi_hu_player_score_jszz(i, table.bao_pei_palyer[i], _out_card_data, false,
                                    false, true, false);
                        }

                    }

                }
            }

            // 记录
            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            return true;
        }
        default:
            return false;
        }
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_JiangSu_JH table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        // 游戏变量
        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(_out_card_player);
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
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    // 癞子
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
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
                if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG) {
                    if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)
                            && table._da_er_da[i] / 4 < 3) {
                        weaveItem_item.setCenterCard(
                                table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
                    } else if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
                        weaveItem_item.setCenterCard(
                                table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
                    } else {
                        weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                    }
                } else if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG
                        || table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_GANG) {
                    if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)
                            && table._da_er_da[i] / 4 < 4) {
                        weaveItem_item.setCenterCard(
                                table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
                    } else if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
                        weaveItem_item.setCenterCard(
                                table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
                    } else {
                        weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                    }
                }
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
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
        int outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._logic.is_magic_index(i)) {
                outcard_count += table._out_card_index[seat_index][i];
            }
        }
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j]) && outcard_count == 0) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
            } else if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DA;
            }
        }
        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }
        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        int real_card = _out_card_data;
        if (table._logic.is_magic_card(_out_card_data) && outcard_count == 0) {
            real_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
        } else if (table._logic.is_magic_card(_out_card_data)) {
            if (_type == GameConstants.WIK_PENG) {
                real_card += GameConstants.CARD_ESPECIAL_TYPE_BEIDONG_DA;
            } else {
                real_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
            }
        }
        // 出牌
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                seat_index);

        // table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        // 听牌显示
        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        // 刷新手上2搭牌
        for (int index = 0; index < table.getTablePlayerNumber(); index++) {
            int dacard_count = 0;
            for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
                if (table._logic.is_magic_index(i)) {
                    dacard_count += table.GRR._cards_index[index][i];
                }
            }
            dacard_count -= table._xian_chu_count[index];
            if (dacard_count > 0) {
                long effect_indexs[] = new long[dacard_count];
                table.operate_effect_action(index, GameConstants.Effect_Action_Other, dacard_count, effect_indexs, 1,
                        seat_index);
            }
        }
        return true;
    }

}
