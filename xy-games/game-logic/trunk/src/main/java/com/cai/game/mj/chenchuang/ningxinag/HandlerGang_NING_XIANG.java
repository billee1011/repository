package com.cai.game.mj.chenchuang.ningxinag;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NING_XIANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerGang;

public class HandlerGang_NING_XIANG extends MJHandlerGang<Table_NING_XIANG> {

	protected int _seat_index;
	public int _provide_player;
	protected int _center_card;
	protected int _action;
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;
	protected int _type;

	public HandlerGang_NING_XIANG() {
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
	public void exe(Table_NING_XIANG table) {
		if(table.is_dao_pai[_seat_index])
			table.exe_qi_pai(_seat_index);

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
	}

	@Override
	public boolean exe_gang(Table_NING_XIANG table) {
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
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
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

		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			int score = 2;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
			}

			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {

			table._player_result.ming_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {// 放杠的人给分
			//可以杠没杠，碰过后再杠不分
			if(table.GRR._weave_items[_seat_index][cbWeaveIndex].is_vavild){
				table._player_result.ming_gang_count[_seat_index]++;
			}
		}

		// 从后面发一张牌给玩家
		if ((GameConstants.GANG_TYPE_AN_GANG == _type)) {
			if(_action == GameConstants.WIK_GANG){
				exe_gang2(table);
			}else
				table.exe_dispatch_card(_seat_index, 0, 0);
			return true;
		}
		//判断抢杠胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card, _action == GameConstants.WIK_GANG);
			
		if (bAroseAction == false) {
			if(_action == GameConstants.WIK_GANG){
				exe_gang2(table);
			}else
				table.exe_dispatch_card(_seat_index, 0, 0);
		} else {
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					if(table._player_result.biaoyan[i] == 1){
						handler_operate_card(table, i, GameConstants.WIK_CHI_HU, _center_card);
					}else{
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					}
				}
			}
		}
		

		return true;
	}
	
	public void exe_gang2(Table_NING_XIANG table) {
		if(exe_gang2_0(table))
			return;
		exe_gang2_2(table, 3);
	}
	public boolean exe_gang2_0(Table_NING_XIANG table) {
		// 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
        }
        
		Boolean flag = false;
		table.is_gang[_seat_index] = true;
		table._player_result.haspiao[_seat_index] = 1;
		table.operate_player_info();
		for (int i = 0; i < table.getKaiGangNum(); i++) {
			table._send_card_count++;
			int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			if(AbstractMJTable.DEBUG_CARDS_MODE)
				send_card_data = 0x22 + i;
			--table.GRR._left_card_count;
			table.gang_mo_cards[i] = send_card_data;
		}
		//客户端显示到桌面
		table.operate_out_card(_seat_index, table.getKaiGangNum(), table.gang_mo_cards,
                GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);
		int hu_card = 0;
		int hu_score = 0;
		for (int i = 0; i < table.getKaiGangNum(); i++) {
			ChiHuRight chr = new ChiHuRight();
			chr.opr_or_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA);
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], table.gang_mo_cards[i], chr, Constants_MJ_NING_XIANG.HU_CARD_TYPE_ZI_MO, _seat_index);
			if(action != GameConstants.WIK_NULL){
				int paiXingScore = table.getPaiXingScore(chr, _seat_index);
				if(paiXingScore > hu_score){
					hu_score = paiXingScore;
					hu_card = table.gang_mo_cards[i];
				}
			}
		}
		if(hu_score > 0){
			flag = true;
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			table.GRR._chi_hu_rights[_seat_index].opr_or_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA);
			table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], hu_card, table.GRR._chi_hu_rights[_seat_index], Constants_MJ_NING_XIANG.HU_CARD_TYPE_ZI_MO, _seat_index);
			table._playerStatus[_seat_index].add_action(GameConstants.WIK_CHI_HU);
			table._provide_card = hu_card;
			table._playerStatus[_seat_index].add_action_card(2, hu_card, GameConstants.WIK_CHI_HU, _seat_index);
			int[] cards_index = Arrays.copyOf(table.GRR._cards_index[_seat_index], table.GRR._cards_index[_seat_index].length);
			for (int i = 0; i < table.getKaiGangNum(); i++) {
				cards_index[table._logic.switch_to_card_index(table.gang_mo_cards[i])]++;
				if(table.is_zt_qi_shou_hu(cards_index, _seat_index, true)){
					break;
				}
				cards_index[table._logic.switch_to_card_index(table.gang_mo_cards[i])]--;
			}
			if(table._player_result.biaoyan[_seat_index] == 1){
				handler_operate_card(table, _seat_index, GameConstants.WIK_CHI_HU, hu_card);
			}else{
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			}
		}
		if(!flag && table.is_can_jia_jiang_jiang[_seat_index])
			table.is_jia_jiang_gang[_seat_index] = true;
		
		if(table.getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ONLY_GNAG_MENY) == 1 && exe_gang2_1(table))
			flag = true;
		return flag;
	}
	
	public boolean exe_gang2_1(Table_NING_XIANG table) {
		boolean flag = false;
		for (int j = 0; j < table.getKaiGangNum(); j++) {
			if(table.estimate_player_gang_card_respond(_seat_index, table.gang_mo_cards[j]))
				flag = true;
		}
		int[] arr = new int [table.getTablePlayerNumber()];
		for(int i = 0 ;i < table.getTablePlayerNumber() ;i++){
			if(i == _seat_index)
				continue;
			int hu_card1 = 0;
			int hu_score1 = 0;
			for (int j = 0; j < table.getKaiGangNum(); j++) {
				ChiHuRight chr = new ChiHuRight();
				chr.opr_or_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO);
				int action = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
						table.GRR._weave_count[i], table.gang_mo_cards[j], chr, Constants_MJ_NING_XIANG.HU_CARD_TYPE_DIAN_PAO, i);
				if(action != GameConstants.WIK_NULL){
					int paiXingScore = table.getPaiXingScore(chr, _seat_index);
					if(paiXingScore > hu_score1){
						hu_score1 = paiXingScore;
						hu_card1 = table.gang_mo_cards[j];
					}
				}
			}
			if(hu_score1 > 0){
				flag = true;
				arr[i] = hu_card1;
				table.GRR._chi_hu_rights[i].set_empty();
				table.GRR._chi_hu_rights[i].opr_or_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO);
				table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
						table.GRR._weave_count[i], hu_card1, table.GRR._chi_hu_rights[i], Constants_MJ_NING_XIANG.HU_CARD_TYPE_DIAN_PAO, i);
				table._playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				table._playerStatus[i].add_chi_hu(hu_card1, _seat_index);
			}	
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			PlayerStatus playerStatus = table._playerStatus[i];
			if (playerStatus.has_action()) {
				if((playerStatus.has_chi_hu() && table._player_result.biaoyan[i] == 1)){
					handler_operate_card(table, i, GameConstants.WIK_CHI_HU, arr[i]);
				}else{
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
		return flag;
	}
	public void exe_gang2_2(Table_NING_XIANG table, int seconds) {
		//客户端显示到桌面
		GameSchedule.put(()->{
			table.operate_out_card(_seat_index, 0, null,
	                GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);
			table.exe_add_discard(_seat_index, table.getKaiGangNum(), table.gang_mo_cards, true, GameConstants.DELAY_SEND_CARD_DELAY);
			table.gang_mo_cards = new int[table.getKaiGangNum()];
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		},seconds, TimeUnit.SECONDS);
	}
	

	@Override
	public boolean handler_operate_card(Table_NING_XIANG table, int seat_index, int operate_code, int operate_card) {
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
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		if(operate_code == Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN){
			// 用户状态
	        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	        	if(i == _seat_index)
	        		continue;
	            table._playerStatus[i].clean_action();
	            table._playerStatus[i].clean_status();

	            table.operate_player_action(i, true);
	        }
	        
			table._player_result.an_gang_count[seat_index]++;
			table._playerStatus[seat_index].clean_action(Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN);
			int qi_shou_hu_score = 0;
			int qi_shou_hu_count = 0;
			Map<Long, Integer> ztqi_shou_hu = table.has_ztqi_shou_hu_type[seat_index];
			for(Entry<Long, Integer> entry : table.ztqi_shou_hu_type[seat_index].entrySet()){
				Long key = entry.getKey();
				if(key != Constants_MJ_NING_XIANG.CHR_ZT_LIU_LIU_SHUN)
					continue;
				qi_shou_hu_count += entry.getValue();
				qi_shou_hu_score +=  entry.getValue() * table.getBottomScore();
				table.lls_card[seat_index].addAll(table.qslls_card[seat_index]);
				if(ztqi_shou_hu.containsKey(key)){
					ztqi_shou_hu.put(key, ztqi_shou_hu.get(key) + entry.getValue());
				}else{
					ztqi_shou_hu.put(key, entry.getValue());
				}
				table.qsh_cards_index = new int[table.getTablePlayerNumber()][GameConstants.MAX_INDEX];
				for (int card : table.qslls_card[seat_index]) {
					table.qsh_cards_index[seat_index][table._logic.switch_to_card_index(card)] = 3;
				}
			}
			table.zt_qsh_score(seat_index, qi_shou_hu_score, qi_shou_hu_count, Constants_MJ_NING_XIANG.CHR_ZT_LIU_LIU_SHUN);
			return true;
		}else if(operate_code == Constants_MJ_NING_XIANG.WIK_ZT_SI_XI){
			// 用户状态
	        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	        	if(i == _seat_index)
	        		continue;
	            table._playerStatus[i].clean_action();
	            table._playerStatus[i].clean_status();

	            table.operate_player_action(i, true);
	        }
	        
			table._player_result.an_gang_count[seat_index]++;
			table._playerStatus[seat_index].clean_action(Constants_MJ_NING_XIANG.WIK_ZT_SI_XI);
			int qi_shou_hu_score = 0;
			int qi_shou_hu_count = 0;
			Map<Long, Integer> ztqi_shou_hu = table.has_ztqi_shou_hu_type[seat_index];
			for(Entry<Long, Integer> entry : table.ztqi_shou_hu_type[seat_index].entrySet()){
				Long key = entry.getKey();
				if(key != Constants_MJ_NING_XIANG.CHR_ZT_SI_XI)
					continue;
				qi_shou_hu_count += entry.getValue();
				qi_shou_hu_score +=  entry.getValue() * table.getBottomScore();
				table.sx_card[seat_index].addAll(table.qssx_card[seat_index]);
				if(ztqi_shou_hu.containsKey(key)){
					ztqi_shou_hu.put(key, ztqi_shou_hu.get(key) + entry.getValue());
				}else{
					ztqi_shou_hu.put(key, entry.getValue());
				}
				table.qsh_cards_index = new int[table.getTablePlayerNumber()][GameConstants.MAX_INDEX];
				for (int card : table.qssx_card[seat_index]) {
					table.qsh_cards_index[seat_index][table._logic.switch_to_card_index(card)] = 4;
				}
			}
			table.zt_qsh_score(seat_index, qi_shou_hu_score, qi_shou_hu_count, Constants_MJ_NING_XIANG.CHR_ZT_SI_XI);
			return true;
		}else if(operate_code == GameConstants.WIK_CHI_HU && seat_index == _seat_index){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			// 用户状态
	        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	            table._playerStatus[i].clean_action();
	            table._playerStatus[i].clean_status();

	            table.operate_player_action(i, true);
	        }
			table.set_niao_card(_seat_index, false);
            table._cur_banker = _seat_index;
            table.process_chi_hu_player_operate(_seat_index, operate_card, false);// 效果
            table.GRR._chi_hu_card[_seat_index][0] = operate_card;

            table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);
            table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		handler_operate_tong_pao(table, seat_index, operate_code, operate_card);
		return true;
	}

	
	private void handler_operate_tong_pao(Table_NING_XIANG table, int seat_index,
			int operate_code, int operate_card) {
		//如果可以胡没胡，过张处理
		if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(operate_card);
			table._playerStatus[seat_index].chi_hu_round_invalid();
			table.score_when_abandoned_jie_pao[seat_index] = table.getPaiXingScore(table.GRR._chi_hu_rights[seat_index], seat_index);
		}
        if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            	if(!table._playerStatus[i].is_respone() && table._playerStatus[i].has_action() && !table._playerStatus[i].has_chi_hu()){
            		table._playerStatus[i].clean_action();
            		table.operate_player_action(i, true);
            	}
            }
        }

        // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                return;
        }

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
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    cbTargetActionRank = table.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    cbTargetActionRank = table.get_action_list_rank(
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
        // 优先级最高的人还没操作
        if (table._playerStatus[target_player].is_respone() == false)
            return;
        
        // 用户状态
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table.operate_player_action(i, true);
        }
        
        operate_card = table._playerStatus[target_player]._operate_card;
        //////////
        // 删除扑克
        switch (target_action) {
        case GameConstants.WIK_LEFT: {
            int cbRemoveCard[] = new int[] { operate_card + 1, operate_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return ;
            }
            otherOperate(table, operate_card);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return ;
        }
        case GameConstants.WIK_RIGHT: {
            int cbRemoveCard[] = new int[] { operate_card - 1, operate_card - 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return ;
            }

            otherOperate(table, operate_card);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return ;
        }
        case GameConstants.WIK_CENTER: {
            int cbRemoveCard[] = new int[] { operate_card - 1, operate_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(seat_index, "吃牌删除出错");
                return ;
            }

            otherOperate(table, operate_card);

            table.exe_chi_peng(target_player, _seat_index, target_action, operate_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD);
            return ;
        }
        case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { operate_card, operate_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return;
			}
			otherOperate(table, operate_card);
			table.exe_chi_peng(target_player, _seat_index, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return;
		}
		case GameConstants.WIK_GANG: {
			otherOperate(table, operate_card);
			table.exe_gang(target_player, _seat_index, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return;
		}
		case Constants_MJ_NING_XIANG.WIK_BU_ZHANG: {
			otherOperate(table, operate_card);
			table.exe_gang(target_player, _seat_index, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return;
		}
        case GameConstants.WIK_NULL: {
        	if(table.gang_mo_cards[0] > 0){
        		exe_gang2_2(table, 1);
        	}else{
        		if(_action == GameConstants.WIK_GANG){
        			exe_gang2(table);
        		}else
        			table.exe_dispatch_card(_seat_index, 0, 0);
        	}
        	return;
        }
        case GameConstants.WIK_CHI_HU:{
        	int jie_pao_count = 0;
        	for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                    continue;
                }
                jie_pao_count++;
            }

            if (jie_pao_count > 0) {
                if (jie_pao_count > 1) {
                	table.set_niao_card(_seat_index, false);
                    table._cur_banker = _seat_index;
                } else if (jie_pao_count == 1) {
                    table._cur_banker = target_player;
                    table.set_niao_card(target_player, false);
                }
                if(target_player == _seat_index){
                	//table.GRR._chi_hu_rights[_seat_index].opr_or_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA);
                }else{
                	if(_action == GameConstants.WIK_GANG)
                		table.GRR._chi_hu_rights[_seat_index].opr_or_long(Constants_MJ_NING_XIANG.CHR_BEI_QIANG_GANG);
                	else
                		table.GRR._chi_hu_rights[_seat_index].opr_or_long(Constants_MJ_NING_XIANG.CHR_FANG_PAO);
                }
                for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                    if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
                        continue;
                    }
                    operate_card = table._playerStatus[i]._operate_card;
                    table.process_chi_hu_player_operate(i, operate_card, false);// 效果
                    table.GRR._chi_hu_card[i][0] = operate_card;

                    table.process_chi_hu_player_score(i, _seat_index, operate_card, false);

                    // 记录
                    table._player_result.jie_pao_count[i]++;
                    table._player_result.dian_pao_count[_seat_index]++;
                }

                GameSchedule.put(
                        new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
                        GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

            }
            return;
        }
        default:
            return;
        }
	}

	@Override
	public boolean handler_player_be_in_room(Table_NING_XIANG table, int seat_index) {
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
		
		//起手胡重连
		table.reconnectionQiShouHu();

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
		table.operate_out_card(_seat_index, table.getKaiGangNum(), table.gang_mo_cards,
                GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);
		return true;
	}
	
	public void otherOperate(Table_NING_XIANG table,int card){
		table.operate_out_card(_seat_index, 0, null,
                GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);
		int[] arr = new int[table.gang_mo_cards.length];
		boolean flag = true;
		int i = 0;
		for(int c : table.gang_mo_cards){
			if(c == card && flag){
				flag = false;
				continue;
			}
			arr[i] = c;
			i++;
		}
		
		table.exe_add_discard(_seat_index, table.getKaiGangNum() - 1, arr, true, GameConstants.DELAY_SEND_CARD_DELAY);
		table.gang_mo_cards = new int[table.getKaiGangNum()];
	}

}
