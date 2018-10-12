package com.cai.game.gzp.handler.gzptc;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.game.gzp.GZPTable;
import com.cai.game.gzp.handler.GZPHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class GZPHandlerOutCardOperate_TC extends GZPHandlerOutCardOperate {
	@Override
	public void exe(GZPTable table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		// 重置玩家状态
		//playerStatus.clean_status();
		table.change_player_status(_out_card_player,GameConstants.INVALID_VALUE);
		playerStatus.clean_action();
	    Arrays.fill(table._temp_guan[_out_card_player], 0);
		Arrays.fill(table._guo_peng[_out_card_player],0); 
		Arrays.fill(table._guo_zhao[_out_card_player],0); 
		//
		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		table._pu_count = 1;
		table._pick_up_card = 0;
		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		// 刷新手牌
		int cards[] = new int[GameConstants.GZP_MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		table.operate_player_cards(_out_card_player, hand_card_count, cards, table.GRR._weave_count[_out_card_player], table.GRR._weave_items[_out_card_player]);

	
		// 检查听牌
		table._playerStatus[_out_card_player]._hu_card_count = table.get_gzp_ting_card_twenty(
				table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],_out_card_player,_out_card_player);

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

		// 玩家出牌 响应判断,是否可以捡牌
		boolean bAroseAction = table.estimate_player_out_card_pickpu(_out_card_player,  _out_card_data);// 
		table.estimate_player_hua(_out_card_player, _out_card_data,true);
		table.estimate_player_zhao(_out_card_player,_out_card_data);
		table.estimate_player_peng(_out_card_player,_out_card_data);
		// 显示出牌
		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT,bAroseAction);
		int cur_logic_index =  table._logic.switch_to_card_logic_index(_out_card_data);
		if(table._sheng_guan_index[_out_card_player][cur_logic_index] == 1)
		{
			table.cannot_outcard(_out_card_player, 1, _out_card_data, true);
		}
		//出牌后，删除不能出牌的捡牌
		table.cannot_pickup_card(_out_card_player, 0, 0, true);
	// EstimatKind.EstimatKind_OutCard
//		if (ting_count > 0) {
//			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
//		} else {
//			ting_cards[0] = 0;
//			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
//		}
		int loop = 0;
		while(loop < table.getTablePlayerNumber()){
			int i = (_out_card_player+loop) %table.getTablePlayerNumber() ; 
			loop ++;
			if(_out_card_player == i)
				continue;
			ChiHuRight chr = table.GRR._chi_hu_rights[i];
			chr.set_empty();
			if(table._guo_hu[i][0] != 0)
				continue;
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
					table.GRR._weave_count[i],table._sheng_guan_index[i],table._pick_up_index[i], _out_card_data, chr, card_type,i,_out_card_player);// 自摸


			if (action != GameConstants.WIK_NULL) {
				// 添加动作
				table._playerStatus[i].add_action(GameConstants.GZP_WIK_CHI_HU);
				table._playerStatus[i].add_chi_hu(_out_card_data, _out_card_data);

			} else {
				chr.set_empty();
			}		
			
		}	
		
		// 如果没有需要操作的玩家，派发扑克
		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i,GameConstants.INVALID_VALUE);
				//table._playerStatus[i].clean_status();
			}

			table.operate_player_action(_out_card_player, true);

			// 加入牌队 客户端250毫秒没有收到加入牌队。自己会添加一张牌到牌队
			int standTime = 400;
			int gameId=table.getGame_id()==0?5:table.getGame_id();
			SysParamModel sysParamModel104 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(gameId).get(1104);
			if(sysParamModel104!=null&&sysParamModel104.getVal5()>0&&sysParamModel104.getVal5()<1000) {
				standTime=sysParamModel104.getVal5();
			}
			
			table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
					standTime);

			// 发牌
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, standTime);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
				if (playerStatus.has_chi_hu()) {
					if ( table.isTrutess(i)) {
						// 见炮胡
						table.operate_player_action(i, false);
						table.exe_jian_pao_hu(i, GameConstants.GZP_WIK_CHI_HU, _out_card_data);
					} else {
						table.operate_player_action(i, false);
					}
				} else {
					if(table.isTrutess(i)) {
						table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _out_card_data);
					}else {
						table.operate_player_action(i, false);
					}
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
	public boolean handler_operate_card(GZPTable table, int seat_index, int operate_code, int operate_card) {
		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "GZPHandlerOutCardOperate_LX 出牌,玩家操作已失效");
			return true;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "GZPHandlerOutCardOperate_LX 出牌,玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			table.log_player_error(seat_index, "GZPHandlerOutCardOperate_LX 出牌操作,没有动作");
			return true;
		}
		table.cannot_hu_zhao_peng_card(seat_index,operate_card);
		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);
	
       if (operate_code == GameConstants.WIK_CHI_HU) {
            table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
           
         } else if (operate_code == GameConstants.WIK_NULL) {
            
            if (table._playerStatus[seat_index].has_chi_hu()) {
                table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
            }
        }
       if(playerStatus.has_chi_hu()){
           Arrays.fill(table._guo_hu[seat_index], 0);
			table._guo_hu[seat_index][0] = 1;
		}
	 	 // 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu())&&table.getRuleValue(GameConstants.GAME_RULE_TC_ONE_PAO_DUO_HU)==1)
                return false;
        }

   
		//执行操作的玩家没有被托管  已经响应    取消托管倒计时
		if( (!table.istrustee[seat_index] )&& table._trustee_schedule[seat_index]!=null){
			table._trustee_schedule[seat_index].cancel(false);
			table._trustee_schedule[seat_index] = null;
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

				// 优先级别
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

		// 变量定义
//		int target_card = table._playerStatus[target_player]._operate_card;

//		// 用户状态
//		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			table._playerStatus[i].clean_action();
//			//table._playerStatus[i].clean_status();
//			table.change_player_status(i,GameConstants.INVALID_VALUE);
//			table.operate_player_action(i, true);
//		}

		// 删除扑克
		switch (target_action) {
		case GameConstants.GZP_WIK_PICKUP:
		{
			table.exe_jian_card(target_player,_out_card_player,target_action,_out_card_data);
			return true;
		}
		case GameConstants.GZP_WIK_PENG: // 碰牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[2];
			table._logic.get_remove_cards(table.GRR._cards_index[target_player], table._pick_up_index[target_player], cbRemoveCard, 2, _out_card_data);
			boolean is_error = table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2);
			if(is_error == false)
			{
				table.log_player_error(target_player, "碰牌删除出错");
				return false;
				
			}
			int[] copy=	Arrays.copyOf(cbRemoveCard, 3);
			copy[copy.length-1]=_out_card_data;
			table.exe_chi_peng(target_player, _out_card_player, target_action, _out_card_data,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,copy);
			return true;
		}
		case GameConstants.GZP_WIK_ZHAO: // 观生操作
		{
			table.exe_gang(target_player, _out_card_player, _out_card_data, target_action, GameConstants.GZP_TYPE_ZHAO, false,
					false);
			return true;
		}
		case GameConstants.GZP_WIK_HUA:  // 滑操作
		{
			boolean flag = table._playerStatus[target_player].is_get_weave_card(GameConstants.GZP_WIK_HUA,operate_card);
			if(flag == false)
			{
				table.log_error(" 找不到可以操作的牌 target_player = "+target_player);
				return true;
			}
			boolean  is_flower = table._logic.is_card_flower(_out_card_data);
			boolean is_out_card = false;
			if(is_flower == true)
			{
				if(table._logic.switch_to_card_flower_index(operate_card) == table._logic.switch_to_card_index(_out_card_data))
					is_out_card = true;
			}
			if(operate_card == _out_card_data || is_out_card)
			{

				table.exe_gang(target_player, _out_card_player, operate_card, target_action, GameConstants.GZP_TYPE_HUA_OUT, false,
						false);
			}
			else 
			{
				table.exe_gang(target_player, _out_card_player, operate_card, target_action, GameConstants.GZP_TYPE_HUA_MO, false,
						false);
			}
		
			
				
			return true;
		}
		case GameConstants.WIK_NULL: {
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				//table._playerStatus[i].clean_status();
				table.change_player_status(i,GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}
			//add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			// 加到牌堆 没有人要
			table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, false, 0);

			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();

			// 发牌
			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: // 胡
		{
//			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//				if (i == target_player) {
//					table.GRR._chi_hu_rights[i].set_valid(true);
//				} else {
//					table.GRR._chi_hu_rights[i].set_valid(false);
//				}
//			}
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				//table._playerStatus[i].clean_status();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}
			table._banker_select = target_player;
			table._shang_zhuang_player = target_player;
			int hu_count = 0;
			table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_FANG_PAO , 1, new long[]{GameConstants.WIK_CHI_HU},
    				1, GameConstants.INVALID_SEAT);
			if(table.getRuleValue(GameConstants.GAME_RULE_TC_ONE_PAO_DUO_HU)==1)
			{
				
			    for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	                if ((i == _out_card_player) || (table.GRR._chi_hu_rights[i].is_valid() == false)) {
	                    continue;
	                }
	                hu_count ++;
	                ChiHuRight chr = table.GRR._chi_hu_rights[i];
	            	table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list,
	        				1, GameConstants.INVALID_SEAT);
	            	table.process_chi_hu_player_score_gzp(i, _out_card_player, _out_card_data, false);
	            	
	        		// 效果
	            	table._player_result.ming_tang_count[_out_card_player]++;
	            	table.GRR._chi_hu_card[i][0] = _out_card_data;

	                table.countChiHuTimes(i, false);
	            }
			    if(hu_count >= 2)
			    {
			    	table._banker_select = _out_card_player;
					table._shang_zhuang_player = _out_card_player;
			    }
			}
			else
			{
				table.process_chi_hu_player_operate(target_player, _out_card_data, false);
				table.process_chi_hu_player_score_gzp(target_player, _out_card_player, _out_card_data, false);
            	table.GRR._chi_hu_card[target_player][0] = _out_card_data;
            	table._player_result.ming_tang_count[_out_card_player]++;
                table.countChiHuTimes(target_player, false);
              
			}
			  
//			
//			table.process_chi_hu_player_score_gzp(target_player, _out_card_player, _out_card_data, false);

	
			

			
			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay+=table.GRR._chi_hu_rights[target_player].type_count-2;
			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		default:
			return false;
		}
	}

	@Override
	public boolean handler_player_be_in_room(GZPTable table, int seat_index) {
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
			for (int j = 0; j < GameConstants.GZP_MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (seat_index != i) {
					if (( table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_ZHAO)
							&& table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				for(int k = 0; k < table.GRR._weave_items[i][j].weave_card.length;k++)
				{
					weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
				}
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
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
		int hand_cards[] = new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _out_card_data;
		if (table._logic.is_magic_card(_out_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
		}
		
		boolean has_action = false;
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			has_action = true;
		}		
		
		boolean anyone_has_action = false;
		for(int i=0;i<table.getTablePlayerNumber();i++){
			if(table._playerStatus[i].has_action() && (table._playerStatus[i].is_respone() == false))
				anyone_has_action = true;
		}
	
		if(has_action)//该玩家有操作
			table.operate_player_action(seat_index, false,false);
		// table.operate_player_get_card(_seat_index, 1, new
		// int[]{_send_card_data});

		table.be_in_room_trustee(seat_index);
		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		table.operate_cannot_card(seat_index);
		table.operate_pick_up_card(seat_index);
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.operate_player_connect_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
		// 出牌
		if(real_card != 0)
			table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
				seat_index,anyone_has_action);
		return true;
	}
}
