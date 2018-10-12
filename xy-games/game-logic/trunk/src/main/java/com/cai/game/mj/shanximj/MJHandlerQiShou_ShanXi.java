package com.cai.game.mj.shanximj;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerQiShou_ShanXi extends AbstractMJHandler<Table_ShanXi> {

    private int _seat_index = GameConstants.INVALID_SEAT;

    public void reset_status(int seat_index) {
        _seat_index = seat_index;
    }

    @Override
    public void exe(Table_ShanXi table) {
        // TODO Auto-generated method stub
        // table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);
        table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
        table.operate_player_action(_seat_index, false);     

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
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
    public boolean handler_operate_card(Table_ShanXi table, int seat_index, int operate_code, int operate_card) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        // 效验操作
        if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
            table.log_error("没有这个操作");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("不是当前玩家操作");
            return false;
        }

        // 放弃操作
        if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
            
            // 用户状态
            table._playerStatus[_seat_index].clean_action();
            table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
            // table._playerStatus[_seat_index].clean_status();

            // table._playerStatus[_seat_index].set_status(MJGameConstants.Player_Status_OUT_CARD);
            // table.operate_player_status();

            table.exe_dispatch_card(table.GRR._banker_player, GameConstants.WIK_NULL, 0);

            return true;
        }

        table.GRR._chi_hu_rights[_seat_index].set_valid(true);
        // 下局胡牌的是庄家
        table.set_niao_card_hz(_seat_index, GameConstants.INVALID_VALUE, true, 0, 0x35); // 结束后设置鸟牌

        table.GRR._chi_hu_card[_seat_index][0] = operate_card;
        
        table._shang_zhuang_player =  table._cur_banker;
        table._cur_banker = _seat_index;

        if (table.is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
                || table.is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
            table.process_chi_hu_player_operate(_seat_index, operate_card, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);
        } else if (table.is_mj(GameConstants.GAME_ID_HENAN)) {
            table.process_chi_hu_player_operate_hnhz(_seat_index, operate_card, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

        } else {
            table.process_chi_hu_player_operate(_seat_index, operate_card, true);
            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);
        }

        // 记录
        table._player_result.zi_mo_count[_seat_index]++;

        GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
                GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_ShanXi table, int seat_index) {
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
        // tableResponse.setActionMask((_response[seat_index] == false) ?
        // _player_action[seat_index] : MJGameConstants.WIK_NULL);

        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);// 是否托管
            //比赛场托管状态取玩家当前托管状态
            if(table.is_match()){
            	tableResponse.addTrustee(table.istrustee[i]);// 是否托管
            }
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
                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
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
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(hand_cards[j])) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
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
        } else {
            // ting_cards[0]=0;
            // table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
        }
        //重连后比赛场发送托管协议
        table.be_in_room_trustee_match(seat_index);
        return true;
    }

}
