package com.cai.game.scphz.handler.yj;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.scphz.SCPHZTable;
import com.cai.game.scphz.handler.PHZHandler;


/**
 * 打点选混
 * @author Administrator
 *
 */
public class PHZHandlerPaiQuan_YJ extends PHZHandler {

	protected int _player_card_type;

	
	
	public void reset_status(int card){
		_player_card_type = card;
	}
	
	
	@Override
	public void exe(SCPHZTable table) {
		// TODO Auto-generated method stub

		// 从牌堆拿出一张牌
//		table._send_card_count++;
//		_da_dian_card = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
//		--table.GRR._left_card_count;


		

		
		
		
		
		
		//显示
		table.operate_fan_jiang(-1,GameConstants.Show_Card_Center, 1,new int[]{table._player_card_type},GameConstants.INVALID_SEAT);

				
	}
	
	
	@Override
	public boolean handler_player_be_in_room(SCPHZTable table,int seat_index) {
//		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
//		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
//
//		TableResponse.Builder tableResponse = TableResponse.newBuilder();
//
//		table.load_room_info_data(roomResponse);
//		table.load_player_info_data(roomResponse);
//		table.load_common_status(roomResponse);
//		
//		
//		// 游戏变量
//		tableResponse.setBankerPlayer(table.GRR._banker_player);
//		tableResponse.setCurrentPlayer(table.GRR._banker_player);
//		tableResponse.setCellScore(0);
//
//		// 状态变量
//		tableResponse.setActionCard(0);
//		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);
//
//		// 历史记录
//		tableResponse.setOutCardData(0);
//		tableResponse.setOutCardPlayer(0);
//
//		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			tableResponse.addTrustee(false);// 是否托管
//			// 剩余牌数
//			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
//			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
//			for (int j = 0; j < 55; j++) {
//				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
//					//癞子
//					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_HUN);
//				}else{
//					int_array.addItem(table.GRR._discard_cards[i][j]);
//				}
//				
//			}
//			tableResponse.addDiscardCards(int_array);
//
//			// 组合扑克
//			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
//			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
//			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
//				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
//				if(table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)){
//					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+GameConstants.CARD_ESPECIAL_TYPE_HUN);
//				}else{
//					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
//				}
//				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
//				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
//				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
//				weaveItem_array.addWeaveItem(weaveItem_item);
//			}
//			tableResponse.addWeaveItemArray(weaveItem_array);
//
//			//
//			tableResponse.addWinnerOrder(0);
//
//			// 牌
//			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
//		}
//
//		// 数据
//		tableResponse.setSendCardData(0);
//		int cards[] = new int[GameConstants.MAX_HH_COUNT];
//		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
//		for(int j=0; j < hand_card_count; j++){
//			if( table._logic.is_magic_card(cards[j])){
//				cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_HUN;
//			}
//		}
//
//		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
//			tableResponse.addCardsData(cards[i]);
//		}
//
//		roomResponse.setTable(tableResponse);
//
//		table.send_response_to_player(seat_index, roomResponse);
		

		//显示
//		table.operate_fan_jiang(-1,GameConstants.Show_Card_Center, 1,new int[table._player_card_type],seat_index);
				
		return true;
	}

}
