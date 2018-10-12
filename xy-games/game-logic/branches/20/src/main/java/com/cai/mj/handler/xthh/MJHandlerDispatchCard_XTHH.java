package com.cai.mj.handler.xthh;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
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

public class MJHandlerDispatchCard_XTHH extends MJHandlerDispatchCard {
	boolean out_send_ting_card=false;
	
	
	
	@Override
	public void exe(MJTable table) {
		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for(int i=0; i < GameConstants.GAME_PLAYER; i++){
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._banker_select=(table._banker_select + 1) % GameConstants.GAME_PLAYER;
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}else if(table.GRR._left_card_count == 4){
			//最后四张牌。看谁能自摸。不用打
			table.operate_effect_action(GameConstants.INVALID_SEAT, GameConstants.Effect_Action_Other, 1, new long[]{GameConstants.EFFECT_LAST_FOUR_CARD}, 1, GameConstants.INVALID_SEAT);
			//延迟3秒
			table.exe_dispatch_last_card(_seat_index,GameConstants.WIK_NULL,3000);
			return;
		}
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		
		

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
		
		
		--table.GRR._left_card_count;

		table._provide_player = _seat_index;
		if(table.DEBUG_CARDS_MODE)_send_card_data = 0x04;	
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		
		//胡牌检测
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,GameConstants.HU_CARD_TYPE_ZIMO);// 自摸

		if(action != GameConstants.WIK_NULL){
			//添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data,_seat_index);
			
		}else{
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		// 加到手牌
		table.GRR._cards_index[_seat_index][send_card_index]++;
		int count = 0;
		int ting_count = 0;
		out_send_ting_card=false;
		
		for( int i = 0; i < GameConstants.MAX_INDEX-GameConstants.CARD_FENG_COUNT-GameConstants.CARD_HUA_COUNT; i++ )
		{
			count = table.GRR._cards_index[_seat_index][i];
			if(count>0){
				//假如打出这张牌
				table.GRR._cards_index[_seat_index][i]--;
				
				//检查打出哪一张牌后可以听牌
				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_xthh_ting_card(table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index], 
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
				
				if(table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count]>0){
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);
					//如果是癞子
					if(table._logic.is_magic_index(i)){
						//table._playerStatus[_seat_index]._hu_out_card_ting[ting_count]+=MJGameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					
					//能听牌
					ting_count++;
					if(send_card_index ==i){
						out_send_ting_card = true;
					}
				}
				
				//加回来
				table.GRR._cards_index[_seat_index][i]++;
			}
		}
		
		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
		if(ting_count>0){
			//减掉发的牌
			table.GRR._cards_index[_seat_index][send_card_index]--;
			
			//刷新手牌
			int cards[] = new int[GameConstants.MAX_COUNT];
			//刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			
			//还原发的牌
			table.GRR._cards_index[_seat_index][send_card_index]++;
			
			
//			for(int i = 0; i < hand_card_count; i++){
//				if(table._logic.is_magic_card(cards[i])){
//					cards[i]+=MJGameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
//				}
//			}
			
			for(int i = 0; i < hand_card_count; i++){
				if(table._logic.is_magic_card(cards[i])){
					cards[i]+=GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}else{
					for(int j = 0; j<ting_count;j++){
						if(cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]){
							cards[i] +=GameConstants.CARD_ESPECIAL_TYPE_TING;
						}
					}
				}
			}
			
			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		
		//癞子
		int real_card = _send_card_data;
	
		if(table._logic.is_magic_card(_send_card_data)){
			real_card+=GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}else{// if(table._logic.is_lai_gen_card(_send_card_data)){
			//real_card+=MJGameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			if(out_send_ting_card){
				real_card+=GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
		}
		
		
		
		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index,1,new int[]{real_card},GameConstants.INVALID_SEAT);
	
		if(curPlayerStatus.has_zi_mo() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
			//见炮胡
			table.exe_jian_pao_hu(_seat_index,GameConstants.WIK_ZI_MO,_send_card_data);
			return ;
		}

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;
			//笑牌检测
			int cbActionMask=table._logic.analyse_xiao_by_dispacth_card(table.GRR._cards_index[_seat_index],_send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult);
			if(cbActionMask!=GameConstants.WIK_NULL){//有笑
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					//加上笑
					curPlayerStatus.add_action(m_gangCardResult.type[i]);//
					curPlayerStatus.add_xiao(m_gangCardResult.cbCardData[i],m_gangCardResult.type[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
			
		}
		
		if(curPlayerStatus.has_action()){//有动作
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
		}else{
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			//自动出牌
			table.operate_player_status();
		}
		
		return;
	}
	
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable table,int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		
		
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}


		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		//出牌
//		if(_type == MJGameConstants.DispatchCard_Type_Gang){
//			table.exe_re_chong(_seat_index,card,_type);
//		}else {
//			table.exe_out_card(_seat_index,card,_type);
//		}
		table.exe_out_card(_seat_index,card,_type);

		return true;
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
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("没有这个操作");
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("不是当前玩家操作");
			return false;
		}
		
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}

		// 执行动作
		switch (operate_code) {
			case GameConstants.WIK_GANG: // 杠牌操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//是否有抢杠胡
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
				
			}
			case GameConstants.WIK_DA_CHAO_TIAN: // 杠牌操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//是否有抢杠胡
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
			}
			case GameConstants.WIK_MENG_XIAO: 
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
			case GameConstants.WIK_HUI_TOU_XIAO: 
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
			case GameConstants.WIK_ZI_MO: // 自摸
			{
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);
				
				table._banker_select = _seat_index;
				// 下局胡牌的是庄家
				//table.set_niao_card(_seat_index,MJGameConstants.INVALID_VALUE,true,0);// 结束后设置鸟牌
	
				table.GRR._chi_hu_card[_seat_index][0] = operate_card;
				
				table.process_chi_hu_player_operate_xthh(_seat_index, operate_card,true);
				table.process_chi_hu_player_score_xthh(_seat_index,_seat_index,operate_card, true);
	
				// 记录
				table._player_result.zi_mo_count[_seat_index]++;
				
				//结束
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
					//癞子
					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				}else{
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)){
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				}else if(table._logic.is_lai_gen_card(table.GRR._weave_items[i][j].center_card)){
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					//weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+MJGameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				}else{
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+GameConstants.WEAVE_SHOW_DIRECT);
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
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		

		//如果断线重连的人是自己
		if(seat_index == _seat_index){
			table._logic.remove_card_by_data(cards, _send_card_data);
		}
		
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;
		//癞子
		for(int j=0; j < hand_card_count; j++){
			if( table._logic.is_magic_card(cards[j])){
				cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}else{ //if(table._logic.is_lai_gen_card(cards[j])){
				//cards[j]+=MJGameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
				if((out_ting_count>0) && (seat_index==_seat_index)){
					for(int k = 0; k<out_ting_count;k++){
						if(cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]){
							cards[j] +=GameConstants.CARD_ESPECIAL_TYPE_TING;
						}
					}
				}
			}
		}
		
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}
		roomResponse.setTable(tableResponse);
		
		//打出去可以听牌的个数
		roomResponse.setOutCardCount(out_ting_count);
		
		for(int i = 0; i < out_ting_count; i++){
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i]+GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for(int j=0; j < ting_card_cout; j++){
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}
		
		table.send_response_to_player(seat_index, roomResponse);
		
		//癞子
		int real_card = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			real_card+=GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}else{// if(table._logic.is_lai_gen_card(_send_card_data)){
			//real_card+=MJGameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			if(out_send_ting_card ){
				real_card+=GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
		}
		//摸牌
		table.operate_player_get_card(_seat_index, 1, new int[]{real_card},seat_index);
		
		if(table._playerStatus[seat_index].has_action()&& (table._playerStatus[seat_index].is_respone()==false)){
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
