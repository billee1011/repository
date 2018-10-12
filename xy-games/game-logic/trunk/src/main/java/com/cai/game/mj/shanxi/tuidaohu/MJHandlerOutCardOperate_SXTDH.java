package com.cai.game.mj.shanxi.tuidaohu;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_TDH;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

public class MJHandlerOutCardOperate_SXTDH extends MJHandlerOutCardOperate<MJTable_SXTDH> {

    @Override
    public void exe(MJTable_SXTDH table) {
        PlayerStatus playerStatus = table._playerStatus[_out_card_player];

        // 重置玩家状态
        table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
        playerStatus.clean_action();

        // 出牌记录
        table._out_card_count++;
        table._out_card_player = _out_card_player;
        table._out_card_data = _out_card_data;

        // 用户切换
        int next_player = (_out_card_player + GameConstants.GAME_PLAYER + 1) % GameConstants.GAME_PLAYER;
        table._current_player = next_player;

        // 刷新手牌
        int cards[] = new int[GameConstants.MAX_COUNT];

        // 刷新自己手牌
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);

        table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

        // 癞子
        int real_card = _out_card_data;
        // 显示出牌
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                GameConstants.INVALID_SEAT);

        table._provide_player = _out_card_player;
        table._provide_card = _out_card_data;

        // 加入牌队
        table.exe_add_discard(_out_card_player, 1, new int[] { real_card }, false,
        		GameConstants.DELAY_SEND_CARD_DELAY);
        // 玩家出牌 响应判断,是否有吃碰杠补胡
        boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data);

        // 如果没有需要操作的玩家，派发扑克
        if (bAroseAction == false) {
            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                table._playerStatus[i].clean_action();
                table.change_player_status(i, GameConstants.INVALID_VALUE);
            }

            table.operate_player_action(_out_card_player, true);


            // 发牌
            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
        } else {
            // 等待别人操作这张牌
            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                playerStatus = table._playerStatus[i];
                if (playerStatus.has_action()) {
                    table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    // 每一个Handler里面的这个方法，都会根据玩法规则不同和本身处理类型的不同而有所区别，比如发牌之后，玩家能对这张牌进行杠牌和胡牌操作
    // 比如玩家打出一张牌之后，其他玩家能对这张牌进行吃、碰、杠、胡操作。
    // 玩家在客户端点了弹出来得‘碰’、‘杠’、‘胡’、‘过’，之后就会进这里
    @Override
    public boolean handler_operate_card(MJTable_SXTDH table, int seat_index, int operate_code,
            int operate_card) {
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
        
        table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
				1);

		
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
			//能碰不碰的，加到废弃的碰的数组里面去，保证碰要过圈
			if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_PENG)) {
				table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
			}
		}
		//能胡不胡的，加到废弃的胡的数组里面去，保证胡要过圈
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU)
				&& operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
//			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            table.process_chi_hu_player_operate(seat_index, operate_card, true);
        } else if (operate_code == GameConstants.WIK_NULL) {
            table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
                    new long[] { GameConstants.WIK_NULL }, 1);
        }

        if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
            table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
        }
        
        for (int i = 0; i < GameConstants.GAME_PLAYER; i++) { 
        	  if((table._playerStatus[i].is_respone() == false) &&(table._playerStatus[i].has_chi_hu())) 
        		  return false; 
        }
         

        // 变量定义 优先级最高操作的玩家和操作
    	int target_player = seat_index;
		int target_action = operate_code;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
				}

				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		int target_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

        // 删除扑克
        switch (target_action) { 
        case GameConstants.WIK_PENG: // 碰牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "碰牌删除出错");
                return false;
            }
            
            //这句需要吗
            table.remove_discard_after_operate(_out_card_player, _out_card_data);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
        }
            break;
        case GameConstants.WIK_GANG: // 杠牌操作
        {
        	table.remove_discard_after_operate(_out_card_player, _out_card_data);
            // 是否有抢杠胡
            table.exe_gang(target_player, _out_card_player, target_card, target_action,
                    GameConstants.GANG_TYPE_JIE_GANG, false, false);
            return true;
        }
        case GameConstants.WIK_NULL: {
            int real_card = _out_card_data;

            // 加到牌堆 没有人要
            table.exe_add_discard(this._out_card_player, 1, new int[] { real_card }, false, 0);

            // 用户切换
            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();
            // 发牌
            table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

            return true;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
        	
        	 // 玩家出牌之后，有人胡牌，记录牌桌、出牌人以及胡牌人的状态信息
            table.GRR._chi_hu_rights[target_player].set_valid(true);
            
            //判断点炮的那个玩家是不是处于报听状态
            if(table._playerStatus[_out_card_player].is_bao_ting()){
            	table.BaoTingDianPaoVaild(_out_card_player);//设置打牌的玩家报听时候点炮的状态
            }else{
            	table.NoBaoTingDianPaoVaild(_out_card_player);//设置打牌的玩家未报听时候点炮的状态
            }
          //如果胡牌的人不是庄家，
			if(table._cur_banker!=target_player){
				table._cur_banker =  (table._cur_banker + (table.getTablePlayerNumber() +1)) % table.getTablePlayerNumber();	//若闲家胡牌则庄家的下家坐庄
			}else{
				table._cur_banker = target_player;//庄家胡牌则继续坐庄
			}
			
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}

			int jie_pao_count = 0;
			for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == true)) {
					jie_pao_count++;
				}
			}

			if (jie_pao_count > 0) {

				table._cur_banker = (table._cur_banker + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == table._cur_banker) {
						table._player_result.qiang[table._cur_banker] = table.continue_banker_count;
					} else {
						table._player_result.qiang[i] = 0;
					}
				}

				for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
					if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}

					table.GRR._chi_hu_card[i][0] = target_card;
					//吃胡时的方法
					//table.process_chi_hu_player_operate(i, operate_card, true);
					table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, Constants_SXTuiDaoHu.HU_CARD_TYPE_JIE_PAO,false);

					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_out_card_player]++;
					if (table.GRR._chi_hu_rights[i].da_hu_count > 0) {
						table._player_result.da_hu_jie_pao[i]++;
						table._player_result.da_hu_dian_pao[_out_card_player]++;
					} else {
						table._player_result.xiao_hu_jie_pao[i]++;
						table._player_result.xiao_hu_dian_pao[_out_card_player]++;
					}
					//table.GRR._chi_hu_rights[i].opr_and(Constants_SXTuiDaoHu.CHR_JIE_PAO);
		            //table.GRR._chi_hu_rights[_out_card_player].opr_and(Constants_SXTuiDaoHu.CHR_FANG_PAO);
				}

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants_TDH.Game_End_NORMAL),
						GameConstants_TDH.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
		}
        default:
            return false;
        }
        return true;
    }

    @Override
    public boolean handler_player_be_in_room(MJTable_SXTDH table, int seat_index) {
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

        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
            tableResponse.addTrustee(false);// 是否托管
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
                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
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
       /* tableResponse.setSendCardData(0);
        int cards[] = new int[GameConstants.MAX_COUNT];*/
        tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
        }
        roomResponse.setTable(tableResponse);
        table.send_response_to_player(seat_index, roomResponse);

        int real_card = _out_card_data;
        // 出牌
        table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
                seat_index);

        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        /**
         * // 听牌显示 int ting_cards[] = table._playerStatus[seat_index]._hu_cards; int
         * ting_count = table._playerStatus[seat_index]._hu_card_count;
         * 
         * if (ting_count > 0) { table.operate_chi_hu_cards(seat_index, ting_count,
         * ting_cards); }
         **/

        return true;
    }
}
