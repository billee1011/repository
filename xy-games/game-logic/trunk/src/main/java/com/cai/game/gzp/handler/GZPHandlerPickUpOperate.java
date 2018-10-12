package com.cai.game.gzp.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.gzp.GZPTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class GZPHandlerPickUpOperate extends GZPHandler {

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _pick_up_index = GameConstants.INVALID_SEAT;  //	捡牌用户
	public int _pick_up_data = GameConstants.INVALID_VALUE; //捡牌
	public int _action  = GameConstants.GZP_WIK_NULL;
	public boolean _is_out;
	
	
	protected int _current_player =GameConstants.INVALID_SEAT; 
	
	
	public void reset_status(int seat_index,int provide_index,int action,int card){
		_out_card_player = provide_index;
		_pick_up_data = card;
		_pick_up_index = seat_index;
		_is_out=false;
		_action = action ;
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
		if(is_out == true && table._pick_up_index[_pick_up_index][table._logic.switch_to_card_index(card)] == 0)
		{
			table.log_error("出捡牌,牌型出错");
			return false;
		}
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _pick_up_index) {
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_info("状态不对不能出牌");
			return false;
		}
		if(is_out == false &&table.GRR._cannot_out_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_info("当前牌不能出");
			return false;
		}
		if(table._cannot_pickup_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_info("当前捡的牌不能出");
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
		
			table._pick_up_index[_pick_up_index][table._logic.switch_to_card_index(card)]--;
			table.operate_pick_up_card(_pick_up_index);
		}
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_pick_up_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		//出牌
		table.exe_out_card(_pick_up_index,card,GameConstants.WIK_NULL);

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
		tableResponse.setCurrentPlayer(_out_card_player);
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
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				if(seat_index!=i) {
					if(( table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_ZHAO) &&table.GRR._weave_items[i][j].public_card==0) {
						weaveItem_item.setCenterCard(0);
					}else {
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
		
		
		//出牌
		if(this._pick_up_data != 0)
			table.operate_out_card(_out_card_player, 1, new int[]{_pick_up_data},GameConstants.OUT_CARD_TYPE_MID,seat_index);
		
		//table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
		if(table._playerStatus[seat_index].has_action()&& (table._playerStatus[seat_index].is_respone()==false)){
			table.operate_player_action(seat_index, false,false);
		}

		table.operate_cannot_card(seat_index);
		table.operate_cannot_pickup_card(seat_index);
		table.operate_pick_up_card(seat_index);
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.operate_player_connect_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
		
		return true;
	}
	
	@Override
	public boolean handler_be_set_trustee(GZPTable table, int seat_index){
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
		if(curPlayerStatus.has_action()){
//			table.operate_player_action(seat_index, true);
//			if(curPlayerStatus.has_zi_mo()){
//				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_ZI_MO, _out_card_data);
//			}else if(curPlayerStatus.has_chi_hu()){
//				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_CHI_HU, _out_card_data);
//			}else{
//				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, 0);
//			}
		}else if(curPlayerStatus.get_status() == GameConstants.Player_Status_OUT_CARD){	
			
			int out_card = GameConstants.INVALID_VALUE;			
			for (int i = 0; i < table.getMaxIndex(); i++) {
				if(table.GRR._cards_index[seat_index][i] > 0){//托管 随意出一张牌
					out_card = table._logic.switch_to_card_data(i);
				}
			}			
			if(out_card != GameConstants.INVALID_VALUE){
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, out_card),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);		
			}
		}
		return false;
	}
}
