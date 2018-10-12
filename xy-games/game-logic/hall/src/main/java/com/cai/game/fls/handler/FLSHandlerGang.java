package com.cai.game.fls.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.fls.FLSTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class FLSHandlerGang extends FLSHandler {
	
	protected int _seat_index;
	protected int _provide_player;
	protected int _center_card;
	protected int _action;//
	protected boolean _p;
	protected boolean _self;
	protected boolean _double;//双杠
	protected int _type;

	
	public FLSHandlerGang(){
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
	
	
	@Override
	public void exe(FLSTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for(int i = 0; i < table.getTablePlayerNumber(); i++){
			if(table._playerStatus[i].has_action()){
				table.operate_player_action(i, true);
			}
			
			table._playerStatus[i].clean_action();
			//table._playerStatus[i].clean_status();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
		}
	
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT,_center_card,_p);
	
		//暗杠
		if(_p==false){
			this.exe_gang(table);
			return;
		}
		
		//检查对这个杠有没有 胡
//		boolean bAroseAction = table.estimate_gang_respond_fls(_seat_index, _center_card,_type);
		
		boolean bAroseAction =false;
		
		if(bAroseAction == false){
			this.exe_gang(table);
		}else{
			PlayerStatus playerStatus = null;
			
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus= table._playerStatus[i];
				if(playerStatus.has_chi_hu() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
					continue;
				}
				if(playerStatus.has_action()){
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					//table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//操作状态
					table.operate_player_action(i, false);
				}
			}
			
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if(playerStatus.has_chi_hu() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
					//见炮胡
					table.exe_jian_pao_hu(i,GameConstants.WIK_CHI_HU,_center_card);
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
		//抢杠胡
		
		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		 // 是否已经响应
		if(playerStatus.has_action()==false){
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}
		
		 // 是否已经响应
		if(playerStatus.is_respone()){
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}
		
		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}
		
		if((operate_code!=GameConstants.WIK_NULL) && (operate_card != _center_card)){
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}
		
		//玩家的操作
		playerStatus.operate(operate_code,operate_card);
				
		if(operate_code == GameConstants.WIK_NULL){
			table.GRR._chi_hu_rights[seat_index].set_valid(false);//胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();//这一轮就不能吃胡了没过牌之前都不能胡
		}else if(operate_code == GameConstants.WIK_CHI_HU){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);//胡牌生效
			table.process_chi_hu_player_operate(seat_index, _center_card,false);//效果
		}else{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}
		
		//清理这个玩家状态
		table._playerStatus[seat_index].clean_action();
		table.change_player_status(seat_index,GameConstants.INVALID_VALUE);
		//table._playerStatus[seat_index].clean_status();
		table.operate_player_action(seat_index, true);
		
		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone()== false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}
		
		int jie_pao_count = 0;
		for(int i=0; i<table.getTablePlayerNumber();i++){
			if((table.GRR._chi_hu_rights[i].is_valid()==false )){//(i == _provide_player) ||
				continue;
			}
			jie_pao_count++;
		}
		
		if(jie_pao_count>0){
			int add_niao = 0;
			if(jie_pao_count>1){
				//红中通炮就不加鸟了
				//结束
				table._banker_select = _seat_index;
			}else if(jie_pao_count==1){
				if(table.is_mj_type(GameConstants.GAME_TYPE_HZ)){
					if(table.GRR._cards_index[_seat_index][table._logic.get_magic_card_index(0)]==0){
						//手上没有红中
						add_niao=1;
					}
				}
				table._banker_select = seat_index;
			}
			for(int i=0; i<table.getTablePlayerNumber();i++){
				if((table.GRR._chi_hu_rights[i].is_valid()==false )){//(i == _provide_player) ||
					continue;
				}
				
				table.GRR._chi_hu_card[i][0] = _center_card;
				
				table.process_chi_hu_player_score_fls(i,_seat_index, _center_card,false);

				// 记录
				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_provide_player]++;
			}
			
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		}else{
			//选择了不胡
			//发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}
	
	/**执行杠
	 * 
	 * 
	 * ***/
	protected boolean exe_gang(FLSTable table){
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		if(_self){
			//自己的牌
			
			// 杠牌处理 如果手上还有4张一样的
			if (_p) {
				// 看看是不是有碰的牌，明杠
				// 寻找组合
				for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
					int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
					int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
					if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
						cbWeaveIndex = i;// 第几个组合可以碰
						break;
					}
				}

				if (cbWeaveIndex == -1){
					table.log_player_error(_seat_index, "杠牌出错");
					return false;
				}
			}else{
				//暗杠
				// 设置变量
				cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_count[_seat_index]++;
			}
		}else{
			//别人打的牌
			
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			
			//删掉出来的那张牌
			table.operate_out_card(_provide_player, 0, null,GameConstants.OUT_CARD_TYPE_MID,GameConstants.INVALID_SEAT);
		}
		
	
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p==true?1:0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		
		// 设置用户
		table._current_player = _seat_index;
		
		
			
		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.MAX_FLS_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		
		//检查听牌
		table._playerStatus[_seat_index]._hu_card_count = table.get_fls_ting_card(
				table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}
		
		/////////////////////////////////////////////////////算分
		if(table.is_mj_type(GameConstants.GAME_TYPE_CS)){
			//长沙麻将杠没有分
			if(_action == GameConstants.WIK_ZHAO){
				// 从后面发一张牌给玩家
				table.exe_dispatch_card(_seat_index,_type,0);
			}else{
				// 从后面发两张牌给玩家
				//table.dispath_cs_gang_card_data(_seat_index);
				table.exe_gang_fls(_seat_index,false);
			}
		}else{
			// 杠得分
			if(_self){
				int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index)
						continue;
	
					if(_p == true){
						//明杠每人1分
						table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -GameConstants.CELL_SCORE;// 暗杠，其他玩家扣分
						table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += GameConstants.CELL_SCORE;//一共加分
					}else{
						//暗杠每人2分
						table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -2*GameConstants.CELL_SCORE;// 暗杠，其他玩家扣分
						table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 2*GameConstants.CELL_SCORE;//一共加分
					}
					
				}
			}else{
				table._player_result.ming_gang_count[_seat_index]++;
				
				int cbIndex = table.GRR._gang_score[_seat_index].gang_count++;
				table.GRR._gang_score[_provide_player].provide_gang_count++;			//放杠
				table.GRR._gang_score[_seat_index].get_gang_count++;		//接杠
				//放杠三分
				table.GRR._gang_score[_seat_index].scores[cbIndex][_seat_index] = 3 * GameConstants.CELL_SCORE;// m_pGameServiceOption->lCellScore*2;//配置参数乘
				table.GRR._gang_score[_seat_index].scores[cbIndex][_provide_player] = -3 * GameConstants.CELL_SCORE;// -m_pGameServiceOption->lCellScore*2;//配置参数乘
			}
		
			
			
			// 从后面发一张牌给玩家
			table.exe_dispatch_card(_seat_index,_type,0);
		}

		return true;
		
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				if(seat_index!=i) {
					if((table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_GANG || table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_ZHAO) &&table.GRR._weave_items[i][j].public_card==0) {
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
		int hand_cards[] = new int[GameConstants.MAX_FLS_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);


		for (int i = 0; i < GameConstants.MAX_FLS_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,seat_index,_center_card,_p);
		
		
		if(table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone()==false){
			table.operate_player_action(seat_index, false);
		}
		
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
	
		return true;
	}

}
