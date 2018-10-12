package com.cai.game.schcp.handler.yanzhong;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DisplayCardRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcp.handler.SCHCPHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;


public class SCHCPHandlerZhao_YanZhong extends SCHCPHandlerChiPeng<SCHCPTable_YanZhong> {
	
	
	public SCHCPHandlerZhao_YanZhong(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(SCHCPTable_YanZhong table) {
		// 组合扑克
	
		// 设置用户
		table._current_player = _seat_index;
	

		
		table.operate_player_get_card(table._last_player, 1, new int[]{_card} , GameConstants.INVALID_SEAT,false);
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]++;
		table._zhao_guo_card[_seat_index][table._logic.switch_to_card_index(_card)]++;

	
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		int chi_peng_cards[] = new int[3];
		int chi_peng_count = table._logic.switch_to_value_to_card(table._logic.get_dot(_card), chi_peng_cards);
		int s_cards[] = new int[3];
		int count = table._logic.switch_to_value_to_card(7,s_cards);
		for(int i = 0; i<count ;i++)
		{
			if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
			{
				table.cannot_outcard(_seat_index, s_cards[i],1, true);
			}
		}

		if(table._ti_mul_long[_seat_index]>0)
		{
			curPlayerStatus.add_action(GameConstants.CP_WIK_TOU);
			curPlayerStatus.add_tou(_card, GameConstants.CP_WIK_TOU, _seat_index);
		}
	
		table.operate_cannot_card(_seat_index, true);
		if (curPlayerStatus.has_action() ) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _card),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.isTrutess(_seat_index)) {
				
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _card),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			if(table.check_out_card(_seat_index)==false)
			{
				table.no_card_out_game_end(_seat_index, 0);
				return ;
			}
//			table.operate_player_get_card(table._last_player, 0, null , GameConstants.INVALID_SEAT,false,1);
//			// 刷新自己手牌
//			int cards[] = new int[GameConstants.MAX_CP_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
//			table.must_out_card();
//			if(table.check_out_card(_seat_index)==false)
//			{
//				table.no_card_out_game_end(_seat_index, 0);
//				return  ;
//			}
//			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
//			table.operate_player_status();
			GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _card,true),
					800, TimeUnit.MILLISECONDS);
			
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
	public boolean handler_operate_card(SCHCPTable_YanZhong table,int seat_index, int operate_code, int operate_card,int lou_pai){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("HHHandlerChiPeng_YX 没有这个操作:"+operate_code);
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("HHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			if(table.check_out_card(_seat_index)==false)
			{
				table.no_card_out_game_end(_seat_index, 0);
				return true ;
			}
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}
		else if(operate_code == GameConstants.CP_WIK_TOU){
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			table.exe_Dispatch_tou_card_data(seat_index,GameConstants.WIK_NULL,0);
			return true;
		}
	

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(SCHCPTable_YanZhong table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);// 加载房间的玩法 状态信息
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_CP_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				if(seat_index!=i) {
				
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addHuXi(table._hu_xi[i]);
			//
			tableResponse.addWinnerOrder(0);

			// 牌
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}
		for(int i = 0; i < table.getTablePlayerNumber();i++)
		{
			int zhao_count = 0;
			for(int j = 0; j <GameConstants.MAX_CP_INDEX;j++ ){
				zhao_count += table._zhao_card[i][j];
			}
			tableResponse.addCardsDataNiao(zhao_count);
		}
		int cards_index[] = new int[GameConstants.MAX_CP_INDEX];
		Arrays.fill(cards_index, 0);
		for(int i = 0; i<GameConstants.MAX_CP_INDEX;i++)
		{
			cards_index[i] = table._zhao_card[seat_index][i];
		}
		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_CP_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < hand_card_count; i++) {
			if(cards_index[table._logic.switch_to_card_index(hand_cards[i])]>0)
			{
				cards_index[table._logic.switch_to_card_index(hand_cards[i])]--;
				hand_cards[i]|=0x100;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		if(table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index,table._is_xiang_gong[seat_index]);
		table.operate_cannot_card(seat_index,false);
		table.operate_must_out_card(seat_index, false);
		table.istrustee[seat_index]=false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
