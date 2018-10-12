package com.cai.game.gzp.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.gzp.GZPTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class GZPHandlerDispatchCard extends GZPHandler {
	protected int _seat_index;
	protected int _send_card_data;
	
	protected int _type;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public GZPHandlerDispatchCard(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int seat_index,int type){
		_seat_index = seat_index;
		_type= type;
	}
	
	@Override
	public void exe(GZPTable table) {
	}
	
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(GZPTable table,int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		boolean is_out = false;
		if((card&0x100)>>8 == 1)
		{
			card&=0xFF;
			is_out = true;
		}
		if(is_out == true && table._pick_up_index[seat_index][table._logic.switch_to_card_index(card)] == 0)
		{
			table.log_error("出捡牌,牌型出错");
			return false;
		}
		
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_info("状态不对不能出牌");
			return false;
		}
		if(is_out == false && table.GRR._cannot_out_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_info("当前牌不能出");
			return false;
		}
//		if (card == MJGameConstants.ZZ_MAGIC_CARD && table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
//			table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
//			table.log_error("癞子牌不能出癞子");
//			return false;
//		}

		// 删除扑克
		if(is_out == true)
		{
		
			table._pick_up_index[_seat_index][table._logic.switch_to_card_index(card)]--;
			table.operate_pick_up_card(_seat_index);
		}
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		//出牌
		table.exe_out_card(_seat_index,card,GameConstants.WIK_NULL);

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(GZPTable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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
			for (int j = 0; j < GameConstants.GZP_MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				for(int k = 0; k < table.GRR._weave_items[i][j].weave_card.length;k++)
				{
					weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
				}
				if(seat_index!=i) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			
			if(i == _seat_index){
				if(table._pu_count == 2)
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-2);
				else
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			}else{
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
		
		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		//如果断线重连的人是自己
		if(seat_index == _seat_index){
			if(table._pu_count == 2)
			{
				table._logic.remove_card_by_data(hand_cards, table._pu_card[0]);
				table._logic.remove_card_by_data(hand_cards, table._pu_card[1]);
			}
			else
				table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		
		//摸牌
		// 只有自己才有数值
		if((table.is_mj_type(GameConstants.GAME_TYPE_GZP)&&table.GRR._left_card_count > 3)||(table.is_mj_type(GameConstants.GAME_TYPE_GZP_DDWF)&&table.GRR._left_card_count > 5))
			if(table._pu_count == 2)
				table.operate_player_get_card(_seat_index, table._pu_count, table._pu_card, GameConstants.INVALID_SEAT,false,1);
			else
				table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data},seat_index,false,1);
		else
			table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data},seat_index,true,0);
		if(table._out_card_data >0){
			// 显示出牌
			table.operate_out_card(table._out_card_player, 1, new int[] { table._out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					seat_index,true);
		}
		
		if(table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone()==false)){
			table.operate_player_action(seat_index, false,false);
		}
		table.operate_cannot_card(seat_index);
		table.operate_pick_up_card(seat_index);
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.operate_player_connect_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
	
		return true;
	}

}
