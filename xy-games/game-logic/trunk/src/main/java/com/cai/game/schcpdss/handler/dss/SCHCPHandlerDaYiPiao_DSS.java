package com.cai.game.schcpdss.handler.dss;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.game.schcpdss.SCHCPDSSTable;
import com.cai.game.schcpdss.handler.SCHCPDSSHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 打点选混
 * @author Administrator
 *
 */
public class SCHCPHandlerDaYiPiao_DSS extends SCHCPDSSHandler {

	protected int _player_card_type;

	
	
	public void reset_status(int card){
		_player_card_type = card;
	}
	
	
	@Override
	public void exe(SCHCPDSSTable table) {
		// TODO Auto-generated method stub

		// 从牌堆拿出一张牌
//		table._send_card_count++;
//		_da_dian_card = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
//		--table.GRR._left_card_count;

		for(int i = 0; i< table.getTablePlayerNumber();i++){
			if(table._player_result.pao[i] == 1)
				continue;
			table._playerStatus[i].add_action(GameConstants.DSS_WIK_PIAO);
			table._playerStatus[i].add_piao(0, GameConstants.DSS_WIK_PIAO, i);
			table._playerStatus[i].add_action(GameConstants.DSS_WIK_NULL);
			table._playerStatus[i].add_pass(0, i);
		    table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
			//操作状态
			table.operate_player_action(i, false);
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
	public boolean handler_operate_card(SCHCPDSSTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("SCHCPHandlerDaYiPiao_DSS 没有这个操作:" + operate_code);
			return false;
		}
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
	
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "SCHCPHandlerDaYiPiao_DSS 出牌,玩家操作已失效");
			return true;
		}
		//if (seat_index != _seat_index) {
		//	table.log_error("DispatchCard 不是当前玩家操作");
		//	return false;
		//}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "SCHCPHandlerDaYiPiao_DSS 出牌,玩家已操作");
			return true;
		}
		if(table._player_result.pao[seat_index]!= -1)
		{
			table.log_player_error(seat_index, "SCHCPHandlerDaYiPiao_DSS ,飘分已经操作过了");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_action();
		playerStatus.clean_status();
		if(operate_code == GameConstants.DSS_WIK_PIAO){
			
			table._player_result.pao[seat_index] = 1;
		}
		else table._player_result.pao[seat_index] = 0;
		table.operate_player_data();
		
		for(int i = 0; i< table.getTablePlayerNumber();i++){
			if(table._player_result.pao[i] == -1)
				return true;
		}
		for(int i = 0 ;i<table.getTablePlayerNumber();i++){
			int cards[] = new int[GameConstants.DSS_MAX_CP_COUNT];
			
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);
			table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i],
					table.GRR._weave_items[i]);
		}
		table.exe_chuli_first_card(table._cur_banker, GameConstants.WIK_NULL, 0);
		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(SCHCPDSSTable table,int seat_index) {

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
				if (seat_index != i) {
					if ( table.GRR._weave_items[i][j].weave_kind == GameConstants.DSS_WIK_PENG
							&& table._is_display == false) {
						weaveItem_item.setCenterCard(0);
					} else {
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._hu_xi[i]);

			// 牌
		}

		// 数据
		tableResponse.setSendCardData(0);
		

		// 如果断线重连的人是自己
		// if(seat_index == _seat_index){
		// if(!((seat_index == table._current_player) && (_send_card_data ==
		// 0)))
		// table._logic.remove_card_by_data(hand_cards, _send_card_data);
		// }
		if (table._playerStatus[seat_index].has_action()  == false){
			int hand_cards[] = new int[GameConstants.DSS_MAX_CP_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
			for (int i = 0; i < hand_card_count; i++) {
				tableResponse.addCardsData(hand_cards[i]);
			}
		}
		else{
			for (int i = 0; i < table.GRR._card_count[seat_index]; i++) {
				tableResponse.addCardsData(table.GRR._cards_data[seat_index][i]);
			}
		}
		

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 摸牌
	
	
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
				
		return true;
	}

}
