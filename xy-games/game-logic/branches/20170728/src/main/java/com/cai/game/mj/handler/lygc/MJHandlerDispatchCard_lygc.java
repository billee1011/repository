package com.cai.game.mj.handler.lygc;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_lygc extends MJHandlerDispatchCard<MJTable> {
	
	@Override
	public void exe(MJTable table) {
		// 用户状态--清除状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			//table._playerStatus[i].clean_status();
		}
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		int llcard = 0;
		/*if(table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {//带混玩法 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}*/
		
		// 荒庄结束
		if (table.GRR._left_card_count <= llcard) {
			for(int i=0; i <table.getTablePlayerNumber(); i++){
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._banker_select=table.GRR._banker_player;//若荒装，则当局的庄家继续坐庄。
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
		--table.GRR._left_card_count;
		
		table._provide_player = _seat_index;
		
		if(table.DEBUG_CARDS_MODE){
			_send_card_data = 0x05;
			/*table._send_card_count = 126; 
			table.GRR._left_card_count =10;*/
		}
		
		
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		//WalkerGeek 硬次不自摸
		if(!table.has_rule(GameConstants.GAME_RULE_HENAN_YCI)){
			int action = table.analyse_chi_hu_card_henan_lygc(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,card_type);// 自摸
			
			if(action != GameConstants.WIK_NULL){
				//添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data,_seat_index);
			}else{
				chr.set_empty();
			}
		}
		
		int real_card = _send_card_data;
		
		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		
		if(!table.has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)){
			//次牌
			if (table.GRR._especial_show_cards.length > 0 && table.GRR._especial_show_cards[0] == real_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}
		
		
		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index,1,new int[]{real_card},GameConstants.INVALID_SEAT);
		
		//包次开始
		if(table.has_rule(GameConstants.GAME_RULE_HENAN_BAOCI) && table.GRR._left_card_count == table._bao_ci_start){
			//效果
			table.operate_effect_action(GameConstants.INVALID_SEAT,GameConstants.Effect_Action_Other, 1, new long[]{GameConstants.CHR_HENAN_BAO_CI_START}, 5,GameConstants.INVALID_SEAT);
			//包次开始状态变更
			table._bao_ci_state = GameConstants.LYGC_BAO_CI_SATRT;
		}
		
//		if(curPlayerStatus.has_zi_mo() && table.has_rule(MJGameConstants.GAME_TYPE_ZZ_JIANPAOHU)){
//			//见炮胡
//			table.exe_jian_pao_hu(_seat_index,MJGameConstants.WIK_ZI_MO,_send_card_data);
//			return ;
//		}

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌
		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count >= llcard) {
			
			int cbActionMask=table._logic.analyse_gang_card_all_lygc(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,true,GameConstants.INVALID_CARD);
			
			if(cbActionMask!=GameConstants.WIK_NULL){//有杠
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)&&table._logic.is_magic_card(m_gangCardResult.cbCardData[i])) {//鬼牌不能杆
						continue;
					}
					//加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					curPlayerStatus.add_action(GameConstants.WIK_GANG);//转转就是杠
					if(!table.has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)){
						//检测杠完之后是否存在次 
						int ci_card =table._logic.switch_to_card_data( table._logic.get_ci_card_index() );
						//从手牌移除杠
						int gang_card_num = table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])]; 
						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])] = GameConstants.INVALID_VALUE;
						boolean flag = false;
						int cbCardCount = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
						if(table.GRR._weave_count[_seat_index] == 3 && cbCardCount == 1){//次单吊处理
							//加入杠到组合
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].public_card =  m_gangCardResult.isPublic[i];
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].center_card = m_gangCardResult.cbCardData[i];
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].weave_kind = GameConstants.WIK_GANG;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].provide_player = _seat_index;
							table.GRR._weave_count[_seat_index]++;
							flag = true;
						}
						
						boolean IS_CI = table.estimate_lygc_gang_ci(_seat_index,ci_card);
						if(IS_CI){
							table._playerStatus[_seat_index].add_action(GameConstants.WIK_LYGC_CI);
							table._playerStatus[_seat_index].add_lygc_ci(m_gangCardResult.cbCardData[i], _seat_index,GameConstants.WIK_LYGC_CI);// 加上杠
						}
						//加入杠到手牌
						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])] = gang_card_num;
						if(flag){//次单吊处理
							//移除杠组合
							table.GRR._weave_count[_seat_index]--;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].public_card =  0;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].center_card = GameConstants.INVALID_CARD;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].weave_kind = GameConstants.WIK_NULL;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].provide_player = GameConstants.INVALID_VALUE;
						}
						//bAroseAction = true;
					}
				}
			}
		}
		
		//检测皮次规则
		if(table.has_rule(GameConstants.GAME_RULE_HENAN_PICI)){
			boolean flag = table.check_pi_ci(_seat_index);
			if(flag){
//				table._playerStatus[_seat_index].clean_action();
//				table.change_player_status(_seat_index,GameConstants.INVALID_VALUE);
				
				return ;
			}
		}
		
		if(curPlayerStatus.has_action()){//有动作
			//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index,false);
		}else{
			//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
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
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("没有这个操作");
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("不是当前玩家操作");
			return false;
		}
		
		// 是否已经响应
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		//playerStatus.clean_status();
		table.change_player_status(seat_index,GameConstants.INVALID_VALUE);
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			//table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_seat_index,GameConstants.INVALID_VALUE);
			if(table._playerStatus[_seat_index].lock_huan_zhang()){
				 GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			}else{
				//table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			
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
			break;
			case GameConstants.WIK_LYGC_CI: // 次操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//操作选择的是次
						table.LYGC_CI_STATE = true;
						table.exe_gang(_seat_index, _seat_index, operate_card, GameConstants.WIK_GANG, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
			}
			break;
		
			case GameConstants.WIK_ZI_MO: // 自摸
			{
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);
				
				table.GRR._chi_hu_card[_seat_index][0] = operate_card;
				
				table._banker_select = _seat_index;
				table.process_chi_hu_player_operate(_seat_index, operate_card,true);
				table.process_chi_hu_player_score_henan_lygc(_seat_index,_seat_index,operate_card, GameConstants.HU_CARD_TYPE_ZIMO,true);
	
				// 记录
				table._player_result.zi_mo_count[_seat_index]++;
				
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

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if( table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._discard_cards[i][j])) ){	
					//次牌
					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_CI);
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
				if( table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._weave_items[i][j].center_card)) ){
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+GameConstants.CARD_ESPECIAL_TYPE_CI);
				}else{
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				//重连组合变灰
				if(table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG){
					for(int k = 0; k < table.getTablePlayerNumber(); k++){
						for(int m = 0 ; m < table.GRR._discard_count[k]; m++ ){
							if(table.GRR._weave_items[i][j].center_card ==  table.GRR._discard_cards[k][m]){
								weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
							}
						}
					}
				}
				
				//杠牌组合变灰
				if(table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG){
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
				}
				
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
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
		//癞子
		for(int j=0; j < hand_card_count; j++){
			if( table._logic.is_ci_card(table._logic.switch_to_card_index(cards[j])) ){
				cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}
		
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		//次牌
		int real_card = _send_card_data;
		/*if(table._logic.is_magic_card(_send_card_data)){
			real_card+=GameConstants.CARD_ESPECIAL_TYPE_HUN;
		}*/
		
		if( table._logic.is_ci_card(table._logic.switch_to_card_index(_send_card_data)) ){
			real_card+=GameConstants.CARD_ESPECIAL_TYPE_CI;
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
