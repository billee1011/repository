package com.cai.game.mj.handler.jsjh;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.LengTuoZiRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_JiangSu_JH extends MJHandlerGang<MJTable_JiangSu_JH> {
    @Override
    public void exe(MJTable_JiangSu_JH table) {
        // TODO Auto-generated method stub
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (table._playerStatus[i].has_action()) {
                table.operate_player_action(i, true);
            }
            table._playerStatus[i].clean_action();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            // table._playerStatus[i].clean_status();
        }

        table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            _action = GameConstants.WIK_AN_GANG;
        }
        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                GameConstants.INVALID_SEAT);

        if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
            this.exe_gang(table);
            return;
        }

        // //杠红中,不能抢杠
        // if(table._logic.switch_to_card_index(_center_card) ==
        // table._logic.get_magic_card_index(0)){
        // this.exe_gang(table);
        // return;
        // }

        // 检查对这个杠有没有 胡
        boolean bAroseAction = false;
        bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

        if (bAroseAction == false) {
            this.exe_gang(table);
        } else {
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
        // 抢杠胡

        // 效验状态
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 是否已经响应
        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return false;
        }

        // 是否已经响应
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
        {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
            table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
            return false;
        }

        // 玩家的操作
        playerStatus.operate(operate_code, operate_card);

        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
            table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
        } else if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            // 效果
            table.process_chi_hu_player_operate(seat_index, new int[] { operate_card }, 1, false);
        } else {
            table.log_player_error(seat_index, "出牌操作,没有动作");
            return false;
        }

        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu())) {
                    return false;
                }
            }
        }

        table.operate_player_action(seat_index, true);

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
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

                // 优先级别
                // int cbTargetActionRank =
                // table._logic.get_action_rank(target_action) +
                // target_p;//table._playerStatus[target_player].get_perform()

                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 选择了不胡
        if (target_action == GameConstants.WIK_NULL) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table.GRR._chi_hu_rights[i].set_valid(false);
            }
            // 发牌给杠的玩家
            this.exe_gang(table);
            return true;
        }
        
        operate_card = _center_card;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if (i == target_player) {
                table.GRR._chi_hu_rights[i].set_valid(true);
            } else {
                table.GRR._chi_hu_rights[i].set_valid(false);
            }
        }

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            // table._playerStatus[i].clean_status();
            table.change_player_status(i, GameConstants.INVALID_VALUE);
            table.operate_player_action(i, true);
        }

        // 胡牌
        if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if (i == target_player) {
                    table.GRR._chi_hu_rights[i].set_valid(true);
                } else {
                    table.GRR._chi_hu_rights[i].set_valid(false);
                }
            }
            table.GRR._chi_hu_card[target_player][0] = _center_card;
            table.process_chi_hu_player_operate(target_player, _center_card, false);
            table.process_chi_hu_player_score_jszz(target_player, _seat_index, _center_card, false, false, false,
                    false);
        } else {
            int hu_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                hu_count++;
            }
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                table.GRR._chi_hu_card[i][0] = _center_card;
                if (hu_count == 3) {
                    table.process_chi_hu_player_score_jszz(i, _seat_index, _center_card, false, true, false, false);
                } else {
                    table.process_chi_hu_player_score_jszz(i, _seat_index, _center_card, false, false, false, false);
                }

            }
        }

        // 记录
        GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

        return true;
    }

    /**
     * 执行杠
     * 
     * 
     ***/
    @Override
    protected boolean exe_gang(MJTable_JiangSu_JH table) {
        int cbCardIndex = table._logic.switch_to_card_index(_center_card);
        int cbWeaveIndex = -1;

        int outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._out_card_index[_seat_index][i] > 0) {
                outcard_count += table._out_card_index[_seat_index][i];
            }
        }
        int da_outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._out_card_index[_seat_index][i] > 0 && !table._logic.is_magic_index(i)) {
                da_outcard_count += table._out_card_index[_seat_index][i];
            }
        }
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            // 暗杠
            // 设置变量
            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;
            if (table._logic.is_magic_card(_center_card)) {
                if (da_outcard_count == 0) {
                    table._da_tou_da[_seat_index] += 6 * 4;
                    table._xian_chu_count[_seat_index] = 0;
                } else {
                    table._da_er_da[_seat_index] += 4 * 4;
                    table._xian_chu_count[_seat_index] = 0;
                }
            }
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            // 别人打的牌

            cbWeaveIndex = table.GRR._weave_count[_seat_index];
            table.GRR._weave_count[_seat_index]++;

            if (table._logic.is_magic_card(_center_card)) {
                if (da_outcard_count == 0) {
                    table._da_tou_da[_seat_index] += 6 * 4;
                    table._xian_chu_count[_seat_index] = 0;
                } else {
                    table._da_er_da[_seat_index] += 4 * 4;
                    table._xian_chu_count[_seat_index] = 0;
                }
            }

            // 删掉出来的那张牌
            // table.operate_out_card(_provide_player, 0,
            // null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
            table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
            // 看看是不是有碰的牌，明杠
            // 寻找组合
            for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
                int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
                int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
                if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
                    cbWeaveIndex = i;// 第几个组合可以碰
                    _provide_player = table.GRR._weave_items[_seat_index][i].provide_player;// 找到放碰到人
                    break;
                }
            }
            // 玩家出牌数量
            if (table._logic.is_magic_card(_center_card)) {
                if (da_outcard_count > 0) {
                    table._da_er_da[_seat_index] += 4;
                } else {
                    table._da_tou_da[_seat_index] += 6;
                }
            }

            if (cbWeaveIndex == -1) {
                table.log_player_error(_seat_index, "杠牌出错");
                return false;
            }
        }

        table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
        table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

        // 设置用户
        table._current_player = _seat_index;

        // 删除手上的牌
        table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
        table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
        // 刷新手牌包括组合

        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j]) && da_outcard_count == 0) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
            } else if (table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DA;
            }
        }

        WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
        int weave_count = table.GRR._weave_count[_seat_index];
        for (int i = 0; i < weave_count; i++) {
            weaves[i] = new WeaveItem();
            weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
            weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
            weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
            weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player
                    + GameConstants.WEAVE_SHOW_DIRECT;
            if (weaves[i].weave_kind == GameConstants.WIK_PENG) {
                if (table._logic.is_magic_card(weaves[i].center_card) && table._da_er_da[_seat_index] / 4 < 3) {
                    weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
                } else if (table._logic.is_magic_card(weaves[i].center_card)) {
                    weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
                }
            } else if (weaves[i].weave_kind == GameConstants.WIK_GANG
                    || weaves[i].weave_kind == GameConstants.WIK_AN_GANG) {
                if (table._logic.is_magic_card(weaves[i].center_card) && table._da_er_da[_seat_index] / 4 < 4) {
                    weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
                } else if (table._logic.is_magic_card(weaves[i].center_card)) {
                    weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
                }
            }

        }

        table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

        // 河南麻将,检查听牌
        table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(
                table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
                table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

        int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
        int ting_count = table._playerStatus[_seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
        } else {
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
        }

        ///////////////////////////////////////////////////// 算分

        int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
        if (GameConstants.GANG_TYPE_AN_GANG == _type) {
            table._player_result.an_gang_count[_seat_index]++;
            if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (i == _seat_index) {
                        continue;
                    }
                    int score = (int) (table._player_result.game_score[i] - 2 * table._di_fen);
                    if (score < -table._yuanzi_fen) {
                        table._player_result.game_score[i] -= table._yuanzi_fen + table._player_result.game_score[i];
                        table._player_result.game_score[_seat_index] += table._yuanzi_fen
                                + table._player_result.game_score[i];
                        table._end_score[i] -= table._yuanzi_fen + table._player_result.game_score[i];
                        table._end_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[i];
                    } else {
                        table._player_result.game_score[i] -= 2 * table._di_fen;
                        table._player_result.game_score[_seat_index] += 2 * table._di_fen;
                        table._end_score[i] -= 2 * table._di_fen;
                        table._end_score[_seat_index] += 2 * table._di_fen;
                    }
                }
            } else {
                int gang_cell = table._di_fen;
                if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
                    gang_cell *= 2;
                }
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if (i == _seat_index) {
                        continue;
                    }
                    table._player_result.game_score[i] -= 2 * gang_cell;
                    table._player_result.game_score[_seat_index] += 2 * gang_cell;
                    table._end_score[i] -= 2 * gang_cell;
                    table._end_score[_seat_index] += 2 * gang_cell;
                }
            }
        } else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
            table._player_result.ming_gang_count[_seat_index]++;
            if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
                if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
                    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                        if (i == _seat_index) {
                            continue;
                        }
                        int score = (int) (table._player_result.game_score[i] - table._di_fen);
                        if (score < -table._yuanzi_fen) {
                            table._player_result.game_score[i] -= table._yuanzi_fen
                                    + table._player_result.game_score[i];
                            table._player_result.game_score[_seat_index] += table._yuanzi_fen
                                    + table._player_result.game_score[i];
                            table._end_score[i] -= table._yuanzi_fen + table._player_result.game_score[i];
                            table._end_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[i];
                        } else {
                            table._player_result.game_score[i] -= table._di_fen;
                            table._player_result.game_score[_seat_index] += table._di_fen;
                            table._end_score[i] -= table._di_fen;
                            table._end_score[_seat_index] += table._di_fen;
                        }
                    }

                } else {
                    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                        if (i == _seat_index) {
                            continue;
                        }
                        table._player_result.game_score[i] -= table._di_fen;
                        table._player_result.game_score[_seat_index] += table._di_fen;
                        table._end_score[i] -= table._di_fen;
                        table._end_score[_seat_index] += table._di_fen;
                    }
                }
            } else {
                if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
                    int score = (int) (table._player_result.game_score[_provide_player] - table._di_fen * 3);
                    if (score < -table._yuanzi_fen) {
                        table._player_result.game_score[_provide_player] -= table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                        table._player_result.game_score[_seat_index] += table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                        table._end_score[_provide_player] -= table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                        table._end_score[_seat_index] += table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                    } else {
                        table._player_result.game_score[_provide_player] -= table._di_fen * 3;
                        table._player_result.game_score[_seat_index] += table._di_fen * 3;
                        table._end_score[_provide_player] -= table._di_fen * 3;
                        table._end_score[_seat_index] += table._di_fen * 3;
                    }
                } else {
                    int gang_cell = table._di_fen;
                    if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
                        gang_cell *= 2;
                    }
                    table._player_result.game_score[_provide_player] -= gang_cell * 3;
                    table._player_result.game_score[_seat_index] += gang_cell * 3;
                    table._end_score[_provide_player] -= gang_cell * 3;
                    table._end_score[_seat_index] += gang_cell * 3;
                }
            }
        } else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {// 放碰的人给分
            table._player_result.ming_gang_count[_seat_index]++;
            if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
                if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
                    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                        if (i == _seat_index) {
                            continue;
                        }
                        int score = (int) (table._player_result.game_score[i] - table._di_fen);
                        if (score < -table._yuanzi_fen) {
                            table._player_result.game_score[i] -= table._yuanzi_fen
                                    + table._player_result.game_score[i];
                            table._player_result.game_score[_seat_index] += table._yuanzi_fen
                                    + table._player_result.game_score[i];
                            table._end_score[i] -= table._yuanzi_fen + table._player_result.game_score[i];
                            table._end_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[i];
                        } else {
                            table._player_result.game_score[i] -= table._di_fen;
                            table._player_result.game_score[_seat_index] += table._di_fen;
                            table._end_score[i] -= table._di_fen;
                            table._end_score[_seat_index] += table._di_fen;
                        }
                    }
                } else {
                    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                        if (i == _seat_index) {
                            continue;
                        }
                        table._player_result.game_score[i] -= table._di_fen;
                        table._player_result.game_score[_seat_index] += table._di_fen;
                        table._end_score[i] -= table._di_fen;
                        table._end_score[_seat_index] += table._di_fen;
                    }
                }
            } else {
                if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
                    int score = (int) (table._player_result.game_score[_provide_player] - table._di_fen * 3);
                    if (score < -table._yuanzi_fen) {
                        table._player_result.game_score[_provide_player] -= table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                        table._player_result.game_score[_seat_index] += table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                        table._end_score[_provide_player] -= table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                        table._end_score[_seat_index] += table._yuanzi_fen
                                + table._player_result.game_score[_provide_player];
                    } else {
                        table._player_result.game_score[_provide_player] -= table._di_fen * 3;
                        table._player_result.game_score[_seat_index] += table._di_fen * 3;
                        table._end_score[_provide_player] -= table._di_fen * 3;
                        table._end_score[_seat_index] += table._di_fen * 3;
                    }
                } else {
                    int gang_cell = table._di_fen;
                    if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
                        gang_cell *= 2;
                    }
                    table._player_result.game_score[_provide_player] -= gang_cell * 3;
                    table._player_result.game_score[_seat_index] += gang_cell * 3;
                    table._end_score[_provide_player] -= gang_cell * 3;
                    table._end_score[_seat_index] += gang_cell * 3;
                }
            }

        }

        RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
        roomResponse2.setGameStatus(table._game_status);
        roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
        table.load_player_info_data(roomResponse2);
        table.send_response_to_room(roomResponse2);

        if (!table._logic.is_da_gen_card(_center_card)) {
            if (GameConstants.GANG_TYPE_ADD_GANG == _type || GameConstants.GANG_TYPE_AN_GANG == _type) {
                // 从后面发一张牌给玩家
                table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_AN_GANG_KAI, 0);
            } else {
                // 从后面发一张牌给玩家
                table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);
            }
        } else {
            table.set_handler_out_card_operate();
        }

        return true;
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
        tableResponse.setCurrentPlayer(_seat_index);
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
            // if(_self){
            // if(i == _seat_index){
            // tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
            // }else{
            // tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            // }
            // }else{
            // tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
            // }

            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

        }

        // 数据
        int outcard_count = 0;
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (table._out_card_index[seat_index][i] > 0 && !table._logic.is_magic_index(i)) {
                outcard_count += table._out_card_index[seat_index][i];
            }
        }
        tableResponse.setSendCardData(0);
        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(cards[j]) && outcard_count == 0) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
            } else {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DA;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        // 效果
        table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }

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
