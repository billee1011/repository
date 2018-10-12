package com.cai.game.mj.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.AbstractMJTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public abstract class MJHandlerGang<T extends AbstractMJTable> extends  AbstractMJHandler<T> {
	
	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;//
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;//双杠
	protected int _type;

	
	public MJHandlerGang(){
	}
	
	public void reset_status(int seat_index,int provide_player, int center_card,int action, int type, boolean self,boolean d){
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if(GameConstants.GANG_TYPE_AN_GANG == _type){
			_p = false;
		}else{
			_p = true;
		}
		_self = self;
		_double = d;
	}
	
	
//	@Override
//	public void exe(T table) {
//		// TODO Auto-generated method stub
//		// 用户状态
//		for(int i = 0; i < GameConstants.GAME_PLAYER; i++){
//			if(table._playerStatus[i].has_action()){
//				table.operate_player_action(i, true);
//			}
//			
//			table._playerStatus[i].clean_action();
//			table.change_player_status(i,GameConstants.INVALID_VALUE);
//			//table._playerStatus[i].clean_status();
//		}
//	
//		
//		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
//		
//		//效果
//		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
//	
//		//暗杠
//		if(_p==false){
//			this.exe_gang(table);
//			return;
//		}
//		
//		//检查对这个杠有没有 胡
//		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);
//		
//		if(bAroseAction == false){
//			this.exe_gang(table);
//		}else{
//			PlayerStatus playerStatus = null;
//			
//			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
//				playerStatus= table._playerStatus[i];
//				if(playerStatus.has_chi_hu() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
//					continue;
//				}
//				if(playerStatus.has_action()){
//					//table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//操作状态
//					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
//					table.operate_player_action(i, false);
//				}
//			}
//			
//			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
//				playerStatus = table._playerStatus[i];
//				if(playerStatus.has_chi_hu() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
//					//见炮胡
//					table.exe_jian_pao_hu(i,GameConstants.WIK_CHI_HU,_center_card);
//				}
//			}
//			
//		}
//		
//	}
//	
//	/***
//	 * //用户操作
//	 * 
//	 * @param seat_index
//	 * @param operate_code
//	 * @param operate_card
//	 * @return
//	 */
//	@Override
//	public boolean handler_operate_card(T table,int seat_index, int operate_code, int operate_card){
//		//抢杠胡
//		
//		// 效验状态
//		PlayerStatus playerStatus = table._playerStatus[seat_index];
//		
//		 // 是否已经响应
//		if(playerStatus.has_action()==false){
//			table.log_player_error(seat_index, "出牌,玩家操作已失效");
//			return false;
//		}
//		
//		 // 是否已经响应
//		if(playerStatus.is_respone()){
//			table.log_player_error(seat_index, "出牌,玩家已操作");
//			return false;
//		}
//		
//		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
//		{
//			table.log_player_error(seat_index, "出牌操作,没有动作");
//			return false;
//		}
//		
//		if((operate_code!=GameConstants.WIK_NULL) && (operate_card != _center_card)){
//			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
//			return false;
//		}
//		
//		//玩家的操作
//		playerStatus.operate(operate_code,operate_card);
//				
//		if(operate_code == GameConstants.WIK_NULL){
//			table.GRR._chi_hu_rights[seat_index].set_valid(false);//胡牌失效
//			table._playerStatus[seat_index].chi_hu_round_invalid();//这一轮就不能吃胡了没过牌之前都不能胡
//		}else if(operate_code == GameConstants.WIK_CHI_HU){
//			table.GRR._chi_hu_rights[seat_index].set_valid(true);//胡牌生效
//			table.process_chi_hu_player_operate(seat_index, _center_card,false);//效果
//		}else{
//			table.log_player_error(seat_index, "出牌操作,没有动作");
//			return false;
//		}
//		
//		//清理这个玩家状态
//		table._playerStatus[seat_index].clean_action();
//		table.change_player_status(seat_index,GameConstants.INVALID_VALUE);
//		//table._playerStatus[seat_index].clean_status();
//		table.operate_player_action(seat_index, true);
//		
//		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
//		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			if ((table._playerStatus[i].is_respone()== false) && (table._playerStatus[i].has_chi_hu()))
//				return false;
//		}
//		
//		int jie_pao_count = 0;
//		for(int i=0; i<GameConstants.GAME_PLAYER;i++){
//			if((table.GRR._chi_hu_rights[i].is_valid()==false )){//(i == _provide_player) ||
//				continue;
//			}
//			jie_pao_count++;
//		}
//		
//		if(jie_pao_count>0){
//			int add_niao = 0;
//			if(jie_pao_count>1){
//				//红中通炮就不加鸟了
//				table.set_niao_card(_provide_player,GameConstants.INVALID_VALUE,true,add_niao);// 结束后设置鸟牌
//				
//				//结束
//				table._banker_select = _seat_index;
//			}else if(jie_pao_count==1){
//				if(table.is_mj_type(GameConstants.GAME_TYPE_HZ)||table.is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)){
//					if(table.GRR._cards_index[_seat_index][table._logic.get_magic_card_index(0)]==0){
//						//手上没有红中
//						add_niao=1;
//					}
//				}
//				table.set_niao_card(seat_index,GameConstants.INVALID_VALUE,true,add_niao);// 结束后设置鸟牌 
//				
//				table._banker_select = seat_index;
//			}
//			for(int i=0; i<GameConstants.GAME_PLAYER;i++){
//				if((table.GRR._chi_hu_rights[i].is_valid()==false )){//(i == _provide_player) ||
//					continue;
//				}
//				
//				table.GRR._chi_hu_card[i][0] = _center_card;
//				
//				table.process_chi_hu_player_score(i,_seat_index, _center_card,false);
//
//				// 记录
//				table._player_result.jie_pao_count[i]++;
//				table._player_result.dian_pao_count[_provide_player]++;
//			}
//			
//			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
//					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
//
//		}else{
//			//选择了不胡
//			//发牌给杠的玩家
//			this.exe_gang(table);
//		}
//
//		return true;
//	}
//	
	/**执行杠
	 * 
	 * 
	 * ***/
	protected abstract boolean exe_gang(T table);
	
	@Override
	public boolean handler_player_be_in_room(T table,int seat_index) {
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
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
//			if(_self){
//				if(i == _seat_index){
//					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
//				}else{
//					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
//				}
//			}else{
//				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
//			}
			
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			
			
			
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);


		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,seat_index);
		
		
		if(table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone()==false){
			table.operate_player_action(seat_index, false);
		}
	
		return true;
	}

}
