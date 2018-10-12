package com.cai.game.phu.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.phu.PHTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class PHHandlerDispatchCard<T extends PHTable> extends PHHandler<T>{
	protected int _seat_index;
	protected int _send_card_data;
	
	protected int _type;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public PHHandlerDispatchCard(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int seat_index,int type){
		_seat_index = seat_index;
		_send_card_data = 0;
		_type= type;
	}
	
	@Override
	public void exe(PHTable table) {
	}
	
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(T table,int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		
		if(table.GRR == null)
			return false;
		if(seat_index<0 || seat_index >= table.getTablePlayerNumber())
			return false;
		if (table._logic.is_valid_card(card) == false) {
//			int cards[] = new int[GameConstants.MAX_HH_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
//			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index],table.GRR._weave_items[seat_index],100);
//			table.operate_minus_discard(seat_index, table.GRR._discard_count[seat_index], table.GRR._discard_cards[seat_index] ,false);
//			
			handler_player_be_in_room( table,  seat_index);
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
//			int cards[] = new int[GameConstants.MAX_HH_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
//			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index],table.GRR._weave_items[seat_index],100);
//			table.operate_minus_discard(seat_index, table.GRR._discard_count[seat_index], table.GRR._discard_cards[seat_index] ,false);
			handler_player_be_in_room( table,  seat_index);
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
//			int cards[] = new int[GameConstants.MAX_HH_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
//			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index],table.GRR._weave_items[seat_index],100);
//			table.operate_minus_discard(seat_index, table.GRR._discard_count[seat_index], table.GRR._discard_cards[seat_index] ,false);
			handler_player_be_in_room( table,  seat_index);
			table.log_error("状态不对不能出牌");
			return false;
		}
//		if (card == MJGameConstants.ZZ_MAGIC_CARD && table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
//			table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
//			table.log_error("癞子牌不能出癞子");
//			return false;
//		}
		if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(card)] >= 3)
		{
			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			// 显示出牌
//			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_MID,
//					GameConstants.INVALID_SEAT);
			// 刷新自己手牌
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index],100);
//			table.operate_minus_discard(seat_index, table.GRR._discard_count[seat_index], table.GRR._discard_cards[seat_index] ,false);
			handler_player_be_in_room( table,  seat_index);
			table.log_error(_seat_index+"出牌出错 HHHandlerDispatchCard "+_seat_index);
			return true;
		}
		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
//			int cards[] = new int[GameConstants.MAX_HH_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
//			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index],table.GRR._weave_items[seat_index],100);
//			table.operate_minus_discard(seat_index, table.GRR._discard_count[seat_index], table.GRR._discard_cards[seat_index] ,false);
			handler_player_be_in_room( table,  seat_index);
			table.log_error("出牌删除出错");
			return false;
		}

		//出牌
		table.exe_out_card(_seat_index,card,GameConstants.WIK_NULL);

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(T table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		for(int i = 0; i<table.getTablePlayerNumber();i++)
			roomResponse.addScore(table._game_mid_score[i]);
		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		
		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(table._banker_count[table.GRR._banker_player]);

		// 状态变量
		tableResponse.setActionCard(0);
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			tableResponse.addTrustee(false);// 是否托管
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
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				if(seat_index!=i) {
					if((table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_TI_LONG || table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_AN_LONG
							|| table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_AN_LONG_LIANG) &&table.GRR._weave_items[i][j].public_card==0) {
						weaveItem_item.setCenterCard(0);
					}else {
						if(table.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)&&table.has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
								&&table._xt_display_an_long[i] ==true)
							weaveItem_item.setCenterCard(0);
						else
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._game_mid_score[i]);
			if(table._warning[i] == 2)
				tableResponse.addTrustee(true);
			else
				tableResponse.addTrustee(false);
			// 牌
			
			if(i == _seat_index){
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			}else{
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		boolean flag = false;
		boolean b_display = false;
		for(int i = 0; i< table.getTablePlayerNumber();i++)
		{
			if(table._playerStatus[i].has_action())
			{
				b_display = true;
				break;
			}
		}
		if(table.GRR._left_card_count == 23)
		{
			if(seat_index == _seat_index){
				table._logic.remove_card_by_data(hand_cards, _send_card_data);
			}
			if(seat_index == _seat_index){
				if(_send_card_data != 0)
				{
					if(b_display)
					{
						table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index,false,1);
						//table.log_info("connect b_disPlayer "+b_display + "seat_index = "+this._seat_index+"card = "+_send_card_data);
					}
					else
					{
						table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index,false,0);
						//table.log_info("connect b_disPlayer "+b_display + "seat_index = "+this._seat_index+"card = "+_send_card_data);
					}
				}
				
			}
			else{
				if(_send_card_data != 0)
				{
					if(b_display)
					{
						table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index,false,1);
					}
					else
					{
						table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index,false,0);
					}
				}
				
			}
			
			flag = true;
		}
				
			
		
		//如果断线重连的人是自己
//		if(seat_index == _seat_index){
//			if(!((seat_index == table._current_player) && (_send_card_data == 0)))
//				table._logic.remove_card_by_data(hand_cards, _send_card_data);
//		}
		
		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		
		table.send_response_to_player(seat_index, roomResponse);
		
		//摸牌

		if((_send_card_data != 0)&&(flag == false))
		{
			if(b_display)
			{
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index,false,1);
				//table.log_info("connect b_disPlayer "+b_display + "seat_index = "+this._seat_index+"card = "+_send_card_data);
			}
			else
			{
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index,false,0);
			    //table.log_info("connect seat_index "+_seat_index + "card" +_send_card_data);
			}
		}
		
		if(table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone()==false)){
			table.operate_player_action(seat_index, false);
		}
		if(table._warning[seat_index] == 1)
		{
			table.operate_is_warning(seat_index,false);
		}
		table.operate_player_timer(false);

//		if(table._warning[seat_index] == 2)
//			table.operate_player_xiang_gong_flag(seat_index,table._is_xiang_gong[seat_index]);
		return true;
	}

}
