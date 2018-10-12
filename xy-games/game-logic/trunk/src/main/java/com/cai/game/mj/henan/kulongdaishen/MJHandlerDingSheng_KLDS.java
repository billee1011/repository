package com.cai.game.mj.henan.kulongdaishen;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KLDS;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.RevomeDingShenMiddleCardRunnable;
import com.cai.future.runnable.ShowDingShenMiddleCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDingSheng_KLDS extends AbstractMJHandler<MJTable_KLDS> {
    protected int _da_dian_card;

    protected int _banker;

    protected ScheduledFuture<Object> future; // 定时器

    private boolean[] huang_sheng_flag;
    
    private int has_sheng_index;   //有神座位
    
    private int sheng;

    public void reset_status(int banker) {
        _banker = banker;
    }

    @Override
    public void exe(MJTable_KLDS table) {
    	has_sheng_index = GameConstants.INVALID_SEAT;
    	sheng  = GameConstants.INVALID_CARD;
    	
        // 骰子动画
        table.show_shai_zi(_banker);
        // 神牌抽取
        _da_dian_card = table._repertory_card[RandomUtil.getRandomNumber(table._all_card_len - 1)];
        --table.GRR._left_card_count;

        if (AbstractMJTable.DEBUG_CARDS_MODE) {
            _da_dian_card = 0x14;
        }
        
        // 显示抽取到的牌
        table.operate_show_card_ding_sheng(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
                GameConstants.INVALID_SEAT);

        int next_index = CalculateMasterCard(table, _da_dian_card);
        int next_card = GameConstants.INVALID_SEAT;
        if (next_index != GameConstants.INVALID_SEAT) {
        	sheng = _da_dian_card;
            next_card = table._logic.switch_to_card_data(next_index);
            boolean flag = false;
            int _cur_banker = table._cur_banker;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                // 判断是否有神
                int num = table.GRR._cards_index[_cur_banker][next_index];
                if (num != 0) {
                    flag = true;
                    has_sheng_index = i;
                }

                int time = SysParamServerUtil.getSysParamValueServer(3, SysParamServerUtil.DEFAULT_2000,
                        SysParamServerUtil.VAL1, SysParamServerUtil.KEY_1200);
                if (i != 0) {
                    time += SysParamServerUtil.getSysParamValueServer(3, SysParamServerUtil.DEFAULT_2000,
                            SysParamServerUtil.VAL3, SysParamServerUtil.KEY_1200) * i;
                }
                // 控制有神节奏
                GameSchedule.put(new ShowDingShenMiddleCardRunnable(table.getRoom_id(), _cur_banker, flag), time,
                        TimeUnit.MILLISECONDS);
                _cur_banker = (table._cur_banker + (i + 1)) % table.getTablePlayerNumber();
                if (flag) {
                    // 选择换神玩法添加换神动作
                	// 初始化
                	huang_sheng_flag = new boolean[] { false, false, false, false };
                    if (table.has_rule(GameConstants_KLDS.GAME_RULE_KLDS_HUANSHEN)
                            && table.ding_sheng_count < GameConstants_KLDS.DING_SHENG_DEFAULT) {
                        if (table.has_rule(GameConstants_KLDS.GAME_RULE_KLDS_QUANBUTONGYI)) {

                            for (int j = 0; j < table.getTablePlayerNumber(); j++) {
                                PlayerStatus curPlayerStatus = table._playerStatus[j];
                                curPlayerStatus.reset();
                                curPlayerStatus.add_action(GameConstants.WIK_HUANG_SHENG);
                                table.change_player_status(j, GameConstants.Player_Status_OPR_CARD);
                                table.operate_player_action_type(j, false);
                            }
                        } else {
                            PlayerStatus curPlayerStatus = table._playerStatus[table._cur_banker];
                            curPlayerStatus.reset();
                            curPlayerStatus.add_action(GameConstants.WIK_HUANG_SHENG);
                            table.change_player_status(table._cur_banker, GameConstants.Player_Status_OPR_CARD);
                            table.operate_player_action_type(table._cur_banker, false);
                        }
                    } else {
                        future = GameSchedule.put(
                                new RevomeDingShenMiddleCardRunnable(table.getRoom_id(), _banker, next_card), 10,
                                TimeUnit.SECONDS);
                    }
                    table.exe_add_discard(table._cur_banker, 1,
                            new int[] { this._da_dian_card + GameConstants_KLDS.GAME_CARD_DISPATCH_NUM }, true, 3500);
                    // 定时器移除特效及完成定神
                    _da_dian_card = next_card;
                    break;
                }
            }
            if (!flag) {
                // 没有神牌加入废牌堆
                table.exe_add_discard(table._cur_banker, 1,
                        new int[] { this._da_dian_card + GameConstants_KLDS.GAME_CARD_DISPATCH_NUM }, true, 3500);
                int time_ding_sheng = SysParamServerUtil.getSysParamValueServer(3, SysParamServerUtil.DEFAULT_2000,
                        SysParamServerUtil.VAL2, SysParamServerUtil.KEY_1200);
                // 再次定神
                table.exe_ding_sheng(_banker, time_ding_sheng, table);
            }

        } else {
            // 没有神牌加入废牌堆
            table.exe_add_discard(table._cur_banker, 1,
                    new int[] { this._da_dian_card + GameConstants_KLDS.GAME_CARD_DISPATCH_NUM }, true, 3500);
            // 再次定神
            int time_ding_sheng = SysParamServerUtil.getSysParamValueServer(3, SysParamServerUtil.DEFAULT_2000,
                    SysParamServerUtil.VAL2, SysParamServerUtil.KEY_1200);
            table.exe_ding_sheng(_banker, time_ding_sheng, table);
        }

    }

    /**
     * 计算神牌下标
     * 
     * @param table
     * @param card
     * @return -1 表示中发白
     */
    public int CalculateMasterCard(MJTable_KLDS table, int card) {
        int index = table._logic.switch_to_card_index(card);
        int card_num = table._logic.get_card_value(card);
        int corlor = table._logic.get_card_color(card);
        int next_index = -1;
        if (card_num > 0 && card_num < 9 && corlor < 3) { // 9的循环是
            next_index = index + 1;
        } else if (card_num == 9 && corlor < 3) {
            next_index = index - 8;
        } else if (index >= table._logic.switch_to_card_index(0x31)
                && index <= table._logic.switch_to_card_index(0x33)) {
            next_index = index + 1;
        } else if (index == table._logic.switch_to_card_index(0x34)) {
            next_index = index - 3;
        }

        return next_index;
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
    public boolean handler_operate_card(MJTable_KLDS table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 效验操作
        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_error("没有这个操作");
            return false;
        }

        // 记录玩家的操作
        playerStatus.operate(operate_code, operate_card);

        table.change_player_status(seat_index, GameConstants.INVALID_VALUE);
        table.operate_player_action_type(seat_index, true);

        // 放弃操作
        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            GameSchedule.put(new RevomeDingShenMiddleCardRunnable(table.getRoom_id(), _banker, _da_dian_card), 0,
                    TimeUnit.SECONDS);
            return true;
        }

        // 没有操作的用户
        huang_sheng_flag[seat_index] = true;
        if (table.has_rule(GameConstants_KLDS.GAME_RULE_KLDS_QUANBUTONGYI)) {
            boolean flag = true;
            for (int i = 0; i < huang_sheng_flag.length; i++) {
                if (huang_sheng_flag[i] == false) {
                    flag = false;
                }
            }
            if (!flag) {
                return false;
            }
        }

        // 执行动作
        switch (operate_code) {
        case GameConstants.WIK_HUANG_SHENG: {
            // 累加换神次数
            table.ding_sheng_count++;
            // future.cancel(true);
            // 没有神牌加入废牌堆
            /*table.exe_add_discard(table._cur_banker, 1,
                    new int[] { this._da_dian_card + GameConstants_KLDS.GAME_CARD_DISPATCH_NUM }, true, 0);*/
            // 再次定神
            table.exe_ding_sheng(_banker, 0, table);
            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_KLDS table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        // 游戏变量
        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(table.GRR._banker_player);
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
                if (table._logic.get_magic_card_count() != 0
                        && table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    // 次牌
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI);
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
                if (table._logic
                        .is_ci_card(table._logic.switch_to_card_index(table.GRR._weave_items[i][j].center_card))) {
                    weaveItem_item.setCenterCard(
                            table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI);
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
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
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.get_magic_card_count() > 0 && table._logic.is_magic_card(cards[j])) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI;
            }
        }

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);
        
        if(table.has_rule(GameConstants_KLDS.GAME_RULE_KLDS_QUANBUTONGYI)){
        	if(!huang_sheng_flag[seat_index]){
        		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
        		curPlayerStatus.reset();
        		curPlayerStatus.add_action(GameConstants.WIK_HUANG_SHENG);
        		table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
        		table.operate_player_action_type(seat_index, false);
        	}
        }else{
        	if(seat_index == _banker && !huang_sheng_flag[seat_index]){
        		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
        		curPlayerStatus.reset();
        		curPlayerStatus.add_action(GameConstants.WIK_HUANG_SHENG);
        		table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
        		table.operate_player_action_type(seat_index, false);
        	}
        }

        //哪家有神
        if(has_sheng_index != GameConstants.INVALID_SEAT){
        	table.operate_show_you_sheng(has_sheng_index,true);
        }
        // 显示
        table.operate_show_card_ding_sheng(seat_index, GameConstants.Show_Card_Center, 1, new int[] { sheng },
                seat_index);

        return true;
    }
}
