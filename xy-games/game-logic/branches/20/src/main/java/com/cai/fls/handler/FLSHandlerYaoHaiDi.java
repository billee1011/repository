package com.cai.fls.handler;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.fls.FLSTable;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class FLSHandlerYaoHaiDi extends FLSHandler {

	private static Logger logger = Logger.getLogger(FLSHandlerYaoHaiDi.class);
	
	protected int _seat_index;
	protected int _send_card_data;
	
	public void reset_status(int seat_index){
		_seat_index = seat_index;
	}
	@Override
	public void exe(FLSTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			
			table.operate_player_action(i, true);
		}
		
		//下局庄家
		table._banker_select = _seat_index;
		
		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
		
		--table.GRR._left_card_count;

		if(table.DEBUG_CARDS_MODE) {
//			_send_card_data=0x03;
		}
		
		table._provide_player = _seat_index;
		
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_YAO_HAI_DI}, 0,GameConstants.INVALID_SEAT);
		table.operate_out_card(_seat_index, 1, new int[]{_send_card_data} ,GameConstants.OUT_CARD_TYPE_LEFT,GameConstants.INVALID_SEAT);
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		boolean has_action=false;
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,GameConstants.HU_CARD_TYPE_ZIMO);// 自摸
				
		if(action != GameConstants.WIK_NULL){
			//添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data,_seat_index);
			chr.opr_or(GameConstants.CHR_HUNAN_HAI_DI_LAO);//海底捞
			has_action = true;
		}	
		
		for(int i=0; i< table.getTablePlayerNumber(); i++){
			if(i==_seat_index){
				continue;
			}
			action = table.analyse_chi_hu_card(table.GRR._cards_index[i],table.GRR._weave_items[i], table.GRR._weave_count[i], _send_card_data, table.GRR._chi_hu_rights[i],GameConstants.HU_CARD_TYPE_PAOHU);// 自摸
			
			if(action != GameConstants.WIK_NULL){
				//添加动作
				table._playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				table._playerStatus[i].add_chi_hu(_send_card_data, _seat_index);
				table.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HAI_DI_PAO);//海底炮
				
				has_action = true;
			}
				
		}
		
		if(has_action==false){
			GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _seat_index, 1, new int[]{_send_card_data},true,table.getMaxCount()),
					1, TimeUnit.SECONDS);
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_DRAW),
					2, TimeUnit.SECONDS);
		}else{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				curPlayerStatus = table._playerStatus[i];
				if(curPlayerStatus.has_action()){
					table.operate_player_action(i, false);
				}
			}
			
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
	public boolean handler_operate_card(FLSTable table,int seat_index, int operate_code, int operate_card){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		 // 是否已经响应
		if(playerStatus.has_action()==false){
			logger.error("[要海底],操作失败,"+seat_index+"玩家操作已失效");
			return true;
		}
		
		 // 是否已经响应
		if(playerStatus.is_respone()){
			logger.error("[要海底],操作失败,"+seat_index+"玩家已操作");
			return true;
		}
		
		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			logger.error("[要海底],操作失败,"+seat_index+"没有动作");
			return true;
		}

		
		//玩家的操作
		playerStatus.operate(operate_code,operate_card);
		
		if(operate_code == GameConstants.WIK_CHI_HU){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);//胡牌生效
			//效果
			if(table._playerStatus[_seat_index].has_zi_mo()==false || table._playerStatus[_seat_index].is_respone()==true){
				table.process_chi_hu_player_operate(seat_index, operate_card,false);
			}
			
			
		}else if(operate_code == GameConstants.WIK_NULL){
			table.GRR._chi_hu_rights[seat_index].set_valid(false);//胡牌无效
			if(table._playerStatus[seat_index].has_chi_hu()){
				table._playerStatus[seat_index].chi_hu_round_invalid();//这一轮就不能吃胡了没过牌之前都不能胡
			}
		}
		
		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;
				
		if(operate_code != GameConstants.WIK_ZI_MO){
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table._playerStatus[i].is_respone()== false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}
			
			// 执行判断
			for (int i = _seat_index; i <table.getTablePlayerNumber(); i++){
				int p =i % table.getTablePlayerNumber();
				// 获取动作
				int cbUserActionRank =0;
				
				if(table._playerStatus[p].has_action()){
					if(table._playerStatus[p].is_respone()){
						//获取已经执行的动作的优先级
						cbUserActionRank =  table._logic.get_action_rank(table._playerStatus[p].get_perform());
					}else{
						//获取最大的动作的优先级
						cbUserActionRank =  table._logic.get_action_list_rank(table._playerStatus[p]._action_count,table._playerStatus[p]._action);
					}
					
					// 优先级别
					int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[seat_index].get_perform());

					// 动作判断 优先级最高的人和动作
					if (cbUserActionRank > cbTargetActionRank) {
						target_player = p;//最高级别人
						target_action = table._playerStatus[p].get_perform();
					}
				}
			}
			// 优先级最高的人还没操作
			if (table._playerStatus[target_player].is_respone()== false)
				return true;
			
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			
			table.operate_player_action(i, true);
		}
		
		// 删除扑克
		switch (target_action) {
			case  GameConstants.WIK_NULL:{
				//删掉出来的那张牌
				table.operate_out_card(this._seat_index, 0, null,GameConstants.OUT_CARD_TYPE_LEFT,GameConstants.INVALID_SEAT);
				
				GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _seat_index, 1, new int[]{_send_card_data},true,table.getMaxCount()),
						1, TimeUnit.SECONDS);
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
						2, TimeUnit.SECONDS);
				
				return true;
			}
			case GameConstants.WIK_ZI_MO: // 自摸
			{
				for(int i=0; i<table.getTablePlayerNumber();i++){
					if(i == _seat_index){
						table.GRR._chi_hu_rights[i].set_valid(true);
					}else{
						table.GRR._chi_hu_rights[i].set_valid(false);
					}
				}
				
				// 吃牌权位
				if (table._out_card_count == 0) {//天胡
					table._provide_player = _seat_index;
					table._provide_card = target_card;
				}
	
				// 检查自摸
//				int cbWeaveItemCount = table.GRR._weave_count[_seat_index];
//				WeaveItem pWeaveItem[] = table.GRR._weave_items[_seat_index];
//	
//				table.GRR._chi_hu_kind[_seat_index] = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], pWeaveItem,
//						cbWeaveItemCount, target_card, table.GRR._chi_hu_rights[_seat_index],true);
//	
//				if(table.GRR._chi_hu_kind[_seat_index]!=0)
//				{
					// 结束信息
					table.GRR._chi_hu_card[_seat_index][0] = target_card;
					table.process_chi_hu_player_operate(_seat_index, _send_card_data,true);
					table.process_chi_hu_player_score_fls(_seat_index,_seat_index,_send_card_data, true);
		
					// 记录
					table._player_result.da_hu_zi_mo[_seat_index]++;
			//	}
				
			
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	
				return true;
			}
			case GameConstants.WIK_CHI_HU: //胡
			{
				int jie_pao_count = 0;
				for(int i=0; i<table.getTablePlayerNumber();i++){
					if((i == _seat_index) ||(table.GRR._chi_hu_rights[i].is_valid()==false )){
						continue;
					}
					jie_pao_count++;
				}
				if(jie_pao_count>1){
					table._banker_select = _seat_index;
				}else{
					table._banker_select = target_player;
				}
				
				for(int i=0; i<table.getTablePlayerNumber();i++){
					if((i == _seat_index) ||(table.GRR._chi_hu_rights[i].is_valid()==false )){
						continue;
					}
					
//					int cbWeaveItemCount = table.GRR._weave_count[i];
//					WeaveItem pWeaveItem[] = table.GRR._weave_items[i];
//		
//					table.GRR._chi_hu_kind[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], pWeaveItem,
//							cbWeaveItemCount, table._playerStatus[i]._operate_card, table.GRR._chi_hu_rights[seat_index],true);
//			
//					if(table.GRR._chi_hu_kind[i]!=0)
//					{
						table.process_chi_hu_player_score_fls(i,_seat_index, _send_card_data,false);
						
						// 记录
						table._player_result.da_hu_jie_pao[i]++;
						table._player_result.da_hu_dian_pao[_seat_index]++;
						
//					}
				}
					
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
			default:
				return false;
		}
	}
	
	@Override
	public boolean handler_player_be_in_room(FLSTable table,int seat_index) {
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
			for (int j = 0; j < GameConstants.MAX_WEAVE_FLS; j++) {
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
		int hand_cards[] = new int[GameConstants.MAX_FLS_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < GameConstants.MAX_FLS_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_YAO_HAI_DI}, 0,seat_index);
		table.operate_out_card(_seat_index, 1, new int[]{_send_card_data} ,GameConstants.OUT_CARD_TYPE_LEFT,seat_index);
		
		//table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
		if(table._playerStatus[seat_index].has_action()&& (table._playerStatus[seat_index].is_respone()==false)){
			table.operate_player_action(seat_index, false);
		}
	
		
		return true;
	}
	

}
