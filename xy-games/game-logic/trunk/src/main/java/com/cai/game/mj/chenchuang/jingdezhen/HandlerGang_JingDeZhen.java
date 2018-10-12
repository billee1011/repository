package com.cai.game.mj.chenchuang.jingdezhen;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_JING_DE_ZHEN;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

public class HandlerGang_JingDeZhen extends MJHandlerGang<Table_JingDeZhen> {

	protected int _seat_index;
	public int _provide_player;
	protected int _center_card;
	protected int _action;
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;
	protected int _type;

	public HandlerGang_JingDeZhen() {
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
	public void exe(Table_JingDeZhen table) {

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

		this.exe_gang(table);
		if (_action == Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG) {
			exe_gang_dispatch(table);
			return;
		}
		//判断抢杠胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			this.exe_gang_dispatch(table);
		} else {
			boolean flag = true;
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					if(table.is_bao_ding[i] == 2 || table.player_bai_count[i] > 0){
						if (table.numberOf1(table.setCanBaiPaiXing(table.GRR._chi_hu_rights[i], i)) >= table.player_bai_count[i]
								&& ((table.bai_pai_xing[i] & Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN) == 0 || !table.GRR._chi_hu_rights[i].opr_and_long(Constants_MJ_JING_DE_ZHEN.CHR_YAO_REN).is_empty())) {
							//handler_operate_card(table, i, GameConstants.WIK_CHI_HU, _center_card);
						} else {
							playerStatus.clean_action();
						}
					}
				}
				if(playerStatus.has_chi_hu()){
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
					flag = false;
				}
			}
			if(flag)
				this.exe_gang_dispatch(table);
		}
	}

	@Override
	public boolean exe_gang(Table_JingDeZhen table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			if(table._logic.is_magic_card(_center_card))
				table.out_bao_count[_seat_index] += 4;
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			if(table._logic.is_magic_card(_center_card))
				table.out_bao_count[_seat_index] += 3;
			if(table.is_bao_ding[_provide_player] != 2 && table.player_bai_count[_provide_player] == 0)
				table.cpg_count[_seat_index][_provide_player]++;
			
			// 别人打的牌
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table.operate_remove_discard(_provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			if(table._logic.is_magic_card(_center_card))
				table.out_bao_count[_seat_index] += 1;
			// 寻找组合 看看是不是有碰的牌，明杠
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以杠
					_provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
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
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		for (int i = 0; i < hand_card_count; i++) {
			int data = cards[i] > 3000 ? cards[i] - 3000 : cards[i];
            if (table._logic.is_magic_card(data)) {
                cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
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
		if(table.player_bai_count[_seat_index] > 0){
			table.operate_show_card(_seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		table.GRR._gang_score[_seat_index].gang_count++;

		return true;
	}

	private void exe_gang_dispatch(Table_JingDeZhen table) {
		if(_action == Constants_MJ_JING_DE_ZHEN.WIK_AN_GANG){
			table.an_gang_count[_seat_index]++;
			table._player_result.an_gang_count[_seat_index]++;
		}else if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			table.ming_gang_count[_seat_index]++;
			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			table.zhi_gang_count[_seat_index]++;
			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			table.wan_gang_count[_seat_index]++;
			table._player_result.ming_gang_count[_seat_index]++;
		}
		

		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, Constants_MJ_JING_DE_ZHEN.HU_CARD_TYPE_GSKH, 0);
	}

	@Override
	public boolean handler_operate_card(Table_JingDeZhen table, int seat_index, int operate_code, int operate_card) {
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

        if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU)){
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
        			new long[] { operate_code }, 1);
            table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
        }
        //如果可以胡没胡，过张处理
  		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
  			table._playerStatus[seat_index].add_cards_abandoned_hu(operate_card);
  			table._playerStatus[seat_index].chi_hu_round_invalid();
  			table.score_when_abandoned_jie_pao[seat_index] = table.getPaiXingScore(table.GRR._chi_hu_rights[seat_index], _seat_index);
  		}
        
		handler_operate_tong_pao(table, seat_index, operate_code, operate_card);
		return true;
	}

	private void handler_operate_tong_pao(Table_JingDeZhen table, int seat_index,
			int operate_code, int operate_card) {

        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            
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
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                continue;
            }
            jie_pao_count++;
        }

        if (jie_pao_count > 0) {
        	table.GRR._chi_hu_rights[_seat_index].opr_or_long(Constants_MJ_JING_DE_ZHEN.CHR_BEI_QIANG_GANG); 
        	if(jie_pao_count == 1){
            	if (table._cur_banker != target_player) {
    				//table.continueBankerCount = 0;
    			} else {
    				//table.continueBankerCount++;
    			}
            	table._cur_banker = target_player;
            	table.set_niao_card(target_player, false);
            }else if(jie_pao_count > 1){
            	boolean is_zhuang_hu = false;
            	int z = table.getTablePlayerNumber();
            	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
                        continue;
                    }
                    if(i == table._cur_banker)
                    	is_zhuang_hu = true;
                    int p = (i - _seat_index + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
                    if(p < z)
                    	z = p;
                }
            	if(is_zhuang_hu){
            		//table.continueBankerCount++;
            	}else{
            		//table.continueBankerCount = 0;
            		table._cur_banker = z;
            	}
            	table.set_niao_card(_seat_index, true);
            }

            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                table.process_chi_hu_player_operate(i, _center_card, false);// 效果
                table.GRR._chi_hu_card[i][0] = _center_card;

                table.process_chi_hu_player_score(i, _seat_index, _center_card, false);

                // 记录
                table._player_result.jie_pao_count[i]++;
                table._player_result.dian_pao_count[_provide_player]++;
            }

            GameSchedule.put(
                    new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                    1, TimeUnit.SECONDS);

        } else {
            this.exe_gang_dispatch(table);
        }

        return;
		
	}


	@Override
	public boolean handler_player_be_in_room(Table_JingDeZhen table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setZongliuzi(table.continueBankerCount == 0 ? -1 : table.continueBankerCount);
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

			//tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			
			
			boolean is_ming_pai = table.is_bao_ding[i] == 2 || table.player_bai_count[i] > 0;
			tableResponse.addTrustee(is_ming_pai);
			tableResponse.addWinnerOrder(is_ming_pai ? 1 : 0);
			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if(is_ming_pai || i == seat_index){
					int data = hand_cards[j] > 3000 ? hand_cards[j] - 3000 : hand_cards[j];
					if (table._logic.is_magic_card(data)) 
						tableResponse.addHuXi(hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					else
						tableResponse.addHuXi(hand_cards[j]);
				}else
					tableResponse.addHuXi(0);
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			int data = hand_cards[i] > 3000 ? hand_cards[i] - 3000 : hand_cards[i];
				if (table._logic.is_magic_card(data)) {
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
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

		return true;
	}

}
