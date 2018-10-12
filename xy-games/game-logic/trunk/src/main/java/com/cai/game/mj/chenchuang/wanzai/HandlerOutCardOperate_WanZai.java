package com.cai.game.mj.chenchuang.wanzai;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_WanZai;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.chenchuang.huarong.Table_HuaRong;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

public class HandlerOutCardOperate_WanZai extends MJHandlerOutCardOperate<Table_WanZai> {

	public int _out_card_player;
	public int _out_card_data;
	public int _type;

	@Override
	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	@Override
	public void exe(Table_WanZai table) {
		if(table.is_chase_vaild){
			if(_out_card_player == table._cur_banker){
				table.banker_out_card = _out_card_data;
				table.banker_out_card_count++;
			}else if(table.banker_out_card == _out_card_data){
				table.out_same_pai_count++;
			}
		}
		
		//清空出牌人的状态
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		//得到下家
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		//刷新手牌
		for (int i = 0; i < hand_card_count; i++) {
            if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
                if (table._logic.is_magic_card(cards[i])) {
                    cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
        }
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);
		//得到打出那些牌听牌
		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {//显示听牌
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;
		
		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);
		//检查其他玩家是否有碰杠
		boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);
		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}
			if(table.is_chase_vaild){
				if(table.out_same_pai_count == 3){
					for(int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i == table._cur_banker)
							continue;
						table.chase_score[i] += 1;
						table.chase_score[table._cur_banker] -= 1;
					}
					table.changeBiaoYan();
					
					table.operate_player_info();
					table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { 900 }, 1,
							GameConstants.INVALID_SEAT);
				}
			}
			table.operate_player_action(_out_card_player, true);
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			int hu = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
						hu++;
				}
			}
			if(hu >= 2){
				exe_tong_pao_hu(table);
				return;
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	private void exe_tong_pao_hu(Table_WanZai table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			PlayerStatus playerStatus = table._playerStatus[i];
			if (playerStatus.has_chi_hu()) {
				table.GRR._chi_hu_rights[i].set_valid(true);// 胡牌生效
			}
		}
        	
		table.GRR._chi_hu_rights[_out_card_player].opr_or(Constants_WanZai.CHR_FANG_PAO);
        table._cur_banker = _out_card_player;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                continue;
            }
            if(_type == Constants_WanZai.HU_CARD_TYPE_TIAN_HU){
            	ChiHuRight chiHuRight = table.GRR._chi_hu_rights[i];
				long[] pai_xing_score = table.get_pai_xing_score(chiHuRight,true);
				chiHuRight.set_empty();
				chiHuRight.opr_or_long(Constants_WanZai.CHR_DI_HU);
				if(pai_xing_score[0] > 10){
					chiHuRight.opr_or_long(pai_xing_score[1]);
				}
            }else if(_type == 3){
            	table.GRR._chi_hu_rights[i].opr_or(Constants_WanZai.CHR_GANG_SHANG_PAO);
            }
            table.GRR._chi_hu_rights[i].set_valid(true);
            // 效果
			table.process_chi_hu_player_operate(i, _out_card_data, false);
            table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false);

            // 记录
            table._player_result.jie_pao_count[i]++;
            table._player_result.dian_pao_count[_out_card_player]++;
        }

        GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker,
                GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
		
	}

	@Override
	public boolean handler_operate_card(Table_WanZai table, int seat_index, int operate_code, int operate_card) {
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
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
			if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_PENG)) {
				table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
			}
		}
		//如果可以胡没胡，过张处理
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

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
            int cbUserActionRank = 0;
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                } else {
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;
                    target_action = table._playerStatus[i].get_perform();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }

        if (table._playerStatus[target_player].is_respone() == false)
            return true;
        
        if (operate_code == GameConstants.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);
			table.GRR._win_order[seat_index] = 1;
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
		}

		int target_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		 case GameConstants.WIK_LEFT: {
	            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
	            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
	                table.log_player_error(seat_index, "吃牌删除出错");
	                return false;
	            }
	            table.remove_discard_after_operate(_out_card_player, _out_card_data);

	            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
	                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
	            return true;
	        }
	        case GameConstants.WIK_RIGHT: {
	            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
	            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
	                table.log_player_error(seat_index, "吃牌删除出错");
	                return false;
	            }

	            table.remove_discard_after_operate(_out_card_player, _out_card_data);

	            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
	                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
	            return true;
	        }
	        case GameConstants.WIK_CENTER: {
	            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
	            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
	                table.log_player_error(seat_index, "吃牌删除出错");
	                return false;
	            }


	            table.remove_discard_after_operate(_out_card_player, _out_card_data);

	            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
	                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
	            return true;
	        }
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants.WIK_NULL: {
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			if(table.is_chase_vaild){
				if(table.out_same_pai_count == 3){
					for(int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i == table._cur_banker)
							continue;
						table.chase_score[i] += 1;
						table.chase_score[table._cur_banker] -= 1;
					}
					table.changeBiaoYan();
					
					table.operate_player_info();
					table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { 900 }, 1,
							GameConstants.INVALID_SEAT);
				}
			}
			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table._cur_banker = target_player;
			if(_type == Constants_WanZai.HU_CARD_TYPE_TIAN_HU){
				ChiHuRight chiHuRight = table.GRR._chi_hu_rights[target_player];
				long[] pai_xing_score = table.get_pai_xing_score(chiHuRight,true);
				chiHuRight.set_empty();
				chiHuRight.opr_or_long(Constants_WanZai.CHR_DI_HU);
				if(pai_xing_score[0] > 10){
					chiHuRight.opr_or_long(pai_xing_score[1]);
				}
			}else if(_type == 3){
				table.GRR._chi_hu_rights[target_player].opr_or(Constants_WanZai.CHR_GANG_SHANG_PAO);
			}
			table.GRR._chi_hu_rights[target_player].set_valid(true);
			table.GRR._chi_hu_rights[_out_card_player].opr_or(Constants_WanZai.CHR_FANG_PAO);
			table.GRR._chi_hu_card[target_player][0] = target_card;
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
			table.process_chi_hu_player_score(target_player, _out_card_player, target_card, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;


			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}
		return true;
	}
	
	@SuppressWarnings("unused")
	private void handler_operate_tong_pao(Table_HuaRong table, int seat_index,
			int operate_code, int operate_card) {
		if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            // 效果
            table.process_chi_hu_player_operate(seat_index, operate_card, false);
        }

        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                return;
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
                int cbTargetActionRank = table._logic
                        .get_action_rank(table._playerStatus[target_player].get_perform());

                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                }
            }
        }
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return;

        int target_card = _out_card_data;

        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
            table.operate_player_action(i, true);
        }

        // 删除扑克
        switch (target_action) {
        case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return;
		}
        case GameConstants.WIK_NULL: {
        	 _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                     % table.getTablePlayerNumber();
             table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
             return;
        }
        case GameConstants.WIK_CHI_HU: // 胡
        {
            int jie_pao_count = 0;
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                jie_pao_count++;
            }

            if (jie_pao_count > 0) {
                if (jie_pao_count > 1) {
                    // 结束
                    table._cur_banker = _out_card_player;
                } else if (jie_pao_count == 1) {
                    table._cur_banker = target_player;
                }
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                        continue;
                    }
                    table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false);

                    // 记录
                    table._player_result.jie_pao_count[i]++;
                    table._player_result.dian_pao_count[_out_card_player]++;
                }

                GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker,
                        GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            }
            return;
        }
        default:
            return;
        }
		
	}

	@Override
	public boolean handler_player_be_in_room(Table_WanZai table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (hand_cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
                if (table._logic.is_magic_card(hand_cards[i])) {
                	hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _out_card_data, false); // 效果
		} else {
			// 听牌显示
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}

			// table.operate_out_card(_out_card_player, 1, new int[] {
			// _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}
}
