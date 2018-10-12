package com.cai.mj.handler.hz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerQiShouHongZhong extends MJHandler {

	private int _seat_index =MJGameConstants.INVALID_SEAT; 
	
	public void reset_status(int seat_index){
		_seat_index = seat_index;
	}
	
	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		table._playerStatus[_seat_index].set_status(MJGameConstants.Player_Status_OPR_CARD);
		table.operate_player_action(_seat_index, false);
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
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != MJGameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("没有这个操作");
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("不是当前玩家操作");
			return false;
		}
		
		// 放弃操作
		if (operate_code == MJGameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			
			//table._playerStatus[_seat_index].set_status(MJGameConstants.Player_Status_OUT_CARD);
			//table.operate_player_status();
			
			
			table.exe_dispatch_card(table.GRR._banker_player,MJGameConstants.WIK_NULL, 0);
			
			return true;
		}

		table.GRR._chi_hu_rights[_seat_index].set_valid(true);
		// 下局胡牌的是庄家
		table.set_niao_card(_seat_index,MJGameConstants.INVALID_VALUE,true,0);// 结束后设置鸟牌
		// 吃牌权位
//		if (table._out_card_count == 0) {//天胡
//			table._provide_player = _current_player;
//			table._provide_card = _send_card_data;
//		}

		table.GRR._chi_hu_card[_seat_index] = operate_card;
		
		table._banker_select = _seat_index;
		table.process_chi_hu_player_operate(_seat_index, operate_card,true);
		table.process_chi_hu_player_score(_seat_index,_seat_index,operate_card, true);

		// 记录
		table._player_result.zi_mo_count[_seat_index]++;
		
		GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, MJGameConstants.Game_End_NORMAL),
				MJGameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
		

		return true;
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
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
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
		
		if(table._playerStatus[seat_index].has_action()){
			table.operate_player_action(seat_index, false);
		}
		
		//如果是红中麻将,检查听牌
		if(table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)){
//					table._playerStatus[_out_card_player]._hu_card_count = table.get_hz_ting_card(table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player], 
//							table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player]);
			
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;
			
			
			if(ting_count>0){
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}else{
//						ting_cards[0]=0;
//						table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
			}
			
		}
		return true;
	}

}