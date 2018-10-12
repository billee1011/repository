package com.cai.game.mj.handler.yyzxz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;
import com.cai.game.mj.handler.moyanggui.MjTableMoYangGui;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate_YYZXZ extends MJHandlerOutCardOperate<MJTable_YYZXZ> {

    @Override
    public void exe(MJTable_YYZXZ table) {
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
        boolean bXiaPai = false;
		int next_player = _out_card_player;
		if(table._logic.checkWanZi(_out_card_data)){
			//更新虾分
			bXiaPai = true;
			table.xia_zi_fen[_out_card_player]++;
			table._player_result.pao[_out_card_player] = table.xia_zi_fen[_out_card_player];
			table.operate_player_data();
	        table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, 
	        		new long[] { GameConstants.WIK_XIA_ZI_BU }, 1,
	                GameConstants.INVALID_SEAT);
		}else{
			next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		}
		table._current_player = next_player;

        // 刷新手牌
        int cards[] = new int[GameConstants.MAX_COUNT];

        // 刷新自己手牌
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
        
        // 虾子牌
        for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.checkWanZi(cards[j]) && table.has_rule(GameConstants.ZXZ_MAN_TIAN_FEI)) {
                cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
        }
        
        table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

        // 显示出牌
        if(!bXiaPai){
            table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);
        }

        
        //听牌检查
		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}
		

        table._provide_player = _out_card_player;
        table._provide_card = _out_card_data;

        int color = table._logic.get_card_color(_out_card_data);

        // 玩家出牌 响应判断,是否有吃碰杠补胡
        boolean bAroseAction = color != 0 && table.estimate_player_out_card_respond(_out_card_player, _out_card_data);// ,
                                                                                                                      // EstimatKind.EstimatKind_OutCard

        // 如果没有需要操作的玩家，派发扑克
        if (bAroseAction == false) {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
                // table._playerStatus[i].clean_status();
            }

            table.operate_player_action(_out_card_player, true);
            
            if(!bXiaPai){
                table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
                        GameConstants.DELAY_SEND_CARD_DELAY);
            }


            // 发牌
            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
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
    public boolean handler_operate_card(MJTable_YYZXZ table, int seat_index, int operate_code, int operate_card) {
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
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            // 效果
            //table.process_chi_hu_player_operate(seat_index, new int[] { operate_card }, 1, false);

        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
            table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
        }

        // TODO 这里优先级高的才能胡
        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu())){
            	if(i == (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber())
            		return false;
            }
               
        }

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;

        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            // 获取动作
            int cbUserActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action);
                }

                // 优先级别
                int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                }
            }
        }
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return true;

        // 修改网络导致吃碰错误 9.26 WalkerGeek
        int target_card = _out_card_data;
        // error int target_card = table._playerStatus[target_player]._operate_card;

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

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

            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return true;
        }

        case GameConstants.WIK_GANG: // 杠牌操作
        case GameConstants.WIK_XIA_ZI_BU: // 杠牌操作
        {
            // 是否有抢杠胡
            table.exe_gang(target_player, _out_card_player, target_card, target_action,
                    GameConstants.GANG_TYPE_JIE_GANG, false, false);
            return true;
        }
        case GameConstants.WIK_NULL: {

            // 加到牌堆 没有人要
        	if(table._logic.checkWanZi(_out_card_data)){
                table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, false, 0);
        	}


            // 用户切换
            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();

            // 发牌
            table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            int jie_pao_count = 0;
            int hupai_index = seat_index;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                hupai_index = i;
                //checkHuIndex(_out_card_player, , hupai_index);
                jie_pao_count++;
            }
            if (jie_pao_count > 0) {

            	if(jie_pao_count > 1){
            		hupai_index = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
            	}
            	
                table._cur_banker = hupai_index;

                table.GRR._chi_hu_card[hupai_index][0] = _out_card_data;

                table.process_chi_hu_player_operate(hupai_index, new int[] { _out_card_data }, 1, false);
                
                table.process_chi_hu_player_score(hupai_index, _out_card_player, _out_card_data, false);


                // 记录
                table._player_result.jie_pao_count[hupai_index]++;
                table._player_result.dian_pao_count[_out_card_player]++;

                GameSchedule.put(
                        new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            }
            for(int i = 0; i < table.getTablePlayerNumber();i++){
            	if(i == hupai_index) continue;
            	table.GRR._chi_hu_rights[i].set_valid(false);
            }
         
            return true;
        }
        default:
            return false;
        }
    }
    
    
    @Override
	public boolean handler_player_be_in_room(MJTable_YYZXZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		tableResponse.setSendCardData(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			/*for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}*/
			for (int j = 0; j < 55; j++) {
	            if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
	                // 癞子
	                int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
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

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		//癞子标记
		for (int j = 0; j < hand_card_count; j++) {
            if (table._logic.is_magic_card(hand_cards[j])) {
            	hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
            }
        }
		
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

    /**
     * 检查胡牌的玩家 离放炮的玩家近 优先级比较高
     */
    private int checkHuIndex(int fang_pao_index, int index1, int index2) {
        if (fang_pao_index == 1) {
            // 离的远的那个是0 近的是3
            return index1 > index2 ? index1 : index2;
        }
        return index1 <= index2 ? index1 : index2;
    }

}
