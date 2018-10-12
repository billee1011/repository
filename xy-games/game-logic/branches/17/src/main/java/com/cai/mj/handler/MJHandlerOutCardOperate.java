package com.cai.mj.handler;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.mj.MJTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate extends MJHandler {

	public int _out_card_player = MJGameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = MJGameConstants.INVALID_VALUE; // 出牌扑克
	public int _type;
	
	
	protected int _current_player =MJGameConstants.INVALID_SEAT; 
	
	
	public void reset_status(int seat_index,int card,int type){
		_out_card_player = seat_index;
		_out_card_data = card;
		_type= type;
	}
	
	
	@Override
	public void exe(MJTable table) {
	}
	
	
	
	
	@Override
	public boolean handler_player_be_in_room(MJTable table,int seat_index) {
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
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
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
			for (int j = 0; j < MJGameConstants.MAX_WEAVE; j++) {
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
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < MJGameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		
		//出牌
		table.operate_out_card(_out_card_player, 1, new int[]{_out_card_data},MJGameConstants.OUT_CARD_TYPE_MID,seat_index);
		
		//table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
		if(table._playerStatus[seat_index].has_action()){
			table.operate_player_action(seat_index, false);
		}
		
		return true;
	}
	
	protected void exe_chi_peng(MJTable table,int target_player,int target_action,int target_card){
		// 组合扑克
		int wIndex = table.GRR._weave_count[target_player]++;
		table.GRR._weave_items[target_player][wIndex].public_card = 1;
		table.GRR._weave_items[target_player][wIndex].center_card = target_card;
		table.GRR._weave_items[target_player][wIndex].weave_kind = target_action;
		table.GRR._weave_items[target_player][wIndex].provide_player = _out_card_player;
		
	
		
		// 设置用户
		_current_player = table._current_player = target_player;

		
		//效果
		table.operate_effect_action(target_player,MJGameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{target_action}, 1,MJGameConstants.INVALID_SEAT);
		
		//删掉出来的那张牌
		//table.operate_out_card(this._out_card_player, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
		table.operate_remove_discard(this._out_card_player, table.GRR._discard_count[_out_card_player]);
		
		//刷新手牌包括组合
		int cards[]= new int[MJGameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
		table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player], table.GRR._weave_items[target_player]);
		
		table.exe_chi_peng(target_player,target_action);
		
	}

}
