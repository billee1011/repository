package com.cai.game.chdphz.handler.dhd;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.chdphz.CHDPHZTable;
import com.cai.game.chdphz.handler.PHZHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class DHDHandlerOutCardOperate_CHD extends PHZHandlerOutCardOperate<CHDPHZTable> {
	@Override
	public void exe(CHDPHZTable table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		table._playerStatus[i].clean_action();
		table._playerStatus[i].clean_status();
		
	}
		if(table.GRR._cards_index[_out_card_player][table._logic.switch_to_card_index(_out_card_data)] >= 3)
		{
			table.log_info(_out_card_player+"出牌出错 HHHandlerOutCardOperate_YX "+_out_card_data);
			return ;
		}
		//
		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		table._last_card = _out_card_data;
		table._cannot_chi[_out_card_player][table._cannot_chi_count[_out_card_player]++] = _out_card_data;
		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_HH_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		table.operate_player_cards(_out_card_player, hand_card_count, cards, table.GRR._weave_count[_out_card_player],table.GRR._weave_items[_out_card_player]);

		// 显示出牌
		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);


		
		

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;
		table._playerStatus[_out_card_player]._hu_card_count = table.get_hh_ting_card_twenty(
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
		//// 玩家出牌 响应判断,是否有跑 ，提， 扫   swe 
		//int pao_type[] = new int[1];
		int ti_pao =  GameConstants.WIK_NULL;
		for(int i = 0; i<GameConstants.GAME_PLAYER_HH;i++)
		{
			if(i==_out_card_player) continue;
			int pao_type[] = new int[1];
			ti_pao = table.estimate_player_respond_phz_chd(i,_out_card_player ,_out_card_data, pao_type,false);
			if(ti_pao != GameConstants.WIK_NULL){
				table.exe_gang(i, _out_card_player, _out_card_data, ti_pao,pao_type[0], false,true, false,1000);
				return ;
			}
			

		}


		// 玩家出牌 响应判断,是否有吃,碰,胡  swe  
		boolean bAroseAction = false;
		
		bAroseAction = table.estimate_player_out_card_respond_hh(_out_card_player,_out_card_data,false);// ,
		
																													// EstimatKind.EstimatKind_OutCar
		
		// 如果没有需要操作的玩家，派发扑克
		
		if (bAroseAction == false &&ti_pao == GameConstants.WIK_NULL) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
			}
			
			table.operate_player_action(_out_card_player, true);
			table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = _out_card_data;
			//
			table.operate_out_card(_out_card_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
			// 加入牌队 客户端250毫秒没有收到加入牌堆。自己会添加一张牌到牌队
			int discard_time = 2000;
			int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1105);
			if (sysParamModel1105 != null && sysParamModel1105.getVal1() > 0 && sysParamModel1105.getVal1() < 10000) {
				discard_time = sysParamModel1105.getVal1();
			}
			table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, true,discard_time);

			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			_out_card_data = 0;
			table._last_player = _current_player;
			int dispatch_time = 3000;
			if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
				dispatch_time = sysParamModel1105.getVal2();
			}
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				 if (table._playerStatus[i].has_action()) {
				    table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
				 //操作状态
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
	 * @param luoCode
	 * @return
	 */
	@Override
	
	public boolean handler_operate_card(CHDPHZTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		// 效验状态

		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HHHandlerOutCardOperate_YX 出牌,玩家操作已失效");
			return true;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "HHHandlerOutCardOperate_YX 出牌,玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			table.log_player_error(seat_index, "HHHandlerOutCardOperate_YX 出牌操作,没有动作");
			return true;
		}
		if((operate_card != table._out_card_data))
		{
			table.log_player_error(seat_index,"HHHandlerOutCardOperate_YX 操作牌，与当前牌不一样");
			return true;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}
		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);
		
		
		//吃操作后，是否有落
		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS:
			if(luoCode != -1)
				playerStatus.set_lou_pai_kind(luoCode);
		}
		
		
//		if (operate_code == GameConstants.WIK_CHI_HU) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
//			// 效果
//			// table.process_chi_hu_player_operate_cs(seat_index,operate_card,1,false);
//
//		} else if (operate_code == GameConstants.WIK_NULL) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
//			if (table._playerStatus[seat_index].has_chi_hu()) {
//				table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
//			}
//		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[3];
		int cbMaxActionRand = 0;
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
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
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
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			return true;
		}
	
		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
	
		//判断可不可以吃的上家用户
		
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS;
		if(target_action == GameConstants.WIK_NULL)
		{
			//过张的牌都不可以吃
			int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = target_card;
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if(table._playerStatus[i]._action[j] == GameConstants.WIK_PENG)
					{
						table._cannot_peng[i][table._cannot_peng_count[i]++] = target_card;
					}
				}
			}
		}
		if((target_action & eat_type) != GameConstants.WIK_NULL)
		{
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
				 for(int j = 0; j< table._playerStatus[i]._action_count;j++)
				 {
					if(table._playerStatus[i]._action[j] == GameConstants.WIK_PENG)
					{
						table._cannot_peng[i][table._cannot_peng_count[i]++] = target_card;
					}
				}
			}
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            boolean flag_temp = false;
				
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
			table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,table._lou_weave_item[target_player][0]);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
	
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
			table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,table._lou_weave_item[target_player][2]);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
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
			table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,table._lou_weave_item[target_player][1]);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			return true;
		}
		case GameConstants.WIK_XXD://吃小
		{
			// 删除扑克
			int target_card_color = table._logic.get_card_color(target_card);
			
			int cbRemoveCard[] = new int[2] ;
			if(target_card_color == 0)
			{
				cbRemoveCard[0] = target_card;
				cbRemoveCard[1] = target_card+16;
			}
			else
			{
				cbRemoveCard[0] = target_card-16;
				cbRemoveCard[1] = target_card-16;
			}
			table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,table._lou_weave_item[target_player][4]);
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			return true;
		}
		case GameConstants.WIK_DDX://吃大
		{
			// 删除扑克
			// 删除扑克
			int target_card_color = table._logic.get_card_color(target_card);
			
			int cbRemoveCard[] = new int[2] ;
			if(target_card_color == 0)
			{
				cbRemoveCard[0] = target_card+16;
				cbRemoveCard[1] = target_card+16;
			}
			else
			{
				cbRemoveCard[0] = target_card-16;
				cbRemoveCard[1] = target_card;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,table._lou_weave_item[target_player][5]);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			return true;
		}
		case GameConstants.WIK_EQS://吃二七十
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card , target_card };
			int target_card_value = table._logic.get_card_value(target_card);
			switch (target_card_value) {
			case 2:
				cbRemoveCard[0] = target_card+5;
				cbRemoveCard[1] = target_card+8;
				break;
			case 7:
				cbRemoveCard[0] = target_card-5;
				cbRemoveCard[1] = target_card+3;
				break;
			case 10:
				cbRemoveCard[0] = target_card-8;
				cbRemoveCard[1] = target_card-3;
				break;

			default:
				break;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错"+target_card);
				return false;
			}
			table.add_lou_weave(target_lou_code,target_player,target_card,_out_card_player,table._lou_weave_item[target_player][3]);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
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
					GameConstants.CHI_PENG_TYPE_OUT_CARD,target_lou_code);
			return true;
		}

		case GameConstants.WIK_NULL: {

			// 加到牌堆 没有人要
			
			// 显示出牌
			table.operate_out_card(_out_card_player, 0,null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
			table.exe_add_discard(this._out_card_player, 1, new int[] { this._out_card_data }, true, 0);

			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			

			// 发牌
			table._last_player = _current_player;
			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 500);
				
			_out_card_data = 0;

			return true;
		}
		case GameConstants.WIK_CHI_HU: // 胡
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}
			table._cur_banker = target_player;

			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
				if (table.GRR._banker_player == target_player) {
					table._cur_banker = target_player;
				} else {
					table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
							% table.getTablePlayerNumber();
				}

			}
			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score_hh(target_player, _out_card_player, _out_card_data, false);

			// 记录
			// table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;
			table.countChiHuTimes(target_player, false);

			
			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay+=table.GRR._chi_hu_rights[target_player].type_count-2;
			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		default:
			return false;
		}
	}

	@Override
	public boolean handler_player_be_in_room(CHDPHZTable table, int seat_index) {
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
		table.istrustee[seat_index]=false;
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
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_ZHAO)
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
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _out_card_data;
		if (table._logic.is_magic_card(_out_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
		}
		// 出牌
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
				seat_index);

		if(table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index,table._is_xiang_gong[seat_index]);
		// table.operate_player_get_card(_seat_index, 1, new
		// int[]{_send_card_data});
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
	

		return true;
	}
}
