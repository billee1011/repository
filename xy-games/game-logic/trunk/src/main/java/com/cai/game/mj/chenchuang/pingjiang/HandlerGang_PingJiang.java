package com.cai.game.mj.chenchuang.pingjiang;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_PING_JIANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

public class HandlerGang_PingJiang extends MJHandlerGang<Table_PingJiang> {

	protected int _seat_index;
	public int _provide_player;
	protected int _center_card;
	protected int _action;
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;
	protected int _type;

	public HandlerGang_PingJiang() {
	}

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		_self = self;
		_double = d;
	}

	@Override
	public void exe(Table_PingJiang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}
		//判断抢杠胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

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

	@Override
	public boolean exe_gang(Table_PingJiang table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table.operate_remove_discard(_provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以杠
					_provide_player = _seat_index;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data_bao_ting(table.GRR._cards_index[_seat_index], cards, _seat_index);
		for (int i = 0; i < hand_card_count; i++) {
            if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
                if (table._logic.is_magic_card(cards[i])) {
                    cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
        }
		
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		/*int cbGangIndex = */table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			//int score = 2;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

//				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
//				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
//			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score;
//			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -score;

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {// 放杠的人给分
			//可以杠没杠，碰过后再杠不分
			if(table.GRR._weave_items[_seat_index][cbWeaveIndex].is_vavild){
				//int score = 1;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index)
						continue;
//					table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;//
//					table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
				}
				table._player_result.ming_gang_count[_seat_index]++;
			}
		}

		
		if(_action == Constants_PING_JIANG.WIK_DA_MI_GANG){
			handler_operate_card(table, _seat_index, _action, _center_card);
		}else{
			// 从后面发一张牌给玩家
			table.exe_dispatch_card(_seat_index, Constants_PING_JIANG.HU_CARD_DA_MI_GSKH, 0);
		}
		return true;
	}

	@Override
	public boolean handler_operate_card(Table_PingJiang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if(_action != Constants_PING_JIANG.WIK_DA_MI_GANG){
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
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		boolean falg = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu())){
            	falg = true;
            	handler_operate_tong_pao(table, seat_index, operate_code, operate_card);
            	break;
            }
        }
		if(!falg){
			table._playerStatus[_seat_index].operate(operate_code, operate_card);
			if(operate_code == Constants_PING_JIANG.WIK_DA_MI_GANG){
				int get_bird_num = table.get_bird_num();
				int[] da_mi_card = new int[get_bird_num];
				for (int i = 0; i < get_bird_num; i++) {
					table._send_card_count++;
					int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
					--table.GRR._left_card_count;
					/*if (table.DEBUG_CARDS_MODE) {
						send_card_data = 0x14;
					}*/
					da_mi_card[i] = send_card_data;
					table.dami_cards.addLast(send_card_data+","+_seat_index);
				}
				table.operate_player_da_mi_card();//客户端显示到桌面
				int hu_count = 0;
				int hu_card = 0;
				int score = 0;
				for (int i = 0; i < da_mi_card.length; i++) {
					ChiHuRight chr = new ChiHuRight();
					int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], da_mi_card[i], chr, Constants_PING_JIANG.CHR_ZI_MO, _seat_index);
					if(action != GameConstants.WIK_NULL){
						hu_count++;
						if(table.GRR._left_card_count == 0 && i == 3){
							chr.opr_or(Constants_PING_JIANG.CHR_HAI_DI_PAI);
							table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_PING_JIANG.CHR_HAI_DI_PAI);
						}
						int paiXingScore = table.getPaiXingScore(chr);
						if(score == 0 || paiXingScore > score){
							score = paiXingScore;
							hu_card = da_mi_card[i];
						}
					}
				}
				if(hu_count > 0){
					table.GRR._chi_hu_rights[_seat_index].set_empty();
					table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], hu_card, table.GRR._chi_hu_rights[_seat_index], Constants_PING_JIANG.CHR_ZI_MO, _seat_index);
					playerStatus.clean_action();
					table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_PING_JIANG.CHR_DA_MI);
					if(hu_count == 2)
						table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_PING_JIANG.CHR_DA_MI_HU_2);
					if(hu_count == 3)
						table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_PING_JIANG.CHR_DA_MI_HU_3);
					playerStatus.add_action(GameConstants.WIK_ZI_MO);
					playerStatus.add_zi_mo(hu_card, _seat_index);
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index, false);
					/*table.GRR._win_order[_seat_index] = 1;
					table._cur_banker = _seat_index;
					table.set_niao_card();
					table.GRR._chi_hu_rights[_seat_index].set_valid(true);
					table.process_chi_hu_player_operate(_seat_index, hu_card, true);
					table.process_chi_hu_player_score(_seat_index, _seat_index, hu_card, true);
					table._player_result.zi_mo_count[_seat_index]++;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
							GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);*/
					return true;
				}else{
					GameSchedule.put(()->{
						table.is_da_mi_out_card = true;
						String[] out_card = table.dami_cards.getLast().split(",");
						table.dami_cards.removeLast();
						table.operate_player_da_mi_card();//客户端显示到桌面
						handler_player_out_card(table, _seat_index, Integer.valueOf(out_card[0]));
					},GameConstants.GAME_FINISH_DELAY_FLS, TimeUnit.SECONDS);
					return true;
				}
			}else{
				table.exe_dispatch_card(_seat_index, Constants_PING_JIANG.HU_CARD_DA_MI_GSKH, 0);
			}
		}
		return true;
	}
	
	@Override
	public boolean handler_player_out_card(Table_PingJiang table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(!table.is_da_mi_out_card){
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
		}

		table.exe_out_card(_seat_index, card, 0);

		return true;
	}
	
	private void handler_operate_tong_pao(Table_PingJiang table, int seat_index,
			int operate_code, int operate_card) {
		table._playerStatus[seat_index].operate(operate_code, operate_card);
		//如果可以胡没胡，过张处理
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(operate_card);
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}
        if (operate_code == GameConstants.WIK_CHI_HU || operate_code == GameConstants.WIK_ZI_MO) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            table.process_chi_hu_player_operate(seat_index, operate_card, false);// 效果
        }

        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                return;
        }

        // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        // 执行判断
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_seat_index + p) % table.getTablePlayerNumber();
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
                }
            }
        }
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return;
        
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }

        int jie_pao_count = 0;
        boolean is_da_hu = false;
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                continue;
            }
            if(table.is_big_hu(table.GRR._chi_hu_rights[i]))
            	is_da_hu = true;
            jie_pao_count++;
        }

        if (jie_pao_count > 0) {
            if (jie_pao_count > 1) {
                // 通炮
                table._cur_banker = _seat_index;
            } else if (jie_pao_count == 1) {
                table._cur_banker = target_player;
            }
            table.set_niao_card();
            if(table.GRR._chi_hu_rights[_seat_index].opr_and(Constants_PING_JIANG.CHR_ZI_MO).is_empty()){
            	table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_PING_JIANG.CHR_FANG_PAO);
            	if(is_da_hu)
            		table._player_result.da_hu_dian_pao[_seat_index]++;
            }
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }

                table.GRR._chi_hu_card[i][0] = operate_card;
                table.process_chi_hu_player_score(i, _seat_index, operate_card, _seat_index == seat_index);
                // 记录
                table._player_result.jie_pao_count[i]++;
                table._player_result.dian_pao_count[_provide_player]++;
            }

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

        } else {
        	if(_seat_index == seat_index){
        		GameSchedule.put(()->{
					table.is_da_mi_out_card = true;
					String[] out_card = table.dami_cards.getLast().split(",");
					table.dami_cards.removeLast();
					table.operate_player_da_mi_card();//客户端显示到桌面
					handler_player_out_card(table, _seat_index, Integer.valueOf(out_card[0]));
				},GameConstants.GAME_FINISH_DELAY_FLS, TimeUnit.SECONDS);
				return;
        	}else
        		this.exe_gang(table);
        }

        return;
		
	}

	@Override
	public boolean handler_player_be_in_room(Table_PingJiang table, int seat_index) {
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
			table.process_chi_hu_player_operate_reconnect(seat_index, _center_card, false); // 效果
		} else {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}
		table.operate_player_da_mi_card();//客户端显示到桌面
		return true;
	}

}
