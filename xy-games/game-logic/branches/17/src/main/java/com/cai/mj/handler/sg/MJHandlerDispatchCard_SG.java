package com.cai.mj.handler.sg;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_SG extends MJHandlerDispatchCard {
	private GangCardResult m_gangCardResult;
	
	public MJHandlerDispatchCard_SG(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(MJTable table) {
		// 用户状态
		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		
		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for(int i=0; i < MJGameConstants.GAME_PLAYER; i++){
				table.GRR._chi_hu_card[i] = MJGameConstants.INVALID_VALUE;
			}
			table._banker_select=(table._banker_select + 1) % MJGameConstants.GAME_PLAYER;
			// 流局
			table.handler_game_finish(table._banker_select, MJGameConstants.Game_End_DRAW);

			return;
		}
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		table._current_player = _seat_index;// 轮到操作的人是自己
		

		// 从牌堆拿出一张牌
		table._send_card_count++;
		if (table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
			_send_card_data = table._repertory_card_zz[table._all_card_len-table.GRR._left_card_count];
		} else {
			_send_card_data = table._repertory_card_cs[table._all_card_len-table.GRR._left_card_count];
		}
		--table.GRR._left_card_count;

		table._provide_player = _seat_index;
			
		if(table.DEBUG_CARDS_MODE)_send_card_data = 0x03;	
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		
		//胡牌检测
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,MJGameConstants.HU_CARD_TYPE_ZIMO);// 自摸

		if(action != MJGameConstants.WIK_NULL){
			//添加动作
			curPlayerStatus.add_action(MJGameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data,_seat_index);
			
		}else{
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}
		
		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
	
		
		//癞子
		int real_card = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			real_card+=MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
		}
		
		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index,1,new int[]{real_card},MJGameConstants.INVALID_SEAT);
	
		if(curPlayerStatus.has_zi_mo() && table.has_rule(MJGameConstants.GAME_TYPE_ZZ_JIANPAOHU)){
			//见炮胡
			table.exe_jian_pao_hu(_seat_index,MJGameConstants.WIK_ZI_MO,_send_card_data);
			return ;
		}

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask=table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index],_send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult);
			if(cbActionMask!=MJGameConstants.WIK_NULL){//有杠
				curPlayerStatus.add_action(MJGameConstants.WIK_GANG);//转转就是杠
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					//加上杠
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
			
		}
		
		if(curPlayerStatus.has_action()){//有动作
			curPlayerStatus.set_status(MJGameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
		}else{
			curPlayerStatus.set_status(MJGameConstants.Player_Status_OUT_CARD);// 出牌状态
			//自动出牌
			table.operate_player_status();
		}
		
		return;
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
			
			table._playerStatus[_seat_index].set_status(MJGameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}

		// 执行动作
		switch (operate_code) {
			case MJGameConstants.WIK_GANG: // 杠牌操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//是否有抢杠胡
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
				
			}
			break;
			case MJGameConstants.WIK_ZI_MO: // 自摸
			{
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);
				
				table._banker_select = _seat_index;
				// 下局胡牌的是庄家
				table.set_niao_card(_seat_index,MJGameConstants.INVALID_VALUE,true,0);// 结束后设置鸟牌
	
				table.GRR._chi_hu_card[_seat_index] = operate_card;
				
				table.process_chi_hu_player_operate_sg(_seat_index, operate_card,true);
				table.process_chi_hu_player_score(_seat_index,_seat_index,operate_card, true);
	
				// 记录
				table._player_result.zi_mo_count[_seat_index]++;
				
				//结束
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, MJGameConstants.Game_End_NORMAL),
						MJGameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	
				return true;
			}
		}

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
		tableResponse.setCurrentPlayer(_seat_index);
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
				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
					//癞子
					int_array.addItem(table.GRR._discard_cards[i][j]+MJGameConstants.CARD_ESPECIAL_TYPE_GUI);
				}else{
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < MJGameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)){
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+MJGameConstants.CARD_ESPECIAL_TYPE_GUI);
				}else{
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			
			if(i == _seat_index){
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			}else{
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
			
			
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[MJGameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		

		//如果断线重连的人是自己
		if(seat_index == _seat_index){
			table._logic.remove_card_by_data(cards, _send_card_data);
		}
		//癞子
		for(int j=0; j < hand_card_count; j++){
			if( table._logic.is_magic_card(cards[j])){
				cards[j]+=MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
			}
		}
		
		for (int i = 0; i < MJGameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		//癞子
		int real_card = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			real_card+=MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
		}
		//摸牌
		table.operate_player_get_card(_seat_index, 1, new int[]{real_card},seat_index);
		
		if(table._playerStatus[seat_index].has_action()){
			table.operate_player_action(seat_index, false);
		}
		
		//听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
